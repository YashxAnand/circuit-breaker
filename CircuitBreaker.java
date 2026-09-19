import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker{
    private final AtomicReference<ICBState> currentState;
    private AtomicLong lastWindowResetM;
    private final long windowTimeM;
    private AtomicLong totalRequestsInWindow;
    private AtomicLong failureRequests;
    private final long minRequestCount;
    private final long failureThreshold;
    private final int halfOpenPercent;

    public static class Builder{
        private long windowTimeM = 2000;
        private long minRequestCount = 10;
        private long failureThreshold = 50;
        private int halfOpenPercent = 20;

        public Builder windowTimeM(long windowTimeM){
            this.windowTimeM = windowTimeM;

            return this;
        }

        public Builder minRequestCount(long minRequestCount){
            this.minRequestCount = minRequestCount;

            return this;
        }

        public Builder failureThreshold(long failureThreshold){
            this.failureThreshold = failureThreshold;

            return this;
        }

        public Builder halfOpenPercent(int halfOpenPercent){
            this.halfOpenPercent = halfOpenPercent;

            return this;
        }

        public CircuitBreaker build(){
            return new CircuitBreaker(this);
        }
    }

    private CircuitBreaker(Builder builder){
        this.currentState = new AtomicReference<>(new ClosedCBState());
        this.lastWindowResetM = new AtomicLong(System.currentTimeMillis());
        this.lastWindowResetM = builder.lastWindowResetM;
        this.windowTimeM = builder.windowTimeM;
        this.totalRequestsInWindow = new AtomicLong(0);
        this.failureRequests = new AtomicLong(0);
        this.minRequestCount = builder.minRequestCount;
        this.failureThreshold = builder.failureThreshold;
        this.halfOpenPercent = builder.halfOpenPercent;
    }

    public void setState(ICBState state){
        this.currentState.set(state);
    }
}