package actors.wordsearchgame.controllers;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.BaseRooter;
import actors.wordsearchgame.creators.WordScrambler;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 *
 * @author akosp
 */
public class WordRootController extends AbstractBehavior<Object> {

      public interface Command {}

      public static  class Stop implements Command {}


    public static Behavior<Object> create() {
        return Behaviors.setup(WordRootController::new);
    }

    private WordRootController(ActorContext<Object> context) {
        super(context);
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(WordFinderController.Command.class,
                        msg-> {
                            System.out.println("WRC Findcommand!!");
                            Behavior<WordFinderController.Command> supervised =
                                    Behaviors.supervise(WordFinderController.create()).onFailure(SupervisorStrategy.stop());
                            ActorRef<WordFinderController.Command> finder =
                                    getContext().spawn(supervised, "finder" + msg.hashCode());
                            getContext().watch(finder);
                            finder.tell(msg);
                            return Behaviors.same();
                        }
                )
                .onMessage(WordTableCreatorController.Command.class,
                        msg-> {
                            System.out.println("WRC topic command!!");
                            Behavior<WordTableCreatorController.Command> supervised =
                                    Behaviors.supervise(WordTableCreatorController.create()).onFailure(SupervisorStrategy.stop());
                            ActorRef<WordTableCreatorController.Command> finder =
                                    getContext().spawn(supervised, "Creator" + msg.hashCode());
                            getContext().watch(finder);
                            finder.tell(msg);
                            return Behaviors.same();
                        }
                )
                .onMessage(WordScrambler.Command.class,
                        msg-> {
                            System.out.println("WRC topic command!!");
                            Behavior<WordScrambler.Command> supervised =
                                    Behaviors.supervise(WordScrambler.create()).onFailure(SupervisorStrategy.stop());
                            ActorRef<WordScrambler.Command> finder =
                                    getContext().spawn(supervised, "Creator" + msg.hashCode());
                            getContext().watch(finder);
                            finder.tell(msg);
                            return Behaviors.same();
                        }
                )
                .onMessage(CrosswordCreatorController.Command.class,
                        msg-> {
                            System.out.println("WRC crossword command!!");
                            Behavior<CrosswordCreatorController.Command> supervised =
                                    Behaviors.supervise(CrosswordCreatorController.create()).onFailure(SupervisorStrategy.stop());
                            ActorRef<CrosswordCreatorController.Command> finder =
                                    getContext().spawn(supervised, "Creator" + msg.hashCode());
                            getContext().watch(finder);
                            finder.tell(msg);
                            return Behaviors.same();
                        }
                )
                .onSignal(Terminated.class,
                        msg -> {
                            System.out.println("\n\n++++++++++++++++++\nWRC Terminated: " + msg.toString() + "\nReferer: " + msg.getRef());
                            getContext().stop(msg.getRef());
                           // this.father.tell(new BaseRooter.Stop());
                            return Behaviors.stopped();
                        }
                )
                .onAnyMessage(
                        msg -> {
                            System.out.println("\n\n++++++++++++++++++\nWRC UNKNOWN COMMAND : " + msg.toString() + "\nReferer: ");
                            // this.father.tell(new BaseRooter.Stop());
                            return Behaviors.stopped();
                        }
                )

                .build();
    }


}
