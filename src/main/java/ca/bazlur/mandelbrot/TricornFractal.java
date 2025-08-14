package ca.bazlur.mandelbrot;

/**
 * Implementation of the Tricorn (Mandelbar) fractal
 * z(n+1) = conjugate(z(n))^2 + c
 */
public class TricornFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    
    @Override
    public int calculateIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            // Take conjugate before squaring
            ComplexNumber conjugate = new ComplexNumber(z.real(), -z.imaginary());
            z = conjugate.square().add(c);
            iterations++;
        }
        
        return iterations;
    }
    
    @Override
    public String getName() {
        return "Tricorn";
    }
    
    @Override
    public String getDescription() {
        return "Tricorn (Mandelbar) fractal: z(n+1) = conjugate(z(n))² + c";
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(0, 0);
    }
    
    @Override
    public double getDefaultZoom() {
        return 200;
    }
}