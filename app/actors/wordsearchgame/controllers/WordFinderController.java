package actors.wordsearchgame.controllers;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.utils.TableDecomposerWorker;
import actors.utils.WordTable;
import actors.wordsearchgame.finders.WordFinder;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.javadsl.StashBuffer;
import models.actorModels.wordgame.scandi.FoundWords;
import utils.Positio;

import java.time.Duration;
import java.util.*;

/**
 *
 * @author akosp
 */
public class WordFinderController extends AbstractBehavior<WordFinderController.Command> {

    public interface Command {}

    public static final class SearchWordsInTable implements Command, WordRootController.Command {

        private String[] table;
        private String topic;
        private ActorRef<Object> replayTo;

        public SearchWordsInTable(String[] table, String topic, ActorRef<Object> replayTo) {
            this.table = table;
            this.topic = topic;
            this.replayTo = replayTo;
        }

        public String[] getTable() {
            return table;
        }

        public ActorRef<Object> getReplayTo() {
            return replayTo;
        }

        public String getTopic() {
            return topic;
        }
    }

    public static final class SearchWordsInTableResponse implements Command,
                                                                    WordTable.FindWords,
                                                                    WordRootController.Command
    {

        private Map<Integer, List<FoundWords>> foundWords;
        private String[] table;

        public SearchWordsInTableResponse(Map<Integer, List<FoundWords>> foundWords, String[] table) {
            this.foundWords = foundWords;
            this.table = table;
        }

        public Map<Integer, List<FoundWords>> getFoundWords() {
            return foundWords;
        }

        public String[] getTable() {
            return table;
        }

    }


    public static class TableDecomposeResponse implements Command,
                                                           WordRootController.Command,
                                                            WordTable.Decompose
    {

        private Map<FoundWords.Direction, String[]> decomposedTableRows;
        private Map<FoundWords.Direction, Positio[]> decomposedTablePositions;
        int rowsCounter;

        public TableDecomposeResponse(Map<FoundWords.Direction, String[]> decomposedTableRows, Map<FoundWords.Direction, Positio[]> decomposedTablePositions, int rowsLength) {
            this.decomposedTableRows = decomposedTableRows;
            this.decomposedTablePositions = decomposedTablePositions;
            this.rowsCounter = rowsLength;
        }

        public Map<FoundWords.Direction, String[]> getDecomposedTableRows() {
            return decomposedTableRows;
        }

        public Map<FoundWords.Direction, Positio[]> getDecomposedTablePositions() {
            return decomposedTablePositions;
        }

        public int getRowsCounter() {
            return rowsCounter;
        }

    }

    public static class ErrorClass implements Command, WordTable.FindWords {
        public String message;

        public ErrorClass(String message) {
            this.message = message;
        }

    }


    private final StashBuffer<Command> buffer;
    public static Behavior<Command> create() {
        return Behaviors.withStash(1,
                stx -> Behaviors.setup(
                        ctx -> new WordFinderController(ctx, stx)
                )
        );
    }

    private Map<Integer, List<FoundWords>> foundWords = new HashMap<>();
    int messageCounter;

    private WordFinderController(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SearchWordsInTable.class,
                        msg -> {
                            PoolRouter<WordFinder.Command> pool
                                    = Routers.pool(5,
                                    Behaviors.supervise(WordFinder.create()).onFailure(SupervisorStrategy.resume())
                            );
                            ActorRef<WordFinder.Command> rooter = getContext().spawn(pool, "rooter-pool");
                            buffer.stash(msg);
                            return buffer.unstashAll(runningReceive(rooter, msg.getReplayTo(), msg.getTopic(),msg.getTable()));
                        }
                )
                .build();
    }


    public Receive<Command> runningReceive(ActorRef<WordFinder.Command> rooter,
                                           ActorRef<Object> sender,
                                           String topic,
                                           String[] table) {
        return newReceiveBuilder()
                .onMessage(SearchWordsInTable.class,
                        msg -> {
                            askTableDecomposer(msg.getTable(), sender);
                            return Behaviors.same();
                        }
                )
                .onMessage(TableDecomposeResponse.class,
                        msg -> {

                            messageCounter = msg.getRowsCounter();
                            int lengthX = msg.getDecomposedTableRows().get(FoundWords.Direction.FORWARD)[0].length();
                            int lengthY = msg.getDecomposedTableRows().get(FoundWords.Direction.FORWARD).length;

                            for (int i = 0; i < lengthX + lengthY  - 1; i++) {
                                if (i < lengthY) {
                                    askWorkerToFindWords(msg.getDecomposedTableRows().get(FoundWords.Direction.FORWARD)[i], FoundWords.Direction.FORWARD,
                                            msg.getDecomposedTablePositions().get(FoundWords.Direction.FORWARD)[i], topic,rooter, sender);
                                }
                                if( i< lengthX ) {
                                    askWorkerToFindWords(msg.getDecomposedTableRows().get(FoundWords.Direction.DOWN)[i], FoundWords.Direction.DOWN,
                                            msg.getDecomposedTablePositions().get(FoundWords.Direction.DOWN)[i], topic,rooter, sender);
                                }
                                askWorkerToFindWords(msg.getDecomposedTableRows().get(FoundWords.Direction.RIGHT)[i], FoundWords.Direction.RIGHT,
                                        msg.getDecomposedTablePositions().get(FoundWords.Direction.RIGHT)[i], topic,rooter, sender);
                                askWorkerToFindWords(msg.getDecomposedTableRows().get(FoundWords.Direction.LEFT)[i], FoundWords.Direction.LEFT,
                                        msg.getDecomposedTablePositions().get(FoundWords.Direction.LEFT)[i], topic,rooter, sender);
                            }

                            return Behaviors.same();
                        }
                )
                .onMessage(SearchWordsInTableResponse.class,
                        msg -> {
                            messageCounter--;
                            foundWords = mergeMaps(foundWords, msg.getFoundWords());
                            if (messageCounter == 0 || messageCounter == 0) {
                                sender.tell(new SearchWordsInTableResponse(foundWords, table));
                                return Behaviors.stopped();
                            }
                            return Behaviors.same();

                        }
                )
                .onMessage(ErrorClass.class,
                        msg -> {
                            getContext().stop(rooter);
                            sender.tell(msg);
                            return Behaviors.stopped();
                        }
                )
                .build();

    }

    private void askTableDecomposer(String[] table, ActorRef<Object> sender) {
                 Behavior<TableDecomposerWorker.Command> supervised =
                     Behaviors.supervise(TableDecomposerWorker.create()).onFailure(SupervisorStrategy.stop());
                ActorRef<TableDecomposerWorker.Command> decomposer =
                     getContext().spawn(supervised, "creator" + new Random().nextInt(100000) );
                 ErrorClass error = new ErrorClass("Table decomposer failed");
                 getContext().watchWith(decomposer, error);
                 getContext().ask(WordTable.Decompose.class,
                         decomposer,
                         Duration.ofSeconds(1),
                         me -> new TableDecomposerWorker.Decompose(table, me),
                         (response, failure) -> {
                             if (response != null) {
                                 WordTable.DecomposeClass result = (WordTable.DecomposeClass)response;
                                 getContext().stop(decomposer);
                                  return new TableDecomposeResponse(result.getDecomposedTableRows(),result.getDecomposedTablePositions(), result.getRowsLength()) ;
                              }

                             return new ErrorClass("Decomposer timed-out");
                     }
        );
    }

    private void askWorkerToFindWords(String row, FoundWords.Direction direction, Positio positio,
                                      String topic,
                                      ActorRef<WordFinder.Command> rooter, ActorRef<Object> sender) {
        getContext().ask(WordTable.FindWords.class, rooter, Duration.ofMillis(1200000),
                me -> new WordFinder.FindWord(row, direction, positio, topic, me),
                (response, failure) -> {
                    if (response != null) {
                        if(response.getClass() == WordFinder.ErrorClass.class)
                            return new ErrorClass(((WordFinder.ErrorClass) response).Message);
                        WordTable.FindWordsClass result = (WordTable.FindWordsClass)response;
                        return new SearchWordsInTableResponse(result.getFoundWords(),null);
                    }
                    return new ErrorClass("Finder pool worker time-out");
                }
        );
    }

    public Map<Integer, List<FoundWords>> mergeMaps(Map<Integer, List<FoundWords>> map1, Map<Integer, List<FoundWords>> map2) {
        Map<Integer, List<FoundWords>> map = new HashMap<>();
        map.putAll(map1);

        map2.forEach((key, value) -> {
            //Get the value for key in map.
            List<FoundWords> list = map.get(key);
            if (list == null) {
                map.put(key, value);
            } else {
                //Merge two list together
                List<FoundWords> mergedValue = new ArrayList<>(value);
                mergedValue.addAll(list);
                map.put(key, mergedValue);
            }
        });
        return map;
    }
}
