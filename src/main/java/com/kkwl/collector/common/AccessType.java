 package  com.kkwl.collector.common;
 
 //原本有static报错，去除了static
 public enum AccessType {
    FIXEDIP("FIXEDIP"),   //固定接入
    DIAL("DIAL"),  //拨号接入
    MQTT("MQTT"),  //MQTT
    KAFKA("KAFKA"),  //KAFKA
    TCPCLIENT("TCPCLIENT");  //tcpClient客户端
   private String typeName;
   
    AccessType(String typeName) { this.typeName = typeName; }
 
 
   
    public String value() { return this.typeName; }
 
   
   public static AccessType getAccessTypeByName(String typeName) {
      switch (typeName) {
       case "FIXEDIP":
          return FIXEDIP;
       case "DIAL":
          return DIAL;
       case "MQTT":
          return MQTT;
       case "KAFKA":
          return KAFKA;
       case "TCPCLIENT":
          return TCPCLIENT;
     } 
      return null;
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\AccessType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */