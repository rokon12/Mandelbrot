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
import javafx.scene.input.ZoomEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
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
    private Fractal currentFractal = new MandelbrotFractal();
    
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private Task<int[][]> currentTask;
    private int[][] currentIterations;
    
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
        setupGestureHandlers();
        setupKeyboardHandlers(scene);
        
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
    
    private void setupGestureHandlers() {
        // Pinch-to-zoom gesture (trackpad/touchscreen)
        canvas.setOnZoom((ZoomEvent event) -> {
            if (!isCalculating.get()) {
                double zoomFactor = event.getZoomFactor();
                // Get the center point of the zoom gesture
                double x = event.getX();
                double y = event.getY();
                zoomAt(x, y, zoomFactor);
                event.consume();
            }
        });
        
        // Handle zoom gesture start
        canvas.setOnZoomStarted((ZoomEvent event) -> {
            // Could store initial state here if needed
            event.consume();
        });
        
        // Handle zoom gesture finish
        canvas.setOnZoomFinished((ZoomEvent event) -> {
            // Could trigger final high-quality render here
            event.consume();
        });
        
        // Rotation gesture (two-finger rotate on trackpad/touchscreen)
        canvas.setOnRotate((RotateEvent event) -> {
            // Could implement rotation of the view if desired
            // For now, we'll just consume the event
            event.consume();
        });
        
        // Swipe gestures for quick navigation
        canvas.setOnSwipeUp((SwipeEvent event) -> {
            if (!isCalculating.get()) {
                // Pan up
                centerY -= (HEIGHT * 0.25) / ZOOM;
                updateCoordinateLabel();
                calculateMandelbrot();
                event.consume();
            }
        });
        
        canvas.setOnSwipeDown((SwipeEvent event) -> {
            if (!isCalculating.get()) {
                // Pan down
                centerY += (HEIGHT * 0.25) / ZOOM;
                updateCoordinateLabel();
                calculateMandelbrot();
                event.consume();
            }
        });
        
        canvas.setOnSwipeLeft((SwipeEvent event) -> {
            if (!isCalculating.get()) {
                // Pan left
                centerX -= (WIDTH * 0.25) / ZOOM;
                updateCoordinateLabel();
                calculateMandelbrot();
                event.consume();
            }
        });
        
        canvas.setOnSwipeRight((SwipeEvent event) -> {
            if (!isCalculating.get()) {
                // Pan right
                centerX += (WIDTH * 0.25) / ZOOM;
                updateCoordinateLabel();
                calculateMandelbrot();
                event.consume();
            }
        });
    }
    
    private void setupKeyboardHandlers(Scene scene) {
        scene.setOnKeyPressed((KeyEvent event) -> {
            if (isCalculating.get()) {
                return; // Don't process keys while calculating
            }
            
            KeyCode code = event.getCode();
            boolean shift = event.isShiftDown();
            boolean ctrl = event.isControlDown() || event.isMetaDown();
            
            switch (code) {
                // Zoom controls
                case PLUS, EQUALS, ADD -> {
                    // + or = key: zoom in at center
                    zoomAt(WIDTH / 2.0, HEIGHT / 2.0, ZOOM_FACTOR);
                }
                case MINUS, SUBTRACT -> {
                    // - key: zoom out at center
                    zoomAt(WIDTH / 2.0, HEIGHT / 2.0, 1.0 / ZOOM_FACTOR);
                }
                
                // Arrow key navigation
                case UP, W -> {
                    double panAmount = shift ? 0.5 : 0.1; // Shift for faster pan
                    centerY -= (HEIGHT * panAmount) / ZOOM;
                    updateCoordinateLabel();
                    calculateMandelbrot();
                }
                case DOWN -> {
                    double panAmount = shift ? 0.5 : 0.1;
                    centerY += (HEIGHT * panAmount) / ZOOM;
                    updateCoordinateLabel();
                    calculateMandelbrot();
                }
                case LEFT, A -> {
                    double panAmount = shift ? 0.5 : 0.1;
                    centerX -= (WIDTH * panAmount) / ZOOM;
                    updateCoordinateLabel();
                    calculateMandelbrot();
                }
                case RIGHT, D -> {
                    double panAmount = shift ? 0.5 : 0.1;
                    centerX += (WIDTH * panAmount) / ZOOM;
                    updateCoordinateLabel();
                    calculateMandelbrot();
                }
                
                // Quick actions
                case R -> {
                    if (ctrl) {
                        // Ctrl+R: Reset view
                        resetView();
                    } else {
                        // R: Recalculate
                        calculateMandelbrot();
                    }
                }
                case SPACE -> {
                    // Space: Recalculate
                    calculateMandelbrot();
                }
                case ESCAPE -> {
                    // ESC: Reset view
                    resetView();
                }
                
                // Save shortcuts
                case S -> {
                    if (ctrl && shift) {
                        // Ctrl+Shift+S: Save HD
                        saveImage(true);
                    } else if (ctrl) {
                        // Ctrl+S: Save current
                        saveImage(false);
                    } else if (!shift) {
                        // S key alone: pan down (same as arrow down)
                        double panAmount = 0.1;
                        centerY += (HEIGHT * panAmount) / ZOOM;
                        updateCoordinateLabel();
                        calculateMandelbrot();
                    }
                }
                
                // Iteration controls
                case I -> {
                    if (shift) {
                        // Shift+I: Decrease iterations
                        int current = Integer.parseInt(iterationField.getText());
                        if (current > 50) {
                            iterationField.setText(String.valueOf(current - 50));
                            calculateMandelbrot();
                        }
                    } else {
                        // I: Increase iterations
                        int current = Integer.parseInt(iterationField.getText());
                        if (current < 10000) {
                            iterationField.setText(String.valueOf(current + 50));
                            calculateMandelbrot();
                        }
                    }
                }
                
                // Help
                case H, F1 -> {
                    showKeyboardShortcuts();
                }
            }
            
            event.consume();
        });
    }
    
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Mandelbrot Explorer Keyboard Shortcuts");
        
        String shortcuts = """
            Navigation:
            • Arrow Keys / WASD: Pan view
            • Shift + Arrows: Pan faster
            • +/- or =/- : Zoom in/out
            • Mouse scroll: Zoom at cursor
            • Pinch gesture: Zoom (trackpad)
            • Swipe gestures: Quick pan
            
            Actions:
            • Space / R: Recalculate
            • Ctrl+R / ESC: Reset view
            • I: Increase iterations (+50)
            • Shift+I: Decrease iterations (-50)
            
            File Operations:
            • Ctrl+S: Save current view
            • Ctrl+Shift+S: Save HD image
            
            Other:
            • Double-click: Zoom in at point
            • Drag: Pan view
            • H / F1: Show this help
            """;
        
        alert.setContentText(shortcuts);
        alert.getDialogPane().setPrefWidth(450);
        alert.showAndWait();
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
        Button saveButton = new Button("Save Image");
        Button saveHDButton = new Button("Save HD");
        Button helpButton = new Button("Help (H)");
        
        zoomInButton.setOnAction(e -> zoomAt(WIDTH / 2.0, HEIGHT / 2.0, ZOOM_FACTOR));
        zoomOutButton.setOnAction(e -> zoomAt(WIDTH / 2.0, HEIGHT / 2.0, 1.0 / ZOOM_FACTOR));
        resetButton.setOnAction(e -> resetView());
        saveButton.setOnAction(e -> saveImage(false));
        saveHDButton.setOnAction(e -> saveImage(true));
        helpButton.setOnAction(e -> showKeyboardShortcuts());
        
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
        
        // Fractal type selector
        ComboBox<String> fractalSelector = new ComboBox<>();
        fractalSelector.getItems().addAll(
            "Mandelbrot",
            "Julia Set",
            "Burning Ship",
            "Tricorn",
            "Multibrot (d=3)",
            "Multibrot (d=4)",
            "Phoenix"
        );
        fractalSelector.setValue("Mandelbrot");
        fractalSelector.setOnAction(e -> {
            String selectedFractal = fractalSelector.getValue();
            switch (selectedFractal) {
                case "Julia Set" -> {
                    currentFractal = new JuliaFractal();
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                case "Burning Ship" -> {
                    currentFractal = new BurningShipFractal();
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                case "Tricorn" -> {
                    currentFractal = new TricornFractal();
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                case "Multibrot (d=3)" -> {
                    currentFractal = new MultibrotFractal(3);
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                case "Multibrot (d=4)" -> {
                    currentFractal = new MultibrotFractal(4);
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                case "Phoenix" -> {
                    currentFractal = new PhoenixFractal();
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
                default -> {
                    currentFractal = new MandelbrotFractal();
                    centerX = currentFractal.getDefaultCenter().real();
                    centerY = currentFractal.getDefaultCenter().imaginary();
                    ZOOM = currentFractal.getDefaultZoom();
                }
            }
            updateCoordinateLabel();
            calculateMandelbrot();
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
            new Label("Fractal:"),
            fractalSelector,
            new Separator(),
            new Label("Max Iterations:"), 
            iterationField, 
            calculateButton, 
            new Separator(),
            zoomInButton, 
            zoomOutButton, 
            resetButton,
            new Separator(),
            saveButton,
            saveHDButton,
            new Separator(),
            new Label("Color:"),
            paletteSelector,
            new Label("Calculator:"),
            strategySelector,
            new Separator(),
            helpButton
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
                return calculator.calculateIterations(WIDTH, HEIGHT, centerX, centerY, ZOOM, maxIterations, currentFractal);
            }
        };
        
        currentTask.setOnSucceeded(e -> {
            int[][] iterations = currentTask.getValue();
            currentIterations = iterations; // Store for saving
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
    
    private void saveImage(boolean highResolution) {
        if (currentIterations == null) {
            showError("No image to save. Please calculate the fractal first.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Fractal Image");
        fileChooser.setInitialFileName("mandelbrot_" + System.currentTimeMillis() + ".png");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Images", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg")
        );
        
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            if (highResolution) {
                saveHighResolutionImage(file);
            } else {
                saveCurrentImage(file);
            }
        }
    }
    
    private void saveCurrentImage(File file) {
        try {
            WritableImage writableImage = new WritableImage(WIDTH, HEIGHT);
            canvas.snapshot(null, writableImage);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            
            String extension = getFileExtension(file.getName());
            ImageIO.write(bufferedImage, extension, file);
            
            showInfo("Image saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Failed to save image: " + e.getMessage());
        }
    }
    
    private void saveHighResolutionImage(File file) {
        // Create a dialog to get HD resolution
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("High Resolution Export");
        dialog.setHeaderText("Choose export resolution");
        
        // Set up the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<String> resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(
            "1920x1080 (Full HD)",
            "2560x1440 (2K)",
            "3840x2160 (4K)",
            "7680x4320 (8K)",
            "Custom"
        );
        resolutionCombo.setValue("1920x1080 (Full HD)");
        
        TextField widthField = new TextField("1920");
        TextField heightField = new TextField("1080");
        widthField.setDisable(true);
        heightField.setDisable(true);
        
        resolutionCombo.setOnAction(e -> {
            String selected = resolutionCombo.getValue();
            boolean isCustom = "Custom".equals(selected);
            widthField.setDisable(!isCustom);
            heightField.setDisable(!isCustom);
            
            if (!isCustom) {
                switch (selected) {
                    case "1920x1080 (Full HD)" -> {
                        widthField.setText("1920");
                        heightField.setText("1080");
                    }
                    case "2560x1440 (2K)" -> {
                        widthField.setText("2560");
                        heightField.setText("1440");
                    }
                    case "3840x2160 (4K)" -> {
                        widthField.setText("3840");
                        heightField.setText("2160");
                    }
                    case "7680x4320 (8K)" -> {
                        widthField.setText("7680");
                        heightField.setText("4320");
                    }
                }
            }
        });
        
        grid.add(new Label("Resolution:"), 0, 0);
        grid.add(resolutionCombo, 1, 0);
        grid.add(new Label("Width:"), 0, 1);
        grid.add(widthField, 1, 1);
        grid.add(new Label("Height:"), 0, 2);
        grid.add(heightField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                try {
                    int w = Integer.parseInt(widthField.getText());
                    int h = Integer.parseInt(heightField.getText());
                    if (w > 0 && h > 0 && w <= 15360 && h <= 8640) {
                        return new int[]{w, h};
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(resolution -> {
            int hdWidth = resolution[0];
            int hdHeight = resolution[1];
            
            statusLabel.setText("Rendering HD image...");
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            
            Task<Void> hdTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Calculate HD iterations
                    int maxIterations = Integer.parseInt(iterationField.getText());
                    int[][] hdIterations = calculator.calculateIterations(
                        hdWidth, hdHeight, centerX, centerY, ZOOM, maxIterations, currentFractal
                    );
                    
                    // Create HD image
                    WritableImage hdImage = new WritableImage(hdWidth, hdHeight);
                    PixelWriter hdPixelWriter = hdImage.getPixelWriter();
                    ColorPalette palette = new ColorPalette(currentPalette);
                    
                    for (int x = 0; x < hdWidth; x++) {
                        for (int y = 0; y < hdHeight; y++) {
                            Color color = palette.getColorForIterations(hdIterations[x][y], maxIterations);
                            hdPixelWriter.setColor(x, y, color);
                        }
                    }
                    
                    // Save the HD image
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(hdImage, null);
                    String extension = getFileExtension(file.getName());
                    ImageIO.write(bufferedImage, extension, file);
                    
                    return null;
                }
            };
            
            hdTask.setOnSucceeded(e -> {
                progressBar.setVisible(false);
                statusLabel.setText("Ready");
                showInfo("HD image saved successfully to: " + file.getAbsolutePath() + 
                        "\nResolution: " + hdWidth + "x" + hdHeight);
            });
            
            hdTask.setOnFailed(e -> {
                progressBar.setVisible(false);
                statusLabel.setText("Ready");
                showError("Failed to save HD image: " + hdTask.getException().getMessage());
            });
            
            Thread hdThread = new Thread(hdTask);
            hdThread.setDaemon(true);
            hdThread.start();
        });
    }
    
    private String getFileExtension(String fileName) {
        String extension = "png";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
            if ("jpeg".equals(extension)) {
                extension = "jpg";
            }
        }
        return extension;
    }
    
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}