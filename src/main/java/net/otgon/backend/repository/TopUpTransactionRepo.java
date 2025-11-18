package net.otgon.backend.repository;

import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopUpTransactionRepo extends JpaRepository<TopUpTransaction, String> {
    List<TopUpTransaction> findByCardOrderByCreatedAtDesc(Card card);
}
