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
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;

public class ElevatorManager {
    private ElevatorSystem elevatorSystem;
    private Timer timer;
    private MqttClient mqttClient;
    private String clinetId = "Elevator";
    
    public ElevatorManager(IElevator plc) throws java.rmi.RemoteException, MqttException {
        // Initialize the MQTT client 
        mqttClient = new MqttClient("tcp://localhost:1883", clinetId, new MemoryPersistence());
                       
        // Create elevator system and publish initial values BEFORE reading values from PLC
        this.elevatorSystem = new ElevatorSystem(plc);
        initialPublish();
        publishChanges();
        
        // Create polling timer task
        this.timer = new Timer(true); // Timer runs as a daemon thread
    }

    public void startPolling() throws MqttException {
	    // connect to mqtt
	    MqttConnectionOptions options = new MqttConnectionOptions();
	    options.setAutomaticReconnect(true);
	    options.setCleanStart(true);  
	 
	    mqttClient.connect(options);
	    	
		timer.scheduleAtFixedRate(new TimerTask() {
		    @Override
		    public void run() {
		        try {
		            elevatorSystem.updateElevators();
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
        if (mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialPublish()
    {
    	publishToMQTT("system/numElevator", String.valueOf(elevatorSystem.getNumElevator()));
    	publishToMQTT("system/numFloors", String.valueOf(elevatorSystem.getNumFloors()));	    	
    	publishToMQTT("system/floorHeight", String.valueOf(elevatorSystem.getFloorHeight()));	
    }
        
    private void publishChanges() {
    	
    	if (elevatorSystem.hasFloorButtonUpChanged()) {
    		publishToMQTT("system/floor/buttonUp", Arrays.toString(elevatorSystem.getFloorButtonUp()));
    	}
    	if (elevatorSystem.hasFloorButtonDownChanged()) {
    		publishToMQTT("system/floor/buttonDown", Arrays.toString(elevatorSystem.getFloorButtonDown()));
    	}
    		
    	
        for (Elevator elevator : elevatorSystem.getElevators()) {
            int elevatorNumber = elevator.getElevatorNumber();

            // Check and publish individual attribute changes
            if (elevator.hasCommittedDirectionChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/committedDirection", String.valueOf(elevator.getCommittedDirection()));
            }

            if (elevator.hasAccelerationChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/acceleration", String.valueOf(elevator.getAcceleration()));
            }

            if (elevator.hasDoorStatusChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/doorStatus", String.valueOf(elevator.getDoorStatus()));
            }

            if (elevator.hasCurrentFloorChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/currentFloor", String.valueOf(elevator.getCurrentFloor()));
            }

            if (elevator.hasPositionChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/position", String.valueOf(elevator.getPosition()));
            }

            if (elevator.hasSpeedChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/speed", String.valueOf(elevator.getSpeed()));
            }

            if (elevator.hasWeightChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/weight", String.valueOf(elevator.getWeight()));
            }

            if (elevator.hasTargetFloorChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/targetFloor", String.valueOf(elevator.getTargetFloor()));
            }

            if (elevator.haveElevatorButtonsChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/floorButtons", Arrays.toString(elevator.getElevatorButtons()));
            }

            if (elevator.haveServiceFloorsChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/serviceFloors", Arrays.toString(elevator.getServiceFloors()));
            }
        }
    }

    // Utility method for publishing MQTT messages
    private void publishToMQTT(String topic, String messageContent) {
    	try {
            if (mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(messageContent.getBytes());
                message.setQos(1); //at least once delivery
                message.setRetained(true); // Set retain flag to true
                mqttClient.publish(topic, message);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
