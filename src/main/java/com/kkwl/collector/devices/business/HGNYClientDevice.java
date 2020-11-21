  package  com.kkwl.collector.devices.business;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.HandlingType;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.common.StatusType;
  import com.kkwl.collector.devices.business.BaseClientDevice;
  import com.kkwl.collector.devices.business.HGNYClientDevice;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan.HGNYMqttProtocolAdapter;
  import com.kkwl.collector.utils.LogTools;
  import java.sql.Timestamp;
  import java.util.HashMap;
  import java.util.Iterator;
  import java.util.Map;
  import org.json.JSONException;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  public class HGNYClientDevice extends BaseClientDevice {
    private static final Logger logger = LoggerFactory.getLogger(HGNYClientDevice.class);
    
    private Double tc;
    
    //private HGNYClientDevice
    public HGNYClientDevice(String sn, String dtuSn, String name, String parentIndex, String type, Byte deviceBeingUsed, Long handlingPeriod, byte shouldReportAlarm, short connectionTimeoutDuration, String protocolParams, String offlineReportId, Timestamp offlineTime, StatusType initStatus) {
      super(sn, dtuSn, name, parentIndex, type, deviceBeingUsed, handlingPeriod, shouldReportAlarm, connectionTimeoutDuration, offlineReportId, offlineTime, initStatus);
      this.protocolParams = protocolParams;
      this.protocol = new HGNYMqttProtocolAdapter();
    }
    //解析协议变量，协议的基本参数
    public void parseProtocolVariable() {
      try {
        JSONObject protocolVariableJSON = new JSONObject(this.protocolParams);
        this.tc = Double.valueOf(protocolVariableJSON.getDouble("tc"));
      } catch (JSONException e) {
        LogTools.log(logger, this.sn, LogType.WARN, "HGNY mqtt device sn = " + this.sn + " error occured when parse protocol variables = " + this.protocolParams);
      } 
    }

    //解析
    public void doParse() {
      if (this.protocol == null) {
        return;
      }
      
      String payLoadJsonString = null;
      if (this.inStringsBuffer.isEmpty()) {
        return;
      }
  
      
      Map<String, Object> parsedValueMap = null;
      String deviceSn = "";
      Map<String, Object> params = new HashMap<String, Object>();
      Iterator<DeviceVariable> deviceVariableIterator = this.deviceVariableMap.values().iterator();
      if (deviceVariableIterator.hasNext()) {
        DeviceVariable firstDeviceVariable = (DeviceVariable)deviceVariableIterator.next(); //
        deviceSn = firstDeviceVariable.getDeviceSn().split("_")[0]; //实际得到的是站点sn   getDeviceSn()为SZ001_LXBE201IN__Ub
      } 
      
      params.put("sn", deviceSn); //实际得到的是站点sn
      params.put("tc", this.tc); //????
      LogTools.log(logger, this.sn, LogType.DEBUG, "HGNY mqtt device variable sn is " + deviceSn);
      
      parsedValueMap = this.protocol.doStringParse(this.inStringsBuffer, params);//************消息队列(字符串)解析  （内部解析，返回值的map）***************
      
      if (parsedValueMap != null && ((Boolean)parsedValueMap.get("result")).booleanValue()) {
        FrameType frameType = (FrameType)parsedValueMap.get("type"); //帧类型
        if (frameType == null) {
          LogTools.log(logger, this.sn, LogType.WARN, "HGNY mqtt device sn = " + this.sn + " can't find frame type");
          
          return;
        } 
        if (frameType == FrameType.MQTT_HGNY_METER_FRAME) { //帧类型为 仪表帧
          handleMqttDoubleValue(parsedValueMap); //*******处理mqtt双精度值******
        }
      } 
    }
    
    private void handleMqttDoubleValue(Map<String, Object> parsedValueMap) {
      Map<String, Float> commonValueMap = (Map)parsedValueMap.get("commonValueMap");
      Timestamp recordTime = (Timestamp)parsedValueMap.get("time");
      for (String key : commonValueMap.keySet()) {
        Float value = (Float)commonValueMap.get(key);
        if (value == null) {
          LogTools.log(logger, this.sn, LogType.WARN, "HGNY mqtt device sn = " + this.sn + " variable sn = " + key + " value is null");
          continue;
        } 
        DeviceVariable deviceVariable = null;
        deviceVariableLock.lock();
        try {
          if (this.deviceVariableMap.keySet().contains(key)) {
            deviceVariable = (DeviceVariable)this.deviceVariableMap.get(key);
          } else {
            LogTools.log(logger, this.sn, LogType.WARN, "HGNY mqtt device sn = " + this.sn + " can't find device variable sn = " + key);
          } 
        } finally {
          deviceVariableLock.unlock();
        }
        if (deviceVariable == null) {
          continue;
        }
        if (deviceVariable.getCoefficient() != null) {
          value = Float.valueOf(value.floatValue() * deviceVariable.getCoefficient().floatValue());
        }
        deviceVariable.setSpecialHandlingType(HandlingType.DO_NOT_UPDATE_TIME);
      }
      DeviceVariable deviceVariable = null;
      Float value = null;
      Timestamp mdpUpdateTime = null;
      Map<String, Object> specialValueMap = (Map)parsedValueMap.get("specialValueMap");
      if (specialValueMap.containsKey("mdp")) {
        deviceVariableLock.lock();
        try {
          String deviceVariableSn = this.sn + "__mdp"; //*****组装变量sn   this.sn为设备sn MNTWK_SZMN_16
          if (this.deviceVariableMap.keySet().contains(deviceVariableSn)) {
            deviceVariable = (DeviceVariable)this.deviceVariableMap.get(deviceVariableSn); //由变量sn得到变量
            value = (Float)specialValueMap.get("mdp");
            mdpUpdateTime = (Timestamp)specialValueMap.get("mdp_time");
            deviceVariable.setSpecialHandlingType(HandlingType.DO_NOT_UPDATE_TIME);
          } 
        } finally {
          deviceVariableLock.unlock();
        } 
      }
      if (deviceVariable != null);
      deviceVariable = null;
      value = null;
      Timestamp mdkwhUpdateTime = null;
      if (specialValueMap.containsKey("mdkwh")) {
        deviceVariableLock.lock();
        try {
          String deviceVariableSn = this.sn + "__mdkwh";
          if (this.deviceVariableMap.keySet().contains(deviceVariableSn)) {
            deviceVariable = (DeviceVariable)this.deviceVariableMap.get(deviceVariableSn);
            value = (Float)specialValueMap.get("mdkwh");
            mdkwhUpdateTime = (Timestamp)specialValueMap.get("mdkwh_time");
            deviceVariable.setSpecialHandlingType(HandlingType.DO_NOT_UPDATE_TIME);
          } 
        } finally {
          deviceVariableLock.unlock();
        } 
      }
      if (deviceVariable != null);
    }
    private void handleMqttIntegerValue(Map<String, Object> parsedValueMap) { //处理mqtt整型数据
      Map<String, Integer> valueMap = (Map)parsedValueMap.get("valueMap");
      for (String key : valueMap.keySet()) {
        Integer value = (Integer)valueMap.get(key);
        if (this.deviceVariableMap.keySet().contains(key));
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\business\HGNYClientDevice.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */