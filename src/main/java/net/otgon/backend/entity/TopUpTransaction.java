package net.otgon.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "topup_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopUpTransaction {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString(); // primary key

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private double amount;

    /** Time when top-up occurred */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}

