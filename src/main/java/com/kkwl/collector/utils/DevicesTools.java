package com.kkwl.collector.utils;

import com.kkwl.collector.common.AccessType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.RunMode;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.business.BaseClientDevice;
import com.kkwl.collector.devices.business.BaseIEC101Device;
import com.kkwl.collector.devices.business.BaseIEC104Device;
import com.kkwl.collector.devices.business.BaseModbusRTUDevice;
import com.kkwl.collector.devices.business.BaseModbusTCPDevice;
import com.kkwl.collector.devices.business.HGNYClientDevice;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.exception.IllegalConfigurationParameterException;
import com.kkwl.collector.models.CollectorDevice;
import com.kkwl.collector.models.CollectorDtu;
import com.kkwl.collector.models.DataSection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicesTools {
	private static Logger logger = LoggerFactory.getLogger(DevicesTools.class);
	//采集通道绑定对应的所有采集设备，全局添加通道服务端/通道客户端
	public static void createAllComponents(List<CollectorDtu> dtus, List<CollectorDevice> devices)
	{
		Map<String, List<CollectorDevice>> dtuDeviceMap = new HashMap<String, List<CollectorDevice>>();
		//将所有采集设备分组
		//格式：{通道sn:[该通道下的采集设备1，该通道下的采集设备2.....]}
		for (CollectorDevice device : devices) {
			if (dtuDeviceMap.containsKey(device.getDtuSn())) {
				((List) dtuDeviceMap.get(device.getDtuSn())).add(device);
				continue;
			}
			List<CollectorDevice> newDeviceList = new ArrayList<CollectorDevice>();
			newDeviceList.add(device);
			dtuDeviceMap.put(device.getDtuSn(), newDeviceList); //通道：设备list
		}
		//匹配当前的运行模式
		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER.value())) { //如果运行模式是 TCP_SERVER
			for (CollectorDtu dtu : dtus) {
				//根据采集通道创建通道服务端
				ServerDTU serverDtu = createServerDTU(dtu);
				//获取当前通道中对应的采集设备
				List<CollectorDevice> collectorDevices = (List) dtuDeviceMap.get(dtu.getSn());
				if (collectorDevices != null) {
					List<BaseBusinessDevice> deviceList = createBusinessDevicesFromCollectorDevices(collectorDevices);
					//通道服务端绑定采集设备
					serverDtu.setDevices(deviceList);
				}
				//全局加入通道服务端
				GlobalVariables.GLOBAL_SERVER_DTUS.add(serverDtu);
			}
		}

		else if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.MQTT_CLIENT.value()) //如果运行模式是 MQTT_CLIENT/KAFKA_CLIENT
				|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
						.equals(RunMode.KAFKA_CLIENT.value())) {
			for (CollectorDtu dtu : dtus) {//mqtt创建通道客户端（用于第三中方式）
				ClientDTU clientDTU = createClientDTU(dtu);

				List<CollectorDevice> collectorDevices = (List) dtuDeviceMap.get(dtu.getSn());
				List<BaseBusinessDevice> deviceList = createBusinessDevicesFromCollectorDevices(collectorDevices); //工作设备list

				clientDTU.setDevices(deviceList);
				GlobalVariables.GLOBAL_CLIENT_DTUS.add(clientDTU); //全局加入通道客户端
			}
		}
	}

	/**
	 * 创建通道服务端
	 * @param collectorDtu  通道对象
	 * @return
	 */
	public static ServerDTU createServerDTU(CollectorDtu collectorDtu) {
		String dtuSn = collectorDtu.getSn();//通道sn
		String dtuIdentifier = collectorDtu.getIdentifier();//通讯注册码
		String dtuIpAddress = collectorDtu.getIpAddress();//通道连接的服务ip（上报ip）
		Integer dtuPort = collectorDtu.getPort();//通道连接的端口（上报端口号）
		int dtuTimeInterval = collectorDtu.getTimeInterval();//采集周期(单位：毫秒) 
		String dtuName = collectorDtu.getName();//通道名称
		AccessType dtuAccessType = AccessType.getAccessTypeByName(collectorDtu
				.getAccessType());//通道接方式
		String dtuParentIndex = collectorDtu.getParentIndex();
		byte dtuShouldReportConnectionAlarm = collectorDtu
				.getShouldReportConnectionAlarm();//是否上报连接告警
		short dtuConnectionTimeoutDuration = collectorDtu
				.getConnectionTimeoutDuration();//连接超时时长（秒）
		StatusType initStatus = (collectorDtu.getStatus() == null || collectorDtu
				.getStatus().intValue() == 1) ? StatusType.ONLINE
				: StatusType.OFFLINE;//默认状态
		// ServerDTU.Builder dtuBuilder = new ServerDTU.Builder(dtuSn, dtuName,
		// dtuParentIndex, collectorDtu.getLastOfflineReportId(),
		// collectorDtu.getLastOfflineReportTime(), initStatus, "DTU");
		
		//String sn, String name, String parentIndex, String identifier, String type, String ipAddress, 
		//Integer port, int timeInterval, long handlingPeriod, byte shouldReportConnectionAlarm, short connectionTimeoutDuration, 
		//AccessType accessType, String offlineReportId, Timestamp offlineTime, StatusType initStatus
		ServerDTU dtuBuilder = new ServerDTU(
				dtuSn, 
				dtuName, 
				dtuParentIndex,
				dtuIdentifier,
				"DTU",//设置类型 固定为DTU
				dtuIpAddress,
				dtuPort,
				dtuTimeInterval,
				GlobalVariables.GLOBAL_DEVICE_HANDING_PERIOD,
				dtuShouldReportConnectionAlarm,
				dtuConnectionTimeoutDuration,
				dtuAccessType,
				collectorDtu.getLastOfflineReportId(),
				collectorDtu.getLastOfflineReportTime(), 
				initStatus 
				);
		if ((dtuIpAddress == null || dtuIpAddress.isEmpty())
				&& (dtuIdentifier == null || dtuIdentifier.isEmpty())) {
			logger.error("Device tools ip address and identifier can't be empty at the same time.");
			throw new IllegalConfigurationParameterException(
					"Device tools ip address and identifier can't be empty at the same time.");
		}

		if (dtuIpAddress != null && !dtuIpAddress.isEmpty()
				&& (dtuPort == null || dtuPort.intValue() == 0)) {
			logger.error("Device tools port number illegal.");
			throw new IllegalConfigurationParameterException(
					"Device tools port illegal : " + dtuPort);
		}

//		return dtuBuilder
//				.setTimeInterval(dtuTimeInterval)
//				.setHandlingPeriod(GlobalVariables.GLOBAL_DEVICE_HANDING_PERIOD)
//				.setIdentifier(dtuIdentifier).setIpAddress(dtuIpAddress)
//				.setPort(dtuPort)
//				.setShouldReportConnectionAlarm(dtuShouldReportConnectionAlarm)
//				.setConnectionTimeoutDuration(dtuConnectionTimeoutDuration)
//				.setAccessType(dtuAccessType).build();
		
		//TODO
			 	dtuBuilder.setTimeInterval(dtuTimeInterval);
				dtuBuilder.setHandlingPeriod(GlobalVariables.GLOBAL_DEVICE_HANDING_PERIOD);
				dtuBuilder.setIdentifier(dtuIdentifier);
				dtuBuilder.setIpAddress(dtuIpAddress);
				dtuBuilder.setPort(dtuPort);
				dtuBuilder.setShouldReportConnectionAlarm(dtuShouldReportConnectionAlarm);
				dtuBuilder.setConnectionTimeoutDuration(dtuConnectionTimeoutDuration);
				dtuBuilder.setAccessType(dtuAccessType);
				
				return dtuBuilder;
	}

	///创建通道客户端
	public static ClientDTU createClientDTU(CollectorDtu collectorDtu) {
		String dtuSn = collectorDtu.getSn(); //采集设备sn
		String dtuIdentifier = collectorDtu.getIdentifier();
		String dtuIpAddress = collectorDtu.getIpAddress();
		Integer dtuPort = collectorDtu.getPort();
		int dtuTimeInterval = collectorDtu.getTimeInterval();
		String dtuName = collectorDtu.getName();
		String dtuAccessType = collectorDtu.getAccessType();
		String dtuParentIndex = collectorDtu.getParentIndex();
		byte dtuShouldReportConnectionAlarm = collectorDtu
				.getShouldReportConnectionAlarm();
		short dtuConnectionTimeoutDuration = collectorDtu
				.getConnectionTimeoutDuration();
		StatusType initStatus = (collectorDtu.getStatus() == null || collectorDtu
				.getStatus().intValue() == 1) ? StatusType.ONLINE
				: StatusType.OFFLINE;
		// ClientDTU.Builder dtuBuilder = new ClientDTU.Builder(dtuSn, dtuName,
		// dtuParentIndex, collectorDtu.getLastOfflineReportId(),
		// collectorDtu.getLastOfflineReportTime(), initStatus, "DTU");
		
		
		//TODO
		
		//String sn, String name, String parentIndex, String type,
		//byte shouldReportConnectionAlarm, short connectionTimeoutDuration,
		// offlineReportId, Timestamp offlineTime, StatusType initStatus
		ClientDTU dtuBuilder = new ClientDTU(dtuSn, dtuName, dtuParentIndex,"DTU",//??
				dtuShouldReportConnectionAlarm,dtuConnectionTimeoutDuration,
				collectorDtu.getLastOfflineReportId(),
				collectorDtu.getLastOfflineReportTime(), initStatus );

		// return
		// dtuBuilder.setShouldReportConnectionAlarm(dtuShouldReportConnectionAlarm).setConnectionTimeoutDuration(dtuConnectionTimeoutDuration).build();
		// } public static BaseBusinessDevice createBusinessDevice(String
		// deviceType, CollectorDevice collectorDevice) { BaseIEC101Device
		// baseIEC101DeviceBuilder;
		 
		dtuBuilder.setShouldReportConnectionAlarm(dtuShouldReportConnectionAlarm);
		dtuBuilder.setConnectionTimeoutDuration(dtuConnectionTimeoutDuration);
		 return dtuBuilder;
	}

	/**
	 * 创建待工作的设备
	 * @param deviceType 设备类别
	 * @param collectorDevice  采集设备对象
	 * @return
	 */
	public static BaseBusinessDevice createBusinessDevice(String deviceType, CollectorDevice collectorDevice) {
		BaseIEC101Device baseIEC101DeviceBuilder;
		HGNYClientDevice hgnyMqttDeviceBuilder; // 创建的HGNYClientDevice
		BaseClientDevice baseMqttDeviceBuilder; //
		BaseModbusTCPDevice baseModbusTCPDeviceBuilder;
		BaseModbusRTUDevice baseModbusRTUDeviceBuilder;
		BaseIEC104Device baseIEC104DeviceBuilder; // 创建的BaseIEC104Device
		String deviceProtocolType = collectorDevice.getProtocolType();//协议类别
		String deviceSn = collectorDevice.getSn();//采集设备sn
		String dtuSn = collectorDevice.getDtuSn();//通道sn
		String deviceName = collectorDevice.getName();//采集设备名称
		String deviceParentIndex = collectorDevice.getParentIndex();//站点
		String deviceProtocolStr = collectorDevice.getProtocolParams();//协议参数
		String deviceAddress = collectorDevice.getAddress();//地址
		Byte deviceBeingUsed = collectorDevice.getDeviceBeingUsed();//是否正在被使用
		Byte shouldReportAlarm = Byte.valueOf((collectorDevice
				.getReportConnectionAlarm() == null) ? 0 : collectorDevice
				.getReportConnectionAlarm().byteValue());//是否上报连接告警
		Short connectionTimeoutDuration = Short.valueOf((collectorDevice
				.getConnectionTimeoutDuration() == null) ? 300
				: collectorDevice.getConnectionTimeoutDuration().shortValue());//连接超时时长
		long handlingPeriod = GlobalVariables.GLOBAL_DEVICE_HANDING_PERIOD;//处理周期
		StatusType initStatus = (collectorDevice.getStatus() == null || collectorDevice
				.getStatus().intValue() == 1) ? StatusType.ONLINE
				: StatusType.OFFLINE;//默认状态

		
		//按照设备类别对应哪个对象
		/*
		GLOBAL_DEVICE_TYPE_MAP.put("IEC104", "BaseIEC104Device");
		GLOBAL_DEVICE_TYPE_MAP.put("MODBUSRTU", "BaseModbusRTUDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("MODBUSTCP", "BaseModbusTCPDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("MQTT_HGNY", "HGNYClientDevice");
		GLOBAL_DEVICE_TYPE_MAP.put("IEC101", "BaseIEC101Device");
		 * 
		 */
		switch (deviceType) {

		case "BaseIEC104Device":
//			baseIEC104DeviceBuilder = new BaseIEC104Device(deviceSn, dtuSn,
//					deviceName, deviceParentIndex, deviceProtocolType,
//					shouldReportAlarm.byteValue(),
//					connectionTimeoutDuration.shortValue(),
//					collectorDevice.getLastOfflineReportId(),
//					collectorDevice.getLastOfflineReportTime(), initStatus);
			//TODO
			//String sn, String dtuSn, String name, String parentIndex, String type, String protocolParams,
			//String deviceAddress, Byte deviceBeingUsed, long handlingPeriod, byte shouldReportAlarm, 
			//short connectionTimeoutDuration, String offlineReportId, Timestamp offlineTime, StatusType initStatus, List<DataSection> dataSections) {
			baseIEC104DeviceBuilder = new BaseIEC104Device(deviceSn, dtuSn, deviceName, deviceParentIndex, deviceProtocolType,deviceProtocolStr,deviceAddress,deviceBeingUsed,handlingPeriod,
			shouldReportAlarm.byteValue(), connectionTimeoutDuration.shortValue(), collectorDevice.getLastOfflineReportId(),
			collectorDevice.getLastOfflineReportTime(), initStatus,collectorDevice.getDataSections());

//			return baseIEC104DeviceBuilder.setDeviceAddress(deviceAddress)
//					.setProtocolParams(deviceProtocolStr)
//					.setDeviceBeingUsed(deviceBeingUsed)
//					.setHandlingPeriod(handlingPeriod)
//					.setDataSections(collectorDevice.getDataSections()).build();
			
			baseIEC104DeviceBuilder.setDeviceAddress(deviceAddress);
			baseIEC104DeviceBuilder.setProtocolParams(deviceProtocolStr);
//			baseIEC104DeviceBuilder.setDeviceBeingUsed(deviceBeingUsed);
			baseIEC104DeviceBuilder.setHandlingPeriod(handlingPeriod);
//			baseIEC104DeviceBuilder.setDataSections(collectorDevice.getDataSections());

			 return baseIEC104DeviceBuilder;
		case "BaseModbusRTUDevice":
			//TODO
			baseModbusRTUDeviceBuilder = new BaseModbusRTUDevice(deviceSn,
					dtuSn, deviceName, deviceParentIndex, deviceProtocolType,
					deviceProtocolStr,deviceAddress,deviceBeingUsed,handlingPeriod,
					shouldReportAlarm.byteValue(),
					connectionTimeoutDuration.shortValue(),
					collectorDevice.getLastOfflineReportId(),
					collectorDevice.getLastOfflineReportTime(), initStatus,collectorDevice.getDataSections());

//			return baseModbusRTUDeviceBuilder.setDeviceAddress(deviceAddress)
//					.setProtocolParams(deviceProtocolStr)
//					.setDeviceBeingUsed(deviceBeingUsed)
//					.setHandlingPeriod(handlingPeriod)
//					.setDataSections(collectorDevice.getDataSections()).build();
			
			baseModbusRTUDeviceBuilder.setDeviceAddress(deviceAddress);
			baseModbusRTUDeviceBuilder.setProtocolParams(deviceProtocolStr);
//			baseModbusRTUDeviceBuilder.setDeviceBeingUsed(deviceBeingUsed);
			baseModbusRTUDeviceBuilder.setHandlingPeriod(handlingPeriod);
//			baseModbusRTUDeviceBuilder.setDataSections(collectorDevice.getDataSections());
			
			return baseModbusRTUDeviceBuilder ;

		case "BaseModbusTCPDevice":
			//TODO  
			baseModbusTCPDeviceBuilder = new BaseModbusTCPDevice(deviceSn,
					dtuSn, deviceName, deviceParentIndex, deviceProtocolType,
					deviceProtocolStr,deviceAddress,deviceBeingUsed,handlingPeriod,
					shouldReportAlarm.byteValue(),
					connectionTimeoutDuration.shortValue(),
					collectorDevice.getLastOfflineReportId(),
					collectorDevice.getLastOfflineReportTime(), initStatus,collectorDevice.getDataSections());

//			return baseModbusTCPDeviceBuilder.setDeviceAddress(deviceAddress)
//					.setProtocolParams(deviceProtocolStr)
//					.setDeviceBeingUsed(deviceBeingUsed)
//					.setHandlingPeriod(handlingPeriod)
//					.setDataSections(collectorDevice.getDataSections()).build();
			
			//TODO
			 baseModbusTCPDeviceBuilder.setDeviceAddress(deviceAddress);
			baseModbusTCPDeviceBuilder.setProtocolParams(deviceProtocolStr);
//			baseModbusTCPDeviceBuilder.setDeviceBeingUsed(deviceBeingUsed)
			baseModbusTCPDeviceBuilder.setHandlingPeriod(handlingPeriod);
//			baseModbusTCPDeviceBuilder.setDataSections(collectorDevice.getDataSections());
			
			return baseModbusTCPDeviceBuilder;

		case "BaseClientDevice":
			//TODO 
			baseMqttDeviceBuilder = new BaseClientDevice(deviceSn, dtuSn,
					deviceName, deviceParentIndex, deviceType,deviceBeingUsed,
					Long.valueOf(handlingPeriod),
					shouldReportAlarm.byteValue(),
					connectionTimeoutDuration.shortValue(),
					collectorDevice.getLastOfflineReportId(),
					collectorDevice.getLastOfflineReportTime(), initStatus);

//			return baseMqttDeviceBuilder.setDeviceBeingUsed(deviceBeingUsed)
//					.setDeviceBeingUsed(deviceBeingUsed)
//					.setHandlingPeriod(Long.valueOf(handlingPeriod)).build();
			
			////参数和属性类型不一致，在父级构造函数中已经赋值了
//			baseMqttDeviceBuilder.setDeviceBeingUsed(deviceBeingUsed);
			baseMqttDeviceBuilder.setHandlingPeriod(Long.valueOf(handlingPeriod));
			
			return baseMqttDeviceBuilder;
			

		case "HGNYClientDevice":
			// TODO
			hgnyMqttDeviceBuilder = new HGNYClientDevice(deviceSn, dtuSn,
					deviceName, deviceParentIndex, deviceType,
					deviceBeingUsed,
					Long.valueOf(handlingPeriod),
					shouldReportAlarm.byteValue(),
					connectionTimeoutDuration.shortValue(),
					deviceProtocolStr,
					collectorDevice.getLastOfflineReportId(),
					collectorDevice.getLastOfflineReportTime(), initStatus);

			// return hgnyMqttDeviceBuilder
			// .setProtocolParams(deviceProtocolStr)
			// .setDeviceBeingUsed(deviceBeingUsed)
			// .setHandlingPeriod(Long.valueOf(handlingPeriod)).build();
			
			hgnyMqttDeviceBuilder.setProtocolParams(deviceProtocolStr);
			//参数和属性类型不一致，在父级构造函数中已经赋值了
//			hgnyMqttDeviceBuilder.setDeviceBeingUsed(deviceBeingUsed);
			hgnyMqttDeviceBuilder.setHandlingPeriod(Long
					.valueOf(handlingPeriod));
			return hgnyMqttDeviceBuilder;

		case "BaseIEC101Device":
			// TODO
			baseIEC101DeviceBuilder = new BaseIEC101Device(
					deviceSn, // sn
					dtuSn, // dtuSn
					deviceName, // name
					deviceParentIndex, // parentIndex
					deviceProtocolType, // type
					deviceProtocolStr,// protocolParams,
					deviceAddress, // deviceAddress
					deviceBeingUsed, // deviceBeingUsed
					handlingPeriod, // handlingPeriod
					shouldReportAlarm.byteValue(), // shouldReportAlarm
					connectionTimeoutDuration.shortValue(), // connectionTimeoutDuration
					collectorDevice.getLastOfflineReportId(), // offlineReportId
					collectorDevice.getLastOfflineReportTime(), // Timestamp
					initStatus,// initStatus
					collectorDevice.getDataSections() // dataSections
			);

//			return baseIEC101DeviceBuilder.setDeviceAddress(deviceAddress)
//					.setProtocolParams(deviceProtocolStr)
//					.setDeviceBeingUsed(deviceBeingUsed)
//					.setHandlingPeriod(handlingPeriod)
//					.setDataSections(collectorDevice.getDataSections()).build();
			
			 baseIEC101DeviceBuilder.setDeviceAddress(deviceAddress);
			 baseIEC101DeviceBuilder.setProtocolParams(deviceProtocolStr);
			 //参数类不一致，在父级的构造函数中已经赋值了（boolean）。
//			 baseIEC101DeviceBuilder.setDeviceBeingUsed(deviceBeingUsed);
			 baseIEC101DeviceBuilder.setHandlingPeriod(handlingPeriod);
			 
			 //没有dataSections属性
//			 baseIEC101DeviceBuilder.setDataSections(collectorDevice.getDataSections());
			 return baseIEC101DeviceBuilder;
		}
		return null;
	}

	/**
	 * 从采集设备中创建工作的采集设备
	 * @param collectorDevices
	 * @return
	 */
	private static List<BaseBusinessDevice> createBusinessDevicesFromCollectorDevices(List<CollectorDevice> collectorDevices) {
		List<BaseBusinessDevice> deviceList = new ArrayList<BaseBusinessDevice>();
		for (CollectorDevice collectorDevice : collectorDevices) {
			//获取协议类型
			String deviceProtocolType = collectorDevice.getProtocolType();
			//根据协议区分是哪一类设备
			String deviceClassName = (String) GlobalVariables.GLOBAL_DEVICE_TYPE_MAP.get(deviceProtocolType);
			if (deviceClassName == null) {//找不到设备类别
				logger.warn("Device tools can't find device type of :"
						+ deviceProtocolType);

				continue;
			}
			//创建待工作的设备（设备类别，采集设备）
			BaseBusinessDevice device = createBusinessDevice(deviceClassName, collectorDevice); //返回一个设备实例
			if (device == null) {
				logger.warn("Device tools can't initialize object type = "
						+ deviceClassName);

				continue;
			}
			device.initDataType();
			device.parseProtocolVariable(); //解析协议变量
			deviceList.add(device);

			GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES.add(device);
			GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.put(device.getSn(),
					device);
		}

		return deviceList; //返回工作设备list
	}
}
