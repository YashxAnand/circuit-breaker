
public class OpenCBState implements ICBState{
    private final CBStates state = CBStates.OPEN;

    @Override 
    public CBStates getState(){return this.state;}

    @Override 
    public void halfOpen(CircuitBreaker context){
        context.setState(new HalfOpenCBState());
    }
}