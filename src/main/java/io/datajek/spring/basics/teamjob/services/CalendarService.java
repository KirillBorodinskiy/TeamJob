package io.datajek.spring.basics.teamjob.services;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.EventInADay;
import io.datajek.spring.basics.teamjob.data.Room;
import io.datajek.spring.basics.teamjob.data.User;
import io.datajek.spring.basics.teamjob.data.repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


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

    public List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate, String userIds, String roomIds) {
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

    public boolean filterEvents(Event event, LocalDate date, Optional<String> userIds, Optional<String> roomIds) {
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

    public static void AddRepositories(Model model, EventRepository eventRepository, UserRepository userRepository, RoomRepository roomRepository) {
        List<Event> eventList = eventRepository.findAll();
        List<User> userList = userRepository.findAll();
        List<Room> roomList = roomRepository.findAll();

        model.addAttribute("events", eventList);
        model.addAttribute("rooms", roomList);
        model.addAttribute("users", userList);
    }
}
