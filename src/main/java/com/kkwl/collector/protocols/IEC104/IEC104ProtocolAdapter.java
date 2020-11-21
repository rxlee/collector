  package  com.kkwl.collector.protocols.IEC104;
  import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.ParsingStatusType;
import com.kkwl.collector.protocols.BaseFrameParser;
import com.kkwl.collector.protocols.BaseProtocol;
import com.kkwl.collector.protocols.IEC104.DDRecruitAckFrameParser;
import com.kkwl.collector.protocols.IEC104.FaultFrameParser;
import com.kkwl.collector.protocols.IEC104.HistoryRecruitmentCompleteFrameParser;
import com.kkwl.collector.protocols.IEC104.IEC104ProtocolAdapter;
import com.kkwl.collector.protocols.IEC104.RecruitmentACKFrameParser;
import com.kkwl.collector.protocols.IEC104.YCFrameParser;
import com.kkwl.collector.protocols.IEC104.YKExecuteAckFrameParser;
import com.kkwl.collector.utils.LogTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
  public class IEC104ProtocolAdapter extends BaseProtocol {
    private static final Logger logger = LoggerFactory.getLogger(IEC104ProtocolAdapter.class);
    private static final int BUF_SIZE = 4096;
    private byte[] toBeParsedBytes = new byte[4096]; //要被解析的字节数组
    //构造函数，添加各种帧解析器
    public IEC104ProtocolAdapter() {
      super(1, "IEC104");
      this.status = ParsingStatusType.PARSING_HEAD;//头部
      
      ConnectAckFrameParser connectAckFrameParser = new ConnectAckFrameParser();
      this.parsers.add(connectAckFrameParser);
      
      InitializationCompleteFrameParser initializationCompleteFrameParser = new InitializationCompleteFrameParser();
      this.parsers.add(initializationCompleteFrameParser);
      
      SFrameParser sFrameParser = new SFrameParser();
      this.parsers.add(sFrameParser);
      
      RecruitmentACKFrameParser recruitmentACKFrameParser = new RecruitmentACKFrameParser();
      this.parsers.add(recruitmentACKFrameParser);
      
      RecruitmentCompleteFrameParser recruitmentCompleteFrameParser = new RecruitmentCompleteFrameParser();
      this.parsers.add(recruitmentCompleteFrameParser);
      
      DDRecruitAckFrameParser ddRecruitAckFrameParser = new DDRecruitAckFrameParser();
      this.parsers.add(ddRecruitAckFrameParser);
      
      DDRecruitCompleteFrameParser ddRecruitCompleteFrameParser = new DDRecruitCompleteFrameParser();
      this.parsers.add(ddRecruitCompleteFrameParser);
      
      DDFrameParser ddFrameParser = new DDFrameParser();
      this.parsers.add(ddFrameParser);
      
      TestAckFrameParser testAckFrameParser = new TestAckFrameParser();
      this.parsers.add(testAckFrameParser);
      
      TestFrameParser testFrameParser = new TestFrameParser();
      this.parsers.add(testFrameParser);
      
      FaultFrameParser faultFrameParser = new FaultFrameParser();
      this.parsers.add(faultFrameParser);
      
      YXFrameParser yxFrameParser = new YXFrameParser();
      this.parsers.add(yxFrameParser);
      
      YCFrameParser ycFrameParser = new YCFrameParser();
      this.parsers.add(ycFrameParser);
      
      CorrectTimeAckFrameParser correctTimeAckFrameParser = new CorrectTimeAckFrameParser();
      this.parsers.add(correctTimeAckFrameParser);
      
      YKSelectAckFrameParser ykSelectAckFrameParser = new YKSelectAckFrameParser();
      this.parsers.add(ykSelectAckFrameParser);
      
      YKExecuteAckFrameParser ykExecuteAckFrameParser = new YKExecuteAckFrameParser();
      this.parsers.add(ykExecuteAckFrameParser);
      
      YKExecuteCompleteFrameParser ykExecuteCompleteFrameParser = new YKExecuteCompleteFrameParser();
      this.parsers.add(ykExecuteCompleteFrameParser);
      
      SOEFrameParser soeFrameParser = new SOEFrameParser();
      this.parsers.add(soeFrameParser);
      
      HistoryRecruitmentAckFrameParser historyRecruitmentAckFrameParser = new HistoryRecruitmentAckFrameParser();
      this.parsers.add(historyRecruitmentAckFrameParser);
      
      HistoryRecruitmentCompleteFrameParser historyRecruitmentCompleteFrameParser = new HistoryRecruitmentCompleteFrameParser();
      this.parsers.add(historyRecruitmentCompleteFrameParser);
      
      HistoryYXParser historyYXParser = new HistoryYXParser();
      this.parsers.add(historyYXParser);
      
      HistoryYCParser historyYCParser = new HistoryYCParser();
      this.parsers.add(historyYCParser);
      
      HistoryDDParser historyDDParser = new HistoryDDParser();
      this.parsers.add(historyDDParser);
    }
  
    ///字节解析【】 返回结果retMapList，参数（收到的原始字节，协议参数），原始数据预处理
    public List<Map<String, Object>> doBytesParse(byte[] inBuffer, Map<String, Object> params) { //协议解析字节【】
      List<Map<String, Object>> retMapList = new ArrayList<Map<String, Object>>();
      Map<String, Object> retMap = new HashMap<String, Object>(); //retMap["result":,"":]
      //如果收到的长度为0，置“result”:0
        if (inBuffer.length == 0)
        {
          retMap.put("result", Boolean.valueOf(false));
          retMapList.add(retMap);
          return retMapList;
        }
      
      System.arraycopy(inBuffer, 0, this.toBeParsedBytes, this.toBeParsedBytesStart, inBuffer.length); //toBeParsedBytes所有的
      this.toBeParsedBytesStart += inBuffer.length;
      this.toBeParsedBytesLength += inBuffer.length;
      
      String sn = (String)params.get("sn");
      this.pos = 0; //初始位置为0
      //一直执行
      while (true) {
        int leftLen = this.toBeParsedBytesLength - this.pos; //剩余长度
        //状态为 头
        if (this.status == ParsingStatusType.PARSING_HEAD) {
          
          if (leftLen < 1) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            break;
           }
          //****************找到起始符号68 如果字节内容不是104，位置累加******************
          while (this.toBeParsedBytes[this.pos] != 104 && this.pos < this.toBeParsedBytesLength)
           {
            this.pos++;
           }
          //结束后置零
          if (this.pos == this.toBeParsedBytesLength)
           {
            this.toBeParsedBytesStart = 0;
            this.toBeParsedBytesLength = 0;
            break;
           }
          this.status = ParsingStatusType.PARSING_LENGTH;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        //状态为 长度
        if (this.status == ParsingStatusType.PARSING_LENGTH) {
          if (leftLen < 2) {
            
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            this.toBeParsedBytesStart -= this.pos;
            break;
          } 
          this.length = this.toBeParsedBytes[this.pos + 1] & 0xFF; //第二个字节指后续长度
          this.bodyLength = this.length + 2;  //整个帧的长度
          this.status = ParsingStatusType.PARSING_BODY;
        } 
        
        leftLen = this.toBeParsedBytesLength - this.pos;
        //状态为 主体
        if (this.status == ParsingStatusType.PARSING_BODY) {
          if (leftLen < this.bodyLength) {
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
              System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength); //覆盖
            }
            this.toBeParsedBytesStart -= this.pos;
            break;
          } 
          System.arraycopy(this.toBeParsedBytes, this.pos, this.buffer, 0, this.bodyLength); //******把每一帧数据放在buffer中，不停覆盖，供后续每一帧的解析
         //解析帧 为具体的内容
          try {
            retMapList.add(parseFrame(this.buffer, params)); //*********将retMap加到list   解析内容帧parseFrame返回retMap*****************

          } catch (Exception e) {
            LogTools.log(logger, sn, LogType.WARN, "104 protocol adapter encontered error.", e);
          } 
          this.pos += this.bodyLength;
          this.status = ParsingStatusType.PARSING_HEAD;
        } 
      } 
      
      retMap.put("result", Boolean.valueOf(false));
      retMapList.add(retMap);
      return retMapList;
    }
    //*****************解析内容帧函数*************************
    private Map<String, Object> parseFrame(byte[] frameBytes, Map<String, Object> params) { //frameBytes 以104开头的每一帧数据
      String sn = (String)params.get("sn");
      StringBuilder hexStr = new StringBuilder();
      for (byte b : frameBytes) {
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) })); //以十六进制形式输出 02 表示不足两位,前面补0输出
      }
      LogTools.log(logger, sn, LogType.DEBUG, "104 protocol adapter begin parse " + hexStr);
      
      for (BaseFrameParser parser : this.parsers) { //********遍历解析器，对每一字节帧数据解析*******
        Map<String, Object> resultMap = parser.parseBytesFrame(frameBytes, params); //********每个帧解析器都重写**解析字节帧，得到resultMap（=retMap）************
        if (((Boolean)resultMap.get("result")).equals(Boolean.valueOf(true))) {
          LogTools.log(logger, sn, LogType.DEBUG, "104 protocol adapter parser name = " + parser.getName() + " matched."); //如果结果为true,说明解析器匹配
          return resultMap; //返回
        }  if (resultMap.containsKey("msg")) {
          LogTools.log(logger, sn, LogType.DEBUG, "104 protocol adapter parser name = " + parser.getName() + " " + (String)resultMap.get("msg"));
        }
      } 
      
      Map<String, Object> retMap = new HashMap<String, Object>();
      retMap.put("result", Boolean.valueOf(false));
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\IEC104ProtocolAdapter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */