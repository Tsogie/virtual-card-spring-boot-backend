package net.otgon.backend.controller;

import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    // =====================================================
    // TEST 1: POST /api/register - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Register Success - Returns JWT token")
    void testRegisterSuccess() throws Exception {
        String mockJwt = "mock.jwt.token.here";

        when(userService.register("alice", "password123", "alice@test.com"))
                .thenReturn(mockJwt);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(mockJwt));

        verify(userService, times(1)).register("alice", "password123", "alice@test.com");
    }

    // =====================================================
    // TEST 2: POST /api/register - DUPLICATE USERNAME
    // =====================================================
    @Test
    @DisplayName("Test 2: Register Fails - Duplicate username returns 400")
    void testRegisterDuplicateUsername() throws Exception {
        when(userService.register("alice", "password123", "alice@test.com"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already exists"));

        verify(userService, times(1)).register("alice", "password123", "alice@test.com");
    }

    // =====================================================
    // TEST 3: POST /api/register - MISSING FIELD
    // =====================================================
    @Test
    @DisplayName("Test 3: Register Fails - Missing password returns 400")
    void testRegisterMissingPassword() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    // =====================================================
    // TEST 4: POST /api/login - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 4: Login Success - Returns JWT token")
    void testLoginSuccess() throws Exception {
        String mockJwt = "mock.jwt.token.here";

        when(userService.loginWithPassword("alice", "password123"))
                .thenReturn(mockJwt);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(mockJwt));

        verify(userService, times(1)).loginWithPassword("alice", "password123");
    }

    // =====================================================
    // TEST 5: POST /api/login - WRONG PASSWORD
    // =====================================================
    @Test
    @DisplayName("Test 5: Login Fails - Wrong password returns 401")
    void testLoginWrongPassword() throws Exception {
        when(userService.loginWithPassword("alice", "wrongpassword"))
                .thenThrow(new RuntimeException("Invalid password"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        verify(userService, times(1)).loginWithPassword("alice", "wrongpassword");
    }

    // =====================================================
    // TEST 6: POST /api/login - USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 6: Login Fails - User not found returns 401")
    void testLoginUserNotFound() throws Exception {
        when(userService.loginWithPassword("nonexistent", "password123"))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        verify(userService, times(1)).loginWithPassword("nonexistent", "password123");
    }

    // =====================================================
    // TEST 7: GET /api/userinfo - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 7: Get User Info Success - Returns user data")
    void testGetUserInfoSuccess() throws Exception {
        String token = "valid.jwt.token";

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", "alice");
        userInfo.put("email", "alice@test.com");
        userInfo.put("cardId", "card-123");
        userInfo.put("balance", 25.50);

        when(userService.getUserInfo(token)).thenReturn(userInfo);

        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.cardId").value("card-123"))
                .andExpect(jsonPath("$.balance").value(25.50));

        verify(userService, times(1)).getUserInfo(token);
    }

    // =====================================================
    // TEST 8: GET /api/userinfo - INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 8: Get User Info Fails - Invalid token returns 401")
    void testGetUserInfoInvalidToken() throws Exception {
        String invalidToken = "invalid.token";

        when(userService.getUserInfo(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing token"));

        verify(userService, times(1)).getUserInfo(invalidToken);
    }

    // =====================================================
    // TEST 9: GET /api/userinfo - MISSING AUTHORIZATION HEADER
    // =====================================================
    @Test
    @DisplayName("Test 9: Get User Info Fails - Missing auth header returns 400")
    void testGetUserInfoMissingHeader() throws Exception {
        mockMvc.perform(get("/api/userinfo"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserInfo(anyString());
    }
}