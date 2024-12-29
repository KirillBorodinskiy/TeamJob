package io.datajek.spring.basics.teamjob;


import java.time.LocalDate;

public class WeekDay {
    private final LocalDate date;
    private final String dayName;
    private final boolean isToday;
    private String[] events;

    public WeekDay(LocalDate currentDate, String displayName, boolean equals) {
        this.date = currentDate;
        this.dayName = displayName;
        this.isToday = equals;
    }
    public String[] getEvents() {
        return events;
    }
    public void setEvents(String[] events) {
        this.events = events;
    }
    public LocalDate getDate() {
        return date;
    }
    public String getDayName() {
        return dayName;
    }
    public boolean isToday() {
        return isToday;
    }
    @Override
    public String toString() {
        return "WeekDay{" +
            "date=" + date +
            ", dayName='" + dayName + '\'' +
            ", isToday=" + isToday +
            '}';
    }
}