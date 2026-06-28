package io.github.mohamedmedhat21.order_processing_system.service;

import java.util.Comparator;
import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.OrderItem;
import io.github.mohamedmedhat21.order_processing_system.exception.InsufficientInventoryException;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void reserveForOrder(Order order) {
		List<OrderItem> sortedItems = order.getItems().stream()
				.sorted(Comparator.comparing(item -> item.getProduct().getId()))
				.toList();

		for (OrderItem item : sortedItems) {
			Long productId = item.getProduct().getId();
			Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
					.orElseThrow(() -> new InsufficientInventoryException(productId, item.getQuantity(), 0));

			if (inventory.getQuantityAvailable() < item.getQuantity()) {
				throw new InsufficientInventoryException(
						productId,
						item.getQuantity(),
						inventory.getQuantityAvailable()
				);
			}

			inventory.setQuantityAvailable(inventory.getQuantityAvailable() - item.getQuantity());
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void releaseForOrder(Order order) {
		List<OrderItem> sortedItems = order.getItems().stream()
				.sorted(Comparator.comparing(item -> item.getProduct().getId()))
				.toList();

		for (OrderItem item : sortedItems) {
			Long productId = item.getProduct().getId();
			Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
					.orElseThrow(() -> new InsufficientInventoryException(productId, item.getQuantity(), 0));
			inventory.setQuantityAvailable(inventory.getQuantityAvailable() + item.getQuantity());
		}
	}
}
