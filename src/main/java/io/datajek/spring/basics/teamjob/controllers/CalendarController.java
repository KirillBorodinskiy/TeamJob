package io.datajek.spring.basics.teamjob.controllers;


import io.datajek.spring.basics.teamjob.WeekDay;
import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.EventInADay;
import io.datajek.spring.basics.teamjob.data.Repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
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
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final EventRepository eventRepository;

    public CalendarController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping({"", "/"})
    public String showWeekCalendar(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date, Model model) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDate firstDayOfWeek = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<WeekDay> weekDays = new ArrayList<>();
        List<Event> allEvents = eventRepository.findAll();

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = firstDayOfWeek.plusDays(i);
            List<EventInADay> dayEvents = convertToDayEvents(allEvents, currentDate);

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

        model.addAttribute("weekDays", weekDays);
        model.addAttribute("currentWeekStart", firstDayOfWeek);
        model.addAttribute("previousWeek", previousWeek);
        model.addAttribute("nextWeek", nextWeek);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("hours", hours);

        return "calendar";
    }

    private List<EventInADay> convertToDayEvents(List<Event> allEvents, LocalDate currentDate) {
        return allEvents.stream()
                .filter(event -> eventOccursOnDate(event, currentDate))
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
                            durationInADay,
                            startTimeToUse,
                            endTimeToUse,
                            event.getStartTime(),
                            event.getEndTime()
                    );
                })
                .collect(Collectors.toList());
    }

    private boolean eventOccursOnDate(Event event, LocalDate date) {
        // 1. Event starts on this day
        boolean startsToday = event.getStartTime().toLocalDate().equals(date);
        // 2. Event ends on this day
        boolean endsToday = event.getEndTime().toLocalDate().equals(date);
        // 3. Event spans over this day (starts before, ends after)
        boolean spansOver = event.getStartTime().toLocalDate().isBefore(date) &&
                event.getEndTime().toLocalDate().isAfter(date);

        return startsToday || endsToday || spansOver;
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