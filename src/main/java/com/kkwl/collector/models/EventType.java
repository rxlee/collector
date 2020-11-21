  package  com.kkwl.collector.models;
  import com.kkwl.collector.models.EventType;
  
  public class EventType {
    private String sn = null;
    
    private String parentIndex = null;
    
    private Integer levelCode = null;
    
    private String triggerTypeSn = null;
    
    private Integer triggerDelay = Integer.valueOf(0);
    
    private Integer autoCheckDelay = Integer.valueOf(0);
  
    
    public String getSn() { return this.sn; }
  
  
    
    public void setSn(String sn) { this.sn = sn; }
  
  
    
    public String getParentIndex() { return this.parentIndex; }
  
  
    
    public void setParentIndex(String parentIndex) { this.parentIndex = parentIndex; }
  
  
    
    public Integer getLevelCode() { return this.levelCode; }
  
  
    
    public void setLevelCode(Integer levelCode) { this.levelCode = levelCode; }
  
  
    
    public String getTriggerTypeSn() { return this.triggerTypeSn; }
  
  
    
    public void setTriggerTypeSn(String triggerTypeSn) { this.triggerTypeSn = triggerTypeSn; }
  
  
    
    public Integer getTriggerDelay() { return this.triggerDelay; }
  
  
    
    public void setTriggerDelay(Integer triggerDelay) { this.triggerDelay = triggerDelay; }
  
  
    
    public Integer getAutoCheckDelay() { return this.autoCheckDelay; }
  
  
    
    public void setAutoCheckDelay(Integer autoCheckDelay) { this.autoCheckDelay = autoCheckDelay; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\EventType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */