package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.data.Room;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/config")
public class ConfigController {
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

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"/rooms"})
    public String Rooms(Model model) {
        List<Room> roomList = roomRepository.findAll();
        model.addAttribute("rooms", roomList);
        return "rooms";
    }

    @GetMapping({"/events"})
    public String Events(Model model) {
        model.addAttribute("events", eventRepository.findAll());
        return "events";
    }

    @GetMapping({"/users"})
    public String Users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

}
