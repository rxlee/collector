 package  com.kkwl.collector.init;
 
 import com.kkwl.collector.channels.netty.NettyServerForDataTransmission;
 import com.kkwl.collector.common.GlobalVariables;
 import com.kkwl.collector.init.DataTransferComponent;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.core.annotation.Order;
 import org.springframework.stereotype.Component;
 
 
 
//@Order标记定义了组件的加载顺序。
 @Component
 @Order(5)//1.GlobalVariablesInitComponent  3. KafkaComponent,MqttComponent 4. BussinessInitComponent 5. DataTransferComponent
 public class DataTransferComponent//数据传输组件
   implements CommandLineRunner
 {
   @Autowired
   private NettyServerForDataTransmission nettyServerForDataTransmission;
   @Value("${netty.bind.ip_address}")
   private String ipAddress;
   @Value("${com.kkwl.data.transmission.port}")
   private String port;
    private static final Logger logger = LoggerFactory.getLogger(DataTransferComponent.class);
 
   
   public void run(String... strings) throws Exception {
     try {
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
          {
            public void run() {
              nettyServerForDataTransmission.start(ipAddress, port);
            }
          });
     }
      catch (Exception e) {
        logger.warn("Global data transfer component terminated because of ", e);
     } 
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\init\DataTransferComponent.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */