package io.github.mohamedmedhat21.order_processing_system.repository;

import io.github.mohamedmedhat21.order_processing_system.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
