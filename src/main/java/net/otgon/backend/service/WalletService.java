package net.otgon.backend.service;

import jakarta.transaction.Transactional;
import net.otgon.backend.dto.TopUpResponse;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TransactionRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WalletService {

    private final CardRepo cardRepo;
    private final TransactionRepo transactionRepo;

    public WalletService(CardRepo cardRepo, TransactionRepo transactionRepo) {
        this.cardRepo = cardRepo;
        this.transactionRepo = transactionRepo;
    }

    /** Transactional, Ensures the balance update + transaction logging is atomic.
     * If something fails in the middle, nothing gets saved.**/
    @Transactional
    public TopUpResponse topup(String cardId) {
        double amount = 5;
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        double newBalance = card.getBalance() + amount;
        card.setBalance(newBalance);
        cardRepo.save(card);

        // Log transaction
        Transaction txn = new Transaction();
        txn.setCard(card);
        txn.setType("TOPUP");
        txn.setAmount(amount);
        txn.setTimestamp(LocalDateTime.now());
        transactionRepo.save(txn);

        return new TopUpResponse(true, newBalance);
    }

}
