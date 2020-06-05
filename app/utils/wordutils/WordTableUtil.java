package utils.wordutils;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import models.actorModels.wordgame.scandi.FoundWords;
import utils.Positio;

import java.util.*;


/**
 *
 * @author akosp
 */
public class WordTableUtil {


    public Map<Integer, List<FoundWords>> findWordsOfRow(String row , Positio positio, FoundWords.Direction direction) {
        Map<Integer, List<FoundWords>> foundWordsMap = new HashMap<>();
        String reverseRow = reverseString(row);
        Set<FoundWords> forwardWords = findWords(row,positio, direction, false);
        Set<FoundWords> reverseWords = findWords(reverseRow,positio, direction, true);

        Set<FoundWords> allWords = new HashSet<>(forwardWords);
        allWords.addAll(reverseWords);

        for(FoundWords f:allWords) {
            List<FoundWords> leftList = foundWordsMap.get(f.getLength());
            if(leftList == null)
                leftList = new ArrayList<>();
            leftList.add(f);
            foundWordsMap.put(f.getLength(), leftList);
        }



        return foundWordsMap;
    }

    private String reverseString(String str){
        StringBuilder sb=new StringBuilder(str);
        sb.reverse();
        return sb.toString();
    }

    private Set<FoundWords> findWords(String row, Positio positio, FoundWords.Direction direction, boolean reverse ) {
        Set<FoundWords> foundWords = new HashSet<>();

        for (int i = 0; i < row.length() ; i++)
            for (int j = i+1; j <= row.length(); j++)  {
                String currentWord = row.substring(i, j);

            }

        return foundWords;
    }

    private Positio findPositioOfFoundWord(Positio positio, int x, int y, int length, FoundWords.Direction direction, boolean reverse) {
        if(direction == FoundWords.Direction.FORWARD) {
            int posx = !reverse ? x : (length-x-1);
            int posy = positio.y-1;
            return new Positio(posx,posy);
        }
        else if(direction == FoundWords.Direction.DOWN) {
            int posx = positio.x-1;
            int posy = !reverse ? x : (length-x-1);
            return new Positio(posx,posy);
        }
        else if(direction == FoundWords.Direction.RIGHT) {
            int posx = !reverse ? (positio.x-1 + x) : (positio.x -1 + length-1-x);
            int posy = !reverse ? (positio.y-1 + x) : (positio.y-1 + length -1-x);
            return new Positio(posx,posy);
        }
        System.out.println("Posotio finder LEFT----->>>");
        int posx = !reverse ? (positio.x-1 - x) : (positio.x -1 - (length-1-x));
        int posy = !reverse ? (positio.y-1 + x) : (positio.y-1 + length -1-x);
        return new Positio(posx,posy);
    }
}
