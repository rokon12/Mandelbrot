package ca.bazlur.mandelbrot;

record ComplexNumber(double real, double imaginary) {

    public double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }

    public ComplexNumber square() {
        double newReal = real * real - imaginary * imaginary;
        double newImaginary = 2 * real * imaginary;
        return new ComplexNumber(newReal, newImaginary);
    }

    public ComplexNumber add(ComplexNumber other) {
        double newReal = this.real + other.real;
        double newImaginary = this.imaginary + other.imaginary;
        return new ComplexNumber(newReal, newImaginary);
    }
}
