package ca.bazlur.mandelbrot;

/**
 * Implementation of the Burning Ship fractal
 * z(n+1) = (|Re(z(n))| + i|Im(z(n))|)^2 + c
 */
public class BurningShipFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    
    @Override
    public int calculateIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            // Take absolute values of real and imaginary parts before squaring
            double absReal = Math.abs(z.real());
            double absImag = Math.abs(z.imaginary());
            ComplexNumber absZ = new ComplexNumber(absReal, absImag);
            z = absZ.square().add(c);
            iterations++;
        }
        
        return iterations;
    }
    
    @Override
    public String getName() {
        return "Burning Ship";
    }
    
    @Override
    public String getDescription() {
        return "Burning Ship fractal: z(n+1) = (|Re(z)| + i|Im(z)|)² + c";
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(-0.5, -0.5);
    }
    
    @Override
    public double getDefaultZoom() {
        return 150;
    }
}