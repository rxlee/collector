  package  com.kkwl.collector.common;
  
  import com.kkwl.collector.common.GlobalEventTypes;
  import com.kkwl.collector.models.EventType;
  import java.util.Map;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  
  public class GlobalEventTypes
  {
    private static Logger logger = LoggerFactory.getLogger(GlobalEventTypes.class);
    
    public static final String DTU_OFFLINE = "dtu_offline";
    
    public static final String C_DEVICE_OFFLINE = "c_device_offline";
    
    public static Map<String, Map<String, EventType>> parentIndex_sn_map = null;
  
    
    public static boolean isSatisfy(String triggerTypeSn, double value, String param1, String param2, String param3, String param4) {
      if (triggerTypeSn == null || triggerTypeSn.isEmpty()) {
        return false;
      }
      switch (triggerTypeSn) {
        case "0_to_1":
          return (1 == (int)value);
        case "1_to_0":
          return (0 == (int)value);
        case "over_a":
          if (param1 == null || param1.isEmpty()) {
            return false;
          }
          return (value > Double.parseDouble(param1));
        case "below_a":
          if (param1 == null || param1.isEmpty()) {
            return false;
          }
          return (value < Double.parseDouble(param1));
      } 
      logger.error("no suppport trigger type sn: " + triggerTypeSn);
      return false;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\GlobalEventTypes.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */