package net.otgon.backend.repository;

import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, String> {

    boolean existsByTxId(String txId);
    List<Transaction> findByCardOrderBySyncedAtDesc(Card card);
    //List<Transaction> findRecent5Transactions(Card card);
}
