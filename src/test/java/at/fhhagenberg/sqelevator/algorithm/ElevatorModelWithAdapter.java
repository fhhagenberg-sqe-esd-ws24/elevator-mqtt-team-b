package at.fhhagenberg.sqelevator.algorithm;

import nz.ac.waikato.modeljunit.Action;
import nz.ac.waikato.modeljunit.FsmModel;
import nz.ac.waikato.modeljunit.RandomTester;
import nz.ac.waikato.modeljunit.StopOnFailureListener;
import nz.ac.waikato.modeljunit.VerboseListener;
import nz.ac.waikato.modeljunit.coverage.ActionCoverage;
import nz.ac.waikato.modeljunit.coverage.StateCoverage;
import nz.ac.waikato.modeljunit.coverage.TransitionCoverage;

import java.lang.reflect.Array;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ElevatorModelWithAdapter implements FsmModel {
    private final int numberFloors = 3;
    private final int numberElevators = 2;
    protected ElevatorAlgorithm algorithm = new ElevatorAlgorithm(); // is state less
    private int selectedElevator = 0;

    // Inputs for the elevator algorithm
    private boolean[] floorButtons;
    private boolean[] serviceFloors;
    private boolean[] buttonUp;
    private boolean[] buttonDown;
    private ElevatorState[] allElevators;

    ElevatorModelWithAdapter() {
        reset(true);
    }

    @Action
    public void selectNextElevator() {
        selectedElevator++;
        selectedElevator = selectedElevator % numberElevators;
    }

    public Object getState() {
        StringBuilder strState = new StringBuilder();
        for (ElevatorState allElevator : allElevators) {
            if (allElevator.currentFloor < numberFloors && allElevator.currentFloor > 0) {
                strState.append("IN_BETWEEN      ");
            } else if (allElevator.currentFloor == numberFloors) {
                strState.append("REACHED_TOP     ");
            } else if (allElevator.currentFloor == 0) {
                strState.append("REACHED_BOTTOM  ");
            } else {
                strState.append("UNKNOWN         ");
            }
        }
        return strState.toString();
    }

    public void reset(boolean testing) {
        allElevators = new ElevatorState[numberElevators];
        floorButtons = new boolean[numberFloors];
        Arrays.fill(floorButtons, false);
        serviceFloors = new boolean[numberFloors];
        Arrays.fill(serviceFloors, true);
        buttonUp = new boolean[numberFloors];
        Arrays.fill(buttonUp, false);
        buttonDown = new boolean[numberFloors];
        Arrays.fill(buttonDown, false);
        for(int i = 0; i < numberElevators; i++) {
            allElevators[i] = new ElevatorState(numberFloors);
        }
        selectedElevator = 0;
    }

    public boolean moveUpGuard() {
        ElevatorState state = allElevators[selectedElevator];
        return state.currentFloor <= state.targetFloor && state.isReadyForNextTarget && state.currentFloor + 1 < numberFloors;
    }

    @Action
    public void moveUp() {
        ElevatorState state = allElevators[selectedElevator];
        int requestedFloor = state.currentFloor + 1;
        Arrays.fill(floorButtons, false);
        floorButtons[requestedFloor] = true;
        // open door so isReadyForNextTarget is set to true
        state.doorStatus = ElevatorState.eDoorStatus.OPEN;

        algorithm.processRequests(state, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(state.targetFloor, requestedFloor);
        assertEquals(state.direction, ElevatorState.eDirection.UP);
        assertFalse(state.isReadyForNextTarget);

        // finish moving
        state.currentFloor = requestedFloor;
        state.doorStatus = ElevatorState.eDoorStatus.OPEN;

        algorithm.processRequests(state, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(state.direction, ElevatorState.eDirection.IDLE);
        assertTrue(state.isReadyForNextTarget);
    }

    public boolean moveDownGuard() {
        ElevatorState state = allElevators[selectedElevator];
        return state.currentFloor >= state.targetFloor && state.isReadyForNextTarget && state.currentFloor - 1 >= 0;
    }

    @Action
    public void moveDown() {
        ElevatorState state = allElevators[selectedElevator];
        int requestedFloor = state.currentFloor - 1;
        Arrays.fill(floorButtons, false);
        floorButtons[requestedFloor] = true;
        // open door so isReadyForNextTarget is set to true
        state.doorStatus = ElevatorState.eDoorStatus.OPEN;

        algorithm.processRequests(state, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(state.targetFloor, requestedFloor);
        assertEquals(state.direction, ElevatorState.eDirection.DOWN);
        assertFalse(state.isReadyForNextTarget);

        // finish moving
        state.currentFloor = requestedFloor;
        state.doorStatus = ElevatorState.eDoorStatus.OPEN;

        algorithm.processRequests(state, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(state.direction, ElevatorState.eDirection.IDLE);
        assertTrue(state.isReadyForNextTarget);
    }

    public boolean idleGuard() {
        ElevatorState state = allElevators[selectedElevator];
        return state.direction == ElevatorState.eDirection.IDLE;
    }

    @Action
    public void idle() {
        ElevatorState state = allElevators[selectedElevator];
        Arrays.fill(floorButtons, false);
        assertTrue(state.isReadyForNextTarget);

        algorithm.processRequests(state, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertTrue(state.isReadyForNextTarget);
        assertEquals(ElevatorState.eDirection.IDLE, state.direction);
    }

}
