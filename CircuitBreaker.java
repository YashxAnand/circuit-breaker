import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker{
    private final AtomicReference<ICBState> currentState;

    public CircuitBreaker(){
        this.currentState = new AtomicReference<>(new ClosedCBState());
    }

    public void setState(ICBState state){
        this.currentState.set(state);;
    }
}