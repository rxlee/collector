package com.kkwl.collector.protocols.IEC104;

import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.protocols.BaseFrameParser;
import com.kkwl.collector.utils.Caculator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DDFrameParser extends BaseFrameParser {
	public DDFrameParser() {
		super(FrameType.IEC104_DD_I_FRAME.value());
	}

	public Map<String, Object> parseBytesFrame(byte[] frameBytes,
			Map<String, Object> params) {
		Map<String, Object> retMap = new HashMap<String, Object>();

		int frameLen = frameBytes[1] & 0xFF;
		if (frameLen > 6
				&& (frameBytes[6] == 15 || frameBytes[6] == -50 || frameBytes[6] == 37)) {
			int csyyLength = ((Integer) params.get("csyyLength")).intValue();
			int ggdzLength = ((Integer) params.get("ggdzLength")).intValue();
			int xxtLength = ((Integer) params.get("xxtLength")).intValue();
			Map<Integer, byte[]> valueMap = new HashMap<Integer, byte[]>();
			Map<Integer, Long> timeValueMap = new HashMap<Integer, Long>();

			boolean isContinuity = false;
			if ((frameBytes[7] & 0x80) == 128) {
				isContinuity = true;
			}

			int num = frameBytes[7] & 0x7F;
			int currentAddress = 8 + csyyLength + ggdzLength;

			if (isContinuity) {
				int targetAddress = (frameBytes[currentAddress] & 0xFF)
						+ ((frameBytes[currentAddress + 1] & 0xFF) << 8);
				int pos = 0;
				if (targetAddress > 25600) {
					pos = targetAddress - 25601;
				} else {
					pos = targetAddress - 3073;
				}
				currentAddress += xxtLength;
				for (int i = 0; i < num; i++) {
					byte[] cp256Time2aBytes;
					LocalDateTime updateTime;
					byte[] value;
//					byte[] value;
//					byte[] value;
					switch (frameBytes[6]) {

					case 15:
						if (currentAddress + 5 > frameBytes.length) {
							retMap.put("result", Boolean.valueOf(false));
							return retMap;
						}

						value = new byte[4];
						value[0] = frameBytes[currentAddress];
						value[1] = frameBytes[currentAddress + 1];
						value[2] = frameBytes[currentAddress + 2];
						value[3] = frameBytes[currentAddress + 3];

						valueMap.put(Integer.valueOf(pos + i), value);
						currentAddress += 5;
						break;
					case -49:
						value = new byte[4];
						value[0] = frameBytes[currentAddress];
						value[1] = frameBytes[currentAddress + 1];
						value[2] = frameBytes[currentAddress + 2];
						value[3] = frameBytes[currentAddress + 3];
						valueMap.put(Integer.valueOf(pos + i), value);

						updateTime = null;
						cp256Time2aBytes = new byte[7];
						System.arraycopy(frameBytes, currentAddress + 6,
								cp256Time2aBytes, 0, 7);
						updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
						if (updateTime == null) {
							retMap.put("result", Boolean.valueOf(false));
							return retMap;
						}

						timeValueMap
								.put(Integer.valueOf(pos + i),
										Long.valueOf(updateTime
												.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID)
												.toInstant().toEpochMilli()));
						currentAddress += 12;
						break;
					case -50:
						if (currentAddress + 4 > frameBytes.length) {
							retMap.put("result", Boolean.valueOf(false));
							return retMap;
						}

						value = new byte[4];
						value[0] = frameBytes[currentAddress];
						value[1] = frameBytes[currentAddress + 1];
						value[2] = frameBytes[currentAddress + 2];
						value[3] = frameBytes[currentAddress + 3];

						valueMap.put(Integer.valueOf(pos + i), value);
						currentAddress += 5;
						break;
					}
				}
			} else {
				if (xxtLength == 3) {
					int infoLen = 0;
					switch (frameBytes[6]) {
					case 15:
						infoLen = 8;
						break;
					case -49:
						infoLen = 15;
						break;
					case -50:
						infoLen = 8;
						break;
					}

					if (currentAddress + 2 + (num - 1) * infoLen > frameBytes.length) {
						retMap.put("result", Boolean.valueOf(false));
						return retMap;
					}

					for (int i = 0; i < num; i++) {
						if (frameBytes[currentAddress + 2 + i * infoLen] != 0) {
							retMap.put("result", Boolean.valueOf(false));
							return retMap;
						}
					}
				}

				for (int i = 0; i < num; i++) {
					byte[] cp256Time2aBytes;
					LocalDateTime updateTime;
					byte[] tmpBuffer/*, tmpBuffer, tmpBuffer*/;
					int targetAddress = (frameBytes[currentAddress] & 0xFF)
							+ ((frameBytes[currentAddress + 1] & 0xFF) << 8);
					currentAddress += xxtLength;
					int pos = 0;
					if (targetAddress > 25600) {
						pos = targetAddress - 25601;
					} else {
						pos = targetAddress - 3073;
					}

					switch (frameBytes[6]) {
					case 15:
						tmpBuffer = new byte[4];
						tmpBuffer[0] = frameBytes[currentAddress];
						tmpBuffer[1] = frameBytes[currentAddress + 1];
						tmpBuffer[2] = frameBytes[currentAddress + 2];
						tmpBuffer[3] = frameBytes[currentAddress + 3];

						valueMap.put(Integer.valueOf(pos), tmpBuffer);
						currentAddress += 5;
						break;
					case -49:
						tmpBuffer = new byte[4];
						tmpBuffer[0] = frameBytes[currentAddress];
						tmpBuffer[1] = frameBytes[currentAddress + 1];
						tmpBuffer[2] = frameBytes[currentAddress + 2];
						tmpBuffer[3] = frameBytes[currentAddress + 3];
						valueMap.put(Integer.valueOf(pos), tmpBuffer);

						updateTime = null;
						cp256Time2aBytes = new byte[7];
						System.arraycopy(frameBytes, currentAddress + 5,
								cp256Time2aBytes, 0, 7);
						updateTime = Caculator.getCP56Time2a(cp256Time2aBytes);
						if (updateTime == null) {
							retMap.put("result", Boolean.valueOf(false));
							return retMap;
						}

						timeValueMap
								.put(Integer.valueOf(pos),
										Long.valueOf(updateTime
												.atZone(GlobalVariables.GLOBAL_DEFAULT_ZONEID)
												.toInstant().toEpochMilli()));
						currentAddress += 12;
						break;
					case -50:
						tmpBuffer = new byte[4];
						tmpBuffer[0] = frameBytes[currentAddress];
						tmpBuffer[1] = frameBytes[currentAddress + 1];
						tmpBuffer[2] = frameBytes[currentAddress + 2];
						tmpBuffer[3] = frameBytes[currentAddress + 3];

						valueMap.put(Integer.valueOf(pos), tmpBuffer);
						currentAddress += 5;
						break;
					}

				}
			}
			int i = frameBytes[2] & 0xFF;
			int j = frameBytes[3] & 0xFF;
			int k = (i + j * 256) / 2;
			k++;

			i = frameBytes[4] & 0xFF;
			j = frameBytes[5] & 0xFF;
			int l = (i + j * 256) / 2;

			retMap.put("result", Boolean.valueOf(true));
			retMap.put("type", FrameType.IEC104_DD_I_FRAME);
			retMap.put("valueMap", valueMap);
			if (!timeValueMap.isEmpty()) {
				retMap.put("timeValueMap", timeValueMap);
			}
			retMap.put("recvNo", Integer.valueOf(k));
			retMap.put("sendNo", Integer.valueOf(l));
		} else {
			retMap.put("result", Boolean.valueOf(false));
		}

		return retMap;
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\
 * BOOT-INF\classes\com\kkwl\collector\protocols\IEC104\DDFrameParser.class Java
 * compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */