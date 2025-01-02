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

package at.fhhagenberg.sqelevator.adapter;

import java.util.List;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import sqelevator.IElevator;

public class ElevatorSystem {
	private IElevator plc;
    private List<Elevator> elevators;
    private int numElevators;
    private int numFloors;
    private int floorHeight;
    private boolean[] floorButtonUp;
    private boolean[] floorButtonDown;
    
    private boolean[] prevFloorButtonUp;
    private boolean[] prevFloorButtonDown;

    public ElevatorSystem(IElevator plc) throws java.rmi.RemoteException { 	
    	this.plc = plc;
    	
        this.numElevators = plc.getElevatorNum();
        this.numFloors = plc.getFloorNum();
        this.floorHeight = plc.getFloorHeight();
        this.elevators = new ArrayList<>();
        
        for (int i = 0; i < numElevators; i++) {
            this.elevators.add(new Elevator(plc, i, numFloors));
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
    
    public int getNumElevator()
    {
    	return numElevators;
    }

    public int getNumFloors() {
        return numFloors;
    }
    
    public int getFloorHeight() {
        return floorHeight;
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
    
    public void updateElevators() throws java.rmi.RemoteException {
    	prevFloorButtonUp = floorButtonUp.clone();
    	prevFloorButtonDown = floorButtonDown.clone();
    	
        for (int i = 0; i < numFloors; i++) {
        	floorButtonUp[i] = plc.getFloorButtonUp(i);
            floorButtonDown[i] = plc.getFloorButtonDown(i);
        }
        
        for (Elevator elevator : elevators) {
            elevator.updateFromPLC();
        }
    }
    
    public void setCommittedDirection(int elevatorNumber, int direction) throws RemoteException {
    	
    	if(plc.getCommittedDirection(elevatorNumber) != direction)
    	{
    		plc.setCommittedDirection(elevatorNumber, direction);    		
    	}  	
    	else
    	{
    		System.out.println("ignored - already set (setCommittedDirection)");
    	}
    }
    
    public void setServicesFloors(int elevatorNumber, int floor, boolean service) throws RemoteException {
    	if(plc.getServicesFloors(elevatorNumber, floor) != service)
    	{
    		plc.setServicesFloors(elevatorNumber, floor, service);
    	}
    	else
    	{
    		System.out.println("ignored - already set (setServicesFloors)");
    	}
    }
    
    public void setTarget(int elevatorNumber, int target) throws RemoteException {
    	if(plc.getTarget(elevatorNumber) != target)
    	{
    		plc.setTarget(elevatorNumber, target);
    	}
    	else
    	{
    		System.out.println("ignored - already set (setTarget)");
    	}
    }
}
