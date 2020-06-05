package actors.wordsearchgame.controllers;

import actors.utils.WordTable;
import actors.wordsearchgame.creators.ScandiCrossWordWordFiller;
import actors.wordsearchgame.creators.ScandiDefinitionTableCreator;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinitionCube;

import java.time.Duration;
import java.util.Random;

public class ScandiCrosswordCreatorController extends AbstractBehavior<ScandiCrosswordCreatorController.Command> {

    public interface Command {}

    public static class ErrorClass implements CrosswordCreatorController.Command, Command, WordTable.FindWords {
        public String message;

        public ErrorClass(String message) {
            this.message = message;
        }
    }

    public static class NotFound implements Command {}

    public static class Scandinavian implements     CrosswordCreatorController.Command,
                                                    WordRootController.Command,
                                                    ScandiDefinitionTableCreator.Command,
                                                    Command {

        private Size size;
        private String topic;
        private String solution = null;
        private ActorRef<Object> replayTo;
        public Scandinavian(Size size, String topic, String anecdote, ActorRef<Object> replayTo) {
            this.size = size;
            this.topic = topic;
            this.solution = anecdote;
            this.replayTo = replayTo;
        }

        public Scandinavian(Size size, String topic, ActorRef<Object> replayTo) {
            this.size = size;
            this.topic = topic;
            this.replayTo = replayTo;
        }



        public Size getSize() {
            return size;
        }

        public String getTopic() {
            return topic;
        }

        public String getSolution() {
            return solution;
        }

        public ActorRef<Object> getReplayTo() {
            return replayTo;
        }


        public enum Size {
            SMALL(new int[]{15, 15}),
            MEDIUM(new int[]{10, 10}),
            LARGE(new int[]{30, 30});

            private int[] tableSize;

            Size(int[] tableSize) {
                this.tableSize = tableSize;
            }

            public char[][] getTable() {
                return new char[tableSize[0]][tableSize[1]];
            }

            public int[] getTableSize() {
                return tableSize;
            }
        }
    }

    public static class Start extends Scandinavian {
        private ActorRef<Command> replayTo;

        public Start(Size size, String topic, ActorRef<Object> replayTo, ActorRef<Command> replayTo1) {
            super(size, topic, replayTo);
            this.replayTo = replayTo1;
        }


        public ActorRef<Command>getReplayToChild() {
            return this.replayTo;
        }
    }

    public static class DefinitionSetupResponse implements Command,
                                                           ScandiDefinitionTableCreator.Command,
                                                           WordTable.FindWords {
        private ScandinawCrosswordWordDefinitionCube[][] definitionTable;

        public DefinitionSetupResponse(ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
            this.definitionTable = definitionTable;
        }

        public ScandinawCrosswordWordDefinitionCube[][] getDefinitionTable() {
            return definitionTable;
        }
    }

   public static class FillScandiTableResponse implements Command {
       private ScandinawCrosswordWordDefinitionCube[][] definitionTable;

       public FillScandiTableResponse(ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
           this.definitionTable = definitionTable;
       }

       public ScandinawCrosswordWordDefinitionCube[][] getDefinitionTable() {
           return definitionTable;
       }
   }

   public static class ScandiControllerResponse implements Command {
       private ScandiTableCell[][] table;
       private ScandinawCrosswordWordDefinitionCube[][] definitionTable;
       private String pictureSrc;

       public ScandiControllerResponse(ScandiTableCell[][] table, ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
           this.table = table;
           this.definitionTable = definitionTable;
       }

       public ScandiTableCell[][] getTable() {
           return table;
       }

       public ScandinawCrosswordWordDefinitionCube[][] getDefinitionTable() {
           return definitionTable;
       }

       public void setPictureSrc(String pictureSrc) {
           this.pictureSrc = pictureSrc;
       }

       public String getPictureSrc() {
           return pictureSrc;
       }
   }


    private StashBuffer<Command> buffer;
    public static Behavior<Command> create() {
        return Behaviors.withStash(1,
                stx -> Behaviors.setup(
                        ctx -> new ScandiCrosswordCreatorController(ctx, stx)
                )
        );
    }

    private int trynum;
    private long counter;
    private ScandiCrosswordCreatorController(ActorContext<Command> context, StashBuffer<Command> buffer) {
        super(context);
        this.buffer = buffer;
        this.trynum = 250;
        this.counter = 0L;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Scandinavian.class,
                        msg -> {
                            buffer.stash(msg);
                            int tryNum = 50;
                            return buffer.unstashAll(definitionTableReceive(msg));
                        })
                .build();
    }

    private Behavior<Command> definitionTableReceive(Scandinavian message) {
        return newReceiveBuilder()
                .onMessage(Scandinavian.class,
                        msg -> {
                            askDefinitionSetup(message);
                             return Behaviors.same();
                        })
                .onMessage(DefinitionSetupResponse.class,
                        msg -> {
                            askScandiCrossWordFiller(msg.getDefinitionTable());
                            return Behaviors.same();
                        })
                .onMessage(NotFound.class,
                        msg -> {
                            if(--this.trynum == 0) {
                                message.getReplayTo().tell(new ErrorClass("Scandi table setup failed"));
                                return Behaviors.stopped();
                            }
                            askDefinitionSetup(message);
                            return Behaviors.same();
                        })
                .onMessage(FillScandiTableResponse.class,
                        msg -> {
                            ScandiTableCell[][] table = fillTable(msg.getDefinitionTable());
                            if( fillSolution(table, message.getSolution()) ) {
                                message.getReplayTo().tell(new ScandiControllerResponse(table, msg.getDefinitionTable()));
                                return Behaviors.stopped();
                            }
                            else {
                                System.out.println("\n\n\n\n" + this.trynum + "\n\n\n\n\n");
                                if(--this.trynum == 0) {
                                    message.getReplayTo().tell(new ErrorClass("Couldnt create scandi table"));
                                    return Behaviors.stopped();
                                }
                            }
                            askDefinitionSetup(message);
                            return Behaviors.same();
                        })
                .onMessage(ErrorClass.class,
                        msg -> {
                            System.out.println("\n\n\n\n" + this.trynum + "\n\n\n\n\n");
                            if(--this.trynum == 0) {
                                message.getReplayTo().tell(new ErrorClass(msg.message));
                                return Behaviors.stopped();
                            }
                            askDefinitionSetup(message);
                            return Behaviors.same();
                        })
                 .onSignal(Terminated.class, msg -> Behaviors.same())
                .build();
    }




    private void askDefinitionSetup(Scandinavian message) {
        this.counter = (++this.counter) % (Long.MAX_VALUE -2);
        ActorRef<ScandiCrosswordCreatorController.Command> creator =
                getContext().spawn(ScandiDefinitionTableCreator.create(), "creator" + this.counter +  (new Random()).nextInt(Integer.MAX_VALUE));
        getContext().ask(Command.class,
                creator,
                Duration.ofMillis(500),
                me -> new Start(message.size, message.topic, message.replayTo, me) {
                },
                (response, failure) -> {
                    if (response != null) {
                        if (response.getClass() == NotFound.class)
                            return new NotFound();
                        else if(response.getClass() == ErrorClass.class)
                            return new ErrorClass("Definition setup failed");
                        return new DefinitionSetupResponse( ((DefinitionSetupResponse)response).getDefinitionTable()  );
                    }
                    getContext().stop(creator);
                    return new ErrorClass("Definition Setup  Timed out");
                }

        );
    }

    private void askScandiCrossWordFiller(ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
        this.counter = (++this.counter) % (Long.MAX_VALUE -2);
        ActorRef<ScandiCrossWordWordFiller.Command> filler =
             getContext().spawn(ScandiCrossWordWordFiller.create(), "fff" + this.counter  );
        getContext().ask(WordTable.FindWords.class,
                filler,
                Duration.ofMillis(500),
                me -> new ScandiCrossWordWordFiller.FillTableWithWords(definitionTable,  me) {
                },
                (response, failure) -> {
                    if (response != null) {
                        if (response.getClass() == WordTable.ErrorFindWords.class)
                            return new ErrorClass(((WordTable.ErrorFindWords) response).message);
                        ScandinawCrosswordWordDefinitionCube[][] newDefinitionTable =
                                createDefinitionTableFromMatrix(((WordTable.ScandiCrossWordResponce) response).getMatrix(),
                                        definitionTable.length,definitionTable[0].length);
                        return new FillScandiTableResponse(newDefinitionTable);
                    }
                    getContext().stop(filler);
                    return new ErrorClass("Lexicon is busy at the moment, sorry!");
                }

        );
    }

    private ScandinawCrosswordWordDefinitionCube[][] createDefinitionTableFromMatrix(ScandiCrossWordWordFiller.DefinitionMatrix matrix, int length, int width) {
        ScandinawCrosswordWordDefinitionCube[][] newDefinitionTable = new ScandinawCrosswordWordDefinitionCube[length][width];
        for(ScandiCrossWordWordFiller.DefinitionMatrix.Definition def : matrix.getDefinitions()) {
            int y = def.getDefinition().getPositio().y;
            int x = def.getDefinition().getPositio().x;
            if(newDefinitionTable[y][x]  ==null) {
                newDefinitionTable[y][x] = new ScandinawCrosswordWordDefinitionCube();
            }
            newDefinitionTable[y][x].addDefinition(def.getDefinition());
        }
        return newDefinitionTable;
    }


    public static class ScandiTableCell {
        public char letter;
        public boolean solution;
        public int index = 0;
        public ScandiTableCell(char letter, boolean solution) {
            super();
            this.letter = letter;
            this.solution = solution;
        }
    }

    private ScandiTableCell[][] fillTable(ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
        ScandiTableCell[][] table = new ScandiTableCell[definitionTable.length][definitionTable[0].length];

        for(int y=0; y< definitionTable.length; y++ ) {
            for(int x=0;x<definitionTable[0].length;x++) {
                if(definitionTable[y][x] !=null) {
                    for(ScandinawCrosswordWordDefinition def: definitionTable[y][x].getDefinitions() ) {
                        fillTableCell(def,table, y, x);
                    }
                }
            }
        }

        return table;
    }

    private void fillTableCell(ScandinawCrosswordWordDefinition def, ScandiTableCell[][] table, int startY, int startX) {
        int x = startX + def.getDirection().getOffSetPoints()[0];
        int y = startY + def.getDirection().getOffSetPoints()[1];
        int addToX = def.getDirection().isForward() ? 1 : 0;
        int addToY = def.getDirection().isForward() ? 0 : 1;


        int length = def.getWord().length();
        for(int i=0; i<length;i++) {
            table[y][x] = new ScandiTableCell(def.getWord().charAt(i), false);
            x += addToX;
            y += addToY;
        }
    }

    private boolean fillSolution(ScandiTableCell[][] table, String solution) {
        boolean passed = true;
        int index =0;
        for(int i=0; i<solution.length() && passed; i++) {
            char currentChar = solution.charAt(i);
            passed = false;
            outer:
            for (ScandiTableCell[] scandiTableCells : table) {
                for (int x = 0; x < table[0].length; x++) {
                    if (scandiTableCells[x] !=null && scandiTableCells[x].letter == currentChar && scandiTableCells[x].index == 0) {
                        passed = true;
                        scandiTableCells[x].solution = true;
                        scandiTableCells[x].index = ++index;
                        break outer;
                    }
                }
            }
        }

        return  passed;
    }
}
