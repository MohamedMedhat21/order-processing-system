package io.github.mohamedmedhat21.order_processing_system.service;

import io.github.mohamedmedhat21.order_processing_system.audit.AuditLogWriter;
import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.dto.PageResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.admin.AdminOrderStatusUpdateRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import io.github.mohamedmedhat21.order_processing_system.security.SecurityUtils;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderTransitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

	private static final String ENTITY_TYPE = "ORDER";

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final AuditLogWriter auditLogWriter;

	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> listAllOrders(Pageable pageable) {
		Page<Order> page = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
		return new PageResponse<>(
				page.getContent().stream().map(OrderResponse::from).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Transactional
	public OrderResponse updateOrderStatus(Long orderId, AdminOrderStatusUpdateRequest request) {
		UserPrincipal admin = SecurityUtils.currentUser();
		User actor = userRepository.findById(admin.id())
				.orElseThrow(() -> new ResourceNotFoundException("User", admin.id()));

		Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		OrderStatus previous = order.getStatus();
		OrderTransitionValidator.validate(previous, request.status());
		order.setStatus(request.status());
		Order saved = orderRepository.save(order);

		auditLogWriter.write(
				ENTITY_TYPE,
				saved.getId(),
				"%s -> %s".formatted(previous, request.status()),
				"Admin status update",
				actor
		);

		return OrderResponse.from(saved);
	}
}
