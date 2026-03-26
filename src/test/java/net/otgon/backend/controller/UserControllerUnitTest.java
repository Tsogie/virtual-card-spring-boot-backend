package net.otgon.backend.controller;

import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    //test data
    String username = "alice";
    String email = "alice@test.com";
    String password = "password";
    String requestBody = String.format("""
            {
              "username": "%s",
              "password": "%s",
              "email": "%s"
            }
            """, username, password, email);
    //TEST-1 POST /api/register SUCCESS REGISTRATION
    @Test
    @DisplayName("Success path")
    void testRegisterSuccess() throws Exception {

        //Arrange
        String token = "mock.token";
        when(userService.register(username, password, email)).thenReturn(token);

        //Act & Assert
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string(token));

        verify(userService, times(1)).register(username, password, email);
    }

    //TEST-2 POST /api/register DUPLICATE USERNAME
    @Test
    @DisplayName("Fail: duplicate username")
    void testRegisterDuplicateUsername() throws Exception {

        //Arrange
        when(userService.register(username, password, email))
                .thenThrow(new RuntimeException("Username already exists"));
        //Act & Arrange
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already exists"));
        verify(userService, times(1)).register(username, password, email);
    }

    //TEST-3  POST /api/register MISSING FIELD
    @Test
    @DisplayName("Fail: missing field")
    void testRegisterMissingField() throws Exception {

        //Arrange
        String requestBodyIncomplete = String.format("""
            {
              "username": "%s",
              "email": "%s"
            }
            """, username, email);

        //Act and Assert
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyIncomplete))
                .andExpect(status().isBadRequest());
        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    //TEST-4 POST /api/login SUCCESS PATH
    @Test
    @DisplayName("Success path log in")
    void testLoginSuccess() throws Exception {

        //Arrange
        String token = "mock.token";
        String requestBodyLogin = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, username, password);
        when(userService.loginWithPassword(username, password)).thenReturn(token);

        //Act and Assert
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogin))
                .andExpect(status().isOk())
                .andExpect(content().string(token));
        verify(userService, times(1)).loginWithPassword(username, password);
    }

    //TEST-5 POST /api/login FAIL WRONG PASSWORD
    @Test
    @DisplayName("Fail: wrong password")
    void testLoginWrongPassword() throws Exception {

        //Arrange
        String requestBodyLogin = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, username, password);
        when(userService.loginWithPassword(username, password))
                .thenThrow(new RuntimeException("Invalid password"));

        //Act and Arrange
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
        verify(userService, times(1)).loginWithPassword(username, password);
    }

    //TEST-6 USER NOT FOUND
    @Test
    @DisplayName("Fail: user not found")
    void testLoginUserNotFound() throws Exception {

        //Arrange
        String requestBodyLogin = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, username, password);

        when(userService.loginWithPassword(username, password))
                .thenThrow(new RuntimeException("User not found"));

        //Act and Arrange
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogin))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));

        verify(userService, times(1))
                .loginWithPassword(username, password);
    }

    //TEST-7 GET /api/userinfo SUCCESS PATH
    @Test
    @DisplayName("Success path get user info")
    void testGetUserInfoSuccess() throws Exception {

        //Arrange
        String token = "mock.token";

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("email", email);
        userInfo.put("cardId", "card-123");
        userInfo.put("balance", 10);

        when(userService.getUserInfo(token)).thenReturn(userInfo);

        //Act and Arrange
        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.cardId").value("card-123"))
                .andExpect(jsonPath("$.balance").value(10));
        verify(userService, times(1)).getUserInfo(token);
    }

    //TEST-8 GET /api/userinfo INVALID TOKEN
    @Test
    @DisplayName("Fail: invalid token")
    void testGetUserInfoInvalidToken() throws Exception {

        //Arrange
        String invalidToken = "mock.token";
        when(userService.getUserInfo(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        //Act and Arrange
        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing token"));

        verify(userService, times(1)).getUserInfo(invalidToken);
    }

    //TEST-9 MISSING AUTH HEADER
    @Test
    @DisplayName("Fail: missing auth header")
    void testGetUserInfoMissingAuthHeader() throws Exception {

        //Act and Assert
        mockMvc.perform(get("/api/userinfo"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).getUserInfo(anyString());
    }

}
