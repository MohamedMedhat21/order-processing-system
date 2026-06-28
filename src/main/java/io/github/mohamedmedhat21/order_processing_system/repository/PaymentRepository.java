package io.github.mohamedmedhat21.order_processing_system.repository;

import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
