package net.otgon.backend.service;

import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.repository.CardRepo;
import net.otgon.backend.repository.TransactionRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WalletService {

    private final CardRepo cardRepo;
    private final TransactionRepo transactionRepo;

    public WalletService(CardRepo cardRepo, TransactionRepo transactionRepo) {
        this.cardRepo = cardRepo;
        this.transactionRepo = transactionRepo;
    }

    public String topup(String cardId) {

        double amount = 5;
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));


        double newBalance = card.getBalance() + amount;
        card.setBalance(newBalance);
        cardRepo.save(card);

       //Log transaction
        Transaction txn = new Transaction();
        txn.setCard(card);
        txn.setType("TOPUP");
        txn.setAmount(amount);
        txn.setTimestamp(LocalDateTime.now());
        transactionRepo.save(txn);


        return "Success, new balance: " + newBalance;
    }
}
