package net.otgon.backend.service;

import net.otgon.backend.dto.DeviceRegisterRequest;
import net.otgon.backend.dto.DeviceRegisterResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private DeviceRepo deviceRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Mock
    private JwtService jwtService;

    // Test data
    private Device mockDevice;
    private User mockUser;
    private Card mockCard;
    private String testDeviceId;

    @BeforeEach
    void setUp() {
    }

    // =====================================================
    // TEST 1: SUCCESSFUL REGISTRATION
    // =====================================================
    @Test
    @DisplayName("Test 1: Register Success - New user with valid data")
    void testRegisterSuccess() {
        // ============ ARRANGE ============
        String username = "alice";
        String password = "password123";
        String email = "alice@test.com";
        String hashedPassword = "$2a$10$hashedPasswordExample";

        // Mock: Username does NOT exist
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        // Mock: Email does NOT exist
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

        // Mock: Password encoder hashes the password
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);

        // Mock: UserRepo saves the user (return the same user)
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock: JWT service generates token
        when(jwtService.generateToken(username)).thenReturn("mock.jwt.token");

        // ============ ACT ============
        String jwt = userService.register(username, password, email);

        // ============ ASSERT ============
        // 1. Verify JWT was returned
        assertNotNull(jwt);
        assertEquals("mock.jwt.token", jwt);

        // 2. Verify password was hashed
        verify(passwordEncoder, times(1)).encode(password);

        // 3. Verify user was saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertEquals(hashedPassword, savedUser.getPassword());

        // 4. Verify card was created with €10 initial balance
        assertNotNull(savedUser.getCard());
        assertEquals(10.0, savedUser.getCard().getBalance(), 0.01);

        // 5. Verify repositories were called correctly
        verify(userRepo, times(1)).findByUsername(username);
        verify(userRepo, times(1)).findByEmail(email);
    }

    // =====================================================
    // TEST 2: USERNAME ALREADY EXISTS
    // =====================================================
    @Test
    @DisplayName("Test 2: Register Fails - Username already taken")
    void testRegisterDuplicateUsername() {
        // ============ ARRANGE ============
        String username = "alice";
        String password = "password123";
        String email = "alice@test.com";

        // Create existing user
        User existingUser = new User();
        existingUser.setUsername(username);

        // Mock: Username DOES exist
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(username, password, email),
                "Expected RuntimeException for duplicate username"
        );

        // Verify exception message
        assertEquals("Username already exists", exception.getMessage());

        // Verify user was NOT saved
        verify(userRepo, never()).save(any(User.class));

        // Verify password was NOT hashed (failed before that)
        verify(passwordEncoder, never()).encode(anyString());
    }

    // =====================================================
    // TEST 3: EMAIL ALREADY EXISTS
    // =====================================================
    @Test
    @DisplayName("Test 3: Register Fails - Email already registered")
    void testRegisterDuplicateEmail() {
        // ============ ARRANGE ============
        String username = "alice";
        String password = "password123";
        String email = "alice@test.com";

        // Create existing user
        User existingUser = new User();
        existingUser.setEmail(email);

        // Mock: Username DOES exist
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(username, password, email),
                "Expected RuntimeException for duplicate email"
        );

        // Verify exception message
        assertEquals("Email already registered", exception.getMessage());

        // Verify user was NOT saved
        verify(userRepo, never()).save(any(User.class));

        // Verify password was NOT hashed (failed before that)
        verify(passwordEncoder, never()).encode(anyString());
    }

    // TEST 4: Login success with correct password
    @Test
    @DisplayName("Test 4: Login Success - All credentials valid")
    void testLoginSuccess() {
        // ============ ARRANGE ============
        String username = "alice";
        String plainPassword = "password123";
        String hashedPassword = "$2a$10$hashedExample";
        User user = createUserWithHashedPassword(username, hashedPassword);

        // Mock: User EXISTS in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Password MATCHES
        // When passwordEncoder.matches("password123", "$2a$10$hashedExample") is called
        // Return TRUE (meaning password is correct)
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(true);

        // Mock: JWT service generates token
        when(jwtService.generateToken(username)).thenReturn("mock.jwt.token");

        // ============ ACT ============
        String jwt = userService.loginWithPassword(username, plainPassword);

        // ============ ASSERT ============
        assertNotNull(jwt);
        assertEquals("mock.jwt.token", jwt);

        // Verify password check happened
        verify(passwordEncoder, times(1)).matches(plainPassword, hashedPassword);

        // Verify JWT was generated
        verify(jwtService, times(1)).generateToken(username);
    }

    // TEST 5: Login fails - user not found
    @Test
    @DisplayName("Test 5: Login Fails - User not found")
    void testLoginUserNotFound() {
        // ============ ARRANGE ============
        String nonExistingUsername = "doesNotExist";
        String password = "password123";

        // Mock: User does NOT exist
        when(userRepo.findByUsername(nonExistingUsername)).thenReturn(Optional.empty());

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginWithPassword(nonExistingUsername, password),
                "Expected RuntimeException for user not found"
        );

        // Verify exception message
        assertEquals("User not found", exception.getMessage());

        // Verify repository was called
        verify(userRepo, times(1)).findByUsername(nonExistingUsername);

        // Verify password check was NEVER called (threw exception before reaching it)
        verify(passwordEncoder, never()).matches(anyString(), anyString());

        // Verify JWT was NEVER generated
        verify(jwtService, never()).generateToken(anyString());
    }

    // TEST 6: Login fails - wrong password
    @Test
    @DisplayName("Test 6: Login Fails - Invalid password")
    void testLoginWrongPassword() {
        // ============ ARRANGE ============

        String username = "alice";
        String correctPasswordHash = "$2a$10$hashedExample";
        String wrongPassword = "password123";

        User user = createUserWithHashedPassword(username, correctPasswordHash);

        // Mock: User EXISTS in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Password NOT matches
        when(passwordEncoder.matches(wrongPassword, correctPasswordHash)).thenReturn(false);

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginWithPassword(username, wrongPassword),
                "Expected RuntimeException for invalid password"
        );

        // Verify exception message
        assertEquals("Invalid password", exception.getMessage());

        // Verify repository was called
        verify(userRepo, times(1)).findByUsername(username);

        // Verify password check happened
        verify(passwordEncoder, times(1)).matches(wrongPassword, correctPasswordHash);

        // Verify JWT was NEVER generated
        verify(jwtService, never()).generateToken(anyString());
    }
    // TEST 7: Get user info success
    @Test
    @DisplayName("Test 7: Get User Info Success - Returns user data")
    void testGetUserInfoSuccess() {
        // ============ ARRANGE ============
        String username = "alice";
        String email = "alice@test.com";
        String token = "mock.jwt.token";
        String passwordHash = "$2a$10$hashedExample";

        User user = createUserWithHashedPassword(username, passwordHash);
        user.setEmail(email);

        String cardId = user.getCard().getId();
        double balance = user.getCard().getBalance();

        // Mock: JWT service extracts username from token
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User EXISTS in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // ============ ACT ============
        Map<String, Object> info = userService.getUserInfo(token);

        // ============ ASSERT ============
        // 1. Verify the map is not null
        assertNotNull(info);

        // 2. Verify the map contains correct data
        assertEquals(username, info.get("username"));
        assertEquals(email, info.get("email"));
        assertEquals(cardId, info.get("cardId"));
        assertEquals(balance, info.get("balance"));

        // 3. Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
    }
    // TEST 8: Get user info - invalid token
    @Test
    @DisplayName("Test 8: Get User Info Fails - Invalid token")
    void testGetUserInfoInvalidToken() {
        // ============ ARRANGE ============
        String invalidToken = "invalid.token";

        // Mock: JWT service throws RuntimeException
        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserInfo(invalidToken)
        );

        // The exception message comes directly from JwtService
        assertEquals("Invalid token", exception.getMessage());

        verify(userRepo, never()).findByUsername(anyString());
    }

    // TEST 9: Get user info - user not found (token valid but user deleted)
    @Test
    @DisplayName("Test 9: Get User Info Fails - User not found")
    void testGetUserInfoUserNotFound() {
        // ============ ARRANGE ============
        String validToken = "valid.token";
        String deletedUsername = "deletedUser";

        // Mock: JWT service extracts username (token is valid)
        when(jwtService.extractUsername(validToken)).thenReturn(deletedUsername);

        // Mock: User does NOT exist in database (was deleted)
        when(userRepo.findByUsername(deletedUsername)).thenReturn(Optional.empty()); // ← ADD THIS

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserInfo(validToken),
                "Expected RuntimeException for user not found"
        );

        // Verify exception message
        assertEquals("User not found", exception.getMessage());

        // Verify JWT extraction happened
        verify(jwtService, times(1)).extractUsername(validToken);

        // Verify we tried to find the user
        verify(userRepo, times(1)).findByUsername(deletedUsername);
    }

    // TEST 10: Register device success
    @Test
    @DisplayName("Test 10: Register Device Success - success")
    void testRegisterDeviceSuccess() {

        // ============ ARRANGE ============

        String username = "alice";
        String token = "mock.jwt.token";
        String passwordHash = "$2a$10$hashedExample";
        String publicKey = "MFkwEwYHKoZIzj0YIKoZIzj0DAQcDQ";

        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setAlias("My Phone");
        request.setPublicKey(publicKey);

        User user = createUserWithHashedPassword(username, passwordHash);

        // Mock: JWT service extracts username from token
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User EXISTS in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Device does NOT exist yet
        when(deviceRepo.findByUser(user)).thenReturn(Optional.empty());

        // Mock: Device repo saves the device (return device with generated ID)
        when(deviceRepo.save(any(Device.class))).thenAnswer(invocation -> {
            Device savedDevice = invocation.getArgument(0);
            savedDevice.setId(UUID.randomUUID().toString());
            return savedDevice;
        });

        // ============ ACT ============
        DeviceRegisterResponse response =  userService.registerDevice(token, request);

        // ============ ASSERT ============
        // 1. Verify response is not null
        assertNotNull(response);

        // 2. Verify response has device ID
        assertNotNull(response.getDeviceId());

        // 3. Verify success message
        assertEquals("Device registered successfully", response.getMessage());

        // 4. Verify execution flow - all methods called correctly
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(deviceRepo, times(1)).findByUser(user);

        // 5. Verify device was saved with correct data
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepo, times(1)).save(deviceCaptor.capture());

        Device savedDevice = deviceCaptor.getValue();
        assertEquals(user, savedDevice.getUser());
        assertEquals(publicKey, savedDevice.getPublicKey());
    }

    // TEST 11: Register device - device already exists
    @Test
    @DisplayName("Test 11: Register Device - Device already exists")
    void testRegisterDeviceAlreadyExists() {

        // ============ ARRANGE ============
        String username = "alice";
        String token = "mock.jwt.token";
        String passwordHash = "$2a$10$hashedExample";
        String publicKey = "MFkwEwYHKoZIzj0YIKoZIzj0DAQcDQ";

        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setAlias("My Phone");
        request.setPublicKey(publicKey);

        User user = createUserWithHashedPassword(username, passwordHash);

        Device existingDevice = new Device();
        existingDevice.setId(UUID.randomUUID().toString());
        existingDevice.setUser(user);
        existingDevice.setPublicKey(publicKey);

        // Mock: JWT service extracts username from token
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User EXISTS in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Device EXISTS in database
        when(deviceRepo.findByUser(user)).thenReturn(Optional.of(existingDevice));

        // ============ ACT ============
        DeviceRegisterResponse response =  userService.registerDevice(token, request);

        // ============ ASSERT ============
        // 1. Verify response is not null
        assertNotNull(response);

        // 2. Verify response has device ID
        assertNotNull(response.getDeviceId());

        // 3. Verify success message
        assertEquals("Device already exists", response.getMessage());

        // 4. Verify execution flow - all methods called correctly
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(deviceRepo, times(1)).findByUser(user);

        // 5. Same existing device returned
        assertEquals(existingDevice.getId(), response.getDeviceId());

        // 6. Verify NO new device was saved (didn't create duplicate)
        verify(deviceRepo, never()).save(any(Device.class));
    }

    // TEST 12: Register device - invalid token
    @Test
    @DisplayName("Test 12: Register Device Fail - Invalid token")
    void testRegisterDeviceInvalidToken() {
        // ============ ARRANGE ============
        String invalidToken = "invalid.token";
        String publicKey = "MFkwEwYHKoZIzj0YIKoZIzj0DAQcDQ";

        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setAlias("My Phone");
        request.setPublicKey(publicKey);

        // Mock: JWT service throws RuntimeException
        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerDevice(invalidToken, request)
        );

        // The exception message comes directly from JwtService
        assertEquals("Invalid token", exception.getMessage());

        // Verify JWT service was called (proves we attempted token validation)
        verify(jwtService, times(1)).extractUsername(invalidToken);

        // Verify device repo was never accessed
        verify(deviceRepo, never()).findByUser(any(User.class));
        verify(deviceRepo, never()).save(any(Device.class));
    }

    // Helper method to create a user with hashed password
    private User createUserWithHashedPassword(String username, String hashedPassword) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword(hashedPassword);

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10.0);
        card.setUser(user);
        user.setCard(card);

        return user;
    }

}