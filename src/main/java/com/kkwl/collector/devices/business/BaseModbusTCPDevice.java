  package  com.kkwl.collector.devices.business;
  
  import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import com.kkwl.collector.protocols.Modbus.ModbusTCPProtocolAdapter;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.RegisterType;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.models.DataSection;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.protocols.BaseProtocol;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.LogTools;

  public class BaseModbusTCPDevice extends BaseBusinessDevice
  {
    private static final Logger logger = LoggerFactory.getLogger(BaseModbusTCPDevice.class);

    private short deviceAddressShort;

    private final BaseProtocol protocol;

    private final Map<String, List<String>> registerVariableMap;

    private final ReentrantLock registerVariableMapLock;

    private String currentSectionName;
    
    private boolean canSendCommand;
    
    private long lastSendTime;
    
    private static final int WAITING_TIMEOUT_COUNT_UPLIMIT = 10;
    
    private int waiting_count = 0;
    
    private List<byte[]> yaokongCommands;
    
    private List<String> yaokongDataSections;
    byte lastCommandCode;
    private int lastMBAPIndex;
    //private BaseModbusTCPDevice
    public BaseModbusTCPDevice(String sn, String dtuSn, String name, String parentIndex, String type, String protocolParams, String deviceAddress, Byte deviceBeingUsed, long handlingPeriod, byte shouldReportAlarm, short connectionTimeoutDuration, String offlineReportId, Timestamp offlineTime, StatusType initStatus, List<DataSection> dataSections) {
      super(sn, dtuSn, name, parentIndex, type, protocolParams, deviceAddress, deviceBeingUsed, handlingPeriod, shouldReportAlarm, connectionTimeoutDuration, offlineReportId, offlineTime, initStatus);
      
      this.protocol = new ModbusTCPProtocolAdapter();
      for (DataSection dataSection : dataSections) {
        int sectionStart = dataSection.getStart().intValue();
        int sectionLength = dataSection.getLength().intValue();
        String sectionType = dataSection.getType();
        byte[] sectionBytes = new byte[sectionLength];
  
        
        Quartet<Integer, Integer, String, byte[]> newDataSection = new Quartet<Integer, Integer, String, byte[]>(Integer.valueOf(sectionStart), Integer.valueOf(sectionLength), sectionType, sectionBytes);
        this.dataSectionMap.put(dataSection.getName(), newDataSection);
        this.dataSectionNames.add(dataSection.getName());
        this.dataSectionNameTypeMap.put(dataSection.getName(), sectionType);
      } 
      
      if (this.dataSectionNames.isEmpty()) {
        this.currentSectionName = null;
      } else {
        this.currentSectionName = (String)this.dataSectionNames.get(0);
      } 
      
      this.registerVariableMap = new HashMap();
      this.registerVariableMapLock = new ReentrantLock();
      this.deviceAddressShort = Short.parseShort(deviceAddress);
      this.canSendCommand = true;
      this.lastSendTime = 0L;
      this.yaokongCommands = new ArrayList();
      this.yaokongDataSections = new ArrayList();
      this.lastMBAPIndex = 0;
    }

    public void parseProtocolVariable() {}

    public void initDataType() {}

    public void doParse() {
      if (this.protocol == null || this.dataSectionNames == null || this.dataSectionNames.isEmpty()) {
        return;
      }
      
      List<Map<String, Object>> parsedValueMapList = null;
      byte[] toBeParsed = getInBytesBuffer();
      if (toBeParsed == null) {
        return;
      }

      Map<String, Object> protocolParams = new HashMap<String, Object>();
      protocolParams.put("sn", this.sn);
      protocolParams.put("deviceAddress", Short.valueOf(this.deviceAddressShort));
      protocolParams.put("sectionName", this.currentSectionName);
      protocolParams.put("lastCommand", Byte.valueOf(this.lastCommandCode));
      protocolParams.put("lastMBAPIndex", Integer.valueOf(this.lastMBAPIndex));
      protocolParams.put("sectionLength", ((Quartet)this.dataSectionMap.get(this.currentSectionName)).getValue1());
      
      parsedValueMapList = this.protocol.doBytesParse(toBeParsed, protocolParams);
      
      for (Map<String, Object> parsedValueMap : parsedValueMapList) {
        if (parsedValueMap != null && ((Boolean)parsedValueMap.get("result")).booleanValue() == true) {
          FrameType frameType = (FrameType)parsedValueMap.get("type");
          if (this.lastCommandCode == 5) {
            if (frameType != FrameType.MODBUS_05_DATA_FRAME) {
              LogTools.log(logger, this.sn, LogType.WARN, "Base modbus TCP device sn " + this.sn + " received unexpected modbus response.");
              
              return;
            } 
            
            handle05DataFrame(parsedValueMap);
          } 
          
          if (this.lastCommandCode == 16) {
            if (frameType != FrameType.MODBUS_10_DATA_FRAME) {
              LogTools.log(logger, this.sn, LogType.WARN, "Base modbus TCP device sn " + this.sn + " received unexpected modbus response.");
              
              return;
            } 
            handle10DataFrame(parsedValueMap);
          } 
          
          if (frameType.value().equals(FrameType.MODBUS_DATA_FRAME.value())) {
            handle01Or020r03Or04DataFrame(parsedValueMap);
            resetCommandSendStatus();
            this.canSwitchToNext = true;
          } 
        } 
      } 
    }

    public void doBusiness() {
      Quartet<Integer, Integer, String, byte[]> dataSection = (Quartet)this.dataSectionMap.get(this.currentSectionName);
      int start = ((Integer)dataSection.getValue0()).intValue();
      int length = ((Integer)dataSection.getValue1()).intValue();
      
      if (this.canSendCommand) {
        if (this.yaokongCommands.size() > 0) {
          byte[] command = (byte[])this.yaokongCommands.get(0);
          if (this.yaokongCommands.get(0)[1] == 5) {
            this.outBytesBuffer.add(new Pair(FrameType.MODBUS_05_COMMAND_FRAME, this.yaokongCommands.get(0)));
            this.lastCommandCode = 5;
          } else if (this.yaokongCommands.get(0)[1] == 16) {
            this.outBytesBuffer.add(new Pair(FrameType.MODBUS_10_COMMAND_FrAME, this.yaokongCommands.get(0)));
            this.lastCommandCode = 16;
          } 
          
          this.canSendCommand = false;
          this.hasDataToSend = true;
          this.lastSendTime = System.currentTimeMillis();
        } else if (((String)dataSection.getValue2()).equals(RegisterType.COIL.strVal())) {
          sendQueryRegisterCommand(1, start, length);
        } else if (((String)dataSection.getValue2()).equals(RegisterType.DISCRETE.strVal())) {
          sendQueryRegisterCommand(2, start, length);
        } else if (((String)dataSection.getValue2()).equals(RegisterType.HOLDING.strVal())) {
          sendQueryRegisterCommand(3, start, length);
        } else if (((String)dataSection.getValue2()).equals(RegisterType.INPUT.strVal())) {
          sendQueryRegisterCommand(4, start, length);
        } 
      }
      
      if (this.waiting_count < 10) {
        this.waiting_count++;
      } else {
        LogTools.log(logger, this.sn, LogType.INFO, "Base mobus TCP device sn = " + this.sn + " waiting for data timeout, switch to another device.");
        
        if (this.lastCommandCode == 5) {
          this.yaokongCommands.remove(0);
          this.yaokongDataSections.remove(0);
        } 
        
        resetCommandSendStatus();
        this.canSwitchToNext = true;
      } 
    }

    public void toggleFlag(FrameType frameType) {}
    
    public byte[] getSectionBytes(String sectionName, int pos, int length) {
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
      }
      
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos + length)
      {
        return null;
      }
      
      String key = sectionName + "%%" + pos;
      if (!this.updatedSectionPos.containsKey(key) || 
        !((Boolean)this.updatedSectionPos.get(key)).booleanValue()) {
        this.updatedSectionPos.put(key, Boolean.valueOf(false));
        return null;
      } 
      
      byte[] bytes = new byte[length];
      System.arraycopy(((Quartet)this.dataSectionMap.get(sectionName)).getValue3(), pos, bytes, 0, length);
      this.updatedSectionPos.put(key, Boolean.valueOf(false));
      return bytes;
    }

    public Byte getSectionByte(String sectionName, int pos) {
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
      }
      
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos) {
        return null;
      }
      
      String key = sectionName + "%%" + pos;
      if (!this.updatedSectionPos.containsKey(key) || 
        !((Boolean)this.updatedSectionPos.get(key)).booleanValue()) {
        this.updatedSectionPos.put(key, Boolean.valueOf(false));
        return null;
      } 
      
      this.updatedSectionPos.put(key, Boolean.valueOf(false));
//      return Byte.valueOf((byte[])((Quartet)this.dataSectionMap.get(sectionName)).getValue3()[pos]);
      return Byte.valueOf(this.dataSectionMap.get(sectionName).getValue3()[pos]);
    }

    public byte[] getSectionBytesForDebug(String sectionName, int pos, int length) {
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
      }
      
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos + length)
      {
        return null;
      }
      
      byte[] bytes = new byte[length];
      System.arraycopy(((Quartet)this.dataSectionMap.get(sectionName)).getValue3(), pos, bytes, 0, length);
      return bytes;
    }

    public Byte getSectionByteForDebug(String sectionName, int pos) {
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
      }
      
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos) {
        return null;
      }
      
//      return Byte.valueOf((byte[])((Quartet)this.dataSectionMap.get(sectionName)).getValue3()[pos]);
      return Byte.valueOf(this.dataSectionMap.get(sectionName).getValue3()[pos]);
    }

    public Pair<Long, Byte> getHistorySectionByte(String sectionName, int pos) { return null; }

    public Pair<Long, byte[]> getHistorySectionBytes(String sectionName, int pos, int length) { return null; }
    
    private void sendQueryRegisterCommand(int type, int start, int length) {
      int sendLength;
      if (this.currentSectionName == null) {
        return;
      }

      if (this.lastMBAPIndex == 65535) {
        this.lastMBAPIndex = 0;
      } else {
        this.lastMBAPIndex++;
      } 
      
      byte[] queryBytesWithMBAP = new byte[12];
      
      queryBytesWithMBAP[0] = (byte)(this.lastMBAPIndex >> 8 & 0xFF);
      queryBytesWithMBAP[1] = (byte)(this.lastMBAPIndex & 0xFF);
      queryBytesWithMBAP[2] = 0;
      queryBytesWithMBAP[3] = 0;
      queryBytesWithMBAP[4] = 0;
      queryBytesWithMBAP[5] = 6;
      
      byte[] queryBytes = new byte[6];
      queryBytes[0] = (byte)(this.deviceAddressShort & 0xFF);
      queryBytes[1] = (byte)(type & 0xFF);
      
      queryBytes[2] = (byte)(start >> 8 & 0xFF);
      queryBytes[3] = (byte)(start & 0xFF);

      if (type == 3 || type == 4) {
        
        sendLength = length / 2;
      } else if (type == 1 || type == 2) {
        
        sendLength = length * 8;
      } else {
        sendLength = 0;
      } 
      
      queryBytes[4] = (byte)(sendLength >> 8 & 0xFF);
      queryBytes[5] = (byte)(sendLength & 0xFF);
      
      System.arraycopy(queryBytes, 0, queryBytesWithMBAP, 6, 6);
      
      boolean bError = false;
      this.outBytesBuffer.add(new Pair(FrameType.MODBUS_COMMAND_FRAME, queryBytesWithMBAP));
      this.lastCommandCode = (byte)type;
      
      if (bError) {
        return;
      }
      
      this.canSendCommand = false;
      this.hasDataToSend = true;
      this.lastSendTime = System.currentTimeMillis();
    }
    
    private void resetCommandSendStatus() {
      int index = 0;
      Iterator<String> dataSectionNameIterator = this.dataSectionNames.iterator();
      while (dataSectionNameIterator.hasNext()) {
        index++;
        String tempDataSectionName = (String)dataSectionNameIterator.next();
        if (this.currentSectionName.equals(tempDataSectionName)) {
          break;
        }
      } 
      
      this.registerVariableMapLock.lock();
      try {
        if (index == this.dataSectionNames.size()) {
          
          this.currentSectionName = (String)this.dataSectionNames.get(0);
        } else {
          this.currentSectionName = (String)this.dataSectionNames.get(index);
        } 
      } finally {
        this.registerVariableMapLock.unlock();
      } 
    }
    
    private void handle01Or020r03Or04DataFrame(Map<String, Object> parsedValueMap) {
      Pair<String, byte[]> valueMap = (Pair)parsedValueMap.get("valueMap");
      String sectionName = (String)valueMap.getValue0();
      byte[] datas = (byte[])valueMap.getValue1();
      
      Quartet<Integer, Integer, String, byte[]> sectionData = (Quartet)this.dataSectionMap.get(sectionName);
      
      if (datas.length == sectionData.getValue3().length) {
        System.arraycopy(datas, 0, sectionData.getValue3(), 0, datas.length);
        for (String key : this.updatedSectionPos.keySet()) {
          if (key.indexOf(sectionName) == 0) {
            this.updatedSectionPos.put(key, Boolean.valueOf(true));
          }
        } 
      } 
    }
    
    private void handle05DataFrame(Map<String, Object> parsedValueMap) {
      Pair<String, byte[]> valueMap = (Pair)parsedValueMap.get("valueMap");
      byte[] datas = (byte[])valueMap.getValue1();
      
      boolean isEqual = true;
      byte[] command = (byte[])this.yaokongCommands.get(0);
      if (datas.length != command.length) {
        
        isEqual = false;
      } else {
        for (int i = 0; i < datas.length; i++) {
          
          if (datas[i] != command[i]) {
            
            isEqual = false;
            
            break;
          } 
        } 
      } 
      if (isEqual) {
        
        String dataSectionName = new String((String)this.yaokongDataSections.get(0));
        this.currentSectionName = dataSectionName;
      } else {
        
        LogTools.log(logger, this.sn, LogType.ERROR, "Base mobus TCP device sn = " + this.sn + " send yaokong failed.");
      } 
      
      this.yaokongCommands.remove(0);
      this.canSendCommand = true;
      this.waiting_count = 0;
    }
    
    private void handle10DataFrame(Map<String, Object> parsedValueMap) {
      Pair<String, byte[]> valueMap = (Pair)parsedValueMap.get("valueMap");
      byte[] datas = (byte[])valueMap.getValue1();
      
      String dataSectionName = new String((String)this.yaokongDataSections.get(0));
      this.currentSectionName = dataSectionName;
      
      this.yaokongCommands.remove(0);
      this.canSendCommand = true;
      this.waiting_count = 0;
    }

    public void resetCanSwitchToNext() {
      this.canSwitchToNext = false;
      this.waiting_count = 0;
      this.canSendCommand = true;
    }

    public void handleValueChangedMessage(String deviceVariableSn, String deviceVariableValue, String type) {
      if (type.equals("yaokong-act")) {
        
        DeviceVariable deviceVariable = GlobalVariables.getDeviceVariableBySn(deviceVariableSn);
        if (!deviceVariable.getCollectorDeviceSn().equals(this.sn)) {
          LogTools.log(logger, this.sn, LogType.ERROR, "Base mobus TCP device sn = " + this.sn + " variable sn = " + deviceVariableSn + " isn't belong to " + this.sn);
          
          return;
        } 
        if (deviceVariable.getRw() == null || deviceVariable.getRw().byteValue() == 1) {
          LogTools.log(logger, this.sn, LogType.ERROR, "Base mobus TCP device sn = " + this.sn + " variable sn = " + deviceVariableSn + " is readonly.");
          
          return;
        } 
        Byte value = null;
        try {
          value = Byte.valueOf(Byte.parseByte(deviceVariableValue));
        } catch (NumberFormatException e) {
          LogTools.log(logger, this.sn, LogType.ERROR, "Base mobus TCP device sn = " + this.sn + " variable sn = " + deviceVariableSn + " transform value error. value = " + deviceVariableValue);
          
          value = null;
        } 
        
        if (value == null) {
          return;
        }
        
        if (value.byteValue() != 0 && value.byteValue() != 1) {
          LogTools.log(logger, this.sn, LogType.ERROR, "Base mobus TCP device sn = " + this.sn + " variable sn = " + deviceVariableSn + " value is invlaud, should be 0 or 1.  value = " + deviceVariableValue);
          
          return;
        } 
        
        if (deviceVariable.getRegisterType().equals(RegisterType.HOLDING.name())) {
          int pos = deviceVariable.getRegisterTypeIndex().intValue() - deviceVariable.getRegisterIndex().intValue() + deviceVariable.getRegisterIndex().intValue() / 2;
          byte[] queryBytesWithCrc = new byte[11];
          byte[] queryBytes = new byte[9];
          queryBytes[0] = (byte)(this.deviceAddressShort & 0xFF);
          queryBytes[1] = 16;
          queryBytes[2] = (byte)(pos >> 8 & 0xFF);
          queryBytes[3] = (byte)(pos & 0xFF);
          queryBytes[4] = 0;
          queryBytes[5] = 1;
          queryBytes[6] = 2;
          queryBytes[7] = 0;
          queryBytes[8] = (byte)((value.byteValue() == 0) ? 0 : 1);
          
          int crc = Caculator.caculateCRC16(queryBytes);
          System.arraycopy(queryBytes, 0, queryBytesWithCrc, 0, 9);
          queryBytesWithCrc[9] = (byte)(crc % 256);
          queryBytesWithCrc[10] = (byte)(crc / 256);
  
        
        }
        else if (deviceVariable.getRegisterType().equals(RegisterType.COIL.name())) {
          byte[] queryBytesWithCrc = new byte[8];
          byte[] queryBytes = new byte[6];
          queryBytes[0] = (byte)(this.deviceAddressShort & 0xFF);
          queryBytes[1] = 5;
          queryBytes[2] = (byte)(deviceVariable.getRegisterIndex().intValue() >> 8 & 0xFF);
          queryBytes[3] = (byte)(deviceVariable.getRegisterIndex().intValue() & 0xFF);
          queryBytes[4] = (byte)((value.byteValue() == 0) ? 0 : -1);
          queryBytes[5] = 0;
          
          int crc = Caculator.caculateCRC16(queryBytes);
          System.arraycopy(queryBytes, 0, queryBytesWithCrc, 0, 6);
          queryBytesWithCrc[6] = (byte)(crc % 256);
          queryBytesWithCrc[7] = (byte)(crc / 256);
        } 
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\business\BaseModbusTCPDevice.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */