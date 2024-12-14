
package at.fhhagenberg.sqelevator.algorithm;

import java.util.*;

public class ElevatorAlgorithm implements IElevatorAlgorithm {

    public ElevatorAlgorithm() {    }

    public void processRequests(ElevatorState elevator, boolean[] floorButtons, boolean[] serviceFloors, boolean[] buttonUp, boolean[] buttonDown) {
        int numFloors = floorButtons.length;

        // If the elevator is idle , decide the initial direction
        if (elevator.direction == ElevatorState.eDirection.IDLE) {
            // Check for requests above
            for (int i = elevator.currentFloor + 1; i < numFloors; i++) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors)) {
                    elevator.targetFloor = i;
                    elevator.direction = ElevatorState.eDirection.UP; // UP
                    return;
                }
            }
            // Check for requests below
            for (int i = elevator.currentFloor - 1; i >= 0; i--) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors)) {
                    elevator.targetFloor = i;
                    elevator.direction = ElevatorState.eDirection.DOWN; // DOWN
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
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors)) {
                    elevator.targetFloor = i;
                    return;
                }
            }
            // No more requests above, switch direction
            elevator.direction = ElevatorState.eDirection.IDLE;
            processRequests(elevator, floorButtons, serviceFloors, buttonUp, buttonDown);
        }

        // If the elevator is moving DOWN
        if (elevator.direction == ElevatorState.eDirection.DOWN) {
            // Check for requests below
            for (int i = elevator.currentFloor - 1; i >= 0; i--) {
                if (shouldServiceFloor(i, floorButtons, buttonUp, buttonDown, serviceFloors)) {
                    elevator.targetFloor = i;
                    return;
                }
            }
            // No more requests below, switch direction
            elevator.direction = ElevatorState.eDirection.IDLE;
            processRequests(elevator, floorButtons, serviceFloors, buttonUp, buttonDown);
        }
    }

    /**
     * Determines if a floor should be serviced based on requests and service availability.
     */
    public boolean shouldServiceFloor(int floor, boolean[] floorButtons, boolean[] buttonUp, boolean[] buttonDown, boolean[] serviceFloors) {
        return (floorButtons[floor] || buttonUp[floor] || buttonDown[floor]) && serviceFloors[floor];
    }

}
