package net.otgon.backend.controller;

import jakarta.validation.Valid;
import net.otgon.backend.dto.LoginRequestDto;
import net.otgon.backend.dto.RegisterRequestDto;
import net.otgon.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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

    //Returning JWT as a response which will used to log in automatically
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto userDto) {
        if (userDto.getUsername() == null || userDto.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username and password required");
        }
        try {
            String token = userService
                    .register(userDto.getUsername(), userDto.getPassword(), userDto.getEmail());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDto userLogInDto) {

        if (userLogInDto.getUsername() == null || userLogInDto.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username and password required");
        }

        String username =  userLogInDto.getUsername();
        String password = userLogInDto.getPassword();

        System.out.println("[LogIn] Incoming username: " + userLogInDto.getUsername());

        String token;

        try {
            token = userService.loginWithPassword(username, password);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        if (token == null) {
            return ResponseEntity.status(404).body("User not found");
        }
        System.out.println("[LogIn] Sending token: " + token);

        return ResponseEntity.ok(token);
    }
    //POST /api/cards/{cardId}/qrcode

    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            System.out.println("Getting user info: " + token);
            return ResponseEntity.ok(userService.getUserInfo(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or missing token");
        }
    }
}

