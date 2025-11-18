package net.otgon.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @Column(length = 36)
    private String id; // UUID for card

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "balance")
    private double balance = 0.0;

}
