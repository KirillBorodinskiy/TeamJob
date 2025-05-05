package io.datajek.spring.basics.teamjob.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

/**
 * Represents the availability of a user with their unoccupied time slots.
 */
@Data
@AllArgsConstructor
public class UserAvailability {
    private User user;
    private ArrayList<TimeFromTo> unoccupiedTimesFromTo;
}