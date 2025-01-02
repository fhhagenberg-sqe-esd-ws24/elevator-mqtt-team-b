package at.fhhagenberg.sqelevator.adapter;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;

import sqelevator.IElevator;

public class MainAdapter {
	private static IElevator controller;
    private static Properties properties;
    private static MqttClient mqttClient;
    private static String brokerUrl;
    private static ElevatorManager elevatorManager;
    
	
	public static void main(String[] args) {
		// System.setSecurityManager(new SecurityManager());
		while(true)
		{
			try {
				// Get properties
		        properties = new Properties();
		        
				String plcUrl = properties.getProperty("plc.url", "rmi://localhost/ElevatorSim");
				
				controller = (IElevator) Naming.lookup(plcUrl);
				displayElevatorSettings();
	
		        // Set log level for the Paho MQTT client
		        Logger logger = Logger.getLogger("org.eclipse.paho.mqttv5.client");
		        logger.setLevel(Level.SEVERE); // Only log SEVERE messages
				
		        // Initialize ElevatorManager
		        elevatorManager = new ElevatorManager(controller, properties);
		        elevatorManager.startPolling();
	
		        while(!elevatorManager.doRestart())
		        {
		        	Thread.sleep(1000);
		        }
		        
		        elevatorManager.stopPolling();
		        
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try
			{
				Thread.sleep(1000);
			} catch (Exception e) {}
		}
	}

	private static void displayElevatorSettings() throws RemoteException {
		System.out.println("ELEVATOR SETTINGS");

		System.out.println("Current clock tick: " + controller.getClockTick());

		System.out.println("Number of elevators: " + controller.getElevatorNum());
		System.out.println("Number of floor: " + controller.getFloorNum());
		System.out.println("Floor height: " + controller.getFloorHeight());

		System.out.print("Floor buttons Up pressed: ");
		for (int floor = 0; floor < controller.getFloorNum(); floor++) {
			System.out.print(controller.getFloorButtonUp(floor) ? "1" : "0");
		}
		System.out.println();
		System.out.print("Floor buttons Down pressed: ");
		for (int floor = 0; floor < controller.getFloorNum(); floor++) {
			System.out.print(controller.getFloorButtonDown(floor) ? "1" : "0");
		}
		System.out.println();

		for (int elevator = 0; elevator < controller.getElevatorNum(); elevator++) {
			System.out.println("Settings of elevator number: " + elevator);
			System.out.println("  Floor: " + controller.getElevatorFloor(elevator));
			System.out.println("  Position: " + controller.getElevatorPosition(elevator));
			System.out.println("  Target: " + controller.getTarget(elevator));
			System.out.println("  Committed direction: " + controller.getCommittedDirection(elevator));
			System.out.println("  Door status: " + controller.getElevatorDoorStatus(elevator));
			System.out.println("  Speed: " + controller.getElevatorSpeed(elevator));
			System.out.println("  Acceleration: " + controller.getElevatorAccel(elevator));
			System.out.println("  Capacity: " + controller.getElevatorCapacity(elevator));
			System.out.println("  Weight: " + controller.getElevatorWeight(elevator));
			System.out.print("  Buttons pressed: ");
			for (int floor = 0; floor < controller.getFloorNum(); floor++) {
				System.out.print(controller.getElevatorButton(elevator, floor) ? "1" : "0");
			}
			System.out.println();
		}
	}
}
