/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorManager.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/

package at.fhhagenberg.sqelevator;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

public class ElevatorManager {
    private IElevator plc;
    private ElevatorSystem elevatorSystem;
    private Timer timer;

    public ElevatorManager(IElevator plc) throws java.rmi.RemoteException {
        this.plc = plc;
        int numElevators = plc.getElevatorNum();
        int numFloors = plc.getFloorNum();
        
        // Create elevator system and publish initial values BEFORE reading values from PLC
        this.elevatorSystem = new ElevatorSystem(numElevators, numFloors);
        publishChanges();
        
        // Create polling timer task
        this.timer = new Timer(true); // Timer runs as a daemon thread
    }

    public void startPolling() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    elevatorSystem.updateElevators(plc);
                    publishChanges();
                } catch (java.rmi.RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 100); // Schedule task every 100ms
    }

    public ElevatorSystem getElevatorSystem() {
        return elevatorSystem;
    }

    public void stopPolling() {
        timer.cancel();
    }

    private void publishChanges() {
    	
    	if (elevatorSystem.hasFloorButtonUpChanged()) {
    		publishToMQTT("floor/buttonUp", Arrays.toString(elevatorSystem.getFloorButtonUp()));
    	}
    	if (elevatorSystem.hasFloorButtonDownChanged()) {
    		publishToMQTT("floor/buttonDown", Arrays.toString(elevatorSystem.getFloorButtonDown()));
    	}
    		
    	
        for (Elevator elevator : elevatorSystem.getElevators()) {
            int elevatorNumber = elevator.getElevatorNumber();

            // Check and publish individual attribute changes
            if (elevator.hasCommittedDirectionChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/committedDirection", String.valueOf(elevator.getCommittedDirection()));
            }

            if (elevator.hasAccelerationChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/acceleration", String.valueOf(elevator.getAcceleration()));
            }

            if (elevator.hasDoorStatusChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/doorStatus", String.valueOf(elevator.getDoorStatus()));
            }

            if (elevator.hasCurrentFloorChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/currentFloor", String.valueOf(elevator.getCurrentFloor()));
            }

            if (elevator.hasPositionChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/position", String.valueOf(elevator.getPosition()));
            }

            if (elevator.hasSpeedChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/speed", String.valueOf(elevator.getSpeed()));
            }

            if (elevator.hasWeightChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/weight", String.valueOf(elevator.getWeight()));
            }

            if (elevator.hasTargetFloorChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/targetFloor", String.valueOf(elevator.getTargetFloor()));
            }

            if (elevator.haveElevatorButtonsChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/floorButtons", Arrays.toString(elevator.getElevatorButtons()));
            }

            if (elevator.haveServiceFloorsChanged()) {
                publishToMQTT("elevator/" + elevatorNumber + "/serviceFloors", Arrays.toString(elevator.getServiceFloors()));
            }
        }
    }

    // Utility method for publishing MQTT messages
    private void publishToMQTT(String topic, String messageContent) {
        //MqttMessage message = new MqttMessage(messageContent.getBytes());
        //mqttClient.publish(topic, message);
    }
}
