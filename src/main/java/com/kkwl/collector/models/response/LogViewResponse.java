  package  com.kkwl.collector.models.response;
  
  import com.kkwl.collector.models.response.LogViewResponse;
  
  
  ///接受到的数据封装    设备sn|ip|port|action|target|time|content
  public class LogViewResponse
  {
    private String deviceSn;
    private String ipAddress;
    private String port;
    
    public String getDeviceSn() { return this.deviceSn; }
    private String action;
    private String target;
    private String time;
    private String content;
    
    public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }
  
  
    
    public String getIpAddress() { return this.ipAddress; }
  
  
    
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
  
  
    
    public String getPort() { return this.port; }
  
  
    
    public void setPort(String port) { this.port = port; }
  
  
    
    public String getTarget() { return this.target; }
  
  
    
    public void setTarget(String target) { this.target = target; }
  
  
    
    public String getTime() { return this.time; }
  
  
    
    public void setTime(String time) { this.time = time; }
  
  
    
    public String getContent() { return this.content; }
  
  
    
    public void setContent(String content) { this.content = content; }
  
  
  
    
    public String getAction() { return this.action; }
  
  
    
    public void setAction(String action) { this.action = action; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\response\LogViewResponse.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */