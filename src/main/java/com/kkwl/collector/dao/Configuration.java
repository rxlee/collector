package  com.kkwl.collector.dao;

import com.kkwl.collector.models.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

@Component
public interface Configuration {
  Integer getStationCount();
  
  List<CollectorDtu> getDtus(Map<String, Object> paramMap);  //通道

  List<CollectorDevice> getDevices(Map<String, Object> paramMap);  //设备
  
  List<DeviceVariable> getDeviceVariables(Map<String, Object> paramMap);  //获得设备变量
  
  List<CollectorRegisterArea> getDeviceRegisterMapList(Map<String, Object> paramMap);  //设备寄存器MAP
  
  List<String> getDeviceStationSns(@Param("collector_device_sns") List<String> paramList); //站点sn
  
  List<String> getDeviceSnsInStation(@Param("station_sn") String paramString);
  
  List<EventType> getEventTypes();
  
  List<EventType> getEventTypesByParentIndex(@Param("parent_index") String paramString);
  
  void updateEventTriggerLastReportInfo(@Param("id") Long paramLong, @Param("last_report_id") String paramString, @Param("last_report_time") Timestamp paramTimestamp);
  
  void createDayHistoryTable(@Param("table_name") String paramString1, @Param("key") String paramString2);
  
  void createMonthHistoryTable(@Param("table_name") String paramString1, @Param("key") String paramString2);
  
  void insertRecords(Map<String, Object> paramMap);
  
  void storeDayValues(Map<String, Object> paramMap);
  
  void storeSingleDayValue(Map<String, Object> paramMap);
  
  void updateSingleDayValue(Map<String, Object> paramMap);
  
  void updateDayValues(Map<String, Object> paramMap);
  
  Integer getDayValueCount(Map<String, Object> paramMap);
  
  List<Map<String, Object>> getDayValueMap(Map<String, Object> paramMap);
  
  List<String> getDayValueTableExistDeviceVariableSns(Map<String, Object> paramMap);
  
  Map<String, Object> getDeviceOfflineInfo(Map<String, Object> paramMap);
  
  void insertUnprocessedData(Map<String, Object> paramMap);
  
  void updateCollectorDtuStatus(Map<String, Object> paramMap);
  
  void updateCollectorDeviceStatus(Map<String, Object> paramMap);
  
  void storeFaultRecordData(Map<String, Object> paramMap);
  //获取站点电费配置
  StationCharge getStationCharge(StationCharge stationCharge);
}


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\dao\Configuration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */