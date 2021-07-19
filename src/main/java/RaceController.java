import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RaceController extends AbstractBehavior<RaceController.Command> {

    public interface Command extends Serializable {
    }

    public static class StartCommand implements Command {
        private static final long serialVersionUID = 1L;

//        public String getCommand() {
//            return command;
//        }
//
//        private String command;
    }

    public static class RacerUpdateCommand implements Command {
        private static final long serialVersionUID = 1L;

        private int position;

        RacerUpdateCommand(ActorRef<Racer.Command> self, int position) {
            this.position = position;
            this.racer = self;
        }

        public int getPosition() {
            return position;
        }

        public ActorRef<Racer.Command> getRacer() {
            return racer;
        }

        private ActorRef<Racer.Command> racer;


    }

    private class GetPositionsCommand implements Command {
        private static final long serialVersionUID = 1L;
    }

    private RaceController(ActorContext context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(RaceController::new);
    }


    private Map<ActorRef<Racer.Command>, Integer> currentRacerPositions;

    private long start;
    private int raceLength = 100;

    private Object TIMER_KEY;

    private void displayRaces() {
        for (int i = 0; i < 50; ++i) System.out.println();
        System.out.println("Race has been running for " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
        int displayLength = 160;
        System.out.println("    " + new String (new char[displayLength]).replace('\0', '='));
        int i=0;
        for (ActorRef<Racer.Command> racer :currentRacerPositions.keySet()) {
            System.out.println(i + " : "  + new String (new char[currentRacerPositions.get(racer) * displayLength / 100]).replace('\0', '*'));
            i++;
        }
    }

    @Override
    public Receive createReceive() {
        return newReceiveBuilder()
                .onMessage(StartCommand.class, command->{
                    start = System.currentTimeMillis();
                    this.currentRacerPositions = new HashMap<>();

                    for (int i=0;i<10;i++) {
                        ActorRef<Racer.Command> racer = getContext().spawn(Racer.create(), "Racer"+i);
                        this.currentRacerPositions.put(racer, 0);
                        racer.tell(new Racer.StartCommand(raceLength));
                    }
                    return Behaviors.withTimers(timer ->{
                        timer.startTimerAtFixedRate(TIMER_KEY, new GetPositionsCommand(), Duration.ofSeconds(1));
                        return this;
                    });
                })

                .onMessage(GetPositionsCommand.class, positionCommand->{

                    for (ActorRef<Racer.Command> racer :currentRacerPositions.keySet()) {
                        racer.tell(new Racer.PositionCommand(getContext().getSelf()));
                        displayRaces();
                    }
                    return this;
                })
                .onMessage(RacerUpdateCommand.class, command -> {
                    currentRacerPositions.put(command.getRacer(), command.getPosition());
                    return this;
                })
                .build();


    }
}
