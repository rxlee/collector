package com.kkwl.collector.utils;

import com.kkwl.collector.common.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumberUtil {
	private static final Logger logger = LoggerFactory
			.getLogger(NumberUtil.class);

	public static byte[] intToByte4(int i) {
		byte[] targets = new byte[4];
		targets[3] = (byte) (i & 0xFF);
		targets[2] = (byte) (i >> 8 & 0xFF);
		targets[1] = (byte) (i >> 16 & 0xFF);
		targets[0] = (byte) (i >> 24 & 0xFF);
		return targets;
	}

	public static byte[] longToByte8(long lo) {
		byte[] byteNum = new byte[8];
		for (int ix = 0; ix < 8; ix++) {
			int offset = 64 - (ix + 1) * 8;
			byteNum[ix] = (byte) (int) (lo >> offset & 0xFFL);
		}
		return byteNum;
	}

	public static long bytesToLong(byte[] value) {
		long num = 0L;
		for (int ix = 0; ix < 8; ix++) {
			num <<= 8;
			num |= (value[ix] & 0xFF);
		}
		return num;
	}

	public static byte[] unsignedShortToByte2(int s) {
		byte[] targets = new byte[2];
		targets[0] = (byte) (s >> 8 & 0xFF);
		targets[1] = (byte) (s & 0xFF);
		return targets;
	}

	public static int byte2ToUnsignedShort(byte[] bytes) {
		return byte2ToUnsignedShort(bytes, 0);
	}

	public static int byte2ToUnsignedShort(byte[] bytes, int off) {
		int high = bytes[off];
		int low = bytes[off + 1];
		return high << 8 & 0xFF00 | low & 0xFF;
	}

	public static int byte2ToSignalShort(byte[] bytes) {
		int high = bytes[0];
		int low = bytes[1];
		int value = high << 8 & 0x7F00 | low & 0xFF;
		if ((high & 0x80) == 128) {
			return -value;
		}
		return value;
	}

	public static int byte2SignedShort(byte[] bytes) {
		short high = (short) bytes[0];
		short low = (short) bytes[1];

		high = (short) (high << 8);
		return (short) (high & 0xFF00 | low & 0xFF);
	}

	public static int byte4ToInt(byte[] bytes) {
		int b0 = bytes[0] & 0xFF;
		int b1 = bytes[1] & 0xFF;
		int b2 = bytes[2] & 0xFF;
		int b3 = bytes[3] & 0xFF;
		return b0 << 24 & 0xFF000000 | b1 << 16 & 0xFF0000 | b2 << 8 & 0xFF00
				| b3 & 0xFF;
	}

	public static long byte4ToUnsignedInt(byte[] bytes) {
		long b0 = (bytes[0] & 0xFF);
		long b1 = (bytes[1] & 0xFF);
		long b2 = (bytes[2] & 0xFF);
		long b3 = (bytes[3] & 0xFF);
		return b0 << 24 & 0xFFFFFFFFFF000000L | b1 << 16 & 0xFF0000L | b2 << 8
				& 0xFF00L | b3 & 0xFFL;
	}

	public static long byte4ToSingalInt(byte[] bytes) {
		long b0 = (bytes[0] & 0xFF);
		long b1 = (bytes[1] & 0xFF);
		long b2 = (bytes[2] & 0xFF);
		long b3 = (bytes[3] & 0xFF);
		long value = b0 << 24 & 0x7F000000L | b1 << 16 & 0xFF0000L | b2 << 8
				& 0xFF00L | b3 & 0xFFL;

		if ((b0 & 0x80L) == 128L) {
			return -value;
		}
		return value;
	}

	public static float getFloatValue(byte[] bytes, DataType dataType) {
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_2143
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_2143 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];
			tmpBytes[2] = bytes[3];
			tmpBytes[3] = bytes[2];
			return Float.intBitsToFloat(bytes2int(tmpBytes));
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_4321
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_4321 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[3];
			tmpBytes[1] = bytes[2];
			tmpBytes[2] = bytes[1];
			tmpBytes[3] = bytes[0];
			return Float.intBitsToFloat(bytes2int(tmpBytes));
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_1234
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_1234 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[0];
			tmpBytes[1] = bytes[1];
			tmpBytes[2] = bytes[2];
			tmpBytes[3] = bytes[3];
			return Float.intBitsToFloat(bytes2int(tmpBytes));
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_3412
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_3412 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[2];
			tmpBytes[1] = bytes[3];
			tmpBytes[2] = bytes[0];
			tmpBytes[3] = bytes[1];
			return Float.intBitsToFloat(bytes2int(tmpBytes));
		}
		if (dataType.valueInteger() == DataType.BYTES_WORD_2.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_WORD_2 received bytes length < 2, return 0");
				return 0.0F;
			}

			return byte2ToUnsignedShort(bytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_SHORT_2_SIGNAL
				.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_WORD_2 received bytes length < 2, return 0");
				return 0.0F;
			}

			return byte2ToSignalShort(bytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_2_HL.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_INT_2_HL received bytes length < 2, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[2];
			tmpBytes[0] = bytes[0];
			tmpBytes[1] = bytes[1];

			return byte2SignedShort(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_2_LH.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_INT_2_LH received bytes length < 2, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[2];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];

			return byte2SignedShort(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_DWORD_4.valueInteger()
				|| dataType.valueInteger() == DataType.BYTES_DWORD_4_1234
						.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_DWORD_4 | BYTES_DWORD_4_1234 received bytes length < 4, return 0");
				return 0.0F;
			}

			return (float) byte4ToUnsignedInt(bytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_DWORD_4_2143
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_DWORD_4_2143 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];
			tmpBytes[2] = bytes[3];
			tmpBytes[3] = bytes[2];

			return (float) byte4ToUnsignedInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_DWORD_4_4321
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_DWORD_4_4321 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[3];
			tmpBytes[1] = bytes[2];
			tmpBytes[2] = bytes[1];
			tmpBytes[3] = bytes[0];
			return (float) byte4ToUnsignedInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_DWORD_4_3412
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_DWORD_4_3412 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[2];
			tmpBytes[1] = bytes[3];
			tmpBytes[2] = bytes[0];
			tmpBytes[3] = bytes[1];
			return (float) byte4ToUnsignedInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4.valueInteger()
				|| dataType.valueInteger() == DataType.BYTES_INT_4_1234
						.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4 | BYTES_INT_4_1234 received bytes length < 4, return 0");
				return 0.0F;
			}

			return byte4ToInt(bytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_2143.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_2143 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];
			tmpBytes[2] = bytes[3];
			tmpBytes[3] = bytes[2];

			return byte4ToInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_4321.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_4321 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[3];
			tmpBytes[1] = bytes[2];
			tmpBytes[2] = bytes[1];
			tmpBytes[3] = bytes[0];
			return byte4ToInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_3412.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_3412 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[2];
			tmpBytes[1] = bytes[3];
			tmpBytes[2] = bytes[0];
			tmpBytes[3] = bytes[1];
			return byte4ToInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL
				.valueInteger()
				|| dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL_1234
						.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_SIGNAL | BYTES_INT_4_SIGNAL_1234 received bytes length < 4, return 0");
				return 0.0F;
			}

			return (float) byte4ToSingalInt(bytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL_2143
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_SIGNAL_2143 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];
			tmpBytes[2] = bytes[3];
			tmpBytes[3] = bytes[2];

			return (float) byte4ToSingalInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL_4321
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_SIGNAL_4321 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[3];
			tmpBytes[1] = bytes[2];
			tmpBytes[2] = bytes[1];
			tmpBytes[3] = bytes[0];
			return (float) byte4ToSingalInt(tmpBytes);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL_3412
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4_SIGNAL_3412 received bytes length < 4, return 0");
				return 0.0F;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[2];
			tmpBytes[1] = bytes[3];
			tmpBytes[2] = bytes[0];
			tmpBytes[3] = bytes[1];
			return (float) byte4ToSingalInt(tmpBytes);
		}
		logger.warn("NumberUtil can't find any match data type with type = "
				+ dataType.valueString() + ", return 0");
		return 0.0F;
	}

	public static int getIntegerValue(byte b, int pos) {
		/* return b >> pos & true; */
		return b >> pos;
	}

	public static byte[] getDigitalBytes(byte b, int pos) {
		byte[] bs = new byte[1];
		// bs[0] = (byte)(b >> pos & true);
		bs[0] = (byte) (b >> pos);
		return bs;
	}

	public static byte[] getAnalogBytes(byte[] bytes, DataType dataType) {
		byte[] zeroBytes = { 0, 0, 0, 0 };
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_2143
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_2143 received bytes length < 4, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];
			tmpBytes[2] = bytes[3];
			tmpBytes[3] = bytes[2];
			return tmpBytes;
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_4321
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_4321 received bytes length < 4, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[3];
			tmpBytes[1] = bytes[2];
			tmpBytes[2] = bytes[1];
			tmpBytes[3] = bytes[0];
			return tmpBytes;
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_1234
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_1234 received bytes length < 4, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[0];
			tmpBytes[1] = bytes[1];
			tmpBytes[2] = bytes[2];
			tmpBytes[3] = bytes[3];
			return tmpBytes;
		}
		if (dataType.valueInteger() == DataType.BYTES_FLOAT_4_3412
				.valueInteger()) {

			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_FLOAT_4_3412 received bytes length < 4, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[4];
			tmpBytes[0] = bytes[2];
			tmpBytes[1] = bytes[3];
			tmpBytes[2] = bytes[0];
			tmpBytes[3] = bytes[1];
			return tmpBytes;
		}
		if (dataType.valueInteger() == DataType.BYTES_WORD_2.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_WORD_2 received bytes length < 2, return 0");
				return zeroBytes;
			}

			float tmpValue = byte2ToUnsignedShort(bytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_SHORT_2_SIGNAL
				.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_WORD_2 received bytes length < 2, return 0");
				return zeroBytes;
			}

			float tmpValue = byte2ToSignalShort(bytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_2_HL.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_INT_2_HL received bytes length < 2, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[2];
			tmpBytes[0] = bytes[0];
			tmpBytes[1] = bytes[1];

			float tmpValue = byte2SignedShort(tmpBytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_2_LH.valueInteger()) {
			if (bytes.length < 2) {
				logger.warn("NumberUtil BYTES_INT_2_LH received bytes length < 2, return 0");
				return zeroBytes;
			}

			byte[] tmpBytes = new byte[2];
			tmpBytes[0] = bytes[1];
			tmpBytes[1] = bytes[0];

			float tmpValue = byte2SignedShort(tmpBytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_DWORD_4.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_DWORD_4 received bytes length < 4, return 0");
				return zeroBytes;
			}

			float tmpValue = (float) byte4ToUnsignedInt(bytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4 received bytes length < 4, return 0");
				return zeroBytes;
			}

			float tmpValue = byte4ToInt(bytes);
			return float2bytes(tmpValue);
		}
		if (dataType.valueInteger() == DataType.BYTES_INT_4_SIGNAL
				.valueInteger()) {
			if (bytes.length < 4) {
				logger.warn("NumberUtil BYTES_INT_4 received bytes length < 4, return 0");
				return zeroBytes;
			}

			float tmpValue = (float) byte4ToSingalInt(bytes);
			return float2bytes(tmpValue);
		}
		logger.warn("NumberUtil can't find any match data type with type = "
				+ dataType.valueString() + ", return 0");
		return zeroBytes;
	}

	public static int bytes2int(byte[] arr) {
		return 0xFF000000 & arr[0] << 24 | 0xFF0000 & arr[1] << 16 | 0xFF00
				& arr[2] << 8 | 0xFF & arr[3];
	}

	private static byte[] float2bytes(float f) {
		int fbit = Float.floatToIntBits(f);

		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (fbit >> 24 - i * 8);
		}

		int len = b.length;

		byte[] dest = new byte[len];

		System.arraycopy(b, 0, dest, 0, len);

		for (int i = 0; i < len / 2; i++) {
			byte temp = dest[i];
			dest[i] = dest[len - i - 1];
			dest[len - i - 1] = temp;
		}

		return dest;
	}
}
