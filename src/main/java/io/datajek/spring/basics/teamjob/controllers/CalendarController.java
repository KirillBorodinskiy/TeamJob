package io.datajek.spring.basics.teamjob.controllers;


import io.datajek.spring.basics.teamjob.data.RoomDay;
import io.datajek.spring.basics.teamjob.data.SearchResult;
import io.datajek.spring.basics.teamjob.data.WeekDay;
import io.datajek.spring.basics.teamjob.data.*;
import io.datajek.spring.basics.teamjob.data.repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.repositories.UserRepository;
import io.datajek.spring.basics.teamjob.services.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final CalendarService calendarService;

    public CalendarController(EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository, CalendarService calendarService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.calendarService = calendarService;
    }

    @GetMapping({"", "/"})
    public String showWeekCalendar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String roomIds,
            Model model
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDate firstDayOfWeek = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<WeekDay> weekDays = new ArrayList<>();
        List<Event> allEvents = eventRepository.findOverlappingEvents(firstDayOfWeek.atStartOfDay(), firstDayOfWeek.plusDays(7).atStartOfDay());

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = firstDayOfWeek.plusDays(i);
            List<EventInADay> dayEvents = calendarService.convertToDayEvents(allEvents, currentDate, userIds, roomIds);

            weekDays.add(new WeekDay(
                    currentDate,
                    currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    currentDate.equals(LocalDate.now()),
                    dayEvents,
                    dayEvents.size()
            ));
        }

        LocalDate previousWeek = firstDayOfWeek.minusWeeks(1);
        LocalDate nextWeek = firstDayOfWeek.plusWeeks(1);
        List<Integer> hours = IntStream.rangeClosed(0, 23).boxed().collect(Collectors.toList());

        CalendarService.AddRepositories(model, eventRepository, userRepository, roomRepository);
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("currentWeekStart", firstDayOfWeek);
        model.addAttribute("previousWeek", previousWeek);
        model.addAttribute("nextWeek", nextWeek);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("hours", hours);
        model.addAttribute("userIds", userIds);
        model.addAttribute("roomIds", roomIds);

        return "calendar";
    }

    @GetMapping("/day")
    public String showDayCalendar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String roomIds,
            Model model
    ) {
        List<Event> allEvents = eventRepository.findOverlappingEvents(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(allEvents, date, userIds, roomIds);
        List<RoomDay> roomDays = new ArrayList<>();
        List<Room> rooms = roomRepository.findAll();

        //Filter rooms that have events on this day
        List<Room> filteredRooms = rooms.stream()
                .filter(room -> dayEvents.stream().anyMatch(event -> event.getRoom() != null && Objects.equals(event.getRoom().getId(), room.getId())))
                .toList();

        LocalDate previousDay = date.minusDays(1);
        LocalDate nextDay = date.plusDays(1);

        for (Room room : filteredRooms) {
            List<EventInADay> roomEvents = dayEvents.stream()
                    .filter(event -> event.getRoom() != null && Objects.equals(event.getRoom().getId(), room.getId()))
                    .collect(Collectors.toList());
            roomDays.add(new RoomDay(room, roomEvents));
        }
        List<Integer> hours = IntStream.rangeClosed(0, 23).boxed().collect(Collectors.toList());
        model.addAttribute("hours", hours);
        CalendarService.AddRepositories(model, eventRepository, userRepository, roomRepository);
        model.addAttribute("roomDays", roomDays);
        model.addAttribute("currentDay", date);
        model.addAttribute("previousDay", previousDay);
        model.addAttribute("nextDay", nextDay);
        model.addAttribute("selectedDate", date);
        model.addAttribute("userIds", userIds);
        model.addAttribute("roomIds", roomIds);

        return "calendar-day";
    }

    @GetMapping("/findAvailable")
    public String findAvailable(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(required = false) Integer durationMinutes,
            Model model) {

        // Default values
        if (date == null) {
            date = LocalDate.now();
        }
        if (startTime == null) {
            startTime = LocalTime.of(0, 0);// BY DEFAULT START AT 00:00!!!!!!!!!!!!!!!!!!!!!!!!!!
        }
        if (endTime == null) {
            endTime = LocalTime.of(23, 59);// BY DEFAULT END AT 23:59!!!!!!!!!!!!!!!!!!!!!!!!!!
        }
        if (durationMinutes == null) {
            durationMinutes = 30; // Default duration
        }
        if (searchType == null) {
            searchType = "rooms"; // Default search type
        }

        LocalDateTime searchStartTime = date.atTime(startTime);
        LocalDateTime searchEndTime = date.atTime(endTime);

        // Parse tags
        Set<String> tagSet = new HashSet<>();
        if (tags != null && !tags.isEmpty()) {
            tagSet.addAll(Arrays.asList(tags.split(",")));
        }

        List<SearchResult> results;


        // Generate available time request and search results
        AvailableTimeRequest availableTimeRequest = calendarService.generateAvailableTimeRequest(
                searchType,
                searchStartTime,
                searchEndTime,
                durationMinutes,
                tagSet
        );

        // Add availabilities to the model based on search type
        switch (searchType) {
            case "rooms" -> model.addAttribute("roomAvailabilities", availableTimeRequest.getRoomAvailabilities());
            case "users" -> model.addAttribute("userAvailabilities", availableTimeRequest.getUserAvailabilities());
            case "events" -> model.addAttribute("eventAvailabilities", availableTimeRequest.getEventAvailabilities());
        }

        // Generate search results from the available time request
        results = calendarService.generateSearchResults(availableTimeRequest, date);

        // Add all tags for the tag dropdown
        calendarService.gatherAllTags(model);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime);
        model.addAttribute("durationMinutes", durationMinutes);
        model.addAttribute("endTime", endTime);
        model.addAttribute("results", results);
        model.addAttribute("searchType", searchType);
        return "findAvailable";
    }

}
