package io.github.mohamedmedhat21.order_processing_system.exception;

import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BaseBusinessException {

	public InvalidStateTransitionException(OrderStatus from, OrderStatus to) {
		super(
				HttpStatus.CONFLICT,
				"INVALID_STATE_TRANSITION",
				"Cannot transition order from %s to %s".formatted(from, to)
		);
	}
}
