package net.otgon.backend.controller;

import lombok.AllArgsConstructor;
import net.otgon.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
@AllArgsConstructor
public class DeviceController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String deviceKey = userService.registerDevice(token);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceKey", deviceKey);
            response.put("message", "Device registered successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}

