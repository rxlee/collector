  package  com.kkwl.collector.services;
  import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
  import java.util.*;

  import com.kkwl.collector.models.StationCharge;
  import io.netty.util.internal.StringUtil;
  import org.apache.commons.collections.map.HashedMap;
  import org.apache.kafka.common.protocol.types.Field;
  import org.javatuples.Pair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kkwl.collector.common.DataType;
import com.kkwl.collector.common.GlobalEventTypes;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.exception.IllegalExpressionException;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.models.EventTrigger;
import com.kkwl.collector.models.EventType;
import com.kkwl.collector.utils.AnalogExpressionCalculator;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.DigitalExpressionCalculator;
import com.kkwl.collector.utils.NumberUtil;
import com.kkwl.collector.utils.TimeFormatter;
  
  @Service
  public class DataParserService {
    private static final Logger logger = LoggerFactory.getLogger(DataParserService.class);

    @Autowired
    private EventTypesService eventTypesService;

    @Autowired
    private Configuration configuration;

    @Autowired
    private DataStorageService dataStorageService;

    @Value("${com.kkwl.collector.redis.expire_time}")
    private int expireTime;

    private static final int VAR_COUNTS_PER_THREAD = 5000; //变量的数量

    private Instant currentInstant; //当前时间
    private Timestamp currentTime;
    private long currentMilliSeconds;
    private byte[] timeBytes;
    private int currentIdx = 0;

    //解析设备变量  [线程池调用 处理模拟量/数字量]
    public void parseDeviceVariables() {
      this.currentInstant = Instant.now();
      this.currentMilliSeconds = this.currentInstant.toEpochMilli();
      this.currentTime = Timestamp.from(this.currentInstant);
      this.timeBytes = NumberUtil.longToByte8(this.currentInstant.toEpochMilli()); //当前8字节有毫秒时间

      int j = 0;
      for (; j < 5000; j++) {

        int varIdx = 5000 * this.currentIdx + j;
        if (varIdx >= GlobalVariables.getSameIndexComposedNames().size()) {
          break;
        }
        //通讯设备编码-数据区名称（YC/YX）-地址序号-长度  LXBE1|topic|key|4
        String sameDeviceSectionPos = (String) GlobalVariables.getSameIndexComposedNames().get(varIdx);  //"LXBE1|YC|100|4"
        String[] arr = sameDeviceSectionPos.split("\\|");
        String businessDeviceSn = arr[0]; //通讯设备编码
        if (!businessDeviceSn.equals("null")) {

          String sectionName = arr[1];  //数据区名称（YC/YX）
          int sectionIndex = Integer.parseInt(arr[2]); //地址序号
          int bytesLen = Integer.parseInt(arr[3]); //长度

          if (GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.containsKey(businessDeviceSn)) {
            BaseBusinessDevice businessDevice = (BaseBusinessDevice) GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.get(businessDeviceSn); //通讯设备
            List<DeviceVariable> deviceVariableList = (List) GlobalVariables.getSameIndexVariablesMap().get(sameDeviceSectionPos);  //变量对象 列表集合
            //长度为1，采用开关量处理
            if (bytesLen == 1)
            {
              Byte b = businessDevice.getSectionByte(sectionName, sectionIndex); //值 （YC 100）
              if (b == null) {
                delVariableFromRedis(sameDeviceSectionPos); //如果变量没有值为空，则从redis中删除 （LXBE1|YC|100|4）
              }
              else {
                for (DeviceVariable deviceVariable : deviceVariableList) {
                  Byte bitPos = deviceVariable.getBitPos();
                  int value = NumberUtil.getIntegerValue(b.byteValue(), (bitPos == null) ? 0 : bitPos.byteValue()); //右移 得到值
                  handleDigitalValue(deviceVariable, value, null);
                }
              }
              //从采集来的历史数据getHistorySectionByte(sectionName, sectionIndex)
              while (true) {
                Pair<Long, Byte> timeByte = businessDevice.getHistorySectionByte(sectionName, sectionIndex); //返回Pair<Long, Byte>
                if (timeByte == null) {
                  break;
                }

                for (DeviceVariable deviceVariable : deviceVariableList) {
                  Byte bitPos = deviceVariable.getBitPos(); //********************************位 位置***********************************
                  int value = NumberUtil.getIntegerValue(((Byte) timeByte.getValue1()).byteValue(), (bitPos == null) ? 0 : bitPos.byteValue()); //第一个参数右移第二个参数位 由字节 得到int型值
                  //处理数字量
                  handleDigitalValue(deviceVariable, value, (Long) timeByte.getValue0()); //(变量，int型值（0或1），时间)
                }
              }

              if (!businessDevice.getNeedHisgoryFlag()) {
                for (DeviceVariable deviceVariable : deviceVariableList) {
                  if (deviceVariable.getHistoryYXValues() != null && deviceVariable
                          .getHistoryYXValues() != null &&
                          !deviceVariable.getHistoryYXValues().isEmpty()) {

                    JSONObject unprocessedData = new JSONObject();
                    while (!deviceVariable.getHistoryYXValues().isEmpty()) {
                      Pair<Instant, Integer> historyPair = (Pair) deviceVariable.getHistoryYXValues().get(0);
                      if (((Instant) historyPair.getValue0()).isBefore(businessDevice.getHistoryBeginInstant()) || ((Instant) historyPair
                              .getValue0()).isAfter(businessDevice.getHistoryEndInstant())) {
                        continue;
                      }
                      unprocessedData.put(GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format((TemporalAccessor) historyPair.getValue0()), historyPair.getValue1());
                      deviceVariable.getHistoryYXValues().remove(0);
                    }

                    HashedMap hashedMap = new HashedMap();
                    hashedMap.put("var_sn", deviceVariable.getSn());
                    hashedMap.put("start_time", GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(businessDevice.getHistoryBeginInstant()));
                    hashedMap.put("end_time", GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(businessDevice.getHistoryEndInstant()));
                    hashedMap.put("data_js", unprocessedData.toString());
                    this.configuration.insertUnprocessedData(hashedMap);
                  }
                }
              }
            }
            else //长度大于1采用模拟量处理
            {
               byte[] bytes = businessDevice.getSectionBytes(sectionName, sectionIndex, bytesLen);  //*********变量的值 字节数组  （yc 100 4）
               if (bytes == null) {
                delVariableFromRedis(sameDeviceSectionPos); //如果变量没有值为空，则从redis中删除 （LXBE1|YC|100|4）
               } else {
                for (DeviceVariable deviceVariable : deviceVariableList) {
                  DataType dataType = DataType.getDataTypeByName(deviceVariable.getDataType()); //数据类型 4字节浮点2143
                  float value = NumberUtil.getFloatValue(bytes, dataType); //由字节 得到浮点型变量值
                  handleAnalogValue(deviceVariable, value, null); //***处理统计、存redis缓存、处理告警

                }
              }
              //***************从采集来的历史数据getHistorySectionByte(sectionName, sectionIndex)********************
               while (true) {
                 Pair<Long, byte[]> timeBytes = businessDevice.getHistorySectionBytes(sectionName, sectionIndex, bytesLen); //历史变量值：Pair 时间 值  (YC  120  4)
                 if (timeBytes == null) { //为空时跳出
                  break;
                }

                 for (DeviceVariable deviceVariable : deviceVariableList) {
                  DataType dataType = DataType.getDataTypeByName(deviceVariable.getDataType());//数据类型 4字节浮点2143
                  float value = NumberUtil.getFloatValue((byte[]) timeBytes.getValue1(), dataType); //由（值字节，类型） 得到浮点型 变量值
                  handleAnalogValue(deviceVariable, value, (Long) timeBytes.getValue0()); //******处理模拟量值（变量，浮点值，时间）
                 }
               }

              if (!businessDevice.getNeedHisgoryFlag()) {
                for (DeviceVariable deviceVariable : deviceVariableList) {
                  if (deviceVariable.getHistoryYCValues() != null && deviceVariable.getHistoryYCValues() != null && !deviceVariable.getHistoryYCValues().isEmpty()) {

                    JSONObject unprocessedData = new JSONObject();
                    while (!deviceVariable.getHistoryYCValues().isEmpty()) {
                      Pair<Instant, Float> historyPair = (Pair) deviceVariable.getHistoryYCValues().get(0);
                      if (((Instant) historyPair.getValue0()).isBefore(businessDevice.getHistoryBeginInstant()) || ((Instant) historyPair
                              .getValue0()).isAfter(businessDevice.getHistoryEndInstant())) {
                        continue;
                      }
                      unprocessedData.put(GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format((TemporalAccessor) historyPair.getValue0()), historyPair.getValue1());
                      deviceVariable.getHistoryYCValues().remove(0);
                    }

                    HashedMap hashedMap = new HashedMap();
                    hashedMap.put("var_sn", deviceVariable.getSn());
                    hashedMap.put("start_time", GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(businessDevice.getHistoryBeginInstant()));
                    hashedMap.put("end_time", GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(businessDevice.getHistoryEndInstant()));
                    hashedMap.put("data_js", unprocessedData.toString());
                    this.configuration.insertUnprocessedData(hashedMap);
                  }
                }
              }
            }
          }
        }
      }
      if (5000 * this.currentIdx + j == GlobalVariables.getSameIndexComposedNames().size())
      {
        for (DeviceVariable deviceVariable : GlobalVariables.getDeviceVariables()) {
          String expression = deviceVariable.getExpression();
          if (expression != null && !expression.isEmpty()) {
            if (deviceVariable.getType().equals("Digital")) {
              try {
                DigitalExpressionCalculator calculator = new DigitalExpressionCalculator(expression);
                if (!calculator.infixExpressionToPostFixExpression(this.currentMilliSeconds)) {
                  continue;
                }
                logger.debug("Data parser service variable sn = " + deviceVariable.getSn() + " expression = " + calculator.getExpression());

                calculator.doParseAndCalculate();
                Byte result = calculator.getResult();
                logger.debug("Data parser service variable sn = " + deviceVariable.getSn() + " result = " + result);

                if (result == null) {
                  continue;
                }

                handleDigitalValue(deviceVariable, result.byteValue(), null); //********
                continue;
              } catch (IllegalExpressionException ex) {
                if (!deviceVariable.isHasBeenDeleted()) {
                  this.dataStorageService.delVariableFromRedis(deviceVariable.getSn());
                  deviceVariable.setHasBeenDeleted(true);
                }
                continue;
              }
            }
            if (deviceVariable.getType().equals("Analog")) {
              try {
                AnalogExpressionCalculator calculator = new AnalogExpressionCalculator(expression);
                if (!calculator.infixExpressionToPostFixExpression(this.currentMilliSeconds)) {
                  continue;
                }
                logger.debug("Data parser service variable sn = " + deviceVariable.getSn() + " expression = " + calculator.getExpression());

                calculator.doParseAndCalculate();
                Float result = calculator.getResult();
                logger.debug("Data parser service variable sn = " + deviceVariable.getSn() + " result = " + result);

                if (result == null) {
                  continue;
                }

                handleAnalogValue(deviceVariable, result.floatValue(), null);
              } catch (IllegalExpressionException ex) {
                if (!deviceVariable.isHasBeenDeleted()) {
                  this.dataStorageService.delVariableFromRedis(deviceVariable.getSn());
                  deviceVariable.setHasBeenDeleted(true);
                }
              }
            }
          }
        }


        this.currentIdx = 0;
      } else {

        this.currentIdx++;
      }
    }

    //从redis删除变量
    private void delVariableFromRedis(String sameDeviceSectionPos) {
      List<DeviceVariable> deviceVariableList = (List) GlobalVariables.getSameIndexVariablesMap().get(sameDeviceSectionPos);
      for (DeviceVariable deviceVariable : deviceVariableList) {
        long updateTimeLong = deviceVariable.getUpdateTime().toInstant().toEpochMilli();
        if (this.currentMilliSeconds - updateTimeLong > (this.expireTime * 60 * 1000) && !deviceVariable.isHasBeenDeleted()) {

          this.dataStorageService.delVariableFromRedis(deviceVariable.getSn());  //（变量sn）
          deviceVariable.setHasBeenDeleted(true);
        }
      }
    }

    //更新全局单个设备变量实时值和时间内存 处理模拟量/状态量时调用
    private boolean updateGlobalDeviceVariableMemory(DeviceVariable deviceVariable, byte[] realtimeMemory, byte[] updateTimeBytes) {
      int length = realtimeMemory.length; //长度
      int posInMemory = deviceVariable.getPosInMemory() * 80;

      System.arraycopy(realtimeMemory, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory + 1, length);
      System.arraycopy(updateTimeBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory + 9, 8);

      return true;
    }

    //处理数字量值
    private void handleDigitalValue(DeviceVariable deviceVariable, int value, Long updateTimeLong) {
      if (deviceVariable.getIsOpposit() != null && deviceVariable.getIsOpposit().byteValue() == 1) {
        if (value == 0) {
          value = 1;
        } else {
          value = 0;
        }
      }

      byte[] bytes = {(byte) value}; //int值转字节
      int recordValue = value;

      if (updateTimeLong == null || updateTimeLong.longValue() > deviceVariable.getUpdateTime().toInstant().toEpochMilli()) {
        //更新单个变量
        updateGlobalDeviceVariableMemory(deviceVariable, bytes, this.timeBytes);   //更新变量（变量，变量字节值，更新时间） 把变量对应的字节在全局字节中替换成新的

        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable() {
          public void run() {
            String strValue = String.valueOf(recordValue);
            String deviceVariableSn = deviceVariable.getSn(); //变量sn
            deviceVariable.setRegisterValue(strValue); //设备变量寄存器的值
            deviceVariable.setUpdateTime((updateTimeLong == null) ? DataParserService.this.currentTime : Timestamp.from(Instant.ofEpochMilli(updateTimeLong.longValue())));
            deviceVariable.setHasBeenDeleted(false);

            //把设备变量 数字量 存到redis
            DataParserService.this.dataStorageService.saveVariableToRedis(deviceVariableSn, strValue); //*****把设备变量 数字量及字符串形式值存到缓存 ******


            DataParserService.this.handleAlarm(recordValue, deviceVariable);

            if (deviceVariable.isInitial()) {
              deviceVariable.setIsInitial(false);
            }
          }
        });

      } else {

        BaseBusinessDevice businessDevice = (BaseBusinessDevice) GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.get(deviceVariable.getCollectorDeviceSn());
        if (businessDevice.getHistoryBeginInstant() != null && updateTimeLong
                .longValue() >= businessDevice.getHistoryBeginInstant().toEpochMilli() && updateTimeLong
                .longValue() < businessDevice.getHistoryEndInstant().toEpochMilli()) {
          deviceVariable.addHistoryYXValue(Instant.ofEpochMilli(updateTimeLong.longValue()), Integer.valueOf(value));
        }
      }
    }

    //处理模拟量值（统计、存redis、告警）
    private void handleAnalogValue(DeviceVariable deviceVariable, float value, Long updateTimeLong) {
      Float coefficient = deviceVariable.getCoefficient();
      if (coefficient != null) {
        value *= coefficient.floatValue();
      }

      if (updateTimeLong == null || updateTimeLong.longValue() > deviceVariable.getUpdateTime().toInstant().toEpochMilli()) { //如果参数更新时间为空 或者大于变量的更新时间

        String deviceVariableSn = deviceVariable.getSn();
        boolean needStore = false;
        if (deviceVariable.getAccumulation() != null) { //变量累计值部位空
          if (Math.abs(deviceVariable.getAccumulationBase()) < 1.0E-7D) {
            deviceVariable.setAccumulationBase(value);
            logger.debug("Data parser service variable sn = " + deviceVariableSn + " accumulation base changed to " + value + " because initial accumulation base = 0");

            needStore = true;
            value = 0.0F;
          } else if (Math.abs(value) < Math.abs(GlobalVariables.GLOBAL_ACCUMULATION_THRESHOLD)) {
            deviceVariable.setAccumulationBase(value);
            logger.debug("Data parser service variable sn = " + deviceVariableSn + " accumulation base changed to " + value + " because abs(value) < abs(threshhold), abs(threshhold) = " +

                    Math.abs(GlobalVariables.GLOBAL_ACCUMULATION_THRESHOLD));
            needStore = true;
            value = 0.0F;
          } else if (Math.abs(value) < Math.abs(deviceVariable.getAccumulationBase())) {
            deviceVariable.setAccumulationBase(value);
            logger.debug("Data parser service variable sn = " + deviceVariableSn + " accumulation base changed to " + value + " because abs(value) < abs(accumulationBase), abs(accumulationBase) = " +

                    Math.abs(deviceVariable.getAccumulationBase()));
            needStore = true;
            value = 0.0F;
          } else {
            value -= deviceVariable.getAccumulationBase();
          }
        }

        if (needStore) {
          GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable() {
            public void run() {
              String[] arr = deviceVariable.getSn().split("__");
              final String hashName = arr[0];
              final String key = arr[1];
              final String accumulationBaseValueStr = String.format("%f", new Object[]{Float.valueOf(deviceVariable.getAccumulationBase())});

              final String timeKey = arr[1] + "_update_time";
              final String timeValueStr = TimeFormatter.timestampToCommonString(deviceVariable.getUpdateTime());
              try {
                GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable() {
                  public void run() {
                    GlobalVariables.GLOBAL_REDIS_SERVCIE.putCachedValue(hashName, key, accumulationBaseValueStr); //存redis累计值
                    GlobalVariables.GLOBAL_REDIS_SERVCIE.putCachedValue(hashName, timeKey, timeValueStr);  //存redis
                  }
                });
              } catch (Exception e) {
                logger.warn("Data parser service variable sn = " + deviceVariableSn + " error occured when store accumulation base of device variable sn = " + deviceVariableSn, e);
              }
            }
          });
        }

        if (deviceVariable.getRecordPeriod() != null && (deviceVariable.getLastCacheTime() == null || this.currentInstant
                        .toEpochMilli() - deviceVariable.getLastCacheTime().toInstant().toEpochMilli() >= 2000L)) {

          try { //解析的变量和值
            logger.debug("Data parser service variable sn = " + deviceVariableSn + " cache value variable sn = " + deviceVariableSn + " data value = " + value);

            deviceVariable.addCatchedValues(this.currentInstant, Float.valueOf(value)); //
            deviceVariable.setLastCacheTime(this.currentTime);
          } catch (Exception e) {
            logger.warn("Data parser service variable sn = " + deviceVariableSn + " error occured when cache device variable sn = " + deviceVariableSn + " and value = " + value);
          }
        }

        Float toBeStoredValue = getToBeStoredValue(deviceVariable, value); //
        if (toBeStoredValue == null) {
          return;
        }
        value = toBeStoredValue.floatValue();

        byte[] valueBytes = NumberUtil.intToByte4(Float.floatToIntBits(value)); //变量值的 字节数组形式
        float recordValue = value;  //变量值的 浮点形式
        //***更新变量在内存数组里的实时值和时间
        updateGlobalDeviceVariableMemory(deviceVariable, valueBytes, this.timeBytes);

        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable() {
          public void run() {
            String strValue = String.format("%.3f", new Object[]{Double.valueOf(Math.round(recordValue * 1000.0D) / 1000.0D)});//********变量值的 字符串形式******
            deviceVariable.setRegisterValue(strValue);
            deviceVariable.setUpdateTime((updateTimeLong == null) ? DataParserService.this.currentTime : Timestamp.from(Instant.ofEpochMilli(updateTimeLong.longValue())));
            deviceVariable.setHasBeenDeleted(false);

            DataParserService.this.handleStatistics(deviceVariable, recordValue); //***处理统计 判断入参值如果是最大值/最小值，则设置最大值/最小值并更新全局字节GLOBAL_MEMORY_BYTES

            DataParserService.this.dataStorageService.saveVariableToRedis(deviceVariableSn, strValue);//*****把设备变量 模拟量及字符串形式值存到缓存 ******

            DataParserService.this.handleAlarm(recordValue, deviceVariable); //处理告警

            if (deviceVariable.isInitial()) {
              deviceVariable.setIsInitial(false);
            }
          }
        });

      } else {

        BaseBusinessDevice businessDevice = (BaseBusinessDevice) GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.get(deviceVariable.getCollectorDeviceSn());

        if (this.currentMilliSeconds - updateTimeLong.longValue() <= GlobalVariables.getPeriodMilliseconds(deviceVariable.getRecordPeriod()).longValue()) {
          if (deviceVariable.getAccumulation() != null) {

            if (value < deviceVariable.getAccumulationBase()) {
              return;
            }

            value -= deviceVariable.getAccumulationBase();
          }

          Iterator<Pair<Instant, Float>> itor = deviceVariable.getCatchedValues().iterator();
          int pos = 0;
          while (itor.hasNext()) {
            Pair<Instant, Float> pair = (Pair) itor.next();
            if (((Instant) pair.getValue0()).toEpochMilli() > updateTimeLong.longValue()) {
              break;
            }
            pos++;
          }
          deviceVariable.getCatchedValues().add(pos, new Pair(Instant.ofEpochMilli(updateTimeLong.longValue()), Float.valueOf(value)));
        } else if (businessDevice.getHistoryBeginInstant() != null && updateTimeLong
                .longValue() >= businessDevice.getHistoryBeginInstant().toEpochMilli() && updateTimeLong
                .longValue() < businessDevice.getHistoryEndInstant().toEpochMilli()) {

          deviceVariable.addHistoryYCValue(Instant.ofEpochMilli(updateTimeLong.longValue()), Float.valueOf(value));
        }
      }
    }

    //处理警告
    private void handleAlarm(double value, DeviceVariable deviceVariable) {
      List<EventTrigger> eventTriggers = deviceVariable.getEventTriggers();
      if (eventTriggers == null || eventTriggers.isEmpty()) {
        return;
      }
      AlarmReportService globalAlarmReportService = GlobalVariables.GLOBAL_ALARM_REPORT_SERVICE;

      Timestamp alarmTime = deviceVariable.getUpdateTime();

      Map<Integer, List<EventTrigger>> toSendEventTriggersMap = new HashMap<Integer, List<EventTrigger>>();

      for (EventTrigger eventTrigger : eventTriggers) {
        EventType eventType = this.eventTypesService.getEventType(eventTrigger.getTypeParentIndex(), eventTrigger.getTypeSn());
        if (eventType == null) {
          continue;
        }

        String lastReportId = (eventTrigger.getLastReportId() == null) ? "" : eventTrigger.getLastReportId();

        if (GlobalEventTypes.isSatisfy(eventType.getTriggerTypeSn(), value, eventTrigger.getParam1(), eventTrigger.getParam2(), eventTrigger.getParam3(), eventTrigger.getParam4())) {

//          if (!lastReportId.isEmpty()) {
//            continue;
//          }
          Integer triggerDelay = Integer.valueOf((eventType.getTriggerDelay() == null) ? 0 : eventType.getTriggerDelay().intValue());
          if (triggerDelay.intValue() > 0) {
            Timestamp firstSatisfyTime = eventTrigger.getFirstSatisfyTime();
            if (firstSatisfyTime == null) {
              eventTrigger.setFirstSatisfyTime(alarmTime);
              continue;
            }
            if (alarmTime.getTime() - firstSatisfyTime.getTime() < (triggerDelay.intValue() * 1000)) {
              continue;
            }
          }

          Integer levelCode = eventType.getLevelCode();
          if (toSendEventTriggersMap.containsKey(levelCode)) {
            ((List) toSendEventTriggersMap.get(levelCode)).add(eventTrigger);
            continue;
          }
          List<EventTrigger> temp = new ArrayList<EventTrigger>();
          temp.add(eventTrigger);
          toSendEventTriggersMap.put(levelCode, temp);

          continue;
        }

        eventTrigger.setFirstSatisfyTime(null);
        if (!lastReportId.isEmpty()) {
          int autoCheckRecoverReport = 0;
          Integer autoCheckDelay = Integer.valueOf((eventType.getAutoCheckDelay() == null) ? 0 : eventType.getAutoCheckDelay().intValue());
          if (autoCheckDelay.intValue() > 0) {
            Timestamp lastReportTime = eventTrigger.getLastReportTime();
            if (lastReportTime != null && alarmTime.getTime() - lastReportTime.getTime() < (autoCheckDelay.intValue() * 1000)) {
              autoCheckRecoverReport = 1;
            }
          }

          String reportId = Caculator.caculateUniqueTimeId();
          globalAlarmReportService.sendAlarmMessage(reportId, lastReportId, autoCheckRecoverReport, deviceVariable.getParentIndex(), "", "", 0, deviceVariable
                  .getSn(), deviceVariable.getName(), String.valueOf(value), TimeFormatter.timestampToCommonString(alarmTime), TimeFormatter.timestampToCommonString(alarmTime), eventTrigger);

          eventTrigger.setLastReportId(null);
          eventTrigger.setLastReportTime(alarmTime);


          this.configuration.updateEventTriggerLastReportInfo(eventTrigger.getId(), null, null);
        }
      }


      if (!toSendEventTriggersMap.isEmpty()) {
        Integer minLevelCode = (Integer) Collections.min(toSendEventTriggersMap.keySet());
        for (EventTrigger eventTrigger : toSendEventTriggersMap.get(minLevelCode)) {

          String reportId = Caculator.caculateUniqueTimeId();
          globalAlarmReportService.sendAlarmMessage(reportId, "", 0, deviceVariable.getParentIndex(), "", "", 1, deviceVariable
                  .getSn(), deviceVariable.getName(), String.valueOf(value), TimeFormatter.timestampToCommonString(alarmTime), null, eventTrigger);
          eventTrigger.setLastReportId(reportId);
          eventTrigger.setLastReportTime(alarmTime);


          this.configuration.updateEventTriggerLastReportInfo(eventTrigger.getId(), reportId, alarmTime);
        }
      }
    }

    //得到被存储的值
    private Float getToBeStoredValue(DeviceVariable deviceVariable, float value) {
      long recordPeriodMilliSeconds = GlobalVariables.getPeriodMilliseconds(deviceVariable.getRecordPeriod()).longValue();

      if (deviceVariable.getRecordType() == null || deviceVariable.getRecordType().equals("实时值")) {
        Iterator<Pair<Instant, Float>> cachedValueItor = deviceVariable.getCatchedValues().iterator();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (this.currentInstant.toEpochMilli() - ((Instant) pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
            cachedValueItor.remove();
          }
        }

        return Float.valueOf(value);
      }
      if (deviceVariable.getRecordType().equals("最大值")) {
        Float retVal = null;
        Iterator<Pair<Instant, Float>> cachedValueItor = deviceVariable.getCatchedValues().iterator();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (this.currentInstant.toEpochMilli() - ((Instant) pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {

            cachedValueItor.remove();

            continue;
          }
          float recordValue = ((Float) pair.getValue1()).floatValue();
          if (recordValue > value) {
            value = recordValue;
          }
        }

        return Float.valueOf((float) (Math.round(value * 1000.0D) / 1000.0D));
      }
      if (deviceVariable.getRecordType().equals("最小值")) {
        Float retVal = null;
        Iterator<Pair<Instant, Float>> cachedValueItor = deviceVariable.getCatchedValues().iterator();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (this.currentInstant.toEpochMilli() - ((Instant) pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {

            cachedValueItor.remove();

            continue;
          }
          float recordValue = ((Float) pair.getValue1()).floatValue();
          if (recordValue < value) {
            value = recordValue;
          }
        }

        return Float.valueOf((float) (Math.round(value * 1000.0D) / 1000.0D));
      }
      if (deviceVariable.getRecordType().equals("平均值")) {
        float sum = 0.0F;
        int count = 0;
        Iterator<Pair<Instant, Float>> cachedValueItor = deviceVariable.getCatchedValues().iterator();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (this.currentInstant.toEpochMilli() - ((Instant) pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {

            cachedValueItor.remove();

            continue;
          }
          float recordValue = ((Float) pair.getValue1()).floatValue();
          sum += recordValue;
          count++;
        }

        if (count == 0) {
          return null;
        }

        return Float.valueOf((float) (Math.round((sum / count) * 1000.0D) / 1000.0D));
      }
      if (deviceVariable.getRecordType().equals("差值")) {
        Iterator<Pair<Instant, Float>> cachedValueItor = deviceVariable.getCatchedValues().iterator();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (this.currentInstant.toEpochMilli() - ((Instant) pair.getValue0()).toEpochMilli() > recordPeriodMilliSeconds) {
            cachedValueItor.remove();
          }
        }


        if (deviceVariable.getCatchedValues().size() < 2) {
          return null;
        }

        Pair<Instant, Float> maxPair = (Pair) deviceVariable.getCatchedValues().get(0);
        Pair<Instant, Float> minPair = (Pair) deviceVariable.getCatchedValues().get(0);
        cachedValueItor = deviceVariable.getCatchedValues().iterator();
        cachedValueItor.next();
        while (cachedValueItor.hasNext()) {
          Pair<Instant, Float> pair = (Pair) cachedValueItor.next();
          if (((Float) pair.getValue1()).floatValue() > ((Float) maxPair.getValue1()).floatValue()) {
            maxPair = pair;
          }

          if (((Float) pair.getValue1()).floatValue() < ((Float) minPair.getValue1()).floatValue()) {
            minPair = pair;
          }
        }

        if (maxPair == null || minPair == null) {
          return null;
        }

        return Float.valueOf((float) (Math.round((((Float) maxPair.getValue1()).floatValue() - ((Float) minPair.getValue1()).floatValue()) * 1000.0D) / 1000.0D));
      }
      return null;
    }

    //处理统计值
    private void handleStatistics(DeviceVariable deviceVariable, float value) {
      byte[] valueBytes = NumberUtil.intToByte4(Float.floatToIntBits(value));
      int posInMemory = deviceVariable.getPosInMemory();

      if (deviceVariable.getMaxValueTime() == null || deviceVariable.getMinValueTime() == null) { //如果最大值/最小值时间为空，则设置计数为0
        deviceVariable.setCount(0);
      } else {
        LocalDate maxValueDate = deviceVariable.getMaxValueTime().toLocalDateTime().toLocalDate(); //最大值时间
        LocalDate minValueDate = deviceVariable.getMinValueTime().toLocalDateTime().toLocalDate(); //最小值时间
        LocalDate nowDate = LocalDate.now(); //当前时间

        if (maxValueDate.isBefore(nowDate) || minValueDate.isBefore(nowDate)) { //如果最大/小值时间在当前时间之前


          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.storeVarDayStatics(deviceVariable); //调用存储单个变量日统计
          deviceVariable.setCount(0);
          deviceVariable.setSum(0.0F);
        }
      }

      if (deviceVariable.getCount() == 0) { //计数为0

        deviceVariable.setMaxValue(value);
        deviceVariable.setMinValue(value);

        deviceVariable.setMinValueTime(deviceVariable.getUpdateTime());
        deviceVariable.setMaxValueTime(deviceVariable.getUpdateTime());


        System.arraycopy(valueBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 17, 4);

        System.arraycopy(this.timeBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 25, 8);


        System.arraycopy(valueBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 33, 4);

        System.arraycopy(this.timeBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 41, 8);
      }
      else { //应该执行此处

        if (value > deviceVariable.getMaxValue()) { //****如果值>最大值，则重新设置最大值，并更新GLOBAL_MEMORY_BYTES
          deviceVariable.setMaxValue(value);
          deviceVariable.setMaxValueTime(deviceVariable.getUpdateTime());

          System.arraycopy(valueBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 17, 4);

          System.arraycopy(this.timeBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 25, 8);
        }


        if (value < deviceVariable.getMinValue()) {//****如果值<最小值，则重新设置最小值，并更新GLOBAL_MEMORY_BYTES
          deviceVariable.setMinValue(value);
          deviceVariable.setMinValueTime(deviceVariable.getUpdateTime());

          System.arraycopy(valueBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 33, 4);

          System.arraycopy(this.timeBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 41, 8);
        }
      }

    //同时更新平均值和差值
      float sum = deviceVariable.getSum();
      sum += value;
      deviceVariable.setSum(sum);

      int count = deviceVariable.getCount();
      count++;
      deviceVariable.setCount(count);

      float average = (count == 0) ? 0.0F : (sum / count);
      byte[] averageBytes = NumberUtil.intToByte4(Float.floatToIntBits(average));
      System.arraycopy(averageBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 49, 4);


      float diff = deviceVariable.getMaxValue() - deviceVariable.getMinValue();
      byte[] diffBytes = NumberUtil.intToByte4(Float.floatToIntBits(diff));
      System.arraycopy(diffBytes, 0, GlobalVariables.GLOBAL_MEMORY_BYTES, posInMemory * 80 + 57, 4);
    }

    //处理需量、力率告警
    public void handleDemandCosAlarm(DeviceVariable deviceVariable) {
      List<String> collectorDevicesSns = new ArrayList<String>();
      for (BaseBusinessDevice baseBusinessDevice : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
        collectorDevicesSns.add(baseBusinessDevice.getSn());
      }
      //获得当前所有站点
      List<String> stationSns = configuration.getDeviceStationSns(collectorDevicesSns);
      //根据站点获取配置的电价数据
      JSONObject elecConfig = new JSONObject();
      stationSns.forEach(stationSn -> {
        StationCharge stationChargeDTO = new StationCharge();
        stationChargeDTO.setStationSn(stationSn);
        StationCharge stationCharge = configuration.getStationCharge(stationChargeDTO);
        if (!StringUtil.isNullOrEmpty(stationCharge.getCapacityCharge()) || !StringUtil.isNullOrEmpty(stationCharge.getCosCharge())) {
          JSONObject demandCos = new JSONObject();
          //jsonObject电价需量配置
          JSONObject capacityCharge = new JSONObject(stationCharge.getCapacityCharge());
          JSONObject cosCharge = new JSONObject(stationCharge.getCosCharge());
          //需量
          if ("最大需量法".equals(capacityCharge.get("method"))) {
            //需量核定容量
            String capacityNormalValue = capacityCharge.get("normal_capacity").toString();
            capacityNormalValue = StringUtil.isNullOrEmpty(capacityNormalValue) ? "0" : capacityNormalValue;
            //需量标准值
            Integer capacityValue = Integer.parseInt(capacityNormalValue);
            //需量预警
            demandCos.append("normal_capacity_earlywarn", capacityValue * 0.9);
            //需量告警值
            demandCos.append("normal_capacity_warn", capacityValue);
          }
          //力率
          if (cosCharge.has("normal_cos")) {
            String cosNoramValue = cosCharge.get("normal_cos").toString();
            cosNoramValue = StringUtil.isNullOrEmpty(cosNoramValue) ? "0" : cosNoramValue;
            //力率标准值
            Double cosValue = Double.parseDouble(cosNoramValue);
            //力率预警值
            demandCos.append("cos_earlywarn", cosValue.doubleValue() + 0.2);
            //力率告警值
            demandCos.append("cos_warn", cosValue);
          }
          //单个站点对应的需量 力率告警值
          elecConfig.append(stationSn, demandCos);
        }
        //获取站点下所有的带P/MD变量的设备
        Map<String, Object> filter = new HashMap<String, Object>();
        List<String> electricVarCodes = Arrays.asList(new String[]{"P", "MD", "epf","eqf"});
        filter.put("parent_index", stationSn);
        filter.put("var_code", electricVarCodes);
        List<DeviceVariable> deviceVars = configuration.getDeviceVariables(filter);
        Map<String, JSONObject> deviceElectricVar = new HashMap<String, JSONObject>();
        deviceVars.forEach(deviceVar -> {
          if (deviceElectricVar.containsKey(deviceVar.getDeviceSn())) {
            JSONObject elecValue = deviceElectricVar.get(deviceVariable.getSn());
            elecValue.append(deviceVar.getVarCode(), deviceVar.getSn());
          } else {
            JSONObject elecValue = new JSONObject(deviceVar.getSn());
            deviceElectricVar.put(deviceVar.getDeviceSn(), elecValue);
          }
        });
        //需量总和
        double[] demandSum = new double[0];
        double[] epfs = new double[0];
        double[] eqfs = new double[0];
        deviceElectricVar.forEach((key, elecValue) -> {
          String p_varSn = elecValue.getString("P");
          Double pRealTime = handleVarialeVarCode(p_varSn);
          String md_varSn = elecValue.getString("MD");
          demandSum[0] += Math.max(handleVarialeVarCode(p_varSn), handleVarialeVarCode(md_varSn));
          String epf_varSn = elecValue.getString("epf");
          epfs[0] += handleVarialeVarCode("epf_varSn");
          String eqf_varSn = elecValue.getString("eqf");
          eqfs[0] += handleVarialeVarCode("eqf_varSn");
        });
        // 需量 力率 阈值
        JSONObject demandCos = (JSONObject) elecConfig.get(stationSn);

        // 功率因数
        if (epfs[0] == 0 && eqfs[0] == 0) {
          double cos = epfs[0] / Math.sqrt(Math.pow(epfs[0], 2) + Math.pow(eqfs[0], 2));
          double cosEarlyWarn = Double.parseDouble(demandCos.get("cos_earlywarn").toString());
          double cosWarn = Double.parseDouble(demandCos.get("cos_warn").toString());
          if (cos > cosWarn && cos < cosEarlyWarn) {

          } else if (cos < cosWarn) {


          }
        }
      });
    }
    public Double handleVarialeVarCode (String varSn){
      String[] var_varCode = varSn.split("__");
      String redisResult = GlobalVariables.GLOBAL_REDIS_SERVCIE.get(var_varCode[0], var_varCode[1]).toString();
      Double returnResult = StringUtil.isNullOrEmpty(redisResult) ? null : Double.parseDouble(redisResult);
      return returnResult;
    }
    //处理时耗、日耗告警
    public void hourDayConsumeAlarm(){}

  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\DataParserService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */