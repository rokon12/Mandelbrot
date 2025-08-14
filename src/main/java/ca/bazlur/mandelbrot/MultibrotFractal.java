package ca.bazlur.mandelbrot;

/**
 * Implementation of the Multibrot fractal
 * z(n+1) = z(n)^d + c, where d is the power (degree)
 */
public class MultibrotFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    private final double power;
    
    public MultibrotFractal(double power) {
        this.power = power;
    }
    
    @Override
    public int calculateIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            z = complexPower(z, power).add(c);
            iterations++;
        }
        
        return iterations;
    }
    
    /**
     * Calculate z^power for complex numbers
     */
    private ComplexNumber complexPower(ComplexNumber z, double power) {
        if (z.real() == 0 && z.imaginary() == 0) {
            return new ComplexNumber(0, 0);
        }
        
        double r = Math.sqrt(z.magnitudeSquared());
        double theta = Math.atan2(z.imaginary(), z.real());
        
        double newR = Math.pow(r, power);
        double newTheta = theta * power;
        
        return new ComplexNumber(
            newR * Math.cos(newTheta),
            newR * Math.sin(newTheta)
        );
    }
    
    @Override
    public String getName() {
        return String.format("Multibrot (d=%.1f)", power);
    }
    
    @Override
    public String getDescription() {
        return String.format("Multibrot fractal: z(n+1) = z(n)^%.1f + c", power);
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(0, 0);
    }
    
    @Override
    public double getDefaultZoom() {
        return 200 / Math.pow(power - 1, 0.5); // Adjust zoom based on power
    }
}