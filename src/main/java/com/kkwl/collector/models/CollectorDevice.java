  package  com.kkwl.collector.models;
  
  import com.kkwl.collector.models.CollectorDevice;
  import com.kkwl.collector.models.DataSection;
  import java.sql.Timestamp;
  import java.util.List;
  

  public class CollectorDevice
  {
    private String sn;
    private String name;
    private String dtuSn;
    private String dtuAccessType;
    private String protocolType;  //协议类型
    private String protocolParams;
    private Byte reportConnectionAlarm;
    private Short connectionTimeoutDuration;
    private Integer status;
    private String statusName;
    private String parentIndex;
    private Byte deviceBeingUsed;
    private String address;
    private String collectorIpAddress;
    private String collectorPort;
    private List<DataSection> dataSections;
    private String lastOfflineReportId = null;
    private Timestamp lastOfflineReportTime = null;
  
  
  
  
    
    public String getSn() { return this.sn; }
  
  
  
  
  
    
    public void setSn(String sn) { this.sn = sn; }
  
  
  
  
  
    
    public String getName() { return this.name; }
  
  
  
  
  
    
    public void setName(String name) { this.name = name; }
  
  
  
  
  
    
    public String getDtuSn() { return this.dtuSn; }
  
  
  
  
  
    
    public void setDtuSn(String dtuSn) { this.dtuSn = dtuSn; }
  
  
  
  
  
    
    public String getProtocolType() { return this.protocolType; }
  
  
  
  
  
    
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
  
  
  
  
  
    
    public String getProtocolParams() { return this.protocolParams; }
  
  
  
  
  
    
    public void setProtocolParams(String protocolParams) { this.protocolParams = protocolParams; }
  
  
  
  
  
    
    public Short getConnectionTimeoutDuration() { return this.connectionTimeoutDuration; }
  
  
  
  
  
    
    public void setConnectionTimeoutDuration(Short connectionTimeoutDuration) { this.connectionTimeoutDuration = connectionTimeoutDuration; }
  
  
  
  
  
    
    public Integer getStatus() { return this.status; }
  
  
  
  
  
    
    public String getStatusName() { return this.statusName; }
  
  
  
  
  
    
    public void setStatusName(String statusName) { this.statusName = statusName; }
  
  
  
  
  
    
    public String getParentIndex() { return this.parentIndex; }
  
  
  
  
  
    
    public void setParentIndex(String parentIndex) { this.parentIndex = parentIndex; }
  
  
  
  
  
    
    public Byte getReportConnectionAlarm() { return this.reportConnectionAlarm; }
  
  
    
    public void setReportConnectionAlarm(Byte reportConnectionAlarm) { this.reportConnectionAlarm = reportConnectionAlarm; }
  
  
  
  
  
    
    public void setStatus(Integer status) { this.status = status; }
  
  
  
  
  
    
    public String getAddress() { return this.address; }
  
  
  
  
  
    
    public void setAddress(String address) { this.address = address; }
  
  
    
    public Byte getDeviceBeingUsed() { return this.deviceBeingUsed; }
  
  
    
    public void setDeviceBeingUsed(Byte deviceBeingUsed) { this.deviceBeingUsed = deviceBeingUsed; }
  
  
  
  
  
    
    public String getCollectorIpAddress() { return this.collectorIpAddress; }
  
  
  
  
  
    
    public void setCollectorIpAddress(String collectorIpAddress) { this.collectorIpAddress = collectorIpAddress; }
  
  
  
  
  
    
    public String getCollectorPort() { return this.collectorPort; }
  
  
  
  
  
    
    public void setCollectorPort(String collectorPort) { this.collectorPort = collectorPort; }
  
  
  
    
    public List<DataSection> getDataSections() { return this.dataSections; }
  
  
    
    public void setDataSections(List<DataSection> dataSections) { this.dataSections = dataSections; }
  
  
  
    
    public String getDtuAccessType() { return this.dtuAccessType; }
  
  
    
    public void setDtuAccessType(String dtuAccessType) { this.dtuAccessType = dtuAccessType; }
  
  
    
    public String getLastOfflineReportId() { return this.lastOfflineReportId; }
  
  
    
    public void setLastOfflineReportId(String lastOfflineReportId) { this.lastOfflineReportId = lastOfflineReportId; }
  
  
    
    public Timestamp getLastOfflineReportTime() { return this.lastOfflineReportTime; }
  
  
    
    public void setLastOfflineReportTime(Timestamp lastOfflineReportTime) { this.lastOfflineReportTime = lastOfflineReportTime; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\CollectorDevice.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */