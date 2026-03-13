package net.otgon.backend.service;

import net.otgon.backend.dto.DeviceRegisterRequest;
import net.otgon.backend.dto.DeviceRegisterResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Device;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.DeviceRepo;
import net.otgon.backend.repository.UserRepo;
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
@DisplayName("User service tests")
public class UserTest {

    @Mock
    UserRepo userRepo;

    @Mock
    DeviceRepo deviceRepo;

    @Mock
    JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    //TEST-1 SUCCESS PATH
    @Test
    @DisplayName("Success path")
    void userRegisterSuccess() {

        //Arrange
        String username = "alice";
        String password = "password";
        String email = "email";
        String hashedPassword = "hashedPassword";

        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(username)).thenReturn("generated.token");

        //Act
        String token = userService.register(username, password, email);

        //Assert
        assertNotNull(token);
        assertEquals("generated.token", token);

        verify(userRepo, times(1)).findByUsername(username);
        verify(userRepo, times(1)).findByEmail(email);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(hashedPassword, savedUser.getPassword());
        assertEquals(email, savedUser.getEmail());

        assertNotNull(savedUser.getCard());
        assertEquals(10, savedUser.getCard().getBalance());

        verify(passwordEncoder, times(1)).encode(password);
    }

    //TEST-2 FAIL: USERNAME ALREADY EXISTS

    @Test
    @DisplayName("Fail: Username exists")
    void registerWithExistingUser() {

        //Arrange
        String username = "alice";
        String password = "password";
        String email = "email";

        User existingUser = new User();
        existingUser.setUsername(username);

        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(username, password, email),
                "Expected RuntimeException for duplicate username"
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepo, times(1)).findByUsername(username);
        verify(userRepo, never()).save(any(User.class));

        verify(passwordEncoder, never()).encode(anyString());
    }

    //TEST-3 FAIL: EMAIL ALREADY EXISTS

    @Test
    @DisplayName("Fail: email exists")
    void registerWithExistingEmail(){

        //Arrange
        String username = "alice";
        String password = "password";
        String email = "email";

        User existingUser = new User();
        existingUser.setEmail(email);

        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));

        //Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.register(username, password, email),
                "Expected RuntimeException for duplicate email");

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepo, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    //TEST-4 LOG IN SUCCESS WITH CORRECT PASSWORD

    @Test
    @DisplayName("Successful log in")
    void loginSuccess(){

        //Arrange
        String password = "password";
        String username = "alice";

        User existingUser = new User();
        existingUser.setUsername("alice");
        existingUser.setPassword("correctHashedPassword");

        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(password, existingUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(username)).thenReturn("generated.token");

        //Act
        String token = userService.loginWithPassword(username, password);

        //Assert
        assertNotNull(token);
        assertEquals("generated.token", token);

        verify(userRepo, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password,  existingUser.getPassword());
        verify(jwtService, times(1)).generateToken(username);
    }

    //TEST-5 FAIL: INCORRECT PASSWORD WHEN LOG IN

    @Test
    @DisplayName("Fail: log in with wrong password")
    void loginWithWrongPassword(){

        //Arrange
        String wrongPassword = "password";
        String username = "alice";

        User existingUser = new User();
        existingUser.setUsername("alice");
        existingUser.setPassword("correctHashedPassword");

        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(wrongPassword, existingUser.getPassword())).thenReturn(false);

        //Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.loginWithPassword(username, wrongPassword),
                "Expected RuntimeException for wrong password");

        assertEquals("Invalid password", exception.getMessage());
        verify(userRepo, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(wrongPassword, existingUser.getPassword());
        verify(jwtService, never()).generateToken(username);
    }

    //TEST-6 FAIL: USERNAME NOT FOUND WHEN LOG IN

    @Test
    @DisplayName("Fail: log in with non existing username")
    void logInWithNotExistingUsername(){

        //Arrange
        String username = "notExistingUsername";
        String password = "password";

        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        //Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.loginWithPassword(username, password),
                "Expected RuntimeException for invalid username");

        assertEquals("User not found", exception.getMessage());
        verify(userRepo, times(1)).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString());
    }

    //TEST-7 SUCCESS: GET USER INFO WITH VALID TOKEN

    @Test
    @DisplayName("Success: get user info with valid token")
    void getUserInfoSuccess(){

        //Arrange
        String username = "alice";
        User user = createUserByUsername(username);
        String email = user.getEmail();
        String cardId = user.getCard().getId();
        double balance = user.getCard().getBalance();
        String validToken = "valid.token";

        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        //Act
        Map<String, Object> info = userService.getUserInfo(validToken);

        //Assert
        assertNotNull(info);
        assertEquals(username, info.get("username"));
        assertEquals(email, info.get("email"));
        assertEquals(cardId, info.get("cardId"));
        assertEquals(balance, info.get("balance"));

        verify(jwtService, times(1)).extractUsername(validToken);
        verify(userRepo, times(1)).findByUsername(username);
    }

    //TEST-8 FAIL: GET USER INFO WITH INVALID TOKEN

    @Test
    @DisplayName("Fail: get user info with invalid token")
    void getUserInfoWithInvalidToken(){

        //Arrange
        String invalidToken = "invalid.token";
        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                ()->userService.getUserInfo(invalidToken),
                "Expected RuntimeException for invalid token");

        assertEquals("Invalid token", exception.getMessage());
        verify(jwtService, times(1)).extractUsername(invalidToken);
        verify(userRepo, never()).findByUsername(anyString());

    }

    //TEST-9 FAIL: GET USER INFO WITH VALID TOKEN, BUT USER NOT FOUND

    @Test
    @DisplayName("Fail: user not found with valid token")
    void getUserInfoWhenUserExistsNoMore(){

        //Arrange
        String username = "alice";
        String validToken = "valid.token";
        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenThrow(new RuntimeException("User not found"));

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () ->  userService.getUserInfo(validToken),
        "Expected RuntimeException for user not found in database"
        );

        assertEquals("User not found", exception.getMessage());
        verify(jwtService, times(1)).extractUsername(validToken);
        verify(userRepo, times(1)).findByUsername(username);
    }

    //TEST-10 SUCCESS: DEVICE REGISTER
    @Test
    @DisplayName("Success: device register")
    void deviceRegisterSuccess(){

        //Arrange
        String validToken = "valid.token";
        String publicKey = "publicKey";
        DeviceRegisterRequest deviceRegisterRequest = new DeviceRegisterRequest();
        deviceRegisterRequest.setAlias("alias");
        deviceRegisterRequest.setPublicKey(publicKey);

        String username = "alice";
        User user = createUserByUsername(username);

        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(deviceRepo.findByUser(user)).thenReturn(Optional.empty());
        when(deviceRepo.save(any(Device.class))).thenAnswer(invocation -> {
            Device savedDevice = invocation.getArgument(0);
            savedDevice.setId(UUID.randomUUID().toString());
            return savedDevice;
        });

        //Act
        DeviceRegisterResponse response = userService.registerDevice(validToken, deviceRegisterRequest);

        //Assert
        assertNotNull(response);
        assertNotNull(response.getDeviceId());
        assertEquals("Device registered successfully", response.getMessage());

        verify(jwtService, times(1)).extractUsername(validToken);
        verify(userRepo, times(1)).findByUsername(username);
        verify(deviceRepo, times(1)).findByUser(user);
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepo, times(1)).save(deviceCaptor.capture());

        Device savedDevice = deviceCaptor.getValue();
        assertEquals(user, savedDevice.getUser());
        assertEquals(publicKey, savedDevice.getPublicKey());

    }

    //TEST-11 SUCCESS: DEVICE REGISTER WHEN USER HAS EXISTING SAME DEVICE

    @Test
    @DisplayName("Success: no change if same device is already registered")
    void registerDeviceWhenUserHasSameDeviceId(){

        //Arrange
        String validToken = "valid.token";
        String publicKey = "publicKey";
        DeviceRegisterRequest deviceRegisterRequest = new DeviceRegisterRequest();
        deviceRegisterRequest.setAlias("alias");
        deviceRegisterRequest.setPublicKey(publicKey);

        String username = "alice";
        User user = createUserByUsername(username);

        Device existingDevice = new Device();
        existingDevice.setId(UUID.randomUUID().toString());
        existingDevice.setUser(user);
        existingDevice.setPublicKey(publicKey);

        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(deviceRepo.findByUser(user)).thenReturn(Optional.of(existingDevice));

        //Act
        DeviceRegisterResponse response = userService.registerDevice(validToken, deviceRegisterRequest);

        //Assert
        assertNotNull(response);
        assertNotNull(response.getDeviceId());
        assertEquals("Device already exists", response.getMessage());

        assertEquals(existingDevice.getId(), response.getDeviceId());
        verify(deviceRepo, never()).save(any(Device.class));
    }

    //TEST-12 SUCCESS: REGISTER DEVICE WHEN USER HAS EXISTING DIFFERENT DEVICE BY REPLACING

    @Test
    @DisplayName("Success: Register Device by replacing old device")
    void resiterDeviceByReplacingExistingDevice(){

        String validToken = "valid.token";
        String publicKey = "publicKey";
        String publicKeyOld = "publicKeyOld";

        DeviceRegisterRequest deviceRegisterRequest = new DeviceRegisterRequest();
        deviceRegisterRequest.setAlias("alias");
        deviceRegisterRequest.setPublicKey(publicKey);

        String username = "alice";
        User user = createUserByUsername(username);

        Device existingDevice = new Device();
        existingDevice.setId(UUID.randomUUID().toString());
        existingDevice.setUser(user);
        existingDevice.setPublicKey(publicKeyOld);

        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(deviceRepo.findByUser(user)).thenReturn(Optional.of(existingDevice));
        when(deviceRepo.save(any(Device.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        //Act
        DeviceRegisterResponse response = userService.registerDevice(validToken, deviceRegisterRequest);

        //Assert
        assertNotNull(response);
        assertNotNull(response.getDeviceId());
        assertEquals("Old device replaced with new one", response.getMessage());

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepo, times(1)).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertEquals(user, savedDevice.getUser());
        assertEquals(publicKey, savedDevice.getPublicKey());
    }

    //TEST-13 FAIL: INVALID TOKEN

    @Test
    @DisplayName("Fail: register device with invalid token")
    void registerDeviceWithInvalidToken(){

        //Arrange
        String inValidToken = "invalid.token";
        String publicKey = "publicKey";

        DeviceRegisterRequest deviceRegisterRequest = new DeviceRegisterRequest();
        deviceRegisterRequest.setAlias("alias");
        deviceRegisterRequest.setPublicKey(publicKey);

        when(jwtService.extractUsername(inValidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        //Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerDevice(inValidToken, deviceRegisterRequest),
                "Expected RuntimeException for invalid token");

        assertEquals("Invalid token", ex.getMessage());
        verify(deviceRepo, never()).save(any(Device.class));

    }

    User createUserByUsername(String username){
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail("email");

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10);
        card.setUser(user);
        user.setCard(card);
        return user;
    }
}
