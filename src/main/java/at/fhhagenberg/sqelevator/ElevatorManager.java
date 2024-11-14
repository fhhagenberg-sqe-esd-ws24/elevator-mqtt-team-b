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
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;

public class ElevatorManager {
    private ElevatorSystem elevatorSystem;
    private Timer timer;
    private MqttClient mqttClient;
    private String clientId = "Elevator";
    private String mqttUrl;
    private long timerPeriod;

    public ElevatorManager(IElevator plc, Properties properties) throws java.rmi.RemoteException, MqttException, IOException {
        // Get properties
        mqttUrl = properties.getProperty("mqtt.url", "tcp://localhost:1883");
        timerPeriod = Long.parseLong(properties.getProperty("timer.period", "100"));

        // Initialize the MQTT client 
        mqttClient = new MqttClient(mqttUrl, clientId, new MemoryPersistence());

        // Create elevator system and publish initial values BEFORE reading values from PLC
        this.elevatorSystem = new ElevatorSystem(plc);
        
        // Create polling timer task
        this.timer = new Timer(true); // Timer runs as a daemon thread
    }

    public void startPolling() throws MqttException {
	    MqttConnectionOptions options = new MqttConnectionOptions();
	    options.setAutomaticReconnect(true);
	    options.setCleanStart(true);  
	    
	    // connect to mqtt broker
	    mqttClient.connect(options);
	    
	    // initial publish after connect
        initialPublish();
	    	
		timer.scheduleAtFixedRate(new TimerTask() {
		    @Override
		    public void run() {
		        try {
		            elevatorSystem.updateElevators();
		            publishChanges(false);
		        } catch (java.rmi.RemoteException e) {
		            e.printStackTrace();
		        }
		    }
        }, 0, timerPeriod); // Schedule task with configurable period
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
    	
        publishChanges(true);
    }
        
    private void publishChanges(boolean doForce) {
    	
    	if (doForce || elevatorSystem.hasFloorButtonUpChanged()) {
    		publishToMQTT("system/floor/buttonUp", Arrays.toString(elevatorSystem.getFloorButtonUp()));
    	}
    	if (doForce || elevatorSystem.hasFloorButtonDownChanged()) {
    		publishToMQTT("system/floor/buttonDown", Arrays.toString(elevatorSystem.getFloorButtonDown()));
    	}
    		
    	
        for (Elevator elevator : elevatorSystem.getElevators()) {
            int elevatorNumber = elevator.getElevatorNumber();

            // Check and publish individual attribute changes
            if (doForce || elevator.hasCommittedDirectionChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/committedDirection", String.valueOf(elevator.getCommittedDirection()));
            }

            if (doForce || elevator.hasAccelerationChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/acceleration", String.valueOf(elevator.getAcceleration()));
            }

            if (doForce || elevator.hasDoorStatusChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/doorStatus", String.valueOf(elevator.getDoorStatus()));
            }

            if (doForce || elevator.hasCurrentFloorChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/currentFloor", String.valueOf(elevator.getCurrentFloor()));
            }

            if (doForce || elevator.hasPositionChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/position", String.valueOf(elevator.getPosition()));
            }

            if (doForce || elevator.hasSpeedChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/speed", String.valueOf(elevator.getSpeed()));
            }

            if (doForce || elevator.hasWeightChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/weight", String.valueOf(elevator.getWeight()));
            }

            if (doForce || elevator.hasTargetFloorChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/targetFloor", String.valueOf(elevator.getTargetFloor()));
            }

            if (doForce || elevator.haveElevatorButtonsChanged()) {
                publishToMQTT("system/elevator/" + elevatorNumber + "/floorButtons", Arrays.toString(elevator.getElevatorButtons()));
            }

            if (doForce || elevator.haveServiceFloorsChanged()) {
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
