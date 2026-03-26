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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for Transaction Service")
@ActiveProfiles("test")
public class TransactionTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private TransactionRepo transactionRepo;
    @Mock
    private TopUpTransactionRepo topUpTransactionRepo;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TransactionService transactionService;

    //TEST-1 SUCCESS PATH WHERE USER HAS 2 TRANSACTIONS (TOP UP, DEDUCTION)
    @Test
    @DisplayName("Success path")
    void getAllUserTransactionsSuccessPath(){

        //Arrange
        String token = "token";
        String username = "alice";
        User user = createUserByUsername(username);
        Card card = user.getCard();

        //Mock transaction
        Transaction tx = new Transaction();
        tx.setCard(card);
        tx.setSyncedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        TopUpTransaction ttx = new TopUpTransaction();
        ttx.setCard(card);
        ttx.setCreatedAt(LocalDateTime.of(2025, 6, 1, 10, 0));

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(transactionRepo.findByCardOrderBySyncedAtDesc(card)).thenReturn(List.of(tx));
        when(topUpTransactionRepo.findByCardOrderByCreatedAtDesc(card)).thenReturn(List.of(ttx));

        //Act
        List<TransactionResponseDto> allTransactions = transactionService.getAllUserTransactions(token);

        //Assert
        assertNotNull(allTransactions);
        assertEquals(2, allTransactions.size());
        assertEquals("TOPUP", allTransactions.get(0).getType());
        assertEquals("DEDUCT", allTransactions.get(1).getType());

        verify(transactionRepo, times(1)).findByCardOrderBySyncedAtDesc(card);
        verify(topUpTransactionRepo, times(1)).findByCardOrderByCreatedAtDesc(card);
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
    }

    //TEST-2 SUCCESS PATH WHERE USER HAS NO TRANSACTION HISTORY
    @Test
    @DisplayName("Success path when user has no transaction yet")
    void getAllUserTransactionsSuccessPathWhenUserHasNoTransactionYet(){

        //Arrange
        String token = "token";
        String username = "alice";
        User user = createUserByUsername(username);
        Card card = user.getCard();

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(transactionRepo.findByCardOrderBySyncedAtDesc(card)).thenReturn(List.of());
        when(topUpTransactionRepo.findByCardOrderByCreatedAtDesc(card)).thenReturn(List.of());

        //Act
        List<TransactionResponseDto> allTransactions = transactionService.getAllUserTransactions(token);

        //Assert
        assertNotNull(allTransactions);
        assertTrue(allTransactions.isEmpty());

        verify(transactionRepo, times(1)).findByCardOrderBySyncedAtDesc(card);
        verify(topUpTransactionRepo, times(1)).findByCardOrderByCreatedAtDesc(card);
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
    }

    //TEST-3 FAIL: INVALID TOKEN THROWS EXCEPTION
    @Test
    @DisplayName("Fail: get all tx with invalid token")
    void getAllUserTransactionsFailWithInvalidToken(){

        //Arrange
        String invalidToken = "invalid.token";
        String username = "alice";

        when(jwtService.extractUsername(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        //Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(invalidToken),
                "Expected RuntimeEx when invalid token" );
        assertEquals("Invalid token", ex.getMessage());
        verify(jwtService, times(1)).extractUsername(invalidToken);
        verify(userRepo, never()).findByUsername(username);
    }

    //TEST-4 FAIL: USER NOT FOUND
    @Test
    @DisplayName("Fail: get all tx fail when user not found")
    void getAllUserTransactionsFailWhenUserNotFound(){

        //Arrange
        String token = "token";
        String username = "alice";

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        //Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(token),
                "Expected RuntimeEx when user not found" );

        assertEquals("User not found", ex.getMessage());
    }

    //TEST-5 FAIL: CARD NOT FOUND
    @Test
    @DisplayName("Fail: card not found even user exists")
    void getAllUserTransactionsFailWhenCardNotFound(){

        //Arrange
        String token = "token";
        String username = "alice";
        User user = createUserByUsername(username);
        user.setCard(null);

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        //Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> transactionService.getAllUserTransactions(token),
                "Expected RuntimeEx when card not found");
        assertEquals("Card not found for user", ex.getMessage());
        verify(jwtService, times(1)).extractUsername(token);
        verify(userRepo, times(1)).findByUsername(username);
        verify(topUpTransactionRepo, never()).findByCardOrderByCreatedAtDesc(any());

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
