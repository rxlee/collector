  package  com.kkwl.collector.configuration;
  
  import org.springframework.context.annotation.Configuration;
  import org.springframework.messaging.simp.config.MessageBrokerRegistry;
  import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
  import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
  import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
  
  
  @Configuration
  @EnableWebSocketMessageBroker
  public class WebSocketConfiguration
    extends AbstractWebSocketMessageBrokerConfigurer
  {
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) { stompEndpointRegistry.addEndpoint(new String[] { "/logEndpoint" }).withSockJS(); }
  
  
  
  
  
    
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) { messageBrokerRegistry.enableSimpleBroker(new String[] { "/topic" }); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\WebSocketConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */