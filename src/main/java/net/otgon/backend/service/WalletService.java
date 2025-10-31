package net.otgon.backend.service;

import net.otgon.backend.entity.Card;
import net.otgon.backend.repository.CardRepo;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final CardRepo cardRepo;

    public WalletService(CardRepo cardRepo) {
        this.cardRepo = cardRepo;
    }

    public String topup(String cardId) {
        Card card = cardRepo.findById(cardId).orElseThrow();
        card.setBalance(card.getBalance() + 5);
        cardRepo.save(card);
        return "Success, new balance: " +  card.getBalance();
    }
}
