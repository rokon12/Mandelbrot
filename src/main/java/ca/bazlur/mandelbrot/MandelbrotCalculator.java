package ca.bazlur.mandelbrot;

import java.util.ArrayList;
import java.util.concurrent.*;

public class MandelbrotCalculator {
    int numThreads;
    ExecutorService executor;
    {
        numThreads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations) {
        int[][] iterations = new int[width][height];

        // Divide image into sections (adjust as needed)
        int rowsPerThread = height / numThreads;

        var futures = new ArrayList<Future<Void>>();
        for (int i = 0; i < numThreads; i++) {
            int startRow = i * rowsPerThread;
            int endRow = (i == numThreads - 1) ? height : startRow + rowsPerThread;
            Future<Void> future = executor.submit(new MandelbrotTask(iterations, startRow, endRow, width, height, centerX, centerY, zoom, maxIterations));
            futures.add(future);
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return iterations;
    }

    private int calculatePointIterations(ComplexNumber c, int maxIterations) {
        ComplexNumber z = new ComplexNumber(0, 0);
        int iterations = 0;

        while (z.magnitudeSquared() < 4 && iterations < maxIterations) {
            z = z.square().add(c);
            iterations++;
        }

        return iterations;
    }

    private ComplexNumber screenToComplex(int x, int y, int width, int height, double centerX, double centerY, double zoom) {
        double real = (x - width / 2.0) / zoom + centerX;
        double imaginary = (y - height / 2.0) / zoom + centerY;
        return new ComplexNumber(real, imaginary);
    }

    class MandelbrotTask implements Callable<Void> {
        private final int[][] iterations;
        private final int startRow;
        private final int endRow;
        private final int width;
        private final int height;
        private final double centerX;
        private final double centerY;
        private final double zoom;
        private final int maxIterations;

        public MandelbrotTask(int[][] iterations, int startRow, int endRow, int width, int height,
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
        public Void call() throws Exception {
            for (int x = 0; x < width; x++) {
                for (int y = startRow; y < endRow; y++) {
                    ComplexNumber c = screenToComplex(x, y, width, height, centerX, centerY, zoom);
                    iterations[x][y] = calculatePointIterations(c, maxIterations);
                }
            }

            return null;
        }
    }
}

