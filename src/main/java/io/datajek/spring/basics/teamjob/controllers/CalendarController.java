package io.datajek.spring.basics.teamjob.controllers;


import io.datajek.spring.basics.teamjob.RoomDay;
import io.datajek.spring.basics.teamjob.SearchResult;
import io.datajek.spring.basics.teamjob.WeekDay;
import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.EventInADay;
import io.datajek.spring.basics.teamjob.data.repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.Room;
import io.datajek.spring.basics.teamjob.data.User;
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
            @RequestParam(required = false) String[] searchType,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model) {

        // Default to current date if not provided
        LocalDate searchDate = (date != null) ? date : LocalDate.now();

        // Parse time strings to LocalTime if provided

        LocalTime searchStartTime = null;
        LocalTime searchEndTime = null;

        if (startTime != null && !startTime.isEmpty()) {
            searchStartTime = LocalTime.parse(startTime);
        }

        if (endTime != null && !endTime.isEmpty()) {
            searchEndTime = LocalTime.parse(endTime);
        }

        // Create LocalDateTime objects for start and end times
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (searchStartTime != null) {
            startDateTime = LocalDateTime.of(searchDate, searchStartTime);
        } else {
            startDateTime = searchDate.atStartOfDay();
        }

        if (searchEndTime != null) {
            endDateTime = LocalDateTime.of(searchDate, searchEndTime);
        } else {
            endDateTime = searchDate.atTime(23, 59, 59);
        }

        // Parse tags
        Set<String> tagSet = new HashSet<>();
        if (tags != null && !tags.isEmpty()) {
            tagSet.addAll(Arrays.asList(tags.split(",")));
        }

        List<SearchResult> results = new ArrayList<>();

        // If no search type is selected, default to searching for rooms
        if (searchType == null || searchType.length == 0) {
            searchType = new String[]{"rooms"};
        }

        // Search for rooms
        if (Arrays.asList(searchType).contains("rooms")) {
            List<Room> rooms = roomRepository.findAll();

            // Filter rooms by tags if tags are provided
            if (!tagSet.isEmpty()) {
                rooms = rooms.stream()
                        .filter(room -> room.getTags() != null && !Collections.disjoint(room.getTags(), tagSet))
                        .toList();
            }

            // Check room availability
            for (Room room : rooms) {
                boolean isAvailable = true;

                // Get all events in the date range
                List<Event> allEvents = eventRepository.findOverlappingEvents(startDateTime, endDateTime);

                // Filter events for this room
                List<Event> roomEvents = allEvents.stream()
                        .filter(event -> event.getRoom() != null && event.getRoom().getId().equals(room.getId()))
                        .toList();

                // Check if any events overlap with the search time range
                if (searchStartTime != null && searchEndTime != null) {
                    for (Event event : roomEvents) {
                        // Check if event overlaps with search time range
                        if (!(event.getEndTime().isBefore(startDateTime) ||
                                event.getStartTime().isAfter(endDateTime))) {
                            isAvailable = false;
                            break;
                        }
                    }
                }

                if (isAvailable) {
                    results.add(new SearchResult("room", room.getId(), room.getName(), room.getTags(), searchDate));
                }
            }
        }

        // Search for people (users)
        if (Arrays.asList(searchType).contains("people")) {
            List<User> users = userRepository.findAll();

            // Filter users by tags if tags are provided
            if (!tagSet.isEmpty()) {
                users = users.stream()
                        .filter(user -> user.getTags() != null && !Collections.disjoint(user.getTags(), tagSet))
                        .toList();
            }

            // Check user availability
            for (User user : users) {
                boolean isAvailable = true;

                // Get all events in the date range
                List<Event> allEvents = eventRepository.findOverlappingEvents(startDateTime, endDateTime);

                // Filter events for this user
                List<Event> userEvents = allEvents.stream()
                        .filter(event -> event.getUser() != null && event.getUser().getId().equals(user.getId()))
                        .toList();

                // Check if any events overlap with the search time range
                if (searchStartTime != null && searchEndTime != null) {
                    for (Event event : userEvents) {
                        // Check if event overlaps with search time range
                        if (!(event.getEndTime().isBefore(startDateTime) ||
                                event.getStartTime().isAfter(endDateTime))) {
                            isAvailable = false;
                            break;
                        }
                    }
                }

                if (isAvailable) {
                    results.add(new SearchResult("user", user.getId(), user.getUsername(), user.getTags(), searchDate));
                }
            }
        }

        // Search for events
        if (Arrays.asList(searchType).contains("events")) {
            List<Event> events = eventRepository.findOverlappingEvents(
                    searchDate.atStartOfDay(),
                    searchDate.plusDays(1).atStartOfDay());

            // Filter events by tags if tags are provided
            if (!tagSet.isEmpty()) {
                events = events.stream()
                        .filter(event -> event.getTags() != null && !Collections.disjoint(event.getTags(), tagSet))
                        .toList();
            }

            // Add matching events to results
            for (Event event : events) {
                results.add(new SearchResult("event", event.getId(), event.getTitle(), event.getTags(), searchDate));
            }
        }

        // Add all tags for the tag dropdown
        calendarService.gatherAllTags(model);
        model.addAttribute("results", results);
        model.addAttribute("searchType", searchType);
        return "findAvailable";
    }

}