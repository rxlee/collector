  package  com.kkwl.collector.protocols.IEC101;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.utils.Caculator;
  import java.time.LocalDateTime;
  import java.util.HashMap;
  import java.util.Map;
  
  
  
  
  public class YCFrameParser
    extends BaseFrameParser
  {
    public YCFrameParser() { super(FrameType.IEC101_YC_FRAME_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      int address = ((Integer)params.get("address")).intValue();
      int frameBytesLen = frameBytes[1] & 0xFF;
      
      if ((frameBytesLen > 8 && frameBytes[7] == 9) || frameBytes[7] == 13 || frameBytes[7] == 21 || frameBytes[7] == 36 || frameBytes[7] == 11) {
        
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
        Map<Integer, byte[]> valueMap = new HashMap<Integer, byte[]>();
        Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();
        
        if (isContinuity) {
          int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
          int pos = 0;
          if (targetAddress > 16384) {
            pos = targetAddress - 16385;
          } else if (targetAddress > 1793) {
            pos = targetAddress - 1793;
          } else {
            retMap.put("result", Boolean.valueOf(false));
            return retMap;
          } 
          currentAddr += 2;
          for (int i = 0; i < number; i++) {
            byte[] cp256Time2aBytes; LocalDateTime updateTime; byte[] value = new byte[4];
            switch (frameBytes[7]) {
              
              case 9:
                if (currentAddr + 2 > frameBytes.length) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos + i), value);
                currentAddr += 3;
                break;
              
              case 13:
                if (currentAddr + 4 > frameBytes.length) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                value[2] = frameBytes[currentAddr + 2];
                value[3] = frameBytes[currentAddr + 3];
                valueMap.put(Integer.valueOf(pos + i), value);
                currentAddr += 5;
                break;
              
              case 21:
                if (currentAddr + 2 > frameBytes.length) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos + i), value);
                currentAddr += 2;
                break;
              
              case 36:
                if (currentAddr + 4 > frameBytes.length) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                value[2] = frameBytes[currentAddr + 2];
                value[3] = frameBytes[currentAddr + 3];
                valueMap.put(Integer.valueOf(pos + i), value);
                
                updateTime = null;
                cp256Time2aBytes = new byte[7];
                System.arraycopy(frameBytes, currentAddr + 6, cp256Time2aBytes, 0, 7);
                updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
                if (updateTime == null) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                timeValueMap.put(Integer.valueOf(pos + i), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
                currentAddr += 12;
                break;
              case 11:
                if (currentAddr + 2 > frameBytes.length) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos + i), value);
                currentAddr += 3;
                break;
            } 
          
          } 
        } else {
          int infoLen = 0;
          switch (frameBytes[7]) {
            case 9:
              infoLen = 5;
              break;
            case 13:
              infoLen = 7;
              break;
            case 21:
              infoLen = 4;
              break;
            case 36:
              infoLen = 14;
              break;
            case 11:
              infoLen = 7;
              break;
          } 
          
          if (currentAddr + 2 + (number - 1) * infoLen > frameBytes.length) {
            retMap.put("result", Boolean.valueOf(false));
            return retMap;
          } 
  
          
          for (int i = 0; i < number; i++) {
            byte[] cp256Time2aBytes; LocalDateTime updateTime; int nTagAddr = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
            currentAddr += 2;
            int pos = 0;
            if (nTagAddr > 16384) {
              pos = nTagAddr - 16385;
            } else {
              pos = nTagAddr - 1793;
            } 
            
            byte[] value = new byte[4];
            switch (frameBytes[7]) {
              case 9:
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos), value);
                currentAddr += 3;
                break;
              case 13:
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                value[2] = frameBytes[currentAddr + 2];
                value[3] = frameBytes[currentAddr + 3];
                valueMap.put(Integer.valueOf(pos), value);
                currentAddr += 5;
                break;
              case 21:
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos), value);
                currentAddr += 2;
                break;
              case 36:
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                value[2] = frameBytes[currentAddr + 2];
                value[3] = frameBytes[currentAddr + 3];
                valueMap.put(Integer.valueOf(pos), value);
                
                updateTime = null;
                cp256Time2aBytes = new byte[7];
                System.arraycopy(frameBytes, currentAddr + 5, cp256Time2aBytes, 0, 7);
                updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
                if (updateTime == null) {
                  retMap.put("result", Boolean.valueOf(false));
                  return retMap;
                } 
                
                timeValueMap.put(Integer.valueOf(pos), Long.valueOf(updateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant().toEpochMilli()));
                currentAddr += 12;
                break;
              case 11:
                value[0] = frameBytes[currentAddr];
                value[1] = frameBytes[currentAddr + 1];
                valueMap.put(Integer.valueOf(pos), value);
                currentAddr += 3;
                break;
            } 
          
          } 
        } 
        retMap.put("valueMap", valueMap);
        if (!timeValueMap.isEmpty()) {
          retMap.put("timeValueMap", timeValueMap);
        }
        
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC101_YC_FRAME_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\YCFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */