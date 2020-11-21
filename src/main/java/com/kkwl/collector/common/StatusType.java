 package  com.kkwl.collector.common;
 ///状态类型
 public enum StatusType {
    IDLE((byte)100, "无状态"),
    REGISTER_ADDRESS((byte)2, "注册地址"),
    CONNECTED((byte)3, "已连接"),
    OFFLINE((byte)0, "离线"),
    ONLINE((byte)1, "在线"),
    NEW_ALARM((byte)1, "新增告警"),
    ALARM_CLEAR((byte)0, "告警消除"),
    ALARM_TYPE_STATUS_CHANGE((byte)1, "状态变位"),
    ALARM_TYPE_STATUS_OVER_MORE_LOWER_LIMIT((byte)2, "越下下限告警"),
    ALARM_TYPE_STATUS_OVER_LOWER_LIMIT((byte)3, "越下限告警"),
    ALARM_TYPE_STATUS_OVER_UPPER_LIMIT((byte)4, "越上限告警"),
    ALARM_TYPE_STATUS_OVER_MORE_UPPER_LIMIT((byte)4, "越上上限告警"),
    CONNECT_ALARM((byte)3, "离线告警"); private byte status;
   private String statusName;
   
    public int intValule() { return this.status; }
 
 
   
    public String strValue() { return this.statusName; }
 
   
   StatusType(byte status, String statusName) {
      this.status = status;
      this.statusName = statusName;
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\StatusType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */