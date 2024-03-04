package ca.bazlur.mandelbrot;

public class MandelbrotCalculator {
    public int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations) {
        int[][] iterations = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ComplexNumber c = screenToComplex(x, y, width, height, centerX, centerY, zoom);
                iterations[x][y] = calculatePointIterations(c, maxIterations);
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
}

