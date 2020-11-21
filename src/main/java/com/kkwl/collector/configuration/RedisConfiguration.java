  package  com.kkwl.collector.configuration;
  
  import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
  
  @Configuration
  public class RedisConfiguration
    extends CachingConfigurerSupport
  {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.password}")
    private String password;
    @Value("${spring.redis.usePassword}")
    private int usePassword;
    @Value("${spring.redis.port}")
    private Integer port;
    @Value("${spring.redis.timeout}")
    private Integer timeout;
    @Value("${spring.redis.pool.max-active}")
    private Integer maxActive;
    @Value("${spring.redis.pool.max-wait}")
    private Integer maxWait;
    @Value("${spring.redis.pool.max-idle}")
    private Integer maxIdle;
    @Value("${spring.redis.pool.min-idle}")
    private Integer minIdle;
    @Value("${com.kkwl.collector.write_to_redis}")
    private Short writeToRedis;
    private static final int realtimeDatabase = 0;
    private static final int cacheDatabase = 1;
    
    @Bean
    public KeyGenerator keyGenerator() { 
    	return new KeyGenerator()
        {
          public Object generate(Object target, Method method, Object... params) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(method.getName());
            for (Object obj : params) {
              sb.append(obj.toString());
            }
            return sb.toString();
          }
        };
      }
    
    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
      String[] cacheNames = { "app_default", "users", "blogs", "goods", "configs", "info" };
      return new RedisCacheManager(redisTemplate);
    }
  
    
    @Bean(name = {"realtime"})
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
      if (this.writeToRedis.shortValue() == 0) {
        return null;
      }
      StringRedisTemplate template = new StringRedisTemplate();
      template.setConnectionFactory(connectionFactory(this.host, this.port.intValue(), this.password, this.maxIdle.intValue(), this.maxActive.intValue(), 0, this.maxWait.intValue(), false));
      Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
      ObjectMapper om = new ObjectMapper();
      om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
      om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
      jackson2JsonRedisSerializer.setObjectMapper(om);
      template.setValueSerializer(jackson2JsonRedisSerializer);
      template.afterPropertiesSet();
      return template;
    }
  
    
    @Bean(name = {"cache"})
    public RedisTemplate<String, String> cacheRedisTemplate(RedisConnectionFactory factory) {
      if (this.writeToRedis.shortValue() == 0) {
        return null;
      }
      StringRedisTemplate template = new StringRedisTemplate();
      template.setConnectionFactory(connectionFactory(this.host, this.port.intValue(), this.password, this.maxIdle.intValue(), this.maxActive.intValue(), 1, this.maxWait.intValue(), false));
      Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
      ObjectMapper om = new ObjectMapper();
      om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
      om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
      jackson2JsonRedisSerializer.setObjectMapper(om);
      template.setValueSerializer(jackson2JsonRedisSerializer);
      template.afterPropertiesSet();
      return template;
    }
  
  
    
    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate redisTemplate) { return redisTemplate.opsForHash(); }
  
  
    
    public RedisConnectionFactory connectionFactory(String hostName, int port, String password, int maxIdle, int maxTotal, int index, long maxWaitMillis, boolean testOnBorrow) {
      JedisConnectionFactory jedis = new JedisConnectionFactory();
      jedis.setHostName(hostName);
      jedis.setPort(port);
      if (this.usePassword == 1 && password != null && !password.isEmpty()) {
        jedis.setPassword(password);
      }
      if (index != 0) {
        jedis.setDatabase(index);
      }
      jedis.setPoolConfig(poolCofig(maxIdle, maxTotal, maxWaitMillis, testOnBorrow));
      
      jedis.afterPropertiesSet();
      return jedis;
    }
  
  
    
    public JedisPoolConfig poolCofig(int maxIdle, int maxTotal, long maxWaitMillis, boolean testOnBorrow) {
      JedisPoolConfig poolCofig = new JedisPoolConfig();
      poolCofig.setMaxIdle(maxIdle);
      poolCofig.setMaxTotal(maxTotal);
      poolCofig.setMaxWaitMillis(maxWaitMillis);
      poolCofig.setTestOnBorrow(testOnBorrow);
      return poolCofig;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\RedisConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */