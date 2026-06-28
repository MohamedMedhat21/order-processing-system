package io.github.mohamedmedhat21.order_processing_system.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mohamedmedhat21.order_processing_system.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class OrderTransitionValidatorTest {

	@Test
	void allowsLinearPipelineTransitions() {
		assertThatCode(() -> OrderTransitionValidator.validate(OrderStatus.CREATED, OrderStatus.INVENTORY_RESERVED))
				.doesNotThrowAnyException();
		assertThatCode(() -> OrderTransitionValidator.validate(OrderStatus.INVENTORY_RESERVED, OrderStatus.PAYMENT_PROCESSING))
				.doesNotThrowAnyException();
		assertThatCode(() -> OrderTransitionValidator.validate(OrderStatus.PAYMENT_PROCESSING, OrderStatus.PAID))
				.doesNotThrowAnyException();
		assertThatCode(() -> OrderTransitionValidator.validate(OrderStatus.PAID, OrderStatus.FULFILLED))
				.doesNotThrowAnyException();
		assertThatCode(() -> OrderTransitionValidator.validate(OrderStatus.FULFILLED, OrderStatus.NOTIFIED))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsIllegalJump() {
		assertThatThrownBy(() -> OrderTransitionValidator.validate(OrderStatus.CREATED, OrderStatus.PAID))
				.isInstanceOf(InvalidStateTransitionException.class);
	}
}
