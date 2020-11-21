package com.kkwl.collector.services;

import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.utils.NumberUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.collections.map.HashedMap;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class DataStorageService
{
  private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);
  private static final int BATCH_REDIS_INSERT_UPDATE_COUNT = 5000;
  private static final int BATCH_MYSQL_INSERT_UPDATE_COUNT = 2000;
  private static final LinkedBlockingQueue<Triplet<String, String, String>> realTimeRecords = new LinkedBlockingQueue(); ///实时记录，到redis
  private static final LinkedBlockingQueue<Pair<String, String>> toBeDeletedRecords = new LinkedBlockingQueue(); ///要被删除的记录
  private static final LinkedBlockingQueue<Triplet<String, String, String>> cachedRealTimeRecords = new LinkedBlockingQueue();
  private static final LinkedBlockingQueue<MonthTableRecord> monthTableRecords = new LinkedBlockingQueue();
  private static final LinkedBlockingQueue<DayTableRecord> dayTableRecords = new LinkedBlockingQueue(); //日表记录值
  private static final int VAR_COUNTS_PER_THREAD = 5000;
  private int currentIdx = 0;
  
  @Autowired
  private Configuration configuration;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  //创建历史表（天）
  public void createDayHistoryTable(int offDay) {
    String tableName = "day" + LocalDate.now().plusDays(offDay).format(GlobalVariables.GLOBAL_HISTORY_DAY_FORMATTER); ///日表名
    String key = tableName + "_VARIANTNAME";
    this.configuration.createDayHistoryTable(tableName, key);
  }
  //创建历史表（月）
  public void createMonthHistoryTable(int offMon) {
    String tableName = "mon" + LocalDate.now().plusMonths(offMon).format(GlobalVariables.GLOBAL_HISTORY_MONTH_FORMATTER);///月表名
    String key = tableName + "_variantname";
    this.configuration.createMonthHistoryTable(tableName, key);
  }
  //按周期报告全局设备变量数据存入dayTableRecords  5分钟/15分钟/30分钟/1小时
  public void periodReportData(String period) {
    Instant now = Instant.now();
    LocalDateTime nowDateTime = now.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDateTime();
    LocalDateTime yesterday = nowDateTime.minusDays(1L);
    
    logger.debug("Periodical task service type = " + period + " current time is " + nowDateTime
        .format(GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER) + ", save data to mysql start.");

    try {
      //遍历全局设备变量，获得日数据记录
      for (DeviceVariable deviceVariable : GlobalVariables.getDeviceVariables()) {
        if (deviceVariable.getRecordPeriod() == null || !deviceVariable.getRecordPeriod().equals(period)) {
          continue;
        }
         ///状态量不存
        if (deviceVariable.getType() == null || deviceVariable.getType().equals("Digital")) {
          continue;
        }

        if (deviceVariable.isInitial()) {
          continue;
        }

        if (deviceVariable.getCollectorDeviceSn() != null)
        {
          if (((BaseBusinessDevice)GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.get(deviceVariable.getCollectorDeviceSn())).getStatus() == StatusType.OFFLINE)///如果掉线
          {
            if (deviceVariable.getVarCode() == null || !GlobalVariables.GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.contains(deviceVariable.getVarCode())) {
              continue;
            }
          }
        }

        int posInMemory = deviceVariable.getPosInMemory(); //内存
        byte[] valueBytes = new byte[4];  ///每个值4个字节
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 1, valueBytes, 0, 4);   //
        
        float value = Float.intBitsToFloat(NumberUtil.bytes2int(valueBytes));//字节 转浮点型 值从全局内存复制字节中相应位置获得
        
        DayTableRecord dayTableRecord = new DayTableRecord();///日记录表
        dayTableRecord.setTableName("day" + nowDateTime.format(GlobalVariables.GLOBAL_HISTORY_DAY_FORMATTER));  ///表名
        dayTableRecord.setVariantName(deviceVariable.getSn());
        dayTableRecord.setValue((float)(Math.round(value * 1000.0D) / 1000.0D));///保留三位小数
        dayTableRecord.setTime(nowDateTime.format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER)); ///时间
        dayTableRecords.add(dayTableRecord);  //******************日数据记录********************
        //凌晨12点时，取前一天的日期
        if (nowDateTime.getHour() == 0 && nowDateTime.getMinute() == 0)
        {
          dayTableRecord = new DayTableRecord();
          dayTableRecord.setTableName("day" + yesterday.format(GlobalVariables.GLOBAL_HISTORY_DAY_FORMATTER));
          dayTableRecord.setVariantName(deviceVariable.getSn());
          dayTableRecord.setValue((float)(Math.round(value * 1000.0D) / 1000.0D));
          dayTableRecord.setTime(nowDateTime.format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER));
          dayTableRecords.add(dayTableRecord); ///
        } 

        if (deviceVariable.getAccumulation() != null) {
          boolean needStore = false;
          
          if (deviceVariable.getAccumulation().equals("hour") && nowDateTime.getMinute() == 0) {
            Float baseValue = Float.valueOf(value + deviceVariable.getAccumulationBase());
            GlobalVariables.getDeviceVariableBySn(deviceVariable.getSn()).setAccumulationBase(baseValue.floatValue());
            needStore = true;
          } else if (deviceVariable.getAccumulation().equals("day") && nowDateTime.getMinute() == 0 && nowDateTime.getHour() == 0) {
            Float baseValue = Float.valueOf(value + deviceVariable.getAccumulationBase());
            GlobalVariables.getDeviceVariableBySn(deviceVariable.getSn()).setAccumulationBase(baseValue.floatValue());
            needStore = true;
          } 
          
          if (needStore) {
            
            String[] arr = deviceVariable.getSn().split("__");
            String hashName = arr[0];
            String key = arr[1];
            String accumulationBaseStr = String.format("%f", new Object[] { Float.valueOf(GlobalVariables.getDeviceVariableBySn(deviceVariable.getSn()).getAccumulationBase()) });
            GlobalVariables.GLOBAL_REDIS_SERVCIE.putCachedValue(hashName, key, accumulationBaseStr); ///把累计值存到缓存
            
            String timeKey = arr[1] + "_update_time";
            String updateTime = nowDateTime.format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER);
            GlobalVariables.GLOBAL_REDIS_SERVCIE.putCachedValue(hashName, timeKey, updateTime);  ///把时间值存到缓存
          } 
        } 
      } 
    } catch (Exception e) {
      logger.error("Periodical task service error occured when get updated values.", e);
    } 
  }
 //存储所有变量 日统计值 init中整点或者59分时调用
  public void storeDayStatics() {
    if (GlobalVariables.getDeviceVariables().isEmpty()) {
      return;
    }
      //本地日期
       LocalDate targetDay = LocalDate.now();
      Instant currentInstant = Instant.now();//当前时间
      LocalDateTime nowDateTime = currentInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDateTime();//将当前时间转化为本地时间
     //如果当前时间是凌晨0点0分
      if (nowDateTime.getHour() == 0 && nowDateTime.getMinute() == 0) {
        targetDay = LocalDate.now().minusDays(1L);//获取前一天
      }
    //表名： mon+月数 mon201912
    String tableName = "mon" + targetDay.format(GlobalVariables.GLOBAL_HISTORY_MONTH_FORMATTER);
    int dayOfMonth = targetDay.getDayOfMonth();//当月天数

    try {
      List<DeviceVariable> deviceVariables = GlobalVariables.getDeviceVariables();
      for (DeviceVariable deviceVariable : deviceVariables) { //遍历所有变量
        if (deviceVariable == null) {
          logger.warn("Periodical task service when get device variable.");
          continue;
        } 
        if (deviceVariable.getType() == null || deviceVariable.getType().equals("Digital")) {
          continue;
        }
        if (deviceVariable.isInitial()) {
          continue;
        }
        //获取内存中的地址
        int posInMemory = deviceVariable.getPosInMemory();
        //将全局变量的值 在postMemory*xx+xx的位置取8个放在数组中
        byte[] maxValueTimeLongBytes = new byte[8];
        byte[] minValueTimeLongBytes = new byte[8];
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 25, maxValueTimeLongBytes, 0, 8); //最大值时间
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 41, minValueTimeLongBytes, 0, 8); //最小值时间
        
        long maxValueTimeLong = NumberUtil.bytesToLong(maxValueTimeLongBytes); ///转长整型
        long minValueTimeLong = NumberUtil.bytesToLong(minValueTimeLongBytes);
        Instant maxValueInstant = Instant.ofEpochMilli(maxValueTimeLong);//获取一个从1970-01-01T00:00:00Z开始的毫秒的Instant实例。
        Instant minValueInstant = Instant.ofEpochMilli(minValueTimeLong);//获取一个从1970-01-01T00:00:00Z开始的毫秒的Instant实例。
        LocalDate maxValueDate = maxValueInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDate();
        LocalDate minValueDate = minValueInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDate();//转换成本地时间
        
        if (!maxValueDate.isEqual(targetDay) || !minValueDate.isEqual(targetDay)) {
          continue;
        }
        //最大值/时间/最小值/时间/平均值/差值 GLOBAL_MEMORY_BYTES_COPY每5分钟由更新GLOBAL_MEMORY_BYTES一次，处理模拟量时判断更新80字节具体内容
        byte[] maxValueBytes = new byte[4];  //最大值
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 17, maxValueBytes, 0, 4);
        float maxValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(maxValueBytes)); ///转浮点型
        
        byte[] minValueBytes = new byte[4];  //最小值
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 33, minValueBytes, 0, 4);
        float minValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(minValueBytes));
        
        byte[] averageValueBytes = new byte[4];  //平均值
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 49, averageValueBytes, 0, 4);
        float averageValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(averageValueBytes));
        
        byte[] diffValueBytes = new byte[4];  //差值
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 57, diffValueBytes, 0, 4);
        float diffValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(diffValueBytes));
        
        MonthTableRecord monthTableRecord = new MonthTableRecord();
        monthTableRecord.setTableName(tableName);
        monthTableRecord.setDayOff(dayOfMonth);
        monthTableRecord.setVariantName(deviceVariable.getSn());
        monthTableRecord.setMaxValue((float)(Math.round(maxValue * 1000.0D) / 1000.0D));
        monthTableRecord.setMaxValueTime((maxValueTimeLong - currentInstant.toEpochMilli() <= 0L) ? GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER
            .format(maxValueInstant) : nowDateTime
            .format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER));
        monthTableRecord.setMinValue((float)(Math.round(minValue * 1000.0D) / 1000.0D));
        monthTableRecord.setMinValueTime((minValueTimeLong - currentInstant.toEpochMilli() <= 0L) ? GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER
            .format(minValueInstant) : nowDateTime
            .format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER));
        monthTableRecord.setAverageValue((deviceVariable.getCount() == 0) ? 0.0F : 
            (float)(Math.round(averageValue * 1000.0D) / 1000.0D));
        monthTableRecord.setCount(deviceVariable.getCount());
        monthTableRecord.setAccuValue((float)(Math.round(diffValue * 1000.0D) / 1000.0D));
        monthTableRecords.add(monthTableRecord);
      } 
    } catch (Exception e) {
      logger.error("Periodical task service error occured when insert statistic data", e);
    } 
  }
  //存储具体一变量日统计值
  public void storeVarDayStatics(DeviceVariable deviceVariable) {
    if (deviceVariable == null) {
      return;
    }
    if (deviceVariable.getType() == null || deviceVariable.getType().equals("Digital")) {//状态量返回
      return;
    }
    
    if (deviceVariable.isInitial()) {
      return;
    }

    
    LocalDate targetDay = LocalDate.now();
    Instant currentInstant = Instant.now();
    LocalDateTime nowDateTime = currentInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDateTime();
    
    if (nowDateTime.getHour() == 0 && nowDateTime.getMinute() == 0) {
      targetDay = LocalDate.now().minusDays(1L);
    }

    
    String tableName = "mon" + targetDay.format(GlobalVariables.GLOBAL_HISTORY_MONTH_FORMATTER); //月表名
    int dayOfMonth = targetDay.getDayOfMonth(); //天

    
    try {
      int posInMemory = deviceVariable.getPosInMemory();

      
      byte[] maxValueTimeLongBytes = new byte[8];
      byte[] minValueTimeLongBytes = new byte[8];
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 25, maxValueTimeLongBytes, 0, 8); //最大值时间
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 41, minValueTimeLongBytes, 0, 8); //最小值时间
      
      long maxValueTimeLong = NumberUtil.bytesToLong(maxValueTimeLongBytes);
      long minValueTimeLong = NumberUtil.bytesToLong(minValueTimeLongBytes);
      Instant maxValueInstant = Instant.ofEpochMilli(maxValueTimeLong);
      Instant minValueInstant = Instant.ofEpochMilli(minValueTimeLong);
      LocalDate maxValueDate = maxValueInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDate();
      LocalDate minValueDate = minValueInstant.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID).toLocalDate();
      
      if (!maxValueDate.isEqual(targetDay) || !minValueDate.isEqual(targetDay)) {
        return;
      }

      
      byte[] maxValueBytes = new byte[4];
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 17, maxValueBytes, 0, 4);
      float maxValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(maxValueBytes)); //最大值
      
      byte[] minValueBytes = new byte[4];
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 33, minValueBytes, 0, 4);
      float minValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(minValueBytes)); //最小值
      
      byte[] averageValueBytes = new byte[4];
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 49, averageValueBytes, 0, 4);
      float averageValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(averageValueBytes)); //平均值

      byte[] diffValueBytes = new byte[4];
      System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, posInMemory * 80 + 57, diffValueBytes, 0, 4);
      float diffValue = Float.intBitsToFloat(NumberUtil.byte4ToInt(diffValueBytes)); //差值
      
      MonthTableRecord monthTableRecord = new MonthTableRecord(); //每个变量每天一条数据
      monthTableRecord.setTableName(tableName);
      monthTableRecord.setDayOff(dayOfMonth);
      monthTableRecord.setVariantName(deviceVariable.getSn());
      monthTableRecord.setMaxValue((float)(Math.round(maxValue * 1000.0D) / 1000.0D));
      monthTableRecord.setMaxValueTime((maxValueTimeLong - currentInstant.toEpochMilli() <= 0L) ? GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER
          .format(maxValueInstant) : nowDateTime
          .format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER));
      monthTableRecord.setMinValue((float)(Math.round(minValue * 1000.0D) / 1000.0D));
      monthTableRecord.setMinValueTime((minValueTimeLong - currentInstant.toEpochMilli() <= 0L) ? GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER
          .format(minValueInstant) : nowDateTime
          .format(GlobalVariables.GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER));
      monthTableRecord.setAverageValue((deviceVariable.getCount() == 0) ? 0.0F : 
          (float)(Math.round(averageValue * 1000.0D) / 1000.0D));
      monthTableRecord.setCount(deviceVariable.getCount());
      monthTableRecord.setAccuValue((float)(Math.round(diffValue * 1000.0D) / 1000.0D));
      monthTableRecords.add(monthTableRecord);
    } catch (Exception e) {
      logger.error("Periodical task service error occured when insert statistic data", e);
    } 
  }
   //加载日数据值的MAP 月表中的值
  public Map<String, Map<String, Object>> loadDayValueMap() {
    LocalDate today = LocalDate.now();
    String tableName = "mon" + today.format(GlobalVariables.GLOBAL_HISTORY_MONTH_FORMATTER); //月表
    int dayOff = today.getDayOfMonth(); //月的天值
    
    Map<String, Object> filter = new HashMap<String, Object>();
    filter.put("table_name", tableName);
    filter.put("day_off", Integer.valueOf(dayOff));
    
    Map<String, Map<String, Object>> retMap = new HashMap<String, Map<String, Object>>();
    List<Map<String, Object>> dayValueMapList = this.configuration.getDayValueMap(filter); //查库获得dayValueMapList
    for (Map<String, Object> dayValueMap : dayValueMapList) { //遍历每一天日数据（月表中的每一条数据最大值/时间/最小值/时间/平均值/差值。。。）
      String sn = (String)dayValueMap.get("variantname");
      retMap.put(sn, dayValueMap); //
    }
    return retMap;
  }

  public void updateCollectorDtuStatus(Map<String, Object> info) { this.configuration.updateCollectorDtuStatus(info); }

  public void updateCollectorDeviceStatus(Map<String, Object> info) { this.configuration.updateCollectorDeviceStatus(info); }

  //解析变量并存缓存（内存）
  public void saveVariableToRedis(String variableSn, String value) {
    String[] arr = variableSn.split("__");
    if (arr.length != 2) {
      logger.warn("Data parser service variable sn = " + variableSn + " doesn't have correct format.");
      return;
    } 
    String hash = arr[0];
    String key = arr[1];
    addValueToReidsCache(hash, key, value);
  }
  ///从redis中删除变量
  public void delVariableFromRedis(String variableSn) {
    String[] arr = variableSn.split("__");
    if (arr.length != 2) {
      logger.warn("Data parser service variable sn = " + variableSn + " doesn't have correct format.");
      return;
    } 
    String hash = arr[0];
    String key = arr[1];
    
    delValueFromRedisCache(hash, key);
  }
  //将实时变量及值添加到到缓存（内存）realTimeRecords
  public void addValueToReidsCache(String hashName, String key, String value) { //( hashName,变量key,变量value)
    Triplet<String, String, String> triplet = new Triplet<String, String, String>(hashName, key, value);
    realTimeRecords.add(triplet);
  }
  //从redis删除
  public void delValueFromRedisCache(String hashName, String key) {
    Pair<String, String> pair = new Pair<String, String>(hashName, key);
    toBeDeletedRecords.add(pair);
  }
  //批存数据到redis
  public void batchSaveValuesToRedis() {
    List<Triplet<String, String, String>> triplets = new LinkedList<Triplet<String, String, String>>();
    
    if (realTimeRecords.isEmpty()) {
      return;
    }
    
    for (int i = 0; i < realTimeRecords.size() && i < 5000 && !realTimeRecords.isEmpty(); i++) {
      Triplet<String, String, String> triplet = (Triplet)realTimeRecords.poll();
      triplets.add(triplet);
    } 
    
    if (!triplets.isEmpty()) {
      GlobalVariables.GLOBAL_REDIS_SERVCIE.batchPut(triplets); //调用服务 批量存redis
    }
    
    triplets = null;
  }
  //批量删除数据从redis
  public void batchDeleteValueFromRedis() {
    List<Pair<String, String>> pairs = new LinkedList<Pair<String, String>>();
    
    if (toBeDeletedRecords.isEmpty()) {
      return;
    }
    
    for (int i = 0; i < toBeDeletedRecords.size() && i < 5000 && !toBeDeletedRecords.isEmpty(); i++) {
      Pair<String, String> pair = (Pair)toBeDeletedRecords.poll();
      pairs.add(pair);
    } 
    
    if (!pairs.isEmpty()) {
      GlobalVariables.GLOBAL_REDIS_SERVCIE.batchDelete(pairs); //调用服务 批量删redis
    }
    
    pairs = null;
  }
  ///批量存入数据库
  public void batchSaveValuesToDB() {
    //存日数据
    Instant now = Instant.now();
    logger.debug("At " + GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(now) + ". There is " + dayTableRecords.size() + " day values to be saved.");
    
    if (!dayTableRecords.isEmpty()) {
      int thieTimeSize = dayTableRecords.size();
      Map<String, Map<String, Object>> dayValueMapByTableName = new HashMap<String, Map<String, Object>>();
      for (int i = 0; i < thieTimeSize && i < 2000; i++) { //小于记录条数
        DayTableRecord dayTableRecord = (DayTableRecord)dayTableRecords.poll();
        
        HashedMap hashedMap = new HashedMap();
        hashedMap.put("VARIANTNAME", dayTableRecord.getVariantName());
        hashedMap.put("TIME", dayTableRecord.getTime());
        hashedMap.put("DATA", Float.valueOf(dayTableRecord.getValue()));
        
        String tableName = dayTableRecord.getTableName(); //日表名
        if (dayValueMapByTableName.containsKey(tableName)) {
          List<Map<String, Object>> valueMapList = (List)((Map)dayValueMapByTableName.get(tableName)).get("valueMapList");
          valueMapList.add(hashedMap); //把日数据map添加到valueMapList中
        } else {
          HashedMap hashedMap1 = new HashedMap();
          
          List<Map<String, Object>> valueMapList = new ArrayList<Map<String, Object>>();
          valueMapList.add(hashedMap);
          
          hashedMap1.put("table_name", tableName);
          hashedMap1.put("valueMapList", valueMapList); //
          dayValueMapByTableName.put(tableName, hashedMap1); //dayValueMapByTableName map tableName：dayValueMap{"table_name":tableName,"valueMapList":valueMapList}
        } 
      } 
      
      for (String tableName : dayValueMapByTableName.keySet()) {
        Map<String, Object> dayValueMap = (Map)dayValueMapByTableName.get(tableName); //map  "table_name":tableName,"valueMapList":valueMapList
        this.configuration.insertRecords(dayValueMap); //!!!存日表数据库 dayValueMap{"table_name":tableName,"valueMapList":valueMapList}
      } 
    } 
    
    try {
      Thread.sleep(1000L);
    } catch (InterruptedException interruptedException) {}

    //存月数据
    now = Instant.now();
    logger.debug("At " + GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(now) + ". There is " + monthTableRecords.size() + " month values to be saved.");

    
    if (!monthTableRecords.isEmpty()) {
      HashedMap hashedMap = new HashedMap();
      int thisTimeSize = monthTableRecords.size();
      for (int i = 0; i < thisTimeSize && i < 2000; i++) {
        MonthTableRecord monthTableRecord = (MonthTableRecord)monthTableRecords.poll();
        
        String tableName = monthTableRecord.getTableName();
        final int dayOff = monthTableRecord.getDayOff();
        String key = tableName + "_" + dayOff;
        
        HashedMap hashedMap1 = new HashedMap();
        hashedMap1.put("variantname", monthTableRecord.getVariantName());
        hashedMap1.put("max_value", Float.valueOf(monthTableRecord.getMaxValue()));
        hashedMap1.put("max_value_time", monthTableRecord.getMaxValueTime());
        hashedMap1.put("min_value", Float.valueOf(monthTableRecord.getMinValue()));
        hashedMap1.put("min_value_time", monthTableRecord.getMinValueTime());
        hashedMap1.put("average_value", Float.valueOf(monthTableRecord.getAverageValue()));
        hashedMap1.put("count", Integer.valueOf(monthTableRecord.getCount()));
        hashedMap1.put("accu_value", Float.valueOf(monthTableRecord.getAccuValue()));
        
        if (hashedMap.containsKey(key)) {
          List<Map<String, Object>> valueMapList = (List)((Map)hashedMap.get(key)).get("valueMapList");
          valueMapList.add(hashedMap1);
        } else {
          HashedMap hashedMap2 = new HashedMap();
          
          List<Map<String, Object>> valueMapList = new ArrayList<Map<String, Object>>();
          valueMapList.add(hashedMap1);
          
          hashedMap2.put("table_name", tableName);
          hashedMap2.put("day_off", Integer.valueOf(dayOff));
          hashedMap2.put("valueMapList", valueMapList);
          hashedMap.put(key, hashedMap2);
        } 
      } 
      for (Object key : hashedMap.keySet()) {
        Map<String, Object> monthDayValueMap = (Map)hashedMap.get(key.toString());
        String tableName = (String)monthDayValueMap.get("table_name");
        final Integer dayOff = (Integer)monthDayValueMap.get("day_off");
        
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("table_name", tableName);
        filter.put("day_off", dayOff);
        List<String> existedDeviceVariableSns = this.configuration.getDayValueTableExistDeviceVariableSns(filter);
        
        List<Map<String, Object>> toBeInsertedList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> toBeUpdatedList = new ArrayList<Map<String, Object>>();
//        for (Map<String, Object> valueMap : monthDayValueMap.get("valueMapList")) 
        for (Map<String, Object> valueMap : (List<Map<String, Object>>)monthDayValueMap.get("valueMapList")) {
          String variantName = (String)valueMap.get("variantname");
          if (existedDeviceVariableSns.contains(variantName)) {
            toBeUpdatedList.add(valueMap); 
            continue;
          } 
          toBeInsertedList.add(valueMap);
        } 

        //更新月表数据sql
        if (!toBeUpdatedList.isEmpty()) {


          String sql = "UPDATE " + tableName + " SET max_value = ?, max_value_time = ?, min_value = ?, min_value_time = ?, average_value = ?, accu_value = ? WHERE variantname =? AND dayf = ?";
          
          this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter()
              {
                public void setValues(PreparedStatement pps, int i) throws SQLException {
                  Map<String, Object> valueMap = (Map)toBeUpdatedList.get(i);
                  pps.setFloat(1, ((Float)valueMap.get("max_value")).floatValue());
                  pps.setString(2, (String)valueMap.get("max_value_time"));
                  pps.setFloat(3, ((Float)valueMap.get("min_value")).floatValue());
                  pps.setString(4, (String)valueMap.get("min_value_time"));
                  pps.setFloat(5, ((Float)valueMap.get("average_value")).floatValue());
                  pps.setFloat(6, ((Float)valueMap.get("accu_value")).floatValue());
                  pps.setString(7, (String)valueMap.get("variantname"));
                  pps.setInt(8, dayOff.intValue());
                }

                public int getBatchSize() {
                  return toBeUpdatedList.size();
                }
              });
        } 
        //存月表数据
        if (!toBeInsertedList.isEmpty()) {
          monthDayValueMap.put("valueMapList", toBeInsertedList);
          this.configuration.storeDayValues(monthDayValueMap);
        } 
      } 
    } 
  }
  
  private Float getToBeStoredValue(Instant nowInstant, DeviceVariable deviceVariable) {
    long recordPeriodMilliSeconds = GlobalVariables.getPeriodMilliseconds(deviceVariable.getRecordPeriod()).longValue();
    
    if (deviceVariable.getRecordType() == null || deviceVariable.getRecordType().equals("实时值"))
      return Float.valueOf(deviceVariable.getRegisterValue()); 
    if (deviceVariable.getRecordType().equals("最大值")) {
      if (deviceVariable.getCatchedValues() == null || deviceVariable.getCatchedValues().isEmpty()) {
        return null;
      }
      Float value = null;
      for (int i = deviceVariable.getCatchedValues().size() - 1; i > 0; i--) {
        Pair<Instant, Float> pair = (Pair)deviceVariable.getCatchedValues().get(i);
        if (nowInstant.toEpochMilli() - ((Instant)pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
          break;
        }
        
        if (value == null || value.compareTo((Float)pair.getValue1()) < 0) {
          value = (Float)pair.getValue1();
        }
      } 
      
      if (value == null) {
        return null;
      }
      
      return Float.valueOf((float)(Math.round(value.floatValue() * 1000.0D) / 1000.0D));
    } 
    if (deviceVariable.getRecordType().equals("最小值")) {
      if (deviceVariable.getCatchedValues() == null || deviceVariable.getCatchedValues().isEmpty()) {
        return null;
      }
      Float value = null;
      for (int i = deviceVariable.getCatchedValues().size() - 1; i > 0; i--) {
        Pair<Instant, Float> pair = (Pair)deviceVariable.getCatchedValues().get(i);
        if (nowInstant.toEpochMilli() - ((Instant)pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
          break;
        }
        
        if (value == null || value.compareTo((Float)pair.getValue1()) > 0) {
          value = (Float)pair.getValue1();
        }
      } 
      
      if (value == null) {
        return value;
      }
      
      return Float.valueOf((float)(Math.round(value.floatValue() * 1000.0D) / 1000.0D));
    } 
    if (deviceVariable.getRecordType().equals("平均值")) {
      if (deviceVariable.getCatchedValues() == null || deviceVariable.getCatchedValues().isEmpty()) {
        return null;
      }
      Double sum = Double.valueOf(0.0D);
      Integer count = Integer.valueOf(0);
      for (int i = deviceVariable.getCatchedValues().size() - 1; i > 0; i--) {
        Pair<Instant, Float> pair = (Pair)deviceVariable.getCatchedValues().get(i);
        if (nowInstant.toEpochMilli() - ((Instant)pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
          break;
        }
        
        sum = Double.valueOf(sum.doubleValue() + ((Float)pair.getValue1()).floatValue());
        count = Integer.valueOf(count.intValue() + 1);
      } 
      
      if (count.intValue() == 0) {
        return null;
      }
      
      return Float.valueOf((float)(Math.round(sum.doubleValue() / count.intValue() * 1000.0D) / 1000.0D));
    } 
    if (deviceVariable.getRecordType().equals("差值")) {
      if (deviceVariable.getCatchedValues() == null || deviceVariable.getCatchedValues().isEmpty() || deviceVariable.getCatchedValues().size() == 1) {
        return null;
      }
      Pair<Instant, Float> startPair = null;
      Pair<Instant, Float> endPair = null;
      Pair<Instant, Float> pair = (Pair)deviceVariable.getCatchedValues().get(deviceVariable.getCatchedValues().size() - 1);
      if (nowInstant.toEpochMilli() - ((Instant)pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
        endPair = pair;

        
        for (int i = deviceVariable.getCatchedValues().size() - 1; i > 0; i++) {
          pair = (Pair)deviceVariable.getCatchedValues().get(i);
          if (nowInstant.toEpochMilli() - ((Instant)pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
            break;
          }
          startPair = pair;
        } 
      } 

      
      if (startPair == null || endPair == null) {
        return null;
      }
      
      return Float.valueOf((float)(Math.round((((Float)endPair.getValue1()).floatValue() - ((Float)startPair.getValue1()).floatValue()) * 1000.0D) / 1000.0D));
    } 
    
    return null;
  }

  
  private class MonthTableRecord
  {
    private String tableName;
    
    private int dayOff;
    
    private String variantName;
    
    private float maxValue;
    
    private String maxValueTime;
    
    private float minValue;
    
    private String minValueTime;
    
    private float averageValue;
    
    private int count;
    
    private float accuValue;

    
    private MonthTableRecord() {}
    
    public String getTableName() { return this.tableName; }


    
    public void setTableName(String tableName) { this.tableName = tableName; }


    
    public int getDayOff() { return this.dayOff; }


    
    public void setDayOff(int dayOff) { this.dayOff = dayOff; }


    
    public String getVariantName() { return this.variantName; }


    
    public void setVariantName(String variantName) { this.variantName = variantName; }


    
    public float getMaxValue() { return this.maxValue; }


    
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }


    
    public String getMaxValueTime() { return this.maxValueTime; }


    
    public void setMaxValueTime(String maxValueTime) { this.maxValueTime = maxValueTime; }


    
    public float getMinValue() { return this.minValue; }


    
    public void setMinValue(float minValue) { this.minValue = minValue; }


    
    public String getMinValueTime() { return this.minValueTime; }


    
    public void setMinValueTime(String minValueTime) { this.minValueTime = minValueTime; }


    
    public float getAverageValue() { return this.averageValue; }


    
    public void setAverageValue(float averageValue) { this.averageValue = averageValue; }


    
    public float getAccuValue() { return this.accuValue; }


    
    public void setAccuValue(float accuValue) { this.accuValue = accuValue; }


    
    public void setCount(int count) { this.count = count; }


    
    public int getCount() { return this.count; }
  }
  
  private class DayTableRecord {
    private String tableName;
    private String variantName;
    private String time;
    private float value;
    
    private DayTableRecord() {}
    
    public String getTableName() { return this.tableName; }


    
    public void setTableName(String tableName) { this.tableName = tableName; }


    
    public String getVariantName() { return this.variantName; }


    
    public void setVariantName(String variantName) { this.variantName = variantName; }


    
    public String getTime() { return this.time; }


    
    public void setTime(String time) { this.time = time; }


    
    public float getValue() { return this.value; }


    
    public void setValue(float value) { this.value = value; }
  }
}
