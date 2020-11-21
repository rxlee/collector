  package  com.kkwl.collector.channels.mqtt;
  
  import com.kkwl.collector.channels.mqtt.MqttMonitorClient;
  import com.kkwl.collector.channels.mqtt.callbacks.HGNYMqttClientCallback;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.devices.communication.ClientDTU;
  import com.kkwl.collector.utils.SslUtil;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.concurrent.TimeUnit;
  import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
  import org.eclipse.paho.client.mqttv3.MqttException;
  import org.eclipse.paho.client.mqttv3.MqttTopic;
  import org.javatuples.Pair;
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Component;
  
  @Component
  public class MqttMonitorClient { //监控客户端
    private static final Logger logger = LoggerFactory.getLogger(MqttMonitorClient.class);
    
    @Value("${com.kkwl.collector.mqtt.clean_session}")
    private Byte ifCleanSession;
    //队列属性
    @Value("${com.kkwl.collector.mqtt.queues}")
    private String queues;
    //回调
    @Value("${com.kkwl.collector.mqtt.callback}")
    private String callBackComponent;
    //保持激活时间
    @Value("${com.kkwl.collector.mqtt.keep_alive}")
    private Integer keepAliveTime;
    
    @Value("${com.kkwl.collector.mqtt.use_ssl}")
    private Byte useSSL;
    
    @Value("${com.kkwl.collector.mqtt.ca_file}")
    private String caFile;
    
    @Value("${com.kkwl.collector.mqtt.cert_file}")
    private String certFile;
    
    @Value("${com.kkwl.collector.mqtt.key_file}")
    private String keyFile;
    
    @Value("${com.kkwl.collector.mqtt.need_auth}")
    private Byte needAuth;
    
    @Value("${com.kkwl.collector.mqtt.username}")
    private String username;
    
    @Value("${com.kkwl.collector.mqtt.password}")
    private String password;
    //连接
    public void connect() {
      try {
        List<String> dtuSns;
        GlobalVariables.MQTT_OPTIONS = new MqttConnectOptions(); //*******000****实例化 连接属性*****
        //清除session
        GlobalVariables.MQTT_OPTIONS.setCleanSession(true);
        //判断是否需要认证
        if (this.needAuth.byteValue() == 1) {
          //mqtt用户名
          GlobalVariables.MQTT_OPTIONS.setUserName(this.username);
          //mqtt密码
          GlobalVariables.MQTT_OPTIONS.setPassword(this.password.toCharArray());
        } 
  
        //设置连接超时时间
        GlobalVariables.MQTT_OPTIONS.setConnectionTimeout(10);
        //设置连接间隔
        GlobalVariables.MQTT_OPTIONS.setKeepAliveInterval(this.keepAliveTime.intValue());
  
        //是否使用秘钥
        if (this.useSSL.byteValue() == 1) {
          GlobalVariables.MQTT_OPTIONS.setSocketFactory(SslUtil.getSocketFactory(this.caFile, this.certFile, this.keyFile, this.password));
        }
        
        Pair<String[], int[]> topics = null;
        switch (this.callBackComponent) {
          case "HGNYMqttClientCallback":
            GlobalVariables.MQTT_CLIENT.setCallback(new HGNYMqttClientCallback());//******111******mqttClient设置回调******
            
            dtuSns = new ArrayList<String>();
            for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) { //遍历CLIENT_DTUS
              dtuSns.add(dtu.getSn()); //等于采集设备sn
            }
            topics = createHGNYTopics(dtuSns); //*****222**给每个采集设备创建主题*********   return Pair(queues, qoses)
            break;
          
          default:
            logger.error("Mqtt monitor client invalid callback configuration");
            break;
        } 
//        if (topics != null && (String[])topics.getValue0().length != 0) {
        if (topics != null && topics.getValue0().length != 0) {
          for (String topicName : (String[])topics.getValue0()) {
            MqttTopic topic = GlobalVariables.MQTT_CLIENT.getTopic(topicName);
            
            GlobalVariables.MQTT_OPTIONS.setWill(topic, ("topic " + topicName + " closed").getBytes(), 2, true);
          } 
        }
        
        GlobalVariables.MQTT_CLIENT.connect(GlobalVariables.MQTT_OPTIONS); //******333*******mqttClient===>连接*************
        
//        if (topics == null || (String[])topics.getValue0().length == 0) { ////无可订阅的主题
        if (topics != null && topics.getValue0().length != 0) {
          logger.warn("Mqtt monitor client no topics to subscribe.");
        } else {
          GlobalVariables.MQTT_CLIENT.subscribe((String[])topics.getValue0(), (int[])topics.getValue1()); //******444****mqttClient===>订阅*************
        } 
      } catch (Exception e) {
        logger.error("Mqtt monitor client error occured when creating mqtt client.", e);
      } 
      
      if (GlobalVariables.MQTT_MONITOR_CLIENT_STATUS_LISTENER == null) {
        try {
          GlobalVariables.MQTT_MONITOR_CLIENT_STATUS_LISTENER = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable()
            {
              public void run()
              {
                if (!GlobalVariables.MQTT_CLIENT.isConnected()) //******如果没有连接
                {
                  MqttMonitorClient.this.connect(); //固定周期连接
                }
              }
            },  0L, this.keepAliveTime.intValue(), TimeUnit.SECONDS);
        }
        catch (Exception ex) {
          logger.error("Mqtt monitor client error occured in monitor schedule.", ex);
        } 
      }
    }
    
    public void disconnect() {
      try {
        GlobalVariables.MQTT_MONITOR_CLIENT_STATUS_LISTENER.cancel(true);
        GlobalVariables.MQTT_MONITOR_CLIENT_STATUS_LISTENER = null;
        GlobalVariables.MQTT_CLIENT.disconnect();
      } catch (MqttException ex) {
        logger.error("Mqtt monitor client error occured when disconnect from server.", ex);
      } catch (Exception ex) {
        logger.error("Mqtt monitor client error occured when disconnect from server.", ex);
      } 
    }
    //创建主题
    public Pair<String[], int[]> createTopics(List<String> queueNameParts) {
      if (queueNameParts.isEmpty()) {
        return null;
      }
      
      Pair<String[], int[]> topics = null;
      switch (this.callBackComponent) {
        case "HGNYMqttClientCallback":
          return createHGNYTopics(queueNameParts);
      } 
      
      return null;
    }
    //创建主题
    private Pair<String[], int[]> createHGNYTopics(List<String> queueNameParts) { //给每个采集设备建立主题 （采集设备sn的list）
      List<String> subscribeTopics = new ArrayList<String>(); //订阅的主题
      List<Integer> subscribeQoses = new ArrayList<Integer>(); //质量因素
      
      try {
        JSONObject queuesCOnfig = new JSONObject(this.queues);
        
        JSONArray queues = queuesCOnfig.getJSONArray("queues"); //队列参数
        for (int i = 0; i < queues.length(); i++) {
          JSONObject queue = queues.getJSONObject(i); //对每一个queue
          
          if (!queue.has("qos")) {
            logger.error("Mqtt monitor client qos configuration required.");
            return null;
          } 
          int qos = queue.getInt("qos");
          
          if (!queue.has("type")) {
            logger.error("Mqtt monitor client type configuration required");
            return null;
          } 
          int type = queue.getInt("type");
          
          if (!queue.has("names")) {
            logger.error("Mqtt monitor client queues name configuration required");
            return null;
          } 
          JSONArray names = queue.getJSONArray("names");
          
          if (type == 0)  //订阅
          {
            for (int j = 0; j < names.length(); j++) {
              String name = names.getString(j);
              
              for (String queueNamePart : queueNameParts) {
                String queueName = name + queueNamePart;  // 每一项+采集设备sn 如  eems/td/meter/LXBE
                subscribeQoses.add(Integer.valueOf(qos));
                subscribeTopics.add(queueName); //订阅的主题
              } 
            } 
          }
        } 
      } catch (JSONException ex) {
        logger.error("Mqtt monitor client error occured when parse queues json configuration", ex);
      } 
      
      int[] qoses = new int[subscribeQoses.size()]; //服务质量总数
      for (int i = 0; i < subscribeQoses.size(); i++) {
        qoses[i] = ((Integer)subscribeQoses.get(i)).intValue(); //转为 服务质量 整型数组
      }
      
      String[] queues = (String[])subscribeTopics.toArray(new String[subscribeTopics.size()]); //转为 订阅主题 字符串数组
      return new Pair(queues, qoses);//返回一个元组
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\mqtt\MqttMonitorClient.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */