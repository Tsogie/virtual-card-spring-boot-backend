package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import net.otgon.backend.dto.UserDto;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import net.otgon.backend.mapper.UserMapper;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final DeviceRepo deviceRepo;


    // ✅ Keep a fixed secret key (in real apps, use env var or config file)
    private static final String SECRET = "mySuperSecretKeyForJWTSigning12345";
    private final Key secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());

    public UserService(UserRepo userRepo, DeviceRepo deviceRepo) {
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;

    }

    public User createNewUser(UserDto userDto) {
        User addedUser = UserMapper.mapToUse(userDto);

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10.0);
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

        long expirationTime = 1000 * 60 * 60; // 1 hour
        return Jwts.builder()
                .setSubject(foundUser.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    // ✅ Extract username directly here instead of using jwtService
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
            // ✅ Assuming balance comes from Card
            info.put("cardId", user.getCard().getId());
            info.put("balance", user.getCard() != null ? user.getCard().getBalance() : 0.0);

            return info;

        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }
    }

    public String registerDevice(String token) {

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

            Optional<Device> existing = deviceRepo.findByUser(user);
            if (existing.isPresent()) {
                return existing.get().getDeviceKey(); // Return the same key
            }


            String deviceKey = UUID.randomUUID().toString();

            Device device = new Device();

            device.setUser(user);
            device.setDeviceKey(deviceKey);

            deviceRepo.save(device);

            return deviceKey;

        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }

    }
}
