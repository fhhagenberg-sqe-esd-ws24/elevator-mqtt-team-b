/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      Elevator.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/

package at.fhhagenberg.sqelevator;

import java.util.Arrays;

public class Elevator {
    private int elevatorNumber;
    private int committedDirection;
    private int acceleration;
    private int doorStatus;
    private int currentFloor;
    private int position;
    private int speed;
    private int weight;
    private int capacity;
    private int targetFloor;
    private boolean[] floorButtons;
    private boolean[] serviceFloors;

    // Previous state (used for comparison)
    private int prevCommittedDirection = -1;
    private int prevAcceleration = -1;
    private int prevDoorStatus = -1;
    private int prevCurrentFloor = -1;
    private int prevPosition = -1;
    private int prevSpeed = -1;
    private int prevWeight = -1;
    private int prevTargetFloor = -1;
    private boolean[] prevFloorButtons = null;
    private boolean[] prevServiceFloors = null;

    public Elevator(int elevatorNumber, int numFloors) {
        this.elevatorNumber = elevatorNumber;
        this.floorButtons = new boolean[numFloors];
        this.serviceFloors = new boolean[numFloors];

        // Initialize previous state arrays
        this.prevFloorButtons = new boolean[numFloors];
        this.prevServiceFloors = new boolean[numFloors];
        
        // Set prev initial values != current initial values to trigger hasChanged on first change
        Arrays.fill(this.prevFloorButtons, true);
        Arrays.fill(this.prevServiceFloors, true);
    }

    // Getters for each attribute
    public int getElevatorNumber() {
        return elevatorNumber;
    }

    public int getCommittedDirection() {
        return committedDirection;
    }

    public int getAcceleration() {
        return acceleration;
    }

    public int getDoorStatus() {
        return doorStatus;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getPosition() {
        return position;
    }

    public int getSpeed() {
        return speed;
    }

    public int getWeight() {
        return weight;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public boolean[] getFloorButtons() {
        return floorButtons.clone(); // Return a copy to preserve encapsulation
    }

    public boolean[] getServiceFloors() {
        return serviceFloors.clone();
    }

    // Update the elevator state from the PLC
    public void updateFromPLC(IElevator plc) throws java.rmi.RemoteException {
        // Store the previous state before updating
        prevCommittedDirection = committedDirection;
        prevAcceleration = acceleration;
        prevDoorStatus = doorStatus;
        prevCurrentFloor = currentFloor;
        prevPosition = position;
        prevSpeed = speed;
        prevWeight = weight;
        prevTargetFloor = targetFloor;
        prevFloorButtons = floorButtons.clone();
        prevServiceFloors = serviceFloors.clone();

        // Update the current state
        committedDirection = plc.getCommittedDirection(elevatorNumber);
        acceleration = plc.getElevatorAccel(elevatorNumber);
        doorStatus = plc.getElevatorDoorStatus(elevatorNumber);
        currentFloor = plc.getElevatorFloor(elevatorNumber);
        position = plc.getElevatorPosition(elevatorNumber);
        speed = plc.getElevatorSpeed(elevatorNumber);
        weight = plc.getElevatorWeight(elevatorNumber);
        capacity = plc.getElevatorCapacity(elevatorNumber);
        targetFloor = plc.getTarget(elevatorNumber);

        for (int i = 0; i < floorButtons.length; i++) {
            floorButtons[i] = plc.getElevatorButton(elevatorNumber, i);
            serviceFloors[i] = plc.getServicesFloors(elevatorNumber, i);
        }
    }

    // Individual checks for each elevator attribute
    public boolean hasCommittedDirectionChanged() {
        return committedDirection != prevCommittedDirection;
    }

    public boolean hasAccelerationChanged() {
        return acceleration != prevAcceleration;
    }

    public boolean hasDoorStatusChanged() {
        return doorStatus != prevDoorStatus;
    }

    public boolean hasCurrentFloorChanged() {
        return currentFloor != prevCurrentFloor;
    }

    public boolean hasPositionChanged() {
        return position != prevPosition;
    }

    public boolean hasSpeedChanged() {
        return speed != prevSpeed;
    }

    public boolean hasWeightChanged() {
        return weight != prevWeight;
    }

    public boolean hasTargetFloorChanged() {
        return targetFloor != prevTargetFloor;
    }

    public boolean haveFloorButtonsChanged() {
        return !Arrays.equals(floorButtons, prevFloorButtons);
    }

    public boolean haveServiceFloorsChanged() {
        return !Arrays.equals(serviceFloors, prevServiceFloors);
    }

    // General method to check if any state has changed
    public boolean hasStateChanged() {
        return hasCommittedDirectionChanged() || hasAccelerationChanged() || hasDoorStatusChanged() ||
               hasCurrentFloorChanged() || hasPositionChanged() || hasSpeedChanged() ||
               hasWeightChanged() || hasTargetFloorChanged() || haveFloorButtonsChanged() ||
               haveServiceFloorsChanged();
    }
}
