package io.datajek.spring.basics.teamjob.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
public class AvailableTimeRequest {
    private LocalDateTime startTime; // Start time of the request
    private LocalDateTime endTime;// End time of the request
    private String type;// "room", "user", "event"
    private List<RoomAvailability> roomAvailabilities; // List of room availabilities with their unoccupied time slots
    private List<UserAvailability> userAvailabilities; // List of user availabilities with their unoccupied time slots
    private List<EventAvailability> eventAvailabilities; // List of event availabilities with their unoccupied time slots

    public AvailableTimeRequest(LocalDateTime startTime, LocalDateTime endTime, String type) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.roomAvailabilities = new ArrayList<>();
        this.userAvailabilities = new ArrayList<>();
        this.eventAvailabilities = new ArrayList<>();
    }
}
