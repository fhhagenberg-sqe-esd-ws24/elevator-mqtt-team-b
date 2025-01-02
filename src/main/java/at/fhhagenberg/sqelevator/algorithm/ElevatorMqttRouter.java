
package at.fhhagenberg.sqelevator.algorithm;

import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Arrays;
import java.util.Objects;


public class ElevatorMqttRouter {

    private final MqttClient mqttClient;
	private boolean isSetupPhase = true;
	private int numElevators = 0;
	private int numFloors = 0;
	private int floorHeight = 0;

	private boolean[] buttonUp;
	private boolean[] buttonDown;
    private ElevatorState[] elevators;
    private IElevatorAlgorithm algorithm;
    private boolean doRestart = false;

    public boolean isSetupPhase() {
        return isSetupPhase;
    }

    public ElevatorMqttRouter(MqttClient mqttClient, IElevatorAlgorithm algorithm) throws MqttException {
        this.mqttClient = mqttClient;
        this.algorithm = algorithm;
    }

    public void connect() throws MqttException {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                System.out.println("Disconnected from MQTT broker.");
                doRestart = true;
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            	System.out.println("Exception in mqttErrorOccurred");
                doRestart = true;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleIncomingMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                System.out.println("Message delivery complete.");
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("Connected to MQTT broker: " + serverURI);
                isSetupPhase = true;
                try 
                {
                	subscribeToSetupTopics();
                }
                catch(MqttException e)
                {
                	System.out.println("Exception in connectComplete");
                    doRestart = true;
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Not used
            }
        });

        mqttClient.connect(options);
    }
    
    private void subscribeToSetupTopics() throws MqttException {
    	mqttClient.subscribe("system/numElevator", 2);
    	mqttClient.subscribe("system/numFloors", 2);
    	mqttClient.subscribe("system/floorHeight", 2);
        System.out.println("Subscribed to MQTT setup topics.");
    }

    private void subscribeTocontinuousTopics() throws MqttException {
        // Monitor RMI connection
        mqttClient.subscribe("system/rmi/connected", 2);
    	// Subscribe to current elevator floor 
        mqttClient.subscribe("system/elevator/+/currentFloor", 2);
        // Subscribe to targeted floor (by PLC)
        // mqttClient.subscribe("system/elevator/+/targetFloor/+", 2);
        // Subscribe to floor button outside the elevator to call the elevator
        mqttClient.subscribe("system/floor/buttonUp", 2);
        mqttClient.subscribe("system/floor/buttonDown", 2);
        // Subscribe to floor button pressed inside the elevator (target)
        mqttClient.subscribe("system/elevator/+/floorButtons", 2);
        // Subscribe to set service floors that are ignored/skipped
        mqttClient.subscribe("system/elevator/+/serviceFloor", 2);
        // Subscribe to door status to check if person has left elevator
        mqttClient.subscribe("system/elevator/+/doorStatus", 2);

        System.out.println("Subscribed to MQTT continuous topics.");
    }

    public void handleIncomingMessage(String topic, MqttMessage message) {
        System.out.println("<= Incoming message: " + topic + " => " + new String(message.getPayload()));

    	// Split and check errors
        String[] parts = topic.split("/");
        if (parts.length < 2) {
            System.err.println("Invalid topic: " + topic);
            return;
        }
        
        // Setup-phase: Build up elevator modell (system, elevators, ...)
        try
        {
	        if (isSetupPhase)
	        {
	        	isSetupPhase = parseSetupMessage(parts, new String(message.getPayload()));
	            if (!isSetupPhase)
	            {
	                try {
	                    subscribeTocontinuousTopics();
	                } catch (MqttException e) {
                        System.out.println("subscribeTocontinuousTopics failed");
	                }

	                elevators = new ElevatorState[numElevators];
	                for (int i = 0; i < numElevators; i++) {
	                    elevators[i] = new ElevatorState(numFloors);
	                }
	                
	                buttonUp = new boolean[numFloors];
	                buttonDown = new boolean[numFloors];
	                Arrays.fill(buttonUp, false);
	                Arrays.fill(buttonDown, false);
	                
	                System.out.println("Setup completed");
	            }
	        }
	        else
	        {
	        	isSetupPhase = parseContinuousMessage(parts, new String(message.getPayload()));
	        }
        } 
        catch (NumberFormatException e)
        {
        }
    }

    public void publishToMQTT(String topic, String messageContent) {
        try {
            if (mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(messageContent.getBytes());
                message.setQos(2); //exactly once delivery
                message.setRetained(true); // Set retain flag to true
                mqttClient.publish(topic, message);
                System.out.println("=> Sent message: " + topic + " => " + new String(message.getPayload()));
            }
        } catch (MqttException e) {
        	System.out.println("Exception in publishToMQTT");
            doRestart = true;
        }
    }

    private void publishChangedTopics(int elevatorIndex) {
        if(elevators[elevatorIndex].hasDirectionChanged()) {
            publishToMQTT("system/elevator/set/" + elevatorIndex + "/committedDirection", String.valueOf(elevators[elevatorIndex].direction.getValue()));
        }
        if(elevators[elevatorIndex].hasTargetFloorChanged()) {
            publishToMQTT("system/elevator/set/" + elevatorIndex + "/target", String.valueOf(elevators[elevatorIndex].targetFloor));
        }
    }
    
    private boolean parseSetupMessage(String [] mqttTopic, String mqttPayload)
    {
    	if ((mqttTopic.length != 2) || (mqttPayload.isEmpty()))
    	{
    		System.err.println("Unhandled setup message: " + Arrays.toString(mqttTopic));
    		return true;
    	}
    	
    	// Handle message
        switch (mqttTopic[1]) {
            case "numElevator":
                numElevators = Integer.parseInt(mqttPayload);
                break;
            case "numFloors":
            	numFloors = Integer.parseInt(mqttPayload);
                break;
            case "floorHeight":
            	floorHeight = Integer.parseInt(mqttPayload);
                break;
            default:
                System.err.println("Unhandled topic: " + mqttTopic);
                break;
        }
        
        // Check if setup done is done
        if ((numElevators != 0) && (numFloors != 0) && (floorHeight != 0))
        {
        	return false;
        }
        else
        {
        	return true;
        }
    }
   
    private boolean parseContinuousMessage(String[]mqttTopic, String mqttPayload)
    {
    	String[] strPayload;
    	
    	if ((mqttTopic.length < 2) || (mqttPayload.isEmpty()) || !Objects.equals(mqttTopic[0], "system"))
    	{
    		System.err.println("Unhandled continuous message: " + Arrays.toString(mqttTopic));
    		return true;
    	}


    	if (Objects.equals(mqttTopic[1], "elevator"))
    	{
        	if (mqttTopic.length != 4)
        	{
        		System.err.println("Unhandled continuous message: " + Arrays.toString(mqttTopic));
        		return true;
        	}
    		
    		int elevatorNumber = Integer.parseInt(mqttTopic[2]);
    		
    		// Elevator message
            switch (mqttTopic[3])
            {
	            case "currentFloor":
	            	elevators[elevatorNumber].currentFloor = Integer.parseInt(mqttPayload);
	                break;
	
	            case "floorButtons":
					strPayload = mqttPayload.replace("[", "").replace("]", "").split(",");
	            	for (int i = 0; i < strPayload.length; i++) {
	            		elevators[elevatorNumber].floorButtons[i] = Boolean.parseBoolean(strPayload[i].trim());
	            	}
	                break;
	
	            case "serviceFloor":
	            	strPayload = mqttPayload.replace("[", "").replace("]", "").split(",");
	                for (int i = 0; i < strPayload.length; i++) {
                        elevators[elevatorNumber].serviceFloors[i] = Boolean.parseBoolean(strPayload[i].trim());
	                }
	                break;
	                
	            case "doorStatus":
	            	elevators[elevatorNumber].doorStatus = ElevatorState.eDoorStatus.fromValue(Integer.parseInt(mqttPayload));
	                break;
	                
	            default:
	                System.err.println("Unhandled topic: " + mqttTopic);
	                break;
	        }
            algorithm.processRequests(elevators[elevatorNumber], elevators, elevators[elevatorNumber].floorButtons, elevators[elevatorNumber].serviceFloors, buttonUp, buttonDown);
            publishChangedTopics(elevatorNumber);

    	}
    	else if (Objects.equals(mqttTopic[1], "floor"))
    	{
        	if (mqttTopic.length != 3)
        	{
        		System.err.println("Unhandled continuous message: " + Arrays.toString(mqttTopic));
        		return true;
        	}
    		
    		// Floor message
    		switch (mqttTopic[2]) 
    		{
	            case "buttonUp":
	            	strPayload = mqttPayload.replace("[", "").replace("]", "").split(",");
	            	for (int i = 0; i < strPayload.length; i++) {
	            	    buttonUp[i] = Boolean.parseBoolean(strPayload[i].trim());
	            	}
	                break;
	            case "buttonDown":
					strPayload = mqttPayload.replace("[", "").replace("]", "").split(",");
	            	for (int i = 0; i < strPayload.length; i++) {
	            	    buttonDown[i] = Boolean.parseBoolean(strPayload[i].trim());
	            	}
	                break;
	            default:
	                System.err.println("Unhandled topic: " + mqttTopic);
	                break;
	        }

            for(int i = 0; i < numElevators; i++) {
                if(elevators[i].direction == ElevatorState.eDirection.IDLE) {
                    algorithm.processRequests(elevators[i], elevators, elevators[i].floorButtons, elevators[i].serviceFloors, buttonUp, buttonDown);
                    publishChangedTopics(i);
                    break;
                }
            }
    	}
    	else if (Objects.equals(mqttTopic[1], "rmi"))
    	{
        	if (mqttTopic.length != 3 || !Objects.equals(mqttTopic[2], "connected"))
        	{
        		System.err.println("Unhandled continuous message: " + Arrays.toString(mqttTopic));
        		return true;
        	}
        	if (Objects.equals(mqttPayload, "0"))
        	{
        		doRestart = true;
        	}
    	}
        return false;
    }

    public void disconnect() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                System.out.println("Disconnected from MQTT broker.");
            }
        } catch (MqttException e) {
        }
    }
    
    public boolean doRestart()
    {
    	return doRestart;
    }
}
