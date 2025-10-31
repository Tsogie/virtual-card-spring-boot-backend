package net.otgon.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QrToken {

    @Id
    @Column(length = 36, nullable = false, unique = true)
    private String jti;   // UUID as string

    @Column(name = "card_id", length = 36, nullable = false)
    private String cardId;   // Reference to user card

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

}
