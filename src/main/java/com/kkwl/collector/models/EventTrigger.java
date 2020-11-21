  package  com.kkwl.collector.models;
  
  import com.kkwl.collector.models.EventTrigger;
  import java.sql.Timestamp;
  
  public class EventTrigger {
    private Long id = null;
    
    private String typeSn = null;
    private String typeParentIndex = null;
    
    private String param1 = null;
    private String param2 = null;
    private String param3 = null;
    private String param4 = null;
    
    private Timestamp firstSatisfyTime = null;
    
    private String lastReportId = null;
    private Timestamp lastReportTime = null;
  
  
    
    public EventTrigger() {}
  
    
    public EventTrigger(Long id, String typeSn, String typeParentIndex, String param1, String param2, String param3, String param4, String lastReportId, Timestamp lastReportTime) {
      this.id = id;
      this.typeSn = typeSn;
      this.typeParentIndex = typeParentIndex;
      this.param1 = param1;
      this.param2 = param2;
      this.param3 = param3;
      this.param4 = param4;
      this.lastReportId = lastReportId;
      this.lastReportTime = lastReportTime;
    }
  
  
    
    public Long getId() { return this.id; }
  
  
    
    public void setId(Long id) { this.id = id; }
  
  
    
    public String getTypeSn() { return this.typeSn; }
  
    
    public void setTypeSn(String typeSn) { this.typeSn = typeSn; }
  
    
    public String getTypeParentIndex() { return this.typeParentIndex; }
  
    
    public void setTypeParentIndex(String typeParentIndex) { this.typeParentIndex = typeParentIndex; }
  
    
    public String getParam1() { return this.param1; }
  
    
    public void setParam1(String param1) { this.param1 = param1; }
  
    
    public String getParam2() { return this.param2; }
  
    
    public void setParam2(String param2) { this.param2 = param2; }
  
    
    public String getParam3() { return this.param3; }
  
    
    public void setParam3(String param3) { this.param3 = param3; }
  
    
    public String getParam4() { return this.param4; }
  
    
    public void setParam4(String param4) { this.param4 = param4; }
  
  
    
    public String getLastReportId() { return this.lastReportId; }
  
  
    
    public void setLastReportId(String lastReportId) { this.lastReportId = lastReportId; }
  
  
    
    public Timestamp getLastReportTime() { return this.lastReportTime; }
  
    
    public void setLastReportTime(Timestamp lastReportTime) { this.lastReportTime = lastReportTime; }
  
  
    
    public Timestamp getFirstSatisfyTime() { return this.firstSatisfyTime; }
  
  
    
    public void setFirstSatisfyTime(Timestamp firstSatisfyTime) { this.firstSatisfyTime = firstSatisfyTime; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\EventTrigger.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */