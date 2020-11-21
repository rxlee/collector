  package  com.kkwl.collector.utils;
  
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.common.StatusType;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.exception.IllegalExpressionException;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.utils.DigitalExpressionCalculator;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Stack;
  import java.util.StringTokenizer;
  
  
  
  
  public class DigitalExpressionCalculator
  {
    private String expression;
    private Byte result;
    List<String> postfixList;
    
    public DigitalExpressionCalculator(String expression) {
      this.expression = null;
  
  
  
      
      this.expression = expression;
      this.postfixList = new ArrayList();
    }
  
  
  
  
  
  
    
    public boolean infixExpressionToPostFixExpression(long calcTime) throws IllegalExpressionException {
      this.expression = this.expression.replace(" ", "");
      
      if (this.expression == null || this.expression.isEmpty()) {
        throw new IllegalExpressionException("空表达式");
      }
  
  
      
      Stack<String> operatorStack = new Stack<String>();
      StringTokenizer st = new StringTokenizer(this.expression, "&|!", true);
      while (st.hasMoreElements()) {
        String token = st.nextToken();
        if (token.equals("&") || token.equals("|")) {
          
          if (!operatorStack.empty()) {
            while (!operatorStack.empty()) {
              this.postfixList.add(operatorStack.pop());
            }
            operatorStack.add(token); continue;
          } 
          operatorStack.add(token); continue;
        } 
        if (token.equals("!")) {
          
          operatorStack.add(token);
          continue;
        } 
        if (GlobalVariables.getDeviceVariableBySn(token) == null) {
          throw new IllegalExpressionException("变量" + token + "不存在.");
        }
        DeviceVariable deviceVariable = GlobalVariables.getDeviceVariableBySn(token);
        
        if (deviceVariable.isInitial())
        {
          return false;
        }
  
        
        if (deviceVariable.getCollectorDeviceSn() != null && ((BaseBusinessDevice)GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP
          .get(deviceVariable.getCollectorDeviceSn())).getStatus() == StatusType.OFFLINE) {
          return false;
        }
        
        String value = deviceVariable.getRegisterValue();
        
        if (value == null)
        {
          return false;
        }
        
        this.postfixList.add(value);
      } 
  
  
  
      
      while (!operatorStack.empty()) {
        this.postfixList.add(operatorStack.pop());
      }
      
      this.expression = String.join("", this.postfixList);
      
      return true;
    }
  
    
    public void doParseAndCalculate() {
      Stack<Byte> operandStack = new Stack<Byte>();
      
      for (int i = 0; i < this.postfixList.size(); i++) {
        String a = (String)this.postfixList.get(i);
        if (a == null) {
          this.result = null;
          
          return;
        } 
        if (a.equals("&") || a.equals("|") || a.equals("!")) {
          if (a.equals("&")) {
            byte op1 = ((Byte)operandStack.pop()).byteValue();
            byte op2 = ((Byte)operandStack.pop()).byteValue();
//            byte tmpResult = (byte)(op1 & op2 & true);
            byte tmpResult = (byte)(op1 & op2 );
            operandStack.push(Byte.valueOf(tmpResult));
          } else if (a.equals("|")) {
            byte op1 = ((Byte)operandStack.pop()).byteValue();
            byte op2 = ((Byte)operandStack.pop()).byteValue();
//            byte tmpResult = (byte)((op1 | op2) & true);
            byte tmpResult = (byte)(op1 | op2);
            operandStack.push(Byte.valueOf(tmpResult));
          } else if (a.equals("!")) {
            byte op1 = ((Byte)operandStack.pop()).byteValue();
//            byte tmpResult = (byte)((op1 ^ 0xFFFFFFFF) & true);
            byte tmpResult = (byte)(op1 ^ 0xFFFFFFFF);
            operandStack.push(Byte.valueOf(tmpResult));
          } 
        } else {
          operandStack.push(Byte.valueOf(Byte.parseByte(a)));
        } 
      } 
      
      this.result = (Byte)operandStack.pop();
    }
  
    
    public String getExpression() { return this.expression; }
  
  
    
    public Byte getResult() { return this.result; }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collecto\\utils\DigitalExpressionCalculator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */