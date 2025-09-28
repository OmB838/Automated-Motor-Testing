package frc.robot.commands;

import frc.robot.subsystems.Motor;
import frc.robot.Constants;
import edu.wpi.first.wpilibj2.command.Command;

public class MotorSpinCommand extends Command {
  private final Motor m;
  private boolean goingForward = true;

  public MotorSpinCommand(Motor subsystem) {
    this.m = subsystem;
    addRequirements(subsystem);
  }

  @Override
  public void initialize() {
    m.resetMotorRotation(); // Start at 0
  }

  @Override
  public void execute() {
    m.setMotorSpeed(Constants.MOTORSPEED);
  }

  @Override
  public void end(boolean interrupted) {
    m.setMotorSpeed(0); // Always stop
  }

  @Override
  public boolean isFinished() {
    return !goingForward && m.getMotorRotation() <= 0.0;
  }
}