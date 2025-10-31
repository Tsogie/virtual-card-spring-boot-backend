package net.otgon.backend.repository;

import net.otgon.backend.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepo extends JpaRepository<Card, String> {
}
