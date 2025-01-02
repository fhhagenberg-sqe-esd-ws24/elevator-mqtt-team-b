
package at.fhhagenberg.sqelevator.algorithm;

import java.util.Random;
import java.util.Properties;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainAlgorithm {

    public static void main(String[] args) {

        String clientId = "Algorithm";
        Properties properties = new Properties();
        String mqttUrl = properties.getProperty("mqtt.broker.url", "tcp://localhost:1883");

        // Set log level for the Paho MQTT client
        Logger logger = Logger.getLogger("org.eclipse.paho.mqttv5.client");
        logger.setLevel(Level.SEVERE); // Only log SEVERE messages
        
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        ElevatorMqttRouter router;
        try {
            router = new ElevatorMqttRouter(mqttUrl, clientId, algorithm);
            router.connect();
        } catch (Exception e) {
        	logger.log(Level.SEVERE, e.getMessage());
        }

        while (true);
    }
}


