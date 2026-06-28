package io.github.mohamedmedhat21.order_processing_system.statemachine;

import io.github.mohamedmedhat21.order_processing_system.audit.AuditLogWriter;
import io.github.mohamedmedhat21.order_processing_system.domain.Notification;
import io.github.mohamedmedhat21.order_processing_system.domain.NotificationStatus;
import io.github.mohamedmedhat21.order_processing_system.domain.NotificationType;
import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.exception.InsufficientInventoryException;
import io.github.mohamedmedhat21.order_processing_system.repository.NotificationRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.service.InventoryService;
import io.github.mohamedmedhat21.order_processing_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStateMachine {

	private static final String ENTITY_TYPE = "ORDER";

	private final OrderRepository orderRepository;
	private final InventoryService inventoryService;
	private final PaymentService paymentService;
	private final NotificationRepository notificationRepository;
	private final AuditLogWriter auditLogWriter;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order reserveInventory(Long orderId) {
		Order order = loadForUpdate(orderId);
		OrderStatus target = OrderStatus.INVENTORY_RESERVED;
		OrderTransitionValidator.validate(order.getStatus(), target);

		try {
			inventoryService.reserveForOrder(order);
		}
		catch (InsufficientInventoryException ex) {
			markFailed(order, "Inventory reservation failed: " + ex.getMessage());
			throw ex;
		}

		return applyTransition(order, target, "Inventory reserved");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order beginPaymentProcessing(Long orderId) {
		Order order = loadForUpdate(orderId);
		return applyTransition(order, OrderStatus.PAYMENT_PROCESSING, "Payment processing started");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order completePayment(Long orderId) {
		Order order = loadForUpdate(orderId);
		OrderTransitionValidator.validate(order.getStatus(), OrderStatus.PAID);

		paymentService.chargeOrder(order);
		return applyTransition(order, OrderStatus.PAID, "Payment completed");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order fulfill(Long orderId) {
		Order order = loadForUpdate(orderId);
		return applyTransition(order, OrderStatus.FULFILLED, "Order fulfilled");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order notifyCustomer(Long orderId) {
		Order order = loadForUpdate(orderId);
		OrderTransitionValidator.validate(order.getStatus(), OrderStatus.NOTIFIED);

		Notification notification = new Notification();
		notification.setOrder(order);
		notification.setUser(order.getUser());
		notification.setType(NotificationType.ORDER_CONFIRMED);
		notification.setMessage("Your order #%d has been confirmed.".formatted(order.getId()));
		notification.setStatus(NotificationStatus.SENT);
		notificationRepository.save(notification);

		return applyTransition(order, OrderStatus.NOTIFIED, "Customer notified");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order cancel(Long orderId) {
		Order order = loadForUpdate(orderId);
		return applyTransition(order, OrderStatus.CANCELLED, "Order cancelled by customer");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order markFailed(Long orderId, String reason) {
		Order order = loadForUpdate(orderId);
		if (order.getStatus() == OrderStatus.FAILED) {
			return order;
		}
		OrderTransitionValidator.validate(order.getStatus(), OrderStatus.FAILED);
		return applyTransition(order, OrderStatus.FAILED, reason);
	}

	private Order markFailed(Order order, String reason) {
		if (order.getStatus() == OrderStatus.FAILED) {
			return order;
		}
		OrderTransitionValidator.validate(order.getStatus(), OrderStatus.FAILED);
		return applyTransition(order, OrderStatus.FAILED, reason);
	}

	private Order applyTransition(Order order, OrderStatus target, String details) {
		OrderStatus previous = order.getStatus();
		OrderTransitionValidator.validate(previous, target);
		order.setStatus(target);
		Order saved = orderRepository.save(order);
		auditLogWriter.write(
				ENTITY_TYPE,
				saved.getId(),
				"%s -> %s".formatted(previous, target),
				details,
				saved.getUser()
		);
		return saved;
	}

	private Order loadForUpdate(Long orderId) {
		return orderRepository.findByIdWithItemsForUpdate(orderId)
				.orElseThrow(() -> new io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException(
						"Order", orderId));
	}
}
