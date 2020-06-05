package actors.wordsearchgame.creators;

import actors.utils.WordTable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinitionCube;


import java.time.Duration;
import java.util.*;

public class ScandiCrossWordWordFiller extends AbstractBehavior<ScandiCrossWordWordFiller.Command> {
    public interface Command {}

    public static class FillTableWithWords implements Command {
        ScandinawCrosswordWordDefinitionCube[][] definitionTable;
        private ActorRef<WordTable.FindWords> replayTo;

        public FillTableWithWords(ScandinawCrosswordWordDefinitionCube[][] definitionTable, ActorRef<WordTable.FindWords> replayTo) {
            this.definitionTable = definitionTable;
            this.replayTo = replayTo;
        }

        public ScandinawCrosswordWordDefinitionCube[][] getDefinitionTable() {
            return definitionTable;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }
    }

    public static class DefinitionMatrix implements Command {
        public static class Definition implements Comparable<Definition> {
            ScandinawCrosswordWordDefinition definition;
           // String pattern;
            StringBuilder pattern;
            List<int[]> positios;
            int affected = 0;
            boolean completed = false;

            public Definition(ScandinawCrosswordWordDefinition definition, StringBuilder pattern, List<int[]> positios, int affected, boolean completed) {
                this.definition = definition;
                this.pattern = pattern;
                this.positios = positios;
                this.affected = affected;
                this.completed = completed;
            }

            public ScandinawCrosswordWordDefinition getDefinition() {
                return definition;
            }

            public List<int[]> getPositios() {
                return positios;
            }

            public int getAffected() {
                return affected;
            }

            public void setAffected(int affected) {
                this.affected = affected;
            }

            public boolean isCompleted() {
                return completed;
            }

            public void setCompleted(boolean completed) {
                this.completed = completed;
            }

            public StringBuilder getPattern() {
                return pattern;
            }

            public void setPattern(StringBuilder pattern) {
                this.pattern = pattern;
            }

            @Override
            public int compareTo(Definition o) {
                return (affected == o.affected) ? o.pattern.length()-pattern.length()
                        : o.affected - affected;
            }
        }
        private List<Definition> definitions;

        public DefinitionMatrix(List<Definition> definitions) {
            this.definitions = definitions;
        }

        public List<Definition> getDefinitions() {
            return definitions;
        }
    }

    public static class FillDefinitionMatrix implements Command {
        public static class AffectionTableCell {
            Map<Integer, List<Map<DefinitionMatrix.Definition, Integer>>> affectionMap;

            public Map<Integer, List<Map<DefinitionMatrix.Definition, Integer>>> getAffectionMap() {
                return affectionMap;
            }

            public AffectionTableCell(Map<Integer, List<Map<DefinitionMatrix.Definition, Integer>>> affectionMap) {
                this.affectionMap = affectionMap;


            }
        }
        private DefinitionMatrix matrix;
        private AffectionTableCell[][] affectionTable;
        private List<String> foundWords;
        private ActorRef<WordTable.FindWords> replayTo;
        private DefinitionMatrix.Definition mostAffected;

        public FillDefinitionMatrix(DefinitionMatrix matrix, AffectionTableCell[][] affectionTable, List<String> foundWords, ActorRef<WordTable.FindWords> replayTo) {
            this.matrix = matrix;
            this.affectionTable = affectionTable;
            this.foundWords = foundWords;
            this.replayTo = replayTo;
        }

        public DefinitionMatrix getMatrix() {
            return matrix;
        }

        public List<String> getFoundWords() {
            return foundWords;
        }

        public ActorRef<WordTable.FindWords> getReplayTo() {
            return replayTo;
        }

        public DefinitionMatrix.Definition getMostAffected() {
            return mostAffected;
        }

        public void setMostAffected(DefinitionMatrix.Definition mostAffected) {
            this.mostAffected = mostAffected;
        }

        public void setFoundWords(List<String> foundWords) {
            this.foundWords = foundWords;
        }

        public AffectionTableCell[][] getAffectionTable() {
            return affectionTable;
        }
    }


    public static class ScandiCrossWordResponse implements Command, WordTable.FindWords {
        DefinitionMatrix matrix;;

        public ScandiCrossWordResponse(DefinitionMatrix matrix) {
            this.matrix = matrix;
        }

        public DefinitionMatrix getMatrix() {
            return matrix;
        }
    }


    public static class ErrorClass implements Command, WordTable.FindWords{
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
                            ctx -> new ScandiCrossWordWordFiller(ctx, stx)
                    );
                }
        );

    }

    public ScandiCrossWordWordFiller(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;

    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
           .onMessage(FillTableWithWords.class,
                msg -> {
                    System.out.println("ScandiCrosswordFiller createReceive!!!!!!!!!!!!!");
                    this.buffer.stash(msg);
                    return buffer.unstashAll(createDefinitionMatrix(msg));
                })
                .build();
    }
    public Receive<Command> createDefinitionMatrix(FillTableWithWords message) {
        return newReceiveBuilder()
                .onMessage(FillTableWithWords.class,
                        msg -> {
                            setupDefinitionMatrix(msg.getDefinitionTable());
                            return Behaviors.same();
                        })
                .onMessage(DefinitionMatrix.class,
                        msg -> {
                            int height = message.getDefinitionTable().length;
                            int width = message.getDefinitionTable()[0].length;
                            FillDefinitionMatrix.AffectionTableCell[][] affectedTable =
                                    createAffectionTable(msg, height, width);
                            this.buffer.stash(msg);
                            return buffer.unstashAll(fillDefinitionMatrix(affectedTable,msg, message.getReplayTo()));
                        })
                .build();
    }
    //Map<Integer, List<Map<DefinitionMatrix.Definition, Integer>>> affectionMap;


    private Behavior<Command> fillDefinitionMatrix(FillDefinitionMatrix.AffectionTableCell[][] affectedTable, DefinitionMatrix matrix, ActorRef<WordTable.FindWords> replayTo) {
        return newReceiveBuilder()
             .onMessage(DefinitionMatrix.class,
                     msg -> {
                          askScandiCrossWordFillerWorker(affectedTable, matrix);
                          return Behaviors.same();
                      })
             .onMessage(ScandiCrossWordResponse.class,
                 msg -> {
                     replayTo.tell(new WordTable.ScandiCrossWordResponce(msg.getMatrix()));

                     return Behaviors.stopped();
                })
                .onMessage(ScandiCrossWordFillerWorker.NotFound.class,
                        msg -> {
                            replayTo.tell(new WordTable.ErrorFindWords("FAILED TO FILL WITH WORDS"));

                            return Behaviors.stopped();
                        })
                .build();
    }

    private void askScandiCrossWordFillerWorker(FillDefinitionMatrix.AffectionTableCell[][] affectionTable, DefinitionMatrix matrix) {
        ActorRef<ScandiCrossWordWordFiller.Command> filler =
                getContext().spawn(ScandiCrossWordFillerWorker.create(), "filler");
        getContext().ask( WordTable.FindWords.class,
                filler,
                Duration.ofMillis(500),
                me -> new FillDefinitionMatrix(matrix, affectionTable, new LinkedList<>(), me),
                (response,failure) -> {
                    if(response!=null) {
                        if(response.getClass() == ScandiCrossWordFillerWorker.NotFound.class)
                            return new ScandiCrossWordFillerWorker.NotFound();
                        return new ScandiCrossWordResponse( ((WordTable.ScandiCrossWordResponce)response).getMatrix());

                    }
                    getContext().stop(filler);
                    return new ErrorClass("Lexicon is busy at the moment, sorry!");
                }
        );
    }

    private void setupDefinitionMatrix(ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
        List<DefinitionMatrix.Definition> definitions = new ArrayList<>();
        for(int y=0; y<definitionTable.length;y++) {
            for(int x=0; x<definitionTable[0].length; x++) {
                if(definitionTable[y][x] != null) {
                    List<DefinitionMatrix.Definition> matrixDefinition = createMatrixDefinition(definitionTable[y][x]);
                    for(DefinitionMatrix.Definition d: matrixDefinition) {
                        definitions.add(d);
                    }
                }
            }
        }

        DefinitionMatrix dm = new DefinitionMatrix(definitions);
        getContext().getSelf().tell(dm);

    }

    private List<DefinitionMatrix.Definition> createMatrixDefinition(ScandinawCrosswordWordDefinitionCube scandinawCrosswordWordDefinitionCube) {
        List<int[]> definitionPositions;
        List<DefinitionMatrix.Definition> definitionMatrix = new ArrayList<>();
        StringBuilder pattern;

        for(ScandinawCrosswordWordDefinition d: scandinawCrosswordWordDefinitionCube.getDefinitions()) {
            definitionPositions = createDefinitionPositions(d);
             pattern = new StringBuilder( new String(new char[d.getLength()]).replace('\0', '#') );
            definitionMatrix.add(new DefinitionMatrix.Definition(d, pattern,definitionPositions,0, false));
        }
        return  definitionMatrix;
    }

    private List<int[]> createDefinitionPositions(ScandinawCrosswordWordDefinition d) {
        List<int[]> definitionPositions = new ArrayList<>();
        int y = d.getPositio().y + d.getDirection().getOffSetPoints()[1];
        int x = d.getPositio().x + d.getDirection().getOffSetPoints()[0];
        int addTox = d.getDirection().isForward() ? 1 : 0;
        int addToY = d.getDirection().isForward() ? 0 : 1;
        int length = d.getLength();

        for(int i=0;i<length;i++){
            definitionPositions.add(new int[] {y,x});
            x+=addTox;
            y+=addToY;
        }
        return definitionPositions;
    }

    private FillDefinitionMatrix.AffectionTableCell[][] createAffectionTable(DefinitionMatrix definitionMatrix, int height, int width) {
        FillDefinitionMatrix.AffectionTableCell[][] affectedTable =
                new FillDefinitionMatrix.AffectionTableCell[height][width];
        for(DefinitionMatrix.Definition def: definitionMatrix.getDefinitions()) {
            for(int i=0; i<def.getPositios().size(); i++) {
                int posY = def.getPositios().get(i)[0];
                int posX = def.getPositios().get(i)[1];
                List<Map<DefinitionMatrix.Definition, Integer>> newAffectionEntry;
                if(affectedTable[posY][posX] == null) {
                    affectedTable[posY][posX] = new FillDefinitionMatrix.AffectionTableCell(new HashMap<>());
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
}


/*     System.out.println("~n~n--------------------------------------------------\n--------------------------->\n\n");
                            System.out.println("\t\t D  E  F  I  N  I  T  I  O  N     M  A  T  R  I  X\n\n");
                            for(int i=0; i<msg.getDefinitions().size();i++) {
                                DefinitionMatrix.Definition dmd = msg.getDefinitions().get(i);
                                ScandinawCrosswordWordDefinition definition = dmd.getDefinition();
                                List<int[]> defpos = dmd.getPositios();
                                System.out.print(i + ": " + definition.getDirection() + " " + definition.getPositio().y
                                        + definition.getPositio().x + "  --> ");
                                for(int[] ps:defpos)
                                    System.out.print( " [" + ps[0] + ":"  + ps[1] + "] ") ;
                                System.out.println("");

                            }  */