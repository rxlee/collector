package com.kkwl.collector.common;

import io.netty.channel.ChannelHandlerContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.javatuples.Triplet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kkwl.collector.channels.netty.NettyClient;
import com.kkwl.collector.channels.netty.NettyServer;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.models.response.LogViewResponse;
import com.kkwl.collector.services.AlarmReportService;
import com.kkwl.collector.services.DataStorageService;
import com.kkwl.collector.services.RedisService;
import com.kkwl.collector.utils.NumberUtil;

public class GlobalVariables {

	public static boolean IS_SERVICE_RUNNING = false;
	public static ScheduledExecutorService GLOBAL_SCHEDULED_SERVICE_POOL = null; //线程池
	public static double GLOBAL_ACCUMULATION_THRESHOLD;
	public static final int MAX_VARIABLES_COUNT = 1000000;
	public static final int VARIABLE_BLOCK_SIZE = 80; //变量块大小
	public static final byte[] GLOBAL_MEMORY_BYTES;  //全局内存字节
	public static final byte[] GLOBAL_MEMORY_BYTES_COPY; //全局内存字节复制
	public static final Map<Integer, Boolean> MEMORY_TAKEN_MAP; //内存使用map
	public static long TOTAL_PARSING_TIME;
	public static long TOTAL_PARSING_COUNT;
	public static short GLOBAL_OFFLINE_CHECK_DURATION;
	public static int GLOBAL_DEVICE_HANDING_PERIOD;
	public static RedisService GLOBAL_REDIS_SERVCIE;  //redis服务
	public static DataStorageService GLOBAL_DATA_STORAGE_SERVICE;  //数据存储服务
	public static AlarmReportService GLOBAL_ALARM_REPORT_SERVICE;  //报警报表服务
	public static Configuration GLOBAL_DB_HANDLER;  //配置
	public static String GLOBAL_COLLECTOR_RUN_MODE; //采集运行模式
	public static String GLOBAL_IEC_STANDARD;
	public static NettyServer NETTY_SERVER;  //netty服务端
	public static List<NettyClient> NETTY_CLIENT_LIST;
	public static MqttClient MQTT_CLIENT; //MQTT客户端
	public static MqttConnectOptions MQTT_OPTIONS;
	public static ScheduledFuture MQTT_MONITOR_CLIENT_STATUS_LISTENER; //监听定时任务
	public static String MESSAGE_COMPRESS_TYPE; //消息压缩类型
	public static ZoneId GLOBAL_DEFAULT_ZONEID;
	public static final Map<String, String> GLOBAL_DEVICE_TYPE_MAP = new HashMap(); //设备类型map
	public static final DateTimeFormatter GLOBAL_COMMON_DATE_FORMATTER;
	public static final DateTimeFormatter GLOBAL_HISTORY_MONTH_FORMATTER;
	public static final DateTimeFormatter GLOBAL_HISTORY_DAY_FORMATTER; //全局日期
	public static final DateTimeFormatter GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER;
	public static List<ServerDTU> GLOBAL_SERVER_DTUS; //server
	public static Map<String, ServerDTU> GLOBAL_CONNECTKEY_SERVER_DTU_MAP;  //连接标识-ServerDTU
	public static List<ClientDTU> GLOBAL_CLIENT_DTUS;
	public static List<BaseBusinessDevice> GLOBAL_BASE_BUSINESS_DEVICES; //全局业务设备列表
	public static Map<String, BaseBusinessDevice> GLOBAL_BUSINESS_DEVICE_MAP; //全局业务设备map
	public static Map<String, ChannelHandlerContext> GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP; //dtu.getSn()-ctx
	
	public static String GLOBAL_WEBSOCKET_BIND_DTU;    //WEBSOCKET绑定的DTU
	public static Queue<LogViewResponse> GLOBAL_WEBSOCKET_MESSAGE_QUEUE;
	public static final List<String> GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK;
	public static final LinkedBlockingQueue<Triplet<String, JSONObject, String>> GLOBAL_CONFIGURATION_CHANGING_QUEUE; //配置改变队列
	private static List<DeviceVariable> DEVICE_VARIABLES;  //设备变量
	private static Map<String, DeviceVariable> DEVICE_VARIABLE_MAP;   //变量sn-设备变量MAP
	private static Map<String, Set<String>> RELATED_VARIABLE_MAP;  //相关联的变量MAP
	private static Map<String, Set<String>> REVERSE_RELATED_VARIABLE_MAP;
	private static List<String> SAME_INDEX_COMPOSED_NAMES; //相同序号组成名字，类容为deviceDataSectionPos 例 LXBE|YC|100|4  设备数据片段-采集设备sn|寄存器名字|寄存器序号|数据类型长度
	private static Map<String, List<DeviceVariable>> SAME_INDEX_VARIABLES_MAP; //相同序号的变量map<deviceDataSectionPos, deviceVariablesWithSameDeviceSectionIndex>
	private static Logger logger;

	static {
		//设备类型MAP
		GLOBAL_DEVICE_TYPE_MAP.put("IEC104", "BaseIEC104Device");
		GLOBAL_DEVICE_TYPE_MAP.put("MODBUSRTU", "BaseModbusRTUDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("MODBUSTCP", "BaseModbusTCPDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("MQTT_HGNY", "HGNYClientDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("IEC101", "BaseIEC101Device");

		GLOBAL_MEMORY_BYTES = new byte[80000000]; //*****************全局内存  每个变量80个字节，除以80为可容纳一百万个变量***************

		GLOBAL_MEMORY_BYTES_COPY = new byte[80000000]; //全局内存复制

		MEMORY_TAKEN_MAP = new LinkedHashMap(); //内存使用map
		//将内存使用map初始化为-序号：0
		for (int i = 0; i < 1000000; i++) {
			MEMORY_TAKEN_MAP.put(Integer.valueOf(i), Boolean.valueOf(false));
		}

		NETTY_CLIENT_LIST = new ArrayList();
		GLOBAL_DEFAULT_ZONEID = ZoneId.systemDefault();
		GLOBAL_COMMON_DATE_FORMATTER = DateTimeFormatter.ofPattern(
				"yyyy-MM-dd HH:mm:ss").withZone(GLOBAL_DEFAULT_ZONEID);

		GLOBAL_HISTORY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM")
				.withLocale(Locale.getDefault())
				.withZone(GLOBAL_DEFAULT_ZONEID);

		GLOBAL_HISTORY_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
				.withLocale(Locale.getDefault())
				.withZone(GLOBAL_DEFAULT_ZONEID);

		GLOBAL_HISTORY_DATE_TIME_STAMP_FORMATTER = DateTimeFormatter
				.ofPattern("yyyy-MM-dd HH:mm:00")
				.withLocale(Locale.getDefault())
				.withZone(GLOBAL_DEFAULT_ZONEID);

		GLOBAL_SERVER_DTUS = new ArrayList();

		GLOBAL_CONNECTKEY_SERVER_DTU_MAP = new HashMap();

		GLOBAL_CLIENT_DTUS = new ArrayList();

		GLOBAL_BASE_BUSINESS_DEVICES = new ArrayList();

		GLOBAL_BUSINESS_DEVICE_MAP = new HashMap();

		GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP = new HashMap();

		GLOBAL_WEBSOCKET_MESSAGE_QUEUE = new LinkedBlockingQueue();

		GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK = new ArrayList();

		GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.add("EPf");
		GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.add("EPr");
		GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.add("EQf");
		GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.add("EQr");

		GLOBAL_CONFIGURATION_CHANGING_QUEUE = new LinkedBlockingQueue(); //实例配置改变队列

		DEVICE_VARIABLES = new ArrayList();  //设备变量

		DEVICE_VARIABLE_MAP = new HashMap(); //设备变量map

		RELATED_VARIABLE_MAP = new HashMap();

		REVERSE_RELATED_VARIABLE_MAP = new HashMap();

		SAME_INDEX_COMPOSED_NAMES = new ArrayList();

		SAME_INDEX_VARIABLES_MAP = new HashMap();

		logger = LoggerFactory.getLogger(GlobalVariables.class);
	}
	
     //获得周期毫秒
	public static Long getPeriodMilliseconds(String period) {
		switch (period) {

		case "1分钟":
			return Long.valueOf(60000L);
		case "5分钟":
			return Long.valueOf(300000L);
		case "10分钟":
			return Long.valueOf(600000L);
		case "15分钟":
			return Long.valueOf(900000L);
		case "20分钟":
			return Long.valueOf(1200000L);
		case "30分钟":
			return Long.valueOf(1800000L);
		case "1小时":
			return Long.valueOf(3600000L);
		}
		return Long.valueOf(300000L);  ///默认5分钟
	}
	//添加设备变量
	public static void addDeviceVariables(List<DeviceVariable> toBeAddedVariables) { //初始化时调用，参数为查库获得的所有变量 添加到内存中
		if (toBeAddedVariables.isEmpty()) {
			return;
		}
		//存放实际变量
		List<DeviceVariable> realAddedVariables = new ArrayList<DeviceVariable>();
		//将要被添加的变量添加到实际变量 ，给变量设置地址（空闲）
		for (DeviceVariable deviceVariable : toBeAddedVariables) { //zth此处 有遥信
			//获取内存中空闲的变量地址
			int pos = getFreeVariablePosInMemory();//**********任一空闲地址 整型值
			if (pos >= 0) {
				deviceVariable.setPosInMemory(pos); //**************设置在全局字节里的变量地址*****************
				MEMORY_TAKEN_MAP.put(Integer.valueOf(pos), Boolean.valueOf(true)); //内存地址已使用，map的value置1，标记为已使用
			} else {
				throw new OutOfMemoryError("Device variable numbers overflow"); //变量数量溢出
			}

			String expression = deviceVariable.getExpression();
			if (expression == null || expression.isEmpty()) {//变量表达式为空

				if ((deviceVariable.getDataType() == null && !deviceVariable.getType().equals("Digital")) || (deviceVariable.getDataType() != null && DataType.getDataTypeByName(deviceVariable.getDataType()) == null)) {
					continue;
				}
				//组装
				//设备数据片段-采集设备sn|寄存器名字|寄存器序号|数据类型长度  deviceDataSectionPos=LXBE|YC|100|4
				String deviceDataSectionPos = deviceVariable.getCollectorDeviceSn() + "|" + deviceVariable.getRegisterName() + "|" + deviceVariable.getRegisterIndex()
						+ "|" + ((deviceVariable.getDataType() == null) ? 1 : DataType.getDataTypeByName(deviceVariable.getDataType()).getSize());

				if (SAME_INDEX_COMPOSED_NAMES.contains(deviceDataSectionPos)) {
					((List) SAME_INDEX_VARIABLES_MAP.get(deviceDataSectionPos)).add(deviceVariable); //list集合：变量
				} else {
					List<DeviceVariable> deviceVariablesWithSameDeviceSectionIndex = new ArrayList<DeviceVariable>();
					deviceVariablesWithSameDeviceSectionIndex.add(deviceVariable);
					SAME_INDEX_VARIABLES_MAP.put(deviceDataSectionPos, deviceVariablesWithSameDeviceSectionIndex);
					SAME_INDEX_COMPOSED_NAMES.add(deviceDataSectionPos); //list集合：deviceDataSectionPos
				}
			}

			DEVICE_VARIABLES.add(deviceVariable);   //********添加到全局变量
			DEVICE_VARIABLE_MAP.put(deviceVariable.getSn(), deviceVariable); // 变量MAP (变量Sn,变量)
			realAddedVariables.add(deviceVariable); //*******添加到实际变量
		}

		Map<String, Map<String, Object>> dayValueMap = GLOBAL_DATA_STORAGE_SERVICE.loadDayValueMap(); //从数据库月表里加载每一条日数据

		Map<String, Map<String, Object>> hashKeyValues = new HashMap<String, Map<String, Object>>();
		LocalDateTime nowTime = LocalDateTime.now();  //当前时间
		//初始化时 将实际的变量加到redis
		for (DeviceVariable deviceVariable : realAddedVariables) {//遍历实际的变量

            //初始化设备变量值类型开关量为0，模拟量为0.000
			if (deviceVariable.getInitialValue() == null) {
				if (deviceVariable.getType().equals("Digital")) {
					deviceVariable.setRegisterValue("0");  //
				} else if (deviceVariable.getType().equals("Analog")) {
					deviceVariable.setRegisterValue("0.000");
				} else {
					logger.warn("global configuration invalid device variable type : " + deviceVariable.getType());
					deviceVariable.setRegisterValue("0");
				}
				deviceVariable.setUpdateTime(Timestamp.valueOf(nowTime));//设置更新时间
			} else {
				if (deviceVariable.getType().equals("Digital")) {
					if (deviceVariable.getInitialValue().floatValue() < 0.5D) {
						deviceVariable.setRegisterValue("0");
					} else {
						deviceVariable.setRegisterValue("1");
					}
				} else if (deviceVariable.getType().equals("Analog")) {
					deviceVariable.setRegisterValue(String.format("%.3f", new Object[] { deviceVariable.getInitialValue() }));
				} else {
					logger.warn("Global configuration invalid device variable type : " + deviceVariable.getType());
					deviceVariable.setRegisterValue("0");
				}
				deviceVariable.setUpdateTime(Timestamp.valueOf(nowTime));
			}
            //变量的累计值
			if (deviceVariable.getAccumulation() != null && !deviceVariable.getAccumulation().isEmpty()) {
				String[] arr = deviceVariable.getSn().split("__");
				String hashName = arr[0];
				String valueKey = arr[1];
				String timeKey = arr[1] + "_update_time";

				String valueStr = (String) GLOBAL_REDIS_SERVCIE.getCachedValues(hashName, valueKey); //从redis上获得值
				String timeStr = (String) GLOBAL_REDIS_SERVCIE.getCachedValues(hashName, timeKey); //从redis上获得时间值

				if (valueStr != null) {
					try {
						Float value = Float.valueOf(valueStr);
						Timestamp updateTime = Timestamp.valueOf(timeStr);

						Instant nowInstant = Instant.now();
						Instant updateTimeInstant = updateTime.toInstant();
						if ((deviceVariable.getAccumulation().equals("hour") && nowInstant
								.toEpochMilli()
								- updateTimeInstant.toEpochMilli() <= 3600000L)
								|| (deviceVariable.getAccumulation().equals(
										"day") && nowInstant.toEpochMilli()
										- updateTimeInstant.toEpochMilli() <= 86400000L)) {

							deviceVariable.setAccumulationBase(value.floatValue());
							deviceVariable.setUpdateTime(updateTime);
						}
					} catch (Exception e) {
						logger.warn("Global configuration error occured when convert string to double with value = " + valueStr);
					}
				}
			}
             //设置日变量数据到GLOBAL_MEMORY_BYTES
			if (dayValueMap.containsKey(deviceVariable.getSn())) {
				Map<String, Object> map = (Map) dayValueMap.get(deviceVariable.getSn());
				float maxValue = ((Float) map.get("max_value")).floatValue();
				deviceVariable.setMaxValue(maxValue);
				Timestamp maxValueTime = (Timestamp) map.get("max_value_time");
				deviceVariable.setMaxValueTime(maxValueTime);
				byte[] maxValueBytes = NumberUtil.intToByte4(Float.floatToIntBits(maxValue));
				byte[] maxValueTimeBytes = NumberUtil.longToByte8(maxValueTime.toInstant().toEpochMilli());

				float minValue = ((Float) map.get("min_value")).floatValue();
				deviceVariable.setMinValue(minValue);
				Timestamp minValueTime = (Timestamp) map.get("min_value_time");
				deviceVariable.setMinValueTime(minValueTime);
				byte[] minValueBytes = NumberUtil.intToByte4(Float.floatToIntBits(minValue));
				byte[] minValueTimeBytes = NumberUtil.longToByte8(minValueTime.toInstant().toEpochMilli());

				float averageValue = ((Float) map.get("average_value")).floatValue();
				deviceVariable.setSum(averageValue);
				Integer count = (Integer) map.get("count");
				deviceVariable.setCount((count == null) ? 1 : count.intValue());
				byte[] averageValueBytes = NumberUtil.intToByte4(Float.floatToIntBits(averageValue));

				float diffValue = ((Float) map.get("accu_value")).floatValue();
				byte[] diffValueBytes = NumberUtil.intToByte4(Float.floatToIntBits(diffValue));
				//变量块大小为80个字节
				System.arraycopy(maxValueBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 17, 4);  ///*80+17是什么意思？？？？
				System.arraycopy(maxValueTimeBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 25, 8);
				System.arraycopy(minValueBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 33, 4);
				System.arraycopy(minValueTimeBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 41, 8);
				System.arraycopy(averageValueBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 49, 4);
				System.arraycopy(diffValueBytes, 0, GLOBAL_MEMORY_BYTES, deviceVariable.getPosInMemory() * 80 + 57, 4);
			}

			if (GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER.value())) { //
				if (deviceVariable.getRegisterType() == null) {

					if (deviceVariable.getExpression() == null
							|| deviceVariable.getExpression().isEmpty()) {
						continue;
					}
				} else {
					RegisterType registerType = RegisterType.getRegisterType(deviceVariable.getRegisterType());
					if (registerType != null && !registerType.isReadable()) {
						continue;
					}
				}
			} else if (!GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.MQTT_CLIENT.value())
					&& !GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.KAFKA_CLIENT.value())) {

				logger.error("Global configuration error invalid run mode");
				return;
			}
            //变量sn分隔后存入到redis
			try {
				String[] arr = deviceVariable.getSn().split("__");

				String hash = arr[0];
				String key = arr[1];
				//存数据到redis缓存
				GLOBAL_DATA_STORAGE_SERVICE.addValueToReidsCache(hash, key, deviceVariable.getRegisterValue()); //***********变量sn分隔后存入到redis********

			}catch (Exception e){
				e.printStackTrace();
				logger.error("解析变量出错："+deviceVariable.getSn());
			}

		}

	}
    //更新设备变量
	public static void updateDeviceVariables(List<DeviceVariable> toBeUpdatedVariables) {
		if (toBeUpdatedVariables.isEmpty()) {
			return;
		}

		for (DeviceVariable deviceVariable : toBeUpdatedVariables) {

			DeviceVariable origDeviceVariable = getDeviceVariableBySn(deviceVariable
					.getSn());
			String expression = deviceVariable.getExpression();
			if (expression == null || expression.isEmpty()) {

				if ((deviceVariable.getDataType() == null && !deviceVariable
						.getType().equals("Digital"))
						|| (deviceVariable.getDataType() != null && DataType
								.getDataTypeByName(deviceVariable.getDataType()) == null)) {
					continue;
				}

				String newDeviceSn = deviceVariable.getCollectorDeviceSn();

				String newDeviceSectionPos = deviceVariable
						.getCollectorDeviceSn()
						+ "|"
						+ deviceVariable.getRegisterName()
						+ "|"
						+ deviceVariable.getRegisterIndex()
						+ "|"
						+ ((deviceVariable.getDataType() == null) ? 1
								: DataType.getDataTypeByName(
										deviceVariable.getDataType()).getSize());

				String origDeviceSectionPos = origDeviceVariable
						.getCollectorDeviceSn()
						+ "|"
						+ origDeviceVariable.getRegisterName()
						+ "|"
						+ origDeviceVariable.getRegisterIndex()
						+ "|"
						+ ((deviceVariable.getDataType() == null) ? 1
								: DataType.getDataTypeByName(
										deviceVariable.getDataType()).getSize());

				if (!newDeviceSectionPos.equals(origDeviceSectionPos)) {

					List<DeviceVariable> origDeviceVariables = (List) SAME_INDEX_VARIABLES_MAP
							.get(origDeviceSectionPos);
					Iterator<DeviceVariable> itor = origDeviceVariables
							.iterator();
					while (itor.hasNext()) {
						DeviceVariable tmp = (DeviceVariable) itor.next();
						if (tmp.getSn().equals(origDeviceVariable.getSn())) {
							itor.remove();
							break;
						}
					}
					if (origDeviceVariables.isEmpty()) {
						SAME_INDEX_VARIABLES_MAP.remove(origDeviceSectionPos);
						SAME_INDEX_COMPOSED_NAMES.remove(origDeviceSectionPos);
					}

					if (SAME_INDEX_COMPOSED_NAMES.contains(newDeviceSectionPos)) {
						((List) SAME_INDEX_VARIABLES_MAP.get(newDeviceSectionPos)).add(origDeviceVariable);
					} else {
						List<DeviceVariable> variableList = new ArrayList<DeviceVariable>();
						variableList.add(origDeviceVariable);
						SAME_INDEX_VARIABLES_MAP.put(newDeviceSectionPos,
								variableList);
						SAME_INDEX_COMPOSED_NAMES.add(newDeviceSectionPos);
					}
				}

				origDeviceVariable.setCollectorDeviceSn(newDeviceSn);
			}

			origDeviceVariable.setName(deviceVariable.getName());
			origDeviceVariable.setUnit(deviceVariable.getUnit());
			origDeviceVariable.setType(deviceVariable.getType());
			origDeviceVariable.setDeviceSn(deviceVariable.getDeviceSn());
			origDeviceVariable.setParentIndex(deviceVariable.getParentIndex());
			origDeviceVariable.setRecordPeriod(deviceVariable.getRecordPeriod());
			origDeviceVariable.setInitialValue(deviceVariable.getInitialValue());
			origDeviceVariable.setBaseValue(deviceVariable.getBaseValue());
			origDeviceVariable.setExpression(deviceVariable.getExpression());

			if (deviceVariable.getType().equals("Digital")) {

				origDeviceVariable.setZeroMeaning(deviceVariable.getZeroMeaning());
				origDeviceVariable.setZeroToOneAlarm(deviceVariable.getZeroToOneAlarm());
				origDeviceVariable.setOneMeaning(deviceVariable.getOneMeaning());
				origDeviceVariable.setOneToZeroAlarm(deviceVariable.getOneToZeroAlarm());
				origDeviceVariable.setBitPos(deviceVariable.getBitPos());
				origDeviceVariable.setIsOpposit(deviceVariable.getIsOpposit());
			} else if (deviceVariable.getType().equals("Analog")) {

				origDeviceVariable.setDataType(deviceVariable.getDataType());
				origDeviceVariable.setCoefficient(deviceVariable.getCoefficient());
				origDeviceVariable.setLowerLimit(deviceVariable.getLowerLimit());
				origDeviceVariable.setUpperLimit(deviceVariable.getUpperLimit());
				origDeviceVariable.setMoreLowerLimit(deviceVariable.getMoreLowerLimit());
				origDeviceVariable.setMoreUpperLimit(deviceVariable.getMoreUpperLimit());
				origDeviceVariable.setAccumulation(deviceVariable.getAccumulation());
				origDeviceVariable.setRecordType(deviceVariable.getRecordType());
			} else if (deviceVariable.getType().equals("String")) {

			}

			origDeviceVariable.setAlarm(deviceVariable.getAlarm());
			origDeviceVariable.setAlarmCondition(deviceVariable.getAlarmCondition());

			if (deviceVariable.getCollectorDeviceSn() != null
					&& !deviceVariable.getCollectorDeviceSn().isEmpty()) {
				origDeviceVariable.setRegisterName(deviceVariable.getRegisterName());
				origDeviceVariable.setRegisterIndex(deviceVariable.getRegisterIndex());
				origDeviceVariable.setRegisterType(deviceVariable.getRegisterType());
				origDeviceVariable.setRegisterTypeIndex(deviceVariable.getRegisterTypeIndex());
			}

			origDeviceVariable.setEventTriggers(deviceVariable.getEventTriggers());
		}
	}

	public static List<String> getSameIndexComposedNames() {
		return SAME_INDEX_COMPOSED_NAMES;
	}

	public static Map<String, List<DeviceVariable>> getSameIndexVariablesMap() {
		return SAME_INDEX_VARIABLES_MAP;
	}
      //删除设备变量
	public static void delDeviceVariables(List<DeviceVariable> toBeDeletedVariables) {
		if (toBeDeletedVariables.isEmpty()) {
			return;
		}
		for (DeviceVariable toBeDeletedVariable : toBeDeletedVariables) {
			Iterator<DeviceVariable> iterator = DEVICE_VARIABLES.iterator();
			while (iterator.hasNext()) {

				DeviceVariable deviceVariable = (DeviceVariable) iterator.next();
				if (deviceVariable.getSn().equals(toBeDeletedVariable.getSn())) {
					int pos = deviceVariable.getPosInMemory();
					if (pos != -1) {
						MEMORY_TAKEN_MAP.put(Integer.valueOf(pos),Boolean.valueOf(false));
					}

					String expression = deviceVariable.getExpression();
					if (expression == null || expression.isEmpty()) {

						String deviceDataSectionPos = deviceVariable.getCollectorDeviceSn()
								+ "|"+ deviceVariable.getRegisterName()
								+ "|"+ deviceVariable.getRegisterIndex()
								+ "|"+ ((deviceVariable.getDataType() == null) ? 1 : DataType.getDataTypeByName(deviceVariable.getDataType()).getSize());

						List<DeviceVariable> deviceVariables = (List) SAME_INDEX_VARIABLES_MAP
								.get(deviceDataSectionPos);
						if (null != deviceVariables && !deviceVariables.isEmpty()) {

							Iterator<DeviceVariable> subIter = deviceVariables.iterator();
							while (subIter.hasNext()) {
								DeviceVariable subDeviceVariable = (DeviceVariable) subIter.next();
								if (deviceVariable.getSn().equals(subDeviceVariable.getSn())) {
									subIter.remove();
									break;
								}
							}
							if (deviceVariables.isEmpty()) {
								SAME_INDEX_VARIABLES_MAP.remove(deviceDataSectionPos);
								SAME_INDEX_COMPOSED_NAMES.remove(deviceDataSectionPos);
							}
						}
					}

					iterator.remove();

					break;
				}
			}

			DEVICE_VARIABLE_MAP.remove(toBeDeletedVariable.getSn());
		}

		Map<String, List<String>> hashKeyValues = new HashMap<String, List<String>>();
		for (DeviceVariable deviceVariable : toBeDeletedVariables) {
			try {
				String[] arr = deviceVariable.getSn().split("__");
				String hashName = arr[0];
				String key = arr[1];

				if (hashKeyValues.containsKey(hashName)) {
					((List) hashKeyValues.get(hashName)).add(key);
					continue;
				}
				List<String> keys = new ArrayList<String>();
				keys.add(key);
				hashKeyValues.put(hashName, keys);
			} catch (Exception e) {
				logger.error("Global configuration error occured when parse device variable name : ",deviceVariable.getSn());
			}
		}
		try {
			// GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Object(hashKeyValues));
			GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
	          {
	            public void run() {
	              for (String hashName : hashKeyValues.keySet()) {
	                for (String key : hashKeyValues.get(hashName)) {
	                  GlobalVariables.GLOBAL_REDIS_SERVCIE.delete(hashName, key);  ///调用redis删除
	                }
	              } 
	            }
	          });
		} catch (Exception e) {
			logger.error("Global configuration error occured when call redis delete key method");
		}
	}

	public static List<DeviceVariable> getDeviceVariablesInParentIndex(
			String parentIndex) {
		List<DeviceVariable> deviceVariables = new ArrayList<DeviceVariable>();

		for (DeviceVariable deviceVariable : DEVICE_VARIABLES) {
			if (deviceVariable.getParentIndex().equals(parentIndex)) {
				deviceVariables.add(deviceVariable);
			}
		}

		return deviceVariables;
	}

	public static List<DeviceVariable> getDeviceVariables() {
		return DEVICE_VARIABLES;
	}

	public static DeviceVariable getDeviceVariableBySn(String sn) {
		if (DEVICE_VARIABLE_MAP.containsKey(sn)) {
			return (DeviceVariable) DEVICE_VARIABLE_MAP.get(sn);
		}

		return null;
	}

	public static void updateDeviceVariableBySn(String sn,
			String registerValue, Timestamp updateTime) {
		if (DEVICE_VARIABLE_MAP.containsKey(sn)) {
			DeviceVariable deviceVariable = (DeviceVariable) DEVICE_VARIABLE_MAP.get(sn);
			deviceVariable.setRegisterValue(registerValue);
			deviceVariable.setUpdateTime(updateTime);
		}
	}

	public static void updateDeviceVariableAlarmInfoBySn(String sn,
			String reportId, Timestamp reportTime, StatusType alarmType) {
		if (DEVICE_VARIABLE_MAP.containsKey(sn)) {
			DeviceVariable deviceVariable = (DeviceVariable) DEVICE_VARIABLE_MAP.get(sn);
			deviceVariable.setReportId(reportId);
			deviceVariable.setReportTime(reportTime);
			deviceVariable.setAlarmType(alarmType);
		}
	}

	public static Set<String> getRelatedDeviceVariableSns(String variableSn) {
		if (RELATED_VARIABLE_MAP.containsKey(variableSn)) {
			return (Set) RELATED_VARIABLE_MAP.get(variableSn);
		}
		return null;
	}
     //得到内存中任一空闲的变量位置
	public static int getFreeVariablePosInMemory() {
		for (Integer key : MEMORY_TAKEN_MAP.keySet()) {               //MEMORY_TAKEN_MAP{28：0，29：1，30：0}
			if (!((Boolean) MEMORY_TAKEN_MAP.get(key)).booleanValue()) {//map值为0时空闲，返回key内存地址值
				return key.intValue();
			}
		}
		return -1;
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\BOOT-INF\classes\com\kkwl\collector\common\GlobalVariables.class Java
 * compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */