package net.otgon.backend.controller;

import net.otgon.backend.dto.TransactionResponseDto;
import net.otgon.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Get all transactions (deductions + top-ups) for the authenticated user
     * @param token JWT token from Authorization header
     * @return List of unified transactions sorted by date (newest first)
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> getUserTransactions(
            @RequestHeader("Authorization") String token) {

        // Extract JWT and get username
        String jwt = token.replace("Bearer ", "");

        List<TransactionResponseDto> transactions =
                transactionService.getAllUserTransactions(jwt);

        return ResponseEntity.ok(transactions);
    }
}