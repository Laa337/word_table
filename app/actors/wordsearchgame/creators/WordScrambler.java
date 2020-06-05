package actors.wordsearchgame.creators;

import actors.recepcionists.Lexicon;
import actors.utils.WordTable;
import actors.wordsearchgame.controllers.WordRootController;
import actors.wordsearchgame.controllers.WordTableCreatorController;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WordScrambler  extends AbstractBehavior<WordScrambler.Command> {

    public interface Command {}

    public static class ScrambleWords implements Command, WordRootController.Command {
        private int wordsNeeded;
        private int minLength;
        private int maxLength;
        private String topic;
        private ActorRef<Object> replayTo;

        public ScrambleWords(int wordsNeeded, int minLength, int maxLength, String topic, ActorRef<Object> replayTo) {
            this.wordsNeeded = wordsNeeded;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.topic = topic;
            this.replayTo = replayTo;
        }

        public int getWordsNeeded() {
            return wordsNeeded;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public String getTopic() {
            return topic;
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

    public static class ScrambleWordsResponse implements WordTable.FindWords, Command  {
        String[] originalWords;
        String[] scrambledWords;

        public ScrambleWordsResponse(String[] originalWords, String[] scrambledWords) {
            this.originalWords = originalWords;
            this.scrambledWords = scrambledWords;
        }

        public String[] getOriginalWords() {
            return originalWords;
        }

        public String[] getScrambledWords() {
            return scrambledWords;
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
                stx -> Behaviors.setup(
                        ctx -> new WordScrambler(ctx, stx)
                )
        );

    }
    private final ActorRef<Receptionist.Listing> GetTopicWordsResponseAdapter;

    private WordScrambler(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;

        this.GetTopicWordsResponseAdapter =
                context.messageAdapter(Receptionist.Listing.class, GetTopicWordsResponseAdapter::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ScrambleWords.class,
                        msg -> {
                            //   System.out.println("TopicTableWorker createReceive");
                            buffer.stash(msg);
                            return buffer.unstashAll(runningReceive(msg));
                        }
                )
                .build();

    }

    public Receive<Command> runningReceive(ScrambleWords message) {
        return newReceiveBuilder()
                .onMessage(ScrambleWords.class,
                        msg -> {
                            getContext().getSystem().receptionist().tell(
                                    Receptionist.find(Lexicon.FindWordsServiceKey, GetTopicWordsResponseAdapter)
                            );

                            return Behaviors.same();
                        }
                )
                .onMessage(GetTopicWordsResponseAdapter.class,
                        response -> {
                            int[] pattern = getPattern( message.getWordsNeeded(), message.getMinLength(),message.getMaxLength());
                            for(ActorRef<Lexicon.Command> ref: response.listing.getServiceInstances(Lexicon.FindWordsServiceKey) ) {
                                askLexiconForTopicWords(message.getTopic(), pattern, ref);
                                break;
                            }
                            return Behaviors.same();
                        }
                )
                .onMessage(TopicWordsResponse.class,
                        response -> {
                            String[] originalWords = Arrays.stream(response.getWords().toArray()).map(Object::toString).toArray(String[]::new);
                            String[] scrambledWords = scrambleWords(originalWords);
                            message.getReplayTo().tell(new ScrambleWordsResponse(originalWords,scrambledWords));
                            return Behaviors.stopped();
                        }
                )
                .onMessage(ErrorClass.class,
                        msg -> {
                            System.out.println("\nERROR   ERRRORR   ERROR   ERROR\n" + msg.message);
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

    private int[] getPattern(int wordsNeeded, int minLength, int maxLength) {
        int[] pattern = new int[maxLength+1];
        for(int i=0;i<maxLength+1;i++)
            pattern[i] = 0;
        int counter=0;
        while(counter<wordsNeeded) {
            pattern[ (new Random()).nextInt(maxLength+1-minLength) +minLength  ] +=1;
            counter++;
        }
        return pattern;
    }

    private String[] scrambleWords(String[] originalWords) {
        String[] response = new String[originalWords.length];
        for(int i=0;i<originalWords.length;i++) {
            List<String> current = new ArrayList<>();
            for(int j=0;j< originalWords[i].length();j++)
                current.add(originalWords[i].substring(j,j+1));
            do {
                Collections.shuffle(current);
                response[i] = current.stream().collect(Collectors.joining());
            }while (response[i].equals(originalWords[i]));
        }

        return response;
    }
}
