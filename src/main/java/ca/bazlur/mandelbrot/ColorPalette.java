package ca.bazlur.mandelbrot;

import javafx.scene.paint.Color;

public class ColorPalette {
  private final Color[] colorGradient;
  private final PaletteType type;

  public enum PaletteType {
    CLASSIC,
    SMOOTH,
    FIRE,
    OCEAN,
    RAINBOW,
    GRAYSCALE
  }

  public ColorPalette() {
    this(PaletteType.SMOOTH);
  }

  public ColorPalette(PaletteType type) {
    this.type = type;
    this.colorGradient = createGradient(type);
  }

  public Color getColorForIterations(int iterations, int maxIterations) {
    if (iterations == maxIterations) {
      return Color.BLACK; // Points inside the set
    }

    // Smooth coloring using normalized iteration count
    double smoothed = iterations + 1 - Math.log(Math.log(256)) / Math.log(2);
    smoothed = Math.max(0, Math.min(smoothed, maxIterations));

    double index = (smoothed / maxIterations) * (colorGradient.length - 1);
    int colorIndex1 = (int) Math.floor(index);
    int colorIndex2 = Math.min(colorIndex1 + 1, colorGradient.length - 1);

    double fraction = index - colorIndex1;

    return interpolateColors(colorGradient[colorIndex1], colorGradient[colorIndex2], fraction);
  }

  private Color[] createGradient(PaletteType type) {
    int numColors = 256;

    return switch (type) {
      case CLASSIC -> createClassicGradient(numColors);
      case SMOOTH -> createSmoothGradient(numColors);
      case FIRE -> createFireGradient(numColors);
      case OCEAN -> createOceanGradient(numColors);
      case RAINBOW -> createRainbowGradient(numColors);
      case GRAYSCALE -> createGrayscaleGradient(numColors);
    };
  }

  private Color[] createClassicGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    for (int i = 0; i < numColors; i++) {
      double fraction = (double) i / (numColors - 1);
      if (fraction < 0.3) {
        gradient[i] = interpolateColors(Color.BLUE, Color.CYAN, fraction / 0.3);
      } else if (fraction < 0.7) {
        gradient[i] = interpolateColors(Color.CYAN, Color.YELLOW, (fraction - 0.3) / 0.4);
      } else {
        gradient[i] = interpolateColors(Color.YELLOW, Color.RED, (fraction - 0.7) / 0.3);
      }
    }
    return gradient;
  }

  private Color[] createSmoothGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    Color[] keyColors = {
        Color.rgb(0, 7, 100),
        Color.rgb(32, 107, 203),
        Color.rgb(237, 255, 255),
        Color.rgb(255, 170, 0),
        Color.rgb(0, 2, 0)
    };

    for (int i = 0; i < numColors; i++) {
      double position = (double) i / (numColors - 1) * (keyColors.length - 1);
      int index = (int) position;
      double fraction = position - index;

      if (index >= keyColors.length - 1) {
        gradient[i] = keyColors[keyColors.length - 1];
      } else {
        gradient[i] = interpolateColors(keyColors[index], keyColors[index + 1], fraction);
      }
    }
    return gradient;
  }

  private Color[] createFireGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    for (int i = 0; i < numColors; i++) {
      double fraction = (double) i / (numColors - 1);
      double r = Math.min(1.0, fraction * 3);
      double g = Math.min(1.0, Math.max(0, (fraction - 0.33) * 3));
      double b = Math.min(1.0, Math.max(0, (fraction - 0.66) * 3));
      gradient[i] = Color.color(r, g, b);
    }
    return gradient;
  }

  private Color[] createOceanGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    Color[] keyColors = {
        Color.rgb(0, 0, 50),
        Color.rgb(0, 50, 150),
        Color.rgb(0, 100, 200),
        Color.rgb(100, 200, 255),
        Color.rgb(200, 255, 255)
    };

    for (int i = 0; i < numColors; i++) {
      double position = (double) i / (numColors - 1) * (keyColors.length - 1);
      int index = (int) position;
      double fraction = position - index;

      if (index >= keyColors.length - 1) {
        gradient[i] = keyColors[keyColors.length - 1];
      } else {
        gradient[i] = interpolateColors(keyColors[index], keyColors[index + 1], fraction);
      }
    }
    return gradient;
  }

  private Color[] createRainbowGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    for (int i = 0; i < numColors; i++) {
      double hue = (double) i / (numColors - 1) * 360;
      gradient[i] = Color.hsb(hue, 1.0, 1.0);
    }
    return gradient;
  }

  private Color[] createGrayscaleGradient(int numColors) {
    Color[] gradient = new Color[numColors];
    for (int i = 0; i < numColors; i++) {
      double gray = (double) i / (numColors - 1);
      gradient[i] = Color.gray(gray);
    }
    return gradient;
  }

  private Color interpolateColors(Color color1, Color color2, double fraction) {
    double red = color1.getRed() * (1 - fraction) + color2.getRed() * fraction;
    double green = color1.getGreen() * (1 - fraction) + color2.getGreen() * fraction;
    double blue = color1.getBlue() * (1 - fraction) + color2.getBlue() * fraction;

    return Color.color(
        Math.max(0, Math.min(1, red)),
        Math.max(0, Math.min(1, green)),
        Math.max(0, Math.min(1, blue))
    );
  }
}