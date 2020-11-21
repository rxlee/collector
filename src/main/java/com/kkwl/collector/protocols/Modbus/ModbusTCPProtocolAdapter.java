  package  com.kkwl.collector.protocols.Modbus;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.common.ParsingStatusType;
  import com.kkwl.collector.protocols.BaseProtocol;
  import com.kkwl.collector.protocols.Modbus.ModbusTCPProtocolAdapter;
  import com.kkwl.collector.utils.LogTools;
  import java.util.ArrayList;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import org.javatuples.Pair;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  ///modbusTCP协议适配器
  public class ModbusTCPProtocolAdapter extends BaseProtocol
  {
    private static final Logger logger = LoggerFactory.getLogger(com.kkwl.collector.protocols.Modbus.ModbusProtocolAdapter.class);
    private byte answerType;
    private byte revDeviceAddress;
    private static final int BUF_SIZE = 2048;
    private byte[] toBeParsedBytes = new byte[2048];
    
    public ModbusTCPProtocolAdapter() {
      super(5, "ModbusTCP");
      this.status = ParsingStatusType.PARSING_HEAD;
    }

    public List<Map<String, Object>> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
      List<Map<String, Object>> retMapList = new ArrayList<Map<String, Object>>();
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      short deviceAddress = ((Short)params.get("deviceAddress")).shortValue();
      String sn = (String)params.get("sn");
      byte lastCommand = ((Byte)params.get("lastCommand")).byteValue();
      int lastMBAPIndex = ((Integer)params.get("lastMBAPIndex")).intValue();
      
      if (inBuffer.length == 0) {
        retMap.put("result", Boolean.valueOf(false));
        retMapList.add(retMap);
        return retMapList;
      } 
      
      System.arraycopy(inBuffer, 0, this.toBeParsedBytes, this.toBeParsedBytesStart, inBuffer.length);
      this.toBeParsedBytesStart += inBuffer.length;
      this.toBeParsedBytesLength += inBuffer.length;
      
      this.pos = 0;
      Boolean needRest = Boolean.valueOf(false);
      while (true) {
        int leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_HEAD) {
          if (leftLen < 2) {
            needRest = Boolean.valueOf(true);

            break;
          }
          
          while (this.pos + 1 < this.toBeParsedBytesLength) {
            //0xff: 1.为了适应与其他语言二进制通讯时各种数据的一致性，需要做一些处理。该作用主要是为了将 有符号数转换为无符号数
        	//      2.十六进制0xff的长度是一个字节，即八位，二进制为：1111 1111，那么一个 8bit 数与1111 1111与运算还是这个数本身，但是一个16bit 数与 0xff就被截断了，比如 1100110011001100 & 0xff结果为 11001100
        	//>>8：     这个是根据需求而定的，可以是>>8也可以是>>16,>>24,等等
        	//		>>8 & 0xff运算的意义其实就是截断，将123456的高位右移8位，通过0xff截取出来。实际意义就是取字节，比如一个4字节的数，需要将每个字节内容取出来转换出目标数据，那么通过>> 并且 &0xff 运算 就可以去除想要的部分。
        	/*
             *    实际意义就是取字节，比如一个4字节的数，需要将每个字节内容取出来转换出目标数据，那么通过>> 并且 &0xff 运算 就可以去除想要的部分。
				再详细点：4字节 ，32 位，按照大端方式排列，
				最高位                                         最低位
				11111111 10101010 11000011 10101010
				最高位8字节要移到最低位那么，这个8个字节>>（3*8），然后与0xff运算，取出，然后后续得>>(2*8) & 0xff ;>>(1*8) & 0xff,均可取出。
				详见：https://www.cnblogs.com/MCSFX/p/11027160.html
        	 */
            if (this.toBeParsedBytes[this.pos] == (byte)(lastMBAPIndex >> 8 & 0xFF) && this.toBeParsedBytes[this.pos + 1] == (byte)(lastMBAPIndex & 0xFF)) {
              break;
            }
            this.pos++;
          } 
          
          if (this.pos == this.toBeParsedBytesLength || this.pos + 1 == this.toBeParsedBytesLength) {
            this.toBeParsedBytesStart = 0;
            this.toBeParsedBytesLength = 0;
            
            break;
          } 
          this.status = ParsingStatusType.PARSING_LENGTH;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_LENGTH) {
          if (leftLen < 9) {
            needRest = Boolean.valueOf(true);
            
            break;
          } 
          this.length = this.toBeParsedBytes[this.pos + 5] & 0xFF;
          this.revDeviceAddress = this.toBeParsedBytes[this.pos + 6];
          this.answerType = this.toBeParsedBytes[this.pos + 7];
          this.bodyLength = this.length + 6;
          this.status = ParsingStatusType.PARSING_BODY;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_BODY) {
          if (leftLen < this.bodyLength) {
            needRest = Boolean.valueOf(true);
            
            break;
          } 
          System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, this.bodyLength);
          try {
            retMapList.add(parseFrame(this.buffer, params));
          } catch (Exception e) {
            LogTools.log(logger, sn, LogType.WARN, "Modbus protocol adapter encontered error.", e);
          } 
          this.pos += this.bodyLength;
          this.status = ParsingStatusType.PARSING_HEAD;
        } 
      } 
      
      if (this.pos != 0 && needRest.booleanValue()) {
        this.toBeParsedBytesLength -= this.pos;
        if (this.toBeParsedBytesLength > 0) {
          System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
        }
        this.toBeParsedBytesStart -= this.pos;
        this.status = ParsingStatusType.PARSING_HEAD;
      } 
      
      retMap.put("result", Boolean.valueOf(false));
      retMapList.add(retMap);
      
      return retMapList;
    }
    
    private Map<String, Object> parseFrame(byte[] frameBytes, Map<String, Object> params) {
      String sn = (String)params.get("sn");
      int sectionLength = ((Integer)params.get("sectionLength")).intValue();
      String sectionName = (String)params.get("sectionName");
      
      StringBuilder hexStr = new StringBuilder();
      for (byte b : frameBytes) {
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
      } 
      
      LogTools.log(logger, sn, LogType.DEBUG, "Modbus protocol adapter begin parse " + hexStr);

      short answerType = (short)(frameBytes[7] & 0xFF);
      int valueLength = this.toBeParsedBytes[this.pos + 8] & 0xFF;
      byte[] dataSection = null;
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (answerType == 5) {
        dataSection = new byte[8];
        System.arraycopy(frameBytes, 0, dataSection, 0, 8);
        retMap.put("type", FrameType.MODBUS_05_DATA_FRAME);
      } else if (answerType == 16) {
        dataSection = new byte[8];
        System.arraycopy(frameBytes, 0, dataSection, 0, 8);
        retMap.put("type", FrameType.MODBUS_10_DATA_FRAME);
      } else {
        if (sectionLength != valueLength) {
          
          LogTools.log(logger, sn, LogType.WARN, "Modbus protocol adapter enter wrong length response.");
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
        
        dataSection = new byte[valueLength];
        System.arraycopy(frameBytes, 9, dataSection, 0, valueLength);
        retMap.put("type", FrameType.MODBUS_DATA_FRAME);
      } 
      
      Pair<String, byte[]> pair = new Pair<String, byte[]>(sectionName, dataSection);
      
      retMap.put("result", Boolean.valueOf(true));
      retMap.put("valueMap", pair);
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\Modbus\ModbusTCPProtocolAdapter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */