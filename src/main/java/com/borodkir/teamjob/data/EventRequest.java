package com.borodkir.teamjob.data;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequest {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long userId;
    private Long roomId;
    private String tags;
    private boolean isRecurring;
    private String rrule;  // Recurrence Rule according to RFC 5545
    private LocalDateTime recurrenceEndDate;
    private String exdate;  // Exception dates for recurring events according to RFC 5545
    private String rdate;   // Additional dates for recurring events according to RFC 5545
    private String interval; // Interval for recurrence
    private List<String> weekdays; // Weekdays for weekly recurrence
    private String description;
}
