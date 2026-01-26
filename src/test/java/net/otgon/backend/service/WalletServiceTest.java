package net.otgon.backend.service;

import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private CardRepo cardRepo;
    @Mock
    private TopUpTransactionRepo topUpTransactionRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private WalletService walletService;

    // =====================================================
    // TEST 1: TOP UP - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Top up Success - Adds amount to balance and saves transaction")
    void testTopupSuccess() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";
        // Test edge case for 100
        double topupAmount = 100.0;
        double initialBalance = 10.0;

        // Create mock user with card
        User user = new User();
        user.setUsername(username);

        Card card = new Card();
        card.setId("card-123");
        card.setBalance(initialBalance);
        card.setUser(user);

        user.setCard(card);

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User exists in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // Mock: Card saved successfully 
        when(cardRepo.save(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock: Transaction saved successfully
        when(topUpTransactionRepo.save(any(TopUpTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ============ ACT ============
        TopUpResponse response = walletService.topup(token, topupAmount);

        // ============ ASSERT ============
        // 1. Response should be successful
        assertTrue(response.isSuccess());

        // 2. New balance should be initial + topup (€10 + €20 = €30)
        assertEquals(110.0, response.getNewBalance());
        assertEquals(topupAmount, response.getAmount());

        // 3. Card balance should be updated
        assertEquals(110.0, card.getBalance());

        // 4. Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(cardRepo, times(1)).save(card);

        // 5. Verify transaction was saved with correct data
        ArgumentCaptor<TopUpTransaction> txnCaptor = ArgumentCaptor.forClass(TopUpTransaction.class);
        verify(topUpTransactionRepo, times(1)).save(txnCaptor.capture());

        TopUpTransaction savedTxn = txnCaptor.getValue();
        assertEquals(card, savedTxn.getCard());
        assertEquals(topupAmount, savedTxn.getAmount());
        assertNotNull(savedTxn.getCreatedAt());
    }

    // =====================================================
    // TEST 2: TOP UP - FAIL, INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 2: Top up Fails - Invalid token throws exception")
    void testTopupInvalidToken() {
        // ============ ARRANGE ============
        String invalidToken = "invalid.jwt.token";
        double topupAmount = 20.0;

        // Mock: JWT service throws exception for invalid token
        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.topup(invalidToken, topupAmount),
                "Expected RuntimeException for invalid token"
        );

        // Verify exception message
        assertEquals("Invalid token", exception.getMessage());

        // Verify we never accessed the database 
        verify(userRepo, never()).findByUsername(anyString());
    }

    // =====================================================
    // TEST 3: TOP UP - FAIL, USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 3: Top up Fails - User not found throws exception")
    void testTopupUserNotFound() {
        // User deleted somehow
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";
        double topupAmount = 20.0;

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User NOT found
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        // ============ ACT & ASSERT ============
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, topupAmount),
                "Expected ResponseStatusException for user not found"
        );

        assertEquals("User not found", exception.getReason());

        // Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
    }

    // =====================================================
    // TEST 4: TOP UP - FAIL, CARD NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Test 4: Top up Fails - Card not found throws exception")
    void testTopupCardNotFound() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        String username = "alice";
        double topupAmount = 20.0;

        // Create mock user without card
        User user = new User();
        user.setUsername(username);
        user.setCard(null);

        // Mock: JWT extracts username
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Mock: User exists in database
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        // ============ ACT & ASSERT ============
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, topupAmount),
                "Expected ResponseStatusException for card not found"
        );

        // Verify exception message
        assertEquals("Card not found for user", exception.getReason());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        // Verify methods were called
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);

    }

    // =====================================================
    // TEST 5: TOP UP - FAIL, NEGATIVE AMOUNT
    // =====================================================
    @Test
    @DisplayName("Test 5: Top up Fails - Negative amount throws exception")
    void testTopupNegativeAmount() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        double negativeTopupAmount = -20.0;

        // ============ ACT & ASSERT ============
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, negativeTopupAmount),
                "Expected ResponseStatusException for negative amount"
        );

        // Verify exception message
        assertEquals("Amount must be positive", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

    }

    // =====================================================
    // TEST 6: TOP UP - FAIL, AMOUNT EXCEEDS MAXIMUM
    // =====================================================
    @Test
    @DisplayName("Test 6: Top up Fails - Amount exceeds maximum amount throws exception")
    void testTopupExceedsMaximum() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        double exceededTopupAmount = 101.0;

        // ============ ACT & ASSERT ============
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, exceededTopupAmount),
                "Expected ResponseStatusException for exceeded amount"
        );

        // Verify exception message
        assertEquals("Amount exceeds maximum (€100)", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // =====================================================
    // TEST 7: TOP UP - FAIL, AMOUNT EQUALS ZERO
    // =====================================================
    @Test
    @DisplayName("Test 7: Top up Fails - Amount zero throws exception")
    void testTopupZeroAmount() {
        // ============ ARRANGE ============
        String token = "valid.jwt.token";
        double zeroTopupAmount = 0.0;

        // ============ ACT & ASSERT ============
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, zeroTopupAmount),
                "Expected ResponseStatusException for negative amount"
        );

        // Verify exception message
        assertEquals("Amount must be positive", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

}