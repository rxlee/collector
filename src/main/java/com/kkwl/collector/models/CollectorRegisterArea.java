  package  com.kkwl.collector.models;
  
  import com.kkwl.collector.models.CollectorRegisterArea;
  
  
  public class CollectorRegisterArea
  {
    private int id;
    private String name;
    private String collectorDeviceSn;
    private int start;
    private int length;
    private String type;
    
    public String getName() { return this.name; }
  
  
    
    public void setName(String name) { this.name = name; }
  
  
    
    public String getCollectorDeviceSn() { return this.collectorDeviceSn; }
  
  
    
    public void setCollectorDeviceSn(String collectorDeviceSn) { this.collectorDeviceSn = collectorDeviceSn; }
  
  
    
    public int getStart() { return this.start; }
  
  
    
    public void setStart(int start) { this.start = start; }
  
  
    
    public int getLength() { return this.length; }
  
  
    
    public void setLength(int length) { this.length = length; }
  
  
    
    public String getType() { return this.type; }
  
  
    
    public void setType(String type) { this.type = type; }
  
  
    
    public int getId() { return this.id; }
  
  
    
    public void setId(int id) { this.id = id; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\CollectorRegisterArea.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */