
package at.fhhagenberg.sqelevator.algorithm;

import at.fhhagenberg.sqelevator.algorithm.ElevatorState.eDirection;

public class ElevatorAlgorithm implements IElevatorAlgorithm {

    public ElevatorAlgorithm() {    }

    public void processRequests(ElevatorState elevator, ElevatorState[] allElevators, boolean[] floorButtons, boolean[] serviceFloors, boolean[] buttonUp, boolean[] buttonDown) {
        int numFloors = floorButtons.length;
        
        if (elevator.doorStatus == ElevatorState.eDoorStatus.OPEN)
        {
        	elevator.isReadyForNextTarget = true;
        }
        
        // Wait until the door has opened and closed
        if (!elevator.isReadyForNextTarget) 
        {
            return; 
        }

        // If the elevator is idle, decide the initial direction
        if (elevator.direction == ElevatorState.eDirection.IDLE) {
            // Check for requests above
            for (int i = elevator.currentFloor + 1; i < numFloors; i++) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors) &&
                		(isInElevatorFloorButtonRequest(i, floorButtons, serviceFloors)
                				|| !isAnotherElevatorHandlingRequest(allElevators, i, ElevatorState.eDirection.UP))) {
                    elevator.targetFloor = i;
                    elevator.direction = ElevatorState.eDirection.UP;
                    elevator.isReadyForNextTarget = false;
                    return;
                }
            }
            // Check for requests below
            for (int i = elevator.currentFloor - 1; i >= 0; i--) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors) &&
                		(isInElevatorFloorButtonRequest(i, floorButtons, serviceFloors)
                				|| !isAnotherElevatorHandlingRequest(allElevators, i, ElevatorState.eDirection.DOWN))) {
                    elevator.targetFloor = i;
                    elevator.direction = ElevatorState.eDirection.DOWN;
                    elevator.isReadyForNextTarget = false;
                    return;
                }
            }
            // No requests, remain idle
            return;
        }

        // If the elevator is moving UP
        if (elevator.direction == ElevatorState.eDirection.UP) {
            // Check for requests above
            for (int i = elevator.currentFloor + 1; i < numFloors; i++) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors) &&
	            		(isInElevatorFloorButtonRequest(i, floorButtons, serviceFloors)
	            				|| !isAnotherElevatorHandlingRequest(allElevators, i, ElevatorState.eDirection.UP))) {
                    elevator.targetFloor = i;
                    elevator.isReadyForNextTarget = false;
                    return;
                }
            }
            // No more requests above, switch direction
            elevator.direction = ElevatorState.eDirection.IDLE;
            processRequests(elevator, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);
        }

        // If the elevator is moving DOWN
        if (elevator.direction == ElevatorState.eDirection.DOWN) {
            // Check for requests below
            for (int i = elevator.currentFloor - 1; i >= 0; i--) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors) &&
                		(isInElevatorFloorButtonRequest(i, floorButtons, serviceFloors)
            				|| !isAnotherElevatorHandlingRequest(allElevators, i, ElevatorState.eDirection.DOWN))) {
                    elevator.targetFloor = i;
                    elevator.isReadyForNextTarget = false;
                    return;
                }
            }
            // No more requests below, switch direction
            elevator.direction = ElevatorState.eDirection.IDLE;
            processRequests(elevator, allElevators, floorButtons, serviceFloors, buttonUp, buttonDown);
        }
    }

    /**
     * Checks if another elevator is already handling the request at a specific floor.
     */
    private boolean isAnotherElevatorHandlingRequest(ElevatorState[] allElevators, int floorRequest, ElevatorState.eDirection directionRequest) {
        for (ElevatorState otherElevator : allElevators) {
            if (otherElevator.direction == directionRequest && otherElevator.targetFloor == floorRequest) {
            	if (otherElevator.direction == eDirection.UP && otherElevator.currentFloor <= floorRequest) {
            		return true; // Another elevator is handling this request
            	} else if (otherElevator.direction == eDirection.DOWN && otherElevator.currentFloor >= floorRequest) {
            		return true; // Another elevator is handling this request
            	} 
            }
        }
        return false; // No elevator is handling this request
    }


    /**
     * Determines if a floor should be serviced based on requests and service availability.
     */
    public boolean shouldServiceFloor(int floor, boolean[] floorButtons, boolean[] buttonUp, boolean[] buttonDown, boolean[] serviceFloors) {
        return (floorButtons[floor] || buttonUp[floor] || buttonDown[floor]) && serviceFloors[floor];
    }
    
    public boolean isInElevatorFloorButtonRequest(int floor, boolean[] floorButtons, boolean[] serviceFloors) {
        return (floorButtons[floor]) && serviceFloors[floor];
    }

}
