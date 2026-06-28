package io.github.mohamedmedhat21.order_processing_system.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.mohamedmedhat21.order_processing_system.dto.PageResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.admin.AdminOrderStatusUpdateRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.admin.DailySalesReportResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.admin.LowStockAlertResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.service.AdminInventoryService;
import io.github.mohamedmedhat21.order_processing_system.service.AdminOrderService;
import io.github.mohamedmedhat21.order_processing_system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

	private final AdminOrderService adminOrderService;
	private final ReportService reportService;
	private final AdminInventoryService adminInventoryService;

	@GetMapping("/orders")
	@Operation(summary = "List all orders (paginated)")
	public PageResponse<OrderResponse> listOrders(@PageableDefault(size = 20) Pageable pageable) {
		return adminOrderService.listAllOrders(pageable);
	}

	@PutMapping("/orders/{id}/status")
	@Operation(summary = "Update order status (validated against state machine transitions)")
	public OrderResponse updateOrderStatus(
			@PathVariable Long id,
			@Valid @RequestBody AdminOrderStatusUpdateRequest request
	) {
		return adminOrderService.updateOrderStatus(id, request);
	}

	@GetMapping("/reports/daily")
	@Operation(summary = "Daily sales report (computed asynchronously via CompletableFuture)")
	public CompletableFuture<DailySalesReportResponse> dailyReport(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		LocalDate reportDate = date != null ? date : LocalDate.now(java.time.ZoneOffset.UTC);
		return reportService.generateDailyReport(reportDate);
	}

	@GetMapping("/inventory/low-stock")
	@Operation(summary = "Products at or below the low-stock threshold")
	public List<LowStockAlertResponse> lowStockAlerts() {
		return adminInventoryService.getLowStockAlerts();
	}
}
