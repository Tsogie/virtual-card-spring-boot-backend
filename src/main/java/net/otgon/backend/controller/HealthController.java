package net.otgon.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test database connection
            Connection conn = dataSource.getConnection();
            boolean dbConnected = conn.isValid(2);
            conn.close();
            
            response.put("status", dbConnected ? "UP" : "DOWN");
            response.put("database", dbConnected ? "Connected" : "Disconnected");
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("database", "Error: " + e.getMessage());
        }
        
        response.put("timestamp", System.currentTimeMillis());
        response.put("profile", activeProfile);
        
        return response;
    }
}