package ca.bazlur.mandelbrot;

public class SingleThreadedCalculator implements MandelbrotCalculatorStrategy {
    
    @Override
    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations) {
        return calculateIterations(width, height, centerX, centerY, zoom, maxIterations, new MandelbrotFractal());
    }
    
    @Override
    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations, Fractal fractal) {
        int[][] iterations = new int[width][height];
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ComplexNumber c = screenToComplex(x, y, width, height, centerX, centerY, zoom);
                iterations[x][y] = calculatePointIterations(c, maxIterations, fractal);
            }
        }
        
        return iterations;
    }
    
    private static int calculatePointIterations(ComplexNumber c, int maxIterations, Fractal fractal) {
        return fractal.calculateIterations(c, maxIterations);
    }
    
    private static ComplexNumber screenToComplex(int x, int y, int width, int height, double centerX, double centerY, double zoom) {
        double real = (x - width / 2.0) / zoom + centerX;
        double imaginary = (y - height / 2.0) / zoom + centerY;
        return new ComplexNumber(real, imaginary);
    }
    
    @Override
    public void close() {
        // No resources to clean up for single-threaded implementation
    }
}