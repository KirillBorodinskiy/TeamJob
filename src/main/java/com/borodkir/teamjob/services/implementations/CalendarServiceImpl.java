package com.borodkir.teamjob.services.implementations;

import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import com.borodkir.teamjob.services.ICalendarService;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.disjoint;


/**
 * The CalendarService class provides various utility methods for managing and retrieving
 * calendar data including events, users, rooms, and their availabilities. It utilizes
 * repositories for accessing stored entities and provides filtering, availability checking,
 * and time slot generation functionalities.
 */
@Service
public class CalendarServiceImpl implements ICalendarService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public CalendarServiceImpl(EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Generates an available time request based on the provided type, time range, and session duration.
     * The method calculates time ranges that are unoccupied within the specified time frame and type.
     *
     * @param type              the type of request, which determines how the entities are filtered ("rooms", "users", or "events")
     * @param startTime         the start time of the time range to analyze
     * @param endTime           the end time of the time range to analyze
     * @param durationInMinutes the minimum duration, in minutes, required for an available time slot
     * @param tags              set of tags to filter entities by
     * @return an AvailableTimeRequest object containing the list of unoccupied time intervals within the specified range and type
     * @throws IllegalArgumentException if the provided type is not valid
     */
    public AvailableTimeRequest generateAvailableTimeRequest(
            String type,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationInMinutes,
            Set<String> tags
    ) {
        // Create a base request with the provided parameters
        AvailableTimeRequest request = new AvailableTimeRequest(startTime, endTime, type);

        // Get all events that overlap with the specified time range
        List<Event> allEvents = eventRepository.findOverlappingEvents(startTime, endTime);

        switch (type) {
            case "rooms":
                // Process room availabilities
                List<Room> filteredRooms = filterRoomsByTags(roomRepository.findAll(), tags);
                List<RoomAvailability> roomAvailabilities = calculateRoomAvailabilities(
                        filteredRooms, allEvents, startTime, endTime, durationInMinutes);
                request.setRoomAvailabilities(roomAvailabilities);
                break;

            case "users":
                // Process user availabilities
                List<User> filteredUsers = filterUsersByTags(userRepository.findAll(), tags);
                List<UserAvailability> userAvailabilities = calculateUserAvailabilities(
                        filteredUsers, allEvents, startTime, endTime, durationInMinutes);
                request.setUserAvailabilities(userAvailabilities);
                break;


            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }

        return request;
    }

    /**
     * Filters a list of rooms by the specified tags.
     *
     * @param rooms the list of rooms to filter
     * @param tags  the set of tags to filter by
     * @return a filtered list of rooms that have at least one of the specified tags
     */
    private List<Room> filterRoomsByTags(List<Room> rooms, Set<String> tags) {
        if (tags.isEmpty()) {
            return rooms;
        }

        return rooms.stream().filter(room -> room.getTags() != null && !disjoint(room.getTags(), tags)).toList();
    }

    /**
     * Filters a list of users by the specified tags.
     *
     * @param users the list of users to filter
     * @param tags  the set of tags to filter by
     * @return a filtered list of users that have at least one of the specified tags
     */
    private List<User> filterUsersByTags(List<User> users, Set<String> tags) {
        if (tags.isEmpty()) {
            return users;
        }

        return users.stream()
                .filter(user -> user.getTags() != null && !disjoint(user.getTags(), tags))
                .toList();
    }

    /**
     * Calculates room availabilities based on the provided rooms, events, and time constraints.
     *
     * @param rooms             the list of rooms to calculate availabilities for
     * @param allEvents         all events that might affect room availability
     * @param startTime         the start time of the time range to analyze
     * @param endTime           the end time of the time range to analyze
     * @param durationInMinutes the minimum duration required for an available time slot
     * @return a list of room availabilities with their unoccupied time slots
     */
    private List<RoomAvailability> calculateRoomAvailabilities(
            List<Room> rooms,
            List<Event> allEvents,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationInMinutes) {

        List<RoomAvailability> roomAvailabilities = new ArrayList<>();

        for (Room room : rooms) {
            // Get events for this room
            List<Event> eventsForRoom = allEvents.stream()
                    .filter(event -> event.getRoom() != null && event.getRoom().getId().equals(room.getId()))
                    .toList();

            // Create occupied time slots for this room
            ArrayList<TimeFromTo> occupiedTimes = createOccupiedTimeSlots(eventsForRoom);

            // Calculate unoccupied time slots for this room
            ArrayList<TimeFromTo> unoccupiedTimes = calculateUnoccupiedTimesFromOccupied(
                    startTime, endTime, durationInMinutes, occupiedTimes);

            // Add room availability to the list
            roomAvailabilities.add(new RoomAvailability(room, unoccupiedTimes));
        }

        return roomAvailabilities;
    }

    /**
     * Calculates user availabilities based on the provided users, events, and time constraints.
     *
     * @param users             the list of users to calculate availabilities for
     * @param allEvents         all events that might affect user availability
     * @param startTime         the start time of the time range to analyze
     * @param endTime           the end time of the time range to analyze
     * @param durationInMinutes the minimum duration required for an available time slot
     * @return a list of user availabilities with their unoccupied time slots
     */
    private List<UserAvailability> calculateUserAvailabilities(
            List<User> users,
            List<Event> allEvents,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationInMinutes) {

        List<UserAvailability> userAvailabilities = new ArrayList<>();

        for (User user : users) {
            // Get events for this user
            List<Event> eventsForUser = allEvents.stream()
                    .filter(event -> event.getUser() != null && event.getUser().getId().equals(user.getId()))
                    .toList();

            // Create occupied time slots for this user
            ArrayList<TimeFromTo> occupiedTimes = createOccupiedTimeSlots(eventsForUser);

            // Calculate unoccupied time slots for this user
            ArrayList<TimeFromTo> unoccupiedTimes = calculateUnoccupiedTimesFromOccupied(
                    startTime, endTime, durationInMinutes, occupiedTimes);

            // Add user availability to the list
            userAvailabilities.add(new UserAvailability(user, unoccupiedTimes));
        }

        return userAvailabilities;
    }

    /**
     * Creates a list of occupied time slots from a list of events.
     *
     * @param events the list of events to create occupied time slots from
     * @return a list of occupied time slots
     */
    private ArrayList<TimeFromTo> createOccupiedTimeSlots(List<Event> events) {
        ArrayList<TimeFromTo> occupiedTimes = new ArrayList<>();

        for (Event event : events) {
            TimeFromTo time = new TimeFromTo(event.getStartTime(), event.getEndTime());
            occupiedTimes.add(time);
        }

        return occupiedTimes;
    }


    /**
     * Calculates the unoccupied time intervals within a given time range, based on a list
     * of occupied time intervals. The resulting unoccupied intervals are filtered by a
     * minimum duration and sorted.
     *
     * @param startTime         the start time of the range to be checked for unoccupied intervals
     * @param endTime           the end time of the range to be checked for unoccupied intervals
     * @param durationInMinutes the minimum duration (in minutes) of an unoccupied interval
     *                          to be included in the result
     * @param occupied          a list of occupied time intervals represented by {@link TimeFromTo} objects
     * @return an ArrayList of unoccupied intervals represented by {@link TimeFromTo} objects,
     * sorted by start time and filtered by the specified minimum duration
     */
    private ArrayList<TimeFromTo> calculateUnoccupiedTimesFromOccupied(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationInMinutes,
            ArrayList<TimeFromTo> occupied
    ) {
        // Sort the occupied time by start time
        occupied.sort(Comparator.comparing(TimeFromTo::getStartTime));
        // Remove overlapping occupied times
        removeOverlaps(occupied);


        ArrayList<TimeFromTo> unoccupiedTimes = new ArrayList<>();
        LocalDateTime currentStartTimeEvent = startTime;
        for (TimeFromTo occupiedTime : occupied) {
            if (currentStartTimeEvent.isBefore(occupiedTime.getStartTime())) {
                unoccupiedTimes.add(new TimeFromTo(currentStartTimeEvent, occupiedTime.getStartTime()));
            }
            currentStartTimeEvent = occupiedTime.getEndTime();
        }
        if (currentStartTimeEvent.isBefore(endTime)) {
            unoccupiedTimes.add(new TimeFromTo(currentStartTimeEvent, endTime));
        }
        // Filter unoccupied times by duration
        unoccupiedTimes = unoccupiedTimes.stream()
                .filter(time -> time.getDurationInMinutes() >= durationInMinutes)
                .collect(Collectors.toCollection(ArrayList::new));
        // Sort the unoccupied times by start time
        unoccupiedTimes.sort(Comparator.comparing(TimeFromTo::getStartTime));
        // Remove overlapping unoccupied times
        removeOverlaps(unoccupiedTimes);

        return unoccupiedTimes;
    }

    /**
     * Removes overlapping time intervals from the provided list of TimeFromTo objects.
     * If two intervals overlap, they are merged into one interval that extends to the later end time.
     * The method modifies the input list in place.
     *
     * @param timeFromTo the list of TimeFromTo objects representing time intervals to process.
     *                   The intervals must be sorted in ascending order by their start time
     *                   before invoking this method for it to work correctly.
     */
    private void removeOverlaps(ArrayList<TimeFromTo> timeFromTo) {
        for (int i = 0; i < timeFromTo.size() - 1; i++) {
            if (timeFromTo.get(i).getEndTime().isAfter(timeFromTo.get(i + 1).getStartTime())) {
                timeFromTo.get(i).setEndTime(timeFromTo.get(i + 1).getEndTime());
                timeFromTo.remove(i + 1);
                i--;
            }
        }
    }

    /**
     * Generates a list of SearchResult objects based on the provided AvailableTimeRequest.
     *
     * @param request the AvailableTimeRequest containing availabilities
     * @param date    the date of the search
     * @return a list of SearchResult objects representing available entities
     */
    public List<SearchResult> generateSearchResults(AvailableTimeRequest request, LocalDate date) {
        List<SearchResult> results = new ArrayList<>();
        String type = request.getType();

        switch (type) {
            case "rooms" -> {
                // Process room availabilities
                for (RoomAvailability roomAvail : request.getRoomAvailabilities()) {
                    Room room = roomAvail.getRoom();
                    // If there are unoccupied time slots, the room is available
                    if (!roomAvail.getUnoccupiedTimesFromTo().isEmpty()) {
                        results.add(new SearchResult("room", room.getId(), room.getName(), room.getTags(), date));
                    }
                }
            }

            case "users" -> {
                // Process user availabilities
                for (UserAvailability userAvail : request.getUserAvailabilities()) {
                    User user = userAvail.getUser();
                    // If there are unoccupied time slots, the user is available
                    if (!userAvail.getUnoccupiedTimesFromTo().isEmpty()) {
                        results.add(new SearchResult("user", user.getId(), user.getUsername(), user.getTags(), date));
                    }
                }
            }

            case "events" -> {
                // Process event availabilities
                for (EventAvailability eventAvail : request.getEventAvailabilities()) {
                    Event event = eventAvail.getEvent();
                    // For events, we add all events that match the criteria
                    results.add(new SearchResult("event", event.getId(), event.getTitle(), event.getTags(), date));
                }
            }
        }

        return results;
    }


    @Override
    public void setupModelForWeekCalendar(Model model, LocalDate date, String userIds, String roomIds, String roomTags, String eventTags, String userTags) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDate firstDayOfWeek = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<WeekDay> weekDays = new ArrayList<>();
        List<Event> allEvents = eventRepository.findOverlappingEvents(firstDayOfWeek.atStartOfDay(), firstDayOfWeek.plusDays(7).atStartOfDay());

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = firstDayOfWeek.plusDays(i);
            List<EventInADay> dayEvents = convertToDayEvents(allEvents, currentDate, userIds, roomIds, roomTags, eventTags, userTags);

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

        AddRepositories(model, eventRepository, userRepository, roomRepository);
        gatherAllTags(model);
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("currentWeekStart", firstDayOfWeek);
        model.addAttribute("previousWeek", previousWeek);
        model.addAttribute("nextWeek", nextWeek);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("hours", hours);
        model.addAttribute("userIds", userIds);
        model.addAttribute("roomIds", roomIds);
    }


    @Override
    public void setupModelForDayCalendar(Model model, LocalDate date, String userIds, String roomIds,
                                         String roomTags, String eventTags, String userTags) {

        List<Event> allEvents = eventRepository.findOverlappingEvents(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        List<EventInADay> dayEvents = convertToDayEvents(allEvents, date, userIds, roomIds, roomTags, eventTags, userTags);
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
        CalendarServiceImpl.AddRepositories(model, eventRepository, userRepository, roomRepository);
        model.addAttribute("roomDays", roomDays);
        model.addAttribute("currentDay", date);
        model.addAttribute("previousDay", previousDay);
        model.addAttribute("nextDay", nextDay);
        model.addAttribute("selectedDate", date);
        model.addAttribute("userIds", userIds);
        model.addAttribute("roomIds", roomIds);
        gatherAllTags(model);
    }

    @Override
    public void setupModelForFindAvailable(Model model, String searchType, String tags, LocalDate date, LocalTime startTime, LocalTime endTime, Integer durationMinutes) {

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
        AvailableTimeRequest availableTimeRequest = generateAvailableTimeRequest(
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
        results = generateSearchResults(availableTimeRequest, date);

        // Add all tags for the tag dropdown
        gatherAllTags(model);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime);
        model.addAttribute("durationMinutes", durationMinutes);
        model.addAttribute("endTime", endTime);
        model.addAttribute("results", results);
        model.addAttribute("searchType", searchType);
    }

    public void gatherAllTags(Model model) {
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

    private Set<String> parseCommaDelimitedString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return Stream.of(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String userIds, String roomIds, String userTags, String roomTags, String eventTags) {
        // Prepare filters
        Set<String> userIdSet = parseCommaDelimitedString(userIds);
        Set<String> roomIdSet = parseCommaDelimitedString(roomIds);
        Set<String> userTagSet = parseCommaDelimitedString(userTags);
        Set<String> roomTagSet = parseCommaDelimitedString(roomTags);
        Set<String> eventTagSet = parseCommaDelimitedString(eventTags);

        List<EventInADay> result = new ArrayList<>();
        for (Event event : allEvents) {
            // 1. Exclude if exdate contains the current date
            if (isExcludedByExdate(event, currentDate)) continue;

            // 2. Check if the event occurs on this date (regular or recurring)
            boolean occursToday = false;

            // Check regular event date
            if (isEventOnDate(event, currentDate)) {
                occursToday = true;
            }
            // Check recurring event
            else if (event.isRecurring() && event.getRrule() != null) {
                occursToday = isRecurringEventOnDate(event, currentDate);
            }

            if (!occursToday) continue;

            // 3. Apply user filter
            if (userIdSet != null && (event.getUser() == null || !userIdSet.contains(String.valueOf(event.getUser().getId()))))
                continue;

            // 4. Apply room filter
            if (roomIdSet != null && (event.getRoom() == null || !roomIdSet.contains(String.valueOf(event.getRoom().getId()))))
                continue;


            // Disjoint is used to find at least one common tag
            // 5. Apply room tags filter
            if (roomTagSet != null && (event.getRoom() == null || event.getRoom().getTags() == null ||
                    disjoint(event.getRoom().getTags(), roomTagSet)))
                continue;

            // 6. Apply event tags filter
            if (eventTagSet != null && (event.getTags() == null || disjoint(event.getTags(), eventTagSet)))
                continue;

            // 7. Apply user tags filter
            if (userTagSet != null && (event.getUser() == null || event.getUser().getTags() == null ||
                    disjoint(event.getUser().getTags(), userTagSet)))
                continue;


            // 8. Calculate start/end times for this day
            LocalDateTime eventStart = event.getStartTime();
            LocalDateTime eventEnd = event.getEndTime();

            // For recurring events, adjust the times based on the recurrence pattern
            if (event.isRecurring() && event.getRrule() != null) {
                int daysBetween = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        event.getStartTime().toLocalDate(), currentDate);
                eventStart = event.getStartTime().plusDays(daysBetween);
                eventEnd = event.getEndTime().plusDays(daysBetween);
            }

            // Calculate duration and start/end times for this day
            double startTimeToUse = eventStart.toLocalDate().isBefore(currentDate) ?
                    0.0 : eventStart.getHour() + (eventStart.getMinute() / 60.0);
            double endTimeToUse = eventEnd.toLocalDate().isAfter(currentDate) ?
                    24.0 : eventEnd.getHour() + (eventEnd.getMinute() / 60.0);
            double durationInADay = endTimeToUse - startTimeToUse;

            // 6. Add to result
            result.add(new EventInADay(
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getRoom(),
                    event.getUser(),
                    event.isRecurring(),
                    event.getRecurrenceEndDate(),
                    durationInADay,
                    startTimeToUse,
                    endTimeToUse,
                    eventStart,
                    eventEnd
            ));
        }
        return result;
    }

    public List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String userIds, String roomIds) {
        return convertToDayEvents(allEvents, currentDate, userIds, roomIds, null, null, null);
    }

    public static void AddRepositories(Model model, EventRepository eventRepository, UserRepository
            userRepository, RoomRepository roomRepository) {
        List<Event> eventList = eventRepository.findAll();
        List<User> userList = userRepository.findAll();
        List<Room> roomList = roomRepository.findAll();

        model.addAttribute("events", eventList);
        model.addAttribute("rooms", roomList);
        model.addAttribute("users", userList);
    }

    // Helper for exdate exclusion
    private boolean isExcludedByExdate(Event event, LocalDate date) {
        if (event.getExdate() == null || event.getExdate().isEmpty()) return false;
        Set<LocalDate> exdates = Arrays.stream(event.getExdate().split(","))
                .map(d -> LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd")))
                .collect(Collectors.toSet());
        return exdates.contains(date);
    }

    private boolean isEventOnDate(Event event, LocalDate date) {
        LocalDate eventStartDate = event.getStartTime().toLocalDate();
        LocalDate eventEndDate = event.getEndTime().toLocalDate();

        return !eventStartDate.isAfter(date) && !eventEndDate.isBefore(date);
    }

    private boolean isRecurringEventOnDate(Event event, LocalDate date) {
        // Basic validation
        if (!isValidRecurringEvent(event)) {
            return false;
        }

        // Check end date and exclusions
        if (isAfterEndDate(event, date) || isExcludedDate(event, date)) {
            return false;
        }

        // Parse recurrence rule
        RecurrenceRule rule = parseRecurrenceRule(event.getRrule());

        // Check if date matches the recurrence pattern
        return switch (rule.frequency()) {
            case "DAILY" -> isDailyRecurrence(event, date, rule.interval());
            case "WEEKLY" -> isWeeklyRecurrence(event, date, rule);
            case "MONTHLY" -> isMonthlyRecurrence(event, date, rule.interval());
            case "YEARLY" -> isYearlyRecurrence(event, date, rule.interval());
            default -> false;
        };
    }

    private record RecurrenceRule(String frequency, int interval, String[] byDay, LocalDateTime until) {
    }

    private boolean isValidRecurringEvent(Event event) {
        return event.isRecurring() && event.getRrule() != null;
    }

    private boolean isAfterEndDate(Event event, LocalDate date) {
        // First check recurrenceEndDate
        if (event.getRecurrenceEndDate() != null &&
                date.isAfter(event.getRecurrenceEndDate().toLocalDate())) {
            return true;
        }

        // Then check UNTIL in RRULE if it exists
        if (event.getRrule() != null) {
            RecurrenceRule rule = parseRecurrenceRule(event.getRrule());
            return rule.until() != null && date.isAfter(rule.until().toLocalDate());
        }

        return false;
    }

    private boolean isExcludedDate(Event event, LocalDate date) {
        if (event.getExdate() == null || event.getExdate().isEmpty()) {
            return false;
        }
        Set<LocalDate> exdates = Arrays.stream(event.getExdate().split(","))
                .map(d -> LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd")))
                .collect(Collectors.toSet());
        return exdates.contains(date);
    }

    private RecurrenceRule parseRecurrenceRule(String rrule) {
        String[] parts = rrule.split(";");
        String frequency = "";
        int interval = 1;
        String[] byDay = null;
        LocalDateTime until = null;

        for (String part : parts) {
            if (part.startsWith("FREQ=")) {
                frequency = part.substring(5);
            } else if (part.startsWith("INTERVAL=")) {
                interval = Integer.parseInt(part.substring(9));
            } else if (part.startsWith("BYDAY=")) {
                byDay = part.substring(6).split(",");
            } else if (part.startsWith("UNTIL=")) {
                String untilStr = part.substring(6);
                // Parse the UNTIL date in format yyyyMMddTHHmmssZ
                until = LocalDateTime.parse(
                        untilStr.substring(0, 8) + "T" + untilStr.substring(9, 15),
                        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                );
            }
        }

        return new RecurrenceRule(frequency, interval, byDay, until);
    }

    private boolean isDailyRecurrence(Event event, LocalDate date, int interval) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                event.getStartTime().toLocalDate(), date);
        return daysBetween % interval == 0;
    }

    private boolean isWeeklyRecurrence(Event event, LocalDate date, RecurrenceRule rule) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                event.getStartTime().toLocalDate(), date);

        // If specific days are specified, check if current day matches
        if (rule.byDay() != null) {
            String targetDay = getDayOfWeekCode(date.getDayOfWeek());
            return Arrays.asList(rule.byDay()).contains(targetDay) &&
                    (daysBetween / 7) % rule.interval() == 0;
        }

        // Otherwise, check if it's the right week
        return daysBetween % (7L * rule.interval()) == 0;
    }

    private boolean isMonthlyRecurrence(Event event, LocalDate date, int interval) {
        // Check if day of month matches
        if (event.getStartTime().getDayOfMonth() != date.getDayOfMonth()) {
            return false;
        }

        // Calculate total months between dates
        int monthDiff = (date.getYear() - event.getStartTime().getYear()) * 12 +
                (date.getMonthValue() - event.getStartTime().getMonthValue());

        return monthDiff % interval == 0;
    }

    private boolean isYearlyRecurrence(Event event, LocalDate date, int interval) {
        return event.getStartTime().getDayOfMonth() == date.getDayOfMonth() &&
                event.getStartTime().getMonth() == date.getMonth() &&
                (date.getYear() - event.getStartTime().getYear()) % interval == 0;
    }

    private String getDayOfWeekCode(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };
    }
}

