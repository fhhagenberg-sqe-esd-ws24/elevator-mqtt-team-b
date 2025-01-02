@echo off

REM Start the MQTT broker using docker-compose in a new CMD window
start "MQTT Broker" cmd /k "cd MqttBrocker && docker-compose up"

REM Wait a few seconds to ensure the MQTT broker starts properly
timeout /t 5 > nul

REM Start elevator.bat in a new CMD window
start "Elevator" cmd /k "elevator-0.1.2-dist\elevator-0.1.2\bin\elevator.bat"

REM Wait for user input before starting Java applications
echo Please start the Elevator Simulator manually and press any key to continue...
pause

REM Use Maven to compile and run the first Main.java in a new CMD window
start "Main.java adapter" cmd /k "mvn -f pom.xml compile exec:java -Dexec.mainClass=at.fhhagenberg.sqelevator.adapter.MainAdapter"

REM Use Maven to compile and run the first Main.java in a new CMD window
start "Main.java algorithm" cmd /k "mvn -f pom.xml compile exec:java -Dexec.mainClass=at.fhhagenberg.sqelevator.algorithm.MainAlgorithm"

REM Script finished
exit
