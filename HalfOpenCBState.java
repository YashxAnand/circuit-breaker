public class HalfOpenCBState{
    private final CBStates state;

    public HalfOpenCBState(){
        this.state = CBStates.HALF_OPEN;
    }

    @Override 
    public CBStates getState(){return this.state;}

    @Override 
    public void close(CircuitBreaker context){
        context.setState(new ClosedCBState());
    }

    @Override 
    public void open(CircuitBreaker context){
        context.setState(new OpenCBState());
    }
}