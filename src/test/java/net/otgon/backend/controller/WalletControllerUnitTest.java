package net.otgon.backend.controller;

import net.otgon.backend.dto.RedeemDeviceRequestDto;
import net.otgon.backend.dto.RedeemResult;
import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.RedeemService;
import net.otgon.backend.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WalletControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private RedeemService redeemService;

    //Test data
    String deviceId = "deviceId";
    String payload = "payload";
    String signature = "signature";

    String requestBody = String.format("""
                {
                "deviceId": "%s",
                "payload": "%s",
                "signature": "%s"
                }
                """, deviceId, payload, signature);

    double amount = 10;
    String requestBodyTopUp = String.format("""
                {
                "amount": "%s"
                }
                """, amount);

    //TEST-1 POST api/wallet/redeem SUCCESS
    @Test
    @DisplayName("Success Path")
    void testRedeemSuccessPath() throws Exception {

        //Arrange
        //Valid request

        //Expected result
        RedeemResult mockResult = new RedeemResult();
        mockResult.setStatus("SUCCESS");
        mockResult.setNewBalance(10);
        mockResult.setFareDeducted(5);

        when(redeemService.redeem(any(RedeemDeviceRequestDto.class))).thenReturn(mockResult);

        //Act & Assert
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.newBalance").value(10))
                .andExpect(jsonPath("$.fareDeducted").value(5));

        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    //TEST-2 POST api/wallet/redeem INSUFFICIENT FUND
    @Test
    @DisplayName("Fail: insufficient fund")
    void testRedeemFailInsufficientFund() throws Exception {

        //Arrange

        //Expected result
        RedeemResult mockResult = new RedeemResult();
        mockResult.setStatus("Insufficient funds");
        mockResult.setNewBalance(0);
        mockResult.setFareDeducted(5);

        when(redeemService.redeem(any(RedeemDeviceRequestDto.class)))
                .thenReturn(mockResult);
        //Act & Assert
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Insufficient funds"))
                .andExpect(jsonPath("$.newBalance").value(0))
                .andExpect(jsonPath("$.fareDeducted").value(5));

        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    //TEST-3 POST api/wallet/redeem MISSING FIELD
    @Test
    @DisplayName("Fail: missing field")
    void testRedeemFailMissingField() throws Exception {

        //Arrange
        String requestBodyIncomplete = String.format("""
                {
                "payload": "%s",
                "signature": "%s"
                }
                """, payload, signature);
        //Act and Assert
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyIncomplete))
                .andExpect(status().isBadRequest());

        verify(redeemService, never()).redeem(any(RedeemDeviceRequestDto.class));
    }

    //TEST-4 POST api/wallet/redeem INVALID SIGNATURE
    @Test
    @DisplayName("Fail: invalid signature")
    void testRedeemFailInvalidSignature() throws Exception {

        //Arrange
        when(redeemService.redeem(any(RedeemDeviceRequestDto.class)))
                .thenThrow(new RuntimeException("Invalid signature — request tampered"));
        //Act and Assert
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    //TEST-5 PUT /api/wallet/topup SUCCESS PATH
    @Test
    @DisplayName("Success path top up")
    void testTopUpSuccessPath() throws Exception {

        //Arrange
        String token = "token";
        double amount = 10;
        TopUpResponse mockResult = new TopUpResponse();
        mockResult.setSuccess(true);
        mockResult.setNewBalance(10);
        mockResult.setAmount(10);

        when(walletService.topup(token, amount)).thenReturn(mockResult);

        //Act and Assert
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyTopUp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newBalance").value(10))
                .andExpect(jsonPath("$.amount").value(10));
        verify(walletService, times(1)).topup(token, amount);
    }

    //TEST-6 PUT /api/wallet/topup MISSING FIELD
    @Test
    @DisplayName("Fail: missing field")
    void testTopUpFailMissingField() throws Exception {

        //Arrange
        String token = "token";

        //Act
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(walletService, never()).topup(anyString(), anyDouble());
    }
    //TEST-7 PUT /api/wallet/topup INVALID TOKEN
    @Test
    @DisplayName("Fail: invalid token")
    void testTopUpFailInvalidToken() throws Exception {

        //Arrange
        String invalidToken = "token";
        when(walletService.topup(invalidToken,  amount))
                .thenThrow(new RuntimeException("Invalid token"));
        //Act and Assert
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyTopUp))
                .andExpect(status().isBadRequest());
        verify(walletService, times(1)).topup(invalidToken, amount);

    }

    //TEST-8 PUT /api/wallet/topup MISSING HEADER
    @Test
    @DisplayName("Fail: missing auth header")
    void  testTopUpFailMissingAuthHeader() throws Exception {
        //Act and Assert
        mockMvc.perform(put("/api/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyTopUp))
                .andExpect(status().isBadRequest());
        verify(walletService, never()).topup(anyString(), anyDouble());

    }

}
