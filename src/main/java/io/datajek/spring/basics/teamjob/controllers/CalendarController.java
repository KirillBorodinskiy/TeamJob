package io.datajek.spring.basics.teamjob.controllers;


import io.datajek.spring.basics.teamjob.WeekDay;
import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.Room;
import io.datajek.spring.basics.teamjob.data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final EventRepository eventRepository;

    private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);

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

            List<Event> dayEvents = allEvents.stream()
                    .filter(event -> {
                        // Check if the event occurs on this day:
                        // 1. Event starts on this day
                        boolean startsToday = event.getStartTime().toLocalDate().equals(currentDate);
                        // 2. Event ends on this day
                        boolean endsToday = event.getEndTime().toLocalDate().equals(currentDate);
                        // 3. Event spans over this day (starts before, ends after)
                        boolean spansOver = event.getStartTime().toLocalDate().isBefore(currentDate) &&
                                event.getEndTime().toLocalDate().isAfter(currentDate);

                        return startsToday || endsToday || spansOver;
                    })
                    .collect(Collectors.toList());

            weekDays.add(new WeekDay(
                    currentDate,
                    currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    currentDate.equals(LocalDate.now()),
                    dayEvents
            ));
                    logger.info("Date: {}, Events: {}", currentDate, dayEvents.size());
        }

        LocalDate previousWeek = firstDayOfWeek.minusWeeks(1);
        LocalDate nextWeek = firstDayOfWeek.plusWeeks(1);
        List<Integer> hours = IntStream.rangeClosed(0, 24).boxed().collect(Collectors.toList());

        model.addAttribute("weekDays", weekDays);
        model.addAttribute("currentWeekStart", firstDayOfWeek);
        model.addAttribute("previousWeek", previousWeek);
        model.addAttribute("nextWeek", nextWeek);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("hours", hours);

        return "calendar";
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