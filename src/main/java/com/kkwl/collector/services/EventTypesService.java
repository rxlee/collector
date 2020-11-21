  package  com.kkwl.collector.services;
  
  import com.kkwl.collector.common.GlobalEventTypes;
  import com.kkwl.collector.dao.Configuration;
  import com.kkwl.collector.models.EventType;
  import com.kkwl.collector.services.EventTypesService;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Service;
  
  
  
  @Service
  public class EventTypesService
  {
    @Autowired
    private Configuration configuration;
    
    public void initAllEventTypes() {
      Map<String, Map<String, EventType>> parentIndex_sn_map = new HashMap<String, Map<String, EventType>>();
      
      List<EventType> eventTypes = this.configuration.getEventTypes();
      for (EventType eventType : eventTypes) {
        String sn = eventType.getSn();
        String parentIndex = eventType.getParentIndex();
        if (parentIndex_sn_map.containsKey(parentIndex)) {
          ((Map)parentIndex_sn_map.get(parentIndex)).put(sn, eventType); continue;
        } 
        Map<String, EventType> temp = new HashMap<String, EventType>();
        temp.put(sn, eventType);
        parentIndex_sn_map.put(parentIndex, temp);
      } 
  
      
      GlobalEventTypes.parentIndex_sn_map = parentIndex_sn_map;
    }
    
    public void updateEventTypesByParentIndex(String parentIndex) {
      if (GlobalEventTypes.parentIndex_sn_map == null) {
        initAllEventTypes();
        return;
      } 
      List<EventType> eventTypes = this.configuration.getEventTypesByParentIndex(parentIndex);
      Map<String, EventType> temp = new HashMap<String, EventType>();
      for (EventType eventType : eventTypes) {
        temp.put(eventType.getSn(), eventType);
      }
      GlobalEventTypes.parentIndex_sn_map.put(parentIndex, temp);
    }
    
    public EventType getEventType(String parentIndex, String sn) {
      if (GlobalEventTypes.parentIndex_sn_map == null) {
        initAllEventTypes();
      }
      if (GlobalEventTypes.parentIndex_sn_map.containsKey(parentIndex) && (
        (Map)GlobalEventTypes.parentIndex_sn_map.get(parentIndex)).containsKey(sn)) {
        return (EventType)((Map)GlobalEventTypes.parentIndex_sn_map.get(parentIndex)).get(sn);
      }
      
      return null;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\EventTypesService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */