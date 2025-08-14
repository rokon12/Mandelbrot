package ca.bazlur.mandelbrot;

public interface MandelbrotCalculatorStrategy extends AutoCloseable {
    int[][] calculateIterations(int width, int height, double centerX, double centerY, double zoom, int maxIterations);
    
    @Override
    void close();
    
    enum StrategyType {
        EXECUTOR_SERVICE("Executor Service (Multi-threaded)"),
        FORK_JOIN("Fork/Join Framework"),
        SINGLE_THREADED("Single Thread");
        
        private final String displayName;
        
        StrategyType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    static MandelbrotCalculatorStrategy create(StrategyType type) {
      return switch (type) {
        case EXECUTOR_SERVICE -> new MandelbrotCalculator();
        case FORK_JOIN -> new MandelbrotCalculatorFJ();
        case SINGLE_THREADED -> new SingleThreadedCalculator();
      };
    }
}