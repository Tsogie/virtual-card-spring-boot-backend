package net.otgon.backend.service;

import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.TransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * Unit tests for TransactionService
 * Service. Get all transaction history using user token.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TopUpTransactionRepo topUpTransactionRepo;
    @Mock
    private TransactionRepo transactionRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TransactionService transactionService;

    // =====================================================
    // TEST 1: GET ALL TRANSACTIONS - SUCCESS, SENDS ALL TRANSACTIONS
    // =====================================================
    @Test
    @DisplayName("Test 1: Get Transactions Success - Sends all user transactions")
    void testGetAllTransactionsSuccess() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";

        // Create mock user with card
        User user = new User();
        user.setUsername(username);

        Card card = new Card();
        card.setId("card-123");
        card.setBalance(10.0);
        card.setUser(user);

        user.setCard(card);

        // Create mock deduction transactions
        Transaction txn1 = new Transaction();
        txn1.setId("txn-1");
        txn1.setAmount(2.50);
        txn1.setStatus("SUCCESS");
        txn1.setSyncedAt(LocalDateTime.now().minusHours(2));

        Transaction txn2 = new Transaction();
        txn2.setId("txn-2");
        txn2.setAmount(3.00);
        txn2.setStatus("SUCCESS");
        txn2.setSyncedAt(LocalDateTime.now().minusHours(1));

        List<Transaction> mockTransactions = Arrays.asList(txn1, txn2);

        // Create mock top-up transactions
        TopUpTransaction topup1 = new TopUpTransaction();
        topup1.setId("topup-1");
        topup1.setAmount(20.0);
        topup1.setCreatedAt(LocalDateTime.now().minusHours(3));

        TopUpTransaction topup2 = new TopUpTransaction();
        topup2.setId("topup-2");
        topup2.setAmount(10.0);
        topup2.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        List<TopUpTransaction> mockTopups = Arrays.asList(topup1, topup2);

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User exists in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Transactions found
        when(transactionRepo.findByCardOrderBySyncedAtDesc(card))
                .thenReturn(mockTransactions);

        // Mock: Top-ups found
        when(topUpTransactionRepo.findByCardOrderByCreatedAtDesc(card))
                .thenReturn(mockTopups);

        // ============ ACT ============
        List<TransactionResponseDto> result = transactionService.getAllUserTransactions(token);

        // ============ ASSERT ============
        // 1. Result should not be null
        assertNotNull(result);

        // 2. Should have 4 total transactions (2 deductions + 2 topups)
        assertEquals(4, result.size());

        // 3. Verify transactions are sorted by timestamp (newest first)
        // The most recent should be topup2 (30 min ago)
        assertEquals("topup-2", result.get(0).getId());
        assertEquals("TOPUP", result.get(0).getType());
        assertEquals(10.0, result.get(0).getAmount());

        // 4. Verify DEDUCT type transaction
        TransactionResponseDto deductTxn = result.stream()
                .filter(dto -> dto.getType().equals("DEDUCT"))
                .findFirst()
                .orElse(null);
        assertNotNull(deductTxn);
        assertEquals("SUCCESS", deductTxn.getStatus());

        // 5. Verify TOPUP type transaction
        TransactionResponseDto topupTxn = result.stream()
                .filter(dto -> dto.getType().equals("TOPUP"))
                .findFirst()
                .orElse(null);
        assertNotNull(topupTxn);
        assertEquals("SUCCESS", topupTxn.getStatus());

        // 6. Verify method calls
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(transactionRepo, times(1)).findByCardOrderBySyncedAtDesc(card);
        verify(topUpTransactionRepo, times(1)).findByCardOrderByCreatedAtDesc(card);
    }

    // =====================================================
    // TEST 2: GET ALL TRANSACTIONS - SUCCESS, SENDS EMPTY LIST []
    // =====================================================
    @Test
    @DisplayName("Test 2: Get Transactions Success - Sends empty list []")
    void testGetAllTransactionsEmpty() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";

        // Create mock user with card (new user, no transactions yet)
        User user = new User();
        user.setUsername(username);

        Card card = new Card();
        card.setId("card-123");
        card.setBalance(10.0);
        card.setUser(user);

        user.setCard(card);

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User exists in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: No transactions found (empty lists)
        when(transactionRepo.findByCardOrderBySyncedAtDesc(card))
                .thenReturn(Collections.emptyList());

        when(topUpTransactionRepo.findByCardOrderByCreatedAtDesc(card))
                .thenReturn(Collections.emptyList());

        // ============ ACT ============
        List<TransactionResponseDto> result = transactionService.getAllUserTransactions(token);

        // ============ ASSERT ============
        // 1. Result should not be null
        assertNotNull(result);

        // 2. Result should be empty list (not null)
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());

        // 3. Verify method calls
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(transactionRepo, times(1)).findByCardOrderBySyncedAtDesc(card);
        verify(topUpTransactionRepo, times(1)).findByCardOrderByCreatedAtDesc(card);
    }

    // =====================================================
    // TEST 3: GET ALL TRANSACTIONS - FAIL, INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 3: Get Transactions Fail - Invalid token throws exception")
    void testInvalidToken() {
        // ============ ARRANGE ============
        String invalidToken = "invalid.jwt.token";

        // Mock: JWT service throws exception for invalid token
        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(invalidToken),
                "Expected RuntimeException for invalid token"
        );

        // Verify exception message
        assertEquals("Invalid token", exception.getMessage());

        // Verify we never accessed the database (failed early)
        verify(userRepo, never()).findByUsername(anyString());
    }
    // =====================================================
    // TEST 4: GET ALL TRANSACTIONS - FAIL, USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 4: Get Transactions Fail  - User not found throws exception")
    void testTopupUserNotFound() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User NOT found
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(token),
                "Expected ResponseStatusException for user not found"
        );

        // Verify exception message
        assertEquals("User not found", exception.getMessage());

        // Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
    }

    // =====================================================
    // TEST 5: GET ALL TRANSACTIONS - FAIL, CARD NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 5: Get Transactions Fail  - Card not found throws exception")
    void testTopupCardNotFound() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";

        // Create mock user without card
        User user = new User();
        user.setUsername(username);
        user.setCard(null);

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User exists in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(token),
                "Expected ResponseStatusException for card not found"
        );

        // Verify exception message
        assertEquals("Card not found for user", exception.getMessage());

        // Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);

    }
}
