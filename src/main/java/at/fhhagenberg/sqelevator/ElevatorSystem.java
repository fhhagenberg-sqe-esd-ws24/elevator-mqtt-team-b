/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorSystem.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/

package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ElevatorSystem {
    private List<Elevator> elevators;
    private int numFloors;
    private boolean[] floorButtonUp;
    private boolean[] floorButtonDown;
    
    private boolean[] prevFloorButtonUp;
    private boolean[] prevFloorButtonDown;

    public ElevatorSystem(int numElevators, int numFloors) {
        this.elevators = new ArrayList<>();
        this.numFloors = numFloors;
        
        for (int i = 0; i < numElevators; i++) {
            this.elevators.add(new Elevator(i, numFloors));
        }
        
        this.floorButtonUp = new boolean[numFloors];
        this.floorButtonDown = new boolean[numFloors];
        this.prevFloorButtonUp = new boolean[numFloors];
        this.prevFloorButtonDown = new boolean[numFloors];
        
        // Set prev initial values != current initial values to trigger hasChanged on first change
        Arrays.fill(this.prevFloorButtonUp, true);
        Arrays.fill(this.prevFloorButtonDown, true);
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public Elevator getElevator(int elevatorNumber) {
        return elevators.get(elevatorNumber);
    }

    public int getNumFloors() {
        return numFloors;
    }

    public boolean[] getFloorButtonUp() {
        return floorButtonUp.clone(); 
    }

    public boolean[] getFloorButtonDown() {
        return floorButtonDown.clone(); 
    }
    
    public boolean hasFloorButtonUpChanged() {
        return !Arrays.equals(floorButtonUp, prevFloorButtonUp);
    }

    public boolean hasFloorButtonDownChanged() {
        return !Arrays.equals(floorButtonDown, prevFloorButtonDown);
    }
    
    public void updateElevators(IElevator plc) throws java.rmi.RemoteException {
    	prevFloorButtonUp = floorButtonUp.clone();
    	prevFloorButtonDown = floorButtonDown.clone();
    	
        for (int i = 0; i < numFloors; i++) {
        	floorButtonUp[i] = plc.getFloorButtonUp(i);
            floorButtonDown[i] = plc.getFloorButtonDown(i);
        }
        
        for (Elevator elevator : elevators) {
            elevator.updateFromPLC(plc);
        }
    }
}
