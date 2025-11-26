package net.otgon.backend.controller;

import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Get all transactions (deductions + top-ups) for the authenticated user
     * @param authHeader header
     * @return List of unified transactions sorted by date (newest first)
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponseDto>> getUserTransactions(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "");
            System.out.println("Getting transactions: " + token);

            List<TransactionResponseDto> transactions =
                    transactionService.getAllUserTransactions(token);

            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            System.err.println("Error fetching transactions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).build();
        }
    }
}