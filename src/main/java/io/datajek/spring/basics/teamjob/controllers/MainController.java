package io.datajek.spring.basics.teamjob.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class MainController {
    @GetMapping({"/login", "/signin"})
    public String signin() {
        return "signin";
    }

    @GetMapping({"/register", "/signup"})
    public String signup() {
        return "signup";
    }

}
