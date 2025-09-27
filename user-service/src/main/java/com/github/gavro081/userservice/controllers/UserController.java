package com.github.gavro081.userservice.controllers;


import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    String getIndex(){
        return "hello from user service";
    }
    @GetMapping("/users")
    List<User> getUsers(){
        return userService.getUsers();
    }
}
