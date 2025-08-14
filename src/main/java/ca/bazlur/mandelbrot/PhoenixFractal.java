package ca.bazlur.mandelbrot;

/**
 * Implementation of the Phoenix fractal
 * z(n+1) = z(n)^2 + c + p*z(n-1)
 */
public class PhoenixFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    private final double p; // Phoenix parameter
    
    public PhoenixFractal() {
        this.p = 0.56667; // Default interesting value
    }
    
    public PhoenixFractal(double p) {
        this.p = p;
    }
    
    @Override
    public int calculateIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        ComplexNumber zPrev = new ComplexNumber(0, 0);
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            ComplexNumber zNext = z.square().add(c).add(
                new ComplexNumber(p * zPrev.real(), p * zPrev.imaginary())
            );
            zPrev = z;
            z = zNext;
            iterations++;
        }
        
        return iterations;
    }
    
    @Override
    public String getName() {
        return String.format("Phoenix (p=%.3f)", p);
    }
    
    @Override
    public String getDescription() {
        return String.format("Phoenix fractal: z(n+1) = z(n)² + c + %.3f*z(n-1)", p);
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(0, 0);
    }
}