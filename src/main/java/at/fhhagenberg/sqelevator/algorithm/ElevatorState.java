
package at.fhhagenberg.sqelevator.algorithm;

public class ElevatorState {
    public int currentFloor;
    public int targetFloor;
    public int direction; // 0 = UP, 1 = DOWN, 2 = UNCOMMITTED
    public boolean[] requests;

    public ElevatorState(int numFloors) {
        this.requests = new boolean[numFloors];
        this.currentFloor = 0;
        this.targetFloor = 0;
        this.direction = 2; // Start as uncommitted
    }
}
