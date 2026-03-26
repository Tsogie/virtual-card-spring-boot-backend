package net.otgon.backend.controller;

import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransactionController Unit Tests")
@ActiveProfiles("test")
public class TransactionControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    //TEST-1 GET api/transactions
    @Test
    @DisplayName("Success path")
    void getUserTransactionsTestSuccessPath() throws Exception {

        //Arrange
        String token = "token";
        //Expected response
        List<TransactionResponseDto> transactionResponseDtoList = new ArrayList<>();
        transactionResponseDtoList.add(new TransactionResponseDto("id-1", "DEDUCT", 10, 0, LocalDateTime.now(), "SUCCESS"));
        transactionResponseDtoList.add(new TransactionResponseDto("id-2", "TOPUP", 5, 15, LocalDateTime.now(), "SUCCESS"));

        when(transactionService.getAllUserTransactions(token))
                .thenReturn(transactionResponseDtoList);
        //Act and Assert
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("id-1"))
                .andExpect(jsonPath("$[0].balanceAfter").value(0))
                .andExpect(jsonPath("$[1].id").value("id-2"));
        verify(transactionService, times(1)).getAllUserTransactions(token);
    }

    //TEST-2 GET api/transactions MISSING AUTH HEADER - Returns 400
    @Test
    @DisplayName("Fail: missing auth header")
    void getUserTransactionsTestFailMissingAuthHeader() throws Exception {

        //Act
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isBadRequest());
        verify(transactionService, never()).getAllUserTransactions(anyString());
    }

    //TEST-3 GET api/transactions INVALID TOKEN - Returns 401
    @Test
    @DisplayName("Fail: invalid token")
    void getUserTransactionsTestFailInvalidToken() throws Exception {

        //Arrange
        String invalidToken = "token";
        when(transactionService.getAllUserTransactions(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));
        //Act
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
        verify(transactionService, times(1)).getAllUserTransactions(invalidToken);

    }

}
