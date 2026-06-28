package io.github.mohamedmedhat21.order_processing_system.repository;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
