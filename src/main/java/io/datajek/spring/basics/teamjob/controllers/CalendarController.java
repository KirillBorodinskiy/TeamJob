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

    public CalendarController(EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
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
            List<EventInADay> dayEvents = convertToDayEvents(allEvents, currentDate, userIds, roomIds);

            weekDays.add(new WeekDay(
                    currentDate,
                    currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    currentDate.equals(LocalDate.now()),
                    dayEvents
            ));
        }

        LocalDate previousWeek = firstDayOfWeek.minusWeeks(1);
        LocalDate nextWeek = firstDayOfWeek.plusWeeks(1);
        List<Integer> hours = IntStream.rangeClosed(0, 23).boxed().collect(Collectors.toList());

        AddRepositories(model, eventRepository, userRepository, roomRepository);
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
        List<EventInADay> dayEvents = convertToDayEvents(allEvents, date, userIds, roomIds);
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
        AddRepositories(model, eventRepository, userRepository, roomRepository);
        model.addAttribute("roomDays", roomDays);
        model.addAttribute("currentDay", date);
        model.addAttribute("previousDay", previousDay);
        model.addAttribute("nextDay", nextDay);
        model.addAttribute("selectedDate", date);
        model.addAttribute("userIds", userIds);
        model.addAttribute("roomIds", roomIds);

        return "calendar-day";
    }

    @GetMapping("/findRoom")
    public String findRoom(Model model) {
        gatherAllTags(model);
        return "findAvailable";
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
        gatherAllTags(model);
        model.addAttribute("results", results);
        model.addAttribute("searchType", searchType);
        return "findAvailable";
    }

    private void gatherAllTags(Model model) {
        Set<String> roomTags = new HashSet<>();
        roomRepository.findAll().forEach(room -> {
            if (room.getTags() != null) {
                roomTags.addAll(room.getTags());
            }
        });
        Set<String> userTags = new HashSet<>();
        userRepository.findAll().forEach(user -> {
            if (user.getTags() != null) {
                userTags.addAll(user.getTags());
            }
        });
        Set<String> eventTags = new HashSet<>();
        eventRepository.findAll().forEach(event -> {
            if (event.getTags() != null) {
                eventTags.addAll(event.getTags());
            }
        });

        model.addAttribute("roomTags", roomTags);
        model.addAttribute("userTags", userTags);
        model.addAttribute("eventTags", eventTags);
    }

    private List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String userIds, String roomIds) {
        return allEvents.stream()
                .filter(event -> filterEvents(event, currentDate, Optional.ofNullable(userIds), Optional.ofNullable(roomIds)))
                .map(event -> {
                    double durationInADay = calculateHoursInDay(event, currentDate);
                    double startTimeToUse = event.getStartTime().toLocalDate().isBefore(currentDate) ?
                            0.0 : event.getStartTime().getHour() + (event.getStartTime().getMinute() / 60.0);

                    double endTimeToUse = event.getEndTime().toLocalDate().isAfter(currentDate) ?
                            24.0 : event.getEndTime().getHour() + (event.getEndTime().getMinute() / 60.0);
                    return new EventInADay(
                            event.getId(),
                            event.getTitle(),
                            event.getDescription(),
                            event.getRoom(),
                            event.getUser(),
                            event.isRecurring(),
                            event.getIsRecurringEndDate(),
                            durationInADay,
                            startTimeToUse,
                            endTimeToUse,
                            event.getStartTime(),
                            event.getEndTime()
                    );
                })
                .collect(Collectors.toList());
    }

    private boolean filterEvents(Event event, LocalDate date, Optional<String> userIds, Optional<String> roomIds) {
        // 1. Event starts on this day
        boolean startsToday = event.getStartTime().toLocalDate().equals(date);
        // 2. Event ends on this day
        boolean endsToday = event.getEndTime().toLocalDate().equals(date);
        // 3. Event spans over this day (starts before, ends after)
        boolean spansOver = event.getStartTime().toLocalDate().isBefore(date) &&
                event.getEndTime().toLocalDate().isAfter(date);
        boolean dateMatch = startsToday || endsToday || spansOver;
        //The filtering above is still needed even after findOverlappingEvents is used, as we are filtering each day


        // If no filters are applied, only check date
        if (userIds.isEmpty() && roomIds.isEmpty()) {
            return dateMatch;
        }
        boolean userMatch = true;
        if (userIds.isPresent() && !userIds.get().isEmpty() && event.getUser() != null) {
            Set<String> userIdSet = Arrays.stream(userIds.get().split(","))
                    .collect(Collectors.toSet());
            userMatch = userIdSet.contains(String.valueOf(event.getUser().getId()));
        }

        // Room filter
        boolean roomMatch = true;
        if (roomIds.isPresent() && !roomIds.get().isEmpty() && event.getRoom() != null) {
            Set<String> roomIdSet = Arrays.stream(roomIds.get().split(","))
                    .collect(Collectors.toSet());
            roomMatch = roomIdSet.contains(String.valueOf(event.getRoom().getId()));
        }

        return dateMatch && userMatch && roomMatch;
    }

    private double calculateHoursInDay(Event event, LocalDate date) {
        // If event starts before this day, use midnight as start time
        LocalDateTime startTimeToUse = event.getStartTime().toLocalDate().isBefore(date) ?
                date.atStartOfDay() : event.getStartTime();

        // If event ends after this day, use end of day as end time
        LocalDateTime endTimeToUse = event.getEndTime().toLocalDate().isAfter(date) ?
                date.atTime(23, 59, 59) : event.getEndTime();

        // Calculate hours between these two times
        return java.time.Duration.between(startTimeToUse, endTimeToUse).toMinutes() / 60.0;
    }

    static void AddRepositories(Model model, EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository) {
        List<Event> eventList = eventRepository.findAll();
        List<User> userList = userRepository.findAll();
        List<Room> roomList = roomRepository.findAll();

        model.addAttribute("events", eventList);
        model.addAttribute("rooms", roomList);
        model.addAttribute("users", userList);
    }
}