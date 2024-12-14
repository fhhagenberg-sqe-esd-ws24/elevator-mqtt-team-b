package at.fhhagenberg.sqelevator.algorithm;


public interface IElevatorAlgorithm {
    /**
     * Processes the elevator state and updates its target floor and direction.
     *
     * @param elevatorState The current state of the elevator (position, target, direction).
     * @param floorButtons  An array indicating which floor buttons inside the elevator are pressed.
     * @param serviceFloors An array indicating which floors the elevator can service.
     * @param buttonUp      An array indicating which up buttons on the floors are pressed.
     * @param buttonDown    An array indicating which down buttons on the floors are pressed.
     */
    void processRequests(ElevatorState elevatorState, boolean[] floorButtons, boolean[] serviceFloors, boolean[] buttonUp, boolean[] buttonDown);

    /**
     * Determines if a floor should be serviced based on current requests and service availability.
     *
     * @param floor         The floor to check.
     * @param floorButtons  An array indicating which floor buttons inside the elevator are pressed.
     * @param serviceFloors An array indicating which floors the elevator can service.
     * @param buttonUp      An array indicating which up buttons on the floors are pressed.
     * @param buttonDown    An array indicating which down buttons on the floors are pressed.
     * @return True if the elevator should stop at the specified floor, false otherwise.
     */
    boolean shouldServiceFloor(int floor, boolean[] floorButtons, boolean[] serviceFloors, boolean[] buttonUp, boolean[] buttonDown);
}

