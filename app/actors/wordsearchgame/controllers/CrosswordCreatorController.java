package actors.wordsearchgame.controllers;

import actors.utils.WordTable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;

public class CrosswordCreatorController extends AbstractBehavior<CrosswordCreatorController.Command> {

  //  private final ActorRef<Receptionist.Listing> GetTopicWordsResponseAdapter;
    private StashBuffer<Command> buffer;



    private CrosswordCreatorController(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;


    }

    public static Behavior<Command> create() {
        return Behaviors.withStash(1,
                stx -> {
                    return Behaviors.setup(
                            ctx -> new CrosswordCreatorController(ctx, stx)
                    );
                }
        );

    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ScandiCrosswordCreatorController.Scandinavian.class,
                        msg -> {
                            System.out.println("CrossWordCreator  createReceive");
                            Behavior<ScandiCrosswordCreatorController.Command> supervised =
                                    Behaviors.supervise(ScandiCrosswordCreatorController.create()).onFailure(SupervisorStrategy.stop());
                            ActorRef<ScandiCrosswordCreatorController.Command> creator =
                                    getContext().spawn(supervised, "Creator" + msg.hashCode());
                            getContext().watch(creator);
                            creator.tell(msg);

                            return Behaviors.same();
                        }
                )
                .onSignal(Terminated.class, msg -> Behaviors.stopped())
                .build();
    }


    public interface Command { }



    //--------------------------------------------------------------------------------





    public static class ErrorClass implements Command, WordTable.FindWords{
        public String message;

        public ErrorClass(String message) {
            this.message = message;
        }
    }
}
