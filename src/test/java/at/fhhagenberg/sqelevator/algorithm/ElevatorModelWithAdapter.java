package at.fhhagenberg.sqelevator.algorithm;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.Assert.assertEquals;
import nz.ac.waikato.modeljunit.Action;
import nz.ac.waikato.modeljunit.FsmModel;

public class ElevatorModelWithAdapter  implements FsmModel {

    protected ElevatorState state = new ElevatorState(3);

    public Object getState() {
       return state;
    }

    public void reset(boolean testing) {

    }

    public boolean enqueueGuard() {
        return true;
    }

    @Action
    public void enqueue() {

    }
}
