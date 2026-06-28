package io.github.mohamedmedhat21.order_processing_system.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

	@Query("""
			SELECT DISTINCT o FROM Order o
			LEFT JOIN FETCH o.items items
			LEFT JOIN FETCH items.product
			WHERE o.user.id = :userId
			ORDER BY o.createdAt DESC
			""")
	List<Order> findByUserIdWithItems(@Param("userId") Long userId);

	@Query("""
			SELECT DISTINCT o FROM Order o
			LEFT JOIN FETCH o.items items
			LEFT JOIN FETCH items.product
			LEFT JOIN FETCH o.user
			WHERE o.id = :id
			""")
	Optional<Order> findByIdWithItems(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT DISTINCT o FROM Order o
			LEFT JOIN FETCH o.items items
			LEFT JOIN FETCH items.product
			LEFT JOIN FETCH o.user
			WHERE o.id = :id
			""")
	Optional<Order> findByIdWithItemsForUpdate(@Param("id") Long id);

	Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

	@Query("""
			SELECT COUNT(o) FROM Order o
			WHERE o.createdAt >= :start AND o.createdAt < :end
			""")
	long countOrdersBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
			WHERE o.createdAt >= :start AND o.createdAt < :end
			AND o.status IN :statuses
			""")
	BigDecimal sumRevenueBetween(
			@Param("start") Instant start,
			@Param("end") Instant end,
			@Param("statuses") List<OrderStatus> statuses
	);

	long countByStatusAndCreatedAtBetween(OrderStatus status, Instant start, Instant end);
}
