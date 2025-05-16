package com.borodkir.teamjob.controllers;


import com.borodkir.teamjob.data.*;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1")
public class RestConfigController {
    private RoomRepository roomRepository;
    private EventRepository eventRepository;
    private UserRepository userRepository;

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }


    @PostMapping("/validateJWT")
    public ResponseEntity<?> validateJWT() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok().build();
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

    @PostMapping("/checkavailability")
    public Boolean checkAvailability(@RequestBody RoomAvailabilityRequest roomAvailabilityRequest) {
        if (roomRepository.findById(roomAvailabilityRequest.getRoomId()).isPresent()) {
            // Invert the result since true from repository means there IS a conflict
            return !eventRepository.findOverlappingEventsInRoom(
                    roomAvailabilityRequest.getStartTime(),
                    roomAvailabilityRequest.getEndTime(),
                    roomRepository.findById(roomAvailabilityRequest.getRoomId()));
        }
        return false;
    }

    @PostMapping("/addevents")
    public ResponseEntity<Event> addEvents(@RequestBody EventRequest eventRequest) {
        //First check if the room isn't already booked
        if (eventRepository.findOverlappingEventsInRoom
                (
                        eventRequest.getStartTime(),
                        eventRequest.getEndTime(),
                        roomRepository.findById(eventRequest.getRoomId())
                )
        ) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        Event event = new Event();
        //Check if the user exists
        if (eventRequest.getUserId() != null) {
            event.setUser(userRepository.findById(eventRequest.getUserId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("User not found with ID: " + eventRequest.getUserId())
                    )
            );
        }
        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setRoom(roomRepository.findById(eventRequest.getRoomId()).orElse(null));

        event.setStartTime(eventRequest.getStartTime());
        event.setEndTime(eventRequest.getEndTime());

        // Handle recurrence
        event.setRecurring(eventRequest.isRecurring());
        if (eventRequest.isRecurring()) {
            // Build RRULE string
            StringBuilder rrule = new StringBuilder(eventRequest.getRrule());
            if (!rrule.isEmpty()) {
                // Add interval if specified
                String interval = eventRequest.getInterval();
                if (interval != null && !interval.isEmpty()) {
                    rrule.append(";INTERVAL=").append(interval);
                }
                
                // Add end date if specified
                if (eventRequest.getRecurrenceEndDate() != null) {
                    rrule.append(";UNTIL=").append(eventRequest.getRecurrenceEndDate().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")));
                }
                
                // Add BYDAY for weekly recurrence
                if (rrule.toString().startsWith("FREQ=WEEKLY") && eventRequest.getWeekdays() != null && !eventRequest.getWeekdays().isEmpty()) {
                    rrule.append(";BYDAY=").append(String.join(",", eventRequest.getWeekdays()));
                }
            }
            event.setRrule(rrule.toString());
            event.setRecurrenceEndDate(eventRequest.getRecurrenceEndDate());
            event.setExdate(eventRequest.getExdate());
            event.setRdate(eventRequest.getRdate());
        }

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
