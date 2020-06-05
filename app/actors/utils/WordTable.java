package actors.utils;

import actors.wordsearchgame.creators.ScandiCrossWordWordFiller;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import models.actorModels.wordgame.scandi.FoundWords;
import utils.Positio;
import models.actorModels.wordgame.wordtable.CandidateWords;


import java.util.List;
import java.util.Map;
import java.util.Set;

public class WordTable extends AbstractBehavior<WordTable.Decompose> {

//------------- F  I  N  D  W  O  R  D  S  -------------------------------------------

    public interface FindWords {}

    public static class FindWordsClass implements FindWords {

        private Map<Integer, List<FoundWords>> foundWords;
        private String[] table;

        public FindWordsClass(Map<Integer, List<FoundWords>> foundWords, String[] table) {
            this.foundWords = foundWords;
            this.table = table;
        }

        public Map<Integer, List<FoundWords>> getFoundWords() {
            return foundWords;
        }

        public String[] getTable() {
            return table;
        }

    }

    public static class FindWordsOfRowClass implements FindWords {

        private Set<CandidateWords> words;

        public FindWordsOfRowClass(Set<CandidateWords> words) {
            this.words = words;
        }

        public Set<CandidateWords> getWords() {
            return words;
        }
    }

    public static class FindWordsOfTopicClass implements FindWords {
        private Set<String> words;

        public FindWordsOfTopicClass(Set<String> words) {
            this.words = words;
        }

        public Set<String> getWords() {
            return words;
        }
    }

    public static class FillCrossWords implements FindWords {
        List<String> foundWords;

        public FillCrossWords(List<String> foundWords) {
            this.foundWords = foundWords;
        }

        public List<String> getFoundWords() {
            return foundWords;
        }
    }

    public static class ScandiCrossWordResponce implements  FindWords {
        ScandiCrossWordWordFiller.DefinitionMatrix matrix;

        public ScandiCrossWordResponce(ScandiCrossWordWordFiller.DefinitionMatrix matrix) {
            this.matrix = matrix;
        }

        public ScandiCrossWordWordFiller.DefinitionMatrix getMatrix() {
            return matrix;
        }
    }

    public static class ErrorFindWords implements FindWords {
        public String message;

        public ErrorFindWords(String message) {
            this.message = message;
        }
    }




    public interface Decompose {}

    public static class DecomposeClass implements Decompose {
        private Map<FoundWords.Direction, String[]> decomposedTableRows;
        private Map<FoundWords.Direction, Positio[]> decomposedTablePositions;
        int rowsLength;

        public DecomposeClass(Map<FoundWords.Direction, String[]> decomposedTableRows, Map<FoundWords.Direction, Positio[]> decomposedTablePositions, int rowsLength) {
            this.decomposedTableRows = decomposedTableRows;
            this.decomposedTablePositions = decomposedTablePositions;
            this.rowsLength = rowsLength;
        }

        public Map<FoundWords.Direction, String[]> getDecomposedTableRows() {
            return decomposedTableRows;
        }

        public Map<FoundWords.Direction, Positio[]> getDecomposedTablePositions() {
            return decomposedTablePositions;
        }

        public int getRowsLength() {
            return rowsLength;
        }
    }

    public static class ErrorDecompose implements Decompose {
        public String message;

        public ErrorDecompose(String message) {
            this.message = message;
        }
    }

//---------------- C R E A T E   W O R D   T A B L E -------------------------------

    public interface CreateWordTable {}

    public static class CreateWordTableClass implements CreateWordTable {
        String[] createdTable;
        private Map<Integer, List<FoundWords>> foundWords;


        public CreateWordTableClass(String[] createdTable, Map<Integer, List<FoundWords>> foundWords) {
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

    public static class ErrorCreateWordTable implements CreateWordTable {
        public String message;

        public ErrorCreateWordTable(String message) {
            this.message = message;
        }
    }


    private WordTable(ActorContext<Decompose> context) {
        super(context);
    }

    @Override
    public Receive<Decompose> createReceive() {
        return null;
    }
}
