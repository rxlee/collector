  package  com.kkwl.collector;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.scheduling.annotation.EnableScheduling;
  import org.springframework.transaction.annotation.EnableTransactionManagement;
  
  
  
  @SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class})
  @EnableTransactionManagement
  @EnableScheduling //启用spring的定时任务功能
  public class CollectorApplication
  {
    public static void main(String[] args) { SpringApplication.run(com.kkwl.collector.CollectorApplication.class, args); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\CollectorApplication.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */