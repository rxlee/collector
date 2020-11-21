  package  com.kkwl.collector.protocols.IEC101;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  
  
  
  public class YXFrameParser
    extends BaseFrameParser
  {
    public YXFrameParser() { super(FrameType.IEC101_YX_FRAME_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      int address = ((Integer)params.get("address")).intValue();
      int frameBytesLen = frameBytes[1] & 0xFF;
      
      if (frameBytesLen > 8 && (frameBytes[7] == 1 || frameBytes[7] == 3)) {
        int addressLo = frameBytes[5] & 0xFF;
        int addressHi = frameBytes[6] & 0xFF;
        int paramAddress = (addressHi << 8) + addressLo;
        if (paramAddress != address) {
          retMap.put("result", Boolean.valueOf(false));
        }
        
        boolean isContinuity = false;
        if ((frameBytes[8] & 0xFFFFFF80) == Byte.MIN_VALUE)
        {
          isContinuity = true;
        }
        
        int number = frameBytes[8] & 0x7F;
        int currentAddr = 13;
        Map<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
        
        if (isContinuity) {
          int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
          int pos = targetAddress - 1;
          
          currentAddr += 2;
          
          for (int i = 0; i < number; i++) {
            
            if (currentAddr >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int value = 0;
            if (frameBytes[7] == 1) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[7] == 3) {
              value = (byte)(frameBytes[currentAddr] - 1);
            } 
            
            valueMap.put(Integer.valueOf(pos + i), Integer.valueOf(value));
            currentAddr++;
          } 
          
          retMap.put("need_reply", Boolean.valueOf(false));
        } else {
          for (int i = 0; i < number; i++) {
            
            if (currentAddr + 2 >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
            currentAddr += 2;
            int pos = targetAddress - 1;
            int value = 0;
            if (frameBytes[7] == 1) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[7] == 3) {
              value = frameBytes[currentAddr] - 1;
            } 
            valueMap.put(Integer.valueOf(pos), Integer.valueOf(value));
            currentAddr++;
          } 
          retMap.put("need_reply", Boolean.valueOf(true));
        } 
        
        retMap.put("valueMap", valueMap);
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC101_YX_FRAME_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\YXFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */