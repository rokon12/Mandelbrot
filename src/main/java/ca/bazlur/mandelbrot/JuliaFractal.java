package ca.bazlur.mandelbrot;

/**
 * Implementation of Julia set fractals
 * z(n+1) = z(n)^2 + c, where c is a fixed parameter and z(0) is the point being tested
 */
public class JuliaFractal implements Fractal {
    private static final double ESCAPE_RADIUS_SQUARED = 256.0;
    private ComplexNumber parameter;
    
    // Some interesting Julia set parameters
    public static final ComplexNumber DENDRITE = new ComplexNumber(0, 1);
    public static final ComplexNumber RABBIT = new ComplexNumber(-0.123, 0.745);
    public static final ComplexNumber DRAGON = new ComplexNumber(-0.8, 0.156);
    public static final ComplexNumber SPIRAL = new ComplexNumber(0.285, 0.01);
    public static final ComplexNumber FEATHER = new ComplexNumber(-0.4, 0.6);
    public static final ComplexNumber SAN_MARCO = new ComplexNumber(-0.75, 0);
    public static final ComplexNumber SIEGEL_DISK = new ComplexNumber(-0.391, -0.587);
    
    public JuliaFractal() {
        // Default to an interesting Julia set
        this.parameter = DRAGON;
    }
    
    public JuliaFractal(ComplexNumber parameter) {
        this.parameter = parameter;
    }
    
    public JuliaFractal(double real, double imaginary) {
        this.parameter = new ComplexNumber(real, imaginary);
    }
    
    @Override
    public int calculateIterations(ComplexNumber point, int maxIterations) {
        ComplexNumber z = point; // Start with the point itself for Julia sets
        int iterations = 0;
        
        while (z.magnitudeSquared() < ESCAPE_RADIUS_SQUARED && iterations < maxIterations) {
            z = z.square().add(parameter);
            iterations++;
        }
        
        return iterations;
    }
    
    @Override
    public String getName() {
        return String.format("Julia Set (c = %.3f + %.3fi)", 
                             parameter.real(), parameter.imaginary());
    }
    
    @Override
    public String getDescription() {
        return "Julia set: z(n+1) = z(n)² + c, where c = " + 
               String.format("%.3f + %.3fi", parameter.real(), parameter.imaginary());
    }
    
    @Override
    public ComplexNumber getDefaultCenter() {
        return new ComplexNumber(0, 0);
    }
    
    @Override
    public double getDefaultZoom() {
        return 150;
    }
    
    @Override
    public boolean requiresParameter() {
        return true;
    }
    
    @Override
    public void setParameter(ComplexNumber parameter) {
        this.parameter = parameter;
    }
    
    public ComplexNumber getParameter() {
        return parameter;
    }
}