package com.borodkir.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TimeFromTo {
    LocalDateTime startTime;
    LocalDateTime endTime;

    public int getDurationInMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }
}


