
package at.fhhagenberg.sqelevator.algorithm;

import java.util.Random;

import java.util.Properties;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class Main {
    public static void main(String[] args) {
    	
        String clientId = "Algorithm";
        Properties properties = new Properties();
        String mqttUrl = properties.getProperty("mqtt.broker.url", "tcp://localhost:1883");
        
    	ElevatorMqttRouter router = new ElevatorMqttRouter(mqttUrl, clientId);
    	router.connect();
    	
/*
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm(numElevators, numFloors);

        // Simulate elevator updates and requests
        Random random = new Random();
        for (int t = 0; t < 20; t++) { // Simulate 20 time steps
            for (int i = 0; i < numElevators; i++) {
                boolean[] requests = new boolean[numFloors];
                for (int j = 0; j < numFloors; j++) {
                    requests[j] = random.nextBoolean();
                }
                algorithm.updateState(i, random.nextInt(numFloors), random.nextInt(3), requests);
            }
            algorithm.processRequests();

            // Display the state of each elevator
            for (int i = 0; i < numElevators; i++) {
                ElevatorState state = algorithm.getElevatorStates().get(i);
                System.out.printf("Elevator %d: Floor %d -> Target %d, Direction %d\n",
                        i, state.currentFloor, state.targetFloor, state.direction);
            }
            System.out.println();
        }
    }*/
}

