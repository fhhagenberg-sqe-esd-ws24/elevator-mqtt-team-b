
package at.fhhagenberg.sqelevator.algorithm;

import java.util.*;

public class ElevatorAlgorithm {
    private final List<ElevatorState> elevators;
    private final int numFloors;

    public ElevatorAlgorithm(int numElevators, int numFloors) {
        this.elevators = new ArrayList<>();
        this.numFloors = numFloors;

        for (int i = 0; i < numElevators; i++) {
            elevators.add(new ElevatorState(numFloors));
        }
    }

    public void updateState(int elevatorId, int currentFloor, int direction, boolean[] requests) {
        ElevatorState elevator = elevators.get(elevatorId);
        elevator.currentFloor = currentFloor;
        elevator.requests = requests.clone();
        elevator.direction = direction;
    }

    public void processRequests() {
        for (ElevatorState elevator : elevators) {
            // Check if there are any active requests
            if (hasActiveRequests(elevator)) {
                moveElevator(elevator);
            } else {
                elevator.direction = 2; // Uncommitted direction
            }
        }
    }

    private boolean hasActiveRequests(ElevatorState elevator) {
        for (boolean request : elevator.requests) {
            if (request) return true;
        }
        return false;
    }

    private void moveElevator(ElevatorState elevator) {
        // If moving up, process upwards requests first
        if (elevator.direction == 0) {
            if (!processUpRequests(elevator)) {
                // No more upwards requests, switch to downwards
                processDownRequests(elevator);
            }
        } else if (elevator.direction == 1) {
            // If moving down, process downwards requests first
            if (!processDownRequests(elevator)) {
                // No more downwards requests, switch to upwards
                processUpRequests(elevator);
            }
        } else {
            // Uncommitted: start by going up if requests exist, else down
            if (!processUpRequests(elevator)) {
                processDownRequests(elevator);
            }
        }
    }

    private boolean processUpRequests(ElevatorState elevator) {
        for (int i = elevator.currentFloor + 1; i < numFloors; i++) {
            if (elevator.requests[i]) {
                elevator.targetFloor = i;
                elevator.direction = 0; // Moving up
                return true;
            }
        }
        return false;
    }

    private boolean processDownRequests(ElevatorState elevator) {
        for (int i = elevator.currentFloor - 1; i >= 0; i--) {
            if (elevator.requests[i]) {
                elevator.targetFloor = i;
                elevator.direction = 1; // Moving down
                return true;
            }
        }
        return false;
    }

    public List<ElevatorState> getElevatorStates() {
        return elevators;
    }
}
