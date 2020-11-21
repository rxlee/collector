  package  com.kkwl.collector.devices;
  
  import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.RunMode;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.BaseDevice;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.business.BaseIEC104Device;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.services.AlarmReportService;
import com.kkwl.collector.services.DataStorageService;
import com.kkwl.collector.services.RedisService;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.LogTools;
import com.kkwl.collector.utils.TimeFormatter;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
  public abstract class BaseDevice
  {
    private static final Logger logger = LoggerFactory.getLogger(BaseDevice.class);
    
    protected String offlineReportId;
    protected Timestamp offlineTime;
    protected String sn; //设备sn
    protected String type;
    protected String name;
    protected String parentIndex;
    protected StatusType status;
    protected Instant lastReceivedPacketTime;
    protected short connectionTimeoutDuration;
    protected byte shouldReportConnectionAlarm;
    protected long handlingPeriod; //采集周期

    public String getOfflineReportId() {
		return offlineReportId;
	}

	public void setOfflineReportId(String offlineReportId) {
		this.offlineReportId = offlineReportId;
	}

	public Timestamp getOfflineTime() {
		return offlineTime;
	}

	public void setOfflineTime(Timestamp offlineTime) {
		this.offlineTime = offlineTime;
	}

	public byte getShouldReportConnectionAlarm() {
		return shouldReportConnectionAlarm;
	}

	public void setShouldReportConnectionAlarm(byte shouldReportConnectionAlarm) {
		this.shouldReportConnectionAlarm = shouldReportConnectionAlarm;
	}

	public long getHandlingPeriod() {
		return handlingPeriod;
	}

	public void setHandlingPeriod(long handlingPeriod) {
		this.handlingPeriod = handlingPeriod;
	}

	public String getName() {
		return name;
	}

	public String getParentIndex() {
		return parentIndex;
	}

	public Instant getLastReceivedPacketTime() {
		return lastReceivedPacketTime;
	}

	public void setConnectionTimeoutDuration(short connectionTimeoutDuration) {
		this.connectionTimeoutDuration = connectionTimeoutDuration;
	}

    public void setSn(String sn) { this.sn = sn; }

    public String getSn() { return this.sn; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public void setName(String name) { this.name = name; }

    public void setParentIndex(String parentIndex) { this.parentIndex = parentIndex; }

    public void setStatus(StatusType status) { this.status = status; }

    public StatusType getStatus() { return this.status; }

    protected void triggerOfflineAlarm(Timestamp currentTime) {
      AlarmReportService globalAlarmReportService = GlobalVariables.GLOBAL_ALARM_REPORT_SERVICE;
      
      if (this.offlineReportId == null || this.offlineReportId.isEmpty()) {
        
        this.offlineTime = currentTime;
        this.offlineReportId = Caculator.caculateUniqueTimeId();
        
        Map<String, Object> statusInfo = new HashMap<String, Object>();
        statusInfo.put("sn", this.sn);
        statusInfo.put("last_offline_report_id", this.offlineReportId);
        statusInfo.put("last_offline_report_time", TimeFormatter.timestampToCommonString(this.offlineTime));
        
        if (this.type.equals("DTU")) {
          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.updateCollectorDtuStatus(statusInfo);
        } else {
          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.updateCollectorDeviceStatus(statusInfo);
        } 
        
        globalAlarmReportService.handleDeviceStatusAlarm(this.sn, this.name, this.type, (byte)1, this.offlineReportId, null, this.offlineTime);
      } 
    }
    
    protected void dismissOfflineAlarm(Timestamp currentTime) {
      AlarmReportService globalAlarmReportService = GlobalVariables.GLOBAL_ALARM_REPORT_SERVICE;
      
      if (this.offlineReportId != null && !this.offlineReportId.isEmpty()) {
        
        globalAlarmReportService.handleDeviceStatusAlarm(this.sn, this.name, this.type, (byte)0, Caculator.caculateUniqueTimeId(), this.offlineReportId, currentTime);
        
        this.offlineReportId = "";
        this.offlineTime = null;
        
        Map<String, Object> statusInfo = new HashMap<String, Object>();
        statusInfo.put("sn", this.sn);
        statusInfo.put("last_offline_report_id", this.offlineReportId);
        statusInfo.put("last_offline_report_time", null);
        
        if (this.type.equals("DTU")) {
          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.updateCollectorDtuStatus(statusInfo);
        } else {
          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.updateCollectorDeviceStatus(statusInfo);
        } 
      } 
    }

    public short getConnectionTimeoutDuration() { return this.connectionTimeoutDuration; }

    public void setLastReceivedPacketTime(Instant instant) {
      this.lastReceivedPacketTime = instant;
      
      if (this.status == StatusType.OFFLINE) {
        this.status = StatusType.ONLINE;
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
        saveDeviceStatusToReids(TimeFormatter.timestampToCommonString(currentTime), "1");
        
        if (this.shouldReportConnectionAlarm == 1) {
          dismissOfflineAlarm(currentTime);
        }
        
        checkIfDeviceNeedHistoryData(instant);
        
        DataStorageService globalDataStorageService = GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE;
        
        Map<String, Object> statusInfo = new HashMap<String, Object>();
        statusInfo.put("sn", this.sn);
        statusInfo.put("online_time", TimeFormatter.timestampToCommonString(currentTime));
        statusInfo.put("update_time", TimeFormatter.timestampToCommonString(currentTime));
        statusInfo.put("status", Integer.valueOf(1));
        
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
          {
            public void run() {
              if (BaseDevice.this.type.equals("DTU")) {
                globalDataStorageService.updateCollectorDtuStatus(statusInfo);
              } else {
                globalDataStorageService.updateCollectorDeviceStatus(statusInfo);
              } 
            }
          });
      } 
    }
    //保存设备状态到redis
    protected void saveDeviceStatusToReids(String timestamp, String status) {
      RedisService globalRedisService = GlobalVariables.GLOBAL_REDIS_SERVCIE;
      try {
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
          {
            public void run() {
              Instant now = Instant.now();
              if (BaseDevice.this.type.equals("DTU")) { //如果基础设备类型为DTU
                if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER.value())) { //模式TCP_SERVER
                  for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
                    if (BaseDevice.this.sn.equals(dtu.getSn())) //如果设备Sn等于通道Sn ,取Sn
                    { globalRedisService.put("DTU__" + BaseDevice.this.sn, "COMMUNICATION_STATUS", status);
                      globalRedisService.put("DTU__" + BaseDevice.this.sn, "COMMUNICATION_REPORT_TIME", timestamp); } 
                  } 
                } else if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.MQTT_CLIENT.value()) || GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
                  .equals(RunMode.KAFKA_CLIENT.value())) { //模式MQTT_CLIENT /KAFKA_CLIENT
                  for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
                    if (BaseDevice.this.sn.equals(dtu.getSn())) {
                      globalRedisService.put("DTU__" + BaseDevice.this.sn, "COMMUNICATION_STATUS", status);
                      globalRedisService.put("DTU__" + BaseDevice.this.sn, "COMMUNICATION_REPORT_TIME", timestamp);
                    } 
                  } 
                } 
              }
              else { //如果基础设备类型不为"DTU"，取设备Sn
                for (BaseBusinessDevice device : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
                  if (BaseDevice.this.sn.equals(device.getSn())) {
                    globalRedisService.put("CD__" + BaseDevice.this.sn, "COMMUNICATION_STATUS", status);
                    globalRedisService.put("CD__" + BaseDevice.this.sn, "COMMUNICATION_REPORT_TIME", timestamp);
                  } 
                } 
              } 
            }
          });
  
      }
      catch (Exception ex) {
        LogTools.log(logger, this.sn, LogType.ERROR, "Base device sn = " + this.sn + " error occured when update device status = " + status);
      } 
    }
    
    public abstract void onDisconnected();
    
    public abstract void delete();
    
    public void checkOnlineOffline() {
      Instant currentInstant = Instant.now();
      Timestamp currentTime = Timestamp.from(currentInstant);
      DataStorageService globalDataStorageService = GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE;
      
      try {
        Map<String, Object> statusInfo = new HashMap<String, Object>();
        statusInfo.put("sn", this.sn);
        if (currentInstant.toEpochMilli() - this.lastReceivedPacketTime.toEpochMilli() > (this.connectionTimeoutDuration * 1000)) {
          
          if (this.status == StatusType.ONLINE) {
            
            statusInfo.put("offline_time", TimeFormatter.timestampToCommonString(currentTime));
            statusInfo.put("update_time", TimeFormatter.timestampToCommonString(currentTime));
            
            statusInfo.put("status", Integer.valueOf(0));
            if (this.shouldReportConnectionAlarm == 1) {
              triggerOfflineAlarm(currentTime);
            }
          } else {
            statusInfo.put("update_time", TimeFormatter.timestampToCommonString(currentTime));
            statusInfo.put("status", Integer.valueOf(0));
          } 
          this.status = StatusType.OFFLINE;
          saveDeviceStatusToReids(TimeFormatter.timestampToCommonString(currentTime), "0");
        } else {
          
          if (this.status == StatusType.OFFLINE) {
            statusInfo.put("online_time", TimeFormatter.timestampToCommonString(currentTime));
            statusInfo.put("update_time", TimeFormatter.timestampToCommonString(currentTime));
            statusInfo.put("status", Integer.valueOf(1));
            if (this.shouldReportConnectionAlarm == 1) {
              dismissOfflineAlarm(currentTime);
            }
            
            checkIfDeviceNeedHistoryData(currentInstant);
          } else {
            statusInfo.put("update_time", TimeFormatter.timestampToCommonString(currentTime));
            statusInfo.put("status", Integer.valueOf(1));
          } 
          this.status = StatusType.ONLINE;
          saveDeviceStatusToReids(TimeFormatter.timestampToCommonString(currentTime), "1");
        } 
        
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
          {
            public void run() {
              if (BaseDevice.this.type.equals("DTU")) {
                globalDataStorageService.updateCollectorDtuStatus(statusInfo);
              } else {
                globalDataStorageService.updateCollectorDeviceStatus(statusInfo);
              } 
            }
          });
  
      }
      catch (Exception e) {
        LogTools.log(logger, this.sn, LogType.ERROR, "Base business device sn = " + this.sn + " error occured when update device status.", e);
      } 
    }

    private void checkIfDeviceNeedHistoryData(Instant currentInstant) {
      if (this instanceof BaseIEC104Device) {
        BaseIEC104Device device = (BaseIEC104Device)this;
  
        
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("sn", this.sn);
        Map<String, Object> info = GlobalVariables.GLOBAL_DB_HANDLER.getDeviceOfflineInfo(param);
        Timestamp offlineTime = (Timestamp)info.get("offline_time");
        Instant historyBeginInstant = offlineTime.toInstant();
        Instant historyEndInstant = currentInstant;
        
        if (historyBeginInstant == null || Duration.between(historyBeginInstant, historyEndInstant).toMinutes() < 5L) {
          
          device.setNeedHistoryFlag(false);
          
          return;
        } 
        //device.setNeedHistoryFlag(true);  //设备是否需要历史数据
        device.setNeedHistoryFlag(false); //20200407 更改，取消历史数据总召
        device.setHistoryBeginInstant(historyBeginInstant);
        device.setHistoryEndInstant(historyEndInstant);
        
        LocalDateTime historyBeginDateTime = LocalDateTime.ofInstant(historyBeginInstant, GlobalVariables.GLOBAL_DEFAULT_ZONEID);
        if (historyBeginDateTime.getMinute() % 5 == 0) {
          historyBeginDateTime = historyBeginDateTime.minusSeconds(historyBeginDateTime.getSecond());
          device.setHistoryCurrentQueryInstant(historyBeginDateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant());
        } else {
          while (historyBeginDateTime.getMinute() % 5 != 0) {
            historyBeginDateTime = historyBeginDateTime.plusMinutes(1L);
          }
          historyBeginDateTime = historyBeginDateTime.minusSeconds(historyBeginDateTime.getSecond());
          device.setHistoryCurrentQueryInstant(historyBeginDateTime.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toInstant());
        } 
        device.setLastHistoryCalledTime(currentInstant);
      } 
    }

  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\BaseDevice.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */