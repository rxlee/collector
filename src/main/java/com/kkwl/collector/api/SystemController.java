  package  com.kkwl.collector.api;
  import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kkwl.collector.channels.netty.NettyServer;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.communication.ClientDTU;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.models.EventType;
import com.kkwl.collector.models.response.BaseResponse;
import com.kkwl.collector.services.ConfigurationChangedNotificationReceiver;
import com.kkwl.collector.services.EventTypesService;
import com.kkwl.collector.utils.TimeFormatter;
  
  @Api("API - System Controller")
  @RestController
  public class SystemController {
    @Autowired
    private NettyServer nettyServer;
    @Autowired
    private EventTypesService eventTypesService;
    
    @ApiOperation(value = "查询在线设备", notes = "查询在线设备")
    @RequestMapping(value = {"/devices/online"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse> getOnlineDeviceInfo() {
      List<BaseBusinessDevice> onlineDevices = new ArrayList<BaseBusinessDevice>();
      
      for (BaseBusinessDevice device : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
        if (device.getStatus() == StatusType.ONLINE) {
          onlineDevices.add(device);
        }
      } 
      
      List<Map<String, Object>> infos = new ArrayList<Map<String, Object>>();
      for (BaseBusinessDevice onlineDevice : onlineDevices) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", onlineDevice.getSn());
        info.put("last_recv_data_packet_time", 
            TimeFormatter.timestampToCommonString(onlineDevice.getLastDataPackteTime()));
        
        infos.add(info);
      } 
      
      BaseResponse retObj = new BaseResponse();
      retObj.setCode(Integer.valueOf(0));
      retObj.setCount(new Long(infos.size()));
      retObj.setData(infos);
      return ResponseEntity.ok(retObj);
    }
    @Autowired
    private ConfigurationChangedNotificationReceiver receiver;
    @Value("${com.kkwl.collector.redis.expire_time}")
    private int expireTime;
    @ApiOperation(value = "查询在线设备", notes = "查询在线设备")
    @RequestMapping(value = {"/dtus/online"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse> getOnlineDTUInfo() {
      List<ServerDTU> onlineServerDtus = new ArrayList<ServerDTU>();
      
      for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
        if (dtu.getStatus() == StatusType.ONLINE) {
          onlineServerDtus.add(dtu);
        }
      } 
      
      List<ClientDTU> onlineClientDtus = new ArrayList<ClientDTU>();
      for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
        if (dtu.getStatus() == StatusType.ONLINE) {
          onlineClientDtus.add(dtu);
        }
      } 
      
      List<Map<String, Object>> infos = new ArrayList<Map<String, Object>>();
      for (ServerDTU onlineDtu : onlineServerDtus) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", onlineDtu.getSn());
        info.put("last_recv_data_packet_time", 
            TimeFormatter.timestampToCommonString(onlineDtu.getLastDataPackteTime()));
        
        infos.add(info);
      } 
      
      for (ClientDTU onlineDtu : onlineClientDtus) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", onlineDtu.getSn());
        info.put("last_recv_data_packet_time", 
            TimeFormatter.timestampToCommonString(onlineDtu.getLastDataPackteTime()));
        
        infos.add(info);
      } 
      
      BaseResponse retObj = new BaseResponse();
      retObj.setCode(Integer.valueOf(0));
      retObj.setCount(new Long(infos.size()));
      retObj.setData(infos);
      return ResponseEntity.ok(retObj);
    }
    
    @ApiOperation(value = "查询所有设备", notes = "查询在线设备")
    @RequestMapping(value = {"/devices"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse> getAllDevices() {
      List<BaseBusinessDevice> devices = new ArrayList<BaseBusinessDevice>();
      
      for (BaseBusinessDevice device : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
        devices.add(device);
      }
      
      List<Map<String, Object>> infos = new ArrayList<Map<String, Object>>();
      for (BaseBusinessDevice device : devices) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", device.getSn());
        info.put("status", 
            (device.getStatus() == null) ? StatusType.OFFLINE.strValue() : device.getStatus().strValue());
        info.put("last_recv_data_packet_time", 
            TimeFormatter.timestampToCommonString(device.getLastDataPackteTime()));
        info.put("connection_timeout_duration", Short.valueOf(device.getConnectionTimeoutDuration()));
        infos.add(info);
      } 
      
      BaseResponse retObj = new BaseResponse();
      retObj.setCode(Integer.valueOf(0));
      retObj.setCount(new Long(infos.size()));
      retObj.setData(infos);
      return ResponseEntity.ok(retObj);
    }
    
    @ApiOperation(value = "查询所有设备", notes = "查询在线设备")
    @RequestMapping(value = {"/dtus"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse> getAllDtus() {
      List<Map<String, Object>> infos = new ArrayList<Map<String, Object>>();
      
      for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", dtu.getSn());
        info.put("access_type", dtu.getType());
        info.put("ip_address", dtu.getIpAddress());
        info.put("port", dtu.getPort());
        info.put("identifier", dtu.getIdentifier());
        info.put("status", (dtu.getStatus() == null) ? StatusType.OFFLINE.strValue() : dtu.getStatus().strValue());
        info.put("connection_timeout_duration", Short.valueOf(dtu.getConnectionTimeoutDuration()));
        info.put("last_recv_data_packet_time", TimeFormatter.timestampToCommonString(dtu.getLastDataPackteTime()));
        
        infos.add(info);
      } 
      
      for (ClientDTU dtu : GlobalVariables.GLOBAL_CLIENT_DTUS) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("sn", dtu.getSn());
        info.put("access_type", dtu.getType());
        info.put("status", (dtu.getStatus() == null) ? StatusType.OFFLINE.strValue() : dtu.getStatus().strValue());
        info.put("connection_timeout_duration", Short.valueOf(dtu.getConnectionTimeoutDuration()));
        info.put("last_recv_data_packet_time", TimeFormatter.timestampToCommonString(dtu.getLastDataPackteTime()));
        
        infos.add(info);
      } 
      
      BaseResponse retObj = new BaseResponse();
      retObj.setCode(Integer.valueOf(0));
      retObj.setCount(new Long(infos.size()));
      retObj.setData(infos);
      return ResponseEntity.ok(retObj);
    }
  
    
    @ApiOperation(value = "查询通信设备变量列表", notes = "查询通信设备变量列表")
    @RequestMapping(value = {"/collector_device/{sn}/variables"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getDeviceVariables(@PathVariable("sn") String collectorDeviceSn) {
      BaseResponse<List<Map<String, Object>>> retObj = new BaseResponse<List<Map<String, Object>>>();
      
      List<Map<String, Object>> variableMaps = new ArrayList<Map<String, Object>>();
      List<DeviceVariable> variables = GlobalVariables.getDeviceVariables();
      for (DeviceVariable variable : variables) {
        if (variable.getCollectorDeviceSn() == null || !variable.getCollectorDeviceSn().equals(collectorDeviceSn)) {
          continue;
        }
        
        Map<String, Object> variableMap = createDeviceVarialInfoMap(variable);
        
        variableMaps.add(variableMap);
      } 
      
      retObj.setCode(Integer.valueOf(200));
      retObj.setCount(new Long(variableMaps.size()));
      retObj.setData(variableMaps);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
  
    
    @ApiOperation(value = "查询当前系统中一个通信设备下所有变量、数据区、索引和类型的拼接", notes = "")
    @RequestMapping(value = {"/collector_device/{sn}/composed_name"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<List<String>>> getDeviceComposedNames(@PathVariable("sn") String collectorDeviceSn) {
      BaseResponse<List<String>> retObj = new BaseResponse<List<String>>();
      List<String> composedNames = new ArrayList<String>();
      
      for (String composedName : GlobalVariables.getSameIndexComposedNames()) {
        if (composedName.indexOf(collectorDeviceSn) == 0) {
          composedNames.add(composedName);
        }
      } 
      
      retObj.setCode(Integer.valueOf(200));
      retObj.setCount(new Long(composedNames.size()));
      retObj.setData(composedNames);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
  
    
    @ApiOperation(value = "查询当前系统中具有相同通信设备、数据区、索引和类型的变量", notes = "")
    @RequestMapping(value = {"/variables/same_composed_name/{composed_name}"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getVariablesWithSameComposedName(@PathVariable("composed_name") String composedName) {
      BaseResponse<List<Map<String, Object>>> retObj = new BaseResponse<List<Map<String, Object>>>();
      List<Map<String, Object>> variableMaps = new ArrayList<Map<String, Object>>();
      
      List<DeviceVariable> variables = (List)GlobalVariables.getSameIndexVariablesMap().get(composedName);
      if (variables == null || variables.size() == 0) {
        retObj.setCode(Integer.valueOf(200));
        retObj.setCount(Long.valueOf(0L));
        retObj.setData(variableMaps);
      } else {
        String[] arr = composedName.split("\\|");
        
        String businessDeviceSn = arr[0];
        if (businessDeviceSn.equals("null")) {
          
          retObj.setCode(Integer.valueOf(200));
          retObj.setCount(Long.valueOf(0L));
          retObj.setData(variableMaps);
        } else {
          String sectionName = arr[1]; //YC
          int sectionIndex = Integer.parseInt(arr[2]); //地址
          int bytesLen = Integer.parseInt(arr[3]); //长度
          String bytesString = "";
          
          if (GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.containsKey(businessDeviceSn)) {
            
            BaseBusinessDevice businessDevice = (BaseBusinessDevice)GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP.get(businessDeviceSn);
            if (bytesLen == 1) {
              Byte b = businessDevice.getSectionByteForDebug(sectionName, sectionIndex);
              if (b == null) {
                
                bytesString = "null";
              } else {
                bytesString = String.format("%02X ", new Object[] { b });
              } 
            } else {
              byte[] bytes = businessDevice.getSectionBytesForDebug(sectionName, sectionIndex, bytesLen);
              if (bytes == null) {
                bytesString = "null";
              } else {
                for (byte b : bytes) {
                  bytesString = bytesString + String.format("%02X ", new Object[] { Byte.valueOf(b) });
                } 
              } 
            } 
          } 
          
          for (DeviceVariable variable : variables) {
            Map<String, Object> variableMap = createDeviceVarialInfoMap(variable);
            variableMap.put("memory value", bytesString);
            variableMaps.add(variableMap);
          } 
          
          retObj.setCode(Integer.valueOf(200));
          retObj.setCount(new Long(variableMaps.size()));
          retObj.setData(variableMaps);
        } 
      } 
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "查询当前系统中具有相同通信设备、数据区、索引和类型的变量", notes = "")
    @RequestMapping(value = {"/memory/{pos}"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<String>> getMemoryInfo(@PathVariable("pos") Integer pos) {
      BaseResponse<String> retObj = new BaseResponse<String>();
      
      byte[] memoryBytes = new byte[80];
      if (pos.intValue() >= 0 && pos.intValue() <= 1000000) {
        System.arraycopy(GlobalVariables.GLOBAL_MEMORY_BYTES, pos.intValue() * 80, memoryBytes, 0, 80);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : memoryBytes) {
          sb.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
        } 
        
        retObj.setCode(Integer.valueOf(200));
        retObj.setCount(new Long(sb.length()));
        retObj.setData(sb.toString());
      } else {
        retObj.setCode(Integer.valueOf(400));
        retObj.setData("Invalid pos.");
      } 
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "查询内存变量列表", notes = "查询内存变量列表")
    @RequestMapping(value = {"/memory/variables"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getMemoryVariables() {
      BaseResponse<List<Map<String, Object>>> retObj = new BaseResponse<List<Map<String, Object>>>();
      
      List<Map<String, Object>> variableMaps = new ArrayList<Map<String, Object>>();
      List<DeviceVariable> variables = GlobalVariables.getDeviceVariables();
      for (DeviceVariable variable : variables) {
        if (variable.getCollectorDeviceSn() != null) {
          continue;
        }
        
        Map<String, Object> variableMap = createDeviceVarialInfoMap(variable);
        
        variableMaps.add(variableMap);
      } 
      
      retObj.setCode(Integer.valueOf(200));
      retObj.setCount(new Long(variableMaps.size()));
      retObj.setData(variableMaps);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "查询设备变量的信息", notes = "")
    @RequestMapping(value = {"/variables/{sn}"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<Map<String, Object>>> getDeviceVariable(@PathVariable("sn") String deviceVarSn) {
      BaseResponse<Map<String, Object>> retObj = new BaseResponse<Map<String, Object>>();
      
      DeviceVariable variable = GlobalVariables.getDeviceVariableBySn(deviceVarSn);
      
      Map<String, Object> variableMap = createDeviceVarialInfoMap(variable);
      
      retObj.setCode(Integer.valueOf(200));
      retObj.setData(variableMap);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "查询变量存储空间的使用情况", notes = "")
    @RequestMapping(value = {"/variables/space_info"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<Map<String, Integer>>> getVariablesSpaceInfo() {
      BaseResponse<Map<String, Integer>> retObj = new BaseResponse<Map<String, Integer>>();
      
      Map<String, Integer> spaceInfo = new HashMap<String, Integer>();
      spaceInfo.put("variables_count", Integer.valueOf(GlobalVariables.getDeviceVariables().size()));
      spaceInfo.put("memory_space_total", Integer.valueOf(1000000));
      
      int usedCount = 0;
      for (Integer key : GlobalVariables.MEMORY_TAKEN_MAP.keySet()) {
        if (((Boolean)GlobalVariables.MEMORY_TAKEN_MAP.get(key)).booleanValue() == true) {
          usedCount++;
        }
      } 
      spaceInfo.put("memory_space_used", Integer.valueOf(usedCount));
      
      retObj.setCode(Integer.valueOf(0));
      retObj.setData(spaceInfo);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
  
    
    @ApiOperation(value = "实时数据查询", notes = "实时数据查询")
    @RequestMapping(value = {"/variables/data/realtime"}, method = {RequestMethod.POST})
    public ResponseEntity<BaseResponse<List<Map<String, String>>>> getRealtimeDatas(@RequestBody Map<String, List<String>> info) {
      BaseResponse<List<Map<String, String>>> retObj = new BaseResponse<List<Map<String, String>>>();
      
      if (!info.containsKey("sns")) {
        retObj.setCode(Integer.valueOf(400));
        retObj.setMessage("参数错误");
        return new ResponseEntity(retObj, HttpStatus.OK);
      } 
      
      List<String> sns = (List)info.get("sns");
      Instant now = Instant.now();
      List<Map<String, String>> datas = new ArrayList<Map<String, String>>();
      ///所有变量
      for (DeviceVariable deviceVariable : GlobalVariables.getDeviceVariables()) {
        if (!sns.contains(deviceVariable.getSn())) {
          continue;
        }
        Map<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("varSn", deviceVariable.getSn());
        valueMap.put("data", deviceVariable.getRegisterValue());
        
        boolean toCheckTime = true;
        
        if (deviceVariable.getVarCode() != null && GlobalVariables.GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.contains(deviceVariable.getVarCode())) {
          toCheckTime = false;
        }

        if (toCheckTime) {
          Instant variableUpdateInstant = deviceVariable.getUpdateTime().toInstant();
          
          if (Duration.between(variableUpdateInstant, now).getSeconds() > (this.expireTime * 60)) {
            valueMap.put("data", null);
          }
        } 
        
        datas.add(valueMap);
      } 
      
      retObj.setCode(Integer.valueOf(200));
      retObj.setCount(new Long(datas.size()));
      retObj.setData(datas);
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "修改数据", notes = "修改数据")
    @RequestMapping(value = {"/variables/action"}, method = {RequestMethod.POST})
    public ResponseEntity<BaseResponse<String>> changeValue(@RequestBody Map<String, Object> info) {
      BaseResponse retObj = new BaseResponse();
      String actionType = (String)info.get("type");
      
      if (actionType.equals("yaokong-act")) {
        List<Map<String, String>> details = (List)info.get("detail");
        
        for (Map<String, String> detail : details) {
          String deviceVariableSn = (String)detail.get("varsn");
          String deviceVariableValue = (String)detail.get("value");
          
          if (deviceVariableSn == null || deviceVariableSn.isEmpty() || deviceVariableValue == null || deviceVariableValue
            .isEmpty()) {
            retObj.setCode(Integer.valueOf(400));
            retObj.setData("参数格式错误");
            return new ResponseEntity(retObj, HttpStatus.OK);
          } 
          
          DeviceVariable deviceVariable = GlobalVariables.getDeviceVariableBySn(deviceVariableSn);
          if (deviceVariable == null) {
            retObj.setCode(Integer.valueOf(400));
            retObj.setData("找不到sn = " + deviceVariableSn + "的变量");
            return new ResponseEntity(retObj, HttpStatus.OK);
          } 
          
          BaseBusinessDevice device = null;
          for (BaseBusinessDevice tmpDevice : GlobalVariables.GLOBAL_BASE_BUSINESS_DEVICES) {
            if (tmpDevice.getSn().equals(deviceVariable.getCollectorDeviceSn())) {
              device = tmpDevice;
            }
          } 
          
          if (device == null) {
            retObj.setCode(Integer.valueOf(400));
            retObj.setData("找不到变量" + deviceVariableSn + "所在的设备");
            return new ResponseEntity(retObj, HttpStatus.OK);
          } 
  
          
          device.handleValueChangedMessage(deviceVariableSn, deviceVariableValue, actionType);
        } 
      } 
      
      retObj.setCode(Integer.valueOf(200));
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
  
  
    
    @ApiOperation(value = "查询告警类别的信息", notes = "")
    @RequestMapping(value = {"/eventtypes/{parent_index}/{sn}"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<EventType>> getEventTypes(@PathVariable("parent_index") String parent_index, @PathVariable("sn") String sn) {
      BaseResponse res = new BaseResponse();
      res.setData(this.eventTypesService.getEventType(parent_index, sn));
      return new ResponseEntity(res, HttpStatus.OK);
    }
  
    
    @ApiOperation(value = "查看历史日志文件列表", notes = "查看历史日志文件列表")
    @RequestMapping(value = {"/logs"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse<List<Map<String, String>>>> getLogsInfo(@RequestParam(value = "parent", required = false) String parent) throws IOException {
      BaseResponse<List<Map<String, String>>> retObj = new BaseResponse<List<Map<String, String>>>();
      retObj.setCode(Integer.valueOf(200));
      
      List<Map<String, String>> fileMapList = new ArrayList<Map<String, String>>();
      String logPath = (parent == null) ? "/logs" : ("/logs/" + parent);
      File path = new File(ResourceUtils.getURL("").getPath() + logPath);
      File[] fs = path.listFiles();
      for (File f : fs) {
        Map<String, String> fileMap = new HashMap<String, String>();
        fileMap.put("name", f.getName());
        if (f.isDirectory()) {
          fileMap.put("type", "dir");
        } else {
          fileMap.put("type", "file");
        } 
        
        fileMapList.add(fileMap);
      } 
      retObj.setCount(new Long(fileMapList.size()));
      retObj.setData(fileMapList);
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
  
    
    @ApiOperation(value = "日志文件下载", notes = "日志文件下载")
    @RequestMapping(value = {"/fileDownload"}, method = {RequestMethod.GET})
    public void downloadFile(@RequestParam("fileName") String filePath, HttpServletResponse response) throws IOException {
      if (filePath != null) {
        
        String rootPath = ResourceUtils.getURL("").getPath();
        String fullPath = rootPath + "logs/" + filePath;
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        File file = new File(fullPath);
        if (file.exists()) {
          response.setContentType("application/force-download");
          response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
          byte[] buffer = new byte[1024];
          //没有类型
          FileInputStream fis = null;
        //没有类型
          BufferedInputStream bis = null;
          try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            ServletOutputStream servletOutputStream = response.getOutputStream();
            int i = bis.read(buffer);
            while (i != -1) {
              servletOutputStream.write(buffer, 0, i);
              i = bis.read(buffer);
            } 
            System.out.println("success");
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if (bis != null) {
              try {
                bis.close();
              } catch (IOException e) {
                e.printStackTrace();
              } 
            }
            if (fis != null) {
              try {
                fis.close();
              } catch (IOException e) {
                e.printStackTrace();
              } 
            }
          } 
        } 
      } 
    }
    
    @ApiOperation(value = "查询平均解析时间", notes = "")
    @RequestMapping(value = {"/system/average_parsing_time"}, method = {RequestMethod.GET})
    public ResponseEntity<BaseResponse> getSystemAverageParsingTime() {
      BaseResponse<Map<String, String>> retObj = new BaseResponse<Map<String, String>>();
      retObj.setCode(Integer.valueOf(200));
      
      if (GlobalVariables.TOTAL_PARSING_COUNT == 0L) {
        retObj.setCount(Long.valueOf(0L));
      } else {
        retObj.setCount(Long.valueOf(GlobalVariables.TOTAL_PARSING_TIME / GlobalVariables.TOTAL_PARSING_COUNT));
      } 
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    @ApiOperation(value = "配置更新测试接口", notes = "")
    @RequestMapping(value = {"/system/configuration"}, method = {RequestMethod.POST})
    public ResponseEntity<BaseResponse> updateConfiguration(@RequestBody String configInfo) {
      BaseResponse retObj = new BaseResponse();
      retObj.setCode(Integer.valueOf(200));
      
      if (configInfo != null && !configInfo.isEmpty()) {
        this.receiver.receiveMessage(configInfo);
      }
      
      return new ResponseEntity(retObj, HttpStatus.OK);
    }
    
    private Map<String, Object> createDeviceVarialInfoMap(DeviceVariable variable) {
      Map<String, Object> variableMap = new LinkedHashMap<String, Object>();
      variableMap.put("variable_code", variable.getSn());
      variableMap.put("variable_type", variable.getType());
      variableMap.put("collector_device_sn", variable.getCollectorDeviceSn());
      variableMap.put("variable_register_name", variable.getRegisterName());
      variableMap.put("variable_register_idx", variable.getRegisterIndex());
      variableMap.put("variable_register_type", variable.getRegisterType());
      variableMap.put("variable_register_type_idx", variable.getRegisterTypeIndex());
      variableMap.put("varialbe_pos_in_mem", Integer.valueOf(variable.getPosInMemory()));
      variableMap.put("variable_value", variable.getRegisterValue());
      variableMap.put("variable_max_value", Float.valueOf(variable.getMaxValue()));
      variableMap.put("variable_max_value_time", (variable.getMaxValueTime() == null) ? null : 
          TimeFormatter.timestampToCommonString(variable.getMaxValueTime()));
      variableMap.put("variable_min_value", Float.valueOf(variable.getMinValue()));
      variableMap.put("variable_min_value_time", (variable.getMinValueTime() == null) ? null : 
          TimeFormatter.timestampToCommonString(variable.getMinValueTime()));
      variableMap.put("variable_sum", Float.valueOf(variable.getSum()));
      variableMap.put("variable_count", Integer.valueOf(variable.getCount()));
      StringBuilder cachedValueSB = new StringBuilder();
      StringBuilder cachedValueTimeSB = new StringBuilder();
      if (variable.getCatchedValues() != null && !variable.getCatchedValues().isEmpty()) {
        for (Pair<Instant, Float> pair : variable.getCatchedValues()) {
          cachedValueSB.append(pair.getValue1());
          cachedValueSB.append(", ");
          cachedValueTimeSB.append(TimeFormatter.timestampToCommonString(Timestamp.from((Instant)pair.getValue0())));
          cachedValueTimeSB.append(", ");
        } 
      }
      
      variableMap.put("variable_cached_value", cachedValueSB.toString());
      variableMap.put("variable_cached_valule_time", cachedValueTimeSB.toString());
      variableMap.put("variable_accumulation", variable.getAccumulation());
      variableMap.put("variable_accumulation_base", Float.valueOf(variable.getAccumulationBase()));
      variableMap.put("variable_expression", variable.getExpression());
      variableMap.put("coefficient", variable.getCoefficient());
      variableMap.put("base_value", variable.getBaseValue());
      variableMap.put("is_opposit", variable.getIsOpposit());
      variableMap.put("variable_update_time", (variable.getUpdateTime() == null) ? null : 
          TimeFormatter.timestampToCommonString(variable.getUpdateTime()));
      
      variableMap.put("event_triggers", variable.getEventTriggers());
      
      return variableMap;
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\api\SystemController.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */