package io.github.mohamedmedhat21.order_processing_system.statemachine;

public enum OrderStatus {
	CREATED,
	INVENTORY_RESERVED,
	PAYMENT_PROCESSING,
	PAID,
	FULFILLED,
	NOTIFIED,
	CANCELLED,
	FAILED
}
