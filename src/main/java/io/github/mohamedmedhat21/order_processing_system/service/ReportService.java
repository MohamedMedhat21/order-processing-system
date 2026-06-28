package io.github.mohamedmedhat21.order_processing_system.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.github.mohamedmedhat21.order_processing_system.dto.admin.DailySalesReportResponse;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

	private static final java.util.List<OrderStatus> REVENUE_STATUSES = java.util.List.of(
			OrderStatus.INVENTORY_RESERVED,
			OrderStatus.PAYMENT_PROCESSING,
			OrderStatus.PAID,
			OrderStatus.FULFILLED,
			OrderStatus.NOTIFIED
	);

	private final OrderRepository orderRepository;
	private final Executor virtualThreadExecutor;

	public ReportService(
			OrderRepository orderRepository,
			@Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor
	) {
		this.orderRepository = orderRepository;
		this.virtualThreadExecutor = virtualThreadExecutor;
	}

	public CompletableFuture<DailySalesReportResponse> generateDailyReport(java.time.LocalDate date) {
		return CompletableFuture.supplyAsync(() -> buildReport(date), virtualThreadExecutor);
	}

	private DailySalesReportResponse buildReport(java.time.LocalDate date) {
		java.time.Instant start = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		java.time.Instant end = date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

		long orderCount = orderRepository.countOrdersBetween(start, end);
		var totalRevenue = orderRepository.sumRevenueBetween(start, end, REVENUE_STATUSES);
		long fulfilledCount = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.FULFILLED, start, end)
				+ orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.NOTIFIED, start, end);
		long failedCount = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.FAILED, start, end);

		return new DailySalesReportResponse(date, orderCount, totalRevenue, fulfilledCount, failedCount);
	}
}
