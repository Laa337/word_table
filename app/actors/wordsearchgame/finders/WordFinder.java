package actors.wordsearchgame.finders;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.utils.WordTable;
import actors.utils.WordTableUtilWorker;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import utils.Positio;
import models.actorModels.wordgame.scandi.FoundWords;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author akosp
 */
public class WordFinder extends AbstractBehavior<WordFinder.Command> {

    public interface Command {}

    public static class FindWord implements Command {
        private String row;
        private FoundWords.Direction direction;
        private Positio positio;
        private String topic;
        private ActorRef<WordTable.FindWords> replayTo;

        public FindWord(String row, FoundWords.Direction direction, Positio positio, String topic, ActorRef<WordTable.FindWords> replayTo) {
            this.row = row;
            this.direction = direction;
            this.positio = positio;
            this.topic = topic;
            this.replayTo = replayTo;
        }

        public String getRow() {
            return row;
        }

        public FoundWords.Direction getDirection() {
            return direction;
        }

        public Positio getPositio() {
            return positio;
        }

        public String getTopic() {
            return topic;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }
    }


    public static class FindWordResponse implements Command, WordTable.FindWords {
        Map<Integer,List<FoundWords>> foundWordsMap = new HashMap<>();

        public FindWordResponse(Map<Integer, List<FoundWords>> foundWordsMap) {
            this.foundWordsMap = foundWordsMap;
        }

        public Map<Integer, List<FoundWords>> getFoundWordsMap() {
            return foundWordsMap;
        }
    }

    public static class ErrorClass implements Command, WordTable.FindWords {
        public String Message;

        public ErrorClass(String message) {
            Message = message;
        }
    }


    private ActorRef<WordTable.FindWords> replayTo;

    public static Behavior<Command> create() {
        return Behaviors.setup(WordFinder::new);
    }

    private WordFinder(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FindWord.class,
                        msg -> {
                            askWordTableUtilWorker(msg);
                            this.replayTo = msg.getReplayTo();
                            return Behaviors.same();
                        }
                )
                .onMessage(FindWordResponse.class,
                        msg -> Behaviors.same()
                )
                .onMessage(ErrorClass.class,
                        msg -> {
                            System.out.println("WordFinder Error:  " + ((ErrorClass)msg).Message);
                            this.replayTo.tell(msg);
                            return Behaviors.same();
                        }
                )
                .build();
    }



    private void askWordTableUtilWorker(FindWord msg) {
        Behavior<WordTableUtilWorker.Command> supervised =
                Behaviors.supervise(WordTableUtilWorker.create()).onFailure(SupervisorStrategy.stop());
        ActorRef<WordTableUtilWorker.Command> finder =
                getContext().spawn(supervised, "creator" + new Random().nextInt(100000) + msg.hashCode() );
        ErrorClass error = new ErrorClass("Table decomposer failed");
        getContext().watchWith(finder, error);

        getContext().ask( WordTable.FindWords.class,
                finder,
                Duration.ofMillis(2000),
                me -> new WordTableUtilWorker.FindWordsOfRow(msg.getRow(), msg.getPositio(), msg.getDirection(), msg.getTopic(), me),
                (response,failure) -> {
                    if(response!=null) {
                        if(response.getClass() == WordTable.ErrorFindWords.class)
                            return new ErrorClass(((WordTable.ErrorFindWords) response).message);
                        msg.getReplayTo().tell(response);
                        return new FindWordResponse(null);
                    }
                    else
                         return new ErrorClass("Lexicon timed out");
                }

        );
    }

}
