import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class CircuitBreaker{
    private final AtomicReference<ICBState> currentState;
    private final AtomicReference<ICBState> openState;
    private final AtomicReference<ICBState> halfOpenState;
    private final AtomicReference<ICBState> closedState;
    private AtomicLong lastWindowResetM;
    private final long windowTimeM;
    private AtomicLong totalRequestsInWindow;
    private AtomicLong failureRequests;
    private final long minRequestCount;
    private final int failureThreshold;
    private final int halfOpenPercent;
    private final long openWindowTimeout;

    public static class Builder{
        private long windowTimeM = 2000;
        private long minRequestCount = 10;
        private int failureThreshold = 50;
        private int halfOpenPercent = 20;
        private long openWindowTimeout = 5000;
        
        public Builder openWindowTimeout(long openWindowTimeout){
            this.openWindowTimeout = openWindowTimeout;

            return this;
        }

        public Builder windowTimeM(long windowTimeM){
            this.windowTimeM = windowTimeM;

            return this;
        }

        public Builder minRequestCount(long minRequestCount){
            this.minRequestCount = minRequestCount;

            return this;
        }

        public Builder failureThreshold(int failureThreshold){
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
        this.closedState = new AtomicReference<>(new ClosedCBState());
        this.halfOpenState = new AtomicReference<>(new HalfOpenCBState());
        this.openState = new AtomicReference<>(new OpenCBState());
        this.currentState = new AtomicReference<>(closedState.get());
        this.lastWindowResetM = new AtomicLong(System.currentTimeMillis());
        this.windowTimeM = builder.windowTimeM;
        this.totalRequestsInWindow = new AtomicLong(0);
        this.failureRequests = new AtomicLong(0);
        this.minRequestCount = builder.minRequestCount;
        this.failureThreshold = builder.failureThreshold;
        this.halfOpenPercent = builder.halfOpenPercent;
        this.openWindowTimeout = builder.openWindowTimeout;
    }

    public AtomicReference<ICBState> getOpenState(){return this.openState;}

    public AtomicReference<ICBState> getClosedState(){return this.closedState;}

    public AtomicReference<ICBState> getHalfOpenState(){return this.halfOpenState;}

    public void setState(AtomicReference<ICBState> state){
        this.currentState.set(state.get());
    }

    private void checkWindowReset(){
        if(this.currentState.get().getState().equals(CBStates.HALF_OPEN))
            return;
        CBStates currState = this.currentState.get().getState();

        long lastResetLongValue = lastWindowResetM.get();

        if(System.currentTimeMillis() - lastResetLongValue >= (currState.equals(CBStates.OPEN)?openWindowTimeout:windowTimeM)){
            if(lastWindowResetM.compareAndSet(lastResetLongValue, System.currentTimeMillis())){
                totalRequestsInWindow.set(0);
                failureRequests.set(0);

                if(this.currentState.get().getState().equals(CBStates.OPEN))
                    this.currentState.compareAndSet(openState.get(), halfOpenState.get());
            }
        }
    }

    private boolean isAllowed(){
        checkWindowReset();
        CBStates currentState = this.currentState.get().getState();

        if(currentState.equals(CBStates.CLOSED)){
            this.totalRequestsInWindow.incrementAndGet();
            return true;
        } else if(currentState.equals(CBStates.HALF_OPEN)){
            long requestNumber = totalRequestsInWindow.incrementAndGet();
            int allowedN = 100/halfOpenPercent;

            return (requestNumber%allowedN) == 0;
        } 

        return false;
    }

    private void onSuccess(){
        if(this.currentState.get().getState().equals(CBStates.HALF_OPEN)){
            if(this.currentState.compareAndSet(halfOpenState.get(), closedState.get())){
                this.lastWindowResetM.set(System.currentTimeMillis());
                this.totalRequestsInWindow.set(0);
                this.failureRequests.set(0);
            }
        }
    }

    private void onFailure(){
        CBStates currState = this.currentState.get().getState();

        if(currState.equals(CBStates.HALF_OPEN)){
            if(this.currentState.compareAndSet(halfOpenState.get(), openState.get())){
                this.lastWindowResetM.set(System.currentTimeMillis());
                this.totalRequestsInWindow.set(0);
                this.failureRequests.set(0);
            }

            return;
        }

        //If closed
        long failureCount = this.failureRequests.incrementAndGet();
        long totalRequests = totalRequestsInWindow.get();

        if(totalRequestsInWindow.get() > minRequestCount){
            int failurePercent = (failureCount * 100) / totalRequests;

            if(failurePercent >= failureThreshold){

                if(this.currentState.compareAndSet(closedState.get(), openState.get())){
                    this.lastWindowResetM.set(System.currentTimeMillis());
                    this.totalRequestsInWindow.set(0);
                    this.failureRequests.set(0);
                }
            }
        }
    }

    public <T> T execute(Supplier<T> request){
        T result = null;

        if(isAllowed()){

            try{
                result = request.get();
                onSuccess();
                return result;
            }catch(Exception e){
                onFailure();
                throw e;
            }
        }

        throw new CBOpenException("Circuit breaker is OPEN!");
    }
}