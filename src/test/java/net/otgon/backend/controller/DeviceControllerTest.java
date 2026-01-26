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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DeviceController Unit Tests")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    // =====================================================
    // TEST 1: POST /api/device/register - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Register Device Success - Returns device ID and success message")
    void testRegisterDeviceSuccess() throws Exception {
        // ============ ARRANGE ============
        // 1. Create mock request data
        String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...";
        String alias = "My Phone";

        // 2. Create expected response
        DeviceRegisterResponse mockResponse = new DeviceRegisterResponse(
                "device-123",
                "Device registered successfully"
        );

        // 3. Mock service to return success response
        when(userService.registerDevice(eq("valid.jwt.token"), any(DeviceRegisterRequest.class)))
                .thenReturn(mockResponse);

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request to /api/device/register
        // 2. Verify HTTP 200 OK status
        // 3. Verify JSON response contains deviceId and message
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + publicKey + "\",\"alias\":\"" + alias + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.message").value("Device registered successfully"));

        // 4. Verify service was called once with correct token
        verify(userService, times(1)).registerDevice(eq("valid.jwt.token"), any(DeviceRegisterRequest.class));
    }

    // =====================================================
    // TEST 2: POST /api/device/register - DEVICE ALREADY EXISTS
    // =====================================================
    @Test
    @DisplayName("Test 2: Register Device - Device already exists returns existing device ID")
    void testRegisterDeviceAlreadyExists() throws Exception {
        // ============ ARRANGE ============
        // 1. Create mock request data
        String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...";

        // 2. Create response for existing device
        DeviceRegisterResponse mockResponse = new DeviceRegisterResponse(
                "device-456",
                "Device already exists"
        );

        // 3. Mock service to return existing device response
        when(userService.registerDevice(eq("valid.jwt.token"), any(DeviceRegisterRequest.class)))
                .thenReturn(mockResponse);

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request
        // 2. Verify HTTP 200 OK 
        // 3. Verify response contains existing deviceId and appropriate message
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + publicKey + "\",\"alias\":\"My Phone\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-456"))
                .andExpect(jsonPath("$.message").value("Device already exists"));

        // 4. Verify service was called
        verify(userService, times(1)).registerDevice(eq("valid.jwt.token"), any(DeviceRegisterRequest.class));
    }

    // =====================================================
    // TEST 3: POST /api/device/register - INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 3: Register Device Fails - Invalid token returns 401")
    void testRegisterDeviceInvalidToken() throws Exception {
        // ============ ARRANGE ============
        // 1. Create request data
        String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...";

        // 2. Mock service to throw exception for invalid token
        when(userService.registerDevice(eq("invalid.token"), any(DeviceRegisterRequest.class)))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request with invalid token
        // 2. Verify HTTP 401 Unauthorized status
        // 3. Verify error message in response body
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + publicKey + "\",\"alias\":\"My Phone\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.deviceId").isEmpty())
                .andExpect(jsonPath("$.message").value("Error: Invalid token"));

        // 4. Verify service was called 
        verify(userService, times(1)).registerDevice(eq("invalid.token"), any(DeviceRegisterRequest.class));
    }

    // =====================================================
    // TEST 4: POST /api/device/register - USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 4: Register Device Fails - User not found returns 401")
    void testRegisterDeviceUserNotFound() throws Exception {
        // ============ ARRANGE ============
        // 1. Create request data
        String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...";

        // 2. Mock service to throw exception when user doesn't exist
        when(userService.registerDevice(eq("valid.token"), any(DeviceRegisterRequest.class)))
                .thenThrow(new RuntimeException("User not found"));

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request
        // 2. Verify HTTP 401 status
        // 3. Verify error message indicates user not found
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + publicKey + "\",\"alias\":\"My Phone\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.deviceId").isEmpty())
                .andExpect(jsonPath("$.message").value("Error: User not found"));

        // 4. Verify service was called
        verify(userService, times(1)).registerDevice(eq("valid.token"), any(DeviceRegisterRequest.class));
    }

    // =====================================================
    // TEST 5: POST /api/device/register - MISSING AUTHORIZATION HEADER
    // =====================================================
    @Test
    @DisplayName("Test 5: Register Device Fails - Missing auth header returns 400")
    void testRegisterDeviceMissingHeader() throws Exception {
        // ============ ARRANGE ============
        // 1. Create request data
        String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...";

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request WITHOUT Authorization header
        // 2. Verify HTTP 400 Bad Request
        // 3. Spring catches this before reaching controller method
        mockMvc.perform(post("/api/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + publicKey + "\",\"alias\":\"My Phone\"}"))
                .andExpect(status().isBadRequest());

        // 4. Verify service was NEVER called 
        verify(userService, never()).registerDevice(anyString(), any(DeviceRegisterRequest.class));
    }

    // =====================================================
    // TEST 6: POST /api/device/register - MISSING PUBLIC KEY
    // =====================================================
    @Test
    @DisplayName("Test 6: Register Device Fails - Missing publicKey returns 400")
    void testRegisterDeviceMissingPublicKey() throws Exception {
        // ============ ARRANGE ============
        // No mock needed - validation happens before service call

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request with missing publicKey field
        // 2. Verify HTTP 400 Bad Request 
        // 3. Request body only has alias, missing required publicKey
        mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"My Phone\"}"))
                .andExpect(status().isBadRequest());

        // 4. Verify service was NEVER called
        verify(userService, never()).registerDevice(anyString(), any(DeviceRegisterRequest.class));
    }
}