package ca.bazlur.mandelbrot;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;


public class MandelbrotApplication extends Application {
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 800;
    private static final double INITIAL_ZOOM = 200;
    private static final double ZOOM_FACTOR = 1.5;
    private static final int DEFAULT_MAX_ITERATIONS = 100;
    private static final double MIN_ZOOM = 50;
    private static final double MAX_ZOOM = 1e15;
    
    private final int WIDTH = DEFAULT_WIDTH;
    private final int HEIGHT = DEFAULT_HEIGHT;
    private double ZOOM = INITIAL_ZOOM;

    private Canvas canvas;
    private WritableImage image;
    private PixelWriter pixelWriter;
    private final TextField iterationField = new TextField(String.valueOf(DEFAULT_MAX_ITERATIONS));
    private final Label coordinateLabel = new Label("Center: (0, 0) | Zoom: 200");
    private final Label statusLabel = new Label("Ready");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Button calculateButton = new Button("Recalculate");
    private final DecimalFormat coordFormat = new DecimalFormat("0.###############");
    
    private double centerX = -0.5;
    private double centerY = 0;
    private MandelbrotCalculatorStrategy calculator;
    private ColorPalette.PaletteType currentPalette = ColorPalette.PaletteType.SMOOTH;
    
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private Task<int[][]> currentTask;
    
    private double dragStartX;
    private double dragStartY;
    private double dragStartCenterX;
    private double dragStartCenterY;

    @Override
    public void start(Stage stage) {
        calculator = MandelbrotCalculatorStrategy.create(MandelbrotCalculatorStrategy.StrategyType.EXECUTOR_SERVICE);
        
        // UI Setup
        canvas = new Canvas(WIDTH, HEIGHT);
        image = new WritableImage(WIDTH, HEIGHT);
        pixelWriter = image.getPixelWriter();
        
        BorderPane root = new BorderPane();
        VBox bottomPanel = new VBox(5);
        bottomPanel.getChildren().addAll(createControlPanel(), createStatusPanel());
        root.setBottom(bottomPanel);
        root.setCenter(canvas);
        
        Scene scene = new Scene(root, WIDTH, HEIGHT + 120);
        
        stage.setScene(scene);
        stage.setTitle("Mandelbrot Explorer - Enhanced");
        stage.setOnCloseRequest(e -> {
            if (calculator != null) {
                calculator.close();
            }
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        });
        stage.show();

        // Event Handlers
        setupMouseHandlers();
        setupScrollHandler();
        
        // Initial Calculation
        calculateMandelbrot();
    }

    private void setupMouseHandlers() {
        canvas.setOnMousePressed(mouseEvent -> {
            dragStartX = mouseEvent.getX();
            dragStartY = mouseEvent.getY();
            dragStartCenterX = centerX;
            dragStartCenterY = centerY;
        });
        
        canvas.setOnMouseDragged(mouseEvent -> {
            if (!isCalculating.get()) {
                double dx = mouseEvent.getX() - dragStartX;
                double dy = mouseEvent.getY() - dragStartY;
                centerX = dragStartCenterX - dx / ZOOM;
                centerY = dragStartCenterY - dy / ZOOM;
                updateCoordinateLabel();
                calculateMandelbrot();
            }
        });
        
        canvas.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                // Double click to zoom in
                zoomAt(mouseEvent.getX(), mouseEvent.getY(), ZOOM_FACTOR);
            }
        });
    }
    
    private void setupScrollHandler() {
        canvas.setOnScroll((ScrollEvent event) -> {
            if (!isCalculating.get()) {
                double zoomFactor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
                zoomAt(event.getX(), event.getY(), zoomFactor);
            }
        });
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        
        // Iteration input with validation
        iterationField.setPrefWidth(80);
        iterationField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                iterationField.setText(oldText);
            }
        });
        
        calculateButton.setOnAction(e -> calculateMandelbrot());
        
        Button zoomInButton = new Button("Zoom In");
        Button zoomOutButton = new Button("Zoom Out");
        Button resetButton = new Button("Reset View");
        
        zoomInButton.setOnAction(e -> zoomAt(WIDTH / 2.0, HEIGHT / 2.0, ZOOM_FACTOR));
        zoomOutButton.setOnAction(e -> zoomAt(WIDTH / 2.0, HEIGHT / 2.0, 1.0 / ZOOM_FACTOR));
        resetButton.setOnAction(e -> resetView());
        
        // Color palette selector
        ComboBox<ColorPalette.PaletteType> paletteSelector = new ComboBox<>();
        paletteSelector.getItems().addAll(ColorPalette.PaletteType.values());
        paletteSelector.setValue(currentPalette);
        paletteSelector.setOnAction(e -> {
            currentPalette = paletteSelector.getValue();
            if (currentTask != null && currentTask.getValue() != null) {
                renderFractal(currentTask.getValue(), Integer.parseInt(iterationField.getText()));
            }
        });
        
        // Calculator strategy selector
        ComboBox<MandelbrotCalculatorStrategy.StrategyType> strategySelector = new ComboBox<>();
        strategySelector.getItems().addAll(MandelbrotCalculatorStrategy.StrategyType.values());
        strategySelector.setValue(MandelbrotCalculatorStrategy.StrategyType.EXECUTOR_SERVICE);
        strategySelector.setOnAction(e -> {
            MandelbrotCalculatorStrategy.StrategyType newStrategy = strategySelector.getValue();
            if (calculator != null) {
                calculator.close();
            }
            calculator = MandelbrotCalculatorStrategy.create(newStrategy);
            calculateMandelbrot();
        });
        
        controlPanel.getChildren().addAll(
            new Label("Max Iterations:"), 
            iterationField, 
            calculateButton, 
            new Separator(),
            zoomInButton, 
            zoomOutButton, 
            resetButton,
            new Separator(),
            new Label("Color:"),
            paletteSelector,
            new Label("Calculator:"),
            strategySelector
        );
        
        return controlPanel;
    }
    
    private HBox createStatusPanel() {
        HBox statusPanel = new HBox(10);
        statusPanel.setPadding(new Insets(5, 10, 10, 10));
        
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        
        HBox.setHgrow(coordinateLabel, Priority.ALWAYS);
        
        statusPanel.getChildren().addAll(coordinateLabel, statusLabel, progressBar);
        
        return statusPanel;
    }
    
    private void resetView() {
        centerX = -0.5;
        centerY = 0;
        ZOOM = INITIAL_ZOOM;
        iterationField.setText(String.valueOf(DEFAULT_MAX_ITERATIONS));
        updateCoordinateLabel();
        calculateMandelbrot();
    }
    
    private void zoomAt(double mouseX, double mouseY, double zoomFactor) {
        double newZoom = ZOOM * zoomFactor;
        
        // Clamp zoom to reasonable values
        if (newZoom < MIN_ZOOM || newZoom > MAX_ZOOM) {
            return;
        }
        
        // Calculate the point under the mouse in complex plane
        double pointX = (mouseX - WIDTH / 2.0) / ZOOM + centerX;
        double pointY = (mouseY - HEIGHT / 2.0) / ZOOM + centerY;
        
        // Update zoom
        ZOOM = newZoom;
        
        // Adjust center so the point under mouse stays in the same position
        centerX = pointX - (mouseX - WIDTH / 2.0) / ZOOM;
        centerY = pointY - (mouseY - HEIGHT / 2.0) / ZOOM;
        
        updateCoordinateLabel();
        calculateMandelbrot();
    }
    
    private void updateCoordinateLabel() {
        String zoomStr = ZOOM > 1000000 ? String.format("%.2e", ZOOM) : coordFormat.format(ZOOM);
        coordinateLabel.setText(String.format("Center: (%s, %s) | Zoom: %s", 
            coordFormat.format(centerX), 
            coordFormat.format(centerY), 
            zoomStr));
    }

    private void calculateMandelbrot() {
        if (isCalculating.get()) {
            return; // Already calculating
        }
        
        String iterText = iterationField.getText().trim();
        if (iterText.isEmpty()) {
            showError("Please enter a valid number of iterations");
            return;
        }
        
        int maxIterations;
        try {
            maxIterations = Integer.parseInt(iterText);
            if (maxIterations < 1 || maxIterations > 10000) {
                showError("Iterations must be between 1 and 10000");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid iteration count");
            return;
        }
        
        isCalculating.set(true);
        calculateButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        statusLabel.setText("Calculating...");
        
        currentTask = new Task<>() {
            @Override
            protected int[][] call() {
                return calculator.calculateIterations(WIDTH, HEIGHT, centerX, centerY, ZOOM, maxIterations);
            }
        };
        
        currentTask.setOnSucceeded(e -> {
            int[][] iterations = currentTask.getValue();
            renderFractal(iterations, maxIterations);
            isCalculating.set(false);
            calculateButton.setDisable(false);
            progressBar.setVisible(false);
            statusLabel.setText("Ready");
        });
        
        currentTask.setOnFailed(e -> {
            isCalculating.set(false);
            calculateButton.setDisable(false);
            progressBar.setVisible(false);
            statusLabel.setText("Calculation failed");
            showError("Calculation failed: " + currentTask.getException().getMessage());
        });
        
        Thread calculationThread = new Thread(currentTask);
        calculationThread.setDaemon(true);
        calculationThread.start();
    }

    private void renderFractal(int[][] iterations, int maxIterations) {
        ColorPalette palette = new ColorPalette(currentPalette);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Use PixelWriter for efficient rendering
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Color color = palette.getColorForIterations(iterations[x][y], maxIterations);
                pixelWriter.setColor(x, y, color);
            }
        }
        
        // Draw the image to canvas
        gc.drawImage(image, 0, 0);
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}