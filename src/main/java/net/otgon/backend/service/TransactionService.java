package net.otgon.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.entity.Card;
import net.otgon.backend.entity.TopUpTransaction;
import net.otgon.backend.entity.Transaction;
import net.otgon.backend.entity.User;
import net.otgon.backend.repository.TopUpTransactionRepo;
import net.otgon.backend.repository.TransactionRepo;
import net.otgon.backend.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    // ✅ SAME SECRET AS UserService
    private static final String SECRET = "mySuperSecretKeyForJWTSigning12345";
    private final Key secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());

    private final UserRepo userRepo;
    private final TransactionRepo transactionRepo;
    private final TopUpTransactionRepo topUpTransactionRepo;

    public TransactionService(UserRepo userRepo,
                              TransactionRepo transactionRepo,
                              TopUpTransactionRepo topUpTransactionRepo) {
        this.userRepo = userRepo;
        this.transactionRepo = transactionRepo;
        this.topUpTransactionRepo = topUpTransactionRepo;
    }

    public List<TransactionResponseDto> getAllUserTransactions(String jwt) {
        // Extract username from JWT
        String username;
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwt);

            username = claimsJws.getBody().getSubject();
        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }

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
            dto.setBalanceAfter(0.0); // We'll calculate this later if needed
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