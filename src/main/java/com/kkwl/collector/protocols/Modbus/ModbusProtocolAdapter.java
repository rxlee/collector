  package  com.kkwl.collector.protocols.Modbus;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.common.ParsingStatusType;
  import com.kkwl.collector.protocols.BaseProtocol;
  import com.kkwl.collector.protocols.Modbus.ModbusProtocolAdapter;
  import com.kkwl.collector.utils.Caculator;
  import com.kkwl.collector.utils.LogTools;
  import java.util.ArrayList;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import org.javatuples.Pair;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  //modbus协议适配器
  public class ModbusProtocolAdapter extends BaseProtocol
  {
    private static final Logger logger = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private byte answerType;//回复类型
    private static final int BUF_SIZE = 2048;
    private byte[] toBeParsedBytes = new byte[2048];
    
    public ModbusProtocolAdapter() {
      super(2, "ModbusRTU");
      this.status = ParsingStatusType.PARSING_DEVICE_ADDRESS;
    }
    ///字节解析 解析原始inBuffer数据返回retMapList{[result:true,]}
    public List<Map<String, Object>> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
      List<Map<String, Object>> retMapList = new ArrayList<Map<String, Object>>();
      Map<String, Object> retMap = new HashMap<String, Object>();
      short deviceAddress = ((Short)params.get("deviceAddress")).shortValue();
      String sn = (String)params.get("sn");
      byte lastCommand = ((Byte)params.get("lastCommand")).byteValue();
      
      if (inBuffer.length == 0) {
        retMap.put("result", Boolean.valueOf(false));
        retMapList.add(retMap);
        return retMapList;
      } 
      ///将字节添加到要被解析的字节数组
      System.arraycopy(inBuffer, 0, this.toBeParsedBytes, this.toBeParsedBytesStart, inBuffer.length);
      this.toBeParsedBytesStart += inBuffer.length;
      this.toBeParsedBytesLength += inBuffer.length;
      //起始位置
      this.pos = 0;
      while (true) {
        int leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_DEVICE_ADDRESS) {
          if (leftLen < 1) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            break;
          } 
          
          while (this.toBeParsedBytes[this.pos] != (byte)deviceAddress && this.pos < this.toBeParsedBytesLength) {
            this.pos++;
          }
          
          if (this.pos == this.toBeParsedBytesLength) {
            this.toBeParsedBytesStart = 0;
            this.toBeParsedBytesLength = 0;
            break;
          } 
          this.status = ParsingStatusType.PARSING_ANSWER_TYPE;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_ANSWER_TYPE) {
          if (leftLen < 2) {
            
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          this.answerType = this.toBeParsedBytes[this.pos + 1];
          
          if (this.answerType == 5 || this.answerType == 16) {
            this.status = ParsingStatusType.PARSING_BODY;
            this.bodyLength = 8;
          } else if ((byte)(this.answerType & 0x80) == Byte.MIN_VALUE) {
            this.status = ParsingStatusType.PARSING_BODY;
            this.bodyLength = 5;
          } else {
            this.status = ParsingStatusType.PARSING_LENGTH;
          } 
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_LENGTH) {
          if (leftLen < 3) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          this.length = this.toBeParsedBytes[this.pos + 2] & 0xFF;
          this.bodyLength = this.length + 5;
          this.status = ParsingStatusType.PARSING_BODY;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_BODY) {
          if (leftLen < this.bodyLength) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, this.bodyLength);
          ///解析帧
          try {
            retMapList.add(parseFrame(this.buffer, params));
          } catch (Exception e) {
            LogTools.log(logger, sn, LogType.WARN, "Modbus protocol adapter encontered error.", e);
          } 
          this.pos += this.bodyLength;
          this.status = ParsingStatusType.PARSING_DEVICE_ADDRESS;
        } 
      } 
      
      retMap.put("result", Boolean.valueOf(false));
      retMapList.add(retMap);
      
      return retMapList;
    }
    ///解析帧函数 解析帧字节返回retMap[type:modbus_data_frame ,result:true,valuemap:[sectionName:dataSection，]]
    private Map<String, Object> parseFrame(byte[] frameBytes, Map<String, Object> params) {
      String sn = (String)params.get("sn");
      int sectionLength = ((Integer)params.get("sectionLength")).intValue();
      String sectionName = (String)params.get("sectionName");
      
      StringBuilder hexStr = new StringBuilder();
      for (byte b : frameBytes) {
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
      } 
      
      LogTools.log(logger, sn, LogType.DEBUG, "Modbus protocol adapter begin parse " + hexStr);
      
      if ((byte)(frameBytes[1] & 0x80) == Byte.MIN_VALUE) {
        LogTools.log(logger, sn, LogType.WARN, "Modbus protocol adapter enter wrong response.");
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("result", Boolean.valueOf(false));
        return retMap;
      } 
      
      byte[] toBeCaculateBytes = new byte[this.bodyLength - 2];
      System.arraycopy(frameBytes, 0, toBeCaculateBytes, 0, this.bodyLength - 2);
      int calcCrc = Caculator.caculateCRC16(toBeCaculateBytes);
      byte loCrc = (byte)(calcCrc % 256);
      byte hiCrc = (byte)(calcCrc / 256);
      
      if (hiCrc != frameBytes[this.bodyLength - 1] || loCrc != frameBytes[this.bodyLength - 2]) {
        LogTools.log(logger, sn, LogType.WARN, "modbus protocol adapter crc check error");
        
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("result", Boolean.valueOf(false));
        return retMap;
      } 
      
      short answerType = (short)(frameBytes[1] & 0xFF);  ///功能码
      byte[] dataSection = null;
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (answerType == 5)
      {///返回写单个线圈8字节
        dataSection = new byte[8];
        System.arraycopy(frameBytes, 0, dataSection, 0, 8);
        retMap.put("type", FrameType.MODBUS_05_DATA_FRAME);
      }
      ///返回写多个寄存器8字节
      else if (answerType == 16) {
        dataSection = new byte[8];
        System.arraycopy(frameBytes, 0, dataSection, 0, 8);
        retMap.put("type", FrameType.MODBUS_10_DATA_FRAME);
      } else {
        short dataSectionLen = (short)(frameBytes[2] & 0xFF); ///读到的数据个数（字节）
        if (sectionLength != dataSectionLen) {
          
          LogTools.log(logger, sn, LogType.WARN, "Modbus protocol adapter enter wrong length response.");
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
        
        dataSection = new byte[dataSectionLen];
        System.arraycopy(frameBytes, 3, dataSection, 0, dataSectionLen);
        retMap.put("type", FrameType.MODBUS_DATA_FRAME);
      } 
      
      Pair<String, byte[]> pair = new Pair<String, byte[]>(sectionName, dataSection);
      
      retMap.put("result", Boolean.valueOf(true));
      retMap.put("valueMap", pair);
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\Modbus\ModbusProtocolAdapter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */