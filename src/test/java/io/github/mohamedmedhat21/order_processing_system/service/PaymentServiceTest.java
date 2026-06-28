package io.github.mohamedmedhat21.order_processing_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.Payment;
import io.github.mohamedmedhat21.order_processing_system.domain.PaymentStatus;
import io.github.mohamedmedhat21.order_processing_system.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(paymentRepository);
	}

	@Test
	void returnsExistingPaymentForDuplicateIdempotencyKey() {
		Order order = order(5L, "25.00");
		Payment existing = payment(order, PaymentStatus.COMPLETED);

		when(paymentRepository.findByIdempotencyKey("order-5")).thenReturn(Optional.of(existing));

		Payment result = paymentService.chargeOrder(order);

		assertThat(result).isSameAs(existing);
	}

	@Test
	void createsPaymentWhenIdempotencyKeyIsNew() {
		Order order = order(9L, "10.00");
		when(paymentRepository.findByIdempotencyKey("order-9")).thenReturn(Optional.empty());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Payment result = paymentService.chargeOrder(order);

		ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(captor.capture());
		assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("order-9");
		assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	void handlesConcurrentInsertViaUniqueConstraint() {
		Order order = order(11L, "15.00");
		Payment existing = payment(order, PaymentStatus.COMPLETED);

		when(paymentRepository.findByIdempotencyKey("order-11"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(existing));
		when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		Payment result = paymentService.chargeOrder(order);

		assertThat(result).isSameAs(existing);
	}

	private static Order order(Long id, String total) {
		Order order = new Order();
		order.setId(id);
		order.setTotalAmount(new BigDecimal(total));
		return order;
	}

	private static Payment payment(Order order, PaymentStatus status) {
		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setIdempotencyKey(PaymentService.idempotencyKeyFor(order.getId()));
		payment.setAmount(order.getTotalAmount());
		payment.setStatus(status);
		return payment;
	}
}
