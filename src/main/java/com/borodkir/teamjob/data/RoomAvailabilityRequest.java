package com.borodkir.teamjob.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAvailabilityRequest {
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
