package com.borodkir.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

/**
 * Represents the availability of a room with its unoccupied time slots.
 */
@Data
@AllArgsConstructor
public class RoomAvailability {
    private Room room;
    private ArrayList<TimeFromTo> unoccupiedTimesFromTo;
}