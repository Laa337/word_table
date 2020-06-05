package models.actorModels.wordgame.wordtable;

import utils.Positio;

public class CandidateWords {
    private String word;
    private Positio positio;
    private boolean reverse;

    public CandidateWords(String word, Positio positio, boolean reverse) {
        this.word = word;
        this.positio = positio;
        this.reverse = reverse;
    }

    public String getWord() {
        return word;
    }

    public Positio getPositio() {
        return positio;
    }

    public boolean isReverse() {
        return reverse;
    }
}
