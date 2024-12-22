package com.project.HowManyDaysUntilBot.utils;

import com.project.HowManyDaysUntilBot.model.Holiday;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Utils {
    Date todaysDate;

    public Utils(Date date){
        todaysDate = date;
    }
    //убрать в утилиты
    public Integer dateToPrimaryKey(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(todaysDate);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int month=date.getMonth()+1;
        return Integer.parseInt(dayOfMonth+""+month);
    }
    //убрать в утилиты
    public Date holidayToDate(Holiday holiday){
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(todaysDate);
        int dayOfMonth = cal1.get(Calendar.DAY_OF_MONTH);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(new Date(todaysDate.getYear(),holiday.getMonth(),holiday.getDay()));
        int dayOfMonthHoliday = cal2.get(Calendar.DAY_OF_MONTH);

        if(dayOfMonth==dayOfMonthHoliday
                && holiday.getMonth()==todaysDate.getMonth()+1){

            return todaysDate;
        }
        int year = (todaysDate.getMonth() < holiday.getMonth()-1 ||
                (todaysDate.getMonth() == holiday.getMonth()-1 && dayOfMonthHoliday>dayOfMonth)) ?
                todaysDate.getYear(): todaysDate.getYear()+1;
        return new Date(year,holiday.getMonth()-1,holiday.getDay());
    }
    //убрать в утилиты
    public String getSufx(Long diff){
        return (diff % 10 == 1 && diff!=11)?" день":
                ( diff % 10 >1 && diff % 10 <5 && (diff<10 || diff>19))?" дня":
                        " дней";
    }
    //убрать в утилиты
    public Pair<String,Long> calculateDiff(Date foundedDate){
        String result=null;
        long diffInMillies = Math.abs(foundedDate.getTime() - todaysDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        result = (diff==0 && foundedDate!=todaysDate)?
                "Праздник уже завтра!":
                "До праздника " + diff + getSufx(diff);

        return new Pair(result,diff);
    }
    //убрать в утилиты
    public String holidaysToString(List<Holiday> holidays){
        StringBuilder sb=new StringBuilder();
        holidays.sort(new Comparator<Holiday>() {
            @Override
            public int compare(Holiday o1, Holiday o2) {
                return o1.getMonth()!=o2.getDayMonth()?
                        o1.getMonth()-o2.getMonth():
                        o1.getDay()-o2.getDay();
            }
        });
        for(var holiday: holidays){
            sb.append(String.format("%s.%s - %s\n",
                    zeroingFormat(holiday.getDay()),
                    zeroingFormat(holiday.getMonth()),holiday.getHolidayName()));
        }
        return sb.toString();
    }
    private String zeroingFormat(Integer i){
        return i>=10?i.toString():new String("0"+i.toString());
    }

    public Integer getDayMonth(Integer day, Integer month){
         String str=day.toString()+ month.toString();
         return Integer.parseInt(str);
    }
}
