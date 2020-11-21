 package  com.kkwl.collector.common;
 import com.kkwl.collector.common.FrameType;
 ///帧类型
 public  enum FrameType {
    IEC104_CONNECT_FRAME("iec104_connect_frame"), ///连接帧
    IEC104_CONNECT_ACK_FRAME("iec104_connect_ack_frame"), ///连接确认帧
    IEC104_INITIALIZATION_COMPLETE_FRAME("iec104_initialization_complete_frame"), ///初始化完成帧
    IEC104_S_ACK_FRAME("iec104_s_ack_frame"), ///S确认帧
    IEC104_RECRUITMENT_FRAME("iec104_recruit_frame"), ///总召帧
    IEC104_RECRUITMENT_ACK_FRAME("iec104_recruit_ack_frame"), ///总召确认帧
    IEC104_RECRUITMENT_COMPLETE_FRAME("iec104_recruit_complete_frame"), ///总召完成帧
    IEC104_YX_I_FRAME("iec104_yx_i_frame"), ///遥信 I帧
    IEC104_YC_I_FRAME("iec104_yc_i_frame"), ///遥测 I帧
    IEC104_TEST_ACK_FRAME("iec104_test_ack_frame"), ///测试确认帧
    IEC104_TEST_FRAME("iec104_test_frame"),  ///测试帧
    IEC104_CORRECT_TIME_FRAME("iec104_correct_time_frame"), ///正确时间帧
    IEC104_CORRECT_TIME_ACK_FRAME("iec104_correct_time_frame_ack"), ///正确时间确认帧
    IEC104_YK_SELECT_FRAME("iec104_yk_select_frame"), ///遥控选择帧
    IEC104_YK_SELECT_ACK_FRAME("iec104_yk_select_ack_frame"), ///遥控选择确认帧
    IEC104_YK_EXECUTE_FRAME("iec104_yk_execute_frame"), ///遥控执行帧
    IEC104_YK_EXECUTE_ACK_FRAME("iec104_yk_execute_ack_frame"), ///遥控执行确认帧
    IEC104_YK_EXECUTE_COMPLETE_FRAME("iec104_yk_execute_complete_frame"), ///遥控执行完成帧
    IEC104_SD_FRAME("iec104_sd_frame"),
    IEC104_SD_ACK_FRAME("iec104_sd_ack_frame"),
    IEC104_SOE_I_FRAME("iec104_soe_i_frame"),
    IEC104_DD_RECRUITMENT_FRAME("iec104_dd_recruit_frame"), ///电度召唤帧
    IEC104_DD_RECRUITMENT_ACK_FRAME("iec104_dd_recruit_ack_frame"), ///电度召唤确认帧
    IEC104_DD_RECRUITMENT_COMPLETE_FRAME("iec104_dd_recruit_complete_frame"), ///电度召唤完成帧
    IEC104_DD_I_FRAME("iec104_dd_i_frame"),  ///电度I帧
    IEC104_FAULT_I_FRAME("iec104_fault_i_frame"), ///默认I帧
    IEC104_HISTORY_RECRUITMENT_FRAME("iec104_history_recruitment_frame"), ///历史总召帧
    IEC104_HISTORY_RECRUITMENT_ACK_FRAME("iec104_history_recruitment_ack_frame"), ///历史总召确认帧
    IEC104_HISTORY_RECRUITMENT_COMPLETE_FRAME("iec104_history_recruitment_complete"), ///历史总召完成帧
    IEC104_HISTORY_YX_FRAME("iec104_history_yx_frame"), ///历史遥信帧
    IEC104_HISTORY_YC_FRAME("iec104_history_yc_frame"), ///历史遥测帧
    IEC104_HISTORY_DD_FRAME("iec104_history_dd_frame"), ///历史电度帧
 
   
    IEC101_REGISTER_ADDRESS_FRAME("iec101_register_address_frame"),
    IEC101_CONNECT_FRAME("iec101_connect_frame"),
    IEC101_CONNECT_ACK_FRAME("iec101_connect_frame"),
    IEC101_RESET_FRAME("iec101_reset_frame"),
    IEC101_RESET_ACK_FRAME("iec101_reset_frame"),
    IEC101_TEST_FRAME("iec101_test_frame"),
    IEC101_TEST_ACK_FRAME("iec101_test_ack_frame"),
    IEC101_RECRUITMENT_FRAME("iec101_recruitment_frame"),
    IEC101_RECRUITMENT_ACK_FRAME("iec101_recruitment_ack_frame"),
    IEC101_RECRUITMENT_COMPLETE_FRAME("iec101_recruitment_complete_frame"),
    IEC101_YX_FRAME_FRAME("iec101_yx_frame"),
    IEC101_YC_FRAME_FRAME("iec101_yc_frame"),
    IEC101_CORRECT_TIME_FRAME("iec101_correct_time_frame"),
    IEC101_CORRECT_TIME_ACK_FRAME("iec101_correct_time_ack_frame"),
    IEC101_SOE_FRAME("iec101_soe_frame"),
    IEC101_YK_SELECT_FRAME("iec101_yk_select_frame"),
    IEC101_YK_SELECT_ACK_FRAME("iec101_yk_select_ack_frame"),
    IEC101_YK_EXECUTE_FRAME("iec101_yk_execute_frame"),
    IEC101_YK_EXECUTE_ACK_FRAME("iec101_yk_execute_ack_frame"),
 
   
    MODBUS_DATA_FRAME("modbus_data_frame"),  ///modbus数据帧
    MODBUS_COMMAND_FRAME("modbus_command_frame"), ///modbus命令帧
    MODBUS_05_COMMAND_FRAME("modbus_05command_frame"), ///modbus05命令帧（写单个线圈）
    MODBUS_05_DATA_FRAME("modbus_05data_frame"), ///modbus05数据帧
    MODBUS_10_COMMAND_FrAME("modbus_10command_frame"), ///modbus10命令帧（写多个寄存器）
    MODBUS_10_DATA_FRAME("modbus_10data_frame"), ///modbus10数据帧
 
   
    MQTT_HGNY_METER_FRAME("mqtt_hgny_meter_frame"), //MQTT仪表帧
    MQTT_HGNY_LISTEN_FRAME("mqtt_hgny_listen_frame"),
    MQTT_HGNY_LOG_FRAME("mqtt_hgny_log_frame"),
    MQTT_HGNY_SCOPE_FRAME("mqtt_hgny_scope_frame");
   private String name;
   
    public String value() { return this.name; }
 
 
   
    FrameType(String name) { this.name = name; }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\FrameType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */