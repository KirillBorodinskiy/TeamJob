package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.RoomRequest;
import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class RestConfigController {
    RoomRepository roomRepository;

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @PostMapping("/editrooms")
    public ResponseEntity<Room> editRooms(@RequestBody RoomRequest roomRequest) {
        if (roomRepository.findByName(roomRequest.getName()).isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        Room room = new Room();
        room.setName(roomRequest.getName());
        room.setDescription(roomRequest.getDescription());
        Room savedRoom = roomRepository.save(room);

        return ResponseEntity.ok(savedRoom);
    }
}
