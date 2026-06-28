package io.github.mohamedmedhat21.order_processing_system.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesReportResponse(
		LocalDate date,
		long orderCount,
		BigDecimal totalRevenue,
		long fulfilledCount,
		long failedCount
) {
}
