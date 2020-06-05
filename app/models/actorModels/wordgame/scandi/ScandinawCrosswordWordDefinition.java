package models.actorModels.wordgame.scandi;

import utils.Positio;

public class ScandinawCrosswordWordDefinition {
    enum DefinitionMode {
        STRING, COMPLETESENTENCE, CHOOSERIGHTANSWER;
    }

   public enum Direction {
        UPFORWARD(  new int[]{0, -1, 1}),
        FORWARD(    new int[]{1, 0, 2}),
        DOWNFORWARD(new int[]{0, 1, 3}),
        LEFTDOWN(   new int[]{-1, 0,4}),
        DOWN(       new int[]{0, 1, 5}),
        RIGHTDOWN(  new int[]{1, 0, 6});

        private int[] offSetPoints;
        Direction(int[] starterPositio) {
            this.offSetPoints = starterPositio;
        }

        public int[] getOffSetPoints() {
            return offSetPoints;
        }
        public boolean isForward() {
            return offSetPoints[2] <4;
        }

        public String getCharCode() {
            if((offSetPoints[2] == 1))
                return "UF";
            else if((offSetPoints[2] == 2) )
                return "F";
            else if((offSetPoints[2] == 3))
                return "DF";
            else if((offSetPoints[2] == 4))
                return "LD";
            else if((offSetPoints[2] == 5))
                return "D";
            return "RD";
        }
    }
    private String word;
    private  DefinitionMode definitionMode;
    private  Direction direction;
    private int length;
    private int score;
    private Positio positio;

    public ScandinawCrosswordWordDefinition(String word, DefinitionMode definitionMode, Direction direction, int length, int score, Positio positio) {
        this.word = word;
        this.definitionMode = definitionMode;
        this.direction = direction;
        this.length = length;
        this.score = score;
        this.positio = positio;
    }

    public ScandinawCrosswordWordDefinition(String word, DefinitionMode definitionMode, Direction direction, Positio positio) {
        this.word = word;
        this.definitionMode = definitionMode;
        this.direction = direction;
        this.positio = positio;
    }

    public String getWord() {
        return word;
    }

    public DefinitionMode getDefinitionMode() {
        return definitionMode;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getScore() {
        return score;
    }

    public int getLength() {
        return length;
    }

    public Positio getPositio() {
        return positio;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setDefinitionMode(DefinitionMode definitionMode) {
        this.definitionMode = definitionMode;
    }
}
