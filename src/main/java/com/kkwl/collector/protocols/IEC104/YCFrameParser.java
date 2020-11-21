  package  com.kkwl.collector.protocols.IEC104;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.protocols.IEC104.YCFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  
  public class YCFrameParser extends BaseFrameParser
  {
    private static final Logger logger = LoggerFactory.getLogger(YCFrameParser.class);
  
    
    YCFrameParser() { super(FrameType.IEC104_YC_I_FRAME.value()); }
  
  
    //************解析字节帧，对于不同处理器（遥测、遥信、总召...）*****************
    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) { //frameBytes以104开头的每一帧字节数组
      Map<String, Object> retMap = new HashMap<String, Object>();
//68 0E(长度)0000(发送序号)0000(接收序号)64(类型标识)01(可变结构限定)  0600(传送原因)0100(APDU地址) 000000(信息体地址)14(信息体元素，站召唤全局）
      int frameLen = frameBytes[1] & 0xFF; //后续字节长度
      byte typeFlag = frameBytes[6];  //类型标识符（遥信、遥测、总召……）
      //如果是遥测：9-归一化值，13-短浮点数，21- ，36- ，11-标度化值
      if (frameLen > 7 && (typeFlag == 9 || typeFlag == 13 || typeFlag == 21 || typeFlag == 36 || typeFlag == 11)) {
        boolean isContinuity = false;
        if ((frameBytes[7] & 0x80) == 128) //可变结构限定（最高位为1则地址连续）
        {
          isContinuity = true;
        }
        
        int csyyLength = ((Integer)params.get("csyyLength")).intValue(); //传送原因长度 2
        int ggdzLength = ((Integer)params.get("ggdzLength")).intValue(); //公共地址长度 2
        int xxtLength = ((Integer)params.get("xxtLength")).intValue();  //信息体长度 3
        int number = frameBytes[7] & 0x7F;  //数据单元信息元素(单个信息元素或同类信息元素组合)的数目
        int currentAddress = 8 + csyyLength + ggdzLength; //*****************当前字节地址 12***********************
        Map<Integer, byte[]> valueMap = new HashMap<Integer, byte[]>();
        Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();
        
        int varLen = 0;
        int infoLen = 0;
        //根据帧的类型标识符（遥信、遥测、总召……） 确定信息长度
        switch (typeFlag) { //类型标识
          case 9: //遥测，归一化数值
            varLen = 2; //变量长度为2
            infoLen = 3 + (isContinuity ? 0 : xxtLength);
            break;
          case 13: //测量值，短浮点数（遥测）
            varLen = 4; //变量长度
            infoLen = 5 + (isContinuity ? 0 : xxtLength); //信息长度，如果连续为5，不连续为8
            break;
          case 21:
            varLen = 2;
            infoLen = 2 + (isContinuity ? 0 : xxtLength);
            break;
          case 36:
            varLen = 4;
            infoLen = 12 + (isContinuity ? 0 : xxtLength);
            break;
          case 11:
            varLen = 2;
            infoLen = 3 + (isContinuity ? 0 : xxtLength);
            if (frameBytes.length > currentAddress + 2 + number * (infoLen + 2)) {
              infoLen = 6 + (isContinuity ? 0 : xxtLength);
            }
            break;
        } 
         //计算的字节长度大于实际的帧字节长度 溢出
        if (currentAddress + 2 + (number - 0) * infoLen > frameBytes.length) {
          logger.error("YCFrameParser length overflow");
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
  
        
        if (isContinuity && xxtLength == 3 && frameBytes[currentAddress + 2] != 0) {
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
        
        if (!isContinuity && xxtLength == 3) {
          for (int i = 0; i < number; i++) {
            if (frameBytes[currentAddress + 2 + i * infoLen] != 0) {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
          } 
        }
        
        if (isContinuity) { //连续时
          //目标地址   currentAddress=12
          int targetAddress = (frameBytes[currentAddress] & 0xFF) + ((frameBytes[currentAddress + 1] & 0xFF) << 8); //*********高字节在前，低字节在后*******
          int pos = 0;
          if (targetAddress > 16384) { //遥测4001H（16385）--5000H（20480），（遥信1H--4000H） 采集到的地址为现场设备的内存地址
            pos = targetAddress - 16385;  //相对位置
          } else if (targetAddress > 1793) { //>701H
            pos = targetAddress - 1793;
          } else {
            retMap.put("result", Boolean.valueOf(false));
            return retMap;
          } 
          currentAddress += xxtLength; //字节帧的当前位置15
          for (int i = 0; i < number; i++) {//number第8个字节确定信息体个数

            byte[] value = new byte[varLen]; //变量字节数组
            for (int k = 0; k < varLen; k++) {
              value[k] = frameBytes[currentAddress + k]; //value[0]、value[1]...找到字节帧的位置
            }
            valueMap.put(Integer.valueOf(pos + i), value); //*********************相对位置：值*****************已去除原始设备的定义区
            currentAddress += infoLen;
          } 
        }
        else { //不连续时
          for (int i = 0; i < number; i++) {
            int nTagAddr = (frameBytes[currentAddress] & 0xFF) + ((frameBytes[currentAddress + 1] & 0xFF) << 8);
            int pos = 0;
            if (nTagAddr > 16384) {
              pos = nTagAddr - 16385;   //4000H遥测
            } else if (nTagAddr > 1793) {
              pos = nTagAddr - 1793;
            } else {
              retMap.put("result", Boolean.valueOf(false));
              return retMap;
            } 
            byte[] value = new byte[varLen];
            for (int k = 0; k < varLen; k++) {
              value[k] = frameBytes[currentAddress + xxtLength + k]; //不连续时加上 信息体长度
            }
            valueMap.put(Integer.valueOf(pos), value);//不连续时***********相对位置（0，1，2...）：值*************
            currentAddress += infoLen;
          } 
        } 
        
        int i = frameBytes[2] & 0xFF;
        int j = frameBytes[3] & 0xFF;
        int k = (i + j * 256) / 2; //发送序号
        k++;
        
        i = frameBytes[4] & 0xFF;
        j = frameBytes[5] & 0xFF;
        int l = (i + j * 256) / 2; //接收序号
        
        retMap.put("valueMap", valueMap);

        if (!timeValueMap.isEmpty()) {
          retMap.put("timeValueMap", timeValueMap);
        }
        retMap.put("recvNo", Integer.valueOf(k));
        retMap.put("sendNo", Integer.valueOf(l));
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.IEC104_YC_I_FRAME);
      } else {
        retMap.put("result", Boolean.valueOf(false));
      } 
      
      return retMap;    //retMap{"valueMap"：valueMap{}，"recvNo"：，"sendNo"：，"result"：，"type"： }
                                           //valueMap{“pos”：value，“pos + i”：value，“pos + i”：value}
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\YCFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */