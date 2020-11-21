  package  com.kkwl.collector.configuration;
  
  import org.springframework.stereotype.Controller;
  import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
  import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
  
  @Controller
  public class WebMvcConfig
    extends WebMvcConfigurerAdapter {
    public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {
      viewControllerRegistry.addViewController("realtimeLog").setViewName("realtimeLog");
      viewControllerRegistry.addViewController("historyLogs").setViewName("historyLogs");
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\WebMvcConfig.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */