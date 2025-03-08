package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.EventRequest;
import io.datajek.spring.basics.teamjob.data.Repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.RoomRequest;
import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RestConfigController {
    RoomRepository roomRepository;
    EventRepository eventRepository;
    private UserRepository userRepository;

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostMapping("/addrooms")
    public ResponseEntity<Room> addRooms(@RequestBody RoomRequest roomRequest) {
        if (roomRepository.findByName(roomRequest.getName()).isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        Room room = new Room();
        room.setName(roomRequest.getName());
        room.setDescription(roomRequest.getDescription());
        Room savedRoom = roomRepository.save(room);

        return ResponseEntity.ok(savedRoom);
    }

    @PostMapping("/addevents")
    public ResponseEntity<Event> addEvents(@RequestBody EventRequest eventRequest) {
        Event event = new Event();
        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setRoom(roomRepository.findById(eventRequest.getRoomId()).orElse(null));
        if (eventRequest.getUserId() != null) {
            event.setUser(userRepository.findById(eventRequest.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + eventRequest.getUserId())));
        }
        event.setStartTime(eventRequest.getStartTime());
        event.setEndTime(eventRequest.getEndTime());
        Event savedEvent = eventRepository.save(event);
        return ResponseEntity.ok(savedEvent);
    }

    @DeleteMapping("/deleterooms/{id}")
    public ResponseEntity<Room> deleteRoom(@PathVariable Long id) {
        if (roomRepository.findById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        roomRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/deleteevents/{id}")
    public ResponseEntity<Event> deleteEvent(@PathVariable Long id) {
        if (eventRepository.findById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        eventRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
