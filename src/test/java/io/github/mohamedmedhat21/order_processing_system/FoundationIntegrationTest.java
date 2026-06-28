package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.ProductRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FoundationIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Test
	void flywayMigrationsAndSeedDataLoad() {
		assertThat(userRepository.findByEmail("admin@example.com"))
				.isPresent()
				.get()
				.extracting("role")
				.isEqualTo(UserRole.ADMIN);

		assertThat(userRepository.findByEmail("customer@example.com"))
				.isPresent()
				.get()
				.extracting("role")
				.isEqualTo(UserRole.CUSTOMER);

		assertThat(productRepository.count()).isEqualTo(5);
		assertThat(inventoryRepository.findByProductId(5L))
				.isPresent()
				.get()
				.extracting("quantityAvailable")
				.isEqualTo(1);
	}
}
