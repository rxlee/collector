  package  com.kkwl.collector.models;
  
  import com.kkwl.collector.models.DataSection;
  //数据区
  public class DataSection {
    private final String name; //
    private final Integer start;
    private final Integer length;
    private final String type;

    //名字，起始位置，长度，类型(YC/YX)
    public DataSection(String name, Integer start, Integer length, String type) { //名字，起始位置，长度，类型YC/YX
      this.name = name;
      this.start = start;
      this.length = length;
      this.type = type;
    }

    public String getName() { return this.name; }
  
  
    
    public Integer getStart() { return this.start; }
  
  
    
    public Integer getLength() { return this.length; }
  
  
    
    public String getType() { return this.type; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\DataSection.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */