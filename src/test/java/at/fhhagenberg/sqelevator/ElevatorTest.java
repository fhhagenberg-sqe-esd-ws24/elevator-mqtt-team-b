/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorTest.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/

package at.fhhagenberg.sqelevator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ElevatorTest {

    private IElevator mockPlc;
    private Elevator elevator;

    @BeforeEach
    public void setUp() throws Exception {
        // Create a mock PLC interface
        mockPlc = mock(IElevator.class);
        // Instantiate the Elevator class with 5 floors (example)
        elevator = new Elevator(1, 5);
    }

    @Test
    public void testInitialElevatorState() {
        // Check if the initial state of the elevator is set correctly
        assertEquals(1, elevator.getElevatorNumber());
        assertEquals(0, elevator.getCommittedDirection());  // Default value
        assertEquals(0, elevator.getAcceleration());         // Default value
        assertEquals(0, elevator.getDoorStatus());           // Default value
    }

    @Test
    public void testUpdateFromPLC() throws Exception {
        // Mock the behavior of the PLC to return specific values
        when(mockPlc.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        when(mockPlc.getElevatorAccel(1)).thenReturn(2);
        when(mockPlc.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
        when(mockPlc.getElevatorFloor(1)).thenReturn(3);
        when(mockPlc.getElevatorPosition(1)).thenReturn(300);
        when(mockPlc.getElevatorSpeed(1)).thenReturn(10);
        when(mockPlc.getElevatorWeight(1)).thenReturn(500);
        when(mockPlc.getTarget(1)).thenReturn(4);

        // Update elevator state from mock PLC
        elevator.updateFromPLC(mockPlc);

        // Assert that the state was correctly updated
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.getCommittedDirection());
        assertEquals(2, elevator.getAcceleration());
        assertEquals(IElevator.ELEVATOR_DOORS_OPEN, elevator.getDoorStatus());
        assertEquals(3, elevator.getCurrentFloor());
        assertEquals(300, elevator.getPosition());
        assertEquals(10, elevator.getSpeed());
        assertEquals(500, elevator.getWeight());
        assertEquals(4, elevator.getTargetFloor());
    }

    @Test
    public void testStateChanges() throws Exception {
        // Initial state change
        assertTrue(elevator.hasCommittedDirectionChanged());
        assertTrue(elevator.hasAccelerationChanged());
    	
        // First update
        when(mockPlc.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        when(mockPlc.getElevatorAccel(1)).thenReturn(2);
        elevator.updateFromPLC(mockPlc);
        
        // Committed i state change
        assertFalse(elevator.hasCommittedDirectionChanged());
        assertTrue(elevator.hasAccelerationChanged());

        // Second update with changes
        when(mockPlc.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);
        when(mockPlc.getElevatorAccel(1)).thenReturn(3);
        elevator.updateFromPLC(mockPlc);

        // Assert that the changes are correctly detected
        assertTrue(elevator.hasCommittedDirectionChanged());
        assertTrue(elevator.hasAccelerationChanged());
        
        // Third update without changes
        when(mockPlc.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);
        when(mockPlc.getElevatorAccel(1)).thenReturn(3);
        elevator.updateFromPLC(mockPlc);
        
        // Assert that the changes are correctly detected
        assertFalse(elevator.hasCommittedDirectionChanged());
        assertFalse(elevator.hasAccelerationChanged());
    }

    @Test
    public void testFloorButtonsChange() throws Exception {
        // First update: all buttons off
        when(mockPlc.getElevatorButton(1, 0)).thenReturn(false);
        when(mockPlc.getElevatorButton(1, 1)).thenReturn(false);
        when(mockPlc.getElevatorButton(1, 2)).thenReturn(false);
        elevator.updateFromPLC(mockPlc);

        // Assert no changes at first
        assertFalse(elevator.haveFloorButtonsChanged());

        // Second update: button 1 pressed
        when(mockPlc.getElevatorButton(1, 1)).thenReturn(true);
        elevator.updateFromPLC(mockPlc);

        // Assert that floor buttons have changed
        assertTrue(elevator.haveFloorButtonsChanged());
    }
}
