package com.kkwl.collector.services;

import com.kkwl.collector.channels.mqtt.MqttMonitorClient;
import com.kkwl.collector.channels.netty.NettyClient;
import com.kkwl.collector.common.AccessType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.RunMode;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.models.CollectorDevice;
import com.kkwl.collector.models.CollectorDtu;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.services.ConfigurationChangedNotificationReceiver;
import com.kkwl.collector.services.DeviceVariableService;
import com.kkwl.collector.services.EventTypesService;
import com.kkwl.collector.utils.DevicesTools;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

//配置改变通知接收器
@Component
public class ConfigurationChangedNotificationReceiver {
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangedNotificationReceiver.class);
	//CountDownLatch是一个计数器闭锁，通过它可以完成类似于阻塞当前线程的功能，即：一个线程或多个线程一直等待，直到其他线程执行的操作完成
	private final CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	private Configuration configurationDBHandler;

	@Autowired
	private DeviceVariableService deviceVariableService;

	@Autowired
	private EventTypesService eventTypesService;

	@Autowired
	private MqttMonitorClient mqttMonitorClient;

	@Value("${netty.bind.public_ip_address}")
	private String ipAddress;

	@Value("${netty.bind.port}")
	private String ports;

	@Value("${netty.client.reconnect-duration}")
	private Integer reconnectDuration;

/*	@RabbitHandler
	@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${com.kkwl.collector.amqp.configuration_changed_notification_queue_name}",durable="true"),
	                exchange = @Exchange(value = "${com.kkwl.collector.amqp.configuration_changed_notification_exchange_name}",type = "topic",durable = "true"),
	                key = "${com.kkwl.collector.amqp.configuration_changed_notification_routing_key}"))*/
	public void receiveMessage(@Payload String message) {
		logger.info("Notification message receiver received : " + message);

		try {
			JSONObject msgObject = new JSONObject(message);
			String action = null;
			if (msgObject.has("action")) {
				action = msgObject.getString("action");
			} else {
				logger.warn("Notification message receiver no action in message");

				return;
			}
			if (action == null || action.isEmpty()) {
				logger.warn("Notification message receiver empty action type : "
						+ action);

				return;
			}
			String type = null;
			if (msgObject.has("type")) {
				type = msgObject.getString("type");
			} else {
				logger.warn("Notification message receiver no type in message");

				return;
			}
			if (type == null || type.isEmpty()) {
				logger.warn("Notification message receiver empty message type : "
						+ type);

				return;
			}
			JSONObject detailObject = null;
			if (msgObject.has("detail")) {
				detailObject = msgObject.getJSONObject("detail");
			} else {
				logger.warn("Notification message receiver no detail in message");

				return;
			}
			if (detailObject == null) {
				logger.warn("Notification message receiver empty detail");

				return;
			}
			switch (action) {
			case "update":
				GlobalVariables.GLOBAL_CONFIGURATION_CHANGING_QUEUE.add(new Triplet(type, detailObject, "update"));
				break;
			case "delete":
				GlobalVariables.GLOBAL_CONFIGURATION_CHANGING_QUEUE.add(new Triplet(type, detailObject, "delete"));
				break;
			}
		} catch (JSONException ex) {
			logger.error(
					"Notification message receiver error occured when parsing json string: ",
					ex);
		} catch (Exception ex) {
			logger.error("Notification message receiver unexpected error: ", ex);
		}

		this.latch.countDown();
	}

	public CountDownLatch getLatch() {
		return this.latch;
	}

	public void updateProcess(String type, JSONObject detail) {
		String temp;
		List<String> dtuSns;
		List<String> deviceSns;
		String parentIndex;
		switch (type) {
		case "device_variable":
			if (detail.has("parent_index")) {
				parentIndex = detail.getString("parent_index");
				updateDeviceVariables(parentIndex);
				break;
			}
			logger.warn("Notification message receiver empty parent_index");
			break;

		case "collector_device":
			parentIndex = null;
			if (detail.has("parent_index")) {
				parentIndex = detail.getString("parent_index");
			} else {
				logger.warn("Notification message receiver key parent_index doesn't exist");
			}

			deviceSns = new ArrayList<String>();
			if (detail.has("device_sns")) {
				JSONArray deviceArr = detail.getJSONArray("device_sns");
				for (int i = 0; i < deviceArr.length(); i++) {
					String deviceSn = deviceArr.getString(i);
					deviceSns.add(deviceSn);
				}
			} else {
				logger.warn("Notification message receiver key devices doesn't exist");
			}

			updateDevices(parentIndex, deviceSns);
			break;
		case "collector_dtu":
			dtuSns = new ArrayList<String>();
			if (detail.has("dtu_sns")) {
				JSONArray dtuArr = detail.getJSONArray("dtu_sns");
				for (int i = 0; i < dtuArr.length(); i++) {
					String dtuSn = dtuArr.getString(i);
					dtuSns.add(dtuSn);
				}
			} else {
				logger.warn("Notification message receiver key devices doesn't exist");
			}

			updateDtus(dtuSns);
			break;
		case "alarm_type":
			temp = null;
			if (detail.has("parent_index")) {
				temp = detail.getString("parent_index");
			} else {
				logger.warn("Update alarm_type Notification message receiver key parent_index doesn't exist");
			}
			if (temp != null && !temp.isEmpty()) {
				this.eventTypesService.updateEventTypesByParentIndex(temp);
			}
			break;
		case "alarm_generater":
			if (detail.has("parent_index")) {
				updateDeviceVariables(detail.getString("parent_index"));
				break;
			}
			logger.warn("Notification message receiver empty parent_index");
			break;
		}
	}

	public void deleteProcess(String type, JSONObject detail) {
		List<String> dtuSns;
		List<String> deviceSns;
		switch (type) {
		case "collector_device":
			deviceSns = new ArrayList<String>();
			if (detail.has("device_sns")) {
				JSONArray deviceArr = detail.getJSONArray("device_sns");
				for (int i = 0; i < deviceArr.length(); i++) {
					String deviceSn = deviceArr.getString(i);
					deviceSns.add(deviceSn);
				}
			} else {
				logger.warn("Notification message receiver there isn't devices key");
			}

			if (deviceSns.isEmpty()) {
				logger.warn("Notification message receiver devices list is empty");

				return;
			}
			deleteCollectorDevices(deviceSns, Boolean.valueOf(true));
			break;
		case "collector_dtu":
			dtuSns = new ArrayList<String>();
			if (detail.has("dtu_sns")) {
				JSONArray deviceArr = detail.getJSONArray("dtu_sns");
				for (int i = 0; i < deviceArr.length(); i++) {
					String dtuSn = deviceArr.getString(i);
					dtuSns.add(dtuSn);
				}
			} else {
				logger.warn("Notification message receiver there isn't devices key");
			}
			if (dtuSns.isEmpty()) {
				logger.warn("Notification message receiver devices list is empty");

				return;
			}
			deleteDtus(dtuSns, Boolean.valueOf(true));
			break;
		}
	}
   //更新设备变量
	private void updateDeviceVariables(String parentIndex) {
		List<DeviceVariable> newDeviceVariables = this.deviceVariableService.getDeviceVariablesByVarParentIndexs(parentIndex);

		if (newDeviceVariables.isEmpty()) {
			logger.warn("Notification message receiver collector device variable size = 0");
		}

		List<String> newDeviceVariableSns = new ArrayList<String>();
		for (DeviceVariable deviceVariable : newDeviceVariables) {
			newDeviceVariableSns.add(deviceVariable.getSn());
		}

		List<DeviceVariable> toBeUpdatedDeviceVariableSns = new ArrayList<DeviceVariable>();
		List<DeviceVariable> toBeAddedDeviceVariables = new ArrayList<DeviceVariable>();
		List<DeviceVariable> originalDeviceVariables = GlobalVariables
				.getDeviceVariablesInParentIndex(parentIndex);
		List<String> originalDeviceVariableSns = new ArrayList<String>();
		List<DeviceVariable> toBeDeletedDeviceVariabvles = new ArrayList<DeviceVariable>();
		for (DeviceVariable deviceVariable : originalDeviceVariables) {
			originalDeviceVariableSns.add(deviceVariable.getSn());

			if (!newDeviceVariableSns.contains(deviceVariable.getSn())) {
				toBeDeletedDeviceVariabvles.add(deviceVariable);
			}
		}

		for (DeviceVariable deviceVariable : newDeviceVariables) {
			if (originalDeviceVariableSns.contains(deviceVariable.getSn())) {
				toBeUpdatedDeviceVariableSns.add(deviceVariable);
				continue;
			}
			toBeAddedDeviceVariables.add(deviceVariable);
		}

		GlobalVariables.delDeviceVariables(toBeDeletedDeviceVariabvles);
		GlobalVariables.addDeviceVariables(toBeAddedDeviceVariables);

		GlobalVariables.updateDeviceVariables(toBeUpdatedDeviceVariableSns);
	}
   //更新设备
	private void updateDevices(String parentIndex, List<String> deviceSns) {
		List<CollectorDevice> collectorDevices = null;
		Map<String, Object> filter = new HashMap<String, Object>();
		if (parentIndex == null || parentIndex.isEmpty()) {
			filter.put("collector_device_sns", deviceSns);
			collectorDevices = this.configurationDBHandler.getDevices(filter);
		} else {
			filter.put("parent_index", parentIndex);
			collectorDevices = this.configurationDBHandler.getDevices(filter);
		}

		if (collectorDevices == null || collectorDevices.isEmpty()) {
			logger.info("Notification message receiver no collector devices");

			return;
		}
		filterCollectorDeviceInThisCollector(collectorDevices);
		if (collectorDevices.isEmpty()) {
			return;
		}

		List<String> collectorDevicesSns = new ArrayList<String>();
		for (CollectorDevice collectorDevice : collectorDevices) {
			collectorDevicesSns.add(collectorDevice.getSn());
		}
		deleteCollectorDevices(collectorDevicesSns, Boolean.valueOf(false));

		Set<String> needToBeResetDtuSns = new HashSet<String>();
		for (CollectorDevice collectorDevice : collectorDevices) {
			String deviceProtocolType = collectorDevice.getProtocolType();
			if (deviceProtocolType == null) {
				logger.warn("Notification message receiver can't get deviceProtocolType sn = "
						+ collectorDevice.getSn());

				continue;
			}
			String deviceClassName = (String) GlobalVariables.GLOBAL_DEVICE_TYPE_MAP
					.get(deviceProtocolType);
			BaseBusinessDevice baseBusinessDevice = DevicesTools.createBusinessDevice(deviceClassName, collectorDevice);
			if (baseBusinessDevice == null) {
				logger.warn("Notification message receiver can't create BaseBusinessDevice sn = "
						+ collectorDevice.getSn());

				continue;
			}
			baseBusinessDevice.initDataType();
			baseBusinessDevice.parseProtocolVariable(); //解析协议变量

			if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.TCP_SERVER.value())) {
				for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
					if (dtu.getSn().equals(collectorDevice.getDtuSn())) {
						logger.info("Notification message receiver add collector device sn = "
								+ baseBusinessDevice.getSn()
								+ " to dtu sn = "
								+ dtu.getSn());
						dtu.addDevice(baseBusinessDevice);
						needToBeResetDtuSns.add(dtu.getSn());
						break;
					}
				}
			} else if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.MQTT_CLIENT.value())
					|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
							.equals(RunMode.KAFKA_CLIENT.value())) {
				for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
					if (dtu.getSn().equals(collectorDevice.getDtuSn())) {
						logger.info("Notification message receiver add collector device sn = "
								+ baseBusinessDevice.getSn()
								+ " to dtu sn = "
								+ dtu.getSn());
						dtu.addDevice(baseBusinessDevice);

						break;
					}
				}
			}
			GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES
					.add(baseBusinessDevice);
			GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.put(
					baseBusinessDevice.getSn(), baseBusinessDevice);
		}

		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER
				.value())) {
			for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
				if (needToBeResetDtuSns.contains(dtu.getSn())) {
					dtu.disconnect();
				}
			}
		}

		logger.debug("Notification message receiver collector device updated.");
	}
   //更新通道
	private void updateDtus(List<String> dtuSns) {
		Map<String, Object> filter = new HashMap<String, Object>();
		filter = new HashMap<String, Object>();
		filter.put("collector_dtu_sns", dtuSns);
		List<CollectorDtu> collectorDtus = this.configurationDBHandler
				.getDtus(filter);

		filterDtuInThisCollector(collectorDtus);

		if (collectorDtus.isEmpty()) {
			logger.info("Notification message receiver no dtu should be updated.");

			return;
		}

		List<String> toBeUpdatedDtuSns = new ArrayList<String>();
		for (CollectorDtu tmpDtu : collectorDtus) {
			toBeUpdatedDtuSns.add(tmpDtu.getSn());
		}

		filter = new HashMap<String, Object>();
		filter.put("collector_dtu_sns", toBeUpdatedDtuSns);
		List<CollectorDevice> collectorDevices = this.configurationDBHandler
				.getDevices(filter);

		List<String> collectorDeviceSns = new ArrayList<String>();
		for (CollectorDevice collectorDevice : collectorDevices) {
			collectorDeviceSns.add(collectorDevice.getSn());
		}

		deleteDtus(toBeUpdatedDtuSns, Boolean.valueOf(false));

		DevicesTools.createAllComponents(collectorDtus, collectorDevices);

		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER
				.value())) {
			for (CollectorDtu collectorDtu : collectorDtus) {
				if (collectorDtu.getAccessType().equals(
						AccessType.TCPCLIENT.value())) {

					NettyClient nettyClient = new NettyClient(
							collectorDtu.getSn(), collectorDtu.getIpAddress(),
							collectorDtu.getPort().intValue(),
							this.reconnectDuration.intValue());
					nettyClient.start();
					GlobalVariables.NETTY_CLIENT_LIST.add(nettyClient);
				}
			}
		} else if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
				.equals(RunMode.MQTT_CLIENT.value())) {

			Pair<String[], int[]> topics = this.mqttMonitorClient
					.createTopics(dtuSns);

			if (!GlobalVariables.MQTT_CLIENT.isConnected()) {
				this.mqttMonitorClient.connect();
			}

			if (topics != null && topics.getValue0().length != 0) {
				for (String topicName : (String[]) topics.getValue0()) {
					MqttTopic topic = GlobalVariables.MQTT_CLIENT
							.getTopic(topicName);

					GlobalVariables.MQTT_OPTIONS.setWill(topic, ("topic "
							+ topicName + " closed").getBytes(), 2, true);
				}

				try {
					GlobalVariables.MQTT_CLIENT.subscribe(
							(String[]) topics.getValue0(),
							(int[]) topics.getValue1());
				} catch (MqttException ex) {
					logger.error(
							"Notification message receiver error occured when adding new topic ",
							ex);
				}
			}
		}
	}
   //删除通道
	private void deleteDtus(List<String> dtuSns, Boolean deleteVarFlag) {
		Map<String, Object> filter = new HashMap<String, Object>();
		filter = new HashMap<String, Object>();
		filter.put("collector_dtu_sns", dtuSns);
		List<CollectorDevice> collectorDevices = this.configurationDBHandler
				.getDevices(filter);

		List<String> collectorDeviceSns = new ArrayList<String>();
		for (CollectorDevice collectorDevice : collectorDevices) {
			collectorDeviceSns.add(collectorDevice.getSn());
		}

		deleteCollectorDevices(collectorDeviceSns, deleteVarFlag);

		deleteGlobalCollectorDTUs(dtuSns);

		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
				.equals(RunMode.MQTT_CLIENT.value())) {

			if (!GlobalVariables.MQTT_CLIENT.isConnected()) {
				this.mqttMonitorClient.connect();
			}

			Pair<String[], int[]> topics = this.mqttMonitorClient
					.createTopics(dtuSns);

			if (topics != null &&  topics.getValue0().length != 0) {
				try {
					GlobalVariables.MQTT_CLIENT.unsubscribe((String[]) topics
							.getValue0());
				} catch (MqttException ex) {
					logger.error(
							"Notification message receiver error occured when adding new topic ",
							ex);
				}
			}
		} else if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
				.equals(RunMode.TCP_SERVER.value())) {

			for (String dtuSn : dtuSns) {
				Iterator<NettyClient> nettyClientIterator = GlobalVariables.NETTY_CLIENT_LIST
						.iterator();
				while (nettyClientIterator.hasNext()) {
					NettyClient nettyClient = (NettyClient) nettyClientIterator
							.next();
					if (nettyClient.getSn().equals(dtuSn)) {
						nettyClient.stop();
						nettyClientIterator.remove();
					}
				}
			}
		}
	}

	private void deleteCollectorDevices(List<String> collectorDevicesSns,
			Boolean deleteVarFlag) {
		if (deleteVarFlag.booleanValue()) {

			List<DeviceVariable> toBeDeletedDeviceVariables = new ArrayList<DeviceVariable>();
			for (DeviceVariable deviceVariable : GlobalVariables
					.getDeviceVariables()) {
				if (deviceVariable.getCollectorDeviceSn() != null
						&& !deviceVariable.getCollectorDeviceSn().isEmpty()
						&& collectorDevicesSns.contains(deviceVariable
								.getCollectorDeviceSn())) {
					toBeDeletedDeviceVariables.add(deviceVariable);
				}
			}
			GlobalVariables.delDeviceVariables(toBeDeletedDeviceVariables);
		}

		for (String collectorDeviceSn : collectorDevicesSns) {
			BaseBusinessDevice tempDevice = null;
			if (GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP
					.containsKey(collectorDeviceSn)) {
				tempDevice = (BaseBusinessDevice) GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP
						.get(collectorDeviceSn);

				if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
						.equals(RunMode.TCP_SERVER.value())) {
					for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
						if (dtu.getSn().equals(tempDevice.getDtuSn()))
							dtu.delDevice(collectorDeviceSn);
					}
					continue;
				}
				if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
						.equals(RunMode.MQTT_CLIENT.value())
						|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
								.equals(RunMode.KAFKA_CLIENT.value())) {
					for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
						if (dtu.getSn().equals(tempDevice.getDtuSn()))
							dtu.delDevice(collectorDeviceSn);
					}
					continue;
				}
				logger.error("Notification message receiver invalid run mode.");
			}
		}

		deleteGlobalCollectorDevices(collectorDevicesSns);
	}

	private void deleteGlobalCollectorDTUs(List<String> dtuSns) {
		for (String dtuSn : dtuSns) {

			if (GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP
					.containsKey(dtuSn)
					&& !((ChannelHandlerContext) GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP
							.get(dtuSn)).isRemoved()) {
				((ChannelHandlerContext) GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP
						.get(dtuSn)).close();
				GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP
						.remove(dtuSn);
			}

			if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.TCP_SERVER.value())) {
				Iterator<ServerDTU> iterator = GlobalVariables.GLOBAL_SERVER_DTUS
						.iterator();
				while (iterator.hasNext()) {
					ServerDTU serverDtu = (ServerDTU) iterator.next();
					if (serverDtu.getSn().equals(dtuSn)) {
						serverDtu.delete();
						iterator.remove();
					}
				}
				continue;
			}
			if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.MQTT_CLIENT.value())
					|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
							.equals(RunMode.KAFKA_CLIENT.value())) {
				Iterator<ClientDTU> iterator = GlobalVariables.GLOBAL_CLIENT_DTUS
						.iterator();
				while (iterator.hasNext()) {
					ClientDTU clientDTU = (ClientDTU) iterator.next();
					if (clientDTU.getSn().equals(dtuSn)) {
						clientDTU.delete();
						iterator.remove();
					}
				}
				continue;
			}
			return;
		}
	}

	private void deleteGlobalCollectorDevices(List<String> collectorDevicesSns) {
		for (String tempSn : collectorDevicesSns) {
			GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.remove(tempSn);
		}

		Iterator<BaseBusinessDevice> iterator = GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES
				.iterator();
		while (iterator.hasNext()) {
			BaseBusinessDevice baseBusinessDevice = (BaseBusinessDevice) iterator
					.next();
			if (collectorDevicesSns.contains(baseBusinessDevice.getSn())) {
				baseBusinessDevice.delete();
				iterator.remove();
			}
		}
	}

	private void filterCollectorDeviceInThisCollector(
			List<CollectorDevice> collectorDevices) {
		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER
				.value())) {
			String[] portArr = this.ports.split(",");
			List<String> portList = new ArrayList<String>();
			Collections.addAll(portList, portArr);

			Iterator<CollectorDevice> it = collectorDevices.iterator();
			while (it.hasNext()) {
				CollectorDevice collectorDevice = (CollectorDevice) it.next();
				if (collectorDevice.getDtuAccessType() != null
						|| collectorDevice.getDtuAccessType().equals(
								AccessType.TCPCLIENT.value())) {
					continue;
				}
				if (collectorDevice.getCollectorIpAddress().equals(
						this.ipAddress)
						&& portList
								.contains(collectorDevice.getCollectorPort())) {
					continue;
				}
				it.remove();
			}
		} else {
			if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.MQTT_CLIENT.value())
					|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
							.equals(RunMode.KAFKA_CLIENT.value())) {
				return;
			}

			logger.warn("Notification message receiver invalid run mode, do not update.");
			return;
		}
	}

	private void filterDtuInThisCollector(List<CollectorDtu> collectorDtus) {
		if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER
				.value())) {
			String[] portArr = this.ports.split(",");
			List<String> portList = new ArrayList<String>();
			Collections.addAll(portList, portArr);

			Iterator<CollectorDtu> it = collectorDtus.iterator();
			while (it.hasNext()) {
				CollectorDtu collectorDtu = (CollectorDtu) it.next();
				if (collectorDtu.getAccessType() != null
						|| collectorDtu.getAccessType().equals(
								AccessType.TCPCLIENT.value())) {
					continue;
				}
				if (collectorDtu.getCollectorIpAddress().equals(this.ipAddress)
						&& portList.contains(collectorDtu.getCollectorPort())) {
					continue;
				}
				it.remove();
			}
		} else {
			if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
					.equals(RunMode.MQTT_CLIENT.value())
					|| GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE
							.equals(RunMode.KAFKA_CLIENT.value())) {
				return;
			}

			logger.warn("Notification message receiver invalid run mode, do not update.");
			return;
		}
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\BOOT-INF\classes\com\kkwl\collector\services\
 * ConfigurationChangedNotificationReceiver.class Java compiler version: 8
 * (52.0) JD-Core Version: 1.0.7
 */