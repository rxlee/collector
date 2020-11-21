 package  com.kkwl.collector.devices.business;
 import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.StandardEnum;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.models.DataSection;
import com.kkwl.collector.protocols.BaseProtocol;
import com.kkwl.collector.protocols.IEC104.IEC104ProtocolAdapter;
import com.kkwl.collector.utils.LogTools;
import com.kkwl.collector.utils.NumberUtil;
 
 public class BaseIEC104Device extends BaseBusinessDevice {
    private static final Logger logger = LoggerFactory.getLogger(BaseIEC104Device.class);
   
   private BaseProtocol protocol;
   
   List<Pair<String, String>> ykCommandList;
   
   private int ykAddress;
   
   private byte ykValue;
   
   private int sdAddress;
   
   private float sdValue;
   private boolean hasSentConnectCommand;
   private boolean hasSentRecruitmentCommand;
   private boolean hasSentDDRecruitmentCommand;
   private boolean hasSentTestCommand;
   private boolean hasSentCorrectTimeCommand;
   private boolean hasSentYKSelectCommand;
   private boolean hasSentYKExecuteCommand;
   private boolean hasSentSDCommand;
   private boolean hasSentHistoryCallCommand;
   private boolean useDD;
   private boolean useJS;
   private Instant lastTestTime;
   private Instant lastConnectTime;
   private Instant lastZzTime;
   private Instant lastDDZZTime;
   private Instant lastCorrectTimeTime;
   private int sendNo;
   private int recvNo; ///接收序列号，两个字节
   private StatusType protocolStatus;
   private boolean requireIFrameAck;
   private String lastIFrameAction;
   private ScheduledFuture iFrameTimeCounter;
   private boolean canHandleYKCommand;
   private boolean isCircuitInitialized;
   private int ddRecruitmentRetryCounts;
   private int correctTimeRetryCounts;
   private int ykSelectRetryCounts;
   private int ykExecuteRetryCounts;
   private int sdRetryCounts;
   private int historyCalledRetryCounts;
    private int csyyLength = 2; //传送原因长度
    private int ggdzLength = 2; //公共地址长度
    private int ggdz = 0;  //公共地址
    private int xxtLength = 3; ///信息体地址长度
    private int timeoutT0 = 30; //秒 TCP连接建立的超时时间，主站没有Connect()过来就退出等待连接状态
    private int timeoutT1 = 15; //秒   RTU启动U格式测试过程后等待U格式测试应答的超时时间
    private int timeoutT2 = 10; //秒 RTU以突发的原因向主站端上送了变化信息
    private int timeoutT3 = 20; //秒  没有实际的数据交换时
    private int zzTimeInterval = 120;  ///秒 总召周期
    private int ddTimeInterval = 300;  ///秒 电度周期
    private int jsTimeInterval = 1800;
    private byte testCount = 0;
    private int zzFailTime = 120;
   ///基本104设备（sn,通道sn，名称，类型，协议参数，设备地址，。。。。。。）
   public BaseIEC104Device(String sn, String dtuSn, String name, String parentIndex, String type, String protocolParams, String deviceAddress, Byte deviceBeingUsed, long handlingPeriod, byte shouldReportAlarm, short connectionTimeoutDuration, String offlineReportId, Timestamp offlineTime, StatusType initStatus, List<DataSection> dataSections)
   {
      super(sn, dtuSn, name, parentIndex, type, protocolParams, deviceAddress, deviceBeingUsed, handlingPeriod, shouldReportAlarm, connectionTimeoutDuration, offlineReportId, offlineTime, initStatus);
     
      for (DataSection dataSection : dataSections) { //DataSection：名字，起始位置，长度，类型YC/YX
        int sectionStart = dataSection.getStart().intValue();  //起始位置
        int sectionLength = dataSection.getLength().intValue(); //长度
        String sectionType = dataSection.getType(); //类型YC/YX
        byte[] sectionBytes = new byte[sectionLength]; //
       
        if (sectionType.equals("YC") && sectionStart >= 16385) { //遥测地址4001H（16385）-5000H（20480）
          sectionStart -= 16385;
        } else if (sectionType.equals("DD") && sectionStart >= 25061) {//电度61E5 H
          sectionStart -= 25061;
        } else if (sectionType.equals("YK") && sectionStart >= 24577) {//遥控地址6001H（24577）-6100H
          sectionStart -= 24577;
       } 
       
        if (sectionType.equals("DD")) {
          this.useDD = true;
       }
 
       //********************DataSection:名字、数据区起始地址、数据区长度、数据区类型（YX/YX/YK/DD）************************
        Quartet<Integer, Integer, String, byte[]> newDataSection = new Quartet<Integer, Integer, String, byte[]>(Integer.valueOf(sectionStart), Integer.valueOf(sectionLength), sectionType, sectionBytes);
        this.dataSectionMap.put(dataSection.getName(), newDataSection); //(LXBE-<100,4,YC,[]>)
        this.dataSectionNames.add(dataSection.getName());
        this.dataSectionNameTypeMap.put(dataSection.getName(), sectionType); //(名字，类型（YX/YX/YK/DD）)
     } 
     //104协议适配器
      this.protocol = new IEC104ProtocolAdapter(); //协议实例化
     
      this.ykCommandList = new ArrayList();
     
      this.lastConnectTime = Instant.now().minusSeconds(1800L);
      this.hasSentConnectCommand = false;
      this.hasSentRecruitmentCommand = false;
      this.hasSentDDRecruitmentCommand = false;
      this.hasSentTestCommand = false;
      this.hasSentCorrectTimeCommand = false;
      this.hasSentYKSelectCommand = false;
      this.hasSentYKExecuteCommand = false;
      this.hasSentSDCommand = false;
      this.hasSentHistoryCallCommand = false;
      this.useJS = false;
      this.protocolStatus = StatusType.OFFLINE;
      this.ddRecruitmentRetryCounts = 0;
      this.correctTimeRetryCounts = 0;
      this.ykSelectRetryCounts = 0;
      this.ykExecuteRetryCounts = 0;
      this.sdRetryCounts = 0;
      this.historyCalledRetryCounts = 0;
   }

   public void initDataType() {}
     //解析协议变量，协议的基本参数（传送原因、公共地址、公共地址长度、协议体长度、超时时间t0、t1、t2、t3。。。。）
   public void parseProtocolVariable() {
     try {
        JSONObject protocolVariableJSON = new JSONObject(this.protocolParams);
       
        if (protocolVariableJSON.has("csyy_length")) {
          this.csyyLength = protocolVariableJSON.getInt("csyy_length");
       }
       
        if (protocolVariableJSON.has("ggdz")) {
          this.ggdz = protocolVariableJSON.getInt("ggdz");
       }
       
        if (protocolVariableJSON.has("ggdz_length")) {
          this.ggdzLength = protocolVariableJSON.getInt("ggdz_length");
       }
       
        if (protocolVariableJSON.has("xxt_length")) {
          this.xxtLength = protocolVariableJSON.getInt("xxt_length");
       }
       
        if (protocolVariableJSON.has("timeout_t0")) {
          this.timeoutT0 = protocolVariableJSON.getInt("timeout_t0");
       }
       
        if (protocolVariableJSON.has("timeout_t1")) {
          this.timeoutT1 = protocolVariableJSON.getInt("timeout_t1");
       }
       
        if (protocolVariableJSON.has("timeout_t2")) {
          this.timeoutT2 = protocolVariableJSON.getInt("timeout_t2");
       }
       
        if (protocolVariableJSON.has("timeout_t3")) {
          this.timeoutT3 = protocolVariableJSON.getInt("timeout_t3");
       }
       
        if (protocolVariableJSON.has("zz_time_interval")) {
          this.zzTimeInterval = protocolVariableJSON.getInt("zz_time_interval");
       }
       
        if (protocolVariableJSON.has("use_dd")) {
          this.useDD = (protocolVariableJSON.getInt("use_dd") == 1);
       }
       
        if (protocolVariableJSON.has("dd_time_interval")) {
          this.ddTimeInterval = protocolVariableJSON.getInt("dd_time_interval");
       }
       
        if (protocolVariableJSON.has("use_js")) {
          this.useJS = (protocolVariableJSON.getInt("use_js") == 1);
       }
       
        if (protocolVariableJSON.has("js_time_interval")) {
          this.jsTimeInterval = protocolVariableJSON.getInt("js_time_interval");
       }
      } catch (JSONException e) {
        LogTools.log(logger, this.sn, LogType.WARN, "Base 104 device sn = " + this.sn + " exception occured when parse protocol variables.", e);
     } 
   }
   //解析收到的InBytesBuffer内容解析报文
   public void doParse() { //**********************解析收到的InBytesBuffer内容解析报文************************
      if (this.protocol == null) {
       return;
     }
      List<Map<String, Object>> parsedValueMapList = null;

      byte[] toBeParsed = getInBytesBuffer(); //**************要被解析的原始数据*****************
      if (toBeParsed == null) {
       return;
     }
      Map<String, Object> protocolParams = new HashMap<String, Object>();
      protocolParams.put("sn", this.sn);
      protocolParams.put("csyyLength", Integer.valueOf(this.csyyLength));
      protocolParams.put("ggdzLength", Integer.valueOf(this.ggdzLength));
      protocolParams.put("xxtLength", Integer.valueOf(this.xxtLength));
     
      boolean shouldCountinue = true;
      while (shouldCountinue) {

          ///doBytesParse()为IEC104ProtocolAdapter的函数，返回要被解析结果map列表集合
        parsedValueMapList = this.protocol.doBytesParse(toBeParsed, protocolParams); //***********字节解析：要解析的字节数组和协议参数（byte[],MAP）*******************
          ///需要解析的parsedValueMap {"type":" ","result":"","recvNo":"","sendNo":"","valueMap":""}--本身是list集合
         //parsedValueMap为一帧数据，根据帧类型然后对每一帧数据解析返回相对应的命令
        for (Map<String, Object> parsedValueMap : parsedValueMapList) {
          if (parsedValueMap != null && ((Boolean)parsedValueMap.get("result")).booleanValue() == true)
          {
            FrameType frameType = (FrameType)parsedValueMap.get("type");
   //*************根据帧类型选择报文处理器 *****************总召确认/初始化完成/总召完成/测试确认/测试/遥信 I帧/遥测 I帧/连接时间确认/遥控选择确认/SD确认帧/SOE I帧/电度I帧
            if (this.protocolStatus == StatusType.ONLINE) //在线
            {
                       ///总召确认 然后发送 IEC104_S_ACK_FRAME
                      if (frameType.value().equals(FrameType.IEC104_RECRUITMENT_ACK_FRAME.value()) && this.hasSentRecruitmentCommand == true) {
                        handleRecruitmentAckFrame(parsedValueMap);
                        continue;
                      }
                      ///初始化完成  发送68 04 01 00 00 00
                      if (frameType.value().equals(FrameType.IEC104_INITIALIZATION_COMPLETE_FRAME.value())) {
                        handleInitializationCompleteFrame(parsedValueMap);
                        continue;
                      }
                      ///总召完成
                      if (frameType.value().equals(FrameType.IEC104_RECRUITMENT_COMPLETE_FRAME.value()) && this.hasSentRecruitmentCommand == true) {
                        handleRecruitmentCompleteFrame(parsedValueMap);
                        continue;
                      }
                      ///测试确认
                      if (frameType.value().equals(FrameType.IEC104_TEST_ACK_FRAME.value()) && this.hasSentTestCommand == true) {
                        handleTestAckFrame();
                        continue;
                      }
                      ///测试
                      if (frameType.value().equals(FrameType.IEC104_TEST_FRAME.value())) {
                        handleTestFrame();
                        continue;
                      }
                      ///遥信 I帧
                      if (frameType.value().equals(FrameType.IEC104_YX_I_FRAME.value())) {
                        handleYXIFrame(parsedValueMap);
                        continue;
                      }
                      ///遥测 I帧
                      if (frameType.value().equals(FrameType.IEC104_YC_I_FRAME.value())) {
                        handleYCIFrame(parsedValueMap);
                        continue;
                      }
                      ///连接时间确认
                      if (frameType.value().equals(FrameType.IEC104_CORRECT_TIME_ACK_FRAME.value())) {
                        handleCorrectTimeAckFrame(parsedValueMap); continue;
                      }
                      ///遥控选择确认
                      if (frameType.value().equals(FrameType.IEC104_YK_SELECT_ACK_FRAME.value()) && this.hasSentYKSelectCommand == true) {
                        handleYKSelectAckFrame(parsedValueMap); continue;
                      }
                      ///遥控执行确认
                      if (frameType.value().equals(FrameType.IEC104_YK_EXECUTE_ACK_FRAME.value()) && this.hasSentYKExecuteCommand == true) {
                        handleYKExecuteAckFrame(parsedValueMap); continue;
                      }
                      ///遥控执行完成确认
                      if (frameType.value().equals(FrameType.IEC104_YK_EXECUTE_COMPLETE_FRAME.value()) && this.hasSentYKExecuteCommand == true) {
                        handleYKExecuteCompleteFrame(parsedValueMap); continue;
                      }
                      ///SD确认帧
                      if (frameType.value().equals(FrameType.IEC104_SD_ACK_FRAME.value()) && this.hasSentSDCommand == true) {
                        handleSDAckFrame(parsedValueMap); continue;
                      }
                        ///SOE I帧
                      if (frameType.value().equals(FrameType.IEC104_SOE_I_FRAME.value())) {
                        handleSOEIFrame(parsedValueMap); continue;
                      }
                      ///电度I帧
                      if (frameType.value().equals(FrameType.IEC104_DD_I_FRAME.value())) {
                        handleDDIFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_FAULT_I_FRAME.value())) {
                        handleFaultIFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_DD_RECRUITMENT_ACK_FRAME.value())) {
                        handleDDRecruitmentAckFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_DD_RECRUITMENT_COMPLETE_FRAME.value())) {
                        handleDDRecruitmentCompleteFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_HISTORY_RECRUITMENT_ACK_FRAME.value())) {
                        handleHistoryRecruitmentAckFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_HISTORY_RECRUITMENT_COMPLETE_FRAME.value())) {
                        handleHistoryRecruitmentCompleteFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_HISTORY_YX_FRAME.value())) {
                        handleHistoryYXFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_HISTORY_YC_FRAME.value())) {
                        handleHistoryYCFrame(parsedValueMap); continue;
                      }
                      if (frameType.value().equals(FrameType.IEC104_HISTORY_DD_FRAME.value())) {
                        handleHistoryDDFrame(parsedValueMap);
                     }
             continue;
           } 
            if (frameType.value().equals(FrameType.IEC104_CONNECT_ACK_FRAME.value())) ///如果收到连接确认帧
            {
              handleConnectAckFrame();
             }
           continue;
         } 
          shouldCountinue = false;
       } 
     } 
   }

   public void doBusiness() {
      Instant currentInstant = Instant.now();
      if (this.protocolStatus == StatusType.OFFLINE) //如果设备不在线，尝试连接
      {
        Duration duration = Duration.between(this.lastConnectTime, currentInstant);
        if (!this.hasSentConnectCommand)//如果没有连接命令则发送连接命令
         {
          LogTools.log(logger, this.sn, LogType.DEBUG, "Base 104 device sn = " + this.sn + " " + duration.getSeconds() + " seconds has passed since last connect command.");
          sendConnectCommand();
          this.lastConnectTime = currentInstant;
          }
        else if (duration.getSeconds() > this.timeoutT0)
         {
          this.hasSentConnectCommand = false;
         }
     } else//如果在线
         {
           Duration idleInterval = Duration.between(this.lastReceivedPacketTime, currentInstant);
          if (!this.hasSentTestCommand && idleInterval.getSeconds() > this.timeoutT3)
          {
               sendTest();
            } else if (this.hasSentTestCommand)  //如果有测试命令
           {
           if (this.lastTestTime != null) {
            Duration testTimeoutDuration = Duration.between(this.lastTestTime, currentInstant);
            if (testTimeoutDuration.getSeconds() > this.timeoutT1) {
              if (this.testCount < 3) {
                this.testCount = (byte)(this.testCount + 1);
                sendTest();
                this.lastTestTime = currentInstant;
             } else
                 {
                LogTools.log(logger, this.sn, LogType.DEBUG, "Base 104 device sn = " + this.sn + " test timeout.");
                setProtocolStatusOffLine();
             } 
           }
         } 
        } else if (GlobalVariables.GLOBAL_IEC_STANDARD.equals(StandardEnum.IEC104_STANDARD_GB.getName()))
          {
            if (this.isCircuitInitialized)
             {
              businessFlow(currentInstant);
             }
          } else
           {
            businessFlow(currentInstant);
            }
     }
   }
//业务流
   private void businessFlow(Instant currentInstant) {
      Duration zzInterval = Duration.between(this.lastZzTime, currentInstant); //总召间隔
      Duration correctTimeInterval = Duration.between(this.lastCorrectTimeTime, currentInstant); //正确时间间隔
      Duration ddzzInterval = Duration.between(this.lastDDZZTime, currentInstant);
      Duration historyCalledInterval = (this.lastHistoryCalledTime == null) ? null : Duration.between(this.lastHistoryCalledTime, currentInstant); //历史调用间隔
     
      if (this.useJS && correctTimeInterval.getSeconds() > this.jsTimeInterval && !this.hasSentCorrectTimeCommand && !this.requireIFrameAck && !this.hasSentTestCommand)
      {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(currentInstant, GlobalVariables.GLOBAL_DEFAULT_ZONEID);
        logger.debug("Base 104 device sn = " + this.sn + " send correct time frame at " + GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(localDateTime));
        sendCorrectTimeCommand();  //发送对时命令
      } else if (!this.hasSentYKSelectCommand && !this.hasSentYKExecuteCommand && !this.requireIFrameAck && !this.ykCommandList.isEmpty() && !this.hasSentTestCommand)
      {
        Pair<String, String> ykPair = (Pair)this.ykCommandList.get(0);
        String deviceVariableSn = (String)ykPair.getValue0();
        String deviceVariableValue = (String)ykPair.getValue1();
       
           try {
              this.ykAddress = GlobalVariables.getDeviceVariableBySn(deviceVariableSn).getRegisterTypeIndex().intValue();
            } catch (Exception e) {
              this.ykAddress = -1;
              LogTools.log(logger, this.sn, LogType.DEBUG, "Base 104 device sn = " + this.sn + " get yk variable address error.");
           }
       
        this.ykValue = Byte.valueOf(deviceVariableValue).byteValue();
        if (this.ykAddress < 0 || (this.ykValue != 0 && this.ykValue != 1))
        {
          LogTools.log(logger, this.sn, LogType.WARN, "Base 104 device sn = " + this.sn + " invalid yaokong value = " + this.ykValue);
          this.ykCommandList.remove(0);
         return;
         }
        sendYKSelect(); ///发送遥控选择
        this.ykCommandList.remove(0);
      } else if (zzInterval.getSeconds() > this.zzTimeInterval) //如果总召间隔大于总召时间周期
      {
        if (!this.hasSentRecruitmentCommand && !this.requireIFrameAck && !this.hasSentTestCommand)
       {
          sendRecruitmentCommand(); //发送总召命令
       }
      } else if (this.useDD && ddzzInterval.getSeconds() > this.ddTimeInterval && this.ddRecruitmentRetryCounts < 3) {
        if (!this.hasSentDDRecruitmentCommand && !this.requireIFrameAck && !this.hasSentTestCommand)
       {
          sendDDRecruitmentCommand(); //发送电度总召命令
       }
      } else if (this.needHistoryFlag && historyCalledInterval != null && historyCalledInterval.getSeconds() > 30L && this.historyCalledRetryCounts < 3) {
       
        if (!this.hasSentHistoryCallCommand && !this.requireIFrameAck && !this.hasSentTestCommand)
       {
          sendHistoryRecruitmentCommand(); //发送历史总召命令
       }
     } 
   }

   public void toggleFlag(FrameType frameType) {
      Instant currentInstant = Instant.now();
      if (frameType.value().equals(FrameType.IEC104_CONNECT_FRAME.value())) {
        this.lastConnectTime = currentInstant;
      } else if (frameType.value().equals(FrameType.IEC104_TEST_FRAME.value())) {
        this.lastTestTime = currentInstant;
      } else if (frameType.value().equals(FrameType.IEC104_CORRECT_TIME_FRAME.value())) {
        this.lastCorrectTimeTime = currentInstant;
      } else if (frameType.value().equals(FrameType.IEC104_RECRUITMENT_FRAME.value())) {
        this.lastZzTime = currentInstant;
      } else if (frameType.value().equals(FrameType.IEC104_DD_RECRUITMENT_FRAME.value())) {
        this.lastDDZZTime = currentInstant;
      } else if (frameType.value().equals(FrameType.IEC104_HISTORY_RECRUITMENT_FRAME.value())) {
        this.lastHistoryCalledTime = currentInstant;
      } else if (!frameType.value().equals(FrameType.IEC104_YK_SELECT_FRAME.value())) {
       
        if (!frameType.value().equals(FrameType.IEC104_YK_EXECUTE_FRAME.value()))
       {
          if (frameType.value().equals(FrameType.IEC104_SD_FRAME.value()));
       }
     } 
   }

   public void handleValueChangedMessage(String deviceVariableSn, String deviceVariableValue, String type) {
     try {
       Pair<String, String> ykPair;
        switch (type) {
         case "yaokong-act":
            ykPair = new Pair<String, String>(deviceVariableSn, deviceVariableValue);
            this.ykCommandList.add(ykPair);
           return;
         case "shedian":
            if (this.hasSentSDCommand) {
             
              LogTools.log(logger, this.sn, LogType.WARN, "bBase 104 device sn = " + this.sn + " can't handle sd command because another command is being handled.");
             
             return;
           } 
           
           try {
              this.sdAddress = GlobalVariables.getDeviceVariableBySn(deviceVariableSn).getRegisterTypeIndex().intValue();
            } catch (Exception e) {
              LogTools.log(logger, this.sn, LogType.DEBUG, "Base 104 device sn = " + this.sn + " get yk variable address error.");
           } 
           
            this.sdValue = Float.valueOf(deviceVariableValue).floatValue();
            sendSDCommand();
           return;
       } 
        LogTools.log(logger, this.sn, LogType.ERROR, "Base 104 device sn = " + this.sn + " invalid type : " + type);
     }
      catch (NumberFormatException e) {
        LogTools.log(logger, this.sn, LogType.ERROR, "Base 104 device sn = " + this.sn + " failed to convert yao kong value to byte : " + deviceVariableValue);
     } 
   }
   //发送连接命令
   private void sendConnectCommand() {
      byte[] command = { 104, 4, 7, 0, 0, 0 };
      this.outBytesBuffer.add(new Pair(FrameType.IEC104_CONNECT_FRAME, command));
      this.hasSentConnectCommand = true;
      this.hasDataToSend = true;
   }
   //处理连接确认帧，置要解析的缓冲区长度为0
   private void handleConnectAckFrame() {
      if (this.hasSentConnectCommand) {
        this.protocol.resetToBeParsedBuffer();
        setProtocolStatusOnLine();
     } 
   }
   
   private void handleInitializationCompleteFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
      this.isCircuitInitialized = true;
   }
 //发送总召命令68 0E 00 00 00 00 64 01   06 00   01 00   00 00 00 14
   private void sendRecruitmentCommand() {
      if (!this.hasSentTestCommand) {

        List<Byte> recruitmentCommand = new ArrayList<Byte>();
        recruitmentCommand.add(Byte.valueOf((byte)104)); //68
        recruitmentCommand.add(Byte.valueOf((byte)14));  //0E
        recruitmentCommand.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));  //00
        recruitmentCommand.add(Byte.valueOf((byte)(this.sendNo >> 7)));  //00
        recruitmentCommand.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));  //00
        recruitmentCommand.add(Byte.valueOf((byte)(this.recvNo >> 7)));  //00
        recruitmentCommand.add(Byte.valueOf((byte)100)); //64
        recruitmentCommand.add(Byte.valueOf((byte)1));  //01
        recruitmentCommand.add(Byte.valueOf((byte)6));  //06
        if (this.csyyLength == 2) {
          recruitmentCommand.add(Byte.valueOf((byte)0)); //00
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)(this.ggdz & 0xFF))); //01
        if (this.ggdzLength == 2) {
          recruitmentCommand.add(Byte.valueOf((byte)0)); //00
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)0)); //00
        recruitmentCommand.add(Byte.valueOf((byte)0)); //00
        if (this.xxtLength == 3) {
          recruitmentCommand.add(Byte.valueOf((byte)0)); //00
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)20)); //14
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFF);
       
        byte[] sendBytes = new byte[recruitmentCommand.size()];
        ///发送的字节数组
        for (int i = 0; i < recruitmentCommand.size(); i++) {
          sendBytes[i] = ((Byte)recruitmentCommand.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_RECRUITMENT_FRAME, sendBytes)); ///添加到队列
     } 
     
      this.hasSentRecruitmentCommand = true;
      this.hasDataToSend = true;
      this.requireIFrameAck = true;
      this.lastIFrameAction = FrameType.IEC104_RECRUITMENT_FRAME.value();
      this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
        {
          public void run() {
            try {
              BaseIEC104Device.this.reDoIFrameTask();
            } catch (Exception ex) {
              LogTools.log(logger, BaseIEC104Device.this.sn, LogType.ERROR, "Base business device sn = " + BaseIEC104Device.this.sn + " error occured when calling data.");
            } 
          }
        }, this.zzFailTime, TimeUnit.SECONDS);  ///I帧 两分钟发送总召
   }
 //处理总召确认帧
   private void handleRecruitmentAckFrame(Map<String, Object> parsedValueMap) {
      if (this.hasSentRecruitmentCommand) {
        this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
        this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
        sendS();
     } 
   }
   
   private void handleRecruitmentCompleteFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
     
      this.hasSentRecruitmentCommand = false;
      this.requireIFrameAck = false;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }
     ///处理遥信数据
    private void handleYXIFrame(Map<String, Object> parsedValueMap) { commonHandle1ByteData(parsedValueMap, "YX"); }

    private void handleYCIFrame(Map<String, Object> parsedValueMap) { commonHandle4BytesData(parsedValueMap, "YC"); }
    //处理4字节遥测数据
   private void commonHandle4BytesData(Map<String, Object> parsedValueMap, String sectionName) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS(); //发送S帧回应

      Map<Integer, byte[]> valueMap = (Map)parsedValueMap.get("valueMap"); //值的map valueMap  地址：值
      Map<Integer, Long> timeValueMap = (Map)parsedValueMap.get("timeValueMap"); //没有此项
      if (valueMap == null || valueMap.size() == 0) {
        LogTools.log(logger, this.sn, LogType.WARN, "Base 104 device sn = " + this.sn + " can't get any " + sectionName.toLowerCase() + " value");
       return;
     } 
      for (Integer key : valueMap.keySet()) {//遍历key 位置
        for (Quartet<Integer, Integer, String, byte[]> dataSection : this.dataSectionMap.values()) { //dataSectionMap-(LXBE-<100,4,YC,[]>)
          if (!((String)dataSection.getValue2()).equals(sectionName)) {
           continue;
             }
         
          int start = ((Integer)dataSection.getValue0()).intValue(); //起始位置 YC[100]
          int length = ((Integer)dataSection.getValue1()).intValue(); //长度  4字节4321
         
          if (key.intValue() * 4 >= start && key.intValue() * 4 + 3 < start + length) {

            String sectionPos = (String)dataSection.getValue2() + (key.intValue() * 4 - start); //类型+地址，YC120
            if (timeValueMap == null || timeValueMap.get(key) == null) {
              byte[] value = (byte[])valueMap.get(key); //根据地址得到相应的 值
              System.arraycopy(value, 0, dataSection.getValue3(), key.intValue() * 4 - start, 4);
             
              this.updatedSectionPos.put(sectionPos, Boolean.valueOf(true));
              continue;
           } 
            Long updateTimeLong = (Long)timeValueMap.get(key);
            if (this.historyValueMap.get(sectionPos) == null) {
              byte[] value = (byte[])valueMap.get(key); //值
             
              LinkedBlockingQueue<Pair<Long, byte[]>> bq = new LinkedBlockingQueue<Pair<Long, byte[]>>(); //<Long时间，bytes[]>
              bq.add(new Pair(updateTimeLong, value)); // （时间，值）
              this.historyValueMap.put(sectionPos, bq);  //******************* {YC120:LinkedBlockingQueue<Pair<updateTimeLong, value>>}
              continue;
           } 
            byte[] value = (byte[])valueMap.get(key); //值
            ((LinkedBlockingQueue)this.historyValueMap.get(sectionPos)).add(new Pair(updateTimeLong, value)); //LinkedBlockingQueue<Pair<Long, byte[]>>添加内容
         } 
       } 
     } 
   }

    private void handleSOEIFrame(Map<String, Object> parsedValueMap) { commonHandle1ByteData(parsedValueMap, "YX"); }
     ///处理1字节遥信（YX）数据
   private void commonHandle1ByteData(Map<String, Object> parsedValueMap, String sectionName) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
     
      Map<Integer, Integer> valueMap = (Map)parsedValueMap.get("valueMap");
      Map<Integer, Long> timeValueMap = (Map)parsedValueMap.get("timeValueMap");
      if (valueMap == null || valueMap.size() == 0) {
        LogTools.log(logger, this.sn, LogType.WARN, "Base 104 device sn = " + this.sn + " can't get any " + sectionName.toLowerCase() + " value.");
       return;
     } 
      for (Integer key : valueMap.keySet()) {
        for (Quartet<Integer, Integer, String, byte[]> dataSection : this.dataSectionMap.values()) {
          if (!((String)dataSection.getValue2()).equals(sectionName)) {
           continue;
         }
         
          int start = ((Integer)dataSection.getValue0()).intValue();
          int length = ((Integer)dataSection.getValue1()).intValue();
         
          if (key.intValue() >= start && key.intValue() < start + length) {
            String sectionPos = (String)dataSection.getValue2() + (key.intValue() - start);
            if (timeValueMap == null || timeValueMap.get(key) == null) {
             
              dataSection.getValue3()[key.intValue() - start] = (byte)(((Integer)valueMap.get(key)).intValue() & 0xFF);
              this.updatedSectionPos.put(sectionPos, Boolean.valueOf(true));
             continue;
           } 
            if (this.historyValueMap.get(sectionPos) == null) {
              byte[] bytes = new byte[1];
              bytes[0] = (byte)(((Integer)valueMap.get(key)).intValue() & 0xFF);
              Long updateTimeLong = (Long)timeValueMap.get(key);
             
              LinkedBlockingQueue<Pair<Long, byte[]>> bq = new LinkedBlockingQueue<Pair<Long, byte[]>>();
              bq.add(new Pair(updateTimeLong, bytes));
              this.historyValueMap.put(sectionPos, bq);
              continue;
           } 
            byte[] bytes = new byte[1];
            bytes[0] = (byte)(((Integer)valueMap.get(key)).intValue() & 0xFF);
            Long updateTimeLong = (Long)timeValueMap.get(key);
            ((LinkedBlockingQueue)this.historyValueMap.get(sectionPos)).add(new Pair(updateTimeLong, bytes));
         } 
       } 
     } 
   }
   ///电度总召命令68 0E 00 00 00 00 65 01 06 00 01 00 00 00 00 45
   private void sendDDRecruitmentCommand() {
      if (this.ddRecruitmentRetryCounts >= 3) {
       return;
     }
     
      if (!this.hasSentTestCommand) {
        List<Byte> recruitmentCommand = new ArrayList<Byte>();
        recruitmentCommand.add(Byte.valueOf((byte)104));
        recruitmentCommand.add(Byte.valueOf((byte)14));
        recruitmentCommand.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        recruitmentCommand.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        recruitmentCommand.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        recruitmentCommand.add(Byte.valueOf((byte)(this.recvNo >> 7)));
        recruitmentCommand.add(Byte.valueOf((byte)101));
        recruitmentCommand.add(Byte.valueOf((byte)1));
        recruitmentCommand.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          recruitmentCommand.add(Byte.valueOf((byte)0));
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)(this.ggdz & 0xFF)));
        if (this.ggdzLength == 2) {
          recruitmentCommand.add(Byte.valueOf((byte)0));
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)0));
        recruitmentCommand.add(Byte.valueOf((byte)0));
        if (this.xxtLength == 3) {
          recruitmentCommand.add(Byte.valueOf((byte)0));
       }
       
        recruitmentCommand.add(Byte.valueOf((byte)69));
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFF);
       
        byte[] sendBytes = new byte[recruitmentCommand.size()];
        for (int i = 0; i < recruitmentCommand.size(); i++) {
          sendBytes[i] = ((Byte)recruitmentCommand.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_DD_RECRUITMENT_FRAME, sendBytes));
     } 
     
      this.hasSentDDRecruitmentCommand = true;
      this.hasDataToSend = true;
      this.requireIFrameAck = true;
      this.lastIFrameAction = FrameType.IEC104_DD_RECRUITMENT_FRAME.value();
      this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
        {
          public void run() {
            try {
              BaseIEC104Device.this.reDoIFrameTask();
            } catch (Exception ex) {
              LogTools.log(logger, BaseIEC104Device.this.sn, LogType.ERROR, "Base business device sn = " + BaseIEC104Device.this.sn + " error occured when calling dd data.");
            } 
          }
        }, this.timeoutT1, TimeUnit.SECONDS);
   }

   private void handleDDRecruitmentAckFrame(Map<String, Object> parsedValueMap) {
      if (this.hasSentDDRecruitmentCommand) {
        this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
        this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
        sendS();
     } 
      this.ddRecruitmentRetryCounts = 0;
   }
   
   private void handleDDRecruitmentCompleteFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
     
      this.hasSentDDRecruitmentCommand = false;
      this.requireIFrameAck = false;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }

    private void handleDDIFrame(Map<String, Object> parsedValueMap) { commonHandle4BytesData(parsedValueMap, "DD"); }

   private void handleFaultIFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
   }
//发送通用S帧确认68 04 01 00
   private void sendS() {
      byte[] sendBytes = new byte[6];
      sendBytes[0] = 104;
      sendBytes[1] = 4;
      sendBytes[2] = 1;
      sendBytes[3] = 0;
      sendBytes[4] = (byte)((this.recvNo & 0x7F) << 1);///对接受序号组装低位在前高位在后，只取低7位，左移一位，最后一位置0相当于加2
      sendBytes[5] = (byte)(this.recvNo >> 7); ///对接受序号组装，右移7位，只取高
     
      this.outBytesBuffer.offer(new Pair(FrameType.IEC104_S_ACK_FRAME, sendBytes));///向队列中添加
     
      this.hasDataToSend = true;
   }

   private void sendTest() {
      byte[] sendBytes = new byte[6];
      sendBytes[0] = 104;
      sendBytes[1] = 4;
      sendBytes[2] = 67;
      sendBytes[3] = 0;
      sendBytes[4] = 0;
      sendBytes[5] = 0;
     
      this.outBytesBuffer.offer(new Pair(FrameType.IEC104_TEST_FRAME, sendBytes));
     
      this.hasSentTestCommand = true;
      this.hasDataToSend = true;
   }

   private void sendTestAck() {
      byte[] sendBytes = new byte[6];
      sendBytes[0] = 104;
      sendBytes[1] = 4;
      sendBytes[2] = -125;
      sendBytes[3] = 0;
      sendBytes[4] = 0;
      sendBytes[5] = 0;
     
      this.outBytesBuffer.offer(new Pair(FrameType.IEC104_TEST_ACK_FRAME, sendBytes));
     
      this.hasDataToSend = true;
   }
   //对时指令
   private void sendCorrectTimeCommand() {
      if (this.correctTimeRetryCounts >= 3) {
       return;
     }
     
      if (!this.hasSentTestCommand) {
        List<Byte> correctTimeBytes = new ArrayList<Byte>();
        correctTimeBytes.add(Byte.valueOf((byte)104));
        correctTimeBytes.add(Byte.valueOf((byte)20));
        correctTimeBytes.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        correctTimeBytes.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        correctTimeBytes.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        correctTimeBytes.add(Byte.valueOf((byte)(this.recvNo >> 7)));
        correctTimeBytes.add(Byte.valueOf((byte)103));
        correctTimeBytes.add(Byte.valueOf((byte)1));
        correctTimeBytes.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          correctTimeBytes.add(Byte.valueOf((byte)0));
       }
       
        correctTimeBytes.add(Byte.valueOf((byte)(this.ggdz & 0xFF)));
        if (this.ggdzLength == 2) {
          correctTimeBytes.add(Byte.valueOf((byte)0));
       }
       
        correctTimeBytes.add(Byte.valueOf((byte)0));
        correctTimeBytes.add(Byte.valueOf((byte)0));
        if (this.xxtLength == 3) {
          correctTimeBytes.add(Byte.valueOf((byte)0));
       }
       
        LocalDateTime now = LocalDateTime.now();
        correctTimeBytes.add(Byte.valueOf((byte)(now.getSecond() * 1000 & 0xFF)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getSecond() * 1000 >> 8)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getMinute() & 0xFF)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getHour() & 0xFF)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getDayOfMonth() & 0xFF)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getMonthValue() & 0xFF)));
        correctTimeBytes.add(Byte.valueOf((byte)(now.getYear() % 100 & 0xFF)));
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFFF);
       
        byte[] sendBytes = new byte[correctTimeBytes.size()];
        for (int i = 0; i < correctTimeBytes.size(); i++) {
          sendBytes[i] = ((Byte)correctTimeBytes.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_CORRECT_TIME_FRAME, sendBytes));
     } 
     
      this.hasSentCorrectTimeCommand = true;
      this.hasDataToSend = true;
      this.requireIFrameAck = true;
      this.lastIFrameAction = FrameType.IEC104_CORRECT_TIME_FRAME.value();
      this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
        {
          public void run() {
            try {
              BaseIEC104Device.this.reDoIFrameTask();
            } catch (Exception e) {
              LogTools.log(logger, BaseIEC104Device.this.sn, LogType.ERROR, "Base business device sn = " + BaseIEC104Device.this.sn + " error occured when correcting time.");
            } 
          }
        }, this.timeoutT1, TimeUnit.SECONDS);
   }

   private void handleCorrectTimeAckFrame(Map<String, Object> parsedValueMap) {
      this.hasSentCorrectTimeCommand = false;
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
      this.requireIFrameAck = false;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }

   private void sendYKSelect() {
      if (this.ykSelectRetryCounts >= 3)
      {
        this.ykSelectRetryCounts = 0;
        this.hasSentYKSelectCommand = false;
       return;
     } 
      if (!this.hasSentTestCommand)
      {
        List<Byte> ykSelectCommandBytes = new ArrayList<Byte>();
        ykSelectCommandBytes.add(Byte.valueOf((byte)104));
        ykSelectCommandBytes.add(Byte.valueOf((byte)14));
        ykSelectCommandBytes.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        ykSelectCommandBytes.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        ykSelectCommandBytes.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        ykSelectCommandBytes.add(Byte.valueOf((byte)(this.recvNo >> 7)));
       
        ykSelectCommandBytes.add(Byte.valueOf((byte)45));
        ykSelectCommandBytes.add(Byte.valueOf((byte)1));
       
        ykSelectCommandBytes.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          ykSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        ykSelectCommandBytes.add(Byte.valueOf((byte)1));
        if (this.ggdzLength == 2) {
          ykSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        int address = 24577 + this.ykAddress;
        ykSelectCommandBytes.add(Byte.valueOf((byte)(address & 0xFF)));
        ykSelectCommandBytes.add(Byte.valueOf((byte)(address >> 8 & 0xFF)));
        if (this.xxtLength == 3) {
          ykSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        ykSelectCommandBytes.add(Byte.valueOf((byte)((this.ykValue == 1) ? 129 : 128)));
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFFF);
       
        byte[] sendBytes = new byte[ykSelectCommandBytes.size()];
        for (int i = 0; i < ykSelectCommandBytes.size(); i++) {
          sendBytes[i] = ((Byte)ykSelectCommandBytes.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_YK_SELECT_FRAME, sendBytes));
       
        this.hasDataToSend = true;
        this.requireIFrameAck = true;
        this.hasSentYKSelectCommand = true;
        this.lastIFrameAction = FrameType.IEC104_YK_SELECT_FRAME.value();
        this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
          {
            public void run() {
              BaseIEC104Device.this.reDoIFrameTask();
            }
          }, this.timeoutT1, TimeUnit.SECONDS);
     } 
   }

   private void handleYKSelectAckFrame(Map<String, Object> parsedValueMap) {
      this.hasSentYKSelectCommand = false;
      this.requireIFrameAck = false;
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
      this.ykSelectRetryCounts = 0;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
     
      Boolean canExecute = (Boolean)parsedValueMap.get("can_execute");
      if (canExecute != null && canExecute.booleanValue()) {
        sendYKExecute();
     }
   }

   private void sendYKExecute() {
      if (this.ykExecuteRetryCounts >= 3) {
        this.ykExecuteRetryCounts = 0;
        this.hasSentYKExecuteCommand = false;
       
       return;
     } 
      if (!this.hasSentTestCommand) {
        List<Byte> ykExecuteCommandBytes = new ArrayList<Byte>();
        ykExecuteCommandBytes.add(Byte.valueOf((byte)104));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)14));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)(this.recvNo >> 7)));
       
        ykExecuteCommandBytes.add(Byte.valueOf((byte)45));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)1));
       
        ykExecuteCommandBytes.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          ykExecuteCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        ykExecuteCommandBytes.add(Byte.valueOf((byte)1));
        if (this.ggdzLength == 2) {
          ykExecuteCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        int address = 24577 + this.ykAddress;
        ykExecuteCommandBytes.add(Byte.valueOf((byte)(address & 0xFF)));
        ykExecuteCommandBytes.add(Byte.valueOf((byte)(address >> 8 & 0xFF)));
        if (this.xxtLength == 3) {
          ykExecuteCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        ykExecuteCommandBytes.add(Byte.valueOf((byte)((this.ykValue == 1) ? 1 : 0)));
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFFF);
       
        byte[] sendBytes = new byte[ykExecuteCommandBytes.size()];
        for (int i = 0; i < ykExecuteCommandBytes.size(); i++) {
          sendBytes[i] = ((Byte)ykExecuteCommandBytes.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_YK_EXECUTE_FRAME, sendBytes));
       
        this.hasDataToSend = true;
        this.hasSentYKExecuteCommand = true;
        this.lastIFrameAction = FrameType.IEC104_YK_EXECUTE_FRAME.value();
        this.requireIFrameAck = true;
        this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
          {
            public void run() {
              BaseIEC104Device.this.reDoIFrameTask();
            }
          },  this.timeoutT1, TimeUnit.SECONDS);
     } 
   }

   private void handleYKExecuteAckFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
      this.hasSentYKExecuteCommand = false;
      this.requireIFrameAck = false;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
      this.ykExecuteRetryCounts = 0;
     
      Boolean result = (Boolean)parsedValueMap.get("execute_result");
      if (result != null && result.booleanValue()) {
        LogTools.log(logger, this.sn, LogType.INFO, "Base 104 device sn = " + this.sn + " execute yk succeed.");
     } else {
        LogTools.log(logger, this.sn, LogType.INFO, "Base 104 device sn = " + this.sn + " execute yk failed. ");
     } 
   }
   
   private void handleYKExecuteCompleteFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      sendS();
   }

   private void sendSDCommand() {
      if (this.sdRetryCounts >= 3) {
        this.sdRetryCounts = 0;
       
       return;
     } 
      if (!this.hasSentTestCommand) {
        List<Byte> sdSelectCommandBytes = new ArrayList<Byte>();
        sdSelectCommandBytes.add(Byte.valueOf((byte)104));
        sdSelectCommandBytes.add(Byte.valueOf((byte)14));
        sdSelectCommandBytes.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        sdSelectCommandBytes.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        sdSelectCommandBytes.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        sdSelectCommandBytes.add(Byte.valueOf((byte)(this.recvNo >> 7)));
       
        sdSelectCommandBytes.add(Byte.valueOf((byte)50));
        sdSelectCommandBytes.add(Byte.valueOf((byte)1));
       
        sdSelectCommandBytes.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          sdSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        sdSelectCommandBytes.add(Byte.valueOf((byte)1));
        if (this.ggdz == 2) {
          sdSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        int address = 25089 + this.sdAddress;
        sdSelectCommandBytes.add(Byte.valueOf((byte)(address & 0xFF)));
        sdSelectCommandBytes.add(Byte.valueOf((byte)(address >> 8 & 0xFF)));
        if (this.xxtLength == 3) {
          sdSelectCommandBytes.add(Byte.valueOf((byte)0));
       }
       
        int floatInt = Float.floatToIntBits(this.sdValue);
        byte[] floatBytes = NumberUtil.intToByte4(floatInt);
        sdSelectCommandBytes.add(Byte.valueOf(floatBytes[0]));
        sdSelectCommandBytes.add(Byte.valueOf(floatBytes[1]));
        sdSelectCommandBytes.add(Byte.valueOf(floatBytes[2]));
        sdSelectCommandBytes.add(Byte.valueOf(floatBytes[3]));
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFFF);
       
        byte[] sendBytes = new byte[sdSelectCommandBytes.size()];
        for (int i = 0; i < sdSelectCommandBytes.size(); i++) {
          sendBytes[i] = ((Byte)sdSelectCommandBytes.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_SD_FRAME, sendBytes));
     } 
     
      this.hasDataToSend = true;
      this.requireIFrameAck = true;
      this.hasSentSDCommand = true;
      this.lastIFrameAction = FrameType.IEC104_SD_FRAME.value();
      this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
        {
          public void run() {
            BaseIEC104Device.this.reDoIFrameTask();
          }
        }, this.timeoutT1, TimeUnit.SECONDS);
   }

   private void handleSDAckFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
      this.hasSentSDCommand = false;
      this.requireIFrameAck = false;
      this.sdRetryCounts = 0;
   }

   private void handleTestAckFrame() {
      if (this.hasSentTestCommand) {
       
        this.hasSentTestCommand = false;
        this.testCount = 0;
     } 
   }

   private void handleTestFrame() { sendTestAck(); }
   //发送历史总召命令
   private void sendHistoryRecruitmentCommand() {
      if (this.historyCalledRetryCounts >= 3) {
        this.historyCalledRetryCounts = 0;
       return;
     } 
      if (!this.hasSentTestCommand) {
        List<Byte> historyRecruitmentCommand = new ArrayList<Byte>();
        historyRecruitmentCommand.add(Byte.valueOf((byte)104)); //68
        historyRecruitmentCommand.add(Byte.valueOf((byte)14)); //0E
        historyRecruitmentCommand.add(Byte.valueOf((byte)((this.sendNo & 0x7F) << 1)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(this.sendNo >> 7)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)((this.recvNo & 0x7F) << 1)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(this.recvNo >> 7)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)-118)); // FFFF FFFF FFFF FF8A
        historyRecruitmentCommand.add(Byte.valueOf((byte)1));
        historyRecruitmentCommand.add(Byte.valueOf((byte)6));
        if (this.csyyLength == 2) {
          historyRecruitmentCommand.add(Byte.valueOf((byte)0));
       }
       
        historyRecruitmentCommand.add(Byte.valueOf((byte)(this.ggdz & 0xFF)));
        if (this.ggdzLength == 2) {
          historyRecruitmentCommand.add(Byte.valueOf((byte)0));
       }
       
        historyRecruitmentCommand.add(Byte.valueOf((byte)0));
        historyRecruitmentCommand.add(Byte.valueOf((byte)0));
        if (this.xxtLength == 3) {
          historyRecruitmentCommand.add(Byte.valueOf((byte)0));
       }
 
       
        LocalDateTime startTime = LocalDateTime.ofInstant(this.historyCurrentQueryInstant, GlobalVariables.GLOBAL_DEFAULT_ZONEID);
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getSecond() * 1000 & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getSecond() * 1000 >> 8)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getMinute() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getHour() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getDayOfMonth() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getMonthValue() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(startTime.getYear() % 100 & 0xFF)));
 
       
        LocalDateTime endTime = startTime.plusMinutes(4L);
        endTime = endTime.plusSeconds(59L);
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getSecond() * 1000 & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getSecond() * 1000 >> 8)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getMinute() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getHour() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getDayOfMonth() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getMonthValue() & 0xFF)));
        historyRecruitmentCommand.add(Byte.valueOf((byte)(endTime.getYear() % 100 & 0xFF)));
       
        historyRecruitmentCommand.add(Byte.valueOf((byte)1));
        historyRecruitmentCommand.set(1, Byte.valueOf((byte)(historyRecruitmentCommand.size() - 2)));
       
        this.sendNo++;
        this.sendNo = (short)(this.sendNo & 0x7FFF);
       
        byte[] sendBytes = new byte[historyRecruitmentCommand.size()];
        for (int i = 0; i < historyRecruitmentCommand.size(); i++) {
          sendBytes[i] = ((Byte)historyRecruitmentCommand.get(i)).byteValue();
       }
       
        this.outBytesBuffer.offer(new Pair(FrameType.IEC104_HISTORY_RECRUITMENT_FRAME, sendBytes));
     } 
     
      this.hasSentHistoryCallCommand = true; /////
      this.hasDataToSend = true;
      this.requireIFrameAck = true;
      this.lastIFrameAction = FrameType.IEC104_HISTORY_RECRUITMENT_FRAME.value();
      this.iFrameTimeCounter = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.schedule(new Runnable()
        {
          public void run() {
            try {
              BaseIEC104Device.this.reDoIFrameTask();
            } catch (Exception ex) {
              LogTools.log(logger, BaseIEC104Device.this.sn, LogType.ERROR, "Base business device sn = " + BaseIEC104Device.this.sn + " error occured when calling history data.");
            } 
          }
        }, this.timeoutT1, TimeUnit.SECONDS);
   }

   private void handleHistoryRecruitmentAckFrame(Map<String, Object> parsedValueMap) {
      if (this.hasSentHistoryCallCommand) {
        this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
        this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
        sendS();
     } 
     
      this.historyCalledRetryCounts = 0;
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }
   
   private void handleHistoryRecruitmentCompleteFrame(Map<String, Object> parsedValueMap) {
      this.recvNo = ((Integer)parsedValueMap.get("recvNo")).intValue();
      this.sendNo = ((Integer)parsedValueMap.get("sendNo")).intValue();
      sendS();
 
     
      this.historyCurrentQueryInstant = this.historyCurrentQueryInstant.plusSeconds(300L);
      this.hasSentHistoryCallCommand = false;
      this.lastHistoryCalledTime = Instant.now();
      this.requireIFrameAck = false;
      if (this.historyCurrentQueryInstant.isAfter(this.historyEndInstant)) {
        this.needHistoryFlag = false;
        this.lastHistoryCalledTime = null;
     } 
   }

    private void handleHistoryYXFrame(Map<String, Object> parsedValueMap) { commonHandle1ByteData(parsedValueMap, "YX"); }

    private void handleHistoryYCFrame(Map<String, Object> parsedValueMap) { commonHandle4BytesData(parsedValueMap, "YC"); }

    private void handleHistoryDDFrame(Map<String, Object> parsedValueMap) { commonHandle4BytesData(parsedValueMap, "DD"); }

   private void reDoIFrameTask() {
      LogTools.log(logger, this.sn, LogType.INFO, "Base 104 device sn = " + this.sn + " i frame timeout, resend " + this.lastIFrameAction);
     
      if (this.lastIFrameAction.equals(FrameType.IEC104_RECRUITMENT_FRAME.value())) {
        this.requireIFrameAck = false;
        this.hasSentRecruitmentCommand = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     }
      else if (this.lastIFrameAction.equals(FrameType.IEC104_CORRECT_TIME_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.correctTimeRetryCounts++;
        sendCorrectTimeCommand();
      } else if (this.lastIFrameAction.equals(FrameType.IEC104_YK_SELECT_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.ykSelectRetryCounts++;
        sendYKSelect();
      } else if (this.lastIFrameAction.equals(FrameType.IEC104_YK_EXECUTE_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.ykExecuteRetryCounts++;
        sendYKExecute();
      } else if (this.lastIFrameAction.equals(FrameType.IEC104_SD_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.sdRetryCounts++;
        sendSDCommand();
      } else if (this.lastIFrameAction.equals(FrameType.IEC104_DD_RECRUITMENT_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.ddRecruitmentRetryCounts++;
        sendDDRecruitmentCommand();
      } else if (this.lastIFrameAction.equals(FrameType.IEC104_HISTORY_RECRUITMENT_FRAME.value())) {
        this.requireIFrameAck = false;
        this.lastIFrameAction = null;
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
        this.historyCalledRetryCounts++;
        sendHistoryRecruitmentCommand();
     } 
   }
   
   public void setProtocolStatusOnLine() {
      this.protocolStatus = StatusType.ONLINE;
      onConnected();
   }
   
   public void setProtocolStatusOffLine() {
      this.lastIFrameAction = null;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
     
      this.protocolStatus = StatusType.OFFLINE;
   }

   public void delete() {
      LogTools.log(logger, this.sn, LogType.DEBUG, "Base 104 device sn = " + this.sn + " sub delete is called.");
      super.delete();
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }

   public void onConnected() {
      Instant currentInstant = Instant.now();
      this.hasSentConnectCommand = false;
      this.hasSentTestCommand = false;
      this.hasSentYKSelectCommand = false;
      this.hasSentYKExecuteCommand = false;
      this.hasSentCorrectTimeCommand = false;
      this.hasSentHistoryCallCommand = false;
      this.lastZzTime = currentInstant.minusSeconds(2000L);
      this.lastCorrectTimeTime = currentInstant.minusSeconds(2000L);
      this.lastDDZZTime = currentInstant.minusSeconds(2000L);
      this.hasSentRecruitmentCommand = false;
      this.hasSentDDRecruitmentCommand = false;
      this.hasSentSDCommand = false;
      this.requireIFrameAck = false;
      this.isCircuitInitialized = false;
      this.recvNo = 0;
      this.sendNo = 0;
      this.ddRecruitmentRetryCounts = 0;
      this.ykSelectRetryCounts = 0;
      this.ykExecuteRetryCounts = 0;
      this.sdRetryCounts = 0;
      this.testCount = 0;
      this.correctTimeRetryCounts = 0;
      this.historyCalledRetryCounts = 0;
      if (this.iFrameTimeCounter != null) {
        this.iFrameTimeCounter.cancel(true);
        this.iFrameTimeCounter = null;
     } 
   }

   public void onDisconnected() {
      setProtocolStatusOffLine();
      this.lastConnectTime = Instant.now().minusSeconds(1800L);
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
    //得到数据区 值字节 状态量
   public Byte getSectionByte(String sectionName, int pos) {
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
     }
     
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos) {
        return null;
     }
     
      String key = (String)this.dataSectionNameTypeMap.get(sectionName) + pos; //YC120
      if (!this.updatedSectionPos.containsKey(key) || !((Boolean)this.updatedSectionPos.get(key)).booleanValue()) {
        return null;
     }
     
      this.updatedSectionPos.put(key, Boolean.valueOf(false));
//      return Byte.valueOf((byte[])((Quartet)this.dataSectionMap.get(sectionName)).getValue3()[pos]);
      return Byte.valueOf(this.dataSectionMap.get(sectionName).getValue3()[pos]);  //[]   dataSectionMap-(LXBE-<100,4,YC,[]>)
   }
     //得到数据区 值字节 模拟量
   public byte[] getSectionBytes(String sectionName, int pos, int length) {
         if (!this.dataSectionNames.contains(sectionName)) {
             return null;
         }

         if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos + length)
         {
             return null;
         }

         String key = (String)this.dataSectionNameTypeMap.get(sectionName) + pos;
         if (!this.updatedSectionPos.containsKey(key) ||
                 !((Boolean)this.updatedSectionPos.get(key)).booleanValue()) {
             return null;
         }

         byte[] bytes = new byte[length];
         System.arraycopy(((Quartet)this.dataSectionMap.get(sectionName)).getValue3(), pos, bytes, 0, length); //dataSectionMap-(LXBE-<100,4,YC,[]>)
         this.updatedSectionPos.put(key, Boolean.valueOf(false));
         return bytes;
     }
    //得到历史值队列 状态量
   public Pair<Long, Byte> getHistorySectionByte(String sectionName, int pos) { //（YC 100）
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
     }
     
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos) {
        return null;
     }
     
      String key = (String)this.dataSectionNameTypeMap.get(sectionName) + pos; //YC120
      if (this.historyValueMap.get(key) == null || ((LinkedBlockingQueue)this.historyValueMap.get(key)).isEmpty()) {
        return null;
     }
     
      Pair<Long, byte[]> record = (Pair)((LinkedBlockingQueue)this.historyValueMap.get(key)).poll(); //出队列
      return new Pair<Long, Byte>(record.getValue0(), Byte.valueOf(record.getValue1()[0]));
   }
    //得到历史值队列 模拟量
   public Pair<Long, byte[]> getHistorySectionBytes(String sectionName, int pos, int length) { //根据 (YC ,地址，长度) 得到 （时间，值）
      if (!this.dataSectionNames.contains(sectionName)) {
        return null;
     }
     
      if (((Integer)((Quartet)this.dataSectionMap.get(sectionName)).getValue1()).intValue() < pos) {
        return null;
     }
     
      String key = (String)this.dataSectionNameTypeMap.get(sectionName) + pos; //YC120 根据参数得到key 类型+地址
      if (this.historyValueMap.get(key) == null || ((LinkedBlockingQueue)this.historyValueMap.get(key)).isEmpty()) {
        return null;
     }
     
      return (Pair)((LinkedBlockingQueue)this.historyValueMap.get(key)).poll(); //根据key得到 存起来的值队列历史值 出队列
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\business\BaseIEC104Device.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */