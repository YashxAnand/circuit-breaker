public class CircuitBreaker{
    private ICBState currentState;

    public CircuitBreaker(){
        this.currentState = new ClosedCBState();
    }

    public void setState(ICBState state){
        this.state = state;
    }
}