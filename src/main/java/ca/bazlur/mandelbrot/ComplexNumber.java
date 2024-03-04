package ca.bazlur.mandelbrot;

class ComplexNumber {
    public double real;
    public double imaginary;

    // Constructor
    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    // Calculate magnitude squared (helps determine if within the Mandelbrot set)
    public double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }

    // Squaring a complex number
    public ComplexNumber square() {
        double newReal = real * real - imaginary * imaginary;
        double newImaginary = 2 * real * imaginary;
        return new ComplexNumber(newReal, newImaginary);
    }

    // Adding two complex numbers
    public ComplexNumber add(ComplexNumber other) {
        double newReal = this.real + other.real;
        double newImaginary = this.imaginary + other.imaginary;
        return new ComplexNumber(newReal, newImaginary);
    }
}
