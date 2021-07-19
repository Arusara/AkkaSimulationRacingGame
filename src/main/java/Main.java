import akka.actor.typed.ActorSystem;

public class Main {


    public static void main(String[] args) {

        ActorSystem<RaceController.Command> actorSystem = ActorSystem.create(RaceController.create(), "Simulationgame");
        actorSystem.tell( new RaceController.StartCommand());

    }
}
