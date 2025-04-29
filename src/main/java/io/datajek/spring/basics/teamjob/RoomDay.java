package io.datajek.spring.basics.teamjob;

import io.datajek.spring.basics.teamjob.data.EventInADay;
import io.datajek.spring.basics.teamjob.data.Room;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoomDay {
    private final Room room;
    private List<EventInADay> events;
}