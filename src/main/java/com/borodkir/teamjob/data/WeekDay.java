package com.borodkir.teamjob.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;


@Data
@ToString
@AllArgsConstructor
public class WeekDay {

    private final LocalDate date;
    private final String dayName;
    private final boolean isToday;

    private List<EventInADay> events;
    private final int amountOfEvents;

}