package ca.bazlur.mandelbrot;

/**
 * Interface for different fractal types
 */
public interface Fractal {
    
    /**
     * Calculate the number of iterations for a given point
     * @param c The complex number to test
     * @param maxIterations Maximum number of iterations
     * @return The number of iterations before escaping
     */
    int calculateIterations(ComplexNumber c, int maxIterations);
    
    /**
     * Get the name of the fractal
     * @return The display name
     */
    String getName();
    
    /**
     * Get a description of the fractal
     * @return Description text
     */
    String getDescription();
    
    /**
     * Get the default center point for this fractal
     * @return Complex number representing the center
     */
    default ComplexNumber getDefaultCenter() {
        return new ComplexNumber(-0.5, 0);
    }
    
    /**
     * Get the default zoom level for this fractal
     * @return The initial zoom level
     */
    default double getDefaultZoom() {
        return 200;
    }
    
    /**
     * Check if this fractal requires a parameter (like Julia sets)
     * @return true if parameter is needed
     */
    default boolean requiresParameter() {
        return false;
    }
    
    /**
     * Set the parameter for fractals that need it (like Julia sets)
     * @param parameter The complex parameter
     */
    default void setParameter(ComplexNumber parameter) {
        // Default implementation does nothing
    }
}