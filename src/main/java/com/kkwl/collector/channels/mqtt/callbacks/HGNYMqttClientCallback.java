  package  com.kkwl.collector.channels.mqtt.callbacks;
  
  import com.kkwl.collector.channels.mqtt.callbacks.HGNYMqttClientCallback;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.devices.communication.ClientDTU;
  import com.kkwl.collector.models.response.LogViewResponse;
  import com.kkwl.collector.utils.LogTools;
  import com.kkwl.collector.utils.ZLibUtils;
  import java.nio.charset.StandardCharsets;
  import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
  import org.eclipse.paho.client.mqttv3.MqttCallback;
  import org.eclipse.paho.client.mqttv3.MqttMessage;
  import org.json.JSONException;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  public class HGNYMqttClientCallback implements MqttCallback {
    private static Logger logger = LoggerFactory.getLogger(HGNYMqttClientCallback.class);
  
  
    
    public void connectionLost(Throwable throwable) { LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.WARN, "HGNY mqtt client callback lost connection, because of :", throwable); }
  
  
    //收到消息 (主题topic，收到的内容)
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception { //消息收到
      byte[] decompressedBytes = null;
      
      if (GlobalVariables.MESSAGE_COMPRESS_TYPE.equals("zlib")) {
        try {
          byte[] receivedBytes = mqttMessage.getPayload();  //*************收到的字节**************
          decompressedBytes = ZLibUtils.decompress(receivedBytes); //入参为压缩字节或压缩输入流，返回解压后的字节
        } catch (Exception ex) {
          LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.WARN, "HGNY mqtt client callback decompress bytes error ", ex);
        } 
      } else {
        decompressedBytes = mqttMessage.getPayload(); //
      } 
      
      if (decompressedBytes == null) {
        return;
      }
      
      String payLoadJsonString = new String(decompressedBytes, StandardCharsets.US_ASCII); //************把解压缩后的字节转为 载荷字符串
      LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.INFO, "HGNY mqtt client callback received from topic = " + s + " content = " + payLoadJsonString
          .replaceAll("\r", "").replaceAll("\n", "").replaceAll(" ", "")
          .replaceAll("\t", "").trim());
      
      if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null && GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU.equals("all")) {
        LogViewResponse response = new LogViewResponse();
        response.setDeviceSn("MQTT");
        response.setAction("RECV");
        response.setContent(payLoadJsonString);
        GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.add(response); //*****供码流使用*******
      } 
      
      if (payLoadJsonString.indexOf("closed") > 0 && payLoadJsonString.indexOf("topic") == 0) {//如果以“topic”开头并且含有"closed"
        LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.INFO, "HGNY mqtt client callback received " + payLoadJsonString);
        
        return;
      } 
      try {
        JSONObject payLoad = new JSONObject(payLoadJsonString); //******** 把载荷字符串转为 负载JSONObject******
        
        if (!payLoad.has("td_mctd")) {
          LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.WARN, "HGNY mqtt client callback can't find td_mctd");
          
          return;
        } 
        String tdMctd = payLoad.getString("td_mctd"); //通道名称
        
        if (!payLoad.has("meter_sn")) {
          LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.WARN, "HGNY mqtt client callback can't find meter_sn");
          
          return;
        } 
        String meterSn = payLoad.getString("meter_sn"); //仪表sn
        
        for (ClientDTU clientDTU : GlobalVariables.GLOBAL_CLIENT_DTUS) {
          if (clientDTU.getSn().equals(tdMctd)) {
            clientDTU.handleReceivedData(meterSn, payLoadJsonString); //******处理收到的payLoadJsonString字符串****
          }
        } 
      } catch (JSONException ex) {
        LogTools.log(logger, "HGNY_MQTT_CALLBACK", LogType.ERROR, "HGNY mqtt client callback error occured when parse message = " + payLoadJsonString, ex);
      } 
    }
    
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\mqtt\callbacks\HGNYMqttClientCallback.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */