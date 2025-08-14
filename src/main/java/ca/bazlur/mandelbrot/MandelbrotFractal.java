package ca.bazlur.mandelbrot;

/**
 * Implementation of the Mandelbrot set fractal
 * z(n+1) = z(n)^2 + c, starting with z(0) = 0
 */
public class MandelbrotFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    
    @Override
    public int calculateIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            z = z.square().add(c);
            iterations++;
        }
        
        return iterations;
    }
    
    @Override
    public String getName() {
        return "Mandelbrot Set";
    }
    
    @Override
    public String getDescription() {
        return "The classic Mandelbrot set: z(n+1) = z(n)² + c";
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(-0.5, 0);
    }
}