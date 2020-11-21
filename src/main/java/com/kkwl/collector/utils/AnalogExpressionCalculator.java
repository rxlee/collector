  package  com.kkwl.collector.utils;
  
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.common.StatusType;
  import com.kkwl.collector.devices.business.BaseBusinessDevice;
  import com.kkwl.collector.exception.IllegalExpressionException;
  import com.kkwl.collector.models.DeviceVariable;
  import com.kkwl.collector.utils.AnalogExpressionCalculator;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Stack;
  import java.util.StringTokenizer;
  import java.util.regex.Pattern;
  
  //模拟表达式计算器
  public class AnalogExpressionCalculator
  {
    private String expression;
    private Float result;
    //栈操作
    private Stack<String> stackOperator;
    //植入的表达式
    private ArrayList<String> infixExpression;
    
    private ArrayList<String> postfixExpression; 
    private Stack<String> stackOperation; private int open;
    private List<String> opList;
    
    
    public AnalogExpressionCalculator(String expression) {
      this.expression = null;
      
      this.stackOperator = new Stack();
      this.infixExpression = new ArrayList();
      this.postfixExpression = new ArrayList();
      this.stackOperation = new Stack();
      this.open = 0;
      this.opList = new ArrayList();
 
      this.expression = expression.replace(" ", "");
      this.opList.add("+");
      this.opList.add("-");
      this.opList.add("*");
      this.opList.add("/");
      this.opList.add("(");
      this.opList.add(")");
      this.opList.add("tan");
      this.opList.add("sin");
      this.opList.add("cos");
      this.opList.add("log");
      this.opList.add("square");
      this.opList.add("sqrt");
      
      //字符串分隔工具--将表达式以+，-，*，/或()分隔， 参数true 意为输出保留分隔的字段 即+-*/()
      StringTokenizer stringTokenizer = new StringTokenizer(this.expression, "+-*/()", true);
      while (stringTokenizer.hasMoreTokens())
        this.infixExpression.add(stringTokenizer.nextToken()); 
    }
   
    public boolean infixExpressionToPostFixExpression(long calcTime) throws IllegalExpressionException {
      for (String token : this.infixExpression) {
        
        if (isOperator(token)) {
          operatorToStack(token); continue;
        }  if (isFloat(token)) {
          this.postfixExpression.add(token); continue;
        } 
        if (GlobalVariables.getDeviceVariableBySn(token) == null) {
          throw new IllegalExpressionException("变量" + token + "不存在.");
        }
        DeviceVariable deviceVariable = GlobalVariables.getDeviceVariableBySn(token);
        
        if (deviceVariable.isInitial())
        {
          return false;
        }
        
        if (deviceVariable.getVarCode() == null || !GlobalVariables.GLOBAL_VAR_CODES_SKIP_NEW_VALUE_CHECK.contains(deviceVariable.getVarCode()))
        {
          if (deviceVariable.getCollectorDeviceSn() != null && ((BaseBusinessDevice)GlobalVariables.GLOBAL_BUSINESS_DEVICE_MAP
            .get(deviceVariable.getCollectorDeviceSn())).getStatus() == StatusType.OFFLINE) {
            return false;
          }
        }
        
        String value = deviceVariable.getRegisterValue();
        
        if (value == null)
        {
          return false;
        }
        
        this.postfixExpression.add(value);
      } 
  
  
  
      
      while (!this.stackOperator.isEmpty()) {
        this.postfixExpression.add(this.stackOperator.pop());
      }
      
      this.expression = String.join("", this.postfixExpression);
      return true;
    }
  
    
    public void doParseAndCalculate() throws IllegalExpressionException {
      int open2 = 0;
      for (String str : this.postfixExpression) {
        if (str.equals("(") || str.equals(")")) {
          open2 = 1; continue;
        }  if (!isOperator(str)) {
          this.stackOperation.push(str); continue;
        }  if ("tansincoslogsquaresqrt".indexOf(str) >= 0) {
          this.stackOperation.push(String.valueOf(operation1((String)this.stackOperation.pop(), str)));
          continue;
        } 
        this.stackOperation.push(String.valueOf(operation2((String)this.stackOperation.pop(), (String)this.stackOperation.pop(), str)));
      } 
      
      if (this.open == 1 || open2 == 1) {
        this.open = 0;
        throw new IllegalExpressionException("表达式格式错误");
      }  if (this.open == 2) {
        this.open = 0;
        throw new IllegalExpressionException("表达式格式错误");
      } 
      this.result = Float.valueOf((String)this.stackOperation.pop());
    }
  
    
    public String getExpression() { return this.expression; }
  
  
    
    public Float getResult() { return this.result; }
  
  
    
    private boolean isOperator(String str) {
      if (this.opList.contains(str)) {
        return true;
      }
      return false;
    }
    
    private boolean isFloat(String str) {
      Pattern pattern = Pattern.compile("^[-\\+]?[\\d|\\.]*$");
      return pattern.matcher(str).matches();
    } private int getPriority(String str) {
      int a;
//      int a;
//      int a;
//      int a;
      switch (str) {
        case "+":
        case "-":
          return 1;
        
        case "*":
        case "/":
          return 2;
        
        case "(":
          return 4;
        
        case ")":
          return 0;
        
        case "tan":
        case "sin":
        case "cos":
        case "log":
        case "square":
        case "sqrt":
          return 3;
      } 
      
      return -1;
    }
  
  
  
  
  
    
    private boolean comparePriority(String str1, String str2) { return (getPriority(str1) > getPriority(str2)); }
  
  
    
    private void operatorToStack(String str) {
      if (this.stackOperator.isEmpty()) {
        this.stackOperator.push(str);
      } else if ("(".equals(str)) {
        this.stackOperator.push(str);
      } else if (")".equals(str)) {
        String string;
        
        while (!"(".equals(string = (String)this.stackOperator.pop()))
          this.postfixExpression.add(string); 
      } else if ("(".equals(this.stackOperator.peek())) {
        this.stackOperator.push(str);
      } else if (comparePriority(str, (String)this.stackOperator.peek())) {
        this.stackOperator.push(str);
      } else if (!comparePriority(str, (String)this.stackOperator.peek())) {
        
        this.postfixExpression.add(this.stackOperator.pop());
        operatorToStack(str);
      } 
    }
  
  
    //双目运算
    private double operation2(String str1, String str2, String str3) {
      double num2 = Double.valueOf(str1).doubleValue();
      double num1 = Double.valueOf(str2).doubleValue();
      
      if (str3.equals("+"))
        return num1 + num2; 
      if (str3.equals("-"))
        return num1 - num2; 
      if (str3.equals("*")) {
        return num1 * num2;
      }
      if (num2 == 0.0D)
        this.open = 1; 
      return num1 / num2;
    }
  
  
  
    //单目运算
    private double operation1(String str1, String str2) {
      double num1 = Double.valueOf(str1).doubleValue();
      
      if (str2.equals("tan")) {
        if (num1 == 90.0D)
          this.open = 2; 
        return 1.0D;
      }  if (str2.equals("sin"))
        return Math.sin(num1); 
      if (str2.equals("cos")) {
        return Math.cos(num1);
      }
      if (str2.equals("log")) {
        return Math.log(num1);
      }
      if (str2.equals("square")){
        return num1*num1;
      }
      return Math.sqrt(num1);
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collecto\\utils\AnalogExpressionCalculator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */