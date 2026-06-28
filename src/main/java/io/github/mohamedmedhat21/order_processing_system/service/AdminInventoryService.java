package io.github.mohamedmedhat21.order_processing_system.service;

import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.dto.admin.LowStockAlertResponse;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminInventoryService {

	private final InventoryRepository inventoryRepository;

	@Value("${app.inventory.low-stock-threshold:5}")
	private int lowStockThreshold;

	@Transactional(readOnly = true)
	public List<LowStockAlertResponse> getLowStockAlerts() {
		return inventoryRepository.findLowStock(lowStockThreshold).stream()
				.map(this::toResponse)
				.toList();
	}

	private LowStockAlertResponse toResponse(Inventory inventory) {
		return new LowStockAlertResponse(
				inventory.getProduct().getId(),
				inventory.getProduct().getName(),
				inventory.getQuantityAvailable(),
				lowStockThreshold
		);
	}
}
