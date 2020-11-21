  package  com.kkwl.collector.models;
  
  import com.kkwl.collector.common.HandlingType;
  import com.kkwl.collector.common.StatusType;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.models.EventTrigger;
  import java.io.Serializable;
  import java.sql.Timestamp;
  import java.time.Instant;
  import java.util.ArrayList;
  import java.util.List;
  import org.javatuples.Pair;
  
   ///设备变量
  public class DeviceVariable implements Serializable
  {
    private String sn;     //变量sn
    private String name;   //变量名称
    private String unit;
    private Byte bitPos; //位 位置
    private String dataType;  //数据类型    4字节浮点4321
    private String type;   //类型Digital/Analog
    private Float initialValue;  //默认初始化值
    private Float baseValue;
    private Byte digitalStatus;
    private Float coefficient;
    private String recordPeriod;  //记录周期
    private String recordType;
    private Float upperLimit;
    private Float moreUpperLimit;
    private Float lowerLimit;
    private Float moreLowerLimit;
    private Byte alarm;
    private String alarmCondition;
    private Byte zeroToOneAlarm;
    private Byte oneToZeroAlarm;
    private String zeroMeaning;
    private String oneMeaning;
    private String deviceSn;   //设备sn 为拓扑结构中的现场设备，deviceSn前面的站点是自动加上的 MNTWK_SZMN_16
    private String collectorDeviceSn;  //采集设备sn 为配置中的通讯设备  SZMN
    private String parentIndex;
    private Byte rw;
    private String registerName;   //寄存器名称YX/YC/YK/DD
    private Integer registerIndex; //寄存器标号 (寄存器地址)
    private String registerType;  //寄存器类型
    private Integer registerTypeIndex;
    private String registerValue;  //**********************寄存器值即为该变量的值********************
    private Timestamp updateTime;  //更新时间
    private Timestamp reportTime;
    private String reportId;
    private StatusType alarmType;
    private String expression; //表达式
    private String accumulation; //累计
    private Byte isOpposit;
    private String varCode;
    private float maxValue;   //最大值
    private Timestamp maxValueTime; //最大值时间
    private float minValue; //最小值
    private Timestamp minValueTime; //最小值时间
    private float accumulationBase; //累计
    private float sum;
    private int count;
    private List<Pair<Instant, Float>> catchedValues;
    private List<Pair<Instant, Integer>> historyYXValues;  //历史遥信值
    private List<Pair<Instant, Float>> historyYCValues;    //历史遥测值
    private Timestamp lastCacheTime;
    private HandlingType specialHandlingType;
    private boolean isInitial = true;
    private int posInMemory = -1; //内存位置


    private boolean hasBeenDeleted;


    private List<EventTrigger> eventTriggers;


    public String getSn() { return this.sn; }






    public void setSn(String sn) { this.sn = sn; }






    public String getName() { return this.name; }






    public void setName(String name) { this.name = name; }






    public String getUnit() { return this.unit; }






    public void setUnit(String unit) { this.unit = unit; }






    public String getRegisterName() { return this.registerName; }






    public void setRegisterName(String registerName) { this.registerName = registerName; }






    public Byte getBitPos() { return this.bitPos; }






    public void setBitPos(Byte bitPos) { this.bitPos = bitPos; }






    public String getDataType() { return this.dataType; }






    public void setDataType(String dataType) { this.dataType = dataType; }






    public String getType() { return this.type; }






    public void setType(String type) { this.type = type; }






    public Float getInitialValue() { return this.initialValue; }






    public void setInitialValue(Float initialValule) { this.initialValue = this.initialValue; }






    public Float getBaseValue() { return this.baseValue; }






    public void setBaseValue(Float baseValue) { this.baseValue = baseValue; }






    public Float getCoefficient() { return this.coefficient; }






    public void setCoefficient(Float coefficient) { this.coefficient = coefficient; }






    public String getRecordPeriod() { return this.recordPeriod; }






    public void setRecordPeriod(String recordPeriod) { this.recordPeriod = recordPeriod; }






    public Float getUpperLimit() { return this.upperLimit; }






    public void setUpperLimit(Float upperLimit) { this.upperLimit = upperLimit; }






    public Float getMoreUpperLimit() { return this.moreUpperLimit; }






    public void setMoreUpperLimit(Float moreUpperLimit) { this.moreUpperLimit = moreUpperLimit; }






    public Float getLowerLimit() { return this.lowerLimit; }






    public void setLowerLimit(Float lowerLimit) { this.lowerLimit = lowerLimit; }






    public Float getMoreLowerLimit() { return this.moreLowerLimit; }






    public void setMoreLowerLimit(Float moreLowerLimit) { this.moreLowerLimit = moreLowerLimit; }






    public Byte getAlarm() { return this.alarm; }






    public void setAlarm(Byte alarm) { this.alarm = alarm; }






    public String getAlarmCondition() { return this.alarmCondition; }






    public void setAlarmCondition(String alarmCondition) { this.alarmCondition = alarmCondition; }






    public Byte getZeroToOneAlarm() { return this.zeroToOneAlarm; }






    public void setZeroToOneAlarm(Byte zeroToOneAlarm) { this.zeroToOneAlarm = zeroToOneAlarm; }






    public Byte getOneToZeroAlarm() { return this.oneToZeroAlarm; }






    public void setOneToZeroAlarm(Byte oneToZeroAlarm) { this.oneToZeroAlarm = oneToZeroAlarm; }






    public String getZeroMeaning() { return this.zeroMeaning; }






    public void setZeroMeaning(String zeroMeaning) { this.zeroMeaning = zeroMeaning; }






    public String getOneMeaning() { return this.oneMeaning; }






    public void setOneMeaning(String oneMeaning) { this.oneMeaning = oneMeaning; }






    public String getDeviceSn() { return this.deviceSn; }






    public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }






    public String getParentIndex() { return this.parentIndex; }






    public void setParentIndex(String parentIndex) { this.parentIndex = parentIndex; }






    public String getRegisterValue() { return this.registerValue; }






    public void setRegisterValue(String registerValue) { this.registerValue = registerValue; }






    public Timestamp getUpdateTime() { return this.updateTime; }






    public void setUpdateTime(Timestamp updateTime) { this.updateTime = updateTime; }






    public Timestamp getReportTime() { return this.reportTime; }






    public void setReportTime(Timestamp reportTime) { this.reportTime = reportTime; }






    public String getReportId() { return this.reportId; }






    public void setReportId(String reportId) { this.reportId = reportId; }






    public String getCollectorDeviceSn() { return this.collectorDeviceSn; }






    public void setCollectorDeviceSn(String collectorDeviceSn) { this.collectorDeviceSn = collectorDeviceSn; }






    public String getExpression() { return this.expression; }






    public void setExpression(String expression) { this.expression = expression; }






    public float getMaxValue() { return this.maxValue; }



    public void setMaxValue(float value) { this.maxValue = value; }






    public float getMinValue() { return this.minValue; }



    public void setMinValue(float value) { this.minValue = value; }






    public float getSum() { return this.sum; }






    public void setSum(float sum) { this.sum = sum; }






    public int getCount() { return this.count; }






    public void setCount(int count) { this.count = count; }




    public float getAccumulationBase() { return this.accumulationBase; }



    public void setAccumulationBase(float accumulationBase) { this.accumulationBase = accumulationBase; }



    public String getAccumulation() { return this.accumulation; }



    public void setAccumulation(String accumulation) { this.accumulation = accumulation; }



    public StatusType getAlarmType() { return this.alarmType; }



    public void setAlarmType(StatusType alarmType) { this.alarmType = alarmType; }




    public Integer getRegisterIndex() { return this.registerIndex; }



    public void setRegisterIndex(Integer registerIndex) { this.registerIndex = registerIndex; }



    public String getRegisterType() { return this.registerType; }



    public void setRegisterType(String registerType) { this.registerType = registerType; }



    public Integer getRegisterTypeIndex() { return this.registerTypeIndex; }



    public void setRegisterTypeIndex(Integer registerTypeIndex) { this.registerTypeIndex = registerTypeIndex; }




    public Byte getIsOpposit() { return this.isOpposit; }



    public void setIsOpposit(Byte isOpposit) { this.isOpposit = isOpposit; }



    public String getVarCode() { return this.varCode; }



    public void setVarCode(String varCode) { this.varCode = varCode; }



    public Timestamp getMaxValueTime() { return this.maxValueTime; }



    public void setMaxValueTime(Timestamp maxValueTime) { this.maxValueTime = maxValueTime; }



    public Timestamp getMinValueTime() { return this.minValueTime; }



    public void setMinValueTime(Timestamp minValueTime) { this.minValueTime = minValueTime; }



    public HandlingType getSpecialHandlingType() { return this.specialHandlingType; }



    public void setSpecialHandlingType(HandlingType specialHandlingType) { this.specialHandlingType = specialHandlingType; }



    public List<Pair<Instant, Float>> getCatchedValues() { return this.catchedValues; }


    public void addCatchedValues(Instant instant, Float value) {
      if (instant == null || value == null) {
        return;
      }

      if (this.catchedValues == null) {
        this.catchedValues = new ArrayList();
      }

      this.catchedValues.add(new Pair(instant, value));
    }



    public String getRecordType() { return this.recordType; }



    public void setRecordType(String recordType) { this.recordType = recordType; }




    public boolean isInitial() { return this.isInitial; }



    public void setIsInitial(boolean isInitial) { this.isInitial = isInitial; }




    public Timestamp getLastCacheTime() { return this.lastCacheTime; }



    public void setLastCacheTime(Timestamp lastCacheTime) { this.lastCacheTime = lastCacheTime; }




    public Byte getDigitalStatus() { return this.digitalStatus; }



    public void setDigitalStatus(Byte digitalStatus) { this.digitalStatus = digitalStatus; }



    public Byte getRw() { return this.rw; }



    public void setRw(Byte rw) {
      if (rw == null) {

        this.rw = Byte.valueOf((byte)0);
      } else {
        this.rw = rw;
      }
    }


    public void setPosInMemory(int pos) { this.posInMemory = pos; }



    public int getPosInMemory() { return this.posInMemory; }



    public List<EventTrigger> getEventTriggers() { return this.eventTriggers; }



    public void setEventTriggers(List<EventTrigger> eventTriggers) { this.eventTriggers = eventTriggers; }



    public boolean isHasBeenDeleted() { return this.hasBeenDeleted; }



    public void setHasBeenDeleted(boolean hasBeenDeleted) { this.hasBeenDeleted = hasBeenDeleted; }



    public List<Pair<Instant, Integer>> getHistoryYXValues() { return this.historyYXValues; }


    public void addHistoryYXValue(Instant ins, Integer yx) {
      if (ins == null || yx == null) {
        return;
      }

      if (yx.intValue() != 0 && yx.intValue() != 1) {
        return;
      }

      if (this.historyYXValues == null) {
        this.historyYXValues = new ArrayList();
      }

      this.historyYXValues.add(new Pair(ins, yx));
    }


    public List<Pair<Instant, Float>> getHistoryYCValues() { return this.historyYCValues; }


    public void addHistoryYCValue(Instant ins, Float yc) {
      if (ins == null || yc == null) {
        return;
      }

      if (this.historyYCValues == null) {
        this.historyYCValues = new ArrayList();
      }

      this.historyYCValues.add(new Pair(ins, yc));
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\DeviceVariable.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */