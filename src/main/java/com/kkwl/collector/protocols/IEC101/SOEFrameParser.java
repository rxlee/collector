  package  com.kkwl.collector.protocols.IEC101;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.utils.Caculator;
  import java.time.LocalDateTime;
  import java.util.HashMap;
  import java.util.Map;
  
  
  
  
  public class SOEFrameParser
    extends BaseFrameParser
  {
    public SOEFrameParser() { super(FrameType.IEC101_SOE_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      int address = ((Integer)params.get("address")).intValue();
      
      int frameLen = frameBytes[1] & 0xFF;
      if (frameLen == 19 && (frameBytes[7] == 30 || frameBytes[7] == 31)) {
        int addressLo = frameBytes[6] & 0xFF;
        int addressHi = frameBytes[7] & 0xFF;
        int paramAddress = (addressHi << 8) + addressLo;
        if (paramAddress != address) {
          retMap.put("result", Boolean.valueOf(false));
        }
        
        int number = frameBytes[8] & 0x7F;
        int currentAddr = 13;
        Map<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
        Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();
        
        boolean isContinuity = false;
        if ((frameBytes[8] & 0x80) == 128)
        {
          isContinuity = true;
        }
        
        if (isContinuity) {
          int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
          int pos = targetAddress - 1;
          
          currentAddr += 2;
          for (int i = 0; i < number; i++) {
            
            if (currentAddr + 8 >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int value = 0;
            if (frameBytes[7] == 30) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[7] == 31) {
              value = frameBytes[currentAddr] - 1;
            } 
            valueMap.put(Integer.valueOf(pos + i), Integer.valueOf(value));
            
            LocalDateTime updateTime = null;
            byte[] cp256Time2aBytes = new byte[7];
            System.arraycopy(frameBytes, currentAddr + 1, cp256Time2aBytes, 0, 7);
            updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
            if (updateTime == null) {
              timeValueMap.put(Integer.valueOf(pos + i), null);
            } else {
              timeValueMap.put(Integer.valueOf(pos + i), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
            } 
            
            currentAddr += 8;
          } 
        } else {
          for (int i = 0; i < number; i++) {
            
            if (currentAddr + 2 + 8 > frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int targetAddress = frameBytes[currentAddr] + (frameBytes[currentAddr + 1] << 8);
            currentAddr += 2;
            int pos = targetAddress - 1;
            int value = 0;
            if (frameBytes[7] == 30) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[7] == 31) {
              value = frameBytes[currentAddr] - 1;
            } 
            valueMap.put(Integer.valueOf(pos), Integer.valueOf(value));
            
            LocalDateTime updateTime = null;
            byte[] cp256Time2aBytes = new byte[7];
            System.arraycopy(frameBytes, currentAddr + 1, cp256Time2aBytes, 0, 7);
            updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
            if (updateTime == null) {
              timeValueMap.put(Integer.valueOf(pos + i), null);
            } else {
              timeValueMap.put(Integer.valueOf(pos + i), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
            } 
            
            currentAddr += 8;
          } 
        } 
        
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC101_SOE_FRAME);
        retMap.put("valueMap", valueMap);
        retMap.put("timeValueMap", timeValueMap);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\SOEFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */