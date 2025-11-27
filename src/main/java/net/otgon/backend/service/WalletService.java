package net.otgon.backend.service;

import jakarta.transaction.Transactional;
import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.TransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WalletService {

    private final UserRepo userRepo;
    private final CardRepo cardRepo;
    private final TopUpTransactionRepo  topUpTransactionRepo;
    private final JwtService jwtService;

    public WalletService(CardRepo cardRepo,
                         TopUpTransactionRepo topUpTransactionRepo,
                         UserRepo userRepo,
                         JwtService jwtService) {
        this.cardRepo = cardRepo;
        this.topUpTransactionRepo = topUpTransactionRepo;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
    }

    /** Transactional, Ensures the balance update + transaction logging is atomic.
     * If something fails in the middle, nothing gets saved.**/
    @Transactional
    public TopUpResponse topup(String token, double amount) {

        // 1. Validate amount first
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount must be positive");
        }
        if (amount > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount exceeds maximum (€100)");
        }

        // 2. Extract username from JWT
        String username = jwtService.extractUsername(token);

        // 3. Find user and their card
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found"));

        Card card = user.getCard();
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Card not found for user");
        }

        // 4. Update balance
        double newBalance = card.getBalance() + amount;
        card.setBalance(newBalance);
        cardRepo.save(card);

        // 5. Save top-up transaction
        TopUpTransaction txn = new TopUpTransaction();
        txn.setCard(card);
        txn.setAmount(amount);
        txn.setCreatedAt(LocalDateTime.now());
        topUpTransactionRepo.save(txn);

        return new TopUpResponse(true, newBalance, amount);
    }

}
