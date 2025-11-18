package net.otgon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {

    private String id;
    private String type;           // "DEDUCT" or "TOPUP"
    private double amount;
    private double balanceAfter;   // Balance after this transaction
    private LocalDateTime timestamp;
    private String status;         // "SUCCESS", "PENDING", etc.
}