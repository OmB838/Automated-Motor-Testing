import frc.robot.subsystems.Motor;
import frc.robot.commands.MotorSpinCommand;
import frc.robot.Constants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MotorSpinCommandTest {

    @Mock
    private Motor mockMotorSubsystem;
    
    private MotorSpinCommand motorSpinCommand;
    
    // Simulated motor state
    private double simulatedPosition = 0.0;
    private double simulatedVelocity = 0.0;
    private double targetSpeed = 0.0;

    @BeforeEach
    public void setUp() {
        motorSpinCommand = new MotorSpinCommand(mockMotorSubsystem);
        
        // Reset simulation state
        simulatedPosition = 0.0;
        simulatedVelocity = 0.0;
        targetSpeed = 0.0;
    }

    @Test
    public void testInitialize() {
        motorSpinCommand.initialize();
        
        // Verify that resetMotorRotation was called
        verify(mockMotorSubsystem).resetMotorRotation();
    }

    @Test
    public void testExecute() {
        motorSpinCommand.execute();
        
        // Verify that setMotorSpeed was called with 5.0
        verify(mockMotorSubsystem).setMotorSpeed(5.0);
    }

    @Test
    public void testEnd() {
        motorSpinCommand.end(false);
        
        // Verify motor is stopped
        verify(mockMotorSubsystem).setMotorSpeed(0.0);
        
        // Test interruption too
        motorSpinCommand.end(true);
        verify(mockMotorSubsystem, times(2)).setMotorSpeed(0.0);
    }

    @Test
    public void testIsFinishedWhenGoingForward() {
        // When goingForward is true (default), should never be finished
        // regardless of motor position
        boolean result = motorSpinCommand.isFinished();
        assertFalse(result);
        
        // getMotorRotation should not be called since goingForward is true
        verify(mockMotorSubsystem, never()).getMotorRotation();
    }

    @Test
    public void testIsFinishedWhenNotGoingForward() {
        // Set goingForward to false
        setGoingForward(false);
        
        // Test with position above zero
        when(mockMotorSubsystem.getMotorRotation()).thenReturn(1.0);
        assertFalse(motorSpinCommand.isFinished());
        
        // Test with position at zero
        when(mockMotorSubsystem.getMotorRotation()).thenReturn(0.0);
        assertTrue(motorSpinCommand.isFinished());
        
        // Test with position below zero
        when(mockMotorSubsystem.getMotorRotation()).thenReturn(-0.5);
        assertTrue(motorSpinCommand.isFinished());
        
        // Verify getMotorRotation was called
        verify(mockMotorSubsystem, atLeast(1)).getMotorRotation();
    }

    @Test
    public void testFullCommandLifecycle() {
        // Test a complete command sequence
        
        // Initialize
        motorSpinCommand.initialize();
        verify(mockMotorSubsystem).resetMotorRotation();
        
        // Execute multiple times
        motorSpinCommand.execute();
        motorSpinCommand.execute();
        verify(mockMotorSubsystem, times(2)).setMotorSpeed(5.0);
        
        // End
        motorSpinCommand.end(false);
        verify(mockMotorSubsystem).setMotorSpeed(0.0);
    }

    @Test
    public void testMotorSpinSimulation() {
        // Set up simulation for this test only
        setupMotorSimulation();
        
        System.out.println("=== Motor Spin Up Test ===");
        motorSpinCommand.initialize();
        
        for (int i = 0; i < 5; i++) {
            motorSpinCommand.execute();
            for (int j = 0; j < Constants.SAMPLING_FREQUENCY; j++){
                double velocity = mockMotorSubsystem.getMotorVelocity();
                double position = mockMotorSubsystem.getMotorRotation();
                System.out.println(String.format("Cycle %d: Velocity=%.1f RPS, Position=%.3f rotations", 
                                (j + 1 + (i * Constants.SAMPLING_FREQUENCY)), velocity, position));
            }
        }
        
        // Should reach target velocity
        assertEquals(5.0, mockMotorSubsystem.getMotorVelocity(), 0.1);
        assertTrue(mockMotorSubsystem.getMotorRotation() > 0);
    }

     //Sets up realistic motor simulation - only called by tests that need it
    private void setupMotorSimulation() {
        // Mock setMotorSpeed to update target speed
        doAnswer(invocation -> {
            targetSpeed = invocation.getArgument(0);
            return null;
        }).when(mockMotorSubsystem).setMotorSpeed(anyDouble());
        
        // Mock resetMotorRotation
        doAnswer(invocation -> {
            simulatedPosition = 0.0;
            return null;
        }).when(mockMotorSubsystem).resetMotorRotation();
        
        // Mock getMotorVelocity with simulation
        when(mockMotorSubsystem.getMotorVelocity()).thenAnswer(invocation -> {
            updateMotorSimulation();
            return simulatedVelocity;
        });
        
        // Mock getMotorRotation with simulation
        when(mockMotorSubsystem.getMotorRotation()).thenAnswer(invocation -> {
            updateMotorSimulation();
            return simulatedPosition;
        });
    }
    
    //Updates the motor simulation physics
    private void updateMotorSimulation() {
        // Simple physics simulation
        double acceleration = 2.0; // RPS per update
        
        if (simulatedVelocity < targetSpeed) {
            simulatedVelocity = Math.min(targetSpeed, simulatedVelocity + acceleration);
        } else if (simulatedVelocity > targetSpeed) {
            simulatedVelocity = Math.max(targetSpeed, simulatedVelocity - acceleration);
        }
        
        // Update position
        double t = (1/((double) Constants.SAMPLING_FREQUENCY));
        simulatedPosition += simulatedVelocity * t;
    }

    //Helper method to set the private goingForward field using reflection
    private void setGoingForward(boolean value) {
        try {
            var field = MotorSpinCommand.class.getDeclaredField("goingForward");
            field.setAccessible(true);
            field.setBoolean(motorSpinCommand, value);
        } catch (Exception e) {
            fail("Could not set goingForward field: " + e.getMessage());
        }
    }
}