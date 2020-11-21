  package  com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan;
  
  import com.kkwl.collector.common.FrameType;
  import com.kkwl.collector.protocols.BaseFrameParser;
  import com.kkwl.collector.protocols.Mqtt.HuaGongNengYuan.MeterFrameParser;
  import com.kkwl.collector.utils.Complex;
  import java.sql.Timestamp;
  import java.time.Instant;
  import java.util.HashMap;
  import java.util.Map;
  import org.json.JSONException;
  import org.json.JSONObject;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  public class MeterFrameParser extends BaseFrameParser {
    private static final Logger logger = LoggerFactory.getLogger(MeterFrameParser.class);
  
    
    public MeterFrameParser() { super(FrameType.MQTT_HGNY_METER_FRAME.value()); }
  
  
    //解析字符串帧
    public Map<String, Object> parseStringFrame(String message, Map<String, Object> params) {
      Map<String, Object> retMap = new HashMap<String, Object>();
      
      String deviceSn = (String)params.get("sn"); //实际为站点sn
      Double tc = (Double)params.get("tc"); //
      
      try {
        JSONObject messageJSON = new JSONObject(message);          //JSONObject messageJSON  "data_type":element
                                                                                      //     "meter_sn" :meterSn
        //无数据类型                                                                    //    "td_mctd"  :tdMctd              specialValueMap
        if (!messageJSON.has("data_type")) {                                      //     "data"    :JSONObject elementData ---    "mdp"     :value
                                                                                      //      "time"    :timeString                  "mdptime" :mdpTimeStr
          logger.warn("mqtt hgny element parser doesn't have data_type");                                           //               "mdkwh"  :value
                                                                                                                   //                "mdkwhtime":mdkTimeStr
          retMap.put("result", Boolean.valueOf(false));                                                           //
          return retMap;                                                                                          //
        }
                                                                                                                  //                 "anguab"  :
        String dataType = messageJSON.getString("data_type");                                                //                 "anguac"
         //数据类型不是element"                                                                                        //              "ub"
        if (!dataType.equals("element")) {
          
          retMap.put("result", Boolean.valueOf(false));                                                           //    commonValueMap
          return retMap;                                                                                          //          deviceSn + "__ublu1" :
        }                                                                                                         //          deviceSn + "__ublu2" :
         //无仪表sn                                                                                                //          deviceSn + "__lf"    :
        if (!messageJSON.has("meter_sn")) {
          logger.warn("mqtt hgny element parser doesn't have meter_sn");                                                        //   retMap
          retMap.put("result", Boolean.valueOf(false));                                                                         //      "commonValueMap":commonValueMap
          return retMap;                                                                                                        //      "specialValueMap":
        }                                                                                                                       //       "time"  :
                                                                                                                                //       "result" :
        String meterSn = messageJSON.getString("meter_sn");                                                                //       "type"   :
         //
        if (!messageJSON.has("td_mctd")) {
          logger.warn("mqtt hgny element parser doesn't have td_mctd");
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
        
        String tdMctd = messageJSON.getString("td_mctd");
         //无数据
        if (!messageJSON.has("data")) {
          logger.warn("mqtt hgny element parser data segment doesn't exist.");
          retMap.put("result", Boolean.valueOf(false));
          return retMap;
        } 
        
        Timestamp recordTime = null;
        if (!messageJSON.has("time")) {
          recordTime = Timestamp.from(Instant.now());
        } else {
          String timeString = messageJSON.getString("time").substring(0, 19);
          recordTime = Timestamp.valueOf(timeString);
        } 
  

        JSONObject elementData = messageJSON.getJSONObject("data"); //元素数据
        Map<String, Object> specialValueMap = new HashMap<String, Object>();
        //specialValueMap  "mdp" "mdp_time"
        if (elementData.has("mdp")) {
          Double value = Double.valueOf(elementData.getDouble("mdp")); //
          elementData.remove("mdp");
          
          if (value != null && elementData.has("mdptime")) {
            String mdpTimeStr = elementData.getString("mdptime");  //
            if (mdpTimeStr != null && !mdpTimeStr.isEmpty()) {
              Timestamp mdpTime = Timestamp.valueOf(mdpTimeStr);
              elementData.remove("mdptime");
              
              specialValueMap.put("mdp", value); //特殊值
              specialValueMap.put("mdp_time", mdpTime); //时间
            } 
          } 
        } 
        //specialValueMap  "mdkwh" "mdkwh_time"
        if (elementData.has("mdkwh")) {
          Double value = Double.valueOf(elementData.getDouble("mdkwh"));
          elementData.remove("mdkwh");
          
          if (value != null && elementData.has("mdkwhtime")) {
            String mdkTimeStr = elementData.getString("mdkwhtime");
            if (mdkTimeStr != null && !mdkTimeStr.isEmpty()) {
              Timestamp mdkwhTime = Timestamp.valueOf(mdkTimeStr);
              elementData.remove("mdkwhtime");
              
              specialValueMap.put("mdkwh", value); //
              specialValueMap.put("mdkwh_time", mdkwhTime);
            }
          } 
        } 
        
        Double anguab = null;
        Double anguac = null;
        Double ua = null;
        Double ub = null;
        Double uc = null;
        Double sttl = null;
        Map<String, Double> commonValueMap = new HashMap<String, Double>();
        for (String key : elementData.keySet()) {
          Double value = null;
          try {
            value = Double.valueOf(elementData.getDouble(key));
            
            if (key.equals("anguab")) {
              anguab = value;
            } else if (key.equals("anguac")) {
              anguac = value;
            } else if (key.equals("ua")) {
              ua = value;
            } else if (key.equals("ub")) {
              ub = value;
            } else if (key.equals("uc")) {
              uc = value;
            } else if (key.equals("sttl")) {
              sttl = value;
            } 
          } catch (JSONException ex) {
            logger.warn("mqtt hgny element parser error occured when get double value of " + key);
            
            continue;
          } 
          commonValueMap.put(deviceSn + "__" + key, value);
        } 
        
        Double[] ublArr = calUubl3(anguab, anguac, ua, ub, uc);
        commonValueMap.put(deviceSn + "__ublu1", ublArr[0]);
        commonValueMap.put(deviceSn + "__ublu2", ublArr[1]);
        
        Double lf = lf(sttl, tc);
        commonValueMap.put(deviceSn + "__lf", lf);
        
        retMap.put("commonValueMap", commonValueMap);
        retMap.put("specialValueMap", specialValueMap);
        retMap.put("time", recordTime);
        retMap.put("result", Boolean.valueOf(true));
        retMap.put("type", FrameType.MQTT_HGNY_METER_FRAME);
      } catch (JSONException ex) {
        logger.error("mqtt hgny element parser error occured when parse json data.", ex);
        retMap.put("result", Boolean.valueOf(false));
        return retMap;
      } 
      
      return retMap; //返回结果map
    }
  
  
    
    public Double[] calUubl3(Double anguab, Double anguac, Double ua, Double ub, Double uc) {
      if (null == anguab || null == anguac || null == ua || null == ub || null == uc) {
        return new Double[] { null, null };
      }
      
      double angua = 0.0D;
      double angub = (angua - anguab.doubleValue()) * Math.PI / 180.0D;
      double anguc = (angua - anguac.doubleValue()) * Math.PI / 180.0D;
      
      Complex vUa = (new Complex(Math.cos(angua), Math.sin(angua))).multiply(ua.doubleValue());
      Complex vUb = (new Complex(Math.cos(angub), Math.sin(angub))).multiply(ub.doubleValue());
      Complex vUc = (new Complex(Math.cos(anguc), Math.sin(anguc))).multiply(uc.doubleValue());
      
      Complex operator = new Complex(-0.5D, Math.sqrt(3.0D) / 2.0D);
      
      Complex vU1 = vUa.add(operator.multiply(vUb)).add(operator.multiply(operator).multiply(vUc)).divide(Double.valueOf(3.0D));
      Complex vU2 = vUa.add(operator.multiply(operator).multiply(vUb)).add(operator.multiply(vUc)).divide(Double.valueOf(3.0D));
      double u1 = vU1.abs();
      double u2 = vU2.abs();
//      return new Double[] { null, (new Double[2][0] = Double.valueOf(u1)).valueOf(u2) };
      //TODO 
      return new Double[] { Double.valueOf(u1),Double.valueOf(u2)};
    }
    
    public static Double lf(Double sttl, Double tc) {
      if (null == sttl || null == tc || tc.doubleValue() == 0.0D) {
        return null;
      }
      
      return Double.valueOf(sttl.doubleValue() / tc.doubleValue());
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\protocols\Mqtt\HuaGongNengYuan\MeterFrameParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */