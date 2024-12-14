
package at.fhhagenberg.sqelevator.algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ElevatorAlgorithmTest {
    @Test
    public void testSingleRequest() {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm(1, 5);
        boolean[] requests = {false, false, true, false, false};
        algorithm.updateState(0, 0, 2, requests);

        algorithm.processRequests();
        ElevatorState state = algorithm.getElevatorStates().get(0);

        assertEquals(2, state.targetFloor);
        assertEquals(0, state.direction);
    }

    @Test
    public void testMultipleRequests() {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm(1, 5);
        boolean[] requests = {false, true, true, false, false};
        algorithm.updateState(0, 2, 2, requests);

        algorithm.processRequests();
        ElevatorState state = algorithm.getElevatorStates().get(0);

        assertEquals(1, state.targetFloor);
        assertEquals(1, state.direction);
    }
}
