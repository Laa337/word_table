package actors.utils;

import actors.recepcionists.Lexicon;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import models.actorModels.wordgame.scandi.FoundWords;
import models.actorModels.wordgame.wordtable.CandidateWords;
import utils.Positio;


import java.time.Duration;
import java.util.*;

public class WordTableUtilWorker extends AbstractBehavior<WordTableUtilWorker.Command> {

    public interface Command {}

    public static class FindWordsOfRow implements Command {

        private String row;
        private Positio positio;
        private FoundWords.Direction direction;
        private String topic;
        private ActorRef<WordTable.FindWords> replayTo;

        public FindWordsOfRow(String row, Positio positio, FoundWords.Direction direction, String topic, ActorRef<WordTable.FindWords> replayTo) {
            this.row = row;
            this.positio = positio;
            this.direction = direction;
            this.topic = topic;
            this.replayTo = replayTo;
        }

        public String getRow() {
            return row;
        }

        public Positio getPositio() {
            return positio;
        }

        public FoundWords.Direction getDirection() {
            return direction;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }

        public String getTopic() {
            return topic;
        }
    }

    public static class FindWordsOfRowResponse implements Command,
                                                          WordTable.FindWords {
        private Set<CandidateWords> foundWordsSet;

        public FindWordsOfRowResponse(Set<CandidateWords> foundWordsSet) {
            this.foundWordsSet = foundWordsSet;
        }

        public Set<CandidateWords> getFoundWordsSet() {
            return foundWordsSet;
        }
    }

    private static class ListingResponse implements Command {
        final Receptionist.Listing listing;

        private ListingResponse(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    public static class ErrorClass implements Command, WordTable.FindWords {
        public String message;

        public ErrorClass(String message) {
            this.message = message;
        }
    }



    public static Behavior<Command> create() {
        return Behaviors.withStash(15,
                stx -> Behaviors.setup(
                        ctx -> {
                            return  new WordTableUtilWorker(stx,ctx);
                        }
                )
        );
    }

    private StashBuffer<Command> buffer;
    private final ActorRef<Receptionist.Listing> listingResponseAdapter;


    private WordTableUtilWorker(StashBuffer<Command> buffer, ActorContext<Command> context) {
        super(context);
        this.buffer = buffer;

        this.listingResponseAdapter =
                context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);
    }


    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FindWordsOfRow.class,
                            msg -> {
                                buffer.stash(msg);
                                Set<CandidateWords> candidates = new HashSet<>();
                                return buffer.unstashAll(runningReceive(msg,candidates));
                            }
                        )
                          .build();
    }

    public Receive<Command> runningReceive(FindWordsOfRow message, Set<CandidateWords> candidates) {
        return newReceiveBuilder()
                .onMessage(FindWordsOfRow.class,
                        msg -> {
                            String row = msg.getRow();
                            String reverseRow = reverseString(row);
                            candidates.addAll(findWords(row, false));
                            candidates.addAll(findWords(reverseRow, true));

                            getContext().getSystem().receptionist().tell(
                                    Receptionist.find(Lexicon.FindWordsServiceKey, listingResponseAdapter)
                            );

                            return Behaviors.same();
                        }
                )
                .onMessage(ListingResponse.class,
                        response ->  {
                              for(ActorRef<Lexicon.Command> ref:  response.listing.getServiceInstances(Lexicon.FindWordsServiceKey) ) {
                                    askLexicon(candidates,message.getTopic(),ref);
                                    break;
                              }
                              return Behaviors.same();
                          })
                .onMessage(FindWordsOfRowResponse.class,
                        msg -> {
                            Map<Integer,List<FoundWords>> foundWordsMap = new HashMap<>();
                            for(CandidateWords c: msg.getFoundWordsSet()) {
                                Positio pos = findPositioOfFoundWord(message.getPositio(),c.getPositio().x,
                                        c.getPositio().y, message.getRow().length(), message.getDirection(), c.isReverse());
                                List<FoundWords> leftList = foundWordsMap.get(c.getWord().length());
                                if(leftList == null)
                                    leftList = new ArrayList<>();
                                leftList.add(new FoundWords(c.getWord(), pos ,c.getWord().length(), message.getDirection(),c.isReverse()));
                                foundWordsMap.put(c.getWord().length(), leftList);
                            }

                            message.replayTo.tell(new WordTable.FindWordsClass(foundWordsMap, null) );
                            return Behaviors.same();
                        }
                )
                .onMessage(ErrorClass.class,
                        msg -> {
                            message.replayTo.tell(new WordTable.ErrorFindWords(msg.message) );
                            return Behaviors.same();
                        }
                )
                .build();
    }


    private void askLexicon(Set<CandidateWords> candidates, String topic, ActorRef<Lexicon.Command> ref) {
        getContext().ask( WordTable.FindWords.class,
                                               ref,
                            Duration.ofMillis(200),
                    me -> new Lexicon.FindTopicWordsInSet(candidates, topic, me),
                   (response,failure) -> {
                     if(response!=null) {
                        if(response.getClass() == WordTable.ErrorFindWords.class)
                            return new ErrorClass(((WordTable.ErrorFindWords) response).message);
                        return new FindWordsOfRowResponse( ((WordTable.FindWordsOfRowClass)response).getWords());
                    }
                    return new ErrorClass("Lexicon timed out");
                }

        );
    }

    private  String reverseString(String str){
        StringBuilder sb=new StringBuilder(str);
        sb.reverse();
        return sb.toString();
    }


    private Set<CandidateWords> findWords(String row, boolean reverse) {
        Set<CandidateWords> words = new HashSet<>();

        for (int i = 0; i < row.length() ; i++)
            for (int j = i+1; j <= row.length(); j++)  {
                String currentWord = row.substring(i, j);
                words.add(new CandidateWords(currentWord, new Positio(i,j), reverse    ));
            }

        return words;
    }

    private Positio findPositioOfFoundWord(Positio positio, int x, int y, int length, FoundWords.Direction direction, boolean reverse) {
        if(direction == FoundWords.Direction.FORWARD) {
            int posx = !reverse ? x : (length-x-1);
            int posy = positio.y-1;
            return new Positio(posx,posy);
        }
        else if(direction == FoundWords.Direction.DOWN) {
            int posx = positio.x-1;
            int posy = !reverse ? x : (length-x-1);
            return new Positio(posx,posy);
        }
        else if(direction == FoundWords.Direction.RIGHT) {
            int posx = !reverse ? (positio.x-1 + x) : (positio.x -1 + length-1-x);
            int posy = !reverse ? (positio.y-1 + x) : (positio.y-1 + length -1-x);
            return new Positio(posx,posy);
        }
        int posx = !reverse ? (positio.x-1 - x) : (positio.x -1 - (length-1-x));
        int posy = !reverse ? (positio.y-1 + x) : (positio.y-1 + length -1-x);
        return new Positio(posx,posy);
    }
}
