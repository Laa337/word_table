package utils;

import java.util.*;
import java.util.stream.Collectors;

public class MapUtil {
    public static Map<String, List<String>> createWordMap(List<String> szavak) {
        Map<String, List<String>> wordMap = new HashMap<>();
        System.out.println("CreateWordMap fut");
        for(String s:szavak) {
            s = s.toLowerCase();
            if(s.length()>7) {
                System.out.println("Bigger");
                continue;
            }
            System.out.println("Smaller");
            List<Character> letters = s.chars().mapToObj(c -> (char)c).collect(Collectors.toList());
            List<List<Character>> letterPatterns = createLetterPattern(letters);
            for(List<Character> outerList: letterPatterns) {
                String charToString = listToString(outerList);

                if(charToString.length() == s.length()) {
                    wordMap.computeIfAbsent(charToString, k -> new ArrayList<>()).add(s);
                }
            }
        }
        return wordMap;
    }
    public static String listToString(List<Character> letters) {
        String word = letters.stream().map(String::valueOf).collect(Collectors.joining());
        return word;
    }

    private static List<List<Character>> createLetterPattern(List<Character> letters) {
        if(letters.isEmpty()) {
            List<List<Character>> result = new ArrayList<>();
            result.add(Collections.emptyList());
            return result;
        }
        Character first = letters.get(0);
        List<Character> rest = letters.subList(1,letters.size());
        List<List<Character>> subans1 = createLetterPattern(rest);
        List<List<Character>> subans2 = insertToRest(first,subans1);
        return concat(subans1,subans2);
    }

    private static List<List<Character>> concat(List<List<Character>> subans1, List<List<Character>> subans2) {
        subans1.addAll(subans2);
        return subans1;
    }

    private static List<List<Character>> insertToRest(Character first, List<List<Character>> subans1) {
        List<List<Character>> resultList = new ArrayList<>();
        for(List<Character> list: subans1) {
            List<Character> copyList = new ArrayList<>();
            copyList.add(first);
            copyList.addAll(list);
            resultList.add(copyList);
        }
        for(List<Character> list: subans1) {
            List<Character> copyList = new ArrayList<>();
            copyList.add('#');
            copyList.addAll(list);
            resultList.add(copyList);
        }

        return resultList;
    }
}
