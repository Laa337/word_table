package actors.wordsearchgame.creators;

import actors.wordsearchgame.controllers.ScandiCrosswordCreatorController;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinitionCube;
import utils.Positio;
import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition;


import java.time.Duration;
import java.util.*;

import static models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition.Direction.*;

public class ScandiDefinitionTableCreator extends AbstractBehavior<ScandiCrosswordCreatorController.Command> {
    public interface Command {}

    private static class NextPositio implements ScandiCrosswordCreatorController.Command {}

    private static class FillPositionsList implements ScandiCrosswordCreatorController.Command {}

    private static class FillPositionsListResponse implements ScandiCrosswordCreatorController.Command {
        List<Positio> positions;

        public FillPositionsListResponse(List<Positio> positions) {
            this.positions = positions;
        }
    }

    private static class CheckIfUnreachablePointsExists implements ScandiCrosswordCreatorController.Command {
        private Positio currentPosition;
        private ScandinawCrosswordWordDefinition.Direction direction;

        public CheckIfUnreachablePointsExists(Positio currentPosition, ScandinawCrosswordWordDefinition.Direction direction) {
            this.currentPosition = currentPosition;
            this.direction = direction;
        }
    }

    private static class FindDefinitionLength implements ScandiCrosswordCreatorController.Command {
        private Positio currentPosition;
        private ScandinawCrosswordWordDefinition.Direction direction;

        public FindDefinitionLength(Positio currentPosition, ScandinawCrosswordWordDefinition.Direction direction) {
            this.currentPosition = currentPosition;
            this.direction = direction;
        }
    }

    private static class ChooseNextStep implements ScandiCrosswordCreatorController.Command {
        private Positio currentPosition;
        private ScandinawCrosswordWordDefinition.Direction direction;
        int length;

        public ChooseNextStep(Positio currentPosition, ScandinawCrosswordWordDefinition.Direction direction, int length) {
            this.currentPosition = currentPosition;
            this.direction = direction;
            this.length = length;
        }
    }

    public static class ContinueTableBuilding implements ScandiCrosswordCreatorController.Command {
        private ScandinawCrosswordWordDefinitionCube[][] definitionTable;
        private char[][] helperTable;
        private Positio position;
        private ActorRef<ScandiCrosswordCreatorController.Command> replayTo;

        public ContinueTableBuilding(ScandinawCrosswordWordDefinitionCube[][] definitionTable, char[][] helperTable, Positio position, ActorRef<ScandiCrosswordCreatorController.Command> replayTo) {
            this.definitionTable = definitionTable;
            this.helperTable = helperTable;
            this.position = position;
            this.replayTo = replayTo;
        }

        public ScandinawCrosswordWordDefinitionCube[][] getDefinitionTable() {
            return definitionTable;
        }

        public char[][] getHelperTable() {
            return helperTable;
        }

        public Positio getPosition() {
            return position;
        }

        public ActorRef<ScandiCrosswordCreatorController.Command> getReplayTo() {
            return replayTo;
        }
    }


    private static class DirectionScore implements Comparable<DirectionScore> {
        public ScandinawCrosswordWordDefinition.Direction direction;
        public int score;

        public DirectionScore(ScandinawCrosswordWordDefinition.Direction direction, int score) {
            this.direction = direction;
            this.score = score;
        }


        @Override
        public int compareTo(DirectionScore o) {
            return  this.score - o.score;
        }
    }

    private StashBuffer<ScandiCrosswordCreatorController.Command> buffer;
    public static Behavior<ScandiCrosswordCreatorController.Command> create() {
        return Behaviors.withStash(1,
                stx -> Behaviors.setup(
                        ctx -> new ScandiDefinitionTableCreator(ctx, stx)
                )
        );
    }

    private ScandiDefinitionTableCreator(ActorContext<ScandiCrosswordCreatorController.Command> context, StashBuffer<ScandiCrosswordCreatorController.Command> buffer) {
        super(context);
        this.buffer = buffer;
    }

    @Override
    public Receive<ScandiCrosswordCreatorController.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ScandiCrosswordCreatorController.Start.class,
                        msg -> {
                            buffer.stash(msg);
                            return buffer.unstashAll(initReceive());
                        })
                .onMessage(ContinueTableBuilding.class,
                        msg -> {
                            buffer.stash(new NextPositio());
                            return buffer.unstashAll(definitionTableBuildReceive(msg.getReplayTo(),
                                    msg.definitionTable, msg.helperTable, msg.position));
                        })
                .build();
    }

    private Behavior<ScandiCrosswordCreatorController.Command> initReceive() {
        return newReceiveBuilder()
                .onMessage(ScandiCrosswordCreatorController.Start.class,
                        msg -> {
                            char[][] helperTable = createHelperTable(msg);
                            ScandinawCrosswordWordDefinitionCube[][] definitionTable =  createDefinitionTable(msg);

                            buffer.stash(new NextPositio());
                            return buffer.unstashAll(definitionTableBuildReceive(msg.getReplayToChild(),definitionTable, helperTable, null));
                        })
                .build();
    }

    private Behavior<ScandiCrosswordCreatorController.Command> definitionTableBuildReceive(
            ActorRef<ScandiCrosswordCreatorController.Command> replayTo, ScandinawCrosswordWordDefinitionCube[][] definitionTable,
            char[][] helperTable, Positio pos) {
        return newReceiveBuilder()
                .onMessage(NextPositio.class,
                        msg -> {
                            getContext().getSelf().tell(new FillPositionsList());
                            return Behaviors.same();
                        })
                .onMessage(FillPositionsList.class,
                        msg -> {
                            List<Positio> positions = findPositions(helperTable, definitionTable, pos);
                            getContext().getSelf().tell(new FillPositionsListResponse(positions));
                            return Behaviors.same();
                        })
                .onMessage(FillPositionsListResponse.class,
                        msg -> {
                            this.buffer.stash(msg);
                            return buffer.unstashAll(positionsLoopReceive(replayTo,definitionTable, helperTable, msg.positions));

                        })
                .build();
    }

    private Behavior<ScandiCrosswordCreatorController.Command> positionsLoopReceive(ActorRef<ScandiCrosswordCreatorController.Command> replayTo,
                                                   ScandinawCrosswordWordDefinitionCube[][] definitionTable,
                                                   char[][] helperTable, List<Positio> positions) {
         return newReceiveBuilder()
                 .onMessage(FillPositionsListResponse.class,
                     msg -> {
                         if(positions == null || positions.isEmpty()) {
                             replayTo.tell(new ScandiCrosswordCreatorController.NotFound());
                             return Behaviors.stopped();
                         }
                          Positio currentPosition = ((LinkedList<Positio>) positions).removeFirst();
                          ScandinawCrosswordWordDefinition.Direction direction = isThereRowToInsertDefinition(definitionTable,helperTable, currentPosition);
                          if(direction == null) {
                              getContext().getSelf().tell(new FillPositionsListResponse(positions));
                              return Behaviors.same();
                          }
                          getContext().getSelf().tell(new CheckIfUnreachablePointsExists(currentPosition, direction));

                          return Behaviors.same();
                  })
                 .onMessage(CheckIfUnreachablePointsExists.class,
                         msg -> {
                             if (!checkIfUnreachablePointsExists(helperTable, msg.currentPosition)) {
                                 getContext().getSelf().tell(new FillPositionsListResponse(positions));
                                 return Behaviors.same();
                             }
                             getContext().getSelf().tell(new FindDefinitionLength(msg.currentPosition, msg.direction));
                             return Behaviors.same();
                         })
                 .onMessage(FindDefinitionLength.class,
                         msg -> {
                             int length = findDefinitionLength(helperTable, msg.currentPosition.x , msg.currentPosition.y , msg.direction);
                             if(length == 0 || length >9) {
                                 getContext().getSelf().tell(new FillPositionsListResponse(positions));
                                 return Behaviors.same();
                             }
                             getContext().getSelf().tell(new ChooseNextStep(msg.currentPosition,msg.direction,length));

                             return Behaviors.same();
                         })
                 .onMessage(ChooseNextStep.class,
                         msg -> {
                             char[][] tempHelperTable  = updateHelperTable(helperTable, msg.currentPosition, msg.direction, msg.length);
                             ScandinawCrosswordWordDefinitionCube[][] tempDefinitionTable =
                                     createUpdatedDefinitionTable(definitionTable, msg.currentPosition, msg.direction, msg.length);
                             int helperTableFull = helperTableIsFull(tempHelperTable);

                             if(helperTableFull == 0) {
                                 replayTo.tell(new ScandiCrosswordCreatorController.DefinitionSetupResponse(tempDefinitionTable));
                                 return Behaviors.stopped();
                             }
                             else if (helperTableFull < 9) {
                                 try {
                                     tempDefinitionTable = fillLastRemainingPositions(tempDefinitionTable, tempHelperTable);
                                 } catch(Exception e) {
                                     replayTo.tell(new ScandiCrosswordCreatorController.ErrorClass("Definition setup failed"));
                                     return Behaviors.stopped();
                                 }
                                 replayTo.tell(new ScandiCrosswordCreatorController.DefinitionSetupResponse(tempDefinitionTable));
                                 return Behaviors.stopped();
                             }
                             else {
                                 Positio nextPositio = getNextDefinitionPositio(tempHelperTable, msg.currentPosition, msg.direction, msg.length);
                                 askContinueTableBuildeing(tempDefinitionTable,tempHelperTable, nextPositio);
                             }
                             return Behaviors.same();
                         })
                 .onMessage(ScandiCrosswordCreatorController.DefinitionSetupResponse.class,
                         msg -> {
                             replayTo.tell(msg);

                             return Behaviors.same();
                         })
                 .onMessage(ScandiCrosswordCreatorController.ErrorClass.class,
                         msg -> {
                             replayTo.tell(new ScandiCrosswordCreatorController.ErrorClass(""));
                             return Behaviors.stopped();
                         })
                 .onMessage(ScandiCrosswordCreatorController.NotFound.class,
                         msg -> {
                             getContext().getSelf().tell(new FillPositionsListResponse(positions));

                             return Behaviors.same();
                         })
                 .build();
    }

    private void askContinueTableBuildeing( ScandinawCrosswordWordDefinitionCube[][] tempDefinitionTable, char[][] tempHelperTable, Positio nextPositio) {
        getContext().ask(ScandiCrosswordCreatorController.Command.class,
                getContext().spawn(ScandiDefinitionTableCreator.create(), Arrays.deepHashCode(tempDefinitionTable) + "" + ((new Random().nextInt(30000)))),
                Duration.ofMillis(200000),
                me -> new ContinueTableBuilding(tempDefinitionTable, tempHelperTable,nextPositio, me),
                (response, failure) -> {
                    if (response != null) {
                        if (response.getClass() == ScandiCrosswordCreatorController.NotFound.class )
                            return new ScandiCrosswordCreatorController.NotFound();
                        if(response.getClass() == ScandiCrosswordCreatorController.ErrorClass.class)
                            return new ScandiCrosswordCreatorController.ErrorClass("Definition setup failed");
                        return new ScandiCrosswordCreatorController.DefinitionSetupResponse( ((ScandiCrosswordCreatorController.DefinitionSetupResponse)response).getDefinitionTable()  );
                    }

                    return new ScandiCrosswordCreatorController.ErrorClass("Definition Setup  Timed out");
                }

        );
    }


    private char[][] createHelperTable(ScandiCrosswordCreatorController.Scandinavian msg) {
        char[][] helperTable = msg.getSize().getTable();
        for(int y=0; y<4;y++){
            for(int x=0; x<5; x++) {
                helperTable[y][x] = 'k';
            }
        }
        return helperTable;
    }

    private ScandinawCrosswordWordDefinitionCube[][] createDefinitionTable(ScandiCrosswordCreatorController.Scandinavian msg) {
        ScandinawCrosswordWordDefinitionCube[][] definitionTable =
            new ScandinawCrosswordWordDefinitionCube[msg.getSize().getTableSize()[0]][msg.getSize().getTableSize()[0]];

        return definitionTable;
    }

    private List<Positio> findPositions(char[][] helperTable, ScandinawCrosswordWordDefinitionCube[][] definitionTable, Positio pos) throws Exception {
        List<Positio> p = new LinkedList<>();
        if (pos == null) {
            p = getAvailablePositions(helperTable, definitionTable);

            if (p.isEmpty())
                throw new Exception("Couldn't create DefinitionTable smthg is wrong definitionTable full at definition insert");
            Collections.shuffle(p);
        } else {
            p.add(pos);
        }

        return p;
    }

    private List<Positio> getAvailablePositions(char[][] helperTable, ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
        List<Positio> result = new LinkedList<>();
        char[][] prohibitedTable = createProhibitedTable(helperTable, definitionTable);
        for (int y = 0; y < helperTable.length; y++) {
            for (int x = 0; x < helperTable.length; x++) {
                if (helperTable[y][x] == '\0' && prohibitedTable[y][x] != 'p')
                    result.add(new Positio(x, y));
            }
        }

        return result;
    }

    private char[][] createProhibitedTable(char[][] helperTable, ScandinawCrosswordWordDefinitionCube[][] definitionTable) {
        char[][] prohibitedTable = new char[helperTable.length][helperTable[0].length];
        for (int y = 0; y < helperTable.length; y++) {
            for (int x = 0; x < helperTable.length; x++) {
                if (helperTable[y][x] == 'd' ) {
                    updateProhibitedTableForDefinition(helperTable, prohibitedTable, definitionTable, x, y);
                } else if (helperTable[y][x] == '\0' ) {
                    updateProhibitedTableForEmpty(helperTable, prohibitedTable, definitionTable, x, y);
                }
            }
        }
        return prohibitedTable;
    }

    private void updateProhibitedTableForEmpty(char[][] helperTable, char[][] prohibitedTable, ScandinawCrosswordWordDefinitionCube[][] definitionTable, int x, int y) {
        if(x==1 && helperTable[y][x-1] == '\0')
            prohibitedTable[y][x]='p';
        else if(y==1 && helperTable[y-1][x] == '\0')
            prohibitedTable[y][x]='p';

        if( x<helperTable[0].length-2  && helperTable[y][x+1] == 'd' && helperTable[y][x+2] == 'd')
            prohibitedTable[y][x] = 'p';
        if( x>1  && helperTable[y][x-1] == 'd' && helperTable[y][x-2] == 'd')
            prohibitedTable[y][x] = 'p';
    }

    private void updateProhibitedTableForDefinition(char[][] helperTable, char[][] prohibitedTable, ScandinawCrosswordWordDefinitionCube[][] definitionTable, int x, int y) {
        if(  y==0 || helperTable[y-1][x] == 'k' ) {
            if(x==1 )
                prohibitedTable[y+1][x-1] = 'p';
            else if(x==helperTable[0].length-2)
                prohibitedTable[y+1][x+1] = 'p';
        }
        else if( y==helperTable.length-1 ) {
            if(x==1 )
                prohibitedTable[y-1][x-1] = 'p';
            else if(x==helperTable[0].length-2)
                prohibitedTable[y-1][x+1] = 'p';
        }
        if(x==0 || helperTable[y][x-1] == 'k' ) {
            if(y==1)
                prohibitedTable[y-1][x+1] = 'p';
            else if(y==helperTable.length-2)
                prohibitedTable[y+1][x+1] = 'p';
        }
        if(x== helperTable[0].length-1 || helperTable[y][x+1] == 'k' ) {
            if(y==1 || y==5)
                prohibitedTable[y-1][x-1] = 'p';
            else if(y==helperTable.length-2)
                prohibitedTable[y+1][x-1] = 'p';
        }
        if(definitionTable[y][x].getDefinitions().get(0).getDirection().isForward()) {
            if( x>0 && y <helperTable.length-2 && helperTable[y+1][x] == '\0') {
                int posY = y+2;
                while(posY <helperTable.length && helperTable[posY][x] =='\0') {
                    prohibitedTable[posY][x] = 'p';
                    posY++;
                }
            }
        }
        else {
            if( y>0 && x <helperTable.length-2 && helperTable[y][x+1] == '\0') {
                int posX = x+2;
                while(posX <helperTable[0].length && helperTable[y][posX] =='\0') {
                    prohibitedTable[y][posX] = 'p';
                    posX++;
                }
            }
        }
    }


    private ScandinawCrosswordWordDefinition.Direction isThereRowToInsertDefinition(ScandinawCrosswordWordDefinitionCube[][] definitionTable, char[][] helperTable, Positio currentPosition) {
        List<DirectionScore> foundDirections = new ArrayList<>();
        List<ScandinawCrosswordWordDefinition.Direction> upAndDown;
        if(currentPosition.y==0 || helperTable[currentPosition.y-1][currentPosition.x] == 'k') {
            if(currentPosition.x<helperTable[0].length -3 && currentPosition.x>1) {
                upAndDown =  Arrays.asList(FORWARD, DOWN, LEFTDOWN, RIGHTDOWN);
            }
            else if(currentPosition.x<helperTable[0].length -3) {
                upAndDown = Arrays.asList(FORWARD, DOWN, RIGHTDOWN);
            }
            else {
                upAndDown = Arrays.asList( DOWN, LEFTDOWN);
            }
        }
        else if(currentPosition.x==0 || helperTable[currentPosition.y][currentPosition.x-1] == 'k' ) {
            if(currentPosition.y<helperTable.length -3 && currentPosition.y>1) {
                upAndDown =  Arrays.asList(FORWARD, DOWN, UPFORWARD, DOWNFORWARD);
            }
            else if(currentPosition.y<helperTable.length -3) {
                upAndDown = Arrays.asList(FORWARD, DOWN, DOWNFORWARD);
            }
            else {
                upAndDown = Arrays.asList(FORWARD, DOWN, UPFORWARD);
            }
        }
        else if(currentPosition.x==helperTable[0].length -2 ) {
            upAndDown = Arrays.asList( DOWN, UPFORWARD);
        }
        else {
            upAndDown = Arrays.asList(FORWARD, DOWN);
        }



        for(ScandinawCrosswordWordDefinition.Direction d: upAndDown) {
            DirectionScore directionScore = checkThisDefinitionInsertPoint(definitionTable,helperTable, currentPosition, d);
            if( directionScore != null) {
                foundDirections.add(directionScore);
            }
        }

        if(!foundDirections.isEmpty()) {
            Collections.sort(foundDirections);
            return foundDirections.get(foundDirections.size()-1).direction;

        }
        return null;
    }

    private DirectionScore checkThisDefinitionInsertPoint(ScandinawCrosswordWordDefinitionCube[][] definitionTable, char[][] helperTable, Positio currentPosition, ScandinawCrosswordWordDefinition.Direction d) {
        int x = currentPosition.x + d.getOffSetPoints()[0];
        int y = currentPosition.y + d.getOffSetPoints()[1];
        int addTox =  d.isForward() ? 1 : 0;
        int addToY =  d.isForward() ? 0 : 1;

        int score = 0;
        do {
            if (x < 0 || y < 0 || x >= helperTable[0].length || y >= helperTable.length)
                break;
            else if (helperTable[y][x] == 'd' || helperTable[y][x] == 'k' )  // kép vagy definitio
                break;
            else if(helperTable[y][x] == '#') {
                score=0;
                break;
            }
            else if (helperTable[y][x] == 'ˇ') {
                if(!d.isForward() ) {
                    score = 0;
                    break;
                }
                else
                    score+=2;
            }
            else if( helperTable[y][x] == '>') {
                if(d.isForward() ) {
                    score = 0;
                    break;
                }
                else
                    score+=2;
            }
            else {
                score+= isThereCrossDefinitionPossibility(definitionTable,x,y,d);
            }
            x+=addTox;
            y+=addToY;
        }while(true);

        if(score > 0)
            return new DirectionScore(d,score);
        return null;
    }

    private int isThereCrossDefinitionPossibility(ScandinawCrosswordWordDefinitionCube[][] definitionTable, int x, int y, ScandinawCrosswordWordDefinition.Direction d) {
        int result = 0;
        int steps = 0;
        int addTox = d.isForward() ? 0 : -1;
        int addToY = d.isForward() ? -1 : 0;

        do {
            if(x < 0 || y < 0 || x >= definitionTable[0].length || y >= definitionTable.length ) {
                if(steps >1)
                    result = 1;
                break;
            }
            if(definitionTable[y][x] != null) {
                result = 0;
            }
            steps++;
            x+=addTox;
            y+=addToY;
        }while(true);

        return result;
    }

    private boolean checkIfUnreachablePointsExists(char[][] helperTable, Positio currentPosition) throws Exception {
        char[][] tempTable = copyHelperTable(helperTable);
        tempTable[currentPosition.y][currentPosition.x] = 'd';
        int existingFreePositions = 0;
        for (int i = 0; i < tempTable.length; i++) {
            for (int j = 0; j < tempTable[0].length; j++) {
                if (tempTable[i][j] == '\0' || tempTable[i][j] == 'a' || tempTable[i][j] == 'ˇ'
                        || tempTable[i][j] == '>' || tempTable[i][j] == '#')
                    existingFreePositions++;
            }
        }
        Positio starterPositio = findStarterPositioForDefinitionInsertValidation(tempTable);
        boolean[][] visitedTable =   new boolean[helperTable.length][helperTable[0].length];
        visitedTable[starterPositio.y][starterPositio.x] = true;
        int availablePositions = countReachablePositions(tempTable, visitedTable, starterPositio, 1);
        return  availablePositions == existingFreePositions;
    }

    private Positio findStarterPositioForDefinitionInsertValidation(char[][] helperTable) throws Exception {
        for (int i = 0; i < helperTable.length; i++) {
            if (helperTable[i][0] != 'd' && helperTable[i][0] != 'k')
                return new Positio(0, i);
        }
        throw new Exception("First coloumn filled abnormally");
    }

    private int countReachablePositions(char[][] helperTable, boolean[][] positios, Positio currentPositio, int foundPositios) {

        List<Positio> neighBourFreePositions = new ArrayList<>();
        int y = currentPositio.y ;
        int x = currentPositio.x-1;
        if(checkThisPosition(positios, helperTable, y, x)) {
            positios[y][x] = true;
            neighBourFreePositions.add(new Positio(x, y));
            foundPositios++;
        }
        y= currentPositio.y -1;
        x = currentPositio.x;
        if(checkThisPosition(positios, helperTable, y, x)) {
            positios[y][x] = true;
            neighBourFreePositions.add(new Positio(x, y));
            foundPositios++;
        }
        y= currentPositio.y +1;
        x = currentPositio.x;
        if(checkThisPosition(positios, helperTable, y, x)) {
            positios[y][x] = true;
            neighBourFreePositions.add(new Positio(x, y));
            foundPositios++;
        }
        y = currentPositio.y ;
        x = currentPositio.x+1;
        if(checkThisPosition(positios, helperTable, y, x)) {
            positios[y][x] = true;
            neighBourFreePositions.add(new Positio(x, y));
            foundPositios++;
        }


        if (neighBourFreePositions.isEmpty())
            return foundPositios;
        for (Positio p : neighBourFreePositions) {
            foundPositios = countReachablePositions(helperTable, positios, p, foundPositios);
        }
        return foundPositios;
    }

    private boolean checkThisPosition(boolean[][] positios, char[][] helperTable, int y, int x) {
        boolean  passed = true;
        if(y < 0 || y >= helperTable.length)
            passed = false;
        else if(x < 0 || x >= helperTable[0].length)
            passed = false;
        else if(helperTable[y][x] != '\0' && helperTable[y][x] != 'a' && helperTable[y][x] != 'ˇ'
                && helperTable[y][x] != '>' && helperTable[y][x] != '#')
            passed = false;
        else if(positios[y][x])
            passed = false;
        return passed;
    }






    private int findDefinitionLength(char[][] helperTable, int posX, int posY, ScandinawCrosswordWordDefinition.Direction direction) {
        int x = posX + direction.getOffSetPoints()[0];
        int y = posY + direction.getOffSetPoints()[1];
        int addTox = direction.isForward() ? 1 : 0;
        int addToY = direction.isForward() ? 0 : 1;
        List<Integer> lengths = new ArrayList<>();

        int size = 1;
        while(true) {
            x += addTox;
            y += addToY;

            if (x >= helperTable[0].length || y >= helperTable.length) {
                if(size == 1 && (direction == UPFORWARD || direction == DOWNFORWARD || direction == LEFTDOWN || direction == RIGHTDOWN))
                    break;
                for(int i= 0; i<size*2; i++)
                    lengths.add(size);
                break;
            }
            else if(y==0 || helperTable[y-1][x] == 'k' && (direction.isForward() && size == 3))
                break;
            else if (helperTable[y][x] == 'd' || helperTable[y][x] == 'k') {
                if(size == 1 && (direction == UPFORWARD || direction == DOWNFORWARD || direction == LEFTDOWN || direction == RIGHTDOWN))
                    break;
                for(int i= 0; i<size*2; i++)
                    lengths.add(size);
                break;
            }
            else if (helperTable[y][x] == '\0') {
                for (int i = 0; i < size * 2; i++)
                    lengths.add(size);
            }
            if(size>9)
                break;
            size++;
        }
        if(lengths.isEmpty())
            return 0;
        if(lengths.size()>1) {
            lengths.remove(0);
        }

        Collections.shuffle(lengths);

        return lengths.get((new Random()).nextInt(lengths.size()));
    }


    private char[][] copyHelperTable(char[][] helperTable) {
        char[][] resultTable = new char[helperTable.length][helperTable[0].length];
        for(int i=0; i<helperTable.length; i++) {
            for(int j = 0; j < helperTable[i].length; j++)
                resultTable[i][j] = helperTable[i][j];
        }
        return resultTable;
    }

    private char[][] updateHelperTable(char[][] helperTable, Positio currentPos, ScandinawCrosswordWordDefinition.Direction directio, int length) {
        char[][] resultTable = copyHelperTable(helperTable);

        resultTable[currentPos.y][currentPos.x] = 'd';
        int x = currentPos.x + directio.getOffSetPoints()[0];
        int y = currentPos.y + directio.getOffSetPoints()[1];
        int addTox = directio.isForward() ? 1 : 0;
        int addToY = directio.isForward() ? 0 : 1;
        for (int i = 0; i < length; i++) {
            if (resultTable[y][x] == 'ˇ' || resultTable[y][x] == '>')
                resultTable[y][x] = '#';
            else
                resultTable[y][x] = directio.isForward() ? '>' : 'ˇ';
            x += addTox;
            y += addToY;
        }



        return resultTable;
    }

    private Positio getNextDefinitionPositio(char[][] helperTable, Positio currentPos, ScandinawCrosswordWordDefinition.Direction directio, int length) {
        Positio nextPositio = null;
        int x = currentPos.x + directio.getOffSetPoints()[0];
        int y = currentPos.y + directio.getOffSetPoints()[1];
        int addTox = directio.isForward() ? 1 : 0;
        int addToY = directio.isForward() ? 0 : 1;
        for (int i = 0; i < length ; i++) {
            x += addTox;
            y += addToY;
        }
        if (x < helperTable[0].length && y < helperTable.length && helperTable[y][x] == '\0')
            nextPositio = new Positio(x, y);
        return nextPositio;
    }


    private int  helperTableIsFull(char[][] helperTable) {
        int remainingPositions = 0;
        for (int y = 0; y < helperTable.length; y++) {
            for (int x = 0; x < helperTable[0].length; x++) {
                if (helperTable[y][x] == '\0')
                    remainingPositions++;
            }
        }
        return remainingPositions;
    }
    private ScandinawCrosswordWordDefinitionCube[][] createUpdatedDefinitionTable(ScandinawCrosswordWordDefinitionCube[][] definitionTable, Positio currentPos, ScandinawCrosswordWordDefinition.Direction directio, int length) {

        ScandinawCrosswordWordDefinitionCube[][] newDefinitionTable =
                new ScandinawCrosswordWordDefinitionCube[definitionTable.length][definitionTable[0].length];
        for(int i=0; i<definitionTable.length; i++) {
            for (int j = 0; j < definitionTable[i].length; j++)
                newDefinitionTable[i][j] = definitionTable[i][j];
        }
        newDefinitionTable[currentPos.y][currentPos.x] = new ScandinawCrosswordWordDefinitionCube();
        ScandinawCrosswordWordDefinition definition = new ScandinawCrosswordWordDefinition(null, null,
                directio, length,0, new Positio(currentPos.x, currentPos.y));
        newDefinitionTable[currentPos.y][currentPos.x].addDefinition(definition);

        return newDefinitionTable;
    }

    private ScandinawCrosswordWordDefinitionCube[][] fillLastRemainingPositions(ScandinawCrosswordWordDefinitionCube[][] tempDefinitionTable, char[][] helperTable) throws Exception {
        boolean again;
        do {
            again = false;
            outer:
            for (int y = 0; y < helperTable.length; y++) {
                for (int x = 0; x < helperTable[0].length; x++) {
                    if (helperTable[y][x] == '\0') {
                        ScandinawCrosswordWordDefinition tempDefinition = findDefinitionInThisRow(helperTable, tempDefinitionTable, x, y);
                        if (tempDefinition == null)
                            throw new Exception("Definition setup failed");
                        helperTable = updateHelperTable(helperTable, tempDefinition.getPositio(),
                                tempDefinition.getDirection(), tempDefinition.getLength());
                        tempDefinitionTable[tempDefinition.getPositio().y][tempDefinition.getPositio().x]
                                .addDefinition(tempDefinition);
                        again = true;
                        break outer;
                    }
                }
            }
        }while(again);
        return fillLastRemainingSingleCells(tempDefinitionTable,helperTable);
        // return tempDefinitionTable;
    }

    private ScandinawCrosswordWordDefinitionCube[][] fillLastRemainingSingleCells(ScandinawCrosswordWordDefinitionCube[][] tempDefinitionTable, char[][] helperTable) throws Exception {
        boolean again;
        do {
            again = false;
            outer:
            for (int y = 0; y < helperTable.length; y++) {
                for (int x = 0; x < helperTable[0].length; x++) {
                    if (helperTable[y][x] == '>' || helperTable[y][x] == 'ˇ'  ) {
                        ScandinawCrosswordWordDefinition tempDefinition = findDefinitionInThisRow(helperTable, tempDefinitionTable, x, y);
                        if (tempDefinition == null)
                            continue;
                        helperTable = updateHelperTable(helperTable, tempDefinition.getPositio(),
                                tempDefinition.getDirection(), tempDefinition.getLength());
                        tempDefinitionTable[tempDefinition.getPositio().y][tempDefinition.getPositio().x]
                                .addDefinition(tempDefinition);
                        again = true;
                        break outer;
                    }
                }
            }
        }while(again);
        return tempDefinitionTable;
    }

    private ScandinawCrosswordWordDefinition findDefinitionInThisRow(char[][] helperTable, ScandinawCrosswordWordDefinitionCube[][] tempDefinitionTable, int posX, int y) {
        int x = posX;
        ScandinawCrosswordWordDefinition definition;

        int steps = 0;
        boolean out = false;
        while(x>=0 && !out) {
            if(tempDefinitionTable[y][x] != null) {
                int length = findLengthForDefinition(helperTable,x+1,y,FORWARD);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, FORWARD, length,0, new Positio(x,y));
                if(length>0 && tempDefinitionTable[y][x].insertable(definition))
                    return definition;
                else
                    out=true;

            }
            if(y>0 && tempDefinitionTable[y-1][x] != null && steps>0) {
                int length = findLengthForDefinition(helperTable,x,y,DOWNFORWARD);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, DOWNFORWARD, length,0, new Positio(x,y-1));
                if(length>1 && tempDefinitionTable[y-1][x].insertable(definition))
                    return definition;

            }
            if(y<tempDefinitionTable.length-1 && tempDefinitionTable[y+1][x] != null && steps>0) {
                int length = findLengthForDefinition(helperTable,x,y,UPFORWARD);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, UPFORWARD, length,0, new Positio(x,y+1));
                if(length>1 && tempDefinitionTable[y+1][x].insertable(definition))
                    return definition;
            }
            steps++;
            x--;
        }
        x=posX;
        steps=0;
        out=false;
        while(y>=0 && !out) {
            if(tempDefinitionTable[y][x] != null) {
                int length = findLengthForDefinition(helperTable,x,y+1,DOWN);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, DOWN, length,0, new Positio(x,y));
                if(length>0 && tempDefinitionTable[y][x].insertable(definition))
                    return definition;
                else
                    out=true;

            }

            if(x>0 && tempDefinitionTable[y][x-1] != null && steps>0) {
                int length = findLengthForDefinition(helperTable,x,y,RIGHTDOWN);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, RIGHTDOWN, length,0, new Positio(x-1,y));
                if(length>1 && tempDefinitionTable[y][x-1].insertable(definition))
                    return definition;
            }
            if(x<tempDefinitionTable[0].length-1 && tempDefinitionTable[y][x+1] != null && steps>0) {
                int length = findLengthForDefinition(helperTable,x,y,LEFTDOWN);
                definition = new ScandinawCrosswordWordDefinition(
                        null, null, LEFTDOWN, length,0, new Positio(x+1,y));
                if(length>1 && tempDefinitionTable[y][x+1].insertable(definition))
                    return definition;
            }
            steps++;
            y--;
        }
        return null;
    }

    private int findLengthForDefinition(char[][] helperTable, int x, int y, ScandinawCrosswordWordDefinition.Direction direction) {
        int addTox = direction.isForward() ? 1 : 0;
        int addToY = direction.isForward() ? 0 : 1;

        int length = 0;
        while(x<helperTable[0].length && y < helperTable.length) {
            if(direction.isForward() && (helperTable[y][x] == '>' || helperTable[y][x] == '#'  )) {
                length = 0;
                break;
            }
            if(!direction.isForward() && (helperTable[y][x] == 'ˇ' || helperTable[y][x] == '#'  )) {
                length = 0;
                break;
            }
            if(helperTable[y][x] =='k' || helperTable[y][x] =='d')
                break;
            length++;
            x+=addTox;
            y+=addToY;
        }
        return length;
    }

}
