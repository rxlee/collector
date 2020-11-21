 package  com.kkwl.collector.common;
 
 public enum RunMode {
    TCP_SERVER("tcp_server"),
    MQTT_CLIENT("mqtt_client"),
    KAFKA_CLIENT("kafka_client");
   private String name;
   
    public String value() { return this.name; }
 
 
   
    RunMode(String name) { this.name = name; }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\RunMode.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */