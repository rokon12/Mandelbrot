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
        int[][] iterations = new int[width][height];
        
        MandelbrotTask task = new MandelbrotTask(iterations, 0, height, width, height, centerX, centerY, zoom, maxIterations);
        forkJoinPool.invoke(task);
        
        return iterations;
    }

    private static int calculatePointIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;
        double escapeRadiusSquared = 256.0; // Larger escape radius for smoother coloring

        while (z.magnitudeSquared() < escapeRadiusSquared && iterations < maxIterations) {
            z = z.square().add(c);
            iterations++;
        }

        return iterations;
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

        MandelbrotTask(int[][] iterations, int startRow, int endRow, int width, int height,
                       double centerX, double centerY, double zoom, int maxIterations) {
            this.iterations = iterations;
            this.startRow = startRow;
            this.endRow = endRow;
            this.width = width;
            this.height = height;
            this.centerX = centerX;
            this.centerY = centerY;
            this.zoom = zoom;
            this.maxIterations = maxIterations;
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
                    centerX, centerY, zoom, maxIterations
                );
                
                MandelbrotTask bottomTask = new MandelbrotTask(
                    iterations, midRow, endRow, width, height, 
                    centerX, centerY, zoom, maxIterations
                );
                
                // Fork both tasks and join
                invokeAll(topTask, bottomTask);
            }
        }
        
        private void computeDirectly() {
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    ComplexNumber c = screenToComplex(x, y, width, height, centerX, centerY, zoom);
                    iterations[x][y] = calculatePointIterations(c, maxIterations);
                }
            }
        }
    }
}