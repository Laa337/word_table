package actors.wordsearchgame.creators;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import actors.utils.WordTable;
import actors.wordsearchgame.controllers.WordTableCreatorController;
import models.actorModels.wordgame.scandi.FoundWords;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import utils.Positio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author akosp
 */
public class WordTableCreator extends AbstractBehavior<WordTableCreator.Command> {

    public interface Command {
    };

    public static class CreateTable implements Command {

        private final List<String> wordsToInsert;
        private final int sizeX;
        private final int sizeY;
        private final boolean unique;
        private WordTableCreatorController.ErrorClass error;
        private final ActorRef<WordTable.CreateWordTable> replayTo;

        public CreateTable(List<String> wordsToInsert, int sizeX, int sizeY, boolean unique,
                           WordTableCreatorController.ErrorClass error, ActorRef<WordTable.CreateWordTable> replayTo) {
            this.wordsToInsert = wordsToInsert;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.unique = unique;
            this.error = error;
            this.replayTo = replayTo;
        }

        public List<String> getWordsToInsert() {
            return wordsToInsert;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public boolean isUnique() {
            return unique;
        }

        public ActorRef<WordTable.CreateWordTable> getReplayTo() {
            return replayTo;
        }
    }

    private static class FinalTable {

        public String[] table;
        public Map<Integer, List<FoundWords>> foundWords;

        public FinalTable(String[] table, Map<Integer, List<FoundWords>> foundWords) {
            this.table = table;
            this.foundWords = foundWords;
        }
    }

    private WordTableCreatorController.ErrorClass errorMessage;

    public static Behavior<Command> create() {
        return Behaviors.setup(WordTableCreator::new);
    }

    private WordTableCreator(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateTable.class,
                        msg -> {
                            System.out.println("----actors.wordsearchgame.utils.TableCreator createReceive");
                            this.errorMessage = msg.error;
                            msg.getWordsToInsert().forEach(System.out::println);
                            FinalTable ft = null;
                            for (int i = 0; i < 30000; i++) {
                                ft = createTable(msg.getWordsToInsert(), msg.getSizeX(),msg.getSizeY(), msg.isUnique());
                              //  System.out.println("\n---------------------------\n**************\nTable Creation Probe\n" + ft.toString() + "\n" + ft.table.toString() + "\nI: " + i + "\n\n\n");
                                if (ft != null) {
                                    msg.getReplayTo().tell(new WordTable.CreateWordTableClass(ft.table, ft.foundWords));
                                    break;

                                } else {
                                   // System.out.println("\nNemSikerült a táblát megcsinálni");
                                }
                            }
                            if(ft == null)
                                throw new Exception("Sajna nem sikerült a táblát megcsinálni");
                            return Behaviors.same();
                        }
                )
                .build();

    }

    private FinalTable createTable(List<String> wordsToInsert, int sizeX, int sizeY, boolean unique) throws Exception {
        String[] starterTable = createStarterTable(sizeX, sizeY, unique);



        List<int[]> startPoints = createStarterPoints(starterTable,sizeX,sizeY);


        FinalTable finalTable = createInsertedWordTable(wordsToInsert, starterTable, startPoints);
        return finalTable;
    }

    private String[] createStarterTable(int sizeX, int sizeY, boolean unique) {
        String[] starterTable = new String[sizeY];
        for (int i = 0; i < sizeY; i++) {
            starterTable[i] = new String(new char[sizeX]).replace('\0', '#');
        }

        return starterTable;
    }

    private List<int[]> createStarterPoints(String[] starterTable,int sizeX, int sizeY) {
        List<int[]> starterPoints = new ArrayList<>();
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                     starterPoints.add(new int[]{i, j});
            }
        }
        Collections.shuffle(starterPoints, new Random());


        return starterPoints;
    }

    private FinalTable createInsertedWordTable(List<String> wordsToInsert, String[] starterTable, List<int[]> startPoints) throws Exception {
        FoundWords foundWord;

        List<FoundWords> foundWords;

            foundWords = insertSucceded(wordsToInsert.get(0), starterTable, startPoints);
            if (!foundWords.isEmpty()) {
                if (wordsToInsert.size() == 1) {
                    foundWord = foundWords.get(0);
                    String[] tempTable = updateTable(starterTable, foundWord);
                    Map<Integer, List<FoundWords>> tempFoundWordsMap = new HashMap<>();
                    List<FoundWords> tempFoundWordList = new ArrayList<>();
                    tempFoundWordList.add(foundWord);
                    tempFoundWordsMap.put(foundWord.getLength(), tempFoundWordList);
                    return new FinalTable(tempTable, tempFoundWordsMap);
                } else {
                    for (FoundWords currentFoundWord : foundWords) {
                        String[] tempTable = updateTable(starterTable, currentFoundWord);
                        List<String> tempWordsToInsert = new ArrayList<>();
                        tempWordsToInsert.addAll(wordsToInsert);
                        tempWordsToInsert.remove(currentFoundWord.getWord());
                        FinalTable tempFinalTable = createInsertedWordTable(tempWordsToInsert, tempTable, startPoints);
                        if (tempFinalTable != null) {
                            List<FoundWords> tempFoundWord = tempFinalTable.foundWords.get(currentFoundWord.getLength());
                            if (tempFoundWord == null) {
                                tempFoundWord = new ArrayList<>();
                            }
                            tempFoundWord.add(currentFoundWord);
                            tempFinalTable.foundWords.put(currentFoundWord.getLength(), tempFoundWord);

                            return tempFinalTable;
                        }

                    }

                    return null;
                }
            }

        return null;
    }

    private List<FoundWords> insertSucceded(String word, String[] starterTable, List<int[]> startPoints) throws Exception {
        List<FoundWords.Direction> directions = Arrays.asList(FoundWords.Direction.values());
        Collections.shuffle(directions);
        boolean reverse = new Random().nextInt(10) > 6;
        FoundWords foundWord;
        List<FoundWords> foundWords = new ArrayList<>();
        boolean deep =  ( Math.max(starterTable[0].length(), starterTable.length) > 5) ? true : false;

        int counter = 0;
        for (int[] p : startPoints) {
            for (FoundWords.Direction d : directions) {
                if(this.errorMessage.message.equals("stop"))
                    throw new Exception("Creator timed out");
                foundWord = insertSuccededForCurrent(word, starterTable, p, d, reverse);
                if (foundWord != null) {
                    foundWords.add(foundWord);
                    counter++;
                }
                foundWord = insertSuccededForCurrent(word, starterTable, p, d, !reverse);
                if (foundWord != null) {
                    foundWords.add(foundWord);
                    counter++;
                }

                if(deep && counter>10)
                    return foundWords;
            }
        }
        return foundWords;
    }

    private FoundWords insertSuccededForCurrent(String word, String[] starterTable, int[] p, FoundWords.Direction direction, boolean reverse) {
        FoundWords foundWord = null;
        int size = word.length();
        int tableSizeX = starterTable[0].length();
        int tableSizeY = starterTable.length;
        boolean found = true;
        int[] nextPosition = {p[0], p[1]};

        for (int i = 0; i < size && found; i++) {
            if (i > 0) {
                nextPosition = getNextPosition(nextPosition, tableSizeX, tableSizeY, direction, reverse);
            }
            if (nextPosition == null) {
                found = false;
            } else {
                String letterInTable = starterTable[nextPosition[1]].substring(nextPosition[0], nextPosition[0] + 1);
                String letterInWord = String.valueOf(word.charAt(i));
                if (!letterInTable.equals("#")) {
                    if (!letterInTable.equals(letterInWord)) {
                        found = false;
                    }
                }
            }
        }
        if (found) {
            foundWord = new FoundWords(word, new Positio(p[0], p[1]), size, direction, reverse);
        }
        return foundWord;
    }

    private int[] getNextPosition(int[] position, int tableSizeX, int tableSizeY, FoundWords.Direction direction, boolean reverse) {
        int[] nextPosition = new int[2];
        int addToX = 1;
        int addToY = 1;

        if (direction == FoundWords.Direction.FORWARD) {
            if (reverse) {
                addToX = -1;
            }
            addToY = 0;

        } else if (direction == FoundWords.Direction.DOWN) {
            if (reverse) {
                addToY = -1;
            }
            addToX = 0;

        } else if (direction == FoundWords.Direction.RIGHT) {
            if (reverse) {
                addToX = -1;
                addToY = -1;
            }

        } else {
            if (reverse) {
                addToY = -1;
            } else {
                addToX = -1;
            }
        }

        nextPosition[0] = position[0] + addToX;
        nextPosition[1] = position[1] + addToY;

        if (nextPosition[0] < 0 || nextPosition[1] < 0 || nextPosition[0] == tableSizeX || nextPosition[1] == tableSizeY) {
            return null;
        }
        return nextPosition;
    }

    private String[] updateTable(String[] starterTable, FoundWords foundWord) {
       // System.out.println("UpdateTable runs");
        int posX = foundWord.getPos().x;
        int posY = foundWord.getPos().y;
        int sizeX = starterTable[0].length();
        int sizeY = starterTable.length;

        String[] tempTable = new String[sizeY];
        for (int i = 0; i < sizeY; i++) {
            tempTable[i] = String.copyValueOf(starterTable[i].toCharArray());
        }
        for (int i = 0; i < foundWord.getLength(); i++) {
            if (i > 0) {
                int[] temp = getNextPosition(new int[]{posX, posY}, sizeX, sizeY, foundWord.getDirection(), foundWord.isReverse());
                posX = temp[0];
                posY = temp[1];
            }
            String currentLetter = "";

            currentLetter = String.valueOf(foundWord.getWord().charAt(i));
            tempTable[posY] = tempTable[posY].substring(0, posX)
                    + currentLetter + tempTable[posY].substring(posX + 1);
        }

        return tempTable;
    }
}
