package net.otgon.backend.controller;

import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import net.otgon.backend.dto.DeviceRegisterRequest;
import net.otgon.backend.dto.DeviceRegisterResponse;
import net.otgon.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
@AllArgsConstructor
public class DeviceController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<DeviceRegisterResponse> registerDevice(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DeviceRegisterRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            DeviceRegisterResponse response = userService.registerDevice(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return 401 with a minimal response containing error info
            DeviceRegisterResponse errorResponse = new DeviceRegisterResponse(
                    null,
                    "Error: " + e.getMessage()
            );
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

}

