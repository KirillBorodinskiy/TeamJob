package com.borodkir.teamjob.services;

import com.borodkir.teamjob.data.*;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public interface ICalendarService {
    AvailableTimeRequest generateAvailableTimeRequest(String type, LocalDateTime startTime, LocalDateTime endTime, int durationInMinutes, Set<String> tags);

    void gatherAllTags(Model model);

    List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String userIds, String roomIds);

    List<SearchResult> generateSearchResults(AvailableTimeRequest request, LocalDate date);

    void setupModelForWeekCalendar(Model model, LocalDate date, String userIds, String roomIds);

    void setupModelForDayCalendar(Model model, LocalDate date, String userIds, String roomIds);

    void setupModelForFindAvailable(Model model, String searchType, String tags, LocalDate date, LocalTime startTime, LocalTime endTime, Integer durationMinutes);
}