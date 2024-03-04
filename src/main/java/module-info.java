module ca.bazlur.mandelbrot {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
//    requires com.almasb.fxgl.all;
    requires java.desktop;

    opens ca.bazlur.mandelbrot to javafx.fxml;
    exports ca.bazlur.mandelbrot;
}