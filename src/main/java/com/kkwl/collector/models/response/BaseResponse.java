  package  com.kkwl.collector.models.response;
  
  import com.fasterxml.jackson.annotation.JsonProperty;
  import com.kkwl.collector.models.response.BaseResponse;
  
  
  
  
  public class BaseResponse<T> extends Object
  {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("code")
    private Integer code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("type")
    private String type;
    @JsonProperty("count")
    private Long count;
    @JsonProperty("data")
    private T data;
    
    public Long getId() { return this.id; }
  
  
    
    public void setId(Long id) { this.id = id; }
  
  
    
    public Integer getCode() { return this.code; }
  
  
    
    public void setCode(Integer code) { this.code = code; }
  
  
    
    public String getMessage() { return this.message; }
  
  
    
    public void setMessage(String message) { this.message = message; }
  
  
    
    public String getType() { return this.type; }
  
  
    
    public void setType(String type) { this.type = type; }
  
  
    
    public Long getCount() { return this.count; }
  
  
    
    public void setCount(Long count) { this.count = count; }
  
  
    
    public T getData() { return (T)this.data; }
  
  
    
    public void setData(T data) { this.data = data; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\response\BaseResponse.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */