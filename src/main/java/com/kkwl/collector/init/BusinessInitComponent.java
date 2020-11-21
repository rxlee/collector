  package  com.kkwl.collector.init;
  
  import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.init.BusinessInitComponent;
import com.kkwl.collector.services.AlarmReportService;
import com.kkwl.collector.services.ConfigurationChangedNotificationReceiver;
import com.kkwl.collector.services.DataParserService;
import com.kkwl.collector.services.DeviceOfflineCheckingService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.javatuples.Triplet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

//@Order标记定义了组件的加载顺序。
  @Component
  @Order(4) //业务初始化组件（业务处理）
  public class BusinessInitComponent implements CommandLineRunner
  {
    private static final Logger logger = LoggerFactory.getLogger(BusinessInitComponent.class);
    
    private static final int PERIOD = 2;
    //报警通知
    @Autowired
    AlarmReportService alarmReportService;
    //配置
    @Autowired
    Configuration configuration;
    //设备掉线检测
    @Autowired
    DeviceOfflineCheckingService offlineCheckingService;
    //数据解析器
    @Autowired
    DataParserService dataParserService;
    //配置改变通知接收器
    @Autowired
    ConfigurationChangedNotificationReceiver configurationModifier;

    private LocalDateTime last5MinutesTaskTime = null;//最后5分钟任务时间
    private LocalDateTime last15MinutesTaskTime = null;//最后15分钟任务时间
    private LocalDateTime last30MinutesTaskTime = null;//最后30分钟任务时间
    private LocalDateTime last1HourTaskTime = null;//最后1小时任务任务时间
    private LocalDateTime lastGenerateTableTime = null;//最后生成表的时间
    private LocalDateTime lastStatisticsTime = null;//最后统计的时间

    public void run(String... strings) throws Exception {
    	//系统启动后创建当天表、当月表
      GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.createDayHistoryTable(0);
      GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.createMonthHistoryTable(0);
      //线程池-周期报告数据
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              LocalDateTime now = LocalDateTime.now();
              //1如果 整点或者59分时 存储日统计
              if ((now.getMinute() == 0 || now.getMinute() == 59) && (BusinessInitComponent.this.lastStatisticsTime == null || Duration.between(BusinessInitComponent.this.lastStatisticsTime, now).toMillis() > 65000L))
              {

                GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                    {
                      public void run() {
                        try {
                          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.storeDayStatics(); //调用-存储日统计
                        } catch (Exception e) {
                          logger.error("Global configuration error occured when periodically report data.", e);
                        } 
                      }
                    });
                BusinessInitComponent.this.lastStatisticsTime = now;
              } 
              //2如果 5分钟的倍数，执行不同的周期报告数据
              if (now.getMinute() % 5 == 0 && (BusinessInitComponent.this.last5MinutesTaskTime == null || Duration.between(BusinessInitComponent.this.last5MinutesTaskTime, now).toMinutes() > 2L))
              {
                System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES, 0, GlobalVariables.GLOBAL_MEMORY_BYTES_COPY, 0, 80000000);
                //整5分钟执行一次报告数据
                GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                    {
                    public void run() {
                        try {
                          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.periodReportData("5分钟"); ///调用
                        } catch (Exception ex) {
                          logger.error("Global configuration error occured when periodically report data.", ex);
                        } 
                      }
                    });
                BusinessInitComponent.this.last5MinutesTaskTime = now;
                //15分钟执行一次报告数据
                if (now.getMinute() % 15 == 0 && (BusinessInitComponent.this.last15MinutesTaskTime == null || Duration.between(BusinessInitComponent.this.last15MinutesTaskTime, now).toMinutes() > 2L)) {
                  GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                      {
                        public void run() {
                          try {
                            GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.periodReportData("15分钟");
                          } catch (Exception ex) {
                            logger.error("Global configuration error occured when periodically report data.", ex);
                          } 
                        }
                      });
                  BusinessInitComponent.this.last15MinutesTaskTime = now;
                } 
                //30分钟执行一次报告数据
                if (now.getMinute() % 30 == 0 && (BusinessInitComponent.this.last30MinutesTaskTime == null || Duration.between(BusinessInitComponent.this.last30MinutesTaskTime, now).toMinutes() > 2L)) {
                  GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                      {
                        public void run() {
                          try {
                            GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.periodReportData("30分钟");
                          } catch (Exception ex) {
                            logger.error("Global configuration error occured when periodically report data.", ex);
                          } 
                        }
                      });
                  BusinessInitComponent.this.last30MinutesTaskTime = now;
                }
                //1小时执行一次报告数据
                if (now.getMinute() == 0 && (BusinessInitComponent.this.last1HourTaskTime == null || Duration.between(BusinessInitComponent.this.last1HourTaskTime, now).toMinutes() > 2L)) {
                  GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                      {
                        public void run() {
                          try {
                            GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.periodReportData("1小时");
                          } catch (Exception ex) {
                            logger.error("Global configuration error occured when periodically report data.", ex);
                          } 
                        }
                      });
                  BusinessInitComponent.this.last1HourTaskTime = now;
                } 
              }
              //3如果配置改变了，执行更新或者删除线程
              else if (!GlobalVariables.GLOBAL_CONFIGURATION_CHANGING_QUEUE.isEmpty()) {
                
                Triplet<String, JSONObject, String> configurationChanging = (Triplet)GlobalVariables.GLOBAL_CONFIGURATION_CHANGING_QUEUE.poll();
                if (((String)configurationChanging.getValue2()).equals("update")) {
                  BusinessInitComponent.this.configurationModifier.updateProcess((String)configurationChanging.getValue0(), (JSONObject)configurationChanging.getValue1());
                } else if (((String)configurationChanging.getValue2()).equals("delete")) {
                  BusinessInitComponent.this.configurationModifier.deleteProcess((String)configurationChanging.getValue0(), (JSONObject)configurationChanging.getValue1());
                }
              }
              //4否则 调用-解析设备变量
              else {
                Instant startTime = Instant.now();
                BusinessInitComponent.this.dataParserService.parseDeviceVariables(); //调用-解析设备变量
                Instant endTime = Instant.now();
                
                GlobalVariables.TOTAL_PARSING_TIME += Duration.between(startTime, endTime).toMillis();
                GlobalVariables.TOTAL_PARSING_COUNT++;
              } 
            } catch (Exception e) {
              logger.error("Global configuration call data parser error.", e);
            } 
          }
        }, 0L, 30L, TimeUnit.MILLISECONDS);  //延迟30毫秒
      //线程池-30秒循环创建下一天的日表，记录建表时间，如果有时间记录则不重复建表
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable()
        {
          public void run() {
            try {
              LocalDateTime now = LocalDateTime.now();
                ///22：56
              if (now.getMinute() == 56 && now.getHour() == 22 && (BusinessInitComponent.this.lastGenerateTableTime == null || Duration.between(BusinessInitComponent.this.lastGenerateTableTime, now).toMinutes() > 2L))
              {
                GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
                    {
                      public void run() {
                        ///创建日历史表
                        try {
                          GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.createDayHistoryTable(1);
                        } catch (Exception ex) {
                          logger.error("Global configuration error occured when creating day history table", ex);
                        } 
                        
                        LocalDate localDate = LocalDate.now();

                        if (localDate.getDayOfMonth() == localDate.lengthOfMonth()) {
                          ///创建月历史表
                          try {
                            GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.createMonthHistoryTable(1);
                          } catch (Exception ex) {
                            logger.error("Global configuration error occured when creating month history table", ex);
                          } 
                        }
                      }
                    });
                BusinessInitComponent.this.lastGenerateTableTime = now;
              } 
            } catch (Exception e) {
              logger.error("error occured when executing scheduled task.", e);
            } 
          }
        }, 0L, 30L, TimeUnit.SECONDS); ///周期30秒
      //线程池-批量存值到redis
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.batchSaveValuesToRedis();  //固定周期执行线程  批量存redis
            } catch (Exception e) {
              logger.error("Global configuration call batch save redis error.", e);
            } 
          }
        }, 0L, 100L, TimeUnit.MILLISECONDS);
      //线程池-从redis批量删除数据
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.batchDeleteValueFromRedis(); //固定周期执行线程 批量从redis删
            } catch (Exception e) {
              logger.error("Global configuration call batch delete redis error.", e);
            } 
          }
        }, 0L, 200L, TimeUnit.MILLISECONDS);
      //线程池-批量存数据库
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              GlobalVariables.GLOBAL_DATA_STORAGE_SERVICE.batchSaveValuesToDB();  ///存数据库
            } catch (Exception e) {
              logger.error("Global configuration call batch save db error.", e);
            } 
          }
        }, 0L, 1L, TimeUnit.SECONDS);
      //线程池-检查设备是否离线
      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable()
        {
          public void run() {
            try {
              BusinessInitComponent.this.offlineCheckingService.checkIfDeviceOffline();
            } catch (Exception e) {
              logger.error("Global configuration call check offline error.", e);
            } 
          }
        }, 0L, GlobalVariables.GLOBAL_OFFLINE_CHECK_DURATION, TimeUnit.SECONDS);

      ///调用-需量、力率告警
//      GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable(){
//        @Override
//        public void run() {
//          try{
//            dataParserService.handleDemandCosAlarm();
//          }catch(Exception e){
//            logger.error("Gloable congfiguration call handleDemandCosAlarm error.",e);
//          }
//        }
//      },200L,1000L,TimeUnit.MILLISECONDS);
    }
    //调用-时耗、日耗告警
//     GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable(){
//    @Override
//    public void run() {
//      try{
//        dataParserService.hourDayConsumeAlarm();
//      }catch(Exception e){
//        logger.error("Gloable congfiguration call handleDemandCosAlarm error.",e);
//      }
//    }
//  },200L,1000L,TimeUnit.MILLISECONDS);

  }
