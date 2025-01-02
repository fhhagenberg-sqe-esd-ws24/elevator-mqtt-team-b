# Elevator Control System

## Overview
This is a Elevator Control System. It provides a simulation and control system for elevators, featuring:

- MQTT integration for real-time communication.
- A modular design with algorithms to handle elevator requests efficiently.
- Comprehensive unit and integration testing.

## Requirements
To run the project, ensure the following are installed:

- **Java 17** or higher
- **Apache Maven** (minimum version 3.6)
- **Docker** (required for integration tests with HiveMQ)
- An MQTT broker (default: mosquitto)
- A simulated or real implementation of the `IElevator` interface

## Downloading and Running the Release

### Download the Release
To download the precompiled `executable-jars` artifacts file:
1. Visit the releases page of the GitHub repository.
2. Download the latest release `.jar` file (e.g., `mqtt-elevator-teamb-adapter-jar-with-dependencies.jar`).

### Run the Application

#### Start the Elevator Simulation
Ensure the elevator simulator is running and accessible.

#### Run the Main Adapter and Algorithm
Navigate to the directory where the `.jar` file was downloaded and execute:
```bash
java -jar mqtt-elevator-teamb-adapter-jar-with-dependencies.jar
java -jar mqtt-elevator-teamb-algorithm-jar-with-dependencies.jar
```

The application connects to the MQTT broker and starts the elevator management system.

## Setup and Execution

### 1. Clone the Repository
```bash
git clone <repository-url>
cd <repository-directory>
```

### 2. Build the Project
Use Maven to build the project and install dependencies:
```bash
mvn clean package
```

### 3. Run the Application

#### Start the Elevator Simulation
Ensure the elevator simulator is running and accessible.

#### Run the Main Adapter and Algorithm
```bash
java -jar mqtt-elevator-teamb-adapter-jar-with-dependencies.jar
java -jar mqtt-elevator-teamb-algorithm-jar-with-dependencies.jar
```

The application connects to the MQTT broker and starts the elevator management system.

### 4. Run Tests
To execute all tests and generate a coverage report:
```bash
mvn test jacoco:report
```
Reports can be found under `target/site/jacoco`.


## Testing Concept

### Unit Tests
Unit tests are designed to verify the functionality of individual components:
- **Elevator.java**: Tests include initialization, state updates, and change detection. 
- **ElevatorAlgorithm.java**: Tests cover direction switching, prioritization, and unserviceable floor handling. Model based testing is used. The model is the elevator state class that is already used in the production code. In addition to that classic unit tests are used to increase the code coverage.
- **ElevatorMqttRouter.java**: Dependency injection is used to abstract the router from both the mqtt client as well as the algorithm. Therefore the router that acts as middle layer between mqtt client and elevator algorithm and can be tested without the actual concrete classes.
### Integration Tests
Integration tests ensure that components work together correctly:
- **ElevatorManagerIntegrationTest.java**: Verifies MQTT communication, topic subscription, and data publishing using mocked `IElevator` objects and a HiveMQ container.
### Tools and Frameworks
- **JUnit 5**: For writing and running unit and integration tests.
- **Mockito**: For mocking dependencies like the `IElevator` interface.
- **Testcontainers**: For managing Dockerized MQTT brokers in integration tests.
### Continuous Integration
The project includes a GitHub Actions workflow (`maven.yml`) that:
1. Builds the project.
2. Runs all tests with code coverage.
3. Uploads the JaCoCo coverage report.


