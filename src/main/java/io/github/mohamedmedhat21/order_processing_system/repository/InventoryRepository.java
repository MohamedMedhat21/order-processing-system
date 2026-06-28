package io.github.mohamedmedhat21.order_processing_system.repository;

import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

	Optional<Inventory> findByProductId(Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
	Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
