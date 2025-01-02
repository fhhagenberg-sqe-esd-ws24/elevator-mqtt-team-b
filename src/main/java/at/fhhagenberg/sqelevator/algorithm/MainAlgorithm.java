package at.fhhagenberg.sqelevator.algorithm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainAlgorithm {

    private static final Logger restartLogger = Logger.getLogger("RestartLogger");

    static {
        try {
            // Configure the logger to write to a file
            FileHandler fileHandler = new FileHandler("restart_log_algorithm.txt", true); // Append mode
            restartLogger.addHandler(fileHandler);
            restartLogger.setUseParentHandlers(false); // Prevent console logging
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        String clientId = "Algorithm";
        Properties properties = new Properties();
        String mqttUrl = properties.getProperty("mqtt.broker.url", "tcp://localhost:1883");

        // Set log level for the Paho MQTT client
        Logger logger = Logger.getLogger("org.eclipse.paho.mqttv5.client");
        logger.setLevel(Level.SEVERE); // Only log SEVERE messages

        while (true) {
            ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
            ElevatorMqttRouter router;
            try {
                router = new ElevatorMqttRouter(mqttUrl, clientId, algorithm);
                router.connect();

                while (!router.doRestart()) {
                    Thread.sleep(1000);
                }

                // Log the restart with a timestamp
                logRestart();

                router.disconnect();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    private static void logRestart() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        restartLogger.log(Level.INFO, "System restarted at: " + timestamp);
        System.out.println("Logged restart at: " + timestamp);
    }
}
