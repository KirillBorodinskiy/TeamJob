package com.borodkir.teamjob.controllers;

import com.borodkir.teamjob.data.Role;
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
import java.util.Map;
import java.util.stream.Collectors;

import static com.borodkir.teamjob.services.implementations.CalendarServiceImpl.addUserRoleInfo;
import com.borodkir.teamjob.data.User;

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
        // Prepare a list of maps for uniform template structure
        List<Map<String, Object>> roomsWithTagsString = roomList.stream().map(room -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("room", room);
            map.put("tagsString", String.join(", ", room.getTags()));
            return map;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("roomsWithTagsString", roomsWithTagsString);
        addUserRoleInfo(model);
        return "rooms";
    }

    @GetMapping({"/events"})
    public String Events(Model model) {
        List<com.borodkir.teamjob.data.Event> eventList = eventRepository.findAll();
        // Prepare a list of maps for uniform template structure
        List<Map<String, Object>> eventsWithDetailsString = eventList.stream().map(event -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("event", event);
            map.put("tagsString", String.join(", ", event.getTags()));
            map.put("userString", event.getUser() != null ? event.getUser().getUsername() : "");
            map.put("roomString", event.getRoom() != null ? event.getRoom().getName() : "");
            return map;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("eventsWithDetailsString", eventsWithDetailsString);
        addUserRoleInfo(model);
        return "events";
    }

    @GetMapping({"/users"})
    public String Users(Model model) {
        List<User> userList = userRepository.findAll();
        // Add a rolesString property to each user (using a wrapper or map)
        List<Map<String, Object>> usersWithRolesString = userList.stream().map(user -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("user", user);
            map.put("rolesString", user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));
            map.put("tags", user.getTags());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("usersWithRolesString", usersWithRolesString);
        addUserRoleInfo(model);
        return "users";
    }

}
