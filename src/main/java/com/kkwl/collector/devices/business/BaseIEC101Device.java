package com.kkwl.collector.devices.business;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.models.DataSection;
import com.kkwl.collector.protocols.BaseProtocol;
import com.kkwl.collector.protocols.IEC101.IEC101ProtocolAdapter;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.LogTools;

public class BaseIEC101Device extends BaseBusinessDevice {
	private static final Logger logger = LoggerFactory
			.getLogger(BaseIEC101Device.class);

	private BaseProtocol protocol;

	List<Pair<String, String>> ykCommandList;

	private int ykAddress;

	private byte ykValue;

	private boolean hasSentConnectCommand;

	private boolean hasSentResetCommand;

	private boolean hasSentTestCommand;
	private boolean hasSentRecruitmentCommand;
	private boolean hasSentCorrectTimeCommand;
	private boolean hasSentYKSelectCommand;
	private boolean hasSentYKExecuteCommand;
	private Instant lastTestTime;
	private Instant lastConnectTime;
	private Instant lastResetTime;
	private Instant lastZzTime;
	private Instant lastCorrectTimeTime;
	private StatusType protocolStatus;
	private boolean requireIFrameAck;
	private Instant lastIFrameTime;
	private boolean useJS;
	private int timeoutT0 = 30;
	private int timeoutT1 = 15;
	private int timeoutT2 = 10;
	private int timeoutT3 = 20;
	private int zzTimeInterval = 120;
	private int jsTimeInterval = 1800;
	private byte testCount = 0;

	private Integer deviceAddressInt;

	private String lastIFrameAction;

	private ScheduledFuture iFrameTimeCounter;

	// private BaseIEC101Device
	public BaseIEC101Device(String sn, String dtuSn, String name,
			String parentIndex, String type, String protocolParams,
			String deviceAddress, Byte deviceBeingUsed, long handlingPeriod,
			byte shouldReportAlarm, short connectionTimeoutDuration,
			String offlineReportId, Timestamp offlineTime,
			StatusType initStatus, List<DataSection> dataSections) {
		super(sn, dtuSn, name, parentIndex, type, protocolParams,
				deviceAddress, deviceBeingUsed, handlingPeriod,
				shouldReportAlarm, connectionTimeoutDuration, offlineReportId,
				offlineTime, initStatus);

		for (DataSection dataSection : dataSections) {
			int sectionStart = dataSection.getStart().intValue();
			int sectionLength = dataSection.getLength().intValue();
			String sectionType = dataSection.getType();
			byte[] sectionBytes = new byte[sectionLength];

			if (sectionType.equals("YC") && sectionStart >= 16385) {
				sectionStart -= 16385;
			} else if (sectionType.equals("YK") && sectionStart >= 24577) {
				sectionStart -= 24577;
			}

			Quartet<Integer, Integer, String, byte[]> newDataSection = new Quartet<Integer, Integer, String, byte[]>(
					Integer.valueOf(sectionStart),
					Integer.valueOf(sectionLength), sectionType, sectionBytes);
			this.dataSectionMap.put(dataSection.getName(), newDataSection);
			this.dataSectionNames.add(dataSection.getName());
			this.dataSectionNameTypeMap.put(dataSection.getName(), sectionType);
		}

		this.protocol = new IEC101ProtocolAdapter();

		this.ykCommandList = new ArrayList();

		try {
			this.deviceAddressInt = Integer.valueOf(Integer
					.parseInt(deviceAddress));
		} catch (Exception e) {
			this.deviceAddressInt = null;
			LogTools.log(logger, sn, LogType.DEBUG, "Base 101 device sn = "
					+ sn + " encountered invalid address "
					+ this.deviceAddressInt, e);
		}

		Instant currentInstant = Instant.now();
		this.lastConnectTime = currentInstant.minusSeconds(1800L);
		this.lastResetTime = currentInstant.minusSeconds(1800L);
		this.lastCorrectTimeTime = currentInstant.minusSeconds(1800L);
		this.hasSentConnectCommand = false;
		this.hasSentRecruitmentCommand = false;
		this.hasSentTestCommand = false;
		this.hasSentCorrectTimeCommand = false;
		this.hasSentYKSelectCommand = false;
		this.hasSentYKExecuteCommand = false;
		this.protocolStatus = StatusType.IDLE;
	}

	public void doParse() {
		if (this.protocol == null) {
			return;
		}

		List<Map<String, Object>> parsedValueMapList = null;
		byte[] toBeParsed = getInBytesBuffer();
		if (toBeParsed == null) {
			return;
		}

		Map<String, Object> protocolParams = new HashMap<String, Object>();
		protocolParams.put("sn", this.sn);
		protocolParams.put("address", this.deviceAddressInt);

		boolean shouldCountinue = true;
		while (shouldCountinue) {
			parsedValueMapList = this.protocol.doBytesParse(toBeParsed,
					protocolParams);
			for (Map<String, Object> parsedValueMap : parsedValueMapList) {
				if (parsedValueMap != null
						&& ((Boolean) parsedValueMap.get("result"))
								.booleanValue() == true) {
					FrameType frameType = (FrameType) parsedValueMap
							.get("type");
					if (this.protocolStatus == StatusType.ONLINE) {

						if (frameType.value().equals(
								FrameType.IEC101_RECRUITMENT_ACK_FRAME.value())) {
							handleRecruitmentAckFrame();
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_RECRUITMENT_COMPLETE_FRAME
										.value())) {
							handleRecruitmentCompleteFrame();
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_YX_FRAME_FRAME.value())) {
							handleYXFrame(parsedValueMap);
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_YC_FRAME_FRAME.value())) {
							handleYCFrame(parsedValueMap);
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_TEST_ACK_FRAME.value())) {
							handleTestAckFrame();
							continue;
						}
						if (frameType.value()
								.equals(FrameType.IEC101_CORRECT_TIME_ACK_FRAME
										.value())) {
							handleCorrectTimeAckFrame();
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_YK_SELECT_ACK_FRAME.value())
								&& this.hasSentYKSelectCommand == true) {
							handleYKSelectAckFrame(parsedValueMap);
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_YK_EXECUTE_ACK_FRAME.value())
								&& this.hasSentYKExecuteCommand == true) {
							handleYKExecuteAckFrame(parsedValueMap);
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_TEST_FRAME.value())) {
							handleTestFrame();
							continue;
						}
						if (frameType.value().equals(
								FrameType.IEC101_SOE_FRAME.value())) {
							handleSOEFrame(parsedValueMap);
						}
						continue;
					}
					if (frameType.value().equals(
							FrameType.IEC101_CONNECT_ACK_FRAME.value())) {
						handleConnectAckFrame();
						continue;
					}
					if (frameType.value().equals(
							FrameType.IEC101_RESET_ACK_FRAME.value())) {
						handleResecAckFrame();
					}
					continue;
				}
				shouldCountinue = false;
			}
		}
	}

	public void doBusiness() {
		Instant currentInstant = Instant.now();

		if (this.protocolStatus == StatusType.IDLE) {
			Duration duration = Duration.between(this.lastConnectTime,
					currentInstant);
			if (!this.hasSentConnectCommand) {
				LogTools.log(
						logger,
						this.sn,
						LogType.DEBUG,
						"Base 101 device sn = "
								+ this.sn
								+ " "
								+ duration.getSeconds()
								+ " seconds has passed since last connect command.");

				sendConnectCommand();
				this.lastConnectTime = currentInstant;
			} else if (duration.getSeconds() > this.timeoutT0) {
				this.hasSentConnectCommand = false;
			}

		} else if (this.protocolStatus == StatusType.CONNECTED) {
			Duration durtion = Duration.between(this.lastResetTime,
					currentInstant);
			if (!this.hasSentResetCommand) {
				sendResetCommand();
				this.lastResetTime = currentInstant;
			} else if (durtion.getSeconds() > 10L) {
				this.protocolStatus = StatusType.IDLE;
				this.hasSentResetCommand = false;
				this.hasSentConnectCommand = false;
				this.lastConnectTime = currentInstant
						.minusSeconds(this.timeoutT1);
			}

		} else if (this.protocolStatus == StatusType.ONLINE) {
			Duration idleInterval = Duration.between(
					this.lastReceivedPacketTime, currentInstant);
			if (!this.hasSentTestCommand
					&& idleInterval.getSeconds() > this.timeoutT3) {

				sendTest();
			} else if (this.hasSentTestCommand) {

				if (this.lastTestTime != null) {
					Duration testTimeoutDuration = Duration.between(
							this.lastTestTime, currentInstant);
					if (testTimeoutDuration.getSeconds() > this.timeoutT1) {
						if (this.testCount < 3) {
							this.testCount = (byte) (this.testCount + 1);
							sendTest();
							this.lastTestTime = currentInstant;
						} else {

							LogTools.log(logger, this.sn, LogType.DEBUG,
									"Base 101 device sn = " + this.sn
											+ " test timeout.");
							setProtocolStatusOffLine();
						}
					}
				}
			} else {
				businessFlow(currentInstant);
			}
		}
	}

	public void parseProtocolVariable() {
		try {
			JSONObject protocolVariableJSON = new JSONObject(
					this.protocolParams);

			if (protocolVariableJSON.has("timeout_t1")) {
				this.timeoutT1 = protocolVariableJSON.getInt("timeout_t1");
			}

			if (protocolVariableJSON.has("timeout_t2")) {
				this.timeoutT2 = protocolVariableJSON.getInt("timeout_t2");
			}

			if (protocolVariableJSON.has("timeout_t3")) {
				this.timeoutT3 = protocolVariableJSON.getInt("timeout_t3");
			}

			if (protocolVariableJSON.has("zz_time_interval")) {
				this.zzTimeInterval = protocolVariableJSON
						.getInt("zz_time_interval");
			}

			if (protocolVariableJSON.has("use_js")) {
				this.useJS = (protocolVariableJSON.getInt("use_js") == 1);
			}

			if (protocolVariableJSON.has("js_time_interval")) {
				this.jsTimeInterval = protocolVariableJSON
						.getInt("js_time_interval");
			}
		} catch (JSONException e) {
			LogTools.log(logger, this.sn, LogType.WARN, "Base 101 device sn = "
					+ this.sn
					+ " exception occured when parse protocol variables.", e);
		}
	}

	public void toggleFlag(FrameType frameType) {
		Instant currentInstant = Instant.now();
		if (frameType.value().equals(FrameType.IEC101_CONNECT_FRAME.value())) {
			this.lastConnectTime = currentInstant;
		} else if (frameType.value()
				.equals(FrameType.IEC101_TEST_FRAME.value())) {
			this.lastTestTime = currentInstant;
		} else if (frameType.value().equals(
				FrameType.IEC101_RESET_FRAME.value())) {
			this.lastResetTime = currentInstant;
		} else if (frameType.value().equals(
				FrameType.IEC101_RECRUITMENT_FRAME.value())) {
			this.lastZzTime = currentInstant;
		}
	}

	public void initDataType() {
	}

	public Pair<Long, Byte> getHistorySectionByte(String sectionName, int pos) {
		return null;
	}

	public Pair<Long, byte[]> getHistorySectionBytes(String sectionName,
			int pos, int length) {
		return null;
	}

	public byte[] getSectionBytes(String sectionName, int pos, int length) {
		if (!this.dataSectionNames.contains(sectionName)) {
			return null;
		}

		if (((Integer) ((Quartet) this.dataSectionMap.get(sectionName))
				.getValue1()).intValue() < pos + length) {
			return null;
		}

		String key = (String) this.dataSectionNameTypeMap.get(sectionName)
				+ pos;
		if (!this.updatedSectionPos.containsKey(key)
				|| !((Boolean) this.updatedSectionPos.get(key)).booleanValue()) {
			return null;
		}

		byte[] bytes = new byte[length];
		System.arraycopy(
				((Quartet) this.dataSectionMap.get(sectionName)).getValue3(),
				pos, bytes, 0, length);
		this.updatedSectionPos.put(key, Boolean.valueOf(false));
		return bytes;
	}

	public byte[] getSectionBytesForDebug(String sectionName, int pos,
			int length) {
		if (!this.dataSectionNames.contains(sectionName)) {
			return null;
		}

		if (((Integer) ((Quartet) this.dataSectionMap.get(sectionName))
				.getValue1()).intValue() < pos + length) {
			return null;
		}

		byte[] bytes = new byte[length];
		System.arraycopy(
				((Quartet) this.dataSectionMap.get(sectionName)).getValue3(),
				pos, bytes, 0, length);
		return bytes;
	}

	public Byte getSectionByteForDebug(String sectionName, int pos) {
		if (!this.dataSectionNames.contains(sectionName)) {
			return null;
		}

		if (((Integer) ((Quartet) this.dataSectionMap.get(sectionName))
				.getValue1()).intValue() < pos) {
			return null;
		}

		// return
		// Byte.valueOf((byte[])((Quartet)this.dataSectionMap.get(sectionName)).getValue3()[pos]);
		return Byte
				.valueOf(this.dataSectionMap.get(sectionName).getValue3()[pos]);
	}

	public Byte getSectionByte(String sectionName, int pos) {
		if (!this.dataSectionNames.contains(sectionName)) {
			return null;
		}

		if (((Integer) ((Quartet) this.dataSectionMap.get(sectionName))
				.getValue1()).intValue() < pos) {
			return null;
		}

		String key = (String) this.dataSectionNameTypeMap.get(sectionName)
				+ pos;
		if (!this.updatedSectionPos.containsKey(key)
				|| !((Boolean) this.updatedSectionPos.get(key)).booleanValue()) {
			return null;
		}

		this.updatedSectionPos.put(key, Boolean.valueOf(false));
		/*
		 * return
		 * Byte.valueOf((byte[])((Quartet)this.dataSectionMap.get(sectionName
		 * )).getValue3()[pos]);
		 */
		return Byte
				.valueOf(this.dataSectionMap.get(sectionName).getValue3()[pos]);
	}

	public void delete() {
		LogTools.log(logger, this.sn, LogType.DEBUG, "Base 101 device sn = "
				+ this.sn + " sub delete is called.");
		super.delete();
	}

	public void onConnected() {
		resetProtocolStatus();
	}

	public void onDisconnected() {
		this.protocolStatus = StatusType.IDLE;
	}

	private void resetProtocolStatus() {
		Instant currentInstant = Instant.now();

		this.hasSentConnectCommand = false;
		this.hasSentResetCommand = false;
		this.hasSentTestCommand = false;
		this.hasSentRecruitmentCommand = false;
		this.hasSentYKSelectCommand = false;
		this.hasSentYKExecuteCommand = false;
		this.protocolStatus = StatusType.IDLE;
		this.requireIFrameAck = false;
		this.testCount = 0;
		this.lastZzTime = currentInstant.minusSeconds(2000L);
		this.lastResetTime = currentInstant.minusSeconds(2000L);
		this.lastConnectTime = currentInstant.minusSeconds(2000L);
		this.lastCorrectTimeTime = currentInstant.minusSeconds(2000L);
	}

	private void businessFlow(Instant currentInstant) {
		if (this.requireIFrameAck) {
			if (isWaitIFrameAckFrameTimeOut()) {
				resetProtocolStatus();
			}
			return;
		}
		Duration zzInterval = Duration.between(this.lastZzTime, currentInstant);
		Duration correctTimeInterval = Duration.between(
				this.lastCorrectTimeTime, currentInstant);

		if (this.useJS
				&& correctTimeInterval.getSeconds() > this.jsTimeInterval
				&& !this.hasSentCorrectTimeCommand && !this.hasSentTestCommand) {

			sendCorrectTimeCommand();
			this.lastCorrectTimeTime = currentInstant;
			this.hasSentCorrectTimeCommand = true;
			startWaitIFrameAckFrame();
		} else if (!this.hasSentYKSelectCommand
				&& !this.hasSentYKExecuteCommand
				&& !this.ykCommandList.isEmpty() && !this.hasSentTestCommand) {
			Pair<String, String> ykPair = (Pair) this.ykCommandList.get(0);
			String deviceVariableSn = (String) ykPair.getValue0();
			String deviceVariableValue = (String) ykPair.getValue1();

			try {
				this.ykAddress = GlobalVariables
						.getDeviceVariableBySn(deviceVariableSn)
						.getRegisterTypeIndex().intValue();
			} catch (Exception e) {
				LogTools.log(logger, this.sn, LogType.DEBUG,
						"Base 101 device sn = " + this.sn
								+ " get yk variable address error.");
			}

			this.ykValue = Byte.valueOf(deviceVariableValue).byteValue();
			if (this.ykValue != 0 && this.ykValue != 1) {
				LogTools.log(logger, this.sn, LogType.WARN,
						"Base 101 device sn = " + this.sn
								+ " invalid yaokong value = " + this.ykValue);
				this.ykCommandList.remove(0);

				return;
			}
			sendYKSelect();
			this.ykCommandList.remove(0);
			this.hasSentYKSelectCommand = true;
			startWaitIFrameAckFrame();
		} else if (zzInterval.getSeconds() > this.zzTimeInterval
				&& !this.hasSentRecruitmentCommand && !this.hasSentTestCommand) {

			sendRecruitmentCommand();
			this.lastZzTime = currentInstant;
			this.hasSentRecruitmentCommand = true;
			startWaitIFrameAckFrame();
		}
	}

	private void setProtocolStatusOffLine() {
		this.protocolStatus = StatusType.IDLE;
	}

	public void handleValueChangedMessage(String deviceVariableSn,
			String deviceVariableValue, String type) {
		try {
			Pair<String, String> ykPair;
			switch (type) {
			case "yaokong-act":
				ykPair = new Pair<String, String>(deviceVariableSn,
						deviceVariableValue);
				this.ykCommandList.add(ykPair);

			case "shedian":
				return;
			}
			LogTools.log(logger, this.sn, LogType.ERROR,
					"Base 101 device sn = " + this.sn + " invalid type : "
							+ type);
		} catch (NumberFormatException e) {
			LogTools.log(logger, this.sn, LogType.ERROR,
					"Base 101 device sn = " + this.sn
							+ " failed to convert yao kong value to byte : "
							+ deviceVariableValue);
		}
	}

	private void sendConnectCommand() {
		byte[] command = { 16, 73,
				(byte) (this.deviceAddressInt.intValue() % 256),
				(byte) (this.deviceAddressInt.intValue() / 256), 0, 22 };

		byte[] frameBytes = new byte[3];
		System.arraycopy(command, 1, frameBytes, 0, 3);
		int crc = Caculator.caculateIEC101CRC(frameBytes);

		command[4] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_CONNECT_FRAME,
				command));
		this.hasSentConnectCommand = true;
		this.hasDataToSend = true;
	}

	private void sendResetCommand() {
		byte[] command = { 16, 64,
				(byte) (this.deviceAddressInt.intValue() % 256),
				(byte) (this.deviceAddressInt.intValue() / 256), 0, 22 };

		byte[] frameBytes = new byte[3];
		System.arraycopy(command, 1, frameBytes, 0, 3);
		int crc = Caculator.caculateIEC101CRC(frameBytes);

		command[4] = (byte) (crc & 0xFF);
		this.outBytesBuffer
				.add(new Pair(FrameType.IEC101_RESET_FRAME, command));
		this.hasSentResetCommand = true;
		this.hasDataToSend = true;
	}

	private void sendCorrectTimeCommand() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 18));
		commandBufs.add(Byte.valueOf((byte) 18));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 115));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 103));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) 6));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));

		LocalDateTime now = LocalDateTime.now();
		commandBufs.add(Byte.valueOf((byte) (now.getSecond() * 1000 & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (now.getSecond() * 1000 >> 8)));
		commandBufs.add(Byte.valueOf((byte) (now.getMinute() & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (now.getHour() & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (now.getDayOfMonth() & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (now.getMonthValue() & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (now.getYear() % 100 & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		byte[] toBeCaculated = new byte[18];
		for (int i = 0; i < 18; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}

		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[22] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_CORRECT_TIME_FRAME,
				commandBytes));
		this.hasDataToSend = true;
	}

	private void handleConnectAckFrame() {
		if (this.hasSentConnectCommand && this.deviceAddressInt != null) {
			this.hasSentConnectCommand = false;
			this.protocolStatus = StatusType.CONNECTED;
		}
	}

	private void handleResecAckFrame() {
		if (this.hasSentResetCommand) {
			this.protocol.resetToBeParsedBuffer();
			this.protocolStatus = StatusType.ONLINE;
		}
	}

	private void handleRecruitmentAckFrame() {
	}

	private void handleRecruitmentCompleteFrame() {
		if (this.hasSentRecruitmentCommand) {
			this.hasSentRecruitmentCommand = false;
			this.requireIFrameAck = false;
		}
	}

	private void sendTest() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 13));
		commandBufs.add(Byte.valueOf((byte) 13));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 114));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) -122));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) -86));
		commandBufs.add(Byte.valueOf((byte) 85));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		byte[] toBeCaculated = new byte[13];
		for (int i = 0; i < 13; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}
		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[17] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_TEST_FRAME,
				commandBytes));
		this.hasSentTestCommand = true;
		this.hasDataToSend = true;
	}

	private void sendRecruitmentCommand() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 83));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 100));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) 6));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 20));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		commandBytes[1] = 12;
		commandBytes[2] = 12;

		byte[] toBeCaculated = new byte[12];
		for (int i = 0; i < 12; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}
		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[16] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_RECRUITMENT_FRAME,
				commandBytes));
		this.hasDataToSend = true;
	}

	private void sendYXAck() {
		byte[] command = { 16, 0,
				(byte) (this.deviceAddressInt.intValue() % 256),
				(byte) (this.deviceAddressInt.intValue() / 256), 0, 22 };

		byte[] frameBytes = new byte[3];
		System.arraycopy(command, 1, frameBytes, 0, 3);
		int crc = Caculator.caculateIEC101CRC(frameBytes);

		command[4] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_CONNECT_FRAME,
				command));
		this.hasDataToSend = true;
	}

	void handleYXFrame(Map<String, Object> parsedValueMap) {
		Boolean needReply = (Boolean) parsedValueMap.get("need_reply");
		if (needReply != null && needReply.booleanValue() == true) {
			sendYXAck();
		}

		commonHandle1ByteData(parsedValueMap, "YX");
	}

	void handleSOEFrame(Map<String, Object> parsedValueMap) {
		sendYXAck();
		commonHandle1ByteData(parsedValueMap, "YX");
	}

	void handleYCFrame(Map<String, Object> parsedValueMap) {
		commonHandle4BytesData(parsedValueMap, "YC");
	}

	private void commonHandle4BytesData(Map<String, Object> parsedValueMap,
			String sectionName) {
		Map<Integer, byte[]> valueMap = (Map) parsedValueMap.get("valueMap");
		Map<Integer, Long> timeValueMap = (Map) parsedValueMap
				.get("timeValueMap");
		if (valueMap == null || valueMap.size() == 0) {
			LogTools.log(logger, this.sn, LogType.WARN, "Base 101 device sn = "
					+ this.sn + " can't get any " + sectionName.toLowerCase()
					+ " value");

			return;
		}
		for (Integer key : valueMap.keySet()) {
			for (Quartet<Integer, Integer, String, byte[]> dataSection : this.dataSectionMap
					.values()) {
				if (!((String) dataSection.getValue2()).equals(sectionName)) {
					continue;
				}

				int start = ((Integer) dataSection.getValue0()).intValue();
				int length = ((Integer) dataSection.getValue1()).intValue();

				if (key.intValue() * 4 >= start
						&& key.intValue() * 4 + 3 < start + length) {
					String sectionPos = (String) dataSection.getValue2()
							+ (key.intValue() * 4 - start);
					if (timeValueMap == null || timeValueMap.get(key) == null) {
						byte[] value = (byte[]) valueMap.get(key);
						System.arraycopy(value, 0, dataSection.getValue3(),
								key.intValue() * 4 - start, 4);

						this.updatedSectionPos.put(sectionPos,
								Boolean.valueOf(true));
						continue;
					}
					Long updateTimeLong = (Long) timeValueMap.get(key);
					if (this.historyValueMap.get(sectionPos) == null) {
						byte[] value = (byte[]) valueMap.get(key);

						LinkedBlockingQueue<Pair<Long, byte[]>> bq = new LinkedBlockingQueue<Pair<Long, byte[]>>();
						bq.add(new Pair(updateTimeLong, value));
						this.historyValueMap.put(sectionPos, bq);
						continue;
					}
					byte[] value = (byte[]) valueMap.get(key);
					((LinkedBlockingQueue) this.historyValueMap.get(sectionPos))
							.add(new Pair(updateTimeLong, value));
				}
			}
		}
	}

	private void commonHandle1ByteData(Map<String, Object> parsedValueMap,
			String sectionName) {
		Map<Integer, Integer> valueMap = (Map) parsedValueMap.get("valueMap");
		Map<Integer, Long> timeValueMap = (Map) parsedValueMap
				.get("timeValueMap");
		if (valueMap == null || valueMap.size() == 0) {
			LogTools.log(logger, this.sn, LogType.WARN, "Base 101 device sn = "
					+ this.sn + " can't get any " + sectionName.toLowerCase()
					+ " value.");

			return;
		}
		for (Integer key : valueMap.keySet()) {
			for (Quartet<Integer, Integer, String, byte[]> dataSection : this.dataSectionMap
					.values()) {
				if (!((String) dataSection.getValue2()).equals(sectionName)) {
					continue;
				}

				int start = ((Integer) dataSection.getValue0()).intValue();
				int length = ((Integer) dataSection.getValue1()).intValue();

				if (key.intValue() >= start && key.intValue() < start + length) {
					String sectionPos = (String) dataSection.getValue2()
							+ (key.intValue() - start);
					if (timeValueMap == null || timeValueMap.get(key) == null) {

						dataSection.getValue3()[key.intValue() - start] = (byte) (((Integer) valueMap
								.get(key)).intValue() & 0xFF);
						this.updatedSectionPos.put(sectionPos,
								Boolean.valueOf(true));
						continue;
					}
					if (this.historyValueMap.get(sectionPos) == null) {
						byte[] bytes = new byte[1];
						bytes[0] = (byte) (((Integer) valueMap.get(key))
								.intValue() & 0xFF);
						Long updateTimeLong = (Long) timeValueMap.get(key);

						LinkedBlockingQueue<Pair<Long, byte[]>> bq = new LinkedBlockingQueue<Pair<Long, byte[]>>();
						bq.add(new Pair(updateTimeLong, bytes));
						this.historyValueMap.put(sectionPos, bq);
						continue;
					}
					byte[] bytes = new byte[1];
					bytes[0] = (byte) (((Integer) valueMap.get(key)).intValue() & 0xFF);
					Long updateTimeLong = (Long) timeValueMap.get(key);
					((LinkedBlockingQueue) this.historyValueMap.get(sectionPos))
							.add(new Pair(updateTimeLong, bytes));
				}
			}
		}
	}

	private void handleTestAckFrame() {
		if (this.hasSentTestCommand) {

			this.hasSentTestCommand = false;
			this.testCount = 0;
		}
	}

	private void handleTestFrame() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 13));
		commandBufs.add(Byte.valueOf((byte) 13));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) -12));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) 7));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) -86));
		commandBufs.add(Byte.valueOf((byte) 85));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		byte[] toBeCaculated = new byte[13];
		for (int i = 0; i < 13; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}
		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[16] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_TEST_ACK_FRAME,
				commandBytes));
		this.hasSentTestCommand = true;
		this.hasDataToSend = true;
	}

	private void handleCorrectTimeAckFrame() {
		if (this.hasSentCorrectTimeCommand) {
			this.hasSentCorrectTimeCommand = false;
			this.requireIFrameAck = false;
		}
	}

	private void sendYKSelect() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 83));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 46));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) 6));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));

		int address = 24577 + this.ykAddress;
		commandBufs.add(Byte.valueOf((byte) (address & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (address >> 8 & 0xFF)));

		commandBufs.add(Byte.valueOf((byte) ((this.ykValue == 1) ? 130 : 129)));

		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		commandBytes[1] = 12;
		commandBytes[2] = 12;

		byte[] toBeCaculated = new byte[12];
		for (int i = 0; i < 12; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}
		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[16] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_RECRUITMENT_FRAME,
				commandBytes));
		this.hasDataToSend = true;
	}

	private void handleYKSelectAckFrame(Map<String, Object> parsedValueMap) {
		this.hasSentYKSelectCommand = false;
		if (((Boolean) parsedValueMap.get("idle")).booleanValue()) {

			sendYKExecute();
			this.hasSentYKExecuteCommand = true;
			startWaitIFrameAckFrame();
		} else {
			logger.warn("YK Select Reject");
			this.requireIFrameAck = false;
		}
	}

	private void sendYKExecute() {
		List<Byte> commandBufs = new ArrayList<Byte>();
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 104));
		commandBufs.add(Byte.valueOf((byte) 83));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));
		commandBufs.add(Byte.valueOf((byte) 46));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs.add(Byte.valueOf((byte) 6));
		commandBufs.add(Byte.valueOf((byte) 1));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() % 256)));
		commandBufs
				.add(Byte.valueOf((byte) (this.deviceAddressInt.intValue() / 256)));

		int address = 24577 + this.ykAddress;
		commandBufs.add(Byte.valueOf((byte) (address & 0xFF)));
		commandBufs.add(Byte.valueOf((byte) (address >> 8 & 0xFF)));

		commandBufs.add(Byte.valueOf((byte) ((this.ykValue == 1) ? 2 : 1)));

		commandBufs.add(Byte.valueOf((byte) 0));
		commandBufs.add(Byte.valueOf((byte) 22));

		byte[] commandBytes = new byte[commandBufs.size()];
		for (int i = 0; i < commandBufs.size(); i++) {
			commandBytes[i] = ((Byte) commandBufs.get(i)).byteValue();
		}

		commandBytes[1] = 12;
		commandBytes[2] = 12;

		byte[] toBeCaculated = new byte[12];
		for (int i = 0; i < 12; i++) {
			toBeCaculated[i] = ((Byte) commandBufs.get(i + 4)).byteValue();
		}
		int crc = Caculator.caculateIEC101CRC(toBeCaculated);
		commandBytes[16] = (byte) (crc & 0xFF);
		this.outBytesBuffer.add(new Pair(FrameType.IEC101_RECRUITMENT_FRAME,
				commandBytes));
		this.hasDataToSend = true;
	}

	private void handleYKExecuteAckFrame(Map<String, Object> parsedValueMap) {
		this.hasSentYKExecuteCommand = false;
		this.requireIFrameAck = false;
	}

	private void startWaitIFrameAckFrame() {
		this.requireIFrameAck = true;
		this.lastIFrameTime = Instant.now();
	}

	private boolean isWaitIFrameAckFrameTimeOut() {
		return (this.requireIFrameAck && Duration.between(this.lastIFrameTime,
				Instant.now()).getSeconds() > this.timeoutT2);
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\
 * BOOT-INF\classes\com\kkwl\collector\devices\business\BaseIEC101Device.class
 * Java compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */