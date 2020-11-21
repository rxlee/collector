  package  com.kkwl.collector.devices.business;
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.common.LogType;
  import com.kkwl.collector.common.StatusType;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.devices.business.BaseClientDevice;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.protocols.BaseProtocol;
  import com.kkwl.collector.utils.LogTools;
  import java.sql.Timestamp;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.concurrent.locks.Lock;
  import java.util.concurrent.locks.ReentrantLock;
  import org.javatuples.Pair;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  public class BaseClientDevice extends BaseBusinessDevice {
    protected Map<String, DeviceVariable> deviceVariableMap = new HashMap(); //设备变量map
    protected static final Lock deviceVariableLock = new ReentrantLock();
    
    protected BaseProtocol protocol;
    private static final Logger logger = LoggerFactory.getLogger(BaseClientDevice.class);
  
  
    
    public BaseClientDevice(String sn, String dtuSn, String name, String parentIndex, String type, Byte deviceBeingUsed, Long handlingPeriod, byte shouldReportAlarm, short connectionTimeoutDuration, String offlineReportId, Timestamp offlineTime, StatusType initStatus) { super(sn, dtuSn, name, parentIndex, type, "", "", deviceBeingUsed, handlingPeriod.longValue(), shouldReportAlarm, connectionTimeoutDuration, offlineReportId, offlineTime, initStatus); }
  
  
  
    
    public void doBusiness() {}
  
  
  
    
    public void parseProtocolVariable() {}
  
  
  
    
    public void toggleFlag(FrameType frameType) {}
  
  
  
    
    public void initDataType() {}
  
  
  
    
    public void doParse() { LogTools.log(logger, this.sn, LogType.DEBUG, "Base mqtt device sn = " + this.sn + " empty doParse."); }
  
  
  
    
    public byte[] getSectionBytes(String sectionName, int pos, int length) { return null; }
  
  
  
    
    public byte[] getSectionBytesForDebug(String sectionName, int pos, int length) { return null; }
  
  
  
    
    public Byte getSectionByteForDebug(String sectionName, int pos) { return null; }
  
  
  
    
    public Byte getSectionByte(String sectionName, int pos) { return null; }
  
  
  
    
    public Pair<Long, Byte> getHistorySectionByte(String sectionName, int pos) { return null; }
  
  
  
    
    public Pair<Long, byte[]> getHistorySectionBytes(String sectionName, int pos, int length) { return null; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\business\BaseClientDevice.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */