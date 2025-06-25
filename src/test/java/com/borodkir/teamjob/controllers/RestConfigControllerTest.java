package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.UserDetailsImpl;
import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoleRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import com.borodkir.teamjob.services.implementations.DefaultValueServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RestConfigControllerTest {

    @Value("${testing.app.secret}")
    private String secret;

    @Value("${testing.app.lifetime}")
    private int lifetime;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Room testRoom;
    private User testUser;
    private ObjectMapper objectMapper;
    private String jwtToken;
    private static final String TEST_PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Clear repositories
        eventRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        // Create test room
        testRoom = new Room();
        testRoom.setName("Test Room");
        testRoom.setDescription("Test Room Description");
        testRoom = roomRepository.save(testRoom);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        Role userRole = roleRepository.findByName(DefaultValueServiceImpl.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        testUser.addRole(userRole);
        testUser = userRepository.save(testUser);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Get JWT token
        jwtToken = getJwtToken();
    }

    private String getJwtToken() {
        UserDetailsImpl userDetails = (UserDetailsImpl) UserDetailsImpl.build(testUser);
        LocalDateTime now = LocalDateTime.now();
        Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expiration = Date.from(now.plusSeconds(lifetime).atZone(ZoneId.systemDefault()).toInstant());

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Test
    void testCreateRegularEvent() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Test Event");
        request.setDescription("Test Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(false);

        // Send request
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Test Event", savedEvent.getTitle(), "Expected title: 'Test Event', but got: '" + savedEvent.getTitle() + "'");
        assertFalse(savedEvent.isRecurring(), "Expected isRecurring to be false, but got: " + savedEvent.isRecurring());
        assertNull(savedEvent.getRrule(), "Expected rrule to be null, but got: '" + savedEvent.getRrule() + "'");
    }

    @Test
    void testCreateDailyRecurringEvent() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Daily Recurring Event");
        request.setDescription("Daily Recurring Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(true);
        request.setRrule("FREQ=DAILY");
        request.setInterval("1");
        request.setRecurrenceEndDate(LocalDateTime.now().plusDays(7));

        // Send request
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Daily Recurring Event", savedEvent.getTitle(), "Expected title: 'Daily Recurring Event', but got: '" + savedEvent.getTitle() + "'");
        assertTrue(savedEvent.isRecurring(), "Expected isRecurring to be true, but got: " + savedEvent.isRecurring());
        assertTrue(savedEvent.getRrule().startsWith("FREQ=DAILY;INTERVAL=1"), "Expected rrule to start with 'FREQ=DAILY;INTERVAL=1', but got: '" + savedEvent.getRrule() + "'");
        assertNotNull(savedEvent.getRecurrenceEndDate(), "Expected recurrenceEndDate to be not null");
    }

    @Test
    void testCreateWeeklyRecurringEvent() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Weekly Recurring Event");
        request.setDescription("Weekly Recurring Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(true);
        request.setRrule("FREQ=WEEKLY");
        request.setInterval("1");
        request.setWeekdays(Arrays.asList("MO", "WE", "FR"));
        request.setRecurrenceEndDate(LocalDateTime.now().plusWeeks(4));

        // Send request
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Weekly Recurring Event", savedEvent.getTitle(), "Expected title: 'Weekly Recurring Event', but got: '" + savedEvent.getTitle() + "'");
        assertTrue(savedEvent.isRecurring(), "Expected isRecurring to be true, but got: " + savedEvent.isRecurring());
        assertTrue(savedEvent.getRrule().startsWith("FREQ=WEEKLY;INTERVAL=1") && savedEvent.getRrule().contains("BYDAY=MO,WE,FR"), 
            "Expected rrule to start with 'FREQ=WEEKLY;INTERVAL=1' and contain 'BYDAY=MO,WE,FR', but got: '" + savedEvent.getRrule() + "'");
        assertNotNull(savedEvent.getRecurrenceEndDate(), "Expected recurrenceEndDate to be not null");
    }

    @Test
    void testCreateMonthlyRecurringEvent() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Monthly Recurring Event");
        request.setDescription("Monthly Recurring Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(true);
        request.setRrule("FREQ=MONTHLY");
        request.setInterval("1");
        request.setRecurrenceEndDate(LocalDateTime.now().plusMonths(6));

        // Send request
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Monthly Recurring Event", savedEvent.getTitle(), "Expected title: 'Monthly Recurring Event', but got: '" + savedEvent.getTitle() + "'");
        assertTrue(savedEvent.isRecurring(), "Expected isRecurring to be true, but got: " + savedEvent.isRecurring());
        assertTrue(savedEvent.getRrule().startsWith("FREQ=MONTHLY;INTERVAL=1"), "Expected rrule to start with 'FREQ=MONTHLY;INTERVAL=1', but got: '" + savedEvent.getRrule() + "'");
        assertNotNull(savedEvent.getRecurrenceEndDate(), "Expected recurrenceEndDate to be not null");
    }

    @Test
    void testCreateYearlyRecurringEvent() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Yearly Recurring Event");
        request.setDescription("Yearly Recurring Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(true);
        request.setRrule("FREQ=YEARLY");
        request.setInterval("1");
        request.setRecurrenceEndDate(LocalDateTime.now().plusYears(2));

        // Send request
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Yearly Recurring Event", savedEvent.getTitle(), "Expected title: 'Yearly Recurring Event', but got: '" + savedEvent.getTitle() + "'");
        assertTrue(savedEvent.isRecurring(), "Expected isRecurring to be true, but got: " + savedEvent.isRecurring());
        assertTrue(savedEvent.getRrule().startsWith("FREQ=YEARLY;INTERVAL=1"), "Expected rrule to start with 'FREQ=YEARLY;INTERVAL=1', but got: '" + savedEvent.getRrule() + "'");
        assertNotNull(savedEvent.getRecurrenceEndDate(), "Expected recurrenceEndDate to be not null");
    }

    @Test
    void testCreateInvalidRecurringEvent() throws Exception {
        // Create event request with missing rrule
        EventRequest request = new EventRequest();
        request.setTitle("Invalid Recurring Event");
        request.setDescription("Invalid Recurring Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(true);
        // No rrule set

        // Send request
        mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify that the event was saved as non-recurring
        List<Event> events = eventRepository.findAll();
        assertEquals(1, events.size(), "Expected 1 event, but got: " + events.size());
        Event savedEvent = events.getFirst();
        assertFalse(savedEvent.isRecurring(), "Expected isRecurring to be false, but got: " + savedEvent.isRecurring());
        assertNull(savedEvent.getRrule(), "Expected rrule to be null, but got: '" + savedEvent.getRrule() + "'");
    }
} 