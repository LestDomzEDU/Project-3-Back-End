package com.project03.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestingController {
    // just checking to see if 
    @GetMapping("/testing")
    public String testing() {
        return "Route is working";
    }
}