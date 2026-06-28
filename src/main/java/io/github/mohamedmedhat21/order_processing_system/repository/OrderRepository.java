package io.github.mohamedmedhat21.order_processing_system.repository;

import java.util.List;
import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
