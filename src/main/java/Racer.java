import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import javafx.geometry.Pos;

import java.io.Serializable;
import java.util.Random;

public class Racer extends AbstractBehavior<Racer.Command> {

    public interface Command extends Serializable {

    }

    public static class StartCommand implements Command {

        public int getRaceLength() {
            return raceLength;
        }

        private int raceLength;

        StartCommand(int raceLength) {
            this.raceLength = raceLength;
        }

    }

    public static class PositionCommand implements Command {

        public ActorRef<RaceController.Command> getController() {
            return controller;
        }

        public PositionCommand(ActorRef<RaceController.Command> controller) {
            this.controller = controller;
        }

        private ActorRef<RaceController.Command> controller;
    }

    private final double defaultAverageSpeed = 48.2;
    private int averageSpeedAdjustmentFactor;
    private Random random;

    private double currentSpeed = 0;
    private double currentPosition = 0;

    private int raceLength;



    public Racer(ActorContext context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Racer::new);
    }

    @Override
    public Receive createReceive() {

        return newReceiveBuilder()
                .onMessage(StartCommand.class, command->{
                    this.raceLength = command.getRaceLength();
                    random = new Random();
                    averageSpeedAdjustmentFactor = random.nextInt(30)-10;

                    return this;
                })
                .onMessage(PositionCommand.class, command->{

                    determineNextSpeed();
                    currentPosition += getDistanceMovedPerSecond();
                    if (currentPosition > raceLength )
                        currentPosition  = raceLength;

                    command.getController().tell(new RaceController.RacerUpdateCommand(getContext().getSelf(), (int) currentPosition));

                    // send the message to controller about
                  return this;
                })
                .build();

    }

    private double getMaxSpeed() {
        return defaultAverageSpeed * (1+((double)averageSpeedAdjustmentFactor / 100));
    }

    private double getDistanceMovedPerSecond() {
        return currentSpeed * 1000 / 3600;
    }

    private void determineNextSpeed() {
        if (currentPosition < (raceLength / 4)) {
            currentSpeed = currentSpeed  + (((getMaxSpeed() - currentSpeed) / 10) * random.nextDouble());
        }
        else {
            currentSpeed = currentSpeed * (0.5 + random.nextDouble());
        }

        if (currentSpeed > getMaxSpeed())
            currentSpeed = getMaxSpeed();

        if (currentSpeed < 5)
            currentSpeed = 5;

        if (currentPosition > (raceLength / 2) && currentSpeed < getMaxSpeed() / 2) {
            currentSpeed = getMaxSpeed() / 2;
        }
    }

}
