package io.github.mohamedmedhat21.order_processing_system.statemachine;

import java.util.Map;
import java.util.Set;

import io.github.mohamedmedhat21.order_processing_system.exception.InvalidStateTransitionException;

public final class OrderTransitionValidator {

	private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
			OrderStatus.CREATED, Set.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.CANCELLED, OrderStatus.FAILED),
			OrderStatus.INVENTORY_RESERVED, Set.of(OrderStatus.PAYMENT_PROCESSING, OrderStatus.FAILED),
			OrderStatus.PAYMENT_PROCESSING, Set.of(OrderStatus.PAID, OrderStatus.FAILED),
			OrderStatus.PAID, Set.of(OrderStatus.FULFILLED, OrderStatus.FAILED),
			OrderStatus.FULFILLED, Set.of(OrderStatus.NOTIFIED),
			OrderStatus.NOTIFIED, Set.of(),
			OrderStatus.CANCELLED, Set.of(),
			OrderStatus.FAILED, Set.of()
	);

	private OrderTransitionValidator() {
	}

	public static void validate(OrderStatus from, OrderStatus to) {
		Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
		if (!allowed.contains(to)) {
			throw new InvalidStateTransitionException(from, to);
		}
	}
}
