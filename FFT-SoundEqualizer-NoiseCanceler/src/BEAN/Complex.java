package BEAN;

public class Complex {
    private final double real;
    private final double imag;

    public Complex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    public double getReal() { return real; }
    public double getImag() { return imag; }

    public Complex add(Complex b) {
        return new Complex(this.real + b.real, this.imag + b.imag);
    }

    public Complex subtract(Complex b) {
        return new Complex(this.real - b.real, this.imag - b.imag);
    }

    public Complex multiply(Complex b) {
        return new Complex(this.real * b.real - this.imag * b.imag,
                           this.real * b.imag + this.imag * b.real);
    }
    
    public double abs() {
        return Math.sqrt(real * real + imag * imag);
    }
    
    public double getPhase() {
        return Math.atan2(imag, real);
    }
}
