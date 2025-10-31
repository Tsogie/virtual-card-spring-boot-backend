package net.otgon.backend.repository;

import net.otgon.backend.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrTokenRepo extends JpaRepository<QrToken, String> {
}
