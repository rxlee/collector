  package  com.kkwl.collector.protocols.IEC104;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  public class YKExecuteAckFrameParser
    extends BaseFrameParser
  {
    public YKExecuteAckFrameParser() { super(FrameType.IEC104_YK_EXECUTE_ACK_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      int frameLen = frameBytes[1] & 0xFF;
      if (frameLen == 14 && frameBytes[6] == 45 && (frameBytes[15] & 0x10) == 0) {
        int i = frameBytes[2] & 0xFF;
        int j = frameBytes[3] & 0xFF;
        int k = (i + j * 256) / 2;
        k++;
        
        i = frameBytes[4] & 0xFF;
        j = frameBytes[5] & 0xFF;
        int l = (i + j * 256) / 2;
        
        if (frameBytes[8] == 7) {
          
          retMap.put("execute_result", Boolean.valueOf(true));
        } else {
          
          retMap.put("execute_result", Boolean.valueOf(false));
        } 
        
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("recvNo", Integer.valueOf(k));
        retMap.put("sendNo", Integer.valueOf(l));
        retMap.put("type", FrameType.IEC104_YK_EXECUTE_ACK_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\YKExecuteAckFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */