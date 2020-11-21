 package  com.kkwl.collector.common;
 import com.kkwl.collector.common.RegisterType;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.Map;
 
 public enum RegisterType {
    YK("YK", false, true),
    YX("YX", true, false),
    YC("YC", true, false),
    DD("DD", true, false),
    SD("SD", false, true),
    HOLDING("HOLDING", true, true),
    INPUT("INPUT", true, false),
    DISCRETE("DISCRETE", true, false),
    COIL("COIL", true, true); private String name; private boolean readable;
   
   RegisterType(String name, boolean readable, boolean writeable) {
      this.name = name;
      this.readable = readable;
      this.writeable = writeable;
   }
   private boolean writeable; private static Map<String, RegisterType> registerTypeMap;
   
    public String strVal() { return this.name; }
 
 
   
    public boolean isReadable() { return this.readable; }
 
 
   
    public boolean isWriteable() { return this.writeable; }
 
   
   public static RegisterType getRegisterType(String name) {
      for (String key : registerTypeMap.keySet()) {
        if (key.equals(name)) {
          return (RegisterType)registerTypeMap.get(key);
       }
     } 
     
      return null;
   }
 
 
   
   static  {
      registerTypeMap = new HashMap();
 
     
      for (RegisterType e : EnumSet.allOf(RegisterType.class))
        registerTypeMap.put(e.name, e); 
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\RegisterType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */