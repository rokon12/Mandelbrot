package ca.bazlur.mandelbrot;

import javafx.scene.paint.Color;

public class ColorPalette {
    private final Color[] colorGradient;

    public ColorPalette() {
        this.colorGradient = createGradient();
    }

    public Color getColorForIterations(int iterations, int maxIterations) {
        if (iterations == maxIterations) {
            return Color.BLACK; // Points inside the set typically get black
        }

        double index = (double) iterations / maxIterations * (colorGradient.length - 1);
        int colorIndex1 = (int) Math.floor(index);
        int colorIndex2 = Math.min(colorIndex1 + 1, colorGradient.length - 1);

        double fraction = index - colorIndex1; // For smooth interpolation

        return interpolateColors(colorGradient[colorIndex1], colorGradient[colorIndex2], fraction);
    }

    private Color[] createGradient() {
        int numColors = 256;
        Color[] gradient = new Color[numColors];

        int colorIndex = 0;
        double fraction = 0.0;

        while (colorIndex < numColors) {
            if (fraction < 0.3) {
                gradient[colorIndex] = interpolateColors(Color.BLUE, Color.CYAN, fraction / 0.3);
            } else if (fraction < 0.7) {
                gradient[colorIndex] = interpolateColors(Color.CYAN, Color.YELLOW, (fraction - 0.3) / 0.4);
            } else {
                gradient[colorIndex] = interpolateColors(Color.YELLOW, Color.RED, (fraction - 0.7) / 0.3);
            }
            fraction += 1.0 / numColors;
            colorIndex++;
        }

        return gradient;
    }

    private Color interpolateColors(Color color1, Color color2, double fraction) {
        double red = color1.getRed() * (1 - fraction) + color2.getRed() * fraction;
        double green = color1.getGreen() * (1 - fraction) + color2.getGreen() * fraction;
        double blue = color1.getBlue() * (1 - fraction) + color2.getBlue() * fraction;

        return Color.color(red, green, blue);
    }
}

