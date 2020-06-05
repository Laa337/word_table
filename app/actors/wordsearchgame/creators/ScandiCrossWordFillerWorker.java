package actors.wordsearchgame.creators;

import actors.recepcionists.Lexicon;
import actors.utils.WordTable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition;


import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ScandiCrossWordFillerWorker extends AbstractBehavior<ScandiCrossWordWordFiller.Command> {


    private static class GetTopicWordsResponseAdapter implements WordTable.FindWords, ScandiCrossWordWordFiller.Command {
        private  Receptionist.Listing listing;

        public GetTopicWordsResponseAdapter(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    private static class FindMostAffected implements ScandiCrossWordWordFiller.Command { }

    private  static class FindMostAffectedResponse implements ScandiCrossWordWordFiller.Command, WordTable.FindWords {}

    private static class AskLexicon implements ScandiCrossWordWordFiller.Command {
        private ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition;

        public AskLexicon(ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition) {
            this.definition = definition;
        }

        public ScandiCrossWordWordFiller.DefinitionMatrix.Definition getDefinition() {
            return definition;
        }

    }

    private static class FillMostAffectedResponse implements ScandiCrossWordWordFiller.Command,
            WordTable.FindWords{
        public ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition;
        public List<String> foundWords;

        public FillMostAffectedResponse(ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition, List<String> foundWords) {
            this.definition = definition;
            this.foundWords = foundWords;
        }
    }


    public static class CreatePatternMap implements ScandiCrossWordWordFiller.Command,
            WordTable.FindWords{
        ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition;
        List<String> foundWords;

        public CreatePatternMap(ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition, List<String> foundWords) {
            this.definition = definition;
            this.foundWords = foundWords;
        }
    }

    public static class CreatePatternMapResponse implements ScandiCrossWordWordFiller.Command, WordTable.FindWords {
        private List<String> foundWords;
        ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition;

        public CreatePatternMapResponse(List<String> foundWords, ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition) {
            this.foundWords = foundWords;
            this.definition = definition;
        }

        public List<String> getFoundWords() {
            return foundWords;
        }
    }


    public static class NotFound implements ScandiCrossWordWordFiller.Command, WordTable.FindWords  {
        String message;

        public NotFound() {
        }

        public NotFound(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private  ActorRef<Receptionist.Listing> GetTopicWordsResponseAdapter;
    private StashBuffer<ScandiCrossWordWordFiller.Command> buffer;

    public static Behavior<ScandiCrossWordWordFiller.Command> create() {
        return Behaviors.withStash(1,
                stx -> Behaviors.setup(
                        ctx -> new ScandiCrossWordFillerWorker(ctx, stx)
                )
        );

    }

    public ScandiCrossWordFillerWorker(ActorContext<ScandiCrossWordWordFiller.Command> context, StashBuffer<ScandiCrossWordWordFiller.Command> buffer) {
        super(context);
        this.buffer = buffer;


    }

    @Override
    public Receive<ScandiCrossWordWordFiller.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ScandiCrossWordWordFiller.FillDefinitionMatrix.class,
                        msg -> {
                            System.out.println("ScandiWorker createReceive running");

                            if(allCompleted(msg.getMatrix())) {
                                msg.getReplayTo().tell(new WordTable.ScandiCrossWordResponce( msg.getMatrix()));
                                return Behaviors.same();
                            }
                            buffer.stash(msg);
                            return buffer.unstashAll(lexiconReceive(msg));
                        })
                .build();
    }

    private boolean allCompleted(ScandiCrossWordWordFiller.DefinitionMatrix matrix) {
        boolean completed=true;
        for(ScandiCrossWordWordFiller.DefinitionMatrix.Definition def : matrix.getDefinitions()) {
            if(!def.isCompleted()) {
                completed=false;
                break;
            }
        }
        return completed;
    }



        private Behavior<ScandiCrossWordWordFiller.Command> lexiconReceive(ScandiCrossWordWordFiller.FillDefinitionMatrix message) {
        return newReceiveBuilder()
                .onMessage(ScandiCrossWordWordFiller.FillDefinitionMatrix.class,
                        msg -> {
                            this.GetTopicWordsResponseAdapter =
                                    getContext().messageAdapter(Receptionist.Listing.class,
                                            ScandiCrossWordFillerWorker.GetTopicWordsResponseAdapter::new
                                    );
                            getContext().getSystem().receptionist().tell(
                                    Receptionist.find(Lexicon.FindWordsServiceKey, GetTopicWordsResponseAdapter)
                            );
                            return  Behaviors.same();
                        })
                .onMessage(GetTopicWordsResponseAdapter.class,
                        response->{
                            List<ActorRef<Lexicon.Command>> lexicons = new ArrayList<>();
                            for(ActorRef<Lexicon.Command> ref: response.listing.getServiceInstances(Lexicon.FindWordsServiceKey) ) {
                                lexicons.add(ref);
                            }
                            buffer.stash(message);
                            return buffer.unstashAll(fillingReceive(message,lexicons));
                        })
                .build();
    }


    private Behavior<ScandiCrossWordWordFiller.Command> fillingReceive(ScandiCrossWordWordFiller.FillDefinitionMatrix message, List<ActorRef<Lexicon.Command>> lexicons) {
        return newReceiveBuilder()
                .onMessage(ScandiCrossWordWordFiller.FillDefinitionMatrix.class,
                        msg -> {

                            if(message.getMostAffected() == null)
                                getContext().getSelf().tell(new FindMostAffected());
                            else
                                getContext().getSelf().tell(new CreatePatternMapResponse(message.getFoundWords(), message.getMostAffected()));
                            return Behaviors.same();
                        })
                .onMessage(FindMostAffected.class,
                        msg -> {
                            Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, String> patternMap= new HashMap<>();
                            for(ScandiCrossWordWordFiller.DefinitionMatrix.Definition d : message.getMatrix().getDefinitions()) {
                                if(d.getPattern().toString().length()>2 && !d.isCompleted())
                                     patternMap.put(d, d.getPattern().toString().toLowerCase());
                            }
                            askLexiconToRankPatterns(lexicons.get(0), patternMap);
                            return Behaviors.same();
                        })
                .onMessage(FindMostAffectedResponse.class,
                        msg -> {
                            Collections.sort(message.getMatrix().getDefinitions());
                            ScandiCrossWordWordFiller.DefinitionMatrix.Definition mostAffected;
                            int index = 0;
                            while(true) {
                                mostAffected = message.getMatrix().getDefinitions().get(index++);
                                if(!mostAffected.isCompleted())
                                    break;
                            }
                            getContext().getSelf().tell(new AskLexicon(mostAffected));
                            return Behaviors.same();
                        })
                .onMessage(AskLexicon.class,
                        response->{
                            ActorRef<Lexicon.Command> lexicon = lexicons.get( 0 );
                            askLexiconForCrosswordWord(lexicon,response.definition);

                            return  Behaviors.same();
                        })
                .onMessage(FillMostAffectedResponse.class,
                        response->{
                            if(response.foundWords.isEmpty()) {
                                message.getReplayTo().tell(new NotFound());
                            }
                            else {
                                response.definition.setCompleted(true);
                                getContext().getSelf().tell(new CreatePatternMap( response.definition, response.foundWords));
                            }
                            return  Behaviors.same();
                        })
                .onMessage(CreatePatternMap.class,
                        msg->{
                            Map<String,List<String>> patternsTosSort = new HashMap<>();
                            for(String word: msg.foundWords) {
                                List<String>  patterns = updatePatterns(word,msg.definition,message.getAffectionTable(), false);
                                if(patterns.isEmpty()) {
                                    msg.definition.setCompleted(true);
                                    msg.definition.definition.setWord(word);
                                    message.setMostAffected(msg.definition);
                                    askScandiCrossWordFillerWorker(message.getAffectionTable(), message.getMatrix());

                                    return Behaviors.same();
                                }
                                patternsTosSort.put(word,patterns);
                            }
                            askLexiconToSortWords(lexicons.get( 0 ),patternsTosSort,msg.definition);
                            return  Behaviors.same();
                        })
                .onMessage(CreatePatternMapResponse.class,
                        response->{
                            if(response.foundWords.isEmpty()) {
                                message.getReplayTo().tell(new NotFound());
                            }
                            else {
                                String newWord = ((LinkedList<String>) response.foundWords).removeFirst();
                                response.definition.definition.setWord(newWord);
                                response.definition.setCompleted(true);
                                message.setMostAffected(response.definition);
                                message.setFoundWords(response.foundWords);

                                updatePatterns(newWord,response.definition,message.getAffectionTable(), true);

                                askScandiCrossWordFillerWorker(message.getAffectionTable(), message.getMatrix());
                            }
                            return  Behaviors.same();
                        })
                .onMessage(NotFound.class,
                        response->{
                            if(message.getFoundWords().isEmpty()) {
                                message.getReplayTo().tell(new NotFound());
                                return  Behaviors.stopped();
                            }
                            else {
                                getContext().getSelf().tell(new ScandiCrossWordWordFiller.FillDefinitionMatrix(
                                        message.getMatrix(), message.getAffectionTable(), message.getFoundWords(), message.getReplayTo()
                                ));
                            }
                            return  Behaviors.same();
                        })
                .onMessage(ScandiCrossWordWordFiller.ScandiCrossWordResponse.class,
                        response->{

                            message.getReplayTo().tell(new WordTable.ScandiCrossWordResponce( response.getMatrix()));
                            return  Behaviors.stopped();
                        })
                .build();
    }

    private void askLexiconToRankPatterns(ActorRef<Lexicon.Command> ref, Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, String> patternMap) {
        getContext().ask( WordTable.FindWords.class,
                ref,
                Duration.ofMillis(500),
                me -> new Lexicon.RankCroCrossWordPatterns(patternMap,"", me),
                (response,failure) -> {
                    if(response !=null) {
                        if (response.getClass() == WordTable.ErrorFindWords.class)
                            return new NotFound();
                        return new FindMostAffectedResponse();
                    }
                    return new ScandiCrossWordWordFiller.ErrorClass("Lexicon is busy at the moment, sorry!");
                }
        );
    }

    private void askLexiconToSortWords(ActorRef<Lexicon.Command> ref, Map<String, List<String>> patternsTosSort, ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition) {
        getContext().ask( WordTable.FindWords.class,
                ref,
                Duration.ofMillis(500),
                me -> new Lexicon.SortCrosswordWords(patternsTosSort,"", me),
                (response,failure) -> {
                    if(response !=null) {
                        if (response.getClass() == WordTable.ErrorFindWords.class)
                            return new NotFound();
                        return new CreatePatternMapResponse(((Lexicon.SortCrosswordWordsResponse) response).getSortedWords(), definition);
                    }
                    return new ScandiCrossWordWordFiller.ErrorClass("Lexicon is busy at the moment, sorry!");
                }
        );
    }



    private void askLexiconForCrosswordWord(ActorRef<Lexicon.Command> ref, ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition) {
        getContext().ask( WordTable.FindWords.class,
                ref,
                Duration.ofMillis(300),
                me -> new Lexicon.FillCrossWords(definition.pattern.toString(),1550,me),
                (response,failure) -> {
                    if(response!=null) {
                        if(response.getClass() == WordTable.ErrorFindWords.class)
                            return new NotFound();
                        return new FillMostAffectedResponse(definition, ((WordTable.FillCrossWords)response).getFoundWords());
                    }

                    return new ScandiCrossWordWordFiller.ErrorClass("Lexicon is busy at the moment, sorry!");
                }
        );
    }

    private void askScandiCrossWordFillerWorker(ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[][] affectionTable,
                                                ScandiCrossWordWordFiller.DefinitionMatrix matrix) {
        ScandiCrossWordWordFiller.DefinitionMatrix newMatrix = copyMatrix(matrix);
        ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[][] newAffectionTable =
                updateAffectionTable(newMatrix, affectionTable.length, affectionTable[0].length);

        ActorRef<ScandiCrossWordWordFiller.Command> filler =
             getContext().spawn(ScandiCrossWordFillerWorker.create(), "filler" + ( (new Random()).nextInt(Integer.MAX_VALUE)  )  );
        getContext().ask( WordTable.FindWords.class,
                filler,
                Duration.ofMillis(400),
                me -> new ScandiCrossWordWordFiller.FillDefinitionMatrix(newMatrix, newAffectionTable, new ArrayList<>(), me),
                (response,failure) -> {
                    if(response!=null) {
                        if(response.getClass() == NotFound.class)
                            return new NotFound( "Word Not Found");
                        return new ScandiCrossWordWordFiller.ScandiCrossWordResponse( ((WordTable.ScandiCrossWordResponce)response).getMatrix());
                    }
                    getContext().stop(filler);
                    return new ScandiCrossWordWordFiller.ErrorClass("Lexicon is busy at the moment, sorry!");
                }
        );
    }

    private ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[][] updateAffectionTable(ScandiCrossWordWordFiller.DefinitionMatrix newMatrix, int height, int width) {
        ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[][] affectedTable =
                new ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[height][width];
        for(ScandiCrossWordWordFiller.DefinitionMatrix.Definition def: newMatrix.getDefinitions()) {
            for(int i=0; i<def.getPositios().size(); i++) {
                int posY = def.getPositios().get(i)[0];
                int posX = def.getPositios().get(i)[1];
                List<Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, Integer>> newAffectionEntry;
                if(affectedTable[posY][posX] == null) {
                    affectedTable[posY][posX] = new ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell(new HashMap<>());
                    newAffectionEntry = new ArrayList<>();
                }
                else {
                    newAffectionEntry = affectedTable[posY][posX].getAffectionMap().get(posY*100 + posX);
                }
                newAffectionEntry.add(Collections.singletonMap(def,i));
                affectedTable[posY][posX].getAffectionMap().put(posY*100+posX, newAffectionEntry);
            }
        }

        return affectedTable;
    }
    //Map<Integer, List<Map<DefinitionMatrix.Definition, Integer>>> affectionMap;




    private ScandiCrossWordWordFiller.DefinitionMatrix copyMatrix(ScandiCrossWordWordFiller.DefinitionMatrix matrix) {
        List<ScandiCrossWordWordFiller.DefinitionMatrix.Definition> definitions =
                matrix.getDefinitions().stream()
                        .map(m -> new ScandiCrossWordWordFiller.DefinitionMatrix.Definition(
                                new ScandinawCrosswordWordDefinition(m.getDefinition().getWord(),
                                        null, m.getDefinition().getDirection(), m.getDefinition().getLength(),
                                        0, m.getDefinition().getPositio()),
                                new StringBuilder(m.getPattern()), m.getPositios(), m.getAffected(), m.isCompleted()
                        ))
                        .collect(Collectors.toList());
        return new ScandiCrossWordWordFiller.DefinitionMatrix(definitions);
    }

    private List<String> updatePatterns(String word,ScandiCrossWordWordFiller.DefinitionMatrix.Definition definition, ScandiCrossWordWordFiller.FillDefinitionMatrix.AffectionTableCell[][] affectionTable, boolean update) {
        List<String> updatedPatterns = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder(word);

        for(int i=0; i<definition.getPositios().size(); i++) {
            int height = definition.getPositios().get(i)[0];
            int width = definition.getPositios().get(i)[1];
            List<Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, Integer>> patternsToUpdate =
                    affectionTable[height][width].getAffectionMap().get(height*100 + width);
            if(patternsToUpdate !=null) {
                for(Map<ScandiCrossWordWordFiller.DefinitionMatrix.Definition, Integer> patternMap: patternsToUpdate) {
                    ScandiCrossWordWordFiller.DefinitionMatrix.Definition  currentDefinition =
                            (ScandiCrossWordWordFiller.DefinitionMatrix.Definition)patternMap.keySet().toArray()[0];
                    if(currentDefinition.isCompleted())
                        continue;
                    StringBuilder newPattern = currentDefinition.getPattern();
                    newPattern.setCharAt(patternMap.get(currentDefinition), currentWord.charAt(i));

                    updatedPatterns.add(newPattern.toString().toLowerCase());
                }
            }

        }

        return updatedPatterns;
    }
}

