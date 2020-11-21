  package  com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.protocols.BaseProtocol;
  import com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan.HGNYMqttProtocolAdapter;
  import com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan.MeterFrameParser;
  import com.kkwl.collector.utils.LogTools;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.concurrent.LinkedBlockingQueue;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  public class HGNYMqttProtocolAdapter extends BaseProtocol {
    private static final Logger logger = LoggerFactory.getLogger(HGNYMqttProtocolAdapter.class);
    
    public HGNYMqttProtocolAdapter() {
      super(3, "HGNYMQTT");
      
      MeterFrameParser meterFrameParser = new MeterFrameParser();
      this.parsers.add(meterFrameParser);
    }
  
    //消息队列解析
    public Map<String, Object> doStringParse(LinkedBlockingQueue<String> messages, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      if (messages.isEmpty()) {
        retMap.put("result", Boolean.valueOf(false));
        return retMap;
      } 
      
      String sn = (String)params.get("sn");
      String message = (String)messages.poll();
      
      LogTools.log(logger, sn, LogType.DEBUG, "HGNY mqtt parser begin parse " + message);
      
      for (BaseFrameParser parser : this.parsers) { //遍历帧解析器
        Map<String, Object> resultMap = parser.parseStringFrame(message, params); //********解析字符串帧**************
        if (((Boolean)resultMap.get("result")).equals(Boolean.valueOf(true))) {
          LogTools.log(logger, sn, LogType.DEBUG, "HGNY mqtt parser name = " + parser.getName() + " matched.");
          return resultMap;
        } 
      } 
      
      retMap.put("result", Boolean.valueOf(false));
      return retMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\Mqtt\HuaGongNengYuan\HGNYMqttProtocolAdapter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */