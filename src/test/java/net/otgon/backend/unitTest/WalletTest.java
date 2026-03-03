package net.otgon.backend.unitTest;

import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.UserRepo;
import net.otgon.backend.service.JwtService;
import net.otgon.backend.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        double amount = 10;

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
