 package  com.kkwl.collector.common;
 
 public enum StandardEnum {
    IEC104_STANDARD_GB("STANDARD_GB"),
    IEC104_STANDARD_2002("STANDARD_2002");
   private String name;
   
    public String getName() { return this.name; }
 
 
   
    StandardEnum(String name) { this.name = name; }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\StandardEnum.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */