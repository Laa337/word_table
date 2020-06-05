package actors.recepcionists;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.BaseRooter;
import actors.utils.WordTable;
import actors.wordsearchgame.creators.ScandiCrossWordWordFiller;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import models.actorModels.wordgame.wordtable.CandidateWords;
import utils.MapUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 *
 * @author akosp
 */
public class Lexicon  extends AbstractBehavior<Lexicon.Command> {
    private static Map<String,Set<String>> dictionary = new HashMap<>();
    private static Map<String,List<String>> topicMap;
    private final ActorRef<Object> creator;

    public interface Command {}

    public static class Contains implements  Command {
        private String word;
        private String topic;
        ActorRef<WordTable.FindWords> replayTo;

        public Contains(String word, String topic, ActorRef<WordTable.FindWords> replayTo) {
            this.word = word;
            this.topic = topic;
            this.replayTo = replayTo;
        }

        public String getWord() {
            return word;
        }

        public String getTopic() {
            return topic;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }
    }

    public static class ContainsResponse implements Command, WordTable.FindWords {
        private boolean found;

        public ContainsResponse(boolean found) {
            this.found = found;
        }

        public boolean isFound() {
            return found;
        }
    }




   public static class FindTopicWordsInSet implements Command {
       private Set<CandidateWords> words;
       private String topic;
       ActorRef<WordTable.FindWords> replayTo;

       public FindTopicWordsInSet(Set<CandidateWords> words, String topic, ActorRef<WordTable.FindWords> replayTo) {
           this.words = words;
           this.topic = topic;
           this.replayTo = replayTo;
       }

       public Set<CandidateWords> getWords() {
           return words;
       }

       public String getTopic() {
           return topic;
       }

       public ActorRef<WordTable.FindWords> getReplayTo() {
           return replayTo;
       }

   }

   public static class SortCrosswordWords implements Command {
       Map<String, List<String>> wordsToSort;
       private String topic;
       private ActorRef<WordTable.FindWords> replayTo;

       public SortCrosswordWords(Map<String, List<String>> wordsToSort, String topic, ActorRef<WordTable.FindWords> replayTo) {
           this.wordsToSort = wordsToSort;
           this.topic = topic;
           this.replayTo = replayTo;
       }
   }

   public static class SortCrosswordWordsResponse implements Command, WordTable.FindWords {
        private List<String> sortedWords;

       public SortCrosswordWordsResponse(List<String> sortedWords) {
           this.sortedWords = sortedWords;
       }

       public List<String> getSortedWords() {
           return sortedWords;
       }
   }

   public static class RankCroCrossWordPatterns implements Command {
        private Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, String> patterns;
       private String topic;
       private ActorRef<WordTable.FindWords> replayTo;

       public RankCroCrossWordPatterns(Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, String> patterns, String topic, ActorRef<WordTable.FindWords> replayTo) {
           this.patterns = patterns;
           this.topic = topic;
           this.replayTo = replayTo;
       }

       public Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, String> getPatterns() {
           return patterns;
       }

       public String getTopic() {
           return topic;
       }

       public ActorRef<WordTable.FindWords> getReplayTo() {
           return replayTo;
       }
   }

    public static class RankCroCrossWordPatternsResponse implements Command, WordTable.FindWords {

    }

    public static class FindWordsForTopic implements Command{
        private String topic;
        private int[] pattern;
        private ActorRef<WordTable.FindWords> replayTo;

        public FindWordsForTopic(String topic, int[] pattern, ActorRef<WordTable.FindWords> replayTo) {
            this.topic = topic;
            this.pattern = pattern;
            this.replayTo = replayTo;
        }

        public String getTopic() {
            return topic;
        }

        public int[] getPattern() {
            return pattern;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }
    }

    public static class FillCrossWords implements Command {
        private String word;
        private int counter;
        private ActorRef<WordTable.FindWords> replayTo;

        public FillCrossWords(String word, int counter, ActorRef<WordTable.FindWords> replayTo) {
            this.word = word;
            this.counter = counter;
            this.replayTo = replayTo;
        }

        public String getWord() {
            return word;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }

        public int getCounter() {
            return counter;
        }
    }

    public static Behavior<Command> create(ActorRef<Object> creator) {
        return Behaviors.setup(ctx -> new Lexicon(ctx,creator));
    }

    public static final ServiceKey<Command> FindWordsServiceKey =
            ServiceKey.create(Command.class, "findWordService");


    private Lexicon( ActorContext<Command> context, ActorRef<Object> creator) {
        super(context);
        this.creator = creator;

        try {
            dictionary.put("allat", Files.lines(Paths.get("C:\\Git\\Szotar\\akkaszotar\\allatok.txt"), StandardCharsets.UTF_8).collect(toSet()));
            dictionary.put("minden", Files.lines(Paths.get("C:\\Git\\Szotar\\akkaszotar\\angol_szavak.txt"), StandardCharsets.UTF_8).collect(toSet()));
            dictionary.put("nevmas", Files.lines(Paths.get("C:\\Git\\Szotar\\akkaszotar\\nevmasok.txt"), StandardCharsets.UTF_8).collect(toSet()));
            dictionary.put("angol", Files.lines(Paths.get("C:\\Git\\Szotar\\akkaszotar\\angol.txt"), StandardCharsets.UTF_8).collect(toSet()));

          //  System.getProperty("user.dir") + "\\public\\images\\";
            Thread.sleep(3000);
            topicMap =  MapUtil.createWordMap(new ArrayList<>(dictionary.get("minden")));
            System.out.println("\n\n\t\tF   I    L   E      O   K  \n\n ");
            creator.tell(new BaseRooter.LexiconOk());

        } catch (Exception ex) {
            Logger.getLogger(Lexicon.class.getName()).log(Level.SEVERE, null, ex);
        }

        context.getSystem().receptionist().tell(Receptionist.register(FindWordsServiceKey, context.getSelf()));

    }



    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Contains.class,
                        msg -> {
                            Set<CandidateWords> words = new HashSet<>();
                            if(dictionary.get(msg.getTopic()) == null) {
                                msg.getReplayTo().tell(new WordTable.ErrorFindWords("No such topic!"));
                                return Behaviors.same();
                            }

                            boolean found = false;
                            if(dictionary.get(msg.getTopic()).contains(msg.getWord()) )
                                found = true;

                            msg.getReplayTo().tell(new ContainsResponse(found));
                            return Behaviors.same();
                        })
                        .onMessage(FindTopicWordsInSet.class,
                                msg -> {
                                    Set<CandidateWords> words = new HashSet<>();
                                    if(dictionary.get(msg.getTopic()) == null) {
                                        msg.getReplayTo().tell(new WordTable.ErrorFindWords("No such topic!"));
                                        return Behaviors.same();
                                    }
                                    for(CandidateWords c: msg.getWords() ) {
                                        if(!c.getWord().contains("~") && dictionary.get(msg.getTopic()).contains(c.getWord()) )
                                            words.add(c);
                                    }

                                    msg.getReplayTo().tell(new WordTable.FindWordsOfRowClass(words));
                                    return Behaviors.same();
                                })
                .onMessage(FindWordsForTopic.class,
                        msg -> {
                            Set<String> words = new HashSet<>();
                            Set<String> topicWordsSet = dictionary.get(msg.getTopic());
                            if( topicWordsSet== null) {
                                msg.getReplayTo().tell(new WordTable.ErrorFindWords("No such topic!"));
                                return Behaviors.same();
                            }
                            int counter = 0;
                            for(int i= 2; i<msg.getPattern().length;i++)
                                counter+=msg.getPattern()[i];
                            int safetyNumber= 150;
                            while(counter>0 && safetyNumber>0) {
                                int step = (new Random()).nextInt(5)+1;
                                int i=0;
                                for(String s: topicWordsSet) {
                                    i++;
                                    if(s.length() >= msg.getPattern().length)
                                        continue;
                                    if( i%step == 1 && msg.getPattern()[s.length()] > 0 ) {
                                        if(words.add(s)) {
                                            msg.getPattern()[s.length()] -=1;
                                            counter--;
                                        }
                                    }
                                }
                                safetyNumber--;
                            }

                            msg.getReplayTo().tell(new WordTable.FindWordsOfTopicClass(words));
                            return Behaviors.same();
                        })
                .onMessage(FillCrossWords.class,
                        msg -> {
                            String originalWord = msg.getWord();

                            List<String> words;
                            if(originalWord.length()>7)
                                words = getCrosswordWords(originalWord, "minden");
                            else
                                words = topicMap.get(originalWord.toLowerCase());
                            if(words == null)
                                msg.getReplayTo().tell(new WordTable.FillCrossWords(new LinkedList<String>()));
                            else {
                                words = new LinkedList<>(words);
                                Collections.shuffle(words);
                                msg.getReplayTo().tell(new WordTable.FillCrossWords(words));
                            }
                            return Behaviors.same();
                        })
                .onMessage(RankCroCrossWordPatterns.class,
                        msg -> {
                            Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition,String> patternMap =
                                    msg.patterns;

                            patternMap.entrySet().forEach(
                                    ( (entry) -> entry.getKey().setAffected(
                                        topicMap.get(entry.getValue()) != null ?
                                                100000 - topicMap.get(entry.getValue().toLowerCase()).size() :
                                                1000000
                                    )
                            ));

                            msg.getReplayTo().tell(new RankCroCrossWordPatternsResponse());
                            return Behaviors.same();
                        })
                .onMessage(SortCrosswordWords.class,
                        msg -> {
                            Map<String, Integer> sortedMap = new HashMap<>();
                            for (Map.Entry<String, List<String>> entry : msg.wordsToSort.entrySet()) {
                                boolean passed = true;
                                int patternIndex = 0;
                                List<Integer> scores = Stream.generate( () -> 0 )
                                                        .limit(entry.getValue().size())
                                                        .collect(Collectors.toList());
                                for(String s:entry.getValue()) {
                                    List<String> foundWords = topicMap.get(s);
                                    if(foundWords == null) {
                                        passed = false;
                                        break;
                                    }
                                    scores.set( patternIndex, foundWords.size());
                                    patternIndex++;
                                }
                                if(!passed)
                                    continue;
                                sortedMap.put(entry.getKey(), Collections.min(scores) *100 + Collections.max(scores)/3);
                            }
                            List<String> sortedWords =
                                    sortedMap.entrySet().stream()
                                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
                            if(sortedWords == null)
                                msg.replayTo.tell(new SortCrosswordWordsResponse(new LinkedList<>()));
                            msg.replayTo.tell(new SortCrosswordWordsResponse(new LinkedList<>(sortedWords)));
                            return Behaviors.same();
                        })
                          .build();
    }

    private List<String> getCrosswordWords(String originalWord, String topic) {
        List<String> foundWords = new LinkedList<>();
        for(String s: dictionary.get(topic)) {
            if(originalWord.length()==s.length() && fullfillPattern(originalWord,s.toLowerCase())) {
                foundWords.add(s.toLowerCase());
            }
        }
        return foundWords;
    }

    private boolean fullfillPattern(String originalWord, String s) {
        boolean matches = true;
        int index = 0;
        for(char c : originalWord.toCharArray()) {
            if(c!= '#' && c!=s.charAt(index)) {
                matches = false;
                break;
            }
            index++;
        }

        return matches;
    }

}
