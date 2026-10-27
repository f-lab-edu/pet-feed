package com.petfeed.gateway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {
    @GetMapping({"/", "/ui", "/ui/"})
    public String home() {
        return "redirect:/ui/index.html";
    }
}
