 package  com.kkwl.collector.common;
 
 public enum DataType {
    COMMON_FLOAT("浮点数", 0, 0),
    BYTES_FLOAT_4_2143("4字节浮点2143", 1, 4),
    BYTES_FLOAT_4_4321("4字节浮点4321", 2, 4),
    BYTES_FLOAT_4_1234("4字节浮点1234", 3, 4),
    BYTES_FLOAT_4_3412("4字节浮点3412", 4, 4),
    BYTES_INT_2_HL("2字节整数高位在前", 5, 2),
    BYTES_INT_2_LH("2字节整数低位在前", 6, 2),
    BYTES_WORD_2("2字节无符号整数", 7, 2),
    BYTES_DWORD_4("4字节无符号整数", 8, 4),
    BYTES_INT_4("4字节有符号整数", 9, 4),
    BYTES_SHORT_2_SIGNAL("2字节整数最高位符号位", 10, 2),
    BYTES_INT_4_SIGNAL("4字节整数最高位符号位", 11, 4),
    BYTE_TYPE("字节类型", 12, 1),
    BIT_TYPE("比特位类型", 13, 1),
    BYTES_DWORD_4_1234("4字节无符号整数1234", 14, 4),
    BYTES_DWORD_4_4321("4字节无符号整数4321", 15, 4),
    BYTES_DWORD_4_2143("4字节无符号整数2143", 16, 4),
    BYTES_DWORD_4_3412("4字节无符号整数3412", 17, 4),
    BYTES_INT_4_1234("4字节有符号整数1234", 18, 4),
    BYTES_INT_4_4321("4字节有符号整数4321", 19, 4),
    BYTES_INT_4_2143("4字节有符号整数2143", 20, 4),
    BYTES_INT_4_3412("4字节有符号整数3412", 21, 4),
    BYTES_INT_4_SIGNAL_1234("4字节整数最高位符号位1234", 22, 4),
    BYTES_INT_4_SIGNAL_4321("4字节整数最高位符号位4321", 23, 4),
    BYTES_INT_4_SIGNAL_2143("4字节整数最高位符号位2143", 24, 4),
    BYTES_INT_4_SIGNAL_3412("4字节整数最高位符号位3412", 25, 4);
    private String typeName;
    private int type;
    private int size;
   
    public int valueInteger() { return this.type; }
 
 
   
    public String valueString() { return this.typeName; }
 
 
   
    public int getSize() { return this.size; }
 
   
   public static DataType getDataTypeByName(String name) {
      for (DataType d : values()) {
        if (d.typeName.equals(name)) {
          return d;
       }
     } 
     
      return null;
   }
   
   DataType(String typeNameEN, int type, int size) {
      this.typeName = typeNameEN;
      this.type = type;
      this.size = size;
   }
 }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\common\DataType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */