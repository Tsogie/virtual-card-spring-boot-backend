package net.otgon.backend.integration;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String requestBody;

    @BeforeEach
    public void setup() throws Exception {
        requestBody = String.format("""
                {
                "username": "alice",
                "password": "password",
                "email": "test@test.com"
                }
                """);
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    //TEST-1 SUCCESS PATH - FULL FLOW OF USER REGISTRATION RETURNS TOKEN
    @Test
    @DisplayName("Test-1 Success registration")
    void registerTestSuccessPath() throws Exception {

        //Arrange
        requestBody = """
                {
                "username": "user",
                "password": "password",
                "email": "user@test.com"
                }
                """;
        //Act
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())));
    }

    //TEST-2 SUCCESS PATH - USER LOGIN WITH VALID CREDENTIALS
    @Test
    @DisplayName("Test-2 Success login")
    void loginTestSuccessPath() throws Exception {

        //Arrange
        String requestBodyLogIn = """
                {
                "username": "alice",
                "password": "password"
                }
                """;

        //Act
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogIn))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())));
    }

    //TEST-3 FAIL WHEN USER WRONG PASSWORD
    @Test
    @DisplayName("Test-3 Fail wrong password")
    void loginTestWrongPassword() throws Exception {
        //Arrange
        String requestBodyLogIn = """
                {
                "username": "alice",
                "password": "wrong.password"
                }
                """;

        //Act
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogIn))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(emptyString())));
    }

    private String getToken() throws Exception {

        String requestLogIn = """
                {
                "username": "alice",
                "password": "password"
                }
                """;

        return mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestLogIn))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    //TEST-4 ACCESS PROTECTED ENDPOINT WITH VALID TOKEN RETURN 200
    @Test
    @DisplayName("Test-4 Success: access protected endpoint with valid token")
    void accessProtectedEndpointWithValidTokenSuccess() throws Exception {

        //Arrange
        String validToken = getToken();

        //Act
        mockMvc.perform(get("/api/userinfo").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())));
    }

    //TEST-5 ACCESS PROTECTED ENDPOINT WITH INVALID TOKEN RETURN 403
    @Test
    @DisplayName("Test-5 Fail: access protected endpoint with invalid token")
    void accessProtectedEndpointWithInvalidToken() throws Exception {

        //Arrange
        String wrongToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.wrongsignature";

        //Act
        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer " + wrongToken))
                .andExpect(status().isForbidden());
    }

    //TEST-6 ACCESS PROTECTED ENDPOINT WITH MALFORMED TOKEN RETURN 403
    @Test
    @DisplayName("Test-6 Fail: access protected endpoint with malformed token")
    void accessProtectedEndpointWithMalformedToken() throws Exception {

        //Arrange
        String malformedToken = "malformedToken";

        //Act
        mockMvc.perform(get("/api/userinfo")
                        .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isForbidden());
    }




}
