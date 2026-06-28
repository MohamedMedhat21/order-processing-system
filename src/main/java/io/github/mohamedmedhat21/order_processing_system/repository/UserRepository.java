package io.github.mohamedmedhat21.order_processing_system.repository;

import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
}
