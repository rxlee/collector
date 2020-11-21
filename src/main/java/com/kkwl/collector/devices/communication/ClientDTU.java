package com.kkwl.collector.devices.communication;

import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.BaseDevice;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.models.response.LogViewResponse;
import com.kkwl.collector.utils.LogTools;
import com.kkwl.collector.utils.TimeFormatter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDTU extends BaseDevice {
	private static final Logger logger = LoggerFactory.getLogger(ClientDTU.class);
	private List<BaseBusinessDevice> devices;

	public ClientDTU(String sn, String name, String parentIndex, String type, byte shouldReportConnectionAlarm, short connectionTimeoutDuration,
			String offlineReportId, Timestamp offlineTime, StatusType initStatus) {
		this.devices = new ArrayList();
		this.deviceIndexLock = new ReentrantLock();
		this.sn = sn;
		this.name = name;
		this.parentIndex = parentIndex;
		this.type = type;
		this.shouldReportConnectionAlarm = shouldReportConnectionAlarm;
		this.connectionTimeoutDuration = connectionTimeoutDuration;
		this.status = initStatus;
		this.lastReceivedPacketTime = (initStatus == StatusType.OFFLINE && offlineTime != null) ? offlineTime
				.toInstant() : Instant.now();
		this.offlineReportId = offlineReportId;
		this.offlineTime = offlineTime;
	}

	private ReentrantLock deviceIndexLock;
//处理收到的数据
	public void handleReceivedData(String deviceSn, String payLoad) { //（仪表sn,载荷字符串）
		Instant now = Instant.now();
		setLastReceivedPacketTime(now);

		if (this.devices.isEmpty()) {
			return;
		}

		if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null && this.sn.equals(GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU)) {
			LogViewResponse response = new LogViewResponse();
			response.setDeviceSn(this.sn);
			response.setAction("RECV");
			response.setTime(TimeFormatter.timestampToCommonString(Timestamp
					.from(now)));
			response.setContent(payLoad);
			GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.add(response); //****供码流使用***** ？？？？？？？？？？？重复
		}

		lockDeviceLock();
		try {
			for (BaseBusinessDevice device : this.devices) {
				if (device.getType().equals("HGNYClientDevice")) {

					if (device.getSn().endsWith("_" + deviceSn)) {
						device.getInStringsBuffer().add(payLoad);
						device.setLastReceivedPacketTime(now);
					}

				} else if (device.getSn().equals(deviceSn)) {
					device.getInStringsBuffer().add(payLoad); //把负载添加到 字符串缓冲区
					device.setLastReceivedPacketTime(now);
				}

				device.doParse(); //******************解析********************
			}
		} finally {
			this.deviceIndexLock.unlock();
		}
	}

	public void onDisconnected() {
	}

	public void delete() {
	}

	public void setDevices(List<BaseBusinessDevice> devices) {
		this.devices = devices;
	}

	public void addDevice(BaseBusinessDevice toBeAddedDevice) {
		lockDeviceLock();

		try {
			this.devices.add(toBeAddedDevice);
		} finally {
			this.deviceIndexLock.unlock();
		}
	}

	public void delDevice(String toBeDeletedDeviceSn) {
		lockDeviceLock();

		try {
			Iterator<BaseBusinessDevice> deviceIterator = this.devices
					.iterator();
			while (deviceIterator.hasNext()) {
				BaseBusinessDevice tmpDevice = (BaseBusinessDevice) deviceIterator
						.next();
				if (tmpDevice.getSn().equals(toBeDeletedDeviceSn)) {
					tmpDevice.delete();
					deviceIterator.remove();
					break;
				}
			}
		} finally {
			this.deviceIndexLock.unlock();
		}
	}

	public Timestamp getLastDataPackteTime() {
		return Timestamp.from(this.lastReceivedPacketTime);
	}

	private void lockDeviceLock() {
		while (!this.deviceIndexLock.tryLock()) {

			boolean shouldReturning = false;
			try {
				Thread.sleep(5L);
			} catch (InterruptedException e) {
				LogTools.log(logger, this.sn, LogType.INFO, "Dtu sn = "
						+ this.sn + " waiting for getting device index lock.");
				shouldReturning = true;
			}

			if (shouldReturning)
				return;
		}
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\
 * BOOT-INF\classes\com\kkwl\collector\devices\communication\ClientDTU.class
 * Java compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */