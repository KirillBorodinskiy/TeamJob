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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@WithMockUser
@DataJpaTest
class RoomRepositoryTest {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

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
        testRoom.setTags(Set.of("tag1", "tag2"));
        testRoom.setCreatedDate(baseTime);
        roomRepository.save(testRoom);


        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("example@example.com");
        testUser.setCreatedBy("testUser");
        testUser.setCreatedDate(baseTime);
        userRepository.save(testUser);

        Event testEvent = new Event();
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
        roomRepository.deleteAll();
    }

    @Test
    void findByName() {
        Room foundRoom = roomRepository.findByName(testRoom.getName()).orElse(null);
        assertNotNull(foundRoom);
        assertEquals(testRoom.getName(), foundRoom.getName());
        assertEquals(testRoom.getDescription(), foundRoom.getDescription());
        assertEquals(testRoom.getCreatedBy(), foundRoom.getCreatedBy());
        assertEquals(testRoom.getCreatedDate(), foundRoom.getCreatedDate());
    }


    @Test
    void existsByName() {
        assertTrue(roomRepository.existsByName(testRoom.getName()));
        assertFalse(roomRepository.existsByName("Nonexistent Room"));
    }

    @Test
    void findByTagsAnyMatch() {
        Set<Room> foundRooms = Set.copyOf(roomRepository.findByTagsAnyMatch(List.of("tag1")));
        assertEquals(1, foundRooms.size());
        assertTrue(foundRooms.contains(testRoom));
    }

    @Test
    void findAllOccupiedByStartTimeAndEndTime() {
        Set<Room> foundRooms = Set.copyOf(roomRepository.findAllOccupiedByStartTimeAndEndTime(baseTime, baseTime.plusHours(2)));
        assertEquals(1, foundRooms.size());
        assertTrue(foundRooms.contains(testRoom));
    }
}