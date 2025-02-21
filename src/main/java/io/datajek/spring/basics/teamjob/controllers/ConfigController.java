package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Repositories.EventRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.RoomRepository;
import io.datajek.spring.basics.teamjob.data.Room;
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

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping({"/rooms"})
    public String Rooms(Model model) {
        List<Room> roomList = roomRepository.findAll();
        model.addAttribute("rooms", roomList);
        return "rooms";
    }

    @GetMapping({"/events"})
    public String Events(Model model) {
        List<Event> eventList = eventRepository.findAll();
        model.addAttribute("events", eventList);
        return "events";
    }


}
