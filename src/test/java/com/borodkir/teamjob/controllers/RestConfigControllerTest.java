package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.DefaultValueService;
import com.borodkir.teamjob.UserDetailsImpl;
import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoleRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private User adminUser;
    private User configUser;
    private ObjectMapper objectMapper;
    private String userJwtToken;
    private String adminJwtToken;
    private String configJwtToken;
    private static final String TEST_PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Clear repositories
        eventRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role userRole = createRoleIfNotExists(DefaultValueService.ROLE_USER);
        Role adminRole = createRoleIfNotExists(DefaultValueService.ROLE_ADMIN);
        Role configRole = createRoleIfNotExists(DefaultValueService.ROLE_CONFIG);

        // Create test room
        testRoom = new Room();
        testRoom.setName("Test Room");
        testRoom.setDescription("Test Room Description");
        testRoom = roomRepository.save(testRoom);

        // Create test user with USER role
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.addRole(userRole);
        testUser = userRepository.save(testUser);

        // Create admin user with ADMIN role
        adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setEmail("adminuser@example.com");
        adminUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        adminUser.addRole(adminRole);
        adminUser = userRepository.save(adminUser);

        // Create config user with CONFIG role
        configUser = new User();
        configUser.setUsername("configuser");
        configUser.setEmail("configuser@example.com");
        configUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        configUser.addRole(configRole);
        configUser = userRepository.save(configUser);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Get JWT tokens for different users
        userJwtToken = getJwtToken(testUser);
        adminJwtToken = getJwtToken(adminUser);
        configJwtToken = getJwtToken(configUser);
    }

    private Role createRoleIfNotExists(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return roleRepository.save(role);
                });
    }

    private String getJwtToken(User user) {
        UserDetailsImpl userDetails = (UserDetailsImpl) UserDetailsImpl.build(user);
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
    void testCreateRegularEventWithAdminRole() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Test Event");
        request.setDescription("Test Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(false);

        // Send request with admin token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminJwtToken)
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
    void testCreateRegularEventWithConfigRole() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Test Event Config");
        request.setDescription("Test Description Config");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(false);

        // Send request with config token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + configJwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Test Event Config", savedEvent.getTitle(), "Expected title: 'Test Event Config', but got: '" + savedEvent.getTitle() + "'");
        assertFalse(savedEvent.isRecurring(), "Expected isRecurring to be false, but got: " + savedEvent.isRecurring());
        assertNull(savedEvent.getRrule(), "Expected rrule to be null, but got: '" + savedEvent.getRrule() + "'");
    }

    @Test
    void testCreateRegularEventWithUserRoleShouldFail() throws Exception {
        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Test Event User");
        request.setDescription("Test Description User");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(false);

        // Send request with user token - should fail due to insufficient permissions
        mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userJwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateDailyRecurringEventWithAdminRole() throws Exception {
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

        // Send request with admin token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminJwtToken)
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
    void testCreateWeeklyRecurringEventWithConfigRole() throws Exception {
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

        // Send request with config token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + configJwtToken)
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
    void testCreateMonthlyRecurringEventWithAdminRole() throws Exception {
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

        // Send request with admin token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminJwtToken)
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
    void testCreateYearlyRecurringEventWithConfigRole() throws Exception {
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

        // Send request with config token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + configJwtToken)
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
    void testCreateInvalidRecurringEventWithAdminRole() throws Exception {
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

        // Send request with admin token
        mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminJwtToken)
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

    @Test
    void testUserWithMultipleRoles() throws Exception {
        // Create a user with both ADMIN and CONFIG roles
        User multiRoleUser = new User();
        multiRoleUser.setUsername("multiuser");
        multiRoleUser.setEmail("multiuser@example.com");
        multiRoleUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        
        // Add both ADMIN and CONFIG roles
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(DefaultValueService.ROLE_ADMIN).orElseThrow());
        roles.add(roleRepository.findByName(DefaultValueService.ROLE_CONFIG).orElseThrow());
        multiRoleUser.setRoles(roles);
        multiRoleUser = userRepository.save(multiRoleUser);

        String multiRoleJwtToken = getJwtToken(multiRoleUser);

        // Create event request
        EventRequest request = new EventRequest();
        request.setTitle("Multi-Role User Event");
        request.setDescription("Multi-Role User Description");
        request.setRoomId(testRoom.getId());
        request.setUserId(testUser.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setRecurring(false);

        // Send request with multi-role user token
        MvcResult result = mockMvc.perform(post("/api/v1/addevents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + multiRoleJwtToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        Event savedEvent = objectMapper.readValue(result.getResponse().getContentAsString(), Event.class);
        assertNotNull(savedEvent, "Saved event should not be null");
        assertEquals("Multi-Role User Event", savedEvent.getTitle(), "Expected title: 'Multi-Role User Event', but got: '" + savedEvent.getTitle() + "'");
    }
} 