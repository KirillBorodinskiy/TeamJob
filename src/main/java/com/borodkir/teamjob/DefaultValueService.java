package com.borodkir.teamjob;

import com.borodkir.teamjob.data.Event;
import com.borodkir.teamjob.data.Role;
import com.borodkir.teamjob.data.Room;
import com.borodkir.teamjob.data.User;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoleRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Optional;

import static java.util.Collections.singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DefaultValueService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultValueService.class);
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private RoomRepository roomRepository;
    private EventRepository eventRepository;
    private PasswordEncoder passwordEncoder;
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_CONFIG = "ROLE_CONFIG";


    private static final List<String> SAMPLE_ROOM_TAGS = Arrays.asList(
            "rooms_Meeting Room", "rooms_Conference Room", "rooms_Projector", "rooms_Whiteboard", "rooms_Large",
            "rooms_Small", "rooms_Quiet Zone", "rooms_Lab", "rooms_Computer Class", "rooms_Auditorium",
            "rooms_Video Conferencing", "rooms_Ground Floor", "rooms_Restricted Access"
    );

    private static final List<String> SAMPLE_USER_TAGS = Arrays.asList(
            "users_IT Support", "users_Developer", "users_Manager", "users_Teacher", "users_Student", "users_Admin Staff",
            "users_HR", "users_Finance", "users_Marketing", "users_Part-Time", "users_Remote", "users_Math Dept",
            "users_Science Dept", "users_Trainer"
    );

    private static final List<String> SAMPLE_EVENT_TAGS = Arrays.asList(
            "event_Meeting", "event_Urgent", "event_Client Call", "event_Internal", "event_Project Alpha", "event_Project Beta",
            "event_Training", "event_Workshop", "event_Presentation", "event_Review", "event_Planning",
            "event_Optional", "event_Recurring", "event_Cancelled", "event_Team Building"
    );

    @Autowired
    public DefaultValueService(RoleRepository roleRepository, UserRepository userRepository, RoomRepository roomRepository, EventRepository eventRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.eventRepository = eventRepository;
    }


    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    @Transactional
    public void insert() {
        try {
            logger.info("Starting default data insertion...");
            // Create Roles
            createRoleIfNotExists(DefaultValueService.ROLE_CONFIG);
            createRoleIfNotExists(DefaultValueService.ROLE_ADMIN);
            createRoleIfNotExists(DefaultValueService.ROLE_USER);
            roleRepository.flush();

            // Create Admin User
            User adminUser = new User();
            adminUser.setUsername("adminadmin");
            adminUser.setPassword(passwordEncoder.encode("adminadmin"));
            adminUser.setEmail("adminadmin@gmail.com");
            adminUser.setRoles(new HashSet<>(roleRepository.findAll()));
            // Combine specific role tags with some random relevant tags
            Set<String> adminTags = new HashSet<>(Arrays.asList("Admin", "System"));
            adminTags.addAll(getRandomTags(SAMPLE_USER_TAGS, 2));
            adminUser.setTags(adminTags);
            createUser(adminUser);

            // --- Create Regular User ---
            User user = new User();
            user.setUsername("useruser");
            user.setPassword(passwordEncoder.encode("useruser"));
            user.setEmail("useruser@user.com");

            Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));
            user.setRoles(new HashSet<>(singleton(userRole)));

            Set<String> regularUserTags = new HashSet<>(Arrays.asList("Standard", "General"));
            regularUserTags.addAll(getRandomTags(SAMPLE_USER_TAGS, 1));
            user.setTags(regularUserTags);
            createUser(user);

            userRepository.flush();

            logger.info("Generating fake data for testing...");
            generateFakeUsers(10); // Will get user tags assigned within
            generateFakeRooms(5); // Will get room tags assigned within
            generateFakeEvents(2); // Will get event tags assigned within
            logger.info("Fake data generation completed.");

        } catch (Exception e) {
            logger.error("Error inserting default data: {}", e.getMessage(), e);
        }
    }

    private void createRoleIfNotExists(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            logger.info("Role '{}' created", roleName);
        } else {
            logger.debug("Role '{}' already exists", roleName);
        }
    }

    private void createRoom(Room room) {

        if (room.getTags() == null || room.getTags().isEmpty()) {
            room.setTags(getRandomTags(SAMPLE_ROOM_TAGS, 3));
        }

        if (!roomRepository.existsByName(room.getName())) {
            roomRepository.save(room);
            logger.info("Room '{}' created with tags: {}", room.getName(), room.getTags());
        } else {
            logger.debug("Room '{}' already exists", room.getName());
        }
    }

    private void createUser(User user) {

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found for user creation"));
            user.setRoles(new HashSet<>(singleton(userRole)));
        }

        if (user.getTags() == null || user.getTags().isEmpty()) {
            user.setTags(getRandomTags(SAMPLE_USER_TAGS, 2));
        }

        if (!userRepository.existsByUsername(user.getUsername())) {
            userRepository.save(user);
            logger.info("User '{}' created with tags: {}", user.getUsername(), user.getTags());
        } else {
            logger.debug("User '{}' already exists", user.getUsername());
        }
    }

    /**
     * Generates fake users for testing purposes.
     *
     * @param count The number of fake users to generate
     */
    private void generateFakeUsers(int count) {
        logger.debug("Generating {} fake users...", count);
        String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Lisa", "William", "Jessica"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor"};
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String username = firstName.toLowerCase() + lastName.toLowerCase() + random.nextInt(100);

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail(username + "@example.com");
            createUser(user);
        }
        logger.debug("Fake users generation complete.");
    }

    /**
     * Generates fake rooms for testing purposes.
     *
     * @param count The number of fake rooms to generate
     */
    private void generateFakeRooms(int count) {
        logger.debug("Generating {} fake rooms...", count);
        String[] roomBaseNames = {"Conference Room", "Meeting Room", "Auditorium", "Training Room", "Board Room",
                "Classroom", "Lecture Hall", "Workshop", "Studio", "Lab", "Gym", "Office", "Lounge"};
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String name = roomBaseNames[random.nextInt(roomBaseNames.length)] + " " + (i + 1);

            Room room = new Room();
            room.setName(name);
            room.setDescription("Description for " + name);
            createRoom(room);
        }
        logger.debug("Fake rooms generation complete.");
    }

    /**
     * Generates fake events spanning the previous, current, and next week.
     *
     * @param eventsPerDay The number of events to generate per day
     */
    private void generateFakeEvents(int eventsPerDay) {
        logger.debug("Generating fake events, {} per day...", eventsPerDay);
        List<User> users = userRepository.findAll();
        List<Room> rooms = roomRepository.findAll();

        if (users.isEmpty() || rooms.isEmpty()) {
            logger.warn("Cannot generate events: no users or rooms available");
            return;
        }

        Random random = new Random();
        String[] eventTitles = {"Team Meeting", "Project Review", "Training Session", "Client Call", "Workshop",
                "Presentation", "Brainstorming", "Interview", "Conference", "Webinar"};
        String[] eventDescriptions = {
                "Discussing project progress and next steps",
                "Reviewing the current status of the project",
                "Training session for new team members",
                "Call with client to discuss requirements",
                "Workshop on new technologies",
                "Presentation of quarterly results",
                "Brainstorming session for new ideas",
                "Interview with potential candidate",
                "Conference call with partners",
                "Webinar on industry trends"
        };

        LocalDate previousWeekStart = LocalDate.now()
                .minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int eventsCreated = 0;
        for (int week = 0; week < 3; week++) {
            LocalDate weekStart = previousWeekStart.plusWeeks(week);
            for (int day = 0; day < 7; day++) {
                LocalDate currentDate = weekStart.plusDays(day);
                for (int i = 0; i < eventsPerDay; i++) {
                    try {
                        int startHour = 8 + random.nextInt(9);
                        int startMinute = random.nextInt(4) * 15;
                        LocalTime startTime = LocalTime.of(startHour, startMinute);
                        int durationMinutes = (random.nextInt(4) + 1) * 30;
                        LocalTime endTime = startTime.plusMinutes(durationMinutes);

                        Event event = new Event();
                        event.setTitle(eventTitles[random.nextInt(eventTitles.length)]);
                        event.setDescription(eventDescriptions[random.nextInt(eventDescriptions.length)]);
                        event.setStartTime(LocalDateTime.of(currentDate, startTime));
                        event.setEndTime(LocalDateTime.of(currentDate, endTime));
                        
                        // Select random user and room
                        User selectedUser = users.get(random.nextInt(users.size()));
                        Room selectedRoom = rooms.get(random.nextInt(rooms.size()));
                        
                        event.setUser(selectedUser);
                        event.setRoom(selectedRoom);
                        event.setRecurring(false);
                        event.setTags(getRandomTags(SAMPLE_EVENT_TAGS, 2));

                        // Check for overlapping events
                        boolean hasOverlap = eventRepository.findOverlappingEventsInRoom(
                                event.getStartTime(),
                                event.getEndTime(),
                                Optional.of(event.getRoom()));
                        
                        if (!hasOverlap) {
                            Event savedEvent = eventRepository.save(event);
                            eventsCreated++;
                            logger.info("Event created: {} on {} with tags: {}", savedEvent.getTitle(), currentDate, savedEvent.getTags());
                        } else {
                            logger.debug("Skipping event due to overlap: {} on {}", event.getTitle(), currentDate);
                        }
                    } catch (Exception e) {
                        logger.error("Error creating event on {}: {}", currentDate, e.getMessage(), e);
                        // Continue with next event instead of failing completely
                    }
                }
            }
        }
        logger.debug("Fake events generation complete. Created {} events.", eventsCreated);
    }

    /**
     * Helper method to get random tags from a given list.
     *
     * @param tagList The list of tags to choose from
     * @param count   The number of tags to select randomly
     * @return A Set of randomly selected tags
     */
    private Set<String> getRandomTags(List<String> tagList, int count) {
        Random random = new Random();
        Set<String> selectedTags = new HashSet<>();

        // Ensure we don't try to get more tags than exist
        int actualCount = Math.min(count, tagList.size());

        while (selectedTags.size() < actualCount) {
            selectedTags.add(tagList.get(random.nextInt(tagList.size())));
        }

        return selectedTags;
    }
}