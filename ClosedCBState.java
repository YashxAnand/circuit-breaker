public class ClosedCBState implements ICBState{
    private final CBStates state;

    public ClosedCBState(){
        this.state = CBStates.CLOSED;
    }

    @Override 
    public CBStates getState(){
        return this.state;
    }

    @Override
    public void halfOpen(CircuitBreaker context){
        context.setState(new HalfOpenCBState());
    }
}