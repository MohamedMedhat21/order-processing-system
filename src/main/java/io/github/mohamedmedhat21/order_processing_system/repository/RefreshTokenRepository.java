package io.github.mohamedmedhat21.order_processing_system.repository;

import java.time.Instant;
import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, Instant now);

	@Modifying
	@Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
	int revokeAllActiveForUser(@Param("userId") Long userId);

	@Modifying
	@Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.id = :id")
	int revokeById(@Param("id") Long id);
}
