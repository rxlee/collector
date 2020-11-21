  package  com.kkwl.collector.protocols.IEC104;
  
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
    public SOEFrameParser() { super(FrameType.IEC104_SOE_I_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      int frameLen = frameBytes[1] & 0xFF;
      if (frameLen > 6 && (frameBytes[6] == 30 || frameBytes[6] == 31)) {
        boolean isContinuity = false;
        if ((frameBytes[7] & 0x80) == 128)
        {
          isContinuity = true;
        }
        
        int csyyLength = ((Integer)params.get("csyyLength")).intValue();
        int ggdzLength = ((Integer)params.get("ggdzLength")).intValue();
        int xxtLength = ((Integer)params.get("xxtLength")).intValue();
        Map<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
        Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();
        
        int nNum = frameBytes[7] & 0x7F;
        int currentAddr = 8 + csyyLength + ggdzLength;
        
        if (isContinuity) {
          int targetAddress = frameBytes[currentAddr] + (frameBytes[currentAddr + 1] << 8);
          int pos = targetAddress - 1;
          
          currentAddr += xxtLength;
          for (int i = 0; i < nNum; i++) {
            
            if (currentAddr + 8 >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int value = 0;
            if (frameBytes[6] == 30) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[6] == 31) {
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
          for (int i = 0; i < nNum; i++) {
            
            if (currentAddr + xxtLength + 8 > frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int targetAddress = frameBytes[currentAddr] + (frameBytes[currentAddr + 1] << 8);
            currentAddr += xxtLength;
            int pos = targetAddress - 1;
            int value = 0;
            if (frameBytes[6] == 30) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[6] == 31) {
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
        
        int i = frameBytes[2] & 0xFF;
        int j = frameBytes[3] & 0xFF;
        int k = (i + j * 256) / 2;
        k++;
        
        i = frameBytes[4] & 0xFF;
        j = frameBytes[5] & 0xFF;
        int l = (i + j * 256) / 2;
        
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC104_SOE_I_FRAME);
        retMap.put("valueMap", valueMap);
        retMap.put("timeValueMap", timeValueMap);
        retMap.put("recvNo", Integer.valueOf(k));
        retMap.put("sendNo", Integer.valueOf(l));
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\SOEFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */