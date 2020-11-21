  package  com.kkwl.collector.services;
  
  import com.kkwl.collector.services.RedisService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
  
  @Service
  public class RedisService
  {
    @Autowired
    @Qualifier("realtime")
    private RedisTemplate redisTemplate;
    @Autowired
    @Qualifier("cache")
    private RedisTemplate cacheRedisTemplate;
    @Value("${com.kkwl.collector.redis.expire_time}")
    private int expireTime;
    @Value("${com.kkwl.collector.write_to_redis}")
    private Integer writeToRedis;
    
    public void put(String hashName, String key, String value) {
      if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
        this.redisTemplate.opsForHash().put(hashName, key, value);
        this.redisTemplate.expire(hashName, this.expireTime, TimeUnit.MINUTES);
      } 
    }
    ///批量放
    @SuppressWarnings("unchecked")
	public void batchPut(final List<Triplet<String, String, String>> list) {
        if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
          this.redisTemplate.executePipelined(new RedisCallback<Object>()
              {
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                  redisConnection.openPipeline();
                  
                  for (Triplet<String, String, String> o : list) {
                    redisConnection.hSet(((String)o.getValue0()).getBytes(), ((String)o.getValue1()).getBytes(), ((String)o.getValue2()).getBytes());
                    redisConnection.expire(((String)o.getValue0()).getBytes(), (RedisService.this.expireTime * 60));
                  } 
                  
                  redisConnection.closePipeline();
                  return null;
                }
              });
        }
      }
    ///批量删
    @SuppressWarnings("unchecked")
	public void batchDelete(final List<Pair<String, String>> list) {
        if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
          this.redisTemplate.executePipelined(new RedisCallback<Object>()
              {
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                  redisConnection.openPipeline();
                  
                  for (Pair<String, String> o : list) {
                    redisConnection.hDel(((String)o.getValue0()).getBytes(), new byte[][] { ((String)o.getValue1()).getBytes() });
                  } 
                  
                  redisConnection.closePipeline();
                  return null;
                }
              });
        }
      }
    
    public Object get(String hashName, String key) { return this.redisTemplate.opsForHash().get(hashName, key); }
  
    
    public void putAll(String hashName, Map<String, Object> keyValues) {
      if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
        this.redisTemplate.opsForHash().putAll(hashName, keyValues);
        this.redisTemplate.expire(hashName, this.expireTime, TimeUnit.MINUTES);
      } 
    }
    ///删除变量
    public void delete(String hashName, String key) {
      if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
        this.redisTemplate.opsForHash().delete(hashName, new Object[] { key });
      }
    }
  
    
    public void putCachedValue(String hashName, String key, String value) {
      if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
        this.cacheRedisTemplate.opsForHash().put(hashName, key, value);
      }
    }
  
    
    public Object getCachedValues(String hashName, String key) {
      if (this.writeToRedis != null && this.writeToRedis.intValue() == 1) {
        return this.cacheRedisTemplate.opsForHash().get(hashName, key);
      }
      return null;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\RedisService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */