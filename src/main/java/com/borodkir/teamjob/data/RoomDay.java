package com.borodkir.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoomDay {
    private final Room room;
    private List<EventInADay> events;
}