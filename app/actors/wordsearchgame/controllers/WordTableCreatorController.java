package actors.wordsearchgame.controllers;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.recepcionists.Lexicon;
import actors.utils.WordTable;
import actors.wordsearchgame.creators.WordTableCreator;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import models.actorModels.wordgame.scandi.FoundWords;


import java.time.Duration;
import java.util.*;

/**
 * @author akosp
 */
public class WordTableCreatorController extends AbstractBehavior<WordTableCreatorController.Command> {

    public interface Command {
    }


    public static class CreateTableFromTopic implements Command, WordRootController.Command {

        private String topic;
        private int sizeX;
        private int sizeY;
        private boolean uniqueTable;
        private ActorRef<Object> replayTo;

        public CreateTableFromTopic(String topic, int sizeX, int sizeY, boolean uniqueTable, ActorRef<Object> replayTo) {
            this.topic = topic;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.uniqueTable = uniqueTable;
            this.replayTo = replayTo;
        }

        public String getTopic() {
            return topic;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public boolean isUniqueTable() {
            return uniqueTable;
        }

        public ActorRef<Object> getReplayTo() {
            return replayTo;
        }
    }

    public static class TopicWordsResponse implements Command, WordTable.FindWords {
        Set<String> words;

        public TopicWordsResponse(Set<String> words) {
            this.words = words;
        }

        public Set<String> getWords() {
            return words;
        }
    }

    public static class CreateTableResponse implements Command,
                                                       WordTable.CreateWordTable {

        private String[] createdTable;
        private Map<Integer, List<FoundWords>> foundWords;


        public CreateTableResponse(String[] createdTable, Map<Integer, List<FoundWords>> foundWords) {
            this.createdTable = createdTable;
            this.foundWords = foundWords;
        }

        public String[] getCreatedTable() {
            return createdTable;
        }

        public Map<Integer, List<FoundWords>> getFoundWords() {
            return foundWords;
        }

    }



    private static class GetTopicWordsResponseAdapter implements WordTable.FindWords, Command {
        final Receptionist.Listing listing;

        private GetTopicWordsResponseAdapter(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    public static class ErrorClass implements Command {
        public String message;

        public ErrorClass(String message) {
            this.message = message;
        }
    }


    private StashBuffer<Command> buffer;

    public static Behavior<Command> create() {
        return Behaviors.withStash(1,
                stx -> {
                    return Behaviors.setup(
                            ctx -> new WordTableCreatorController(ctx, stx)
                    );
                }
        );

    }
    private final ActorRef<Receptionist.Listing> GetTopicWordsResponseAdapter;

    private WordTableCreatorController(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;

        this.GetTopicWordsResponseAdapter =
                context.messageAdapter(Receptionist.Listing.class, GetTopicWordsResponseAdapter::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateTableFromTopic.class,
                        msg -> {
                            buffer.stash(msg);
                            return buffer.unstashAll(runningReceive(msg, new HashSet<>()));
                        }
                )
                .build();

    }

    public Receive<Command> runningReceive(CreateTableFromTopic message, Set<String> wordsToInsert) {
        return newReceiveBuilder()
                .onMessage(CreateTableFromTopic.class,
                        msg -> {
                            getContext().getSystem().receptionist().tell(
                                    Receptionist.find(Lexicon.FindWordsServiceKey, GetTopicWordsResponseAdapter)
                            );

                            return Behaviors.same();
                        }
                )
                .onMessage(GetTopicWordsResponseAdapter.class,
                        response -> {
                            int[] pattern = getPattern(message.getSizeX(),message.getSizeY(), message.isUniqueTable());
                            for(ActorRef<Lexicon.Command> ref: ((GetTopicWordsResponseAdapter) response).listing.getServiceInstances(Lexicon.FindWordsServiceKey) ) {
                                  askLexiconForTopicWords(message.getTopic(), pattern, ref);
                                  break;
                             }
                            return Behaviors.same();
                        }
                )
                .onMessage(TopicWordsResponse.class,
                        msg -> {
                            askWordTableCreator(new ArrayList<>(msg.getWords()) , message.getSizeX(), message.getSizeY(), message.isUniqueTable());
                            return Behaviors.same();
                        }
                )
                .onMessage(CreateTableResponse.class,
                        msg -> {
                            message.replayTo.tell(msg);
                            return Behaviors.stopped();
                        }
                )
                .onMessage(ErrorClass.class,
                        msg -> {
                            message.replayTo.tell(msg);
                             return Behaviors.stopped();
                        }
                )
                .build();
    }



    private void askLexiconForTopicWords(String topic, int[] pattern,ActorRef<Lexicon.Command> ref) {
        getContext().ask( WordTable.FindWords.class,
                ref,
                Duration.ofMillis(2000),
                me -> new Lexicon.FindWordsForTopic(topic, pattern, me),
                (response,failure) -> {
                    if(response!=null) {
                        if(response.getClass() == WordTable.ErrorFindWords.class)
                            return new ErrorClass(((WordTable.ErrorFindWords) response).message);
                        return new TopicWordsResponse( ((WordTable.FindWordsOfTopicClass)response).getWords());
                    }

                    return new ErrorClass("Lexicon is busy at the moment, sorry!");
                }

        );
    }



    private void askWordTableCreator(List<String> wordsToInsert, int sizeX, int sizeY, boolean unique) {
        Behavior<WordTableCreator.Command> supervised =
                Behaviors.supervise(WordTableCreator.create()).onFailure(SupervisorStrategy.stop());
        ActorRef<WordTableCreator.Command> creator =
                getContext().spawn(supervised, "creator" + new Random().nextInt(100000) );
        ErrorClass error = new ErrorClass("actors.wordsearchgame.utils.TableCreator");
        getContext().watchWith(creator,error);
        getContext().ask(WordTable.CreateWordTable.class,
                creator,
                Duration.ofMillis(4500),
                me -> new WordTableCreator.CreateTable(wordsToInsert, sizeX, sizeY, unique, error, me),
                (response, failure) -> {
                    if (response != null) {
                        if(response.getClass() == WordTable.ErrorCreateWordTable.class)
                            return new ErrorClass( ((WordTable.ErrorCreateWordTable)response).message );
                        return new CreateTableResponse(((WordTable.CreateWordTableClass)response).getCreatedTable(),
                                                     ((WordTable.CreateWordTableClass)response).getFoundWords());
                    } else {
                        error.message="stop";
                        return new ErrorClass("Creator timed out, sorry");

                    }
                }
        );
    }

    private int[] getPattern(int sizeX, int sizeY, boolean uniqueTable) {
        int maxLength = Math.max(sizeX,sizeY) - (int)(Math.abs(sizeX-sizeY) * 0.8) ;
        int wordsNum = Math.max((sizeX * sizeY + 16 - Math.abs(sizeX-sizeY)*12) / 9, 6);

        int[] pattern = new int[maxLength];
        for(int j=0;j<maxLength;j++) {
            if(j==0  || j==1) {
                pattern[j] = 0;
                continue;
            }
            pattern[j] = (int)(wordsNum/j) + (new Random()).nextInt(maxLength/3);
        }


        return pattern;
    }
}
