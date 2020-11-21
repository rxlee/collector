  package  com.kkwl.collector.services;
  
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.devices.communication.ClientDTU;
  import com.kkwl.collector.devices.communication.ServerDTU;
  import com.kkwl.collector.services.DataStorageService;
  import com.kkwl.collector.services.RedisService;
  import java.time.Instant;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Service;
  
  
  @Service
  public class DeviceOfflineCheckingService
  {
    private static final Logger logger = LoggerFactory.getLogger(com.kkwl.collector.services.DeviceOfflineCheckingService.class);
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private DataStorageService dataStorageService;
    
    public void checkIfDeviceOffline() {
      Instant currentInstant = Instant.now();
      String currentTime = GlobalVariables.GLOBAL_COMMON_DATE_FORMATTER.format(currentInstant);
      
      for (ServerDTU serverDtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
        serverDtu.checkOnlineOffline();
      }
      
      for (ClientDTU clientDtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
        clientDtu.checkOnlineOffline();
      }
      
      for (BaseBusinessDevice businessDevice : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES)
        businessDevice.checkOnlineOffline(); 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\DeviceOfflineCheckingService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */