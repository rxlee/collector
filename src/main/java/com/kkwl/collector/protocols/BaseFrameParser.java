  package  com.kkwl.collector.protocols;
  
  import com.kkwl.collector.protocols.BaseFrameParser;
  import java.util.HashMap;
  import java.util.Map;
  //基本帧解析器
  public abstract class BaseFrameParser {
    private String name;
    
    public BaseFrameParser(String name) { this.name = name; }

    public Map<String, Object> parseBytesFrame(byte[] frameBytes, Map<String, Object> params) { return new HashMap(); }

    public Map<String, Object> parseStringFrame(String frameData, Map<String, Object> params) { return new HashMap(); }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\BaseFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */