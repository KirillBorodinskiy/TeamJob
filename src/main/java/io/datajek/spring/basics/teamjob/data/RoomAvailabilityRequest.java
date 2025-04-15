package io.datajek.spring.basics.teamjob.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAvailabilityRequest {
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
