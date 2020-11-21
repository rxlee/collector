  package  com.kkwl.collector.init;
  import com.kkwl.collector.channels.mqtt.MqttMonitorClient;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.init.MqttComponent;
  import com.kkwl.collector.services.DataLoadService;
  import org.eclipse.paho.client.mqttv3.MqttClient;
  import org.eclipse.paho.client.mqttv3.MqttException;
  import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.CommandLineRunner;
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.core.annotation.Order;
  import org.springframework.stereotype.Component;
  
  @Component
  @ConditionalOnProperty(name = {"com.kkwl.collector.runmode"}, havingValue = "mqtt_client")
  @Order(3)
  public class MqttComponent implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MqttComponent.class);
    
    @Autowired
    private MqttMonitorClient mqttMonitorClient;
    
    @Autowired
    private DataLoadService dataLoadService;
    
    @Value("${com.kkwl.collector.mqtt.server}")
    private String mqttServerUrl;
    
    @Value("${com.kkwl.collector.mqtt.client_id}")
    private String clientId;
  
    
    public void run(String... args) throws Exception {
      logger.info("Global configuration begin to initialize mqtt component.");
      
      try {
        this.dataLoadService.loadData("MQTT");  //加载通道和采集设备
        
        GlobalVariables.MQTT_MONITOR_CLIENT_STATUS_LISTENER = null;
  
        
        GlobalVariables.MQTT_CLIENT = new MqttClient(this.mqttServerUrl, this.clientId, new MemoryPersistence()); //****新建全局MqttClient*****
      } catch (MqttException e) {
        logger.error("Global configuration mqtt monitor client error occured when creating mqtt client.");
      } 
      
      this.mqttMonitorClient.connect(); //*********===>监控客户端 进行连接****
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\init\MqttComponent.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */