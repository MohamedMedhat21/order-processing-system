package io.github.mohamedmedhat21.order_processing_system.service;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.Payment;
import io.github.mohamedmedhat21.order_processing_system.domain.PaymentStatus;
import io.github.mohamedmedhat21.order_processing_system.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public Payment chargeOrder(Order order) {
		String idempotencyKey = idempotencyKeyFor(order.getId());

		return paymentRepository.findByIdempotencyKey(idempotencyKey)
				.orElseGet(() -> createPayment(order, idempotencyKey));
	}

	private Payment createPayment(Order order, String idempotencyKey) {
		try {
			Payment payment = new Payment();
			payment.setOrder(order);
			payment.setIdempotencyKey(idempotencyKey);
			payment.setAmount(order.getTotalAmount());
			payment.setStatus(PaymentStatus.COMPLETED);
			simulateGatewayCharge(order);
			return paymentRepository.save(payment);
		}
		catch (DataIntegrityViolationException ex) {
			return paymentRepository.findByIdempotencyKey(idempotencyKey)
					.orElseThrow(() -> ex);
		}
	}

	private void simulateGatewayCharge(Order order) {
		// Simulated payment gateway — always succeeds unless extended for failure scenarios.
	}

	public static String idempotencyKeyFor(Long orderId) {
		return "order-" + orderId;
	}
}
