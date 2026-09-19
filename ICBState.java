interface ICBState{
    CBStates getState();

    default void close(CircuitBreaker context){
        throw new CBStateTransitionException("Can't move to close state from current state!")
    }

    default void halfOpen(CircuitBreaker context){
        throw new CBStateTransitionException("Can't move to half open state from current state!");
    }

    default void open(CircuitBreaker context){
        throw new CBStateTransitionException("Can't move to open state from current state!");
    }
}