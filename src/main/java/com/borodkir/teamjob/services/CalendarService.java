package com.borodkir.teamjob.services;

import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The CalendarService class provides various utility methods for managing and retrieving
 * calendar data including events, users, rooms, and their availabilities. It utilizes
 * repositories for accessing stored entities and provides filtering, availability checking,
 * and time slot generation functionalities.
 */
@Service
public class CalendarService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public CalendarService(EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository) {
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

            case "events":
                // Process event availabilities
                List<Event> filteredEvents = filterEventsByTags(allEvents, tags);
                List<EventAvailability> eventAvailabilities = calculateEventAvailabilities(
                        filteredEvents, startTime, endTime, durationInMinutes);
                request.setEventAvailabilities(eventAvailabilities);
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

        return rooms.stream().filter(room -> room.getTags() != null && !Collections.disjoint(room.getTags(), tags)).toList();
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
                .filter(user -> user.getTags() != null && !Collections.disjoint(user.getTags(), tags))
                .toList();
    }

    /**
     * Filters a list of events by the specified tags.
     *
     * @param events the list of events to filter
     * @param tags   the set of tags to filter by
     * @return a filtered list of events that have at least one of the specified tags
     */
    private List<Event> filterEventsByTags(List<Event> events, Set<String> tags) {
        if (tags.isEmpty()) {
            return events;
        }

        return events.stream()
                .filter(event -> event.getTags() != null && !Collections.disjoint(event.getTags(), tags))
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
     * Calculates event availabilities based on the provided events and time constraints.
     *
     * @param events            the list of events to calculate availabilities for
     * @param startTime         the start time of the time range to analyze
     * @param endTime           the end time of the time range to analyze
     * @param durationInMinutes the minimum duration required for an available time slot
     * @return a list of event availabilities with their unoccupied time slots
     */
    private List<EventAvailability> calculateEventAvailabilities(
            List<Event> events,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationInMinutes) {

        List<EventAvailability> eventAvailabilities = new ArrayList<>();

        for (Event event : events) {
            // Create occupied time slots for this event
            ArrayList<TimeFromTo> occupiedTimes = new ArrayList<>();
            occupiedTimes.add(new TimeFromTo(event.getStartTime(), event.getEndTime()));

            // Calculate unoccupied time slots for this event
            ArrayList<TimeFromTo> unoccupiedTimes = calculateUnoccupiedTimesFromOccupied(
                    startTime, endTime, durationInMinutes, occupiedTimes);

            // Add event availability to the list
            eventAvailabilities.add(new EventAvailability(event, unoccupiedTimes));
        }

        return eventAvailabilities;
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

    public List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String
            userIds, String roomIds) {
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

    public boolean filterEvents(Event event, LocalDate
            date, Optional<String> userIds, Optional<String> roomIds) {
        // 1. Event starts on this day
        boolean startsToday = event.getStartTime().toLocalDate().equals(date);
        // 2. Event ends on this day
        boolean endsToday = event.getEndTime().toLocalDate().equals(date);
        // 3. Event spans over this day (starts before, ends after)
        boolean spansOver = event.getStartTime().toLocalDate().isBefore(date) &&
                event.getEndTime().toLocalDate().isAfter(date);
        boolean dateMatch = startsToday || endsToday || spansOver;

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

    public double calculateHoursInDay(Event event, LocalDate date) {
        // If event starts before this day, use midnight as start time
        LocalDateTime startTimeToUse = event.getStartTime().toLocalDate().isBefore(date) ?
                date.atStartOfDay() : event.getStartTime();

        // If event ends after this day, use end of day as end time
        LocalDateTime endTimeToUse = event.getEndTime().toLocalDate().isAfter(date) ?
                date.atTime(23, 59, 59) : event.getEndTime();

        // Calculate hours between these two times
        return java.time.Duration.between(startTimeToUse, endTimeToUse).toMinutes() / 60.0;
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
}
