
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
    
    public enum eDoorStatus {
        OPEN(1),
        CLOSED(2),
    	OPENING(3),
    	CLOSING(4);

        private final int value;

        eDoorStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
        
        public static eDoorStatus fromValue(int value) {
            for (eDoorStatus status : eDoorStatus.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    public int currentFloor;
    public int targetFloor;
    // previous is necessary to determine if algorithm changed the property
    public int previousTargetFloor;
    // previous is necessary to determine if algorithm changed the property
    public boolean[] serviceFloors;
    public boolean[] floorButtons;

    public eDirection previousDirection; // 0 = UP, 1 = DOWN, 2 = UNCOMMITTED
    public eDirection direction;
    
    public eDoorStatus doorStatus;
    public boolean isReadyForNextTarget;

    public ElevatorState(int numberOfFloors) {
        serviceFloors = new boolean[numberOfFloors];
        floorButtons = new boolean[numberOfFloors];
        Arrays.fill(this.serviceFloors, true);
        Arrays.fill(this.floorButtons, true);
        this.currentFloor = 0;
        this.targetFloor = 0;
        this.direction = eDirection.IDLE; // Start as uncommitted
        this.doorStatus = eDoorStatus.CLOSED;
        this.isReadyForNextTarget = true;
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
