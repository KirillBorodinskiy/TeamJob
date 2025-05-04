package io.datajek.spring.basics.teamjob.data;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeFromTo {
    LocalDateTime startTime;
    LocalDateTime endTime;

    public int getDurationInMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }
}


