package ca.bazlur.mandelbrot;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;


public class HelloApplication extends Application {
    private final int WIDTH = 1000;
    private final int HEIGHT = 800;
    private double ZOOM = 200;

    private GraphicsContext gc;
    private final TextField iterationField = new TextField("100");

    private double centerX = -0.5;
    private double centerY = 0;

    @Override
    public void start(Stage stage) throws IOException {
        // Keep track of the main stage
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        BorderPane root = new BorderPane();
        root.setBottom(createControlPanel());
        root.setCenter(canvas);

        Scene scene = new Scene(root, WIDTH, HEIGHT + 100);

        final double[] dragStartX = new double[1];
        final double[] dragStartY = new double[1];

        canvas.setOnMousePressed(mouseEvent -> {
            dragStartX[0] = mouseEvent.getX();
            dragStartY[0] = mouseEvent.getY();
        });

        // Mouse drag handler for handling the movement
        canvas.setOnMouseDragged(mouseEvent -> {
            double dx = mouseEvent.getX() - dragStartX[0];
            double dy = mouseEvent.getY() - dragStartY[0];
            centerX -= dx / ZOOM;
            centerY -= dy / ZOOM;
            dragStartX[0] = mouseEvent.getX();
            dragStartY[0] = mouseEvent.getY();
            calculateMandelbrot();
        });

        stage.setScene(scene);
        stage.setTitle("Mandelbrot Explorer");
        stage.show();

        calculateMandelbrot();
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10); // Spacing between controls

        controlPanel.setPadding(new Insets(10));

        Button zoomInButton = new Button("Zoom In");
        Button zoomOutButton = new Button("Zoom Out");

        // Bind action events to zoom functions
        zoomInButton.setOnAction(e -> zoomIn(WIDTH / 2, HEIGHT / 2));
        zoomOutButton.setOnAction(e -> zoomOut(WIDTH / 2, HEIGHT / 2));

        Button calculateButton = new Button("Recalculate");
        calculateButton.setOnAction(e -> calculateMandelbrot());

        controlPanel.getChildren().addAll(new Label("Iterations:"), iterationField, calculateButton, zoomInButton, zoomOutButton);

        return controlPanel;
    }

    private void calculateMandelbrot() {
        int MAX_ITERATIONS = Integer.parseInt(iterationField.getText()); // Get iterations

        gc.clearRect(0, 0, WIDTH, HEIGHT); // Clear previous drawing

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                double zx = 0.0;
                double zy = 0.0;
                double cx = (x - WIDTH / 2.0) / ZOOM + centerX;
                double cy = (y - HEIGHT / 2.0) / ZOOM + centerY;
                int iterations = 0;

                while (zx * zx + zy * zy < 4 && iterations < MAX_ITERATIONS) {
                    double temp = zx * zx - zy * zy + cx;
                    zy = 2 * zx * zy + cy;
                    zx = temp;
                    iterations++;
                }

                if (iterations < MAX_ITERATIONS) {
                    double hue = 360.0 * ((double) iterations / MAX_ITERATIONS); // map iterations to value between [0, 360]
                    gc.setStroke(Color.hsb(hue, 1.0, 1.0, 1.0)); // saturation and brightness both set to 1.0
                    gc.strokeLine(x, y, x, y);
                }
            }
        }
    }

    private void zoomIn(int x, int y) {
        ZOOM *= 2; // Increase zoom level
        updateCenterAndCalculate(x, y);
    }

    private void zoomOut(int x, int y) {
        if (ZOOM > 1) {
            ZOOM /= 2; // Decrease zoom level
        }
        updateCenterAndCalculate(x, y);
    }

    private void updateCenterAndCalculate(int x, int y) {
        // Calculate new center based on interaction point
        double newCenterX = (x - WIDTH / 2.0) / ZOOM + centerX;
        double newCenterY = (y - HEIGHT / 2.0) / ZOOM + centerY;

        centerX = newCenterX;
        centerY = newCenterY;

        calculateMandelbrot();
    }

    public static void main(String[] args) {
        launch();
    }
}