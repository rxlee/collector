 package  com.kkwl.collector.utils;
 
 import com.kkwl.collector.utils.Caculator;
 import java.time.LocalDateTime;
 
 
 
 
 
 
 
 public class Caculator
 {
    private static byte[] lock = new byte[0];
   private static final long w = 100000000L;
   
   public static int caculateCRC16(byte[] frameBytes) {
      int CRC = 65535;
      int POLYNOMIAL = 40961;
     
      for (int i = 0; i < frameBytes.length; i++) {
        CRC ^= frameBytes[i] & 0xFF;
        for (int j = 0; j < 8; j++) {
//          if ((CRC & true) != 0) {
	        if (CRC  != 0) {
	            CRC >>= 1;
	            CRC ^= POLYNOMIAL;
	         } else {
	            CRC >>= 1;
	         } 
       } 
     } 
     
      return CRC;
   }
   
   public static int caculateIEC101CRC(byte[] frameBytes) {
      int CRC = 0;
      for (byte b : frameBytes) {
        CRC += (b & 0xFF);
     }
     
      return CRC & 0xFF;
   }
 
 
 
 
 
   
   public static String caculateUniqueTimeId() {
      long r = 0L;
      synchronized (lock) {
        r = (long)((Math.random() + 1.0D) * 1.0E8D);
     } 
     
      return System.currentTimeMillis() + String.valueOf(r).substring(1);
   }
   
   public static LocalDateTime getCP56Time2a(byte[] bytes) {
      if (bytes.length != 7) {
        return null;
     }
     
      int year = (bytes[6] & 0x7F) + 2000;
      int month = bytes[5] & 0xF;
      int dayOfMonth = bytes[4] & 0x1F;
      int hour = bytes[3] & 0x1F;
      int minute = bytes[2] & 0x3F;
      int b1 = bytes[1] & 0xFF;
      int b0 = bytes[0] & 0xFF;
      int second = ((b1 << 8) + b0) / 1000;
      return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collecto\\utils\Caculator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */