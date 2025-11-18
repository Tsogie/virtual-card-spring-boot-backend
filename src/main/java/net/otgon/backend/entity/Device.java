package net.otgon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 2048)
    private String publicKey;


//    @Column(nullable = false, unique = true, length = 64)
//    private String deviceKey; // e.g., UUID or random string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

}

