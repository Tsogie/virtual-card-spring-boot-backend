package net.otgon.backend.integration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import net.minidev.json.JSONObject;
import net.otgon.backend.dto.DeviceRegisterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

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

        // add top up transaction
        double amount = 10;
        String requestBodyTopUp = String.format("""
                {
                "amount": "%s"
                }
                """, amount);
        token = getToken();

        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyTopUp))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Success get all transactions")
    void getAllTransactionsSuccess() throws Exception {

        //Arrange
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("TOPUP"))
                .andExpect(jsonPath("$[0].amount").value(10));
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
