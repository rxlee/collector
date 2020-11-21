 package  com.kkwl.collector.api;
 
 import com.kkwl.collector.api.WebSocketController;
 import com.kkwl.collector.common.GlobalVariables;
 import com.kkwl.collector.models.request.WebSocketMessage;
 import com.kkwl.collector.services.WebSocketService;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.messaging.handler.annotation.MessageMapping;
 import org.springframework.stereotype.Controller;
 import org.springframework.web.bind.annotation.ResponseBody;
 
 @Controller
 public class WebSocketController {
   @Autowired
   private WebSocketService webSocketService;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
   
   @MessageMapping({"/bindDtu"})
   @ResponseBody
   public String bindDtu(WebSocketMessage webSocketMessage) throws Exception {
      logger.debug("WebSocketController received bind dtu request of " + webSocketMessage.getSn());
     
      if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null) {
        logger.warn("WebSocketController can't bind dtu due to " + GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU + " is binded.");
        this.webSocketService.sendMessage("ERROR: " + GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU + " is binded!");
     } else {
        logger.info("WebSocketController " + webSocketMessage.getSn() + " is successfully binded.");
        this.webSocketService.sendMessage("INFO: " + webSocketMessage.getSn() + " is successfully binded!");
        GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU = webSocketMessage.getSn();
        this.webSocketService.sendMessage();
     } 
     
      return "";
   }
   //“断开”时，清空GLOBAL_WEBSOCKET_BIND_DTU
   @MessageMapping({"/unbindDtu"})
   @ResponseBody
   public String unbindDtu(WebSocketMessage webSocketMessage) throws Exception {
      if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null) {
        GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU = null;
     }
     
      GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.clear();
      this.webSocketService.sendMessage("INFO: " + GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU + " is successfully unbinded!");
     
      return "";
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\api\WebSocketController.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */