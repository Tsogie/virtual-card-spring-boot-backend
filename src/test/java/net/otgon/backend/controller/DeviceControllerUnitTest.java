package net.otgon.backend.controller;

import net.otgon.backend.dto.DeviceRegisterRequest;
import net.otgon.backend.dto.DeviceRegisterResponse;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DeviceController Unit Tests")
@ActiveProfiles("test")
public class DeviceControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    //Test data
    String token = "token";
    String deviceId = "deviceId";
    String requestBody = String.format("""
                {
                "alias": "alias",
                "publicKey": "publicKey"
                }
                """);

    //TEST-1 POST api/device/register SUCCESS PATH
    @Test
    @DisplayName("Test-1 Success path")
    void registerDeviceTestSuccessPath() throws Exception {

        //Arrange
        //Expected response
        String message = "Device registered successfully";
        DeviceRegisterResponse response =
                new DeviceRegisterResponse(deviceId, message);
        when(userService.registerDevice(eq(token), any(DeviceRegisterRequest.class)))
                .thenReturn(response);

        //Act and Assess
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId))
                .andExpect(jsonPath("$.message").value(message));
        verify(userService, times(1)).registerDevice(eq(token), any(DeviceRegisterRequest.class));
    }

    //TEST-2 POST api/device/register MISSING AUTH HEADER
    @Test
    @DisplayName("Test-2 Fail: missing auth header")
    void registerDeviceTestMissingAuthHeader() throws Exception{

        //Arrange
        //Act and Assess
        mockMvc.perform(post("/api/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
        verify(userService, never()).registerDevice(any(), any(DeviceRegisterRequest.class));

    }

    //TEST-3 POST api/device/register MISSING FIELD VALUE
    @Test
    @DisplayName("Test-3 Fail: missing field")
    void registerDeviceTestMissingField() throws Exception{

        //Arrange
        String requestBodyIncomplete = String.format("""
                {
                "alias": "alias"
                }
                """);
        //Act and Assess
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyIncomplete))
                .andExpect(status().isBadRequest());
        verify(userService, never()).registerDevice(anyString(), any(DeviceRegisterRequest.class));
    }

    //TEST-4 POST api/device/register invalid token throws exception
    @Test
    @DisplayName("Test-4 Fail: invalid token")
    void registerDeviceTestInvalidToken() throws Exception{

        //Arrange
        String invalidToken = "invalidToken";
        when(userService.registerDevice(eq(invalidToken), any(DeviceRegisterRequest.class)))
                .thenThrow(new RuntimeException("Invalid token"));
        //Act and Assert
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.deviceId").isEmpty())
                .andExpect(jsonPath("$.message").value("Invalid token"));
        verify(userService, times(1))
                .registerDevice(eq(invalidToken), any(DeviceRegisterRequest.class));
    }

    //TEST-5 POST api/device/register USER NOT FOUND
    @Test
    @DisplayName("Test-5 Fail: user not found")
    void  registerDeviceTestNotFound() throws Exception{

        when(userService.registerDevice(eq(token), any(DeviceRegisterRequest.class)))
                .thenThrow(new RuntimeException("User not found"));
        //Act and Assert
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.deviceId").isEmpty())
                .andExpect(jsonPath("$.message").value("User not found"));
        verify(userService, times(1))
                .registerDevice(eq(token), any(DeviceRegisterRequest.class));
    }

    //TEST-6 POST api/device/register DEVICE ALREADY EXISTS
    @Test
    @DisplayName("Test-6 Device already exist")
    void registerDeviceTestAlreadyExist() throws Exception{

        //Arrange
        //Expected response
        String message = "Device already exists";
        DeviceRegisterResponse responseExistingDevice =
                new DeviceRegisterResponse(deviceId, message);
        when(userService.registerDevice(eq(token), any(DeviceRegisterRequest.class)))
                .thenReturn(responseExistingDevice);
        //Act and Assess
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId))
                .andExpect(jsonPath("$.message").value(message));
        verify(userService, times(1)).registerDevice(eq(token), any(DeviceRegisterRequest.class));

    }

}
