package io.datajek.spring.basics.teamjob;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

/**
 * Represents a search result for available rooms, users, or events.
 */
@Data
@AllArgsConstructor
public class SearchResult {
    private String type; // "room", "user", or "event"
    private Long id;
    private String name;
    private Set<String> tags;
    private LocalDate date;

}