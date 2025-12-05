package net.otgon.backend.controller;

import net.otgon.backend.dto.*;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.RedeemService;
import net.otgon.backend.service.WalletService;
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

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WalletController Unit Tests")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private RedeemService redeemService;

    @MockitoBean
    private JwtService jwtService;

    // =====================================================
    // TEST 1: POST /api/wallet/redeem - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Redeem Success - Valid transaction deducts balance")
    void testRedeemSuccess() throws Exception {
        // ============ ARRANGE ============
        // 1. Create valid redeem request
        String deviceId = "device-123";
        String txId = "tx-456";
        String payload = "base64EncodedPayload";
        String signature = "base64EncodedSignature";

        // 2. Create expected success result
        RedeemResult mockResult = new RedeemResult();
        mockResult.setStatus("SUCCESS");
        mockResult.setNewBalance(17.50);
        mockResult.setFareDeducted(2.50); // ← Changed from message

        // 3. Mock service to return success
        when(redeemService.redeem(any(RedeemDeviceRequestDto.class)))
                .thenReturn(mockResult);

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request to /api/wallet/redeem
        // 2. Verify HTTP 200 OK status
        // 3. Verify JSON response contains status, balance, and fareDeducted
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"deviceId\":\"" + deviceId + "\"," +
                                "\"txId\":\"" + txId + "\"," +
                                "\"payload\":\"" + payload + "\"," +
                                "\"signature\":\"" + signature + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.newBalance").value(17.50))
                .andExpect(jsonPath("$.fareDeducted").value(2.50)); // ← Changed

        // 4. Verify service was called once
        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    // =====================================================
    // TEST 2: POST /api/wallet/redeem - INSUFFICIENT FUNDS
    // =====================================================
    @Test
    @DisplayName("Test 2: Redeem Fails - Insufficient funds returns error status")
    void testRedeemInsufficientFunds() throws Exception {
        // ============ ARRANGE ============
        // 1. Create redeem request
        String deviceId = "device-123";
        String txId = "tx-789";

        // 2. Create insufficient funds result
        RedeemResult mockResult = new RedeemResult();
        mockResult.setStatus("Insufficient funds");
        mockResult.setNewBalance(2.00); // Balance too low
        mockResult.setFareDeducted(0.0); // ← No fare deducted (transaction failed)

        // 3. Mock service to return insufficient funds
        when(redeemService.redeem(any(RedeemDeviceRequestDto.class)))
                .thenReturn(mockResult);

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request
        // 2. Verify HTTP 200 OK (not error - just failed transaction)
        // 3. Verify response indicates insufficient funds
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"deviceId\":\"" + deviceId + "\"," +
                                "\"txId\":\"" + txId + "\"," +
                                "\"payload\":\"base64Payload\"," +
                                "\"signature\":\"base64Signature\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Insufficient funds"))
                .andExpect(jsonPath("$.newBalance").value(2.00))
                .andExpect(jsonPath("$.fareDeducted").value(0.0)); // ← Changed

        // 4. Verify service was called
        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    // =====================================================
    // TEST 3: POST /api/wallet/redeem - INVALID SIGNATURE
    // =====================================================
    @Test
    @DisplayName("Test 3: Redeem Fails - Invalid signature throws exception")
    void testRedeemInvalidSignature() throws Exception {
        // ============ ARRANGE ============
        // 1. Create redeem request with tampered signature
        String deviceId = "device-123";

        // 2. Mock service to throw exception for invalid signature
        when(redeemService.redeem(any(RedeemDeviceRequestDto.class)))
                .thenThrow(new RuntimeException("Invalid signature"));

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request with invalid signature
        // 2. Verify HTTP 500 Internal Server Error (unhandled exception)
        // 3. Controller doesn't catch this exception, so it propagates
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"deviceId\":\"" + deviceId + "\"," +
                                "\"txId\":\"tx-invalid\"," +
                                "\"payload\":\"base64Payload\"," +
                                "\"signature\":\"tamperedSignature\"" +
                                "}"))
                .andExpect(status().isInternalServerError());

        // 4. Verify service was called
        verify(redeemService, times(1)).redeem(any(RedeemDeviceRequestDto.class));
    }

    // =====================================================
// TEST 4: POST /api/wallet/redeem - MISSING DEVICE ID
// =====================================================
    @Test
    @DisplayName("Test 4: Redeem Fails - Missing deviceId returns 400")
    void testRedeemMissingDeviceId() throws Exception {
        // ============ ARRANGE ============
        // No mock needed if DTO has @NotBlank validation

        // ============ ACT & ASSERT ============
        // 1. Make HTTP POST request missing required deviceId field
        // 2. Verify HTTP 400 Bad Request (@Valid catches this)
        // 3. Request body only has payload and signature
        mockMvc.perform(post("/api/wallet/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"payload\":\"base64Payload\"," +
                                "\"signature\":\"base64Signature\"" +
                                "}"))
                .andExpect(status().isBadRequest());

        // 4. Verify service was NEVER called (validation failed)
        verify(redeemService, never()).redeem(any(RedeemDeviceRequestDto.class));
    }

    // =====================================================
    // TEST 5: PUT /api/wallet/topup - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 5: Top Up Success - Adds amount to balance")
    void testTopupSuccess() throws Exception {
        // ============ ARRANGE ============
        // 1. Create top-up request
        double topupAmount = 20.0;

        // 2. Create expected response
        TopUpResponse mockResponse = new TopUpResponse(true, 30.0, topupAmount);

        // 3. Mock service to return success
        when(walletService.topup("valid.jwt.token", topupAmount))
                .thenReturn(mockResponse);

        // ============ ACT & ASSERT ============
        // 1. Make HTTP PUT request to /api/wallet/topup
        // 2. Verify HTTP 200 OK status
        // 3. Verify response shows success, new balance, and amount
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.0}")) // ← Fixed: hardcoded value
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newBalance").value(30.0))
                .andExpect(jsonPath("$.amount").value(20.0));

        // 4. Verify service was called with correct parameters
        verify(walletService, times(1)).topup("valid.jwt.token", topupAmount);
    }

    // =====================================================
    // TEST 6: PUT /api/wallet/topup - INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 6: Top Up Fails - Invalid token throws exception")
    void testTopupInvalidToken() throws Exception {
        // ============ ARRANGE ============
        // 1. Create top-up request
        double topupAmount = 20.0;

        // 2. Mock service to throw exception for invalid token
        when(walletService.topup("invalid.token", topupAmount))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        // 1. Make HTTP PUT request with invalid token
        // 2. Verify HTTP 500 Internal Server Error
        // 3. Controller doesn't handle token exceptions
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.0}")) // ← Fixed: hardcoded value
                .andExpect(status().isInternalServerError());

        // 4. Verify service was called
        verify(walletService, times(1)).topup("invalid.token", topupAmount);
    }

    // =====================================================
    // TEST 7: PUT /api/wallet/topup - NEGATIVE AMOUNT
    // =====================================================
    @Test
    @DisplayName("Test 7: Top Up Fails - Negative amount returns 400")
    void testTopupNegativeAmount() throws Exception {
        // ============ ARRANGE ============
        double negativeAmount = -10.0;

        // Mock service to throw ResponseStatusException
        when(walletService.topup("valid.jwt.token", negativeAmount))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Amount must be positive"
                ));

        // ============ ACT & ASSERT ============
        mockMvc.perform(put("/api/wallet/topup")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-10.0}"))
                .andExpect(status().isBadRequest());

        verify(walletService, times(1)).topup("valid.jwt.token", negativeAmount);
    }

    // =====================================================
    // TEST 8: PUT /api/wallet/topup - MISSING AUTHORIZATION HEADER
    // =====================================================
    @Test
    @DisplayName("Test 8: Top Up Fails - Missing auth header returns 400")
    void testTopupMissingHeader() throws Exception {
        // ============ ARRANGE ============
        // No mock needed - Spring catches missing header

        // ============ ACT & ASSERT ============
        // 1. Make HTTP PUT request WITHOUT Authorization header
        // 2. Verify HTTP 400 Bad Request
        // 3. Spring requires @RequestHeader, so request fails at framework level
        mockMvc.perform(put("/api/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.0}"))
                .andExpect(status().isBadRequest());

        // 4. Verify service was NEVER called
        verify(walletService, never()).topup(anyString(), anyDouble());
    }
}