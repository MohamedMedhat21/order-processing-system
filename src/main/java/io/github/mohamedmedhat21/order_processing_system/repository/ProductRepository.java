package io.github.mohamedmedhat21.order_processing_system.repository;

import io.github.mohamedmedhat21.order_processing_system.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByActiveTrue(Pageable pageable);
}
