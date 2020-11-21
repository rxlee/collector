  package  com.kkwl.collector.services;
  
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.dao.Configuration;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.services.DeviceVariableValueChangedNotificationReceiver;
  import java.util.concurrent.CountDownLatch;
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.amqp.rabbit.annotation.RabbitListener;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.messaging.handler.annotation.Payload;
  import org.springframework.stereotype.Component;
  
  @Component
  public class DeviceVariableValueChangedNotificationReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DeviceVariableValueChangedNotificationReceiver.class);
    
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @Autowired
    Configuration configurationDBHandler;
    
    @RabbitListener(queues = {"#{deviceVariableValueChangedNotificationQueue.name}"})
    public void receiveMessage(@Payload String message) {
      logger.info("Device variable value changed message receiver received : " + message);
      
      JSONObject msg = null;
      try {
        msg = new JSONObject(message);
      } catch (JSONException ex) {
        logger.error("Device variable value changed message receiver error occured when parsing json string.", ex);
        
        return;
      } 
      if (!msg.has("type")) {
        logger.error("Device variable value changed message receiver message doesn't have key 'type'");
        
        return;
      } 
      if (!msg.has("detail")) {
        logger.error("Device variable value changed message receiver message doesn't have key 'detail'");
        
        return;
      } 
      String type = msg.getString("type");
      JSONArray details = msg.getJSONArray("detail");
      
      for (int i = 0; i < details.length(); i++) {
        
        JSONObject detail = details.getJSONObject(i);
        
        if (!detail.has("varsn") || !detail.has("value")) {
          logger.error("Device variable value changed message receiver message detail info is invalid: ", detail.toString());
          
          return;
        } 
        String deviceVariableSn = detail.getString("varsn");
        String deviceVariableValue = detail.getString("value");
  
        
        DeviceVariable deviceVariable = GlobalVariables.getDeviceVariableBySn(deviceVariableSn);
        if (deviceVariable == null) {
          logger.error("Device variable value changed message receiver can't find device variable sn = " + deviceVariableSn);
        } else {
          
          BaseBusinessDevice device = null;
          for (BaseBusinessDevice tmpDevice : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
            if (tmpDevice.getSn().equals(deviceVariable.getCollectorDeviceSn())) {
              device = tmpDevice;
            }
          } 
          if (device == null) {
            logger.warn("Device variable value changed message receiver device variable sn = " + deviceVariableSn + " isn't in this collector.");
  
            
            return;
          } 
          
          device.handleValueChangedMessage(deviceVariableSn, deviceVariableValue, type);
        } 
      } 
      this.latch.countDown();
    }
  
    
    public CountDownLatch getLatch() { return this.latch; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\DeviceVariableValueChangedNotificationReceiver.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */