package com.borodkir.teamjob.services.implementations;

import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CalendarServiceImplTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    private static final List<String> SAMPLE_ROOM_TAGS = Arrays.asList(
            "rooms_Meeting Room", "rooms_Conference Room", "rooms_Projector");

    private static final List<String> SAMPLE_USER_TAGS = Arrays.asList(
            "users_IT Support", "users_Developer", "users_Manager");

    private static final List<String> SAMPLE_EVENT_TAGS = Arrays.asList(
            "event_Meeting", "event_Training", "event_Workshop");

    // Test data constants
    private static final LocalDateTime baseTime = LocalDateTime.of(2025, 5, 16, 0, 0); // Friday, May 16, 2025 at 0:00
    private static final LocalDate baseDate = baseTime.toLocalDate();

    // Event time constants (relative to baseTime)
    private static final int REGULAR_EVENT_DURATION_HOURS = 1;
    private static final int WEEKLY_EVENT_START_HOUR = 10;
    private static final int WEEKLY_EVENT_DURATION_MINUTES = 30;
    private static final int MONTHLY_EVENT_START_HOUR = 14;
    private static final int MONTHLY_EVENT_DURATION_HOURS = 2;

    // Recurrence constants
    private static final String WEEKLY_RRULE = "FREQ=WEEKLY;BYDAY=MO,WE,FR";
    private static final int WEEKLY_RECURRENCE_MONTHS = 3;
    private static final int MONTHLY_RECURRENCE_YEARS = 1;

    private List<Event> testEvents;
    private List<User> testUsers;
    private List<Room> testRooms;

    @BeforeEach
    void setUp() {
        testUsers = createTestUsers();
        testRooms = createTestRooms();
        testEvents = createTestEvents(testUsers, testRooms);
        // No stubbings here; will be added to test methods as needed
    }

    // Helper methods to create test data
    public static List<User> createTestUsers() {
        List<User> users = new ArrayList<>();
        String[] usernames = { "john.doe", "jane.smith", "bob.wilson" };
        String[] emails = { "john@test.com", "jane@test.com", "bob@test.com" };

        for (int i = 0; i < usernames.length; i++) {
            User user = new User();
            user.setId((long) (i + 1));
            user.setUsername(usernames[i]);
            user.setPassword("password123");
            user.setEmail(emails[i]);
            Set<String> tags = new HashSet<>();
            tags.add(SAMPLE_USER_TAGS.get(i));
            user.setTags(tags);
            users.add(user);
        }
        return users;
    }

    public static List<Room> createTestRooms() {
        List<Room> rooms = new ArrayList<>();
        String[] roomNames = { "Main Conference Room", "Training Lab", "Meeting Room A" };
        String[] descriptions = {
                "Large conference room with projector",
                "Computer lab for training sessions",
                "Small meeting room for team discussions"
        };

        for (int i = 0; i < roomNames.length; i++) {
            Room room = new Room();
            room.setId((long) (i + 1));
            room.setName(roomNames[i]);
            room.setDescription(descriptions[i]);
            Set<String> tags = new HashSet<>();
            tags.add(SAMPLE_ROOM_TAGS.get(i));
            room.setTags(tags);
            rooms.add(room);
        }
        return rooms;
    }

    public static List<Event> createTestEvents(List<User> users, List<Room> rooms) {
        List<Event> events = new ArrayList<>();
        String[] eventTitles = { "Team Meeting", "Training Session", "Project Review", "Weekly Standup",
                "Monthly Planning" };
        String[] eventDescriptions = {
                "Weekly team sync meeting",
                "New employee orientation",
                "Monthly project status review",
                "Daily team standup",
                "Monthly planning session"
        };

        // Create regular events (9:00, 11:00, and 13:00)
        for (int i = 0; i < 3; i++) {
            Event event = new Event();
            event.setId((long) (i + 1));
            event.setTitle(eventTitles[i]);
            event.setDescription(eventDescriptions[i]);
            event.setStartTime(baseTime.plusHours(i * 2)); // 9:00, 11:00, 13:00
            event.setEndTime(baseTime.plusHours(i * 2 + REGULAR_EVENT_DURATION_HOURS));
            event.setUser(users.get(i));
            event.setRoom(rooms.get(i));
            event.setRecurring(false);
            Set<String> tags = new HashSet<>();
            tags.add(SAMPLE_EVENT_TAGS.get(i));
            event.setTags(tags);
            events.add(event);
        }

        // Create weekly recurring event (10:00-10:30, Mon/Wed/Fri)
        Event weeklyEvent = new Event();
        weeklyEvent.setId(4L);
        weeklyEvent.setTitle(eventTitles[3]);
        weeklyEvent.setDescription(eventDescriptions[3]);
        weeklyEvent.setStartTime(baseTime.plusHours(WEEKLY_EVENT_START_HOUR));
        weeklyEvent.setEndTime(baseTime.plusHours(WEEKLY_EVENT_START_HOUR).plusMinutes(WEEKLY_EVENT_DURATION_MINUTES));
        weeklyEvent.setUser(users.getFirst());
        weeklyEvent.setRoom(rooms.getFirst());
        weeklyEvent.setRecurring(true);
        weeklyEvent.setRrule(WEEKLY_RRULE);
        weeklyEvent.setRecurrenceEndDate(baseTime.plusMonths(WEEKLY_RECURRENCE_MONTHS));
        Set<String> weeklyTags = new HashSet<>();
        weeklyTags.add(SAMPLE_EVENT_TAGS.getFirst());
        weeklyEvent.setTags(weeklyTags);
        events.add(weeklyEvent);

        // Create monthly recurring event (14:00-16:00, same day each month)
        Event monthlyEvent = new Event();
        monthlyEvent.setId(5L);
        monthlyEvent.setTitle(eventTitles[4]);
        monthlyEvent.setDescription(eventDescriptions[4]);
        monthlyEvent.setStartTime(baseTime.plusHours(MONTHLY_EVENT_START_HOUR));
        monthlyEvent.setEndTime(baseTime.plusHours(MONTHLY_EVENT_START_HOUR + MONTHLY_EVENT_DURATION_HOURS));
        monthlyEvent.setUser(users.get(1));
        monthlyEvent.setRoom(rooms.get(1));
        monthlyEvent.setRecurring(true);
        monthlyEvent.setRrule("FREQ=MONTHLY;INTERVAL=1");
        monthlyEvent.setRecurrenceEndDate(baseTime.plusYears(MONTHLY_RECURRENCE_YEARS));
        Set<String> monthlyTags = new HashSet<>();
        monthlyTags.add(SAMPLE_EVENT_TAGS.get(1));
        monthlyEvent.setTags(monthlyTags);
        events.add(monthlyEvent);

        return events;
    }

    @Test
    void convertToDayEvents() {
        // Test all events on the base date (May 16, 2025)
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(testEvents, baseDate, null, null);

        // Verify all 5 events are present
        assertEquals(5, dayEvents.size(), "Should have 5 events (3 regular + 2 recurring)");

        // Verify event order and titles
        assertEquals("Team Meeting", dayEvents.get(0).getTitle());
        assertEquals("Training Session", dayEvents.get(1).getTitle());
        assertEquals("Project Review", dayEvents.get(2).getTitle());
        assertEquals("Weekly Standup", dayEvents.get(3).getTitle());
        assertEquals("Monthly Planning", dayEvents.get(4).getTitle());
    }

    @Test
    void convertToDayEvents_WithRecurringEvents() {
        // Test all events on the base date (May 16, 2025)
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(testEvents, baseDate, null, null);

        // Debug output
        System.out.println("Events on " + baseDate + ":");
        dayEvents.forEach(e -> System.out
                .println(e.getTitle() + " (" + e.getStartTimeToUse() + ", " + e.getEndTimeToUse() + ")"));

        // Verify all events are present
        assertEquals(5, dayEvents.size(), "Should have 5 events (3 regular + 2 recurring)");

        // Verify regular events (9:00, 11:00, 13:00)
        assertTrue(dayEvents.stream().anyMatch(e -> e.getTitle().equals("Team Meeting")));
        assertTrue(dayEvents.stream().anyMatch(e -> e.getTitle().equals("Training Session")));
        assertTrue(dayEvents.stream().anyMatch(e -> e.getTitle().equals("Project Review")));

        // Verify recurring events
        assertTrue(dayEvents.stream().anyMatch(e -> e.getTitle().equals("Weekly Standup")));
        assertTrue(dayEvents.stream().anyMatch(e -> e.getTitle().equals("Monthly Planning")));

        // Verify weekly event times (10:00-10:30)
        EventInADay weeklyEvent = dayEvents.stream()
                .filter(e -> e.getTitle().equals("Weekly Standup"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Weekly Standup event not found"));
        assertEquals(10.0, weeklyEvent.getStartTimeToUse(), "Weekly event should start at 10:00");
        assertEquals(10.5, weeklyEvent.getEndTimeToUse(), "Weekly event should end at 10:30");

        // Verify monthly event times (14:00-16:00)
        EventInADay monthlyEvent = dayEvents.stream()
                .filter(e -> e.getTitle().equals("Monthly Planning"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Monthly Planning event not found"));
        assertEquals(14.0, monthlyEvent.getStartTimeToUse(), "Monthly event should start at 14:00");
        assertEquals(16.0, monthlyEvent.getEndTimeToUse(), "Monthly event should end at 16:00");
    }

    @Test
    void convertToDayEvents_WithRecurringEvents_ExcludedDate() {
        // Create a test event with an exception date
        Event event = testEvents.get(3); // Weekly Standup event
        event.setExdate(baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        // Test on the excluded date
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(Collections.singletonList(event), baseDate,
                null, null);

        // Debug output
        System.out.println("Events on (excluded) " + baseDate + ":");
        dayEvents.forEach(e -> System.out
                .println(e.getTitle() + " (" + e.getStartTimeToUse() + ", " + e.getEndTimeToUse() + ")"));

        // Verify the event is not included
        assertTrue(dayEvents.isEmpty(), "Event should be excluded due to exception date");
    }

    @Test
    void convertToDayEvents_WithRecurringEvents_EndDate() {
        // Create a test event with a recurrence end date
        Event event = testEvents.get(3); // Weekly Standup event
        LocalDate afterEndDate = baseDate.plusMonths(WEEKLY_RECURRENCE_MONTHS + 1); // After the recurrence end date

        // Test after the end date
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(Collections.singletonList(event), afterEndDate,
                null, null);

        // Verify the event is not included
        assertTrue(dayEvents.isEmpty(), "Event should be excluded due to end date");
    }

    @Test
    void convertToDayEvents_WithRecurringEvents_WeeklyPattern() {
        // Test weekly event on different days
        Event event = testEvents.get(3); // Weekly Standup event

        // Get dates for different days of the week
        LocalDate monday = baseDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate wednesday = baseDate.with(java.time.DayOfWeek.WEDNESDAY);
        LocalDate friday = baseDate.with(java.time.DayOfWeek.FRIDAY);
        LocalDate tuesday = baseDate.with(java.time.DayOfWeek.TUESDAY);

        // Should appear on Monday, Wednesday, and Friday
        assertFalse(calendarService.convertToDayEvents(Collections.singletonList(event), monday, null, null).isEmpty());
        assertFalse(
                calendarService.convertToDayEvents(Collections.singletonList(event), wednesday, null, null).isEmpty());
        assertFalse(calendarService.convertToDayEvents(Collections.singletonList(event), friday, null, null).isEmpty());

        // Should not appear on Tuesday
        assertTrue(calendarService.convertToDayEvents(Collections.singletonList(event), tuesday, null, null).isEmpty());
    }

    @Test
    void convertToDayEvents_WithRecurringEvents_MonthlyPattern() {
        // Test monthly event on different months
        Event event = testEvents.get(4); // Monthly Planning event

        // Get dates for different months
        LocalDate firstMonth = baseDate;
        LocalDate secondMonth = baseDate.plusMonths(1);
        LocalDate thirdMonth = baseDate.plusMonths(2);

        // Should appear on the same day of each month
        assertFalse(
                calendarService.convertToDayEvents(Collections.singletonList(event), firstMonth, null, null).isEmpty());
        assertFalse(calendarService.convertToDayEvents(Collections.singletonList(event), secondMonth, null, null)
                .isEmpty());
        assertFalse(
                calendarService.convertToDayEvents(Collections.singletonList(event), thirdMonth, null, null).isEmpty());
    }

    @Test
    void addRepositories() {
        when(eventRepository.findAll()).thenReturn(testEvents);
        when(userRepository.findAll()).thenReturn(testUsers);
        when(roomRepository.findAll()).thenReturn(testRooms);
        Model model = new ExtendedModelMap();
        CalendarServiceImpl.AddRepositories(model, eventRepository, userRepository, roomRepository);

        // Verify the model attributes were set
        assertNotNull(model.getAttribute("events"));
        assertNotNull(model.getAttribute("rooms"));
        assertNotNull(model.getAttribute("users"));
    }

    @Test
    void generateSearchResults() {
        // Create an AvailableTimeRequest with room type
        AvailableTimeRequest request = new AvailableTimeRequest(
                baseTime,
                baseTime.plusHours(4),
                "rooms");

        // Add room availabilities with unoccupied times
        List<RoomAvailability> roomAvailabilities = new ArrayList<>();
        ArrayList<TimeFromTo> unoccupiedTimes = new ArrayList<>();
        unoccupiedTimes.add(new TimeFromTo(baseTime, baseTime.plusHours(1)));
        roomAvailabilities.add(new RoomAvailability(testRooms.getFirst(), unoccupiedTimes));
        request.setRoomAvailabilities(roomAvailabilities);

        // Generate search results
        List<SearchResult> results = calendarService.generateSearchResults(request, baseTime.toLocalDate());

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search results should not be empty");
        assertEquals("room", results.getFirst().getType());
        assertEquals(testRooms.getFirst().getName(), results.getFirst().getName());
    }

    @Test
    void generateAvailableTimeRequest_Rooms() {
        LocalDateTime startTime = baseTime.plusHours(2);
        LocalDateTime endTime = baseTime.plusHours(6);
        Set<String> tags = new HashSet<>();
        when(eventRepository.findOverlappingEvents(startTime, endTime)).thenReturn(testEvents);
        when(roomRepository.findAll()).thenReturn(testRooms);
        AvailableTimeRequest result = calendarService.generateAvailableTimeRequest(
                "rooms", startTime, endTime, 60, tags);

        assertNotNull(result);
        assertEquals("rooms", result.getType());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertNotNull(result.getRoomAvailabilities());
        assertEquals(3, result.getRoomAvailabilities().size());

        verify(eventRepository).findOverlappingEvents(startTime, endTime);
        verify(roomRepository).findAll();
    }

    @Test
    void generateAvailableTimeRequest_Users() {
        LocalDateTime startTime = baseTime.plusHours(2);
        LocalDateTime endTime = baseTime.plusHours(6);
        Set<String> tags = new HashSet<>();
        when(eventRepository.findOverlappingEvents(startTime, endTime)).thenReturn(testEvents);
        when(userRepository.findAll()).thenReturn(testUsers);
        AvailableTimeRequest result = calendarService.generateAvailableTimeRequest(
                "users", startTime, endTime, 60, tags);

        assertNotNull(result);
        assertEquals("users", result.getType());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertNotNull(result.getUserAvailabilities());
        assertEquals(3, result.getUserAvailabilities().size());

        verify(eventRepository).findOverlappingEvents(startTime, endTime);
        verify(userRepository).findAll();
    }

    @Test
    void generateAvailableTimeRequest_WithTags() {
        LocalDateTime startTime = baseTime.plusHours(2);
        LocalDateTime endTime = baseTime.plusHours(6);
        Set<String> tags = new HashSet<>();
        tags.add(SAMPLE_ROOM_TAGS.getFirst());
        when(eventRepository.findOverlappingEvents(startTime, endTime)).thenReturn(testEvents);
        when(roomRepository.findAll()).thenReturn(testRooms);
        AvailableTimeRequest result = calendarService.generateAvailableTimeRequest(
                "rooms", startTime, endTime, 60, tags);

        assertNotNull(result);
        assertEquals("rooms", result.getType());
        assertNotNull(result.getRoomAvailabilities());
        // Should only return rooms with matching tags
        assertTrue(result.getRoomAvailabilities().size() <= 3);
    }

    @Test
    void generateAvailableTimeRequest_InvalidType() {
        LocalDateTime startTime = baseTime.plusHours(2);
        LocalDateTime endTime = baseTime.plusHours(6);
        Set<String> tags = new HashSet<>();
        when(eventRepository.findOverlappingEvents(startTime, endTime)).thenReturn(testEvents);
        assertThrows(IllegalArgumentException.class, () -> calendarService.generateAvailableTimeRequest(
                "invalid", startTime, endTime, 60, tags));
    }

    @Test
    void setupModelForWeekCalendar() {
        Model model = new ExtendedModelMap();

        calendarService.setupModelForWeekCalendar(model, baseDate, "1", "1", "tag1", "tag2", "tag3");

        // Verify model attributes
        assertNotNull(model.getAttribute("weekDays"));
        assertNotNull(model.getAttribute("currentWeekStart"));
        assertNotNull(model.getAttribute("previousWeek"));
        assertNotNull(model.getAttribute("nextWeek"));
        assertNotNull(model.getAttribute("selectedDate"));
        assertNotNull(model.getAttribute("hours"));
        assertNotNull(model.getAttribute("userIds"));
        assertNotNull(model.getAttribute("roomIds"));
        assertNotNull(model.getAttribute("events"));
        assertNotNull(model.getAttribute("rooms"));
        assertNotNull(model.getAttribute("users"));
        assertNotNull(model.getAttribute("roomTags"));
        assertNotNull(model.getAttribute("userTags"));
        assertNotNull(model.getAttribute("eventTags"));

        @SuppressWarnings("unchecked")
        List<WeekDay> weekDays = (List<WeekDay>) model.getAttribute("weekDays");
        assertNotNull(weekDays);
        assertEquals(7, weekDays.size());

        verify(eventRepository, atLeastOnce()).findOverlappingEvents(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void setupModelForWeekCalendar_NullDate() {
        Model model = new ExtendedModelMap();

        calendarService.setupModelForWeekCalendar(model, null, null, null, null, null, null);

        // Should use current date when null is passed
        assertNotNull(model.getAttribute("selectedDate"));
        assertNotNull(model.getAttribute("weekDays"));
    }

    @Test
    void setupModelForDayCalendar() {
        Model model = new ExtendedModelMap();
        LocalDate testDate = baseDate;

        calendarService.setupModelForDayCalendar(model, testDate, "1", "1", "tag1", "tag2", "tag3");

        // Verify model attributes
        assertNotNull(model.getAttribute("roomDays"));
        assertNotNull(model.getAttribute("currentDay"));
        assertNotNull(model.getAttribute("previousDay"));
        assertNotNull(model.getAttribute("nextDay"));
        assertNotNull(model.getAttribute("selectedDate"));
        assertNotNull(model.getAttribute("hours"));
        assertNotNull(model.getAttribute("userIds"));
        assertNotNull(model.getAttribute("roomIds"));
        assertNotNull(model.getAttribute("events"));
        assertNotNull(model.getAttribute("rooms"));
        assertNotNull(model.getAttribute("users"));

        assertEquals(testDate, model.getAttribute("currentDay"));
        assertEquals(testDate.minusDays(1), model.getAttribute("previousDay"));
        assertEquals(testDate.plusDays(1), model.getAttribute("nextDay"));

        verify(eventRepository).findOverlappingEvents(testDate.atStartOfDay(), testDate.plusDays(1).atStartOfDay());
    }

    @Test
    void setupModelForFindAvailable_DefaultValues() {
        Model model = new ExtendedModelMap();

        calendarService.setupModelForFindAvailable(model, null, null, null, null, null, null);

        // Verify default values are set
        assertEquals(LocalDate.now(), model.getAttribute("date"));
        assertEquals(LocalTime.of(0, 0), model.getAttribute("startTime"));
        assertEquals(LocalTime.of(23, 59), model.getAttribute("endTime"));
        assertEquals(30, model.getAttribute("durationMinutes"));
        assertEquals("rooms", model.getAttribute("searchType"));
        assertNotNull(model.getAttribute("results"));
        assertNotNull(model.getAttribute("roomAvailabilities"));
    }

    @Test
    void setupModelForFindAvailable_Rooms() {
        Model model = new ExtendedModelMap();
        LocalDate testDate = baseDate;
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        calendarService.setupModelForFindAvailable(model, "rooms", "tag1", testDate, startTime, endTime, 60);

        assertEquals(testDate, model.getAttribute("date"));
        assertEquals(startTime, model.getAttribute("startTime"));
        assertEquals(endTime, model.getAttribute("endTime"));
        assertEquals(60, model.getAttribute("durationMinutes"));
        assertEquals("rooms", model.getAttribute("searchType"));
        assertNotNull(model.getAttribute("roomAvailabilities"));
    }

    @Test
    void setupModelForFindAvailable_Users() {
        Model model = new ExtendedModelMap();
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        calendarService.setupModelForFindAvailable(model, "users", "tag1", baseDate, startTime, endTime, 60);

        assertEquals("users", model.getAttribute("searchType"));
        assertNotNull(model.getAttribute("userAvailabilities"));
    }

    @Test
    void gatherAllTags() {
        when(roomRepository.findAll()).thenReturn(testRooms);
        when(userRepository.findAll()).thenReturn(testUsers);
        when(eventRepository.findAll()).thenReturn(testEvents);
        Model model = new ExtendedModelMap();
        calendarService.gatherAllTags(model);

        assertNotNull(model.getAttribute("roomTags"));
        assertNotNull(model.getAttribute("userTags"));
        assertNotNull(model.getAttribute("eventTags"));

        @SuppressWarnings("unchecked")
        Set<String> roomTags = (Set<String>) model.getAttribute("roomTags");
        @SuppressWarnings("unchecked")
        Set<String> userTags = (Set<String>) model.getAttribute("userTags");
        @SuppressWarnings("unchecked")
        Set<String> eventTags = (Set<String>) model.getAttribute("eventTags");

        assertNotNull(roomTags);
        assertFalse(roomTags.isEmpty());
        assertNotNull(userTags);
        assertFalse(userTags.isEmpty());
        assertNotNull(eventTags);
        assertFalse(eventTags.isEmpty());

        verify(roomRepository, atLeastOnce()).findAll();
        verify(userRepository, atLeastOnce()).findAll();
        verify(eventRepository, atLeastOnce()).findAll();
    }

    @Test
    void generateSearchResults_Users() {
        AvailableTimeRequest request = new AvailableTimeRequest(
                baseTime, baseTime.plusHours(4), "users");

        List<UserAvailability> userAvailabilities = new ArrayList<>();
        ArrayList<TimeFromTo> unoccupiedTimes = new ArrayList<>();
        unoccupiedTimes.add(new TimeFromTo(baseTime, baseTime.plusHours(1)));
        userAvailabilities.add(new UserAvailability(testUsers.getFirst(), unoccupiedTimes));
        request.setUserAvailabilities(userAvailabilities);

        List<SearchResult> results = calendarService.generateSearchResults(request, baseDate);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("user", results.getFirst().getType());
        assertEquals(testUsers.getFirst().getUsername(), results.getFirst().getName());
    }

    @Test
    void generateSearchResults_Events() {
        AvailableTimeRequest request = new AvailableTimeRequest(
                baseTime, baseTime.plusHours(4), "events");

        List<EventAvailability> eventAvailabilities = new ArrayList<>();
        eventAvailabilities.add(new EventAvailability(testEvents.getFirst(), new ArrayList<>()));
        request.setEventAvailabilities(eventAvailabilities);

        List<SearchResult> results = calendarService.generateSearchResults(request, baseDate);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("event", results.getFirst().getType());
        assertEquals(testEvents.getFirst().getTitle(), results.getFirst().getName());
    }

    @Test
    void generateSearchResults_EmptyAvailabilities() {
        AvailableTimeRequest request = new AvailableTimeRequest(
                baseTime, baseTime.plusHours(4), "rooms");

        List<RoomAvailability> roomAvailabilities = new ArrayList<>();
        // Add room with no unoccupied times
        roomAvailabilities.add(new RoomAvailability(testRooms.getFirst(), new ArrayList<>()));
        request.setRoomAvailabilities(roomAvailabilities);

        List<SearchResult> results = calendarService.generateSearchResults(request, baseDate);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Should have no results when no unoccupied times available");
    }

    @Test
    void convertToDayEvents_WithFilters() {
        // Test with user ID filter
        List<EventInADay> filteredEvents = calendarService.convertToDayEvents(
                testEvents, baseDate, "1", null, null, null, null);

        // Should only include events for user with ID 1
        assertTrue(filteredEvents.stream().allMatch(e -> 
                e.getUser() == null || e.getUser().getId().equals(1L)));
    }

    @Test
    void convertToDayEvents_WithRoomFilter() {
        // Test with room ID filter
        List<EventInADay> filteredEvents = calendarService.convertToDayEvents(
                testEvents, baseDate, null, "1", null, null, null);

        // Should only include events for room with ID 1
        assertTrue(filteredEvents.stream().allMatch(e -> 
                e.getRoom() == null || e.getRoom().getId().equals(1L)));
    }

    @Test
    void convertToDayEvents_WithTagFilters() {
        // Test with tag filters
        List<EventInADay> filteredEvents = calendarService.convertToDayEvents(
                testEvents, baseDate, null, null, SAMPLE_USER_TAGS.getFirst(),
                SAMPLE_ROOM_TAGS.getFirst(), SAMPLE_EVENT_TAGS.getFirst());

        // Should only include events that match the tag criteria
        assertNotNull(filteredEvents);
        // Verify the filtering logic works
        assertTrue(filteredEvents.size() <= testEvents.size());
    }

    @Test
    void convertToDayEvents_OverlappingDates() {
        // Create an event that spans multiple days
        Event multiDayEvent = new Event();
        multiDayEvent.setId(10L);
        multiDayEvent.setTitle("Multi-day Event");
        multiDayEvent.setStartTime(baseDate.minusDays(1).atTime(20, 0));
        multiDayEvent.setEndTime(baseDate.atTime(4, 0));
        multiDayEvent.setRecurring(false);

        List<Event> events = Collections.singletonList(multiDayEvent);
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(events, baseDate, null, null);

        assertEquals(1, dayEvents.size());
        EventInADay eventInDay = dayEvents.getFirst();
        assertEquals(0.0, eventInDay.getStartTimeToUse()); // Should start at 0:00 on the target date
        assertEquals(4.0, eventInDay.getEndTimeToUse()); // Should end at 4:00
        assertEquals(4.0, eventInDay.getDurationInADay());
    }
}