package com.borodkir.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventInADay {
    private Long id;
    private String title;
    private String description;
    private Room room;
    private User user;
    private boolean isRecurring;
    private LocalDateTime isRecurringEndDate;
    private double durationInADay;
    private double startTimeToUse;
    private double endTimeToUse;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
