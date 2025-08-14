package ca.bazlur.mandelbrot;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MandelbrotCalculatorFJ implements MandelbrotCalculatorStrategy {
    private final ForkJoinPool forkJoinPool;
    private static final int THRESHOLD = 50; // Rows per task threshold
    
    public MandelbrotCalculatorFJ() {
        this.forkJoinPool = new ForkJoinPool();
    }
    
    public MandelbrotCalculatorFJ(int parallelism) {
        this.forkJoinPool = new ForkJoinPool(parallelism);
    }

    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations) {
        return calculateIterations(width, height, centerX, centerY, zoom, maxIterations, new MandelbrotFractal());
    }
    
    @Override
    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations, Fractal fractal) {
        int[][] iterations = new int[width][height];
        
        MandelbrotTask task = new MandelbrotTask(iterations, 0, height, width, height, centerX, centerY, zoom, maxIterations, fractal);
        forkJoinPool.invoke(task);
        
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
        forkJoinPool.shutdown();
    }

    static class MandelbrotTask extends RecursiveAction {
        private final int[][] iterations;
        private final int startRow;
        private final int endRow;
        private final int width;
        private final int height;
        private final double centerX;
        private final double centerY;
        private final double zoom;
        private final int maxIterations;
        private final Fractal fractal;

        MandelbrotTask(int[][] iterations, int startRow, int endRow, int width, int height,
                       double centerX, double centerY, double zoom, int maxIterations, Fractal fractal) {
            this.iterations = iterations;
            this.startRow = startRow;
            this.endRow = endRow;
            this.width = width;
            this.height = height;
            this.centerX = centerX;
            this.centerY = centerY;
            this.zoom = zoom;
            this.maxIterations = maxIterations;
            this.fractal = fractal;
        }

        @Override
        protected void compute() {
            if (endRow - startRow <= THRESHOLD) {
                // Base case: compute directly
                computeDirectly();
            } else {
                // Fork into subtasks
                int midRow = startRow + (endRow - startRow) / 2;
                
                MandelbrotTask topTask = new MandelbrotTask(
                    iterations, startRow, midRow, width, height, 
                    centerX, centerY, zoom, maxIterations, fractal
                );
                
                MandelbrotTask bottomTask = new MandelbrotTask(
                    iterations, midRow, endRow, width, height, 
                    centerX, centerY, zoom, maxIterations, fractal
                );
                
                // Fork both tasks and join
                invokeAll(topTask, bottomTask);
            }
        }
        
        private void computeDirectly() {
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    ComplexNumber c = screenToComplex(x, y, width, height, centerX, centerY, zoom);
                    iterations[x][y] = calculatePointIterations(c, maxIterations, fractal);
                }
            }
        }
    }
}