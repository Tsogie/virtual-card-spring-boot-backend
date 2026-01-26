package net.otgon.backend.controller;

import net.otgon.backend.dto.*;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransactionController Unit Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    // =====================================================
    // TEST 1: Success - Returns transaction list
    // =====================================================
    @Test
    @DisplayName("Test 1: Get Transactions Success - Returns list of transactions")
    void testGetTransactionsSuccess() throws Exception {
        // ============ ARRANGE ============
        List<TransactionResponseDto> mockTransactions = Arrays.asList(
                new TransactionResponseDto("tx1", "DEDUCT", 2.50, 17.50, LocalDateTime.now(), "SUCCESS"),
                new TransactionResponseDto("tx2", "TOPUP", 20.0, 20.0, LocalDateTime.now(), "SUCCESS")
        );

        when(transactionService.getAllUserTransactions("valid.jwt.token"))
                .thenReturn(mockTransactions);

        // ============ ACT & ASSERT ============
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("tx1"))
                .andExpect(jsonPath("$[0].type").value("DEDUCT"))
                .andExpect(jsonPath("$[1].type").value("TOPUP"));

        verify(transactionService, times(1)).getAllUserTransactions("valid.jwt.token");
    }
    // =====================================================
    // TEST 2: Invalid token - Returns 401
    // =====================================================
    @Test
    @DisplayName("Test 2: Get Transactions Fails - Invalid token returns 401")
    void testGetTransactionsInvalidToken() throws Exception {
        // ============ ARRANGE ============
        when(transactionService.getAllUserTransactions("invalid.token"))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());

        verify(transactionService, times(1)).getAllUserTransactions("invalid.token");
    }

    // =====================================================
    // TEST 3: Missing header - Returns 400
    // =====================================================
    @Test
    @DisplayName("Test 3: Get Transactions Fails - Missing auth header returns 400")
    void testGetTransactionsMissingHeader() throws Exception {
      
        // ============ ACT & ASSERT ============
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).getAllUserTransactions(anyString());
    }
}