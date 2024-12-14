
package at.fhhagenberg.sqelevator.algorithm;

import java.util.ArrayList;
import java.util.Arrays;

public class ElevatorState {
    public enum eDirection {
        IDLE(2),
        UP(0),
        DOWN(1);

        private final int value;

        eDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public int currentFloor;
    public int targetFloor;
    // previous is necessary to determine if algorithm changed the property
    public int previousTargetFloor;
    // previous is necessary to determine if algorithm changed the property
    public int previousDirection; // 0 = UP, 1 = DOWN, 2 = UNCOMMITTED
    public boolean[] serviceFloors;
    public boolean[] floorButtons;

    public eDirection direction;

    public ElevatorState(int numberOfFloors) {
        serviceFloors = new boolean[numberOfFloors];
        floorButtons = new boolean[numberOfFloors];
        Arrays.fill(this.serviceFloors, true);
        Arrays.fill(this.floorButtons, true);
        this.currentFloor = 0;
        this.targetFloor = 0;
        this.direction = eDirection.IDLE; // Start as uncommitted
    }

    public boolean hasTargetFloorChanged() {
        if(previousTargetFloor != targetFloor) {
            previousTargetFloor = targetFloor;
            return true;
        }
        else {
            return false;
        }
    }
    public boolean hasDirectionChanged() {
        if(direction != previousDirection ) {
            previousDirection = direction;
            return true;
        }
        else {
            return false;
        }
    }


}
