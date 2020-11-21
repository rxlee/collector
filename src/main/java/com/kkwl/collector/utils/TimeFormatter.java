  package  com.kkwl.collector.utils;
  
  import java.sql.Date;
  import java.sql.Timestamp;
  import java.text.SimpleDateFormat;
  
  
  public class TimeFormatter
  {
    private static final String COMMON_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String YEAR_MONTH_DAY = "yyyy-MM-dd";
    private static final String TIMESTAMPCODE = "yyyyMMddHHmmssSSS";
    private static final String CODE_FORMAT = "yyyyMMdd";
    
    public static String timestampToCommonString(Timestamp timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(Long.valueOf(timestamp.getTime()));
    }
    
    public static String timestampToShortString(Timestamp timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(Long.valueOf(timestamp.getTime()));
    }
    
    public static String dateToShortString(Date date) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(date);
    }
    
    public static String timestampToCode(Timestamp timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
      return sdf.format(timestamp);
    }
    
    public static String timestampToShortCode(Timestamp timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      return sdf.format(timestamp);
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collecto\\utils\TimeFormatter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */