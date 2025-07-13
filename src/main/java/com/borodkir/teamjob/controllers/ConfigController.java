package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.data.Room;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import com.borodkir.teamjob.data.repositories.UserRepository;
import com.borodkir.teamjob.services.implementations.CalendarServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.borodkir.teamjob.services.implementations.CalendarServiceImpl.addUserRoleInfo;

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
        addUserRoleInfo(model);
        return "rooms";
    }

    @GetMapping({"/events"})
    public String Events(Model model) {
        model.addAttribute("events", eventRepository.findAll());
        addUserRoleInfo(model);
        return "events";
    }

    @GetMapping({"/users"})
    public String Users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        addUserRoleInfo(model);
        return "users";
    }

}
