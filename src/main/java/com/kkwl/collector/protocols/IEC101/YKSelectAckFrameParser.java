  package  com.kkwl.collector.protocols.IEC101;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  public class YKSelectAckFrameParser
    extends BaseFrameParser
  {
    public YKSelectAckFrameParser() { super(FrameType.IEC101_YK_SELECT_ACK_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (frameBytes[1] == 12 && frameBytes[7] == 46 && (frameBytes[15] & 0x80) == 128) {
        retMap.put("result", Boolean.valueOf(true));
        
        retMap.put("idle", Boolean.valueOf((frameBytes[15] != Byte.MIN_VALUE)));
        retMap.put("type", FrameType.IEC101_YK_SELECT_ACK_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\YKSelectAckFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */