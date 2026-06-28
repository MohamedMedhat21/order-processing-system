package io.github.mohamedmedhat21.order_processing_system.service;

import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderStatusResponse;
import io.github.mohamedmedhat21.order_processing_system.exception.ForbiddenException;
import io.github.mohamedmedhat21.order_processing_system.exception.InsufficientInventoryException;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.security.SecurityUtils;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderDraftService orderDraftService;
	private final OrderStateMachine orderStateMachine;
	private final NotificationService notificationService;

	public OrderResponse createOrder(CreateOrderRequest request) {
		Order order = orderDraftService.createDraft(request);
		Long orderId = order.getId();

		try {
			orderStateMachine.reserveInventory(orderId);
			orderStateMachine.beginPaymentProcessing(orderId);
			orderStateMachine.completePayment(orderId);
			orderStateMachine.fulfill(orderId);
			notificationService.publishOrderNotification(orderId);
		}
		catch (InsufficientInventoryException ex) {
			throw ex;
		}

		return loadOrderResponse(orderId);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> listMyOrders() {
		UserPrincipal principal = SecurityUtils.currentUser();
		return orderRepository.findByUserIdWithItems(principal.id()).stream()
				.map(OrderResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long id) {
		return OrderResponse.from(findAccessibleOrder(id));
	}

	@Transactional(readOnly = true)
	public OrderStatusResponse getOrderStatus(Long id) {
		Order order = findAccessibleOrder(id);
		return new OrderStatusResponse(order.getId(), order.getStatus(), order.getUpdatedAt());
	}

	public OrderResponse cancelOrder(Long id) {
		findAccessibleOrder(id);
		orderStateMachine.cancel(id);
		return loadOrderResponse(id);
	}

	private OrderResponse loadOrderResponse(Long orderId) {
		return OrderResponse.from(orderRepository.findByIdWithItems(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId)));
	}

	private Order findAccessibleOrder(Long id) {
		UserPrincipal principal = SecurityUtils.currentUser();
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));

		if (!order.getUser().getId().equals(principal.id()) && principal.role() != UserRole.ADMIN) {
			throw new ForbiddenException("You can only access your own orders");
		}
		return order;
	}
}
