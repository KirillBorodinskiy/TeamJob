package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
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

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
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

    @DeleteMapping("/deleterooms/{id}")
    public ResponseEntity<Room> deleteRoom(@PathVariable Long id) {
        if (roomRepository.findById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        roomRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
