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

package at.fhhagenberg.sqelevator.adapter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;
import java.util.Properties;
import java.io.IOException;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import sqelevator.IElevator;

public class ElevatorManager {
    private ElevatorSystem elevatorSystem;
    private Timer timer;
    private MqttClient mqttClient;
    private String clientId = "ElevatorManager";
    private long timerPeriod;
    private boolean doRestart = false;

	public ElevatorManager(IElevator plc, Properties properties) throws java.rmi.RemoteException, MqttException, IOException 
    {
        // Get properties
        String mqttUrl = properties.getProperty("mqtt.url", "tcp://localhost:1883");
        timerPeriod = Long.parseLong(properties.getProperty("timer.period", "100"));

        // Initialize the MQTT client 
        mqttClient = new MqttClient(mqttUrl, clientId, new MemoryPersistence());

        // Create elevator system and publish initial values BEFORE reading values from PLC
        this.elevatorSystem = new ElevatorSystem(plc);
        
        // Create polling timer task
        this.timer = new Timer(true); // Timer runs as a daemon thread
    }

	public void startPolling() {
	    // Retry logic for connecting to the MQTT broker
	    boolean connected = false;
	    long retryDelay = 5000; // Delay between retries in milliseconds
	    
	    MqttConnectionOptions options = new MqttConnectionOptions();
	    options.setCleanStart(true);

	    doRestart = false;

	    while (!connected) {
	        try {
	            // Attempt to connect to the MQTT broker
	            mqttClient.connect(options);
	            connected = true;
	    		publishToMQTT("system/rmi/connected", String.valueOf(1));
	        } catch (MqttException e) {
                try {
                    Thread.sleep(retryDelay); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Retry interrupted");
                    doRestart = true;
                    return;
                }
	        }
	    }

	    // Proceed with the rest of the method after successful connection
	    try {
	        startAsyncSubscription();

	        // Initial publish after connect
	        initialPublish();

	        // Schedule periodic tasks
	        timer.scheduleAtFixedRate(new TimerTask() {
	            @Override
	            public void run() {
	                try {
	                    elevatorSystem.updateElevators();
	                    publishChanges(false);
	                } catch (java.rmi.RemoteException e) {
	                    //e.printStackTrace();
	                	System.out.println("RMI Exception - Do restart...");
	                	timer.cancel();
	                    doRestart = true;
	                }
	            }
	        }, 0, timerPeriod); // Schedule task with configurable period

	    } catch (Exception e) {
	    	System.out.println("Exception in scheduleAtFixedRate");
	        doRestart = true;
	    }
	}

	public boolean doRestart()
	{
		return doRestart;
	}

    public void stopPolling() { 
    	timer.cancel();
    	publishToMQTT("system/rmi/connected", String.valueOf(0));
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
    	
    	for (Elevator elevator : elevatorSystem.getElevators()) {
            int elevatorNumber = elevator.getElevatorNumber();
            publishToMQTT("system/elevator/" + elevatorNumber + "/capacity", String.valueOf(elevator.getCapacity()));       
    	}
    	
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
                message.setQos(2); //exactly once delivery
                message.setRetained(true); // Set retain flag to true
                mqttClient.publish(topic, message);
            }
        } catch (MqttException e) {
        	System.out.println("Exception in publishToMQTT");
       	 	doRestart = true;
            //e.printStackTrace();
        }
    }  
    
    // Register callback for incoming messages
    private void startAsyncSubscription() throws MqttException {
        mqttClient.setCallback(new MqttCallback() {
             @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            	 System.out.println("Exception in disconnected");
            	 doRestart = true;
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            	System.out.println("Exception in mqttErrorOccurred");
                //exception.printStackTrace();
                doRestart = true;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleIncomingMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                // Not used for subscriptions
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                // Handle connection complete
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Handle auth packet arrived
            }
        });

        // Subscribe to relevant topics
        mqttClient.subscribe("system/elevator/set/+/committedDirection", 2);
        mqttClient.subscribe("system/elevator/set/+/serviceFloor/+", 2);
        mqttClient.subscribe("system/elevator/set/+/target", 2);
    }
    
    private void handleIncomingMessage(String topic, MqttMessage message) {
    	System.out.println("Incomming message: " + topic + "/" + message);
        try {
            String[] parts = topic.split("/");
            if (parts.length < 5) {
                System.err.println("Invalid topic: " + topic);
                return;
            }

            int elevatorNumber = Integer.parseInt(parts[3]);
            
            switch (parts[4]) {
                case "committedDirection":
                    int direction = Integer.parseInt(new String(message.getPayload()));
                    elevatorSystem.setCommittedDirection(elevatorNumber, direction);
                    break;

                case "serviceFloor":
                    if (parts.length == 6) {
                        int floor = Integer.parseInt(parts[5]);
                        boolean service = Boolean.parseBoolean(new String(message.getPayload()));
                        elevatorSystem.setServicesFloors(elevatorNumber, floor, service);
                    }
                    break;

                case "target":
                    int target = Integer.parseInt(new String(message.getPayload()));
                    elevatorSystem.setTarget(elevatorNumber, target);
                    break;

                default:
                    System.err.println("Unhandled topic: " + topic);
                    break;
            }
        } catch (NumberFormatException | java.rmi.RemoteException e) {
            e.printStackTrace();
            System.out.println("Exception in handleIncomingMessage");
            doRestart = true;
        }  
    }  
}
