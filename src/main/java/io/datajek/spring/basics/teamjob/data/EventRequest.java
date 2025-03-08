package io.datajek.spring.basics.teamjob.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long userId;
    private Long roomId;
    private String description;
}
