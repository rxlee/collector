  package  com.kkwl.collector.init;
  
  import com.kkwl.collector.channels.netty.NettyServer;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.dao.Configuration;
  import com.kkwl.collector.init.GlobalVariablesInitComponent;
  import com.kkwl.collector.services.AlarmReportService;
  import com.kkwl.collector.services.DataStorageService;
  import com.kkwl.collector.services.RedisService;
  import java.util.concurrent.Executors;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.CommandLineRunner;
  import org.springframework.core.annotation.Order;
  import org.springframework.stereotype.Component;
  
//@Order标记定义了组件的加载顺序。
  @Component
  @Order(1)//1.GlobalVariablesInitComponent 3. KafkaComponent,MqttComponent 4. BussinessInitComponent 5. DataTransferComponent
  public class GlobalVariablesInitComponent implements CommandLineRunner {//项目启动后执行
    private static final Logger logger = LoggerFactory.getLogger(GlobalVariablesInitComponent.class);
    //设备处理周期
    @Value("${com.kkwl.collector.device_handling_timeout}")
    private int deviceHandlingPeriod;
    //并发的数量
    @Value("${com.kkwl.collector.concurrent_size}")
    private Integer concurrentSize;
    //脱机后检查时长
    @Value("${com.kkwl.collector.offline_check_duration}")
    private Short offlineCheckDuration;
    //临界值
    @Value("${com.kkwl.collector.accumelation_threshold}")
    private double accumulationThreshold;
    //ICE104标准
    @Value("${com.kkwl.collector.standard.IEC104}")
    private String iec104Standard;
    //运行模式
    @Value("${com.kkwl.collector.runmode}")
    private String runMode;
    //消息压缩类型
    @Value("${com.kkwl.collector.message.compress_type}")
    private String messageCompressType;
    //redis服务
    @Autowired
    private RedisService redisService;
    //报警服务
    @Autowired
    private AlarmReportService alarmReportService;
    //netty服务
    @Autowired
    private NettyServer nettyServer;
    //数据存储
    @Autowired
    private DataStorageService dataStorageService;
    //配置的接口 用于获取设备、属性， 创建表，存储数据  等
    @Autowired
    private Configuration configuration;
  
    //初始化全局使用的数据
    public void run(String... args) throws Exception {
      logger.info("lobal configuration begin initializing global variables.");
      
      GlobalVariables.GLOBAL_DEVICE_HANDING_PERIOD = this.deviceHandlingPeriod;
      
      GlobalVariables.GLOBAL_OFFLINE_CHECK_DURATION = this.offlineCheckDuration.shortValue();
      
      GlobalVariables.GLOBAL_REDIS_SERVCIE = this.redisService;
      
      GlobalVariables.GLOBAL_ALARM_REPORT_SERVICE = this.alarmReportService;
      
      GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE = this.runMode;
      
      GlobalVariables.NETTY_SERVER = this.nettyServer;
      
      GlobalVariables.GLOBAL_ACCUMULATION_THRESHOLD = this.accumulationThreshold;
      
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL = Executors.newScheduledThreadPool(this.concurrentSize.intValue());
      
      GlobalVariables.GLOBAL_IEC_STANDARD = this.iec104Standard;
      
      GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE = this.dataStorageService;
      
      GlobalVariables.GLOBAL_DB_HANDLER = this.configuration;
      
      GlobalVariables.MESSAGE_COMPRESS_TYPE = this.messageCompressType;
      
      GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU = null;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\init\GlobalVariablesInitComponent.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */