 package  com.kkwl.collector.common;
 
 import com.kkwl.collector.common.ParsingStatusType;
 ///解析状态类型: 解析头部、解析设备地址、解析回复类型、解析长度、解析内容
 public enum ParsingStatusType
 {
    PARSING_HEAD,
    PARSING_DEVICE_ADDRESS,
    PARSING_ANSWER_TYPE,
    PARSING_LENGTH,
    PARSING_BODY
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\ParsingStatusType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */