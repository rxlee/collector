  package  com.kkwl.collector.protocols.IEC101;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  
  
  
  public class RecruitmentCompleteFrameParser
    extends BaseFrameParser
  {
    public RecruitmentCompleteFrameParser() { super(FrameType.IEC101_RECRUITMENT_COMPLETE_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      int address = ((Integer)params.get("address")).intValue();
      
      if (frameBytes[1] == 12 && frameBytes[7] == 100 && frameBytes[9] == 10 && frameBytes[17] == 22) {
        int addressLo = frameBytes[5] & 0xFF;
        int addressHi = frameBytes[6] & 0xFF;
        int paramAddress = (addressHi << 8) + addressLo;
        
        if (address == paramAddress) {
          retMap.put("type", FrameType.IEC101_RECRUITMENT_COMPLETE_FRAME);
          retMap.put("result", Boolean.valueOf(true));
        } else {
          retMap.put("result", Boolean.valueOf(false));
        } 
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\RecruitmentCompleteFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */