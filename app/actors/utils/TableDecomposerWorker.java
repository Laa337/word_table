package actors.utils;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.wordsearchgame.controllers.WordFinderController;
import models.actorModels.wordgame.scandi.FoundWords;
import utils.Positio;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import utils.decomposers.tabledecomposers.CubeDecomposer;
import utils.decomposers.tabledecomposers.RectangleDecomposer;
import utils.decomposers.tabledecomposers.TableDecomposer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author akosp
 */
public class TableDecomposerWorker extends AbstractBehavior<TableDecomposerWorker.Command> {
    public interface Command {}

    public static class Decompose implements Command {
        private String[] table;
        private ActorRef<WordTable.Decompose> replayTo;

        public Decompose(String[] table, ActorRef<WordTable.Decompose> replayTo) {
            this.table = table;
            this.replayTo = replayTo;
        }

        public String[] getTable() {
            return table;
        }

        public ActorRef<WordTable.Decompose> getReplayTo() {
            return replayTo;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(TableDecomposerWorker::new);
    }


    public TableDecomposerWorker(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Decompose.class,
                        msg -> {
                            int rowsLength = msg.getTable()[0].length();
                            int coloumnsLength = msg.getTable().length;
                            TableDecomposer decomposer;
                            if(rowsLength==coloumnsLength)
                                decomposer = new CubeDecomposer();
                            else
                                decomposer = new RectangleDecomposer();
                            Map<FoundWords.Direction, String[]> decomposedTableRows = new HashMap<>();
                            Map<FoundWords.Direction, Positio[]> decomposedTablePositions = new HashMap<>();


                            String[] tableRows = msg.getTable();
                            String[] tableColoumns  = decomposer.getColoumns(tableRows);
                            String[] tableRightDiagonals = decomposer.getRightDiagonals(tableRows, tableColoumns);
                            String[] tableLeftDiagonals = decomposer.getLeftDiagonals(tableRows, tableColoumns);

                            int rowsCount = tableLeftDiagonals.length +
                                    tableRightDiagonals.length +
                                    tableColoumns.length +
                                    tableRows.length;

                            decomposedTableRows.put(FoundWords.Direction.FORWARD, tableRows);
                            decomposedTableRows.put(FoundWords.Direction.DOWN, tableColoumns );
                            decomposedTableRows.put(FoundWords.Direction.RIGHT, tableRightDiagonals );
                            decomposedTableRows.put(FoundWords.Direction.LEFT, tableLeftDiagonals );

                            decomposedTablePositions.put(FoundWords.Direction.FORWARD, decomposer.getRowPositios(coloumnsLength));
                            decomposedTablePositions.put(FoundWords.Direction.DOWN, decomposer.getColoumnPositios(rowsLength));
                            decomposedTablePositions.put(FoundWords.Direction.RIGHT, decomposer.getRightDiagPositios(rowsLength,coloumnsLength));
                            decomposedTablePositions.put(FoundWords.Direction.LEFT, decomposer.getLeftDiagPositios(rowsLength,coloumnsLength));
                            WordTable.Decompose response = new WordTable.DecomposeClass(decomposedTableRows, decomposedTablePositions, rowsCount);

                            msg.getReplayTo().tell(response);

                            return Behaviors.same();
                        }
                )
                .build();
    }



}
