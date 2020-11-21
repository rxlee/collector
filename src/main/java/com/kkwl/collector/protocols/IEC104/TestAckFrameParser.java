  package  com.kkwl.collector.protocols.IEC104;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  public class TestAckFrameParser
    extends BaseFrameParser
  {
    TestAckFrameParser() { super(FrameType.IEC104_TEST_ACK_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (frameBytes[1] == 4 && frameBytes[2] == -125) {
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC104_TEST_ACK_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\TestAckFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */