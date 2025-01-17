package io.datajek.spring.basics.teamjob;


import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;


@Data
@ToString
public class WeekDay {

    private final LocalDate date;
    private final String dayName;
    private final boolean isToday;

    private String[] events;

    public WeekDay(LocalDate currentDate, String displayName, boolean equals) {
        this.date = currentDate;
        this.dayName = displayName;
        this.isToday = equals;
    }
}