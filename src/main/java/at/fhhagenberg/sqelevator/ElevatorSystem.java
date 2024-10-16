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


import java.util.List;
import java.util.ArrayList;

public class ElevatorSystem {
    private List<Elevator> elevators;
    private int numFloors;

    public ElevatorSystem(int numElevators, int numFloors) {
        this.elevators = new ArrayList<>();
        this.numFloors = numFloors;
        
        for (int i = 0; i < numElevators; i++) {
            this.elevators.add(new Elevator(i, numFloors));
        }
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

    public void updateElevators(IElevator plc) throws java.rmi.RemoteException {
        for (Elevator elevator : elevators) {
            elevator.updateFromPLC(plc);
        }
    }
}
