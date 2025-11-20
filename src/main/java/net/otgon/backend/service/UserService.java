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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final DeviceRepo deviceRepo;
    private Key secretKey;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    public UserService(UserRepo userRepo, DeviceRepo deviceRepo) {
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.secretKey = null;
    }

    @PostConstruct
    private void init() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new RuntimeException("JWT secret is not configured. Set jwt.secret in application.properties or JWT_SECRET env var");
        }
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public User createNewUser(UserDto userDto) {
        User addedUser = UserMapper.mapToUse(userDto);

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10);
        card.setUser(addedUser);
        addedUser.setCard(card);

        return userRepo.save(addedUser);
    }

    public String login(String username) {
        Optional<User> userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return null; // user not found
        }

        User foundUser = userOpt.get();

        return Jwts.builder()
                .setSubject(foundUser.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username directly here instead of using jwtService
    public Map<String, Object> getUserInfo(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            String username = claimsJws.getBody().getSubject();

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
                    .setSigningKey(secretKey)
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