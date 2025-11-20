package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import net.otgon.backend.dto.DeviceRegisterRequest;
import net.otgon.backend.dto.DeviceRegisterResponse;
import net.otgon.backend.dto.UserDto;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import net.otgon.backend.mapper.UserMapper;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final DeviceRepo deviceRepo;
    private final JwtService jwtService;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    public UserService(UserRepo userRepo, DeviceRepo deviceRepo, JwtService jwtService) {
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.jwtService = jwtService;
    }


    public String register(String username, String password, String email) {
        // Check if user exists
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);

        // Create card for user
        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10); // Initial balance
        card.setUser(newUser);
        newUser.setCard(card);

        userRepo.save(newUser);

        // Generate JWT
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(jwtService.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String loginWithPassword(String username, String password) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate JWT
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(jwtService.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username directly here instead of using jwtService
    public Map<String, Object> getUserInfo(String token) {
        try {

            String username = jwtService.extractUsername(token);
            User user = userRepo.findByUsername(username).orElse(null);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            Map<String, Object> info = new HashMap<>();
            info.put("username", user.getUsername());
            info.put("email", user.getEmail());
            info.put("cardId", user.getCard().getId());
            // Safe null check for card
            info.put("balance", user.getCard() != null ? user.getCard().getBalance() : 0);

            return info;

        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }
    }

    public DeviceRegisterResponse registerDevice(String token, DeviceRegisterRequest request) {
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(jwtService.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Device> existing = deviceRepo.findByUser(user);
            if (existing.isPresent()) {
                System.out.println("Pub key existing reg: " +  existing.get().getPublicKey());
                String deviceId = existing.get().getId();
                return new DeviceRegisterResponse(deviceId, "Device already exists");
            }

            Device device = new Device();
            device.setUser(user);
            device.setPublicKey(request.getPublicKey());
            System.out.println("Device Registration: " + request.getPublicKey());

            Device saved = deviceRepo.save(device);
            return new DeviceRegisterResponse(saved.getId(), "Device registered successfully");
        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }
    }

}