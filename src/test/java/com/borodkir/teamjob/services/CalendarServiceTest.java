package com.borodkir.teamjob.services;

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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private CalendarService calendarService;

    private static final List<String> SAMPLE_ROOM_TAGS = Arrays.asList(
            "rooms_Meeting Room", "rooms_Conference Room", "rooms_Projector"
    );

    private static final List<String> SAMPLE_USER_TAGS = Arrays.asList(
            "users_IT Support", "users_Developer", "users_Manager"
    );

    private static final List<String> SAMPLE_EVENT_TAGS = Arrays.asList(
            "event_Meeting", "event_Training", "event_Workshop"
    );

    private final LocalDateTime baseTime = LocalDateTime.now()
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

    private List<Event> testEvents;
    private List<User> testUsers;
    private List<Room> testRooms;

    @BeforeEach
    void setUp() {
        // Create test data
        testUsers = createTestUsers();
        testRooms = createTestRooms();
        testEvents = createTestEvents(testUsers, testRooms);
    }

    // Helper methods to create test data
    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();
        String[] usernames = {"john.doe", "jane.smith", "bob.wilson"};
        String[] emails = {"john@test.com", "jane@test.com", "bob@test.com"};

        for (int i = 0; i < usernames.length; i++) {
            User user = new User();
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

    private List<Room> createTestRooms() {
        List<Room> rooms = new ArrayList<>();
        String[] roomNames = {"Main Conference Room", "Training Lab", "Meeting Room A"};
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

    private List<Event> createTestEvents(List<User> users, List<Room> rooms) {
        List<Event> events = new ArrayList<>();
        String[] eventTitles = {"Team Meeting", "Training Session", "Project Review"};
        String[] eventDescriptions = {
                "Weekly team sync meeting",
                "New employee orientation",
                "Monthly project status review"
        };

        for (int i = 0; i < eventTitles.length; i++) {
            Event event = new Event();
            event.setId((long) (i + 1));
            event.setTitle(eventTitles[i]);
            event.setDescription(eventDescriptions[i]);
            event.setStartTime(baseTime.plusHours(i * 2));
            event.setEndTime(baseTime.plusHours(i * 2 + 1));
            event.setUser(users.get(i));
            event.setRoom(rooms.get(i));
            Set<String> tags = new HashSet<>();
            tags.add(SAMPLE_EVENT_TAGS.get(i));
            event.setTags(tags);
            events.add(event);
        }
        return events;
    }

    @Test
    void generateSearchResults() {
        // Create an AvailableTimeRequest with room type
        AvailableTimeRequest request = new AvailableTimeRequest(
                baseTime,
                baseTime.plusHours(4),
                "rooms"
        );
        
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
    void convertToDayEvents() {
        // Convert to day events
        List<EventInADay> dayEvents = calendarService.convertToDayEvents(testEvents, baseTime.toLocalDate(), null, null);

        // Verify results
        assertEquals(3, dayEvents.size());
        assertEquals("Team Meeting", dayEvents.get(0).getTitle());
        assertEquals("Training Session", dayEvents.get(1).getTitle());
        assertEquals("Project Review", dayEvents.get(2).getTitle());
    }

    @Test
    void filterEvents() {
        // Create a test event
        Event event = testEvents.getFirst(); // Team Meeting event
        LocalDate testDate = baseTime.toLocalDate();
        
        // Test with no filters
        boolean result = calendarService.filterEvents(event, testDate, Optional.empty(), Optional.empty());
        assertTrue(result);
        
        // Test with user filter
        String userIds = String.valueOf(event.getUser().getId());
        result = calendarService.filterEvents(event, testDate, Optional.of(userIds), Optional.empty());
        assertTrue(result);
        
        // Test with room filter
        String roomIds = String.valueOf(event.getRoom().getId());
        result = calendarService.filterEvents(event, testDate, Optional.empty(), Optional.of(roomIds));
        assertTrue(result);
        
        // Test with non-matching filters
        result = calendarService.filterEvents(event, testDate, Optional.of("999"), Optional.of("999"));
        assertFalse(result);
    }

    @Test
    void calculateHoursInDay() {
        Event event = testEvents.getFirst(); // Team Meeting event
        double hours = calendarService.calculateHoursInDay(event, baseTime.toLocalDate());
        
        assertEquals(1.0, hours, 0.001); // Event duration is 1 hour, using delta for double comparison
    }

    @Test
    void addRepositories() {
        // Set up mocks
        when(eventRepository.findAll()).thenReturn(testEvents);
        when(userRepository.findAll()).thenReturn(testUsers);
        when(roomRepository.findAll()).thenReturn(testRooms);
        
        Model model = new ExtendedModelMap();
        CalendarService.AddRepositories(model, eventRepository, userRepository, roomRepository);
        
        // Verify the model attributes were set
        assertNotNull(model.getAttribute("events"));
        assertNotNull(model.getAttribute("rooms"));
        assertNotNull(model.getAttribute("users"));
        
        // Verify the mocks were called
        verify(eventRepository).findAll();
        verify(userRepository).findAll();
        verify(roomRepository).findAll();
    }
}