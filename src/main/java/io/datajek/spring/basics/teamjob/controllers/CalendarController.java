package io.datajek.spring.basics.teamjob.controllers;


import io.datajek.spring.basics.teamjob.WeekDay;
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

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    @GetMapping({"", "/"}) // Handle both /calendar and /calendar/ requests
    public String showWeekCalendar(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                   Model model) {
        // If no date provided, use current date
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Get the first day of the week (Monday) for the given date
        LocalDate firstDayOfWeek = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Generate list of dates for the whole week
        List<WeekDay> weekDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = firstDayOfWeek.plusDays(i);
            weekDays.add(new WeekDay(
                    currentDate,
                    currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    currentDate.equals(LocalDate.now())
            ));
        }

        // Calculate previous and next week dates for navigation
        LocalDate previousWeek = firstDayOfWeek.minusWeeks(1);
        LocalDate nextWeek = firstDayOfWeek.plusWeeks(1);

        model.addAttribute("weekDays", weekDays);
        model.addAttribute("currentWeekStart", firstDayOfWeek);
        model.addAttribute("previousWeek", previousWeek);
        model.addAttribute("nextWeek", nextWeek);
        model.addAttribute("selectedDate", targetDate);

        return "calendar";
    }
}