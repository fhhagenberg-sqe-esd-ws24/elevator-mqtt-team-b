/*! **MH-Moduleheader*****************************************************
 *  Project:    Elevator

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorTest.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2025
 *  \remarks
 *
** *********************************************************************/



package at.fhhagenberg.sqelevator.adapter;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import sqelevator.IElevator;

public class MainAdapterTest {

    private IElevator mockController;
    private ElevatorManager mockElevatorManager;
    private Properties mockProperties;
    private Logger mockLogger;
    private Registry mockRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        mockController = mock(IElevator.class);
        mockElevatorManager = mock(ElevatorManager.class);
        mockProperties = mock(Properties.class);
        mockLogger = mock(Logger.class);

        // Create a mock RMI registry to prevent NullPointerException
        mockRegistry = LocateRegistry.createRegistry(1099);

        // Mock Naming.lookup to avoid actual RMI calls
        mockStatic(Naming.class);
        when(Naming.lookup(eq("rmi://localhost/ElevatorSim"))).thenReturn(mockController);

        // Mock properties behavior
        when(mockProperties.getProperty(eq("plc.url"), eq("rmi://localhost/ElevatorSim")))
                .thenReturn("rmi://localhost/ElevatorSim");
        when(mockProperties.getProperty(eq("timer.period"), eq("100")))
                .thenReturn("100");
    }

    @Test
    public void testMainLoop(@TempDir File tempDir) throws Exception {
        // Set up a temporary file for the logger
        File logFile = new File(tempDir, "restart_log_adapter.txt");
        FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
        mockLogger.addHandler(fileHandler);

        // Mock behavior for the elevator manager
        when(mockElevatorManager.doRestart()).thenReturn(false).thenReturn(true);

        // Run the main loop
        Thread mainThread = new Thread(() -> {
            try {
                MainAdapter.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        mainThread.start();
        Thread.sleep(2000); // Let the loop run briefly
        mainThread.interrupt(); // Stop the loop

        // Verify logger initialization and restart logging
        String expectedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        verify(mockLogger, atLeastOnce()).log(eq(Level.INFO), contains("System restarted at: " + expectedTimestamp));
    }

    @Test
    public void testLoggerInitialization(@TempDir File tempDir) throws IOException {
        // Initialize logger and check file creation
        File logFile = new File(tempDir, "restart_log_adapter.txt");
        new FileHandler(logFile.getAbsolutePath(), true);
        assertTrue(logFile.exists());
    }

    @Test
    public void testPrivateDisplayElevatorSettings() throws Exception {
        // Use reflection to test private method
        var method = MainAdapter.class.getDeclaredMethod("displayElevatorSettings");
        method.setAccessible(true);

        // Mock behavior for controller methods
        when(mockController.getClockTick()).thenReturn(12345L);
        when(mockController.getElevatorNum()).thenReturn(2);
        when(mockController.getFloorNum()).thenReturn(10);
        when(mockController.getFloorHeight()).thenReturn(100);
        when(mockController.getFloorButtonUp(anyInt())).thenReturn(false);
        when(mockController.getFloorButtonDown(anyInt())).thenReturn(true);
        when(mockController.getElevatorFloor(anyInt())).thenReturn(1);
        when(mockController.getElevatorPosition(anyInt())).thenReturn(50);

        // Invoke the private method
        method.invoke(null);

        // Verify interactions
        verify(mockController, times(1)).getClockTick();
        verify(mockController, times(1)).getElevatorNum();
        verify(mockController, times(1)).getFloorNum();
        verify(mockController, times(1)).getFloorHeight();
    }

    @Test
    public void testExceptionHandling() throws RemoteException {
        // Simulate exceptions in the main loop
        doThrow(new RemoteException("Simulated exception")).when(mockController).getClockTick();

        try {
            var method = MainAdapter.class.getDeclaredMethod("displayElevatorSettings");
            method.setAccessible(true);
            method.invoke(null);
        } catch (Exception e) {
            // Verify exception handling logic
            assertTrue(e.getCause() instanceof RemoteException);
        }
    }
}

