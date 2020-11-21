  package  com.kkwl.collector.channels.kafka;
  
  import com.kkwl.collector.channels.kafka.MessageConsumer;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.devices.communication.ClientDTU;
  import com.kkwl.collector.models.response.LogViewResponse;
  import com.kkwl.collector.utils.LogTools;
  import com.kkwl.collector.utils.ZLibUtils;
  import java.util.Base64;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.kafka.annotation.KafkaListener;
  import org.springframework.stereotype.Component;
  
  @Component
  @ConditionalOnProperty(name = {"com.kkwl.collector.runmode"}, havingValue = "kafka_client")
  public class MessageConsumer {
    private static Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    
    @Value("${spring.kafka.consumer.custom-business}")
    private String business;
    
    @KafkaListener(topics = {"${spring.kafka.consumer.custom-topic}"})
    public void processMessage(String content) {
      LogTools.log(logger, "KAFKA_CONSUMER", LogType.INFO, "Kafka client received message " + content + ", send to analyser = " + this.business);
      switch (this.business) {
        case "hgny":
          analyzeHgnyMessage(content);
          return;
      } 
      LogTools.log(logger, "KAFKA_CONSUMER", LogType.ERROR, "Kafka client invalid business name = " + this.business);
    }
  
  
    
    private void analyzeHgnyMessage(String message) {
      if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null && GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU.equals("all")) {
        LogViewResponse response = new LogViewResponse();
        response.setDeviceSn("KAFKA");
        response.setAction("RECV");
        response.setContent(message);
        GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.add(response);  ///
      } 
      
      try {
        JSONObject jsonObject = new JSONObject(message);
        
        if (jsonObject.has("payload")) {
          String base64Message = jsonObject.getString("payload");
          
          byte[] zlibedMessageBytes = Base64.getDecoder().decode(base64Message);
          
          byte[] decompressedBytes = ZLibUtils.decompress(zlibedMessageBytes);
          
          String content = new String(decompressedBytes);
          
          JSONObject payLoad = new JSONObject(content);
          
          LogTools.log(logger, "KAFKA_CONSUMER", LogType.INFO, "HGNY message analyser need to parse message = " + content);
          if (!payLoad.has("td_mctd")) {
            LogTools.log(logger, "KAFKA_CONSUMER", LogType.WARN, "HGNY message analyser can't find td_mctd");
            
            return;
          } 
          String tdMctd = payLoad.getString("td_mctd");
          
          if (!payLoad.has("meter_sn")) {
            LogTools.log(logger, "KAFKA_CONSUMER", LogType.WARN, "HGNY message analyser can't find meter_sn");
            
            return;
          } 
          String meterSn = payLoad.getString("meter_sn");
          
          boolean messageHandled = false;
          for (ClientDTU clientDTU : GlobalVariables.GLOBAL_CLIENT_DTUS) {
            if (clientDTU.getSn().equals(tdMctd)) {
              clientDTU.handleReceivedData(meterSn, content);
              messageHandled = true;
            } 
          } 
          
          if (!messageHandled) {
            LogTools.log(logger, "KAFKA_CONSUMER", LogType.WARN, "HGNY message analyser can't find target dtu.");
          }
        } 
      } catch (Exception ex) {
        LogTools.log(logger, "KAFKA_CONSUMER", LogType.ERROR, "Kafka client callback error occured when parse message = " + message, ex);
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\kafka\MessageConsumer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */