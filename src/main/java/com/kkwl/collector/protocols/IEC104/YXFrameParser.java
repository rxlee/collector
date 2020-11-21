  package  com.kkwl.collector.protocols.IEC104;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  
  public class YXFrameParser extends BaseFrameParser
  {
    public YXFrameParser() { super(FrameType.IEC104_YX_I_FRAME.value()); }
  
  
    
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      int frameLen = frameBytes[1] & 0xFF; //后续字节长度
      if (frameLen > 8 && (frameBytes[6] == 1 || frameBytes[6] == 3)) {
        boolean isContinuity = false;
        if ((frameBytes[7] & 0xFFFFFF80) == Byte.MIN_VALUE)
        {
          isContinuity = true;
        }
        
        int csyyLength = ((Integer)params.get("csyyLength")).intValue();
        int ggdzLength = ((Integer)params.get("ggdzLength")).intValue();
        int xxtLength = ((Integer)params.get("xxtLength")).intValue();
        int number = frameBytes[7] & 0x7F;
        int currentAddr = 8 + csyyLength + ggdzLength; //8+2+2
        Map<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
        
        if (isContinuity) { //连续
          int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8); //信息体地址
          int pos = targetAddress - 1;
          
          currentAddr += xxtLength; //8+2+2+3
          
          for (int i = 0; i < number; i++) { //数量
            
            if (currentAddr >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            
            int value = 0;
            if (frameBytes[6] == 1) { //单点遥信
              value = frameBytes[currentAddr];
            } else if (frameBytes[6] == 3) {
              value = (byte)(frameBytes[currentAddr] - 1);
            } 
            
            valueMap.put(Integer.valueOf(pos + i), Integer.valueOf(value)); //valueMap 位置：值
            currentAddr++;
          } 
        } else { //不连续
          for (int i = 0; i < number; i++) {
            
            if (currentAddr + xxtLength >= frameBytes.length) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap; //
            } 
            
            int targetAddress = (frameBytes[currentAddr] & 0xFF) + ((frameBytes[currentAddr + 1] & 0xFF) << 8);
            currentAddr += xxtLength;
            int pos = targetAddress - 1;
            int value = 0;
            if (frameBytes[6] == 1) {
              value = frameBytes[currentAddr];
            } else if (frameBytes[6] == 3) {
              value = frameBytes[currentAddr] - 1;
            } 
            valueMap.put(Integer.valueOf(pos), Integer.valueOf(value));
            currentAddr++;
          } 
        } 
        //接收标号
        int i = frameBytes[2] & 0xFF;
        int j = frameBytes[3] & 0xFF;
        int k = (i + j * 256) / 2;
        k++;
        //发送标号
        i = frameBytes[4] & 0xFF;
        j = frameBytes[5] & 0xFF;
        int l = (i + j * 256) / 2;
        
        retMap.put("valueMap", valueMap);
        retMap.put("recvNo", Integer.valueOf(k));
        retMap.put("sendNo", Integer.valueOf(l));
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC104_YX_I_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap; //{"valueMap": ,"recvNo": ,"sendNo": ,"result": ,"type": }
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\YXFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */