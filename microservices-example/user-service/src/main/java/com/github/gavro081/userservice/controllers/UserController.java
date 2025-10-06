package com.github.gavro081.userservice.controllers;

import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    List<User> getUsers(){
        return userService.getUsers();
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDetailDto> getUserByName(@PathVariable String username){
        User user = userService.getUserByUsername(username);
        if (user != null) {
            UserDetailDto dto = new UserDetailDto(user.getId());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
