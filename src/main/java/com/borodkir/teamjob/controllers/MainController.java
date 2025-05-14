package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.data.Event;
import com.borodkir.teamjob.data.Room;
import com.borodkir.teamjob.data.repositories.EventRepository;
import com.borodkir.teamjob.data.repositories.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class MainController {
    private final EventRepository eventRepository;
    private final RoomRepository roomRepository;

    public MainController(EventRepository eventRepository, RoomRepository roomRepository) {
        this.eventRepository = eventRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping({"/login", "/signin", "/"})
    public String signin() {
        return "signin";
    }

    @GetMapping({"/register", "/signup"})
    public String signup() {
        return "signup";
    }

    @GetMapping("/signout")
    public String signout() {
        return "signout";
    }

    @GetMapping("/event/{id}")
    public String event(@PathVariable Long id, Model model) {
        Event event = eventRepository.findById(id).orElse(null);
        model.addAttribute("eventId", id);
        model.addAttribute("event", event);
        return "event";
    }

    @GetMapping("/room/{id}")
    public String room(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id).orElse(null);
        model.addAttribute("roomId", id);
        model.addAttribute("room", room);
        return "room";
    }
}
