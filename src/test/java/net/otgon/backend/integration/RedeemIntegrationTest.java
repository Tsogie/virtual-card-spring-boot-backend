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
public class RedeemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    //Test data
    KeyPair keyPair;
    PrivateKey privateKey;
    PublicKey publicKey;
    String publicKeyBase64;
    String username = "alice";
    String txId = UUID.randomUUID().toString();
    double fare = 10;
    long timestamp = System.currentTimeMillis();
    String deviceId;

    @BeforeEach
    void setup() throws Exception{

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        // Register new user
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

        String alias = "alice";
        // Register device
        String token = getToken();
        String requestBodyDevice = String.format("""
                {
                "alias": "%s",
                "publicKey": "%s"
                }
                """, alias, publicKeyBase64);
        String deviceResponse = mockMvc.perform(post("/api/device/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyDevice))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Extract deviceId from response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(deviceResponse);
        deviceId = json.get("deviceId").asText();
    }

    //TEST-1 SUCCESS REDEEM
    @Test
    @DisplayName("Success path redeem")
    public void redeemTestSuccess() throws Exception {

        //Arrange
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        String requestBody = String.format("""
                {
                "deviceId": "%s",
                "payload": "%s",
                "signature": "%s"
                }
                """, deviceId, payloadBase64, signatureBase64);

        //Act
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Success"))
                .andExpect(jsonPath("$.newBalance").value(0))
                .andExpect(jsonPath("$.fareDeducted").value(fare));

    }

    //TEST-2 REDEEM FAIL DEVICE NOT FOUND
    @Test
    @DisplayName("Test-2 Redeem fail: Device not found")
    void redeemTestFailDeviceNotFound() throws Exception {

        //Arrange
        byte[] payloadBytes = createPayload(txId, fare, timestamp);
        byte[] signatureBytes = signPayload(payloadBytes);
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        String nonExistentDeviceId = "nonExistentDeviceId";
        String requestBody = String.format("""
                {
                "deviceId": "%s",
                "payload": "%s",
                "signature": "%s"
                }
                """, nonExistentDeviceId, payloadBase64, signatureBase64);

        //Act
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Device not registered"));
        //"Device not registered"

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

    //Helper method to create payload in bytes
    private byte[] createPayload(String txId, double fare, long timestamp){

        JSONObject payload = new JSONObject();
        payload.put("txId", txId);
        payload.put("fare", fare);
        payload.put("timestamp", timestamp);
        return payload.toString().getBytes(StandardCharsets.UTF_8);

    }

    //Helper method to sign a payload
    private byte[] signPayload(byte[] payload) throws Exception{

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(payload);
        return signature.sign();

    }
}
