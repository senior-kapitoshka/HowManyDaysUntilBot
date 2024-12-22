package com.project.HowManyDaysUntilBot.utils;

import com.project.HowManyDaysUntilBot.model.Holiday;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
public class FileManager {

    private static final Properties properties = new Properties();
    private Scanner scanner;
    private Pattern p=Pattern.compile(rgx);
    private static String rgx="([\\d]+)\\.([\\d]+) ([\\а-яёА-ЯЁ /()«»]+)";


    public FileManager(){
        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")) {
            properties.load(fis);  // Загружает все свойства из файла
        } catch (IOException e) {
            log.error("Не удалось загрузить файл свойств "  + e);
            throw new RuntimeException("Не удалось загрузить файл свойств", e);  // Обработка ошибок
        }
    }

    public String JSONWriter(JSONArray jsonArray){
        try{
            FileWriter file = new FileWriter( properties.getProperty("json.file.path"),false);
            jsonArray.writeJSONString(file);
            file.close();
        }
        catch(IOException ioe  ){

            log.error("JSONWriter Error: " + ioe);

        }
        return properties.getProperty("json.file.path");
    }

    //для добавления в базу из текстового файла
    public List<Holiday> addToBase(){
        List<Holiday> list=new ArrayList<>();
        try{
        scanner = new Scanner(new File(properties.getProperty("dates.file.path")));
        while (scanner.hasNextLine()){
            String s = scanner.nextLine();
            Matcher m=p.matcher(s);
            if(s.matches(rgx)){
                while(m.find()){
                    Integer dayMonth=Integer.parseInt(m.group(1)+m.group(2));
                    System.out.println(dayMonth);

                        Integer day=Integer.parseInt(m.group(1));
                        Integer month=Integer.parseInt(m.group(2));
                        String holidayName=m.group(3);
                        boolean isState=false;
                        Holiday holiday=new Holiday();
                        holiday.setHolidayName(holidayName);
                        holiday.setDay(day);
                        holiday.setDayMonth(dayMonth);
                        holiday.setMonth(month);
                        holiday.setState(isState);
                        list.add(holiday);
                }
            }
        }
        }catch(FileNotFoundException e){
            log.error("Incorrect filename");
        }catch(IOException e){
            log.error("Scanner Error");
        }
        return list;
    }



    public String choosingPhotoToScan(Object toScan){
        String path=null;
        if(toScan instanceof Integer){
            String prop=String.format("%d.file.path",(Integer)toScan);
            path=properties.getProperty(prop);
            if(path==null){
                path=properties.getProperty("no_foto.file.path");
            }
        }else if (toScan instanceof String){
            switch((String)toScan){
                case "vd1"->path=properties.getProperty("vd1.file.path");
                case "vd2"->path=properties.getProperty("vd2.file.path");
                case "russia"->path=properties.getProperty("rus.file.path");
                case "ng_mchitsya"->path=properties.getProperty("ngm.file.path");
                case "friday"->path=properties.getProperty("fri.file.path");
                case "saturday"->path=properties.getProperty("sat.file.path");
                case "sunday"->path=properties.getProperty("sun.file.path");
                default->path=properties.getProperty("no_foto.file.path");
            }
        }

        return path;
    }

}


