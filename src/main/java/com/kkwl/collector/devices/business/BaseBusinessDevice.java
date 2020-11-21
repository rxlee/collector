package com.kkwl.collector.devices.business;

import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.BaseDevice;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.utils.LogTools;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseBusinessDevice extends BaseDevice {
	private static final Logger logger = LoggerFactory.getLogger(BaseBusinessDevice.class);
	private static final int IO_BUF_SIZE = 20480;
	private static final int PARSE_BUF_SIZE = 2048;
	private String dtuSn;
	protected byte[] inBytesBuffer;  //*******************接收的数据数组********************
	protected int inBytesBufferStart;  //
	protected int inBytesBufferLength;  //长度
	protected Lock inBytesBufferLock;
	protected LinkedBlockingQueue<Pair<FrameType, byte[]>> outBytesBuffer; //要发送的数据（帧类型，字节内容）队列
	protected LinkedBlockingQueue<String> inStringsBuffer; //接受到的字符串 队列
	protected LinkedBlockingQueue<String> outStringsBuffer; //要发送的字符串 队列
	protected boolean hasDataToSend;
	protected boolean hasDataToHandle;
	protected String deviceAddress;

	protected String protocolParams;
	protected boolean canSwitchToNext;
	protected boolean needHistoryFlag; //需要历史数据
	protected Instant historyBeginInstant;
	protected Instant historyEndInstant;
	protected Instant historyCurrentQueryInstant;
	protected Instant lastHistoryCalledTime;
	protected final List<String> dataSectionNames;
	protected final Map<String, Quartet<Integer, Integer, String, byte[]>> dataSectionMap;
	protected final Map<String, String> dataSectionNameTypeMap;
	protected final Map<String, Boolean> updatedSectionPos;
	protected final Map<String, LinkedBlockingQueue<Pair<Long, byte[]>>> historyValueMap;
	protected boolean isDeviceBeingUsed;

	public BaseBusinessDevice(String sn, String dtuSn, String name, String parentIndex, String type, String protocolParams, String deviceAddress, Byte deviceBeingUsed, long handlingPeriod, byte shouldReportAlarm, short connectionTimeoutDuration, String offlineReportId, Timestamp offlineTime, StatusType initStatus)
	{
		this.inBytesBufferLock = new ReentrantLock();

		this.dataSectionNames = new ArrayList();
		this.dataSectionMap = new HashMap();
		this.dataSectionNameTypeMap = new HashMap();
		this.updatedSectionPos = new HashMap();
		this.historyValueMap = new HashMap();

		this.sn = sn;
		setDtuSn(dtuSn);
		this.name = name;
		this.parentIndex = parentIndex;
		this.type = type;
		this.protocolParams = protocolParams;
		this.deviceAddress = deviceAddress;
		this.inBytesBuffer = new byte[20480];
		this.inBytesBufferStart = 0;
		this.inBytesBufferLength = 0;
		this.outBytesBuffer = new LinkedBlockingQueue();
		this.inStringsBuffer = new LinkedBlockingQueue();
		this.outStringsBuffer = new LinkedBlockingQueue();
		this.hasDataToSend = false;
		this.hasDataToHandle = false;
		this.isDeviceBeingUsed = true;
		this.lastReceivedPacketTime = (initStatus == StatusType.OFFLINE && offlineTime != null) ? offlineTime
				.toInstant() : Instant.now();
		this.status = initStatus;
		this.handlingPeriod = handlingPeriod;
		this.canSwitchToNext = false;
		this.shouldReportConnectionAlarm = shouldReportAlarm;
		this.connectionTimeoutDuration = connectionTimeoutDuration;
		this.offlineReportId = offlineReportId;
		this.offlineTime = offlineTime;
		this.needHistoryFlag = false; //

		if (deviceBeingUsed != null && deviceBeingUsed.byteValue() == 1) {
			this.isDeviceBeingUsed = true;
		} else {
			this.isDeviceBeingUsed = false;
		}
	}



	public boolean getNeedHisgoryFlag() {
		return this.needHistoryFlag;
	}

	public void setNeedHistoryFlag(boolean isNeeded) {
		this.needHistoryFlag = isNeeded;
	}

	public void setHistoryBeginInstant(Instant historyBeginInstant) {
		this.historyBeginInstant = historyBeginInstant;
	}

	public void setHistoryEndInstant(Instant historyEndInstant) {
		this.historyEndInstant = historyEndInstant;
	}

	public void setHistoryCurrentQueryInstant(Instant historyCurrentQueryInstant) {
		this.historyCurrentQueryInstant = historyCurrentQueryInstant;
	}

	public void setLastHistoryCalledTime(Instant lastHistoryCalledTime) {
		this.lastHistoryCalledTime = lastHistoryCalledTime;
	}

	public Instant getHistoryBeginInstant() {
		return this.historyBeginInstant;
	}

	public Instant getHistoryEndInstant() {
		return this.historyEndInstant;
	}

	public void delete() {
		LogTools.log(logger, this.sn, LogType.DEBUG,
				"Base business device sn = " + this.sn
						+ " base delete is called.");
	}

	public void onConnected() {
	}

	public void onDisconnected() {
	}

	public abstract void doBusiness();

	public void handleValueChangedMessage(String deviceVariableSn,
			String deviceVariableValue, String type) {
	}

	public void doParse() {
		LogTools.log(logger, this.sn, LogType.DEBUG,
				"Base business device sn = " + this.sn + " empty doParse.");
	}

	public abstract void parseProtocolVariable();

	public abstract void toggleFlag(FrameType paramFrameType);

	public abstract void initDataType();

	public abstract byte[] getSectionBytes(String paramString, int paramInt1, int paramInt2);

	public abstract byte[] getSectionBytesForDebug(String paramString,
			int paramInt1, int paramInt2);

	public abstract Byte getSectionByte(String paramString, int paramInt);

	public abstract Byte getSectionByteForDebug(String paramString, int paramInt);

	public abstract Pair<Long, Byte> getHistorySectionByte(String paramString, int paramInt);

	public abstract Pair<Long, byte[]> getHistorySectionBytes(String paramString, int paramInt1, int paramInt2);

	public void resetCanSwitchToNext() {
	}
    //********************将通道接收的内容添加到inBytesBuffer数组 byte[]***************************
	public void addInInBytesBuffer(byte[] buf) {
		this.inBytesBufferLock.lock(); //锁

		try {
			if (this.inBytesBufferStart + buf.length < 20480) {  ///遥信1H--4000H，遥测4001H--5000H（20480）地址小于遥测最大

				System.arraycopy(buf, 0, this.inBytesBuffer, this.inBytesBufferStart, buf.length); //把缓存区内容复制到inBytesBuffer
				this.inBytesBufferStart += buf.length;
				this.inBytesBufferLength += buf.length;
			}
		} finally {
			this.inBytesBufferLock.unlock();
		}
	}
         //取设备收到的内容值
	protected byte[] getInBytesBuffer() {
		byte[] retBytes;
		int len = Math.min(2048, this.inBytesBufferLength); //取最小
		if (len == 0) {
			return null;
		}
		this.inBytesBufferLock.lock();
		try {
			retBytes = new byte[len];
			System.arraycopy(this.inBytesBuffer, 0, retBytes, 0, len);
			this.inBytesBufferLength -= len;
			if (this.inBytesBufferLength > 0) { //如果inBytesBufferLength大于2048，len=2048
				System.arraycopy(this.inBytesBuffer, len, this.inBytesBuffer, 0, this.inBytesBufferLength); //将之前的覆盖
			}
			this.inBytesBufferStart -= len;
		} finally {
			this.inBytesBufferLock.unlock();
		}
		return retBytes;
	}

	public Queue<Pair<FrameType, byte[]>> getOutBytesBuffer() {
		return this.outBytesBuffer;
	}

	public Queue<String> getInStringsBuffer() {
		return this.inStringsBuffer;
	}

	public Queue<String> getOutStringsBuffer() {
		return this.outStringsBuffer;
	}

	public boolean hasDataToSend() {
		return this.hasDataToSend;
	}

	public void setHasDataToSend(boolean hasDataToSend) {
		this.hasDataToSend = hasDataToSend;
	}

	public Timestamp getLastDataPackteTime() {
		return Timestamp.from(this.lastReceivedPacketTime);
	}

	public boolean isDeviceBeingUsed() {
		return this.isDeviceBeingUsed;
	}

	public boolean canSwitchToNext() {
		return this.canSwitchToNext;
	}

	public String getDtuSn() {
		return this.dtuSn;
	}

	public void setDtuSn(String dtuSn) {
		this.dtuSn = dtuSn;
	}

	public int getInBytesBufferStart() {
		return inBytesBufferStart;
	}

	public void setInBytesBufferStart(int inBytesBufferStart) {
		this.inBytesBufferStart = inBytesBufferStart;
	}

	public int getInBytesBufferLength() {
		return inBytesBufferLength;
	}

	public void setInBytesBufferLength(int inBytesBufferLength) {
		this.inBytesBufferLength = inBytesBufferLength;
	}

	public Lock getInBytesBufferLock() {
		return inBytesBufferLock;
	}

	public void setInBytesBufferLock(Lock inBytesBufferLock) {
		this.inBytesBufferLock = inBytesBufferLock;
	}

	public boolean isHasDataToHandle() {
		return hasDataToHandle;
	}

	public void setHasDataToHandle(boolean hasDataToHandle) {
		this.hasDataToHandle = hasDataToHandle;
	}

	public String getDeviceAddress() {
		return deviceAddress;
	}

	public void setDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}

	public String getProtocolParams() {
		return protocolParams;
	}

	public void setProtocolParams(String protocolParams) {
		this.protocolParams = protocolParams;
	}

	public boolean isCanSwitchToNext() {
		return canSwitchToNext;
	}

	public void setCanSwitchToNext(boolean canSwitchToNext) {
		this.canSwitchToNext = canSwitchToNext;
	}

	public boolean isHasDataToSend() {
		return hasDataToSend;
	}

	public boolean isNeedHistoryFlag() {
		return needHistoryFlag;
	}

	public Instant getHistoryCurrentQueryInstant() {
		return historyCurrentQueryInstant;
	}

	public Instant getLastHistoryCalledTime() {
		return lastHistoryCalledTime;
	}

	public List<String> getDataSectionNames() {
		return dataSectionNames;
	}

	public Map<String, Quartet<Integer, Integer, String, byte[]>> getDataSectionMap() {
		return dataSectionMap;
	}

	public Map<String, String> getDataSectionNameTypeMap() {
		return dataSectionNameTypeMap;
	}

	public Map<String, Boolean> getUpdatedSectionPos() {
		return updatedSectionPos;
	}

	public Map<String, LinkedBlockingQueue<Pair<Long, byte[]>>> getHistoryValueMap() {
		return historyValueMap;
	}

	public void setInBytesBuffer(byte[] inBytesBuffer) {
		this.inBytesBuffer = inBytesBuffer;
	}

	public void setOutBytesBuffer(
			LinkedBlockingQueue<Pair<FrameType, byte[]>> outBytesBuffer) {
		this.outBytesBuffer = outBytesBuffer;
	}

	public void setInStringsBuffer(LinkedBlockingQueue<String> inStringsBuffer) {
		this.inStringsBuffer = inStringsBuffer;
	}

	public void setOutStringsBuffer(LinkedBlockingQueue<String> outStringsBuffer) {
		this.outStringsBuffer = outStringsBuffer;
	}

	public void setDeviceBeingUsed(boolean isDeviceBeingUsed) {
		this.isDeviceBeingUsed = isDeviceBeingUsed;
	}
	
	
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\
 * BOOT-INF\classes\com\kkwl\collector\devices\business\BaseBusinessDevice.class
 * Java compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */