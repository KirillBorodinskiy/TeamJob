package io.datajek.spring.basics.teamjob;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Role;
import io.datajek.spring.basics.teamjob.data.Room;
import io.datajek.spring.basics.teamjob.data.User;
import io.datajek.spring.basics.teamjob.data.repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoleRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.repositories.UserRepository;
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

import static java.util.Collections.singleton;

@Service
public class DefaultValueService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private RoomRepository roomRepository;
    private EventRepository eventRepository;
    private PasswordEncoder passwordEncoder;
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_CONFIG = "ROLE_CONFIG";


    private static final List<String> SAMPLE_ROOM_TAGS = Arrays.asList(
            "Meeting Room", "Conference Room", "Projector", "Whiteboard", "Large",
            "Small", "Quiet Zone", "Lab", "Computer Class", "Auditorium",
            "Video Conferencing", "Ground Floor", "Restricted Access"
    );

    private static final List<String> SAMPLE_USER_TAGS = Arrays.asList(
            "IT Support", "Developer", "Manager", "Teacher", "Student", "Admin Staff",
            "HR", "Finance", "Marketing", "Part-Time", "Remote", "Math Dept",
            "Science Dept", "Trainer"
    );

    private static final List<String> SAMPLE_EVENT_TAGS = Arrays.asList(
            "Meeting", "Urgent", "Client Call", "Internal", "Project Alpha", "Project Beta",
            "Training", "Workshop", "Presentation", "Review", "Planning",
            "Optional", "Recurring", "Cancelled", "Team Building"
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

            // Generate Fake Data
            System.out.println("Generating fake data for testing...");
            generateFakeUsers(20); // Will get user tags assigned within
            generateFakeRooms(15); // Will get room tags assigned within
            generateFakeEvents(7); // Will get event tags assigned within
            System.out.println("Fake data generation completed.");

        } catch (Exception e) {
            System.err.println("Error inserting default data: " + e.getMessage());
        }
    }

    private void createRoleIfNotExists(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            System.out.println("Role " + roleName + " created");
        } else {
            System.out.println("Role " + roleName + " already exists");
        }
    }

    private void createRoom(Room room) {

        if (room.getTags() == null || room.getTags().isEmpty()) {
            room.setTags(getRandomTags(SAMPLE_ROOM_TAGS, 3));
        }

        if (!roomRepository.existsByName(room.getName())) {
            roomRepository.save(room);
            System.out.println("Room " + room.getName() + " created with tags: " + room.getTags());
        } else {
            System.out.println("Room " + room.getName() + " already exists");
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
            System.out.println("User " + user.getUsername() + " created with tags: " + user.getTags());
        } else {
            System.out.println("User " + user.getUsername() + " already exists");
        }
    }

    /**
     * Generates fake users for testing purposes.
     *
     * @param count The number of fake users to generate
     */
    private void generateFakeUsers(int count) {
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
    }

    /**
     * Generates fake rooms for testing purposes.
     *
     * @param count The number of fake rooms to generate
     */
    private void generateFakeRooms(int count) {
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
    }

    /**
     * Generates fake events spanning the previous, current, and next week.
     *
     * @param eventsPerDay The number of events to generate per day
     */
    private void generateFakeEvents(int eventsPerDay) {
        List<User> users = userRepository.findAll();
        List<Room> rooms = roomRepository.findAll();

        if (users.isEmpty() || rooms.isEmpty()) {
            System.out.println("Cannot generate events: no users or rooms available");
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

        for (int week = 0; week < 3; week++) {
            LocalDate weekStart = previousWeekStart.plusWeeks(week);
            for (int day = 0; day < 7; day++) {
                LocalDate currentDate = weekStart.plusDays(day);
                for (int i = 0; i < eventsPerDay; i++) {
                    // ... (time calculation logic remains the same) ...
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
                    event.setUser(users.get(random.nextInt(users.size())));
                    event.setRoom(rooms.get(random.nextInt(rooms.size())));
                    event.setRecurring(false);
                    event.setTags(getRandomTags(SAMPLE_EVENT_TAGS, 2));

                    if (!eventRepository.findOverlappingEventsInRoom(
                            event.getStartTime(),
                            event.getEndTime(),
                            Optional.of(event.getRoom()))) {
                        eventRepository.save(event);
                        System.out.println("Event created: " + event.getTitle() +
                                " on " + currentDate + " with tags: " + event.getTags());
                    }
                }
            }
        }
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