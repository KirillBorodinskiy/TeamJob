package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.Rooms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/config")
public class DataEventController {
    private RoomRepository roomRepository;
    private Rooms rooms;

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping({"/Rooms"})
    public String Rooms(Model model) {
        List<Rooms> roomsList = roomRepository.findAll();
        model.addAttribute(roomsList);
        return "rooms";
    }
}
