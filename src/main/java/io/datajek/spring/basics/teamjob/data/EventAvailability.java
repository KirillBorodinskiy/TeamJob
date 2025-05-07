package io.datajek.spring.basics.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

/**
 * Represents the availability of an event with its unoccupied time slots.
 */
@Data
@AllArgsConstructor
public class EventAvailability {
    private Event event;
    private ArrayList<TimeFromTo> unoccupiedTimesFromTo;
}