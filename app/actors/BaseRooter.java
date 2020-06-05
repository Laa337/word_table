package actors;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.recepcionists.Lexicon;
import actors.wordsearchgame.controllers.WordRootController;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author akosp
 */
public class BaseRooter extends AbstractBehavior<Object> {

    public static class LexiconOk {}

    public static Behavior<Object> create() {
        return Behaviors.withStash(5,
                stx -> Behaviors.setup(
                        ctx -> new BaseRooter(stx,ctx)
                )
        );
    }

    private StashBuffer<Object> buffer;
    private int counter;

    public BaseRooter(StashBuffer<Object> buffer, ActorContext<Object> context) {
        super(context);
        System.out.println("B A S E    R O O T E R \n\n");
        for(int i=0; i<1; i++) {
            Behavior<Lexicon.Command> supervised =
                    Behaviors.supervise(Lexicon.create(getContext().getSelf())).onFailure(SupervisorStrategy.restart());
            getContext().spawn(supervised, "lexicon" + i);
        }
        this.buffer = buffer;
        this.counter = 0;
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(LexiconOk.class ,
                        msg -> buffer.unstashAll(runningReceive()))

                .onAnyMessage(
                        msg -> {
                            this.buffer.stash(msg);
                            return this;
                        }
                )
                .build();
    }

    public Receive<Object> runningReceive() {
        return newReceiveBuilder()
                .onMessage(WordRootController.Command.class ,
                        msg -> {
                            this.counter++;
                            System.out.println("Rooter Worker1.Command");
                            System.out.println("Counter: " + counter);
                            createSimpleTellWorker("actors.wordsearchgame.controllers.WordRootController", msg  );

                            if (this.counter == 2)
                                return busyReceive();
                            return Behaviors.same();
                        })

                .onSignal(Terminated.class,
                        msg -> {
                            counter--;
                            System.out.println("BaseRooter Create receiveWorker terminated: " + counter +"\n" + msg.getRef());
                            return Behaviors.same();
                        }
                )
                .build();
    }

    public  Behavior<Object> busyReceive() {
        return newReceiveBuilder()
                .onSignal(Terminated.class,
                        msg -> {
                            --counter;
                            System.out.println("BR busyreceive Worker terminated: " + counter);
                            if(counter==0) {
                                System.out.println("\n--------UnStashh!!!");
                                return buffer.unstashAll(runningReceive());
                            }
                            return Behaviors.same();
                        }
                )
                .onAnyMessage( msg ->{
                    System.out.println("actors.BaseRooter busyreceive message stashed:  " + buffer.size());
                    buffer.stash(msg);
                    return Behaviors.same();
                })
                .build();
    }

    private void createSimpleTellWorker(String classType, Object msg ) {
        Class classToCreate;
        try {
            classToCreate = Class.forName(classType);
            Method method = classToCreate.getMethod("create");
            Behavior<Object> supervised =
                   Behaviors.supervise( (Behavior<Object> ) method.invoke(null,  null)  ).onFailure(SupervisorStrategy.stop());
            ActorRef<Object> worker =
                    getContext().spawn(supervised,"worker" + counter + msg.hashCode());
            getContext().watch(worker);
            worker.tell(msg);
        } catch (ClassNotFoundException | NoSuchMethodException  | IllegalAccessException | InvocationTargetException e ) {
           // sender.tell(e);
        }
    }

}
