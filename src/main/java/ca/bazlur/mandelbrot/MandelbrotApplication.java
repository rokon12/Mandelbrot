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


public class MandelbrotApplication extends Application {
    private final int WIDTH = 1000;
    private final int HEIGHT = 800;
    private double ZOOM = 200;

    private GraphicsContext gc;
    private final TextField iterationField = new TextField("100");

    private double centerX = -0.5;
    private double centerY = 0;
    private final MandelbrotCalculator calculator = new MandelbrotCalculator();

    private final double[] dragStartX = new double[1];
    private final double[] dragStartY = new double[1];

    @Override
    public void start(Stage stage) {
        // UI Setup
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        BorderPane root = new BorderPane();
        root.setBottom(createControlPanel());
        root.setCenter(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT + 100);

        stage.setScene(scene);
        stage.setTitle("Mandelbrot Explorer");
        stage.show();

        // Event Handlers
        setupMousePressedHandler(canvas);
        setupMouseDraggedHandler(canvas);

        // Initial Calculation
        calculateMandelbrot();
    }


    private void setupMousePressedHandler(Canvas canvas) {
        canvas.setOnMousePressed(mouseEvent -> {
            dragStartX[0] = mouseEvent.getX();
            dragStartY[0] = mouseEvent.getY();
        });
    }

    private void setupMouseDraggedHandler(Canvas canvas) {
        canvas.setOnMouseDragged(mouseEvent -> {
            double dx = mouseEvent.getX() - dragStartX[0];
            double dy = mouseEvent.getY() - dragStartY[0];
            centerX -= dx / ZOOM;
            centerY -= dy / ZOOM;
            dragStartX[0] = mouseEvent.getX();
            dragStartY[0] = mouseEvent.getY();
            calculateMandelbrot();
        });
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
        int[][] iterations = calculator.calculateIterations(WIDTH, HEIGHT, centerX, centerY, ZOOM, MAX_ITERATIONS);
        renderFractal(iterations);
    }

    private void renderFractal(int[][] iterations) {
        int MAX_ITERATIONS = Integer.parseInt(iterationField.getText());
        ColorPalette palette = new ColorPalette(); // Could customize the palette here

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Color color = palette.getColorForIterations(iterations[x][y], MAX_ITERATIONS);
                gc.setStroke(color);
                gc.strokeLine(x, y, x, y);
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