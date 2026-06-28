package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.repository.PaymentRepository;
import io.github.mohamedmedhat21.order_processing_system.service.OrderDraftService;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import io.github.mohamedmedhat21.order_processing_system.support.SecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentIdempotencyConcurrencyTest {

	private static final Long PRODUCT_ID = 1L;

	@Autowired
	private OrderDraftService orderDraftService;

	@Autowired
	private OrderStateMachine orderStateMachine;

	@Autowired
	private PaymentRepository paymentRepository;

	@Test
	void concurrentPaymentAttemptsChargeExactlyOnce() throws Exception {
		Long orderId = SecurityTestSupport.callAs(SecurityTestSupport.customerPrincipal(), () -> {
			Long id = orderDraftService.createDraft(new CreateOrderRequest(
					List.of(new OrderLineRequest(PRODUCT_ID, 1))
			)).getId();
			orderStateMachine.reserveInventory(id);
			orderStateMachine.beginPaymentProcessing(id);
			return id;
		});

		int threads = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					startGate.await();
					orderStateMachine.completePayment(orderId);
				}
				catch (Exception ignored) {
					// concurrent transition races are acceptable once one payment exists
				}
				finally {
					doneGate.countDown();
				}
			});
		}

		startGate.countDown();
		assertThat(doneGate.await(60, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		assertThat(paymentRepository.countByOrderId(orderId)).isEqualTo(1);
	}
}
