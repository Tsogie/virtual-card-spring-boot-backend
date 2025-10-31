package net.otgon.backend.controller;

import net.otgon.backend.dto.UserDto;
import net.otgon.backend.entity.User;
import net.otgon.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody UserDto userDto) {
        User addedUser = userService.createNewUser(userDto);
        return new  ResponseEntity<>(addedUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody String username) {
        System.out.println("Incoming username: " + username);
        String token = userService.login(username);

        if (token == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(token);
    }
    //POST /api/cards/{cardId}/qrcode

    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", ""); // remove prefix
            return ResponseEntity.ok(userService.getUserInfo(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or missing token");
        }
    }
}

