package com.borodkir.teamjob.controllers;


import com.borodkir.teamjob.services.ICalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/calendar")
public class CalendarController {
    private final ICalendarService calendarService;

    public CalendarController(ICalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping({"", "/"})
    public String showWeekCalendar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String roomIds,
            @RequestParam(required = false) String roomTags,
            @RequestParam(required = false) String eventTags,
            @RequestParam(required = false) String userTags,
            Model model
    ) {
        calendarService.setupModelForWeekCalendar(model, date, userIds, roomIds, roomTags, eventTags, userTags);

        return "calendar";
    }

    @GetMapping("/day")
    public String showDayCalendar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String userIds,
            @RequestParam(required = false) String roomIds,
            @RequestParam(required = false) String roomTags,
            @RequestParam(required = false) String eventTags,
            @RequestParam(required = false) String userTags,
            Model model
    ) {
        calendarService.setupModelForDayCalendar(model, date, userIds, roomIds, roomTags, eventTags, userTags);

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
        calendarService.setupModelForFindAvailable(model, searchType, tags, date, startTime, endTime, durationMinutes);

        return "findAvailable";
    }

}
