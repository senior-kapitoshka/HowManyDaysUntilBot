package com.project.HowManyDaysUntilBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name="holidayTable")
@Getter
@Setter
public final class Holiday {

    // id - private key in sqltable
    @Id
    private Integer dayMonth;
    private Integer  day;
    private Integer  month;
    private String holidayName;
    private boolean isState;



    @Override
    public String toString() {
        return "Holiday{" +
                "day=" + day +
                ", month=" + month +
                ", holidayName='" + holidayName + '\'' +
                ", isState=" + isState +
                '}';
    }
}
