  package  com.kkwl.collector.services;
  
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.dao.Configuration;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.models.EventTrigger;
  import com.kkwl.collector.services.AlarmReportService;
  import com.kkwl.collector.utils.LogTools;
  import com.kkwl.collector.utils.TimeFormatter;
  import java.sql.Timestamp;
  import java.util.ArrayList;
  import java.util.List;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
//  import org.springframework.amqp.core.AmqpTemplate;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Component;
  

  @Component
  public class AlarmReportService
  {
    private static final Logger logger = LoggerFactory.getLogger(AlarmReportService.class);
    
    @Value("${com.kkwl.collector.amqp.alarm_exchange_name}")
    private String alarmExchangeName;
    
    @Value("${com.kkwl.collector.amqp.alarm_topic}")
    private String alarmTopicName;
    
//    @Autowired
//    AmqpTemplate amqpTemplate;
    
    @Autowired
    Configuration configurationDB;
    
    private static String logFileName = "ALARM";
  
  
    
    public void sendAlarmMessage(String reportId, String recoverReportId, int autoCheckRecoverReport, String parentIndex, String deviceSn, String deviceName, int status, String varSn, String varName, String varValue, String reportTime, String closedTime, EventTrigger eventTrigger) {
      JSONObject alarmMessageObj = new JSONObject();
      alarmMessageObj.put("report_id", reportId);
      alarmMessageObj.put("recover_report_id", recoverReportId);
      alarmMessageObj.put("auto_check_recover_report", autoCheckRecoverReport);
      alarmMessageObj.put("var_sn", varSn);
      alarmMessageObj.put("var_name", varName);
      alarmMessageObj.put("var_value", varValue);
      alarmMessageObj.put("device_sn", deviceSn);
      alarmMessageObj.put("device_name", deviceName);
      alarmMessageObj.put("parent_index", parentIndex);
      alarmMessageObj.put("type_sn", eventTrigger.getTypeSn());
      alarmMessageObj.put("type_parent_index", eventTrigger.getTypeParentIndex());
      alarmMessageObj.put("status", status);
      alarmMessageObj.put("report_time", reportTime);
      alarmMessageObj.put("closed_time", closedTime);
      alarmMessageObj.put("trigger_id", eventTrigger.getId());
      alarmMessageObj.put("param_1", eventTrigger.getParam1());
      alarmMessageObj.put("param_2", eventTrigger.getParam2());
      alarmMessageObj.put("param_3", eventTrigger.getParam3());
      alarmMessageObj.put("param_4", eventTrigger.getParam4());
      
      LogTools.log(logger, logFileName, LogType.INFO, "Alarm report service send alarm : " + alarmMessageObj.toString());
//      this.amqpTemplate.convertAndSend(this.alarmExchangeName, this.alarmTopicName, alarmMessageObj.toString());
    }
  
    
    public void handleDeviceStatusAlarm(String sn, String name, String type, byte status, String reportId, String recoverReportId, Timestamp msgTime) {
      List<String> parentIndexs = new ArrayList<String>();
      
      EventTrigger eventTrigger = new EventTrigger();
      
      if (type.equals("DTU")) {
        eventTrigger.setTypeSn("dtu_offline");
        List<String> collectorDeviceSns = new ArrayList<String>();
        for (BaseBusinessDevice businessDevice : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
          if (businessDevice.getDtuSn().equals(sn)) {
            collectorDeviceSns.add(businessDevice.getSn());
          }
        } 
        
        if (!collectorDeviceSns.isEmpty()) {
          parentIndexs = this.configurationDB.getDeviceStationSns(collectorDeviceSns);
        }
      } else {
        eventTrigger.setTypeSn("c_device_offline");
        List<String> collectorDeviceSns = new ArrayList<String>();
        collectorDeviceSns.add(sn);
        
        parentIndexs = this.configurationDB.getDeviceStationSns(collectorDeviceSns);
      } 
      
      for (String parentIndex : parentIndexs)
        sendAlarmMessage(reportId, recoverReportId, 0, parentIndex, sn, name, status, "", "", "", 
            TimeFormatter.timestampToCommonString(msgTime), (status == 0) ? TimeFormatter.timestampToCommonString(msgTime) : null, eventTrigger); 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\AlarmReportService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */