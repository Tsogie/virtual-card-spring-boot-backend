package net.otgon.backend.repository;

import net.otgon.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u JOIN FETCH u.card WHERE u.id = :id")
    Optional<User> findByIdWithCard(@Param("id") String id);

    Optional<User> findByEmail(String email);
}
