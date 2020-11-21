  package  com.kkwl.collector.utils;
  import com.kkwl.collector.utils.Complex;
  
  public class Complex {
    private double real;
    
    public Complex(double real, double image) {
      this.real = real;
      this.image = image;
    }
    private double image;
    public void Change(double a, double b) {
      this.real = a;
      this.image = b;
    }
    
    public String toString() {
      if (this.image > 0.0D) {
        if (this.real == 0.0D) {
          return this.image + "*i";
        }
        return this.real + "+" + this.image + "*i";
      }  if (this.image < 0.0D) {
        if (this.real == 0.0D) {
          return this.image + "*i";
        }
        return this.real + "" + this.image + "*i";
      } 
      return this.real + "";
    }
    
    public Complex add(Complex Z) {
      double aa = this.real + Z.real;
      double bb = this.image + Z.image;
      return new Complex(aa, bb);
    }
    
    public Complex subtract(Complex Z) {
      double aa = this.real - Z.real;
      double bb = this.image - Z.image;
      return new Complex(aa, bb);
    }
    
    public Complex multiply(Complex Z) {
      double aa = this.real * Z.real - this.image * Z.image;
      double bb = this.image * Z.real + this.real * Z.image;
      return new Complex(aa, bb);
    }
    
    public Complex divide(Complex Z) {
      Z.Change(Z.real, -Z.image);
      Complex ZZ = multiply(Z);
      ZZ.real /= (Z.real * Z.real + Z.image * Z.image);
      ZZ.image /= (Z.real * Z.real + Z.image * Z.image);
      return ZZ;
    }
    
    public Complex multiply(double d) {
      double aa = this.real * d;
      double bb = this.image * d;
      return new Complex(aa, bb);
    }
    
    public Complex divide(Double d) {
      double aa = this.real / d.doubleValue();
      double bb = this.image / d.doubleValue();
      return new Complex(aa, bb);
    }
  
    
    public double abs() { return Math.hypot(this.real, this.image); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collecto\\utils\Complex.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */