  package  com.kkwl.collector.services;
  import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.RunMode;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.models.CollectorRegisterArea;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.services.DeviceVariableService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
  
  @Component
  public class DeviceVariableService {
    private static Logger logger = LoggerFactory.getLogger(DeviceVariable.class);
    
    @Autowired
    private Configuration configuration;
    //根据采集设备sns获得设备变量
    public List<DeviceVariable> getDeviceVariablesByDeviceSns(List<String> collectorDeviceSns) {
      if (collectorDeviceSns == null || collectorDeviceSns.isEmpty()) {
        return new ArrayList();
      }

      List<String> parentIndexs = this.configuration.getDeviceStationSns(collectorDeviceSns); //初始化 查库根据采集设备Sn获取站点集合
      if (parentIndexs == null || parentIndexs.isEmpty()) {
        return new ArrayList();
      }
      
      return getDeviceVariables(collectorDeviceSns, parentIndexs); //根据采集设备sn和站点获得变量
    }
    
    public List<DeviceVariable> getDeviceVariablesByVarParentIndexs(String parentIndex) {
      List<String> collectorDeviceSns = this.configuration.getDeviceSnsInStation(parentIndex);
      if (collectorDeviceSns == null || collectorDeviceSns.isEmpty()) {
        return new ArrayList();
      }

      List<String> parentIndexs = new ArrayList<String>();
      parentIndexs.add(parentIndex);
      
      return getDeviceVariables(collectorDeviceSns, parentIndexs);
    }
    /**
     * 获取所有的设备变量（属性）
     * @param collectorDeviceSns  所有采集设备的sn
     * @param parentIndexs        采集设备对应的站点sn
     * @return   				  所有的设备变量
     */
    //根据采集设备sn和站点获得变量
    private List<DeviceVariable> getDeviceVariables(List<String> collectorDeviceSns, List<String> parentIndexs) {
      if (collectorDeviceSns == null || collectorDeviceSns.isEmpty()) {
        return new ArrayList();
      }
      if (parentIndexs == null || parentIndexs.isEmpty()) {
        return new ArrayList();
      }
      //筛选条件（采集设备的sn）
      Map<String, Object> filter = new HashMap<String, Object>();
      filter.put("collector_device_sns", collectorDeviceSns);
      //验证当前采集设备的运行模式
      if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.TCP_SERVER.value())) {
        //根据采集设备的sn获取-整体数据区（包括名称，地址，起始长度 ，寄存器区.. 对应页面--设备模板--详情页面--设备协议（MODBUSRTU）--选择数据区）
        List<CollectorRegisterArea> collectorRegisterAreas = this.configuration.getDeviceRegisterMapList(filter); //查库
        Map<String, Map<String, CollectorRegisterArea>> collectorRegisterAreaStartIndexMap = new HashMap<String, Map<String, CollectorRegisterArea>>();
       
        //collectorRegisterAreaStartIndexMap 
        // 对应数据  {采集设备sn:{数据区名称： 数据区对象,数据区名称： 数据区对象}}
        for (CollectorRegisterArea collectorRegisterArea : collectorRegisterAreas) {
          String collectorDeviceSn = collectorRegisterArea.getCollectorDeviceSn();
          if (collectorRegisterAreaStartIndexMap.containsKey(collectorDeviceSn)) {
            String registerName = collectorRegisterArea.getName();
            if (!((Map)collectorRegisterAreaStartIndexMap.get(collectorDeviceSn)).containsKey(registerName))
              ((Map)collectorRegisterAreaStartIndexMap.get(collectorDeviceSn)).put(registerName, collectorRegisterArea); 
            continue;
          } 
         
          String registerName = collectorRegisterArea.getName();
          Map<String, CollectorRegisterArea> registerNameStartMap = new HashMap<String, CollectorRegisterArea>();
          registerNameStartMap.put(registerName, collectorRegisterArea);
          collectorRegisterAreaStartIndexMap.put(collectorDeviceSn, registerNameStartMap);
        } 
  
        
        filter = new HashMap<String, Object>();
        filter.put("parent_indexes", parentIndexs);
        //根据站点获取设备变量
        List<DeviceVariable> deviceVariables = this.configuration.getDeviceVariables(filter); //初始化 查库根据站点获得变量
  
        
        Iterator<DeviceVariable> deviceVariableIterator = deviceVariables.iterator();
        while (deviceVariableIterator.hasNext()) {
          DeviceVariable deviceVariable = (DeviceVariable)deviceVariableIterator.next();
          
          String collectorDeviceSn = deviceVariable.getCollectorDeviceSn();
          if (collectorDeviceSn == null || collectorDeviceSn.isEmpty()) {
            //e.g SZ001_LXBE201IN__Wpp + SZ001_LXBE101IN__Wpp 
        	//SZ001_LXBE_2TRBUS__COMM2TRBUS | SZ001_LXBE_1TRS__COMM1TRS | SZ001_LXBE_1EN__COMM1EN | SZ001_LXBE101IN__COMM101IN | SZ001_LXBETR104__COMMTR104 | SZ001_LXBETR105__COMMTR105 
            if (deviceVariable.getExpression() == null || deviceVariable.getExpression().isEmpty())
            {
              deviceVariableIterator.remove(); }
            continue;
          } 
          if (collectorRegisterAreaStartIndexMap.containsKey(collectorDeviceSn)) {
                if (!collectorDeviceSns.contains(collectorDeviceSn)) {
                  //如果采集设备中没有这个当前的sn，去除，不使用
                  deviceVariableIterator.remove();
                  continue;
                }
                String registerName = deviceVariable.getRegisterName();
                if (registerName == null || registerName.isEmpty()) {
                  //如果寄存器没有名字或者空，去除，不使用
                  deviceVariableIterator.remove();
                  continue;
                }
                if (((Map)collectorRegisterAreaStartIndexMap.get(collectorDeviceSn)).containsKey(registerName)) {
                    //typeIndex = monitor_device_var表register_index + 数据区的起始位置
                  deviceVariable.setRegisterType(((CollectorRegisterArea)((Map)collectorRegisterAreaStartIndexMap.get(collectorDeviceSn)).get(registerName)).getType());
                  int typeIndex = deviceVariable.getRegisterIndex().intValue() + ((CollectorRegisterArea)((Map)collectorRegisterAreaStartIndexMap.get(collectorDeviceSn)).get(registerName)).getStart();
                  deviceVariable.setRegisterTypeIndex(Integer.valueOf(typeIndex));
                  continue;
                }
            deviceVariable.setRegisterType(deviceVariable.getRegisterName());
            deviceVariable.setRegisterTypeIndex(deviceVariable.getRegisterIndex());
            continue;
          } 
          
          deviceVariableIterator.remove();
        }
        return deviceVariables;  //返回变量
      }
      //如果当前采集设备的运行模式为MQTT_CLIENT或KAFKA_CLIENT
      if (GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.MQTT_CLIENT.value()) || GlobalVariables.GLOBAL_COLLECTOR_RUN_MODE.equals(RunMode.KAFKA_CLIENT.value()))
      {
        filter = new HashMap<String, Object>();
        filter.put("parent_indexes", parentIndexs);
        List<DeviceVariable> deviceVariables = this.configuration.getDeviceVariables(filter);

        Iterator<DeviceVariable> deviceVariableIterator = deviceVariables.iterator();
        while (deviceVariableIterator.hasNext()) {
          DeviceVariable deviceVariable = (DeviceVariable)deviceVariableIterator.next();
          if (!collectorDeviceSns.contains(deviceVariable.getCollectorDeviceSn())) {
            deviceVariableIterator.remove();
            continue;
          } 
          String collectorDeviceSn = deviceVariable.getCollectorDeviceSn();
          if (collectorDeviceSn == null || collectorDeviceSn.isEmpty())
          {
            if (deviceVariable.getExpression() == null || deviceVariable.getExpression().isEmpty())
            {
              deviceVariableIterator.remove();
            }
          }
        }
        return deviceVariables;
      } 
      return new ArrayList();
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\services\DeviceVariableService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */