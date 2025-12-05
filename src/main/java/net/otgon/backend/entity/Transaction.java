package net.otgon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Transaction {

    @Id
    @Column(length = 36)
    private String id = java.util.UUID.randomUUID().toString();

    /**
     * Unique transaction ID generated on the phone.
     * Prevents duplicates when syncing later.
     */
    @Column(name = "tx_id", unique = true, nullable = false, length = 64)
    private String txId;


    @Column(length = 64)
    private String type;

    /**
     * Base64 ECDSA signature of the transaction payload.
     */
    @Column(name = "signature", nullable = false, columnDefinition = "TEXT")
    private String signature;

    /**
     * The fare amount or top-up amount.
     */
    @Column(nullable = false)
    private double amount;

    /**
     * Device timestamp (epoch milliseconds)
     * A long is better than LocalDateTime for offline sync.
     */
    @Column(name = "device_timestamp", nullable = false)
    private Long Timestamp;

    /**
     * When backend processed it.
     */
    @Column(name = "synced_at")
    private LocalDateTime syncedAt = LocalDateTime.now();

    /**
     * PENDING (received), SUCCESS (balance updated), FAILED (signature invalid)
     */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 20)
    private boolean processed = false;

    /**
     * Relationship to card
     */
    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;
}
