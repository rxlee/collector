  package  com.kkwl.collector.protocols.IEC101;
  import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.ParsingStatusType;
import com.kkwl.collector.protocols.BaseFrameParser;
import com.kkwl.collector.protocols.BaseProtocol;
import com.kkwl.collector.protocols.IEC101.ConnectAckFrameParser;
import com.kkwl.collector.protocols.IEC101.CorrectTimeFrameAckParser;
import com.kkwl.collector.protocols.IEC101.IEC101ProtocolAdapter;
import com.kkwl.collector.protocols.IEC101.RecruitmentAckFrameParser;
import com.kkwl.collector.protocols.IEC101.RecruitmentCompleteFrameParser;
import com.kkwl.collector.protocols.IEC101.ResetAckFrameParser;
import com.kkwl.collector.protocols.IEC101.SOEFrameParser;
import com.kkwl.collector.protocols.IEC101.TestFrameParser;
import com.kkwl.collector.protocols.IEC101.YKSelectAckFrameParser;
import com.kkwl.collector.protocols.IEC101.YXFrameParser;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.LogTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
  public class IEC101ProtocolAdapter extends BaseProtocol {
    private static final Logger logger = LoggerFactory.getLogger(IEC101ProtocolAdapter.class);
    private static final int BUF_SIZE = 4096;
    private byte[] toBeParsedBytes = new byte[4096];
    
    public IEC101ProtocolAdapter() {
      super(4, "IEC101");
      this.status = ParsingStatusType.PARSING_HEAD;
      
      ConnectAckFrameParser connectAckFrameParser = new ConnectAckFrameParser();
      this.parsers.add(connectAckFrameParser);
      
      ResetAckFrameParser resetAckFrameParser = new ResetAckFrameParser();
      this.parsers.add(resetAckFrameParser);
      
      RecruitmentAckFrameParser recruitmentAckFrameParser = new RecruitmentAckFrameParser();
      this.parsers.add(recruitmentAckFrameParser);
      
      RecruitmentCompleteFrameParser recruitmentCompleteFrameParser = new RecruitmentCompleteFrameParser();
      this.parsers.add(recruitmentCompleteFrameParser);
      
      YXFrameParser yxFrameParser = new YXFrameParser();
      this.parsers.add(yxFrameParser);
      
      YCFrameParser ycFrameParser = new YCFrameParser();
      this.parsers.add(ycFrameParser);
      
      TestAckFrameParser testAckFrameParser = new TestAckFrameParser();
      this.parsers.add(testAckFrameParser);
      
      CorrectTimeFrameAckParser correctTimeFrameAckParser = new CorrectTimeFrameAckParser();
      this.parsers.add(correctTimeFrameAckParser);
      
      SOEFrameParser soeFrameParser = new SOEFrameParser();
      this.parsers.add(soeFrameParser);
      
      TestFrameParser testFrameParser = new TestFrameParser();
      this.parsers.add(testFrameParser);
      
      YKSelectAckFrameParser ykSelectAckFrameParser = new YKSelectAckFrameParser();
      this.parsers.add(ykSelectAckFrameParser);
      
      YKExecuteAckFrameParser yKExecuteAckFrameParser = new YKExecuteAckFrameParser();
      this.parsers.add(yKExecuteAckFrameParser);
    }
  
    
    public List<Map<String, Object>> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
      List<Map<String, Object>> retMapList = new ArrayList<Map<String, Object>>();
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (inBuffer.length == 0) {
        retMap.put("result", Boolean.valueOf(false));
        retMapList.add(retMap);
        return retMapList;
      } 
      
      System.arraycopy(inBuffer, 0, this.toBeParsedBytes, this.toBeParsedBytesStart, inBuffer.length);
      this.toBeParsedBytesStart += inBuffer.length;
      this.toBeParsedBytesLength += inBuffer.length;
      
      String sn = (String)params.get("sn");
      this.pos = 0;
      while (true) {
        int leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_HEAD) {
          
          if (leftLen < 1) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          while (this.toBeParsedBytes[this.pos] != 104 && this.toBeParsedBytes[this.pos] != 16 && this.toBeParsedBytes[this.pos] != -21 && this.pos < this.toBeParsedBytesLength)
          {
            this.pos++;
          }
          
          if (this.pos == this.toBeParsedBytesLength) {
            this.toBeParsedBytesStart = 0;
            this.toBeParsedBytesLength = 0;
            
            break;
          } 
          if (this.toBeParsedBytes[this.pos] == -21) {
            
            if (leftLen < 9) {
              this.toBeParsedBytesLength -= this.pos;
              if (this.toBeParsedBytesLength > 0) {
                System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
              }
              this.toBeParsedBytesStart -= this.pos;
              
              break;
            } 
            System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, 9);
            this.pos += 9;
            retMapList.add(parseFrame(this.buffer, params));
          } else if (this.toBeParsedBytes[this.pos] == 16) {
            
            if (leftLen < 6) {
              this.toBeParsedBytesLength -= this.pos;
              if (this.toBeParsedBytesLength > 0) {
                System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
              }
              this.toBeParsedBytesStart -= this.pos;
              
              break;
            } 
            System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, 6);
            this.pos += 6;
            try {
              retMapList.add(parseFrame(this.buffer, params));
            } catch (Exception e) {
              LogTools.log(logger, sn, LogType.WARN, "101 protocol adapter encontered error.", e);
            } 
          } else {
            this.status = ParsingStatusType.PARSING_LENGTH;
          } 
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_LENGTH) {
          
          if (leftLen < 2) {
            
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          this.length = this.toBeParsedBytes[this.pos + 1] & 0xFF;
          this.bodyLength = this.length + 6;
          this.status = ParsingStatusType.PARSING_BODY;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        if (this.status == ParsingStatusType.PARSING_BODY) {
          if (leftLen < this.bodyLength) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            
            break;
          } 
          System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, this.bodyLength);
          try {
            retMapList.add(parseFrame(this.buffer, params));
          } catch (Exception e) {
            LogTools.log(logger, sn, LogType.WARN, "101 protocol adapter encontered error.", e);
          } 
          this.pos += this.bodyLength;
          this.status = ParsingStatusType.PARSING_HEAD;
        } 
      } 
      
      retMap.put("result", Boolean.valueOf(false));
      retMapList.add(retMap);
      return retMapList;
    }
    
    private Map<String, Object> parseFrame(byte[] frameBytes, Map<String, Object> params) {
      String sn = (String)params.get("sn");
      
      StringBuilder hexStr = new StringBuilder();
      for (byte b : frameBytes) {
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
      } 
      
      LogTools.log(logger, sn, LogType.DEBUG, "101 protocol adapter begin parse " + hexStr);
      
      if (frameBytes[0] == 16) {
        
        byte[] toBeCaculated = new byte[3];
        System.arraycopy(frameBytes, 1, toBeCaculated, 0, 3);
        int crc = Caculator.caculateIEC101CRC(toBeCaculated);
        if ((byte)(crc & 0xFF) != frameBytes[4]) {
          LogTools.log(logger, sn, LogType.WARN, "101 protocol adapter crc error.");
          Map<String, Object> retMap = new HashMap<String, Object>();
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
      } else if (frameBytes[0] == 104) {
        
        short len = (short)(frameBytes[1] & 0xFF);
        byte[] toBeCaculated = new byte[len];
        System.arraycopy(frameBytes, 4, toBeCaculated, 0, len);
        int crc = Caculator.caculateIEC101CRC(toBeCaculated);
        if ((byte)(crc & 0xFF) != frameBytes[len + 4]) {
          LogTools.log(logger, sn, LogType.WARN, "101 protocol adapter crc error.");
          Map<String, Object> retMap = new HashMap<String, Object>();
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
      } 
      
      for (BaseFrameParser parser : this.parsers) {
        Map<String, Object> resultMap = parser.parseBytesFrame(frameBytes, params);
        if (((Boolean)resultMap.get("result")).equals(Boolean.valueOf(true))) {
          LogTools.log(logger, sn, LogType.DEBUG, "101 protocol adapter parser name = " + parser.getName() + " matched.");
          return resultMap;
        }  if (resultMap.containsKey("msg")) {
          LogTools.log(logger, sn, LogType.DEBUG, "101 protocol adapter parser name = " + parser.getName() + " " + (String)resultMap.get("msg"));
        }
      } 
      
      Map<String, Object> retMap = new HashMap<String, Object>();
      retMap.put("result", Boolean.valueOf(false));
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC101\IEC101ProtocolAdapter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */