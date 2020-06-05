package models.actorModels.wordgame.scandi;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import utils.Positio;

/**
 *
 * @author akosp
 */
public class FoundWords {
    private String word;
    private int length;
    private Positio pos;
    private Direction direction;
    public boolean reverse;

    public FoundWords(String word, Positio pos, int length, Direction direction, boolean reverse) {
        this.word = word;
        this.length = length;
        this.pos = pos;
        this.direction = direction;
        this.reverse = reverse;
    }


    public static enum Direction {
        FORWARD, DOWN, LEFT, RIGHT
    }

    public FoundWords() {
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Positio getPos() {
        return pos;
    }

    public void setPos(Positio pos) {
        this.pos = pos;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }











    @Override
    public String toString() {
        return "\n\n ---> " + word + "  <-----\nLength: " +
                length + "D\nDirection: " + direction + "\nPosition: " + pos.x + ":" + pos.y
                +"\nReverse: " + reverse + " ..."; //To change body of generated methods, choose Tools | Templates.
    }


}
