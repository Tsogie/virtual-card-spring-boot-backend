package net.otgon.backend.service;

import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.TransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.*;

@Service
public class TransactionService {

    private final UserRepo userRepo;
    private final TransactionRepo transactionRepo;
    private final TopUpTransactionRepo topUpTransactionRepo;
    private final JwtService jwtService;

    public TransactionService(UserRepo userRepo,
                              TransactionRepo transactionRepo,
                              TopUpTransactionRepo topUpTransactionRepo,
                              JwtService jwtService) {
        this.userRepo = userRepo;
        this.transactionRepo = transactionRepo;
        this.topUpTransactionRepo = topUpTransactionRepo;
        this.jwtService = jwtService;
    }

    public List<TransactionResponseDto> getAllUserTransactions(String jwt) {
        // Extract username from JWT
        String username = jwtService.extractUsername(jwt);

        // 1. Get user and card
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = user.getCard();
        if (card == null) {
            throw new RuntimeException("Card not found for user");
        }

        // 2. Fetch both transaction types
        List<Transaction> deductions = transactionRepo.findByCardOrderBySyncedAtDesc(card);
        List<TopUpTransaction> topUps = topUpTransactionRepo.findByCardOrderByCreatedAtDesc(card);

        // 3. Convert to unified DTOs
        List<TransactionResponseDto> allTransactions = new ArrayList<>();

        // Add deductions
        for (Transaction tx : deductions) {
            TransactionResponseDto dto = new TransactionResponseDto();
            dto.setId(tx.getId());
            dto.setType("DEDUCT");
            dto.setAmount(tx.getAmount());
            dto.setTimestamp(tx.getSyncedAt());
            dto.setStatus(tx.getStatus());
            dto.setBalanceAfter(0.0); 
            allTransactions.add(dto);
        }

        // Add top-ups
        for (TopUpTransaction tx : topUps) {
            TransactionResponseDto dto = new TransactionResponseDto();
            dto.setId(tx.getId());
            dto.setType("TOPUP");
            dto.setAmount(tx.getAmount());
            dto.setTimestamp(tx.getCreatedAt());
            dto.setStatus("SUCCESS");
            dto.setBalanceAfter(0.0);
            allTransactions.add(dto);
        }

        // 4. Sort by timestamp (newest first)
        allTransactions.sort(Comparator.comparing(
                TransactionResponseDto::getTimestamp).reversed());

        return allTransactions;
    }
}