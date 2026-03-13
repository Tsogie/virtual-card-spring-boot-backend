package net.otgon.backend.service;

import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Wallet Service Unit Test")
public class WalletTest {

    @Mock
    UserRepo userRepo;
    @Mock
    CardRepo cardRepo;
    @Mock
    TopUpTransactionRepo topUpTransactionRepo;
    @Mock
    JwtService jwtService;
    @InjectMocks
    WalletService walletService;

    //TEST-1 SUCCESS PATH
    @Test
    @DisplayName("Success path")
    void topupSuccess(){

        //Arrange
        String token = "token";
        double amount = 100;

        String username = "username";
        User user = createUserByUsername(username);
        double balance = user.getCard().getBalance();
        double newBalance = balance + amount;

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        when(cardRepo.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(topUpTransactionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        //Act
        TopUpResponse response = walletService.topup(token, amount);

        //Assert
        assertNotNull(response);

        assertTrue(response.isSuccess());
        assertEquals(amount, response.getAmount());
        assertEquals(newBalance, response.getNewBalance());
        assertEquals(newBalance, user.getCard().getBalance());

        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        ArgumentCaptor<TopUpTransaction> txnCaptor = ArgumentCaptor.forClass(TopUpTransaction.class);
        verify(topUpTransactionRepo, times(1)).save(txnCaptor.capture());
        TopUpTransaction txn = txnCaptor.getValue();

        assertEquals(user.getCard(), txn.getCard());
        assertEquals(amount, txn.getAmount());
        assertNotNull(txn.getCreatedAt());
    }

    //TEST-2 FAIL: INVALID TOKEN

    @Test
    @DisplayName("Fail: topup with invalid token")
    void topupWithInvalidToken(){

        //Arrange
        String invalidToken = "invalid.token";
        double amount = 10;

        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        //Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.topup(invalidToken, amount),
                "Expected Runtime Exception for invalid token");

        assertEquals("Invalid token", exception.getMessage());
        verify(jwtService, times(1)).extractUsername(invalidToken);
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());

    }

    //TEST-3 FAIL: AMOUNT IS NEGATIVE
    @Test
    @DisplayName("Fail: top up with negative amount")
    void topupWithNegativeAmount(){
        //Arrange
        String token = "token";
        double negativeAmount = -1;

        //Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, negativeAmount),
                "Expected ResponseStatusException for negative amount");

        assertEquals("Amount must be positive", exception.getReason());
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());
    }

    //TEST-4 FAIL: AMOUNT EXCEEDS 100
    @Test
    @DisplayName("Fail: top up with exceeded amount")
    void topupWithExceededAmount(){

        //Arrange
        String token = "token";
        double exceededAmount = 101;

        //Act & Assert
        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                ()-> walletService.topup(token, exceededAmount),
                "Expected ResponseStatusException for exceeded amount");
        assertEquals("Amount exceeds maximum (€100)", e.getReason());
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());
    }
    
    // TEST-5 FAIL: TOP UP AMOUNT IS ZERO
    @Test
    @DisplayName("Fail: top up with zero")
    void topupWithAmountZero(){

        //Arrange
        String token = "token";
        double zeroAmount = 0;

        //Act & Assert
        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                ()-> walletService.topup(token, zeroAmount),
                "Expected ResponseStatusException for amount of zero");
        assertEquals("Amount must be positive", e.getReason());
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());
    }

    //TEST-6 FAIL: USER NOT FOUND
    @Test
    @DisplayName("Fail: top up with 100")
    void topupWhenUserNotFound(){

        //Arrange
        String token = "token";
        double amount = 10;
        String username = "alice";

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        //Act & Assert
        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                ()-> walletService.topup(token, amount),
                "Expected ResponseStatusException for user not found");
        assertEquals("User not found", e.getReason());
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());
    }

    //TEST-7 FAIL: CARD NOT FOUND
    @Test
    @DisplayName("Fail: card not found")
    void topupWhenCardNotFound(){

        //Arrange
        String token = "token";
        double amount = 10;
        String username = "alice";
        User user = createUserByUsername(username);
        user.setCard(null);

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        //Act & Arrange
        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topup(token, amount),
                "Expected ResponseStatusException for card not found"
        );

        assertEquals("Card not found for user", e.getReason());
        verify(cardRepo, never()).save(any(Card.class));
        verify(topUpTransactionRepo, never()).save(any());
    }

    User createUserByUsername(String username){
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail("email");

        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setBalance(10);
        card.setUser(user);
        user.setCard(card);
        return user;
    }
}
