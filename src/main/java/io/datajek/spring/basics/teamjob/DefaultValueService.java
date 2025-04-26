package io.datajek.spring.basics.teamjob;

import io.datajek.spring.basics.teamjob.data.repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoleRepository;
import io.datajek.spring.basics.teamjob.data.repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Role;
import io.datajek.spring.basics.teamjob.data.Room;
import io.datajek.spring.basics.teamjob.data.User;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

    @PostConstruct
    @Transactional
    public void insert() {
        try {
            if (!roleRepository.existsByName(DefaultValueService.ROLE_CONFIG)) {
                Role roleUser = new Role();
                roleUser.setName(DefaultValueService.ROLE_CONFIG);
                roleRepository.save(roleUser);
                System.out.println("Role " + DefaultValueService.ROLE_CONFIG + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_CONFIG + " already exists");
            }

            if (!roleRepository.existsByName(DefaultValueService.ROLE_ADMIN)) {
                Role roleAdmin = new Role();
                roleAdmin.setName(DefaultValueService.ROLE_ADMIN);
                roleRepository.save(roleAdmin);
                System.out.println("Role " + DefaultValueService.ROLE_ADMIN + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_ADMIN + " already exists");
            }

            if (!roleRepository.existsByName(DefaultValueService.ROLE_USER)) {
                Role roleConfig = new Role();
                roleConfig.setName(DefaultValueService.ROLE_USER);
                roleRepository.save(roleConfig);
                System.out.println("Role " + DefaultValueService.ROLE_USER + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_USER + " already exists");
            }

            // Force a flush to ensure data is committed
            roleRepository.flush();


            User adminUser = new User();
            adminUser.setUsername("adminadmin");
            adminUser.setPassword(passwordEncoder.encode("adminadmin"));
            adminUser.setEmail("adminadmin@gmail.com");
            adminUser.setRoles(new HashSet<>(roleRepository.findAll()));

            createUser(adminUser);

            User user = new User();
            user.setUsername("useruser");
            user.setPassword(passwordEncoder.encode("useruser"));
            user.setEmail("useruser@user.com");
            Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRoles(new HashSet<>(singleton(userRole)));
            createUser(user);

            // Force a flush to ensure data is committed
            userRepository.flush();

            // Generate fake data for testing
            System.out.println("Generating fake data for testing...");
            generateFakeUsers(20);
            generateFakeRooms(15);
            generateFakeEvents(7);
            System.out.println("Fake data generation completed.");

        } catch (Exception e) {
            System.err.println("Error inserting default roles: " + e.getMessage() + e.getCause());
        }
    }

    private void createRoom(Room room) {
        if (!roomRepository.existsByName(room.getName())) {
            roomRepository.save(room);
            System.out.println("Room " + room.getName() + " created");
        } else {
            System.out.println("Room " + room.getName() + " already exists");
        }
    }

    private void createUser(User user) {
        if (!userRepository.existsByUsername(user.getUsername())) {
            user.setRoles(new HashSet<>(roleRepository.findAll()));
            userRepository.save(user);
            System.out.println("User " + user.getUsername() + " created");
        } else {
            System.out.println("User " + user.getUsername() + " already exists");
        }
    }

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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

        Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        for (int i = 0; i < count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String username = firstName.toLowerCase() + lastName.toLowerCase() + random.nextInt(100);

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail(username + "@example.com");
            user.setRoles(new HashSet<>(singleton(userRole)));

            createUser(user);
        }
    }

    /**
     * Generates fake rooms for testing purposes.
     *
     * @param count The number of fake rooms to generate
     */
    private void generateFakeRooms(int count) {
        String[] roomNames = {"Conference Room", "Meeting Room", "Auditorium", "Training Room", "Board Room",
                "Classroom", "Lecture Hall", "Workshop", "Studio", "Lab"};
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String name = roomNames[random.nextInt(roomNames.length)] + " " + (i + 3); // Start from 3 since we already have Room 1 and Room 2

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

        // Get the start of the previous week (Monday)
        LocalDate previousWeekStart = LocalDate.now()
                .minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Generate events for 3 weeks (previous, current, and next week)
        for (int week = 0; week < 3; week++) {
            LocalDate weekStart = previousWeekStart.plusWeeks(week);

            // Generate events for each day of the week
            for (int day = 0; day < 7; day++) {
                LocalDate currentDate = weekStart.plusDays(day);

                // Generate multiple events per day
                for (int i = 0; i < eventsPerDay; i++) {
                    // Random start time between 8 AM and 5 PM
                    int startHour = 8 + random.nextInt(9); // 8 AM to 4 PM max
                    int startMinute = random.nextInt(4) * 15; // 0, 15, 30, or 45 minutes

                    LocalTime startTime = LocalTime.of(startHour, startMinute);

                    // Event duration between 30 minutes and 2 hours
                    int durationMinutes = (random.nextInt(4) + 1) * 30; // 30, 60, 90, or 120 minutes
                    LocalTime endTime = startTime.plusMinutes(durationMinutes);

                    // Create the event
                    Event event = new Event();
                    event.setTitle(eventTitles[random.nextInt(eventTitles.length)]);
                    event.setDescription(eventDescriptions[random.nextInt(eventDescriptions.length)]);
                    event.setStartTime(LocalDateTime.of(currentDate, startTime));
                    event.setEndTime(LocalDateTime.of(currentDate, endTime));
                    event.setUser(users.get(random.nextInt(users.size())));
                    event.setRoom(rooms.get(random.nextInt(rooms.size())));
                    event.setRecurring(false);

                    // Check for overlapping events in the same room
                    if (!eventRepository.findOverlappingEventsInRoom(
                            event.getStartTime(),
                            event.getEndTime(),
                            Optional.of(event.getRoom()))) {
                        eventRepository.save(event);
                        System.out.println("Event created: " + event.getTitle() + " on " + currentDate);
                    }
                }
            }
        }
    }
}
