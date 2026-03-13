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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        String registerBody = """
                {
                "username": "alice",
                "password": "password",
                "email": "test@test.com"
                }
                """;
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));
    }

    //TEST-1 SUCCESS FLOW WHEN TOP UP
    @Test
    @DisplayName("Success top up")
    void topUpTestSuccess() throws Exception {

        //Arrange
        double amount = 10;
        String requestBodyTopUp = String.format("""
                {
                "amount": "%s"
                }
                """, amount);
        String token = getToken();

        //Act
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyTopUp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.newBalance").value(20));;
    }

    //TEST-2 FAIL WHEN TOKEN IS INVALID
    @Test
    @DisplayName("Test-2 Fail: token is invalid")
    void topUpFailInvalidToken() throws Exception {

        //Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.wrongsignature";
        double amount = 10;
        String requestBodyTopUp = String.format("""
                {
                "amount": "%s"
                }
                """, amount);

        //Act
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyTopUp))
                .andExpect(status().isForbidden());
    }
    //TEST-3 FAIL WHEN EXCEEDED AMOUNT
    @Test
    @DisplayName("Test-3 Fail: exceeded amount")
    void topUpFailAmountExceeded() throws Exception {

        //Arrange

        double amount = 101;
        String requestBodyTopUp = String.format("""
                {
                "amount": "%s"
                }
                """, amount);
        String token = getToken();

        //Act
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyTopUp))
                .andExpect(status().isBadRequest());
    }

    private String getToken() throws Exception {

        String requestBodyLogIn = """
                {
                "username": "alice",
                "password": "password"
                }
                """;
        return mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLogIn))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
