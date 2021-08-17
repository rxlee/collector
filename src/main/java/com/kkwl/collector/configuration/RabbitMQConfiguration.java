 package  com.kkwl.collector.configuration;
 
 import com.kkwl.collector.configuration.RabbitMQConfiguration;
/* import org.springframework.amqp.core.AnonymousQueue;
 import org.springframework.amqp.core.Binding;
 import org.springframework.amqp.core.BindingBuilder;
 import org.springframework.amqp.core.FanoutExchange;
 import org.springframework.amqp.core.Queue;
 import org.springframework.amqp.core.TopicExchange;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;

 
 @Configuration*/
 public class RabbitMQConfiguration
 {
   /*@Value("${com.kkwl.collector.amqp.fanout_exchange_name}")
   private String configurationFanoutExchangeName;
   @Value("${com.kkwl.collector.amqp.configuration_changed_notification_exchange_name}")
   private String configurationChangedNotificationExchangeName;
   @Value("${com.kkwl.collector.amqp.configuration_changed_notification_queue_name}")
   private String configurationChangedNotificationQueueName;
   @Value("${com.kkwl.collector.amqp.configuration_changed_notification_routing_key}")
   private String configurationChangedNotificationRoutingKey;
   @Value("${com.kkwl.collector.amqp.device_variable_value_changed_notification_exchange_name}")
   private String deviceVariableValueChangedNotificationExchangeName;
   @Value("${com.kkwl.collector.amqp.device_variable_value_changed_notification_topic}")
   private String deviceVariableValueChangedNotificationTopic;

   @Bean
    public Queue configurationQueue() { return new AnonymousQueue(); }


   @Bean
    public FanoutExchange configurationFanoutExchange() { return new FanoutExchange(this.configurationFanoutExchangeName); }


   @Bean
    public Binding bindExchange(Queue configurationQueue, FanoutExchange configurationFanoutExchange) {
       return BindingBuilder.bind(configurationQueue).to(configurationFanoutExchange); }


   @Bean
    public TopicExchange configurationChangedNotificationExchange() { return new TopicExchange(this.configurationChangedNotificationExchangeName); }


   @Bean
    public Queue configurationChangedNotificationQueue() { return new Queue(this.configurationChangedNotificationQueueName); }


   @Bean
   public Binding bindingConfigurationChangedNotificationExchange(Queue configurationChangedNotificationQueue, TopicExchange configurationChangedNotificationExchange) {
      return BindingBuilder.bind(configurationChangedNotificationQueue)
        .to(configurationChangedNotificationExchange)
        .with(this.configurationChangedNotificationRoutingKey);
   }


   @Bean
    public TopicExchange deviceVariableValueChangedNotificationExchange() { return new TopicExchange(this.deviceVariableValueChangedNotificationExchangeName); }


   @Bean
    public Queue deviceVariableValueChangedNotificationQueue() { return new AnonymousQueue(); }


   @Bean
   public Binding bindingDeviceVariableValueChangedNotificationExchange(Queue deviceVariableValueChangedNotificationQueue, TopicExchange deviceVariableValueChangedNotificationExchange) {
      return BindingBuilder.bind(deviceVariableValueChangedNotificationQueue)
        .to(deviceVariableValueChangedNotificationExchange)
        .with(this.deviceVariableValueChangedNotificationTopic);
   }*/
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\RabbitMQConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */