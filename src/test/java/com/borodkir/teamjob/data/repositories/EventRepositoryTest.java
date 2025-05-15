package com.borodkir.teamjob.data.repositories;

import com.borodkir.teamjob.data.Event;
import com.borodkir.teamjob.data.Room;
import com.borodkir.teamjob.data.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@WithMockUser
@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Event testEvent;
    private Room testRoom;

    LocalDateTime baseTime = LocalDateTime.now()
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);


    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        eventRepository.deleteAll();
        userRepository.deleteAll();
        roomRepository.deleteAll();

        testRoom = new Room();
        testRoom.setName("Test Room");
        testRoom.setDescription("Test description");
        testRoom.setCreatedBy("testUser");
        testRoom.setCreatedDate(baseTime);
        roomRepository.save(testRoom);


        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("example@example.com");
        testUser.setCreatedBy("testUser");
        testUser.setCreatedDate(baseTime);
        userRepository.save(testUser);

        testEvent = new Event();
        testEvent.setTitle("Test event");
        testEvent.setStartTime(baseTime);
        testEvent.setEndTime(baseTime.plusHours(1));
        testEvent.setDescription("Test description");
        testEvent.setRoom(testRoom);
        testEvent.setUser(testUser);
        testEvent.setTags(Set.of("tag1", "tag2"));
        testEvent.setCreatedBy("testUser");
        testEvent.setCreatedDate(baseTime);
        eventRepository.save(testEvent);

    }
    @AfterEach
    void tearDown() {
        // Clean up the database after each test
        eventRepository.deleteAll();
        userRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void findByStartTime() {

        // Act
        Optional<Event> found = eventRepository.findByStartTime(testEvent.getStartTime());

        // Assert
        assertThat(found).isPresent().hasValueSatisfying(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getStartTime()).isEqualTo(testEvent.getStartTime());
        });
    }

    @Test
    void findByEndTime() {
        // Act
        Optional<Event> found = eventRepository.findByEndTime(testEvent.getEndTime());

        // Assert
        assertThat(found).isPresent().hasValueSatisfying(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getEndTime()).isEqualTo(testEvent.getEndTime());
        });
    }

    @Test
    void findByRoom() {
        // Act
        Optional<Event> found = eventRepository.findByRoom(testEvent.getRoom());

        // Assert
        assertThat(found).isPresent().hasValueSatisfying(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getRoom()).isEqualTo(testRoom);
        });
    }

    @Test
    void findByTitle() {
        // Act
        Optional<Event> found = eventRepository.findByTitle(testEvent.getTitle());

        // Assert
        assertThat(found).isPresent().hasValueSatisfying(e -> assertThat(e.getTitle()).isEqualTo(testEvent.getTitle()));
    }

    @Test
    void findAllByTitle() {
        // Act
        var found = eventRepository.findAllByTitle(testEvent.getTitle());

        // Assert
        assertThat(found).isNotNull().hasSize(1).first().satisfies(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getRoom()).isEqualTo(testRoom);
        });
    }

    @Test
    void findOverlappingEvents() {
        // Act
        var found = eventRepository.findOverlappingEvents(testEvent.getStartTime(), testEvent.getEndTime());

        // Assert
        assertThat(found).isNotNull().hasSize(1).first().satisfies(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getRoom()).isEqualTo(testRoom);
        });
    }

    @Test
    void findOverlappingEventsInRoom() {
        // Act
        var found = eventRepository.findOverlappingEventsInRoom(testEvent.getStartTime(), testEvent.getEndTime(), Optional.of(testRoom));

        // Assert
        assertThat(found).isTrue();
    }

    @Test
    void findAllOverlappingEventsInRoom() {
        // Act
        var found = eventRepository.findAllOverlappingEventsInRoom(testEvent.getStartTime(), testEvent.getEndTime(), Optional.of(testRoom));

        // Assert
        assertThat(found).isNotNull().hasSize(1).first().satisfies(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getRoom()).isEqualTo(testRoom);
        });
    }

    @Test
    void findByTagsAnyMatch() {
        // Act
        var found = eventRepository.findByTagsAnyMatch(List.of("tag1", "tag2"));

        // Assert
        assertThat(found).isNotNull().hasSize(1).first().satisfies(e -> {
            assertThat(e.getTitle()).isEqualTo(testEvent.getTitle());
            assertThat(e.getRoom()).isEqualTo(testRoom);
        });
    }
}