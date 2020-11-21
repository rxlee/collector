  package  com.kkwl.collector.init;
  import com.kkwl.collector.init.KafkaComponent;
  import com.kkwl.collector.services.DataLoadService;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.CommandLineRunner;
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.core.annotation.Order;
  import org.springframework.stereotype.Component;
  
  @Component
  @ConditionalOnProperty(name = {"com.kkwl.collector.runmode"}, havingValue = "kafka_client")
  @Order(3)
  public class KafkaComponent implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(KafkaComponent.class);
    
    @Autowired
    private DataLoadService dataLoadService;
  
    
    public void run(String... strings) throws Exception {
      logger.info("Global configuration begin to initialize kafka component.");
      
      this.dataLoadService.loadData("KAFKA");
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\init\KafkaComponent.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */