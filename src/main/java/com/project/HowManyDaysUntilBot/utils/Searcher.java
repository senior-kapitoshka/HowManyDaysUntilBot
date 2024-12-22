package com.project.HowManyDaysUntilBot.utils;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Searcher {
    private final List<String> months = List.of("январь","февраль",
            "март","апрель","май","июнь","июль",
            "август","сентябрь","октябрь","ноябрь","декабрь");
    private  Map<String,Integer> dictionary= new HashMap<>();
    private List<String> words=new ArrayList<>();

    public void setDictionary(List<String> listOfHolidays){
        if(dictionary.isEmpty() && words.isEmpty()){
            Pattern p=Pattern.compile("([\\А-Яа-яё]+)");
            for(String holiday:listOfHolidays) {
                Matcher m = p.matcher(holiday);
                while (m.find()) {
                    int start = m.start();
                    int end = m.end();
                    if (holiday.substring(start, end).length() >= 2) {
                        String word =  holiday.substring(start, end).toLowerCase().trim();
                        dictionary.put(word,dictionary.getOrDefault(word,0)+1);
                        words.add(word);
                    }
                }
            }
        }
    }

    public String peekTag(String holidayTxt){
        String result=null;
        PriorityQueue<Pair<String,Integer>>pq = new PriorityQueue<>(new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return o1.second- o2.second;
            }
        });

        Pattern p=Pattern.compile("([\\а-яё]+)");
        Matcher m=p.matcher(holidayTxt);
        while(m.find()){
            int start=m.start();
            int end=m.end();

            String sim= findMostSimilarWord(holidayTxt.substring(start,end));
            Pair<String,Integer>pr=new Pair<>(sim,dictionary.get(sim));
            pq.add(pr);
        }
        if(pq.isEmpty()) return null;
        return pq.peek().first;
    }

    public String findMostSimilarWord(String txt) {
        return words.stream()
                .min((a, b) -> getSimilarPoint(a, txt) - getSimilarPoint(b, txt))
                .get();
    }

    private int getSimilarPoint(String a, String b) {
        return Math.max(a.length(), b.length()) - ((int) sameLettersLength(a, b));
    }

    private long sameLettersLength(String a, String b) {
        if (a.length() > b.length()) {
            return sameLettersLength(b, a);
        }

        return IntStream.rangeClosed(0, b.length() - a.length())
                .mapToLong(i -> IntStream.range(0, a.length()).filter(j -> a.charAt(j) == b.charAt(i + j)).count())
                .max()
                .orElse(0);
    }

    public int haveMonth(String month){
        return months.indexOf(month);
    }

    public String findMostSimilarMonth(String month) {
        return months.stream()
                .min((a, b) -> getSimilarPoint(a, month) - getSimilarPoint(b, month))
                .get();
    }

}







