  package  com.kkwl.collector.protocols.IEC104;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.utils.Caculator;
  import java.time.LocalDateTime;
  import java.util.HashMap;
  import java.util.Map;
  
  
  
  
  public class HistoryDDParser extends BaseFrameParser
  {
    public HistoryDDParser() { super(FrameType.IEC104_HISTORY_DD_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      int frameLen = frameBytes[1] & 0xFF;
      if (frameLen > 7 && frameBytes[6] == -1) {
        boolean isContinuity = false;
        if ((frameBytes[7] & 0x80) == 128)
        {
          isContinuity = true;
        }
        
        int csyyLength = ((Integer)params.get("csyyLength")).intValue();
        int ggdzLength = ((Integer)params.get("ggdzLength")).intValue();
        int xxtLength = ((Integer)params.get("xxtLength")).intValue();
        int number = frameBytes[7] & 0x7F;
        int currentAddress = 8 + csyyLength + ggdzLength;
        Map<Integer, byte[]> valueMap = new HashMap<Integer, byte[]>();
        Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();
        
        if (isContinuity) {
          LocalDateTime updateTime = null;
          byte[] cp256Time2aBytes = new byte[7];
          System.arraycopy(frameBytes, currentAddress, cp256Time2aBytes, 0, 7);
          updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
          currentAddress += 7;
          
          int targetAddress = (frameBytes[currentAddress] & 0xFF) + ((frameBytes[currentAddress + 1] & 0xFF) << 8);
          if (xxtLength == 3 && 
            frameBytes[currentAddress + 2] != 0) {
            retMap.put("result", Boolean.valueOf(false));
            return retMap;
          } 
  
          
          int pos = 0;
          if (targetAddress > 25600) {
            pos = targetAddress - 25601;
          } else {
            pos = targetAddress - 3073;
          } 
          currentAddress += xxtLength;
          for (int i = 0; i < number; i++) {
            
            if (currentAddress + 4 > frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            byte[] value = new byte[4];
            value[0] = frameBytes[currentAddress];
            value[1] = frameBytes[currentAddress + 1];
            value[2] = frameBytes[currentAddress + 2];
            value[3] = frameBytes[currentAddress + 3];
            valueMap.put(Integer.valueOf(pos + i), value);
            
            timeValueMap.put(Integer.valueOf(pos + i), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
            currentAddress += 5;
          } 
        } else {
          
          if (xxtLength == 3) {
            int infoLen = 15;
  
            
            if (currentAddress + 2 + (number - 1) * infoLen > frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            for (int i = 0; i < number; i++) {
              if (frameBytes[currentAddress + 2 + i * infoLen] != 0) {
                retMap.put("result", Boolean.valueOf(false));
                return retMap;
              } 
            } 
          } 
  
          
          for (int i = 0; i < number; i++) {
            int nTagAddr = (frameBytes[currentAddress] & 0xFF) + ((frameBytes[currentAddress + 1] & 0xFF) << 8);
            currentAddress += xxtLength;
            int pos = 0;
            if (nTagAddr > 25600) {
              pos = nTagAddr - 25601;
            } else {
              pos = nTagAddr - 3073;
            } 
            
            byte[] value = new byte[4];
            value[0] = frameBytes[currentAddress];
            value[1] = frameBytes[currentAddress + 1];
            value[2] = frameBytes[currentAddress + 2];
            value[3] = frameBytes[currentAddress + 3];
            valueMap.put(Integer.valueOf(pos), value);
            
            LocalDateTime updateTime = null;
            byte[] cp256Time2aBytes = new byte[7];
            System.arraycopy(frameBytes, currentAddress + 5, cp256Time2aBytes, 0, 7);
            updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
            if (updateTime == null) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            timeValueMap.put(Integer.valueOf(pos), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
            currentAddress += 12;
          } 
        } 
        
        int i = frameBytes[2] & 0xFF;
        int j = frameBytes[3] & 0xFF;
        int k = (i + j * 256) / 2;
        k++;
        
        i = frameBytes[4] & 0xFF;
        j = frameBytes[5] & 0xFF;
        int l = (i + j * 256) / 2;
        
        retMap.put("valueMap", valueMap);
        retMap.put("timeValueMap", timeValueMap);
        retMap.put("recvNo", Integer.valueOf(k));
        retMap.put("sendNo", Integer.valueOf(l));
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC104_HISTORY_DD_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\HistoryDDParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */