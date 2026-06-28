package io.github.mohamedmedhat21.order_processing_system.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.OrderItem;
import io.github.mohamedmedhat21.order_processing_system.domain.Product;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderStatusResponse;
import io.github.mohamedmedhat21.order_processing_system.exception.BadRequestException;
import io.github.mohamedmedhat21.order_processing_system.exception.ForbiddenException;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.ProductRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import io.github.mohamedmedhat21.order_processing_system.security.SecurityUtils;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		UserPrincipal principal = SecurityUtils.currentUser();
		User user = userRepository.findById(principal.id())
				.orElseThrow(() -> new ResourceNotFoundException("User", principal.id()));

		Map<Long, Product> productsById = loadProducts(request.items());
		BigDecimal total = BigDecimal.ZERO;

		Order order = new Order();
		order.setUser(user);
		order.setStatus(OrderStatus.CREATED);
		order.setTotalAmount(BigDecimal.ZERO);

		for (OrderLineRequest line : request.items()) {
			Product product = productsById.get(line.productId());
			if (!product.isActive()) {
				throw new BadRequestException("Product is not available: " + line.productId());
			}

			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setProduct(product);
			item.setQuantity(line.quantity());
			item.setUnitPrice(product.getPrice());
			order.getItems().add(item);

			total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
		}

		order.setTotalAmount(total);
		return OrderResponse.from(orderRepository.save(order));
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
		Order order = findAccessibleOrder(id);
		return OrderResponse.from(order);
	}

	@Transactional(readOnly = true)
	public OrderStatusResponse getOrderStatus(Long id) {
		Order order = findAccessibleOrder(id);
		return new OrderStatusResponse(order.getId(), order.getStatus(), order.getUpdatedAt());
	}

	@Transactional
	public OrderResponse cancelOrder(Long id) {
		Order order = findAccessibleOrder(id);
		if (order.getStatus() != OrderStatus.CREATED) {
			throw new BadRequestException("Only orders in CREATED status can be cancelled");
		}
		order.setStatus(OrderStatus.CANCELLED);
		return OrderResponse.from(orderRepository.save(order));
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

	private Map<Long, Product> loadProducts(List<OrderLineRequest> items) {
		Map<Long, Product> productsById = new HashMap<>();
		for (OrderLineRequest line : items) {
			if (productsById.containsKey(line.productId())) {
				continue;
			}
			Product product = productRepository.findById(line.productId())
					.orElseThrow(() -> new BadRequestException("Unknown product id: " + line.productId()));
			productsById.put(line.productId(), product);
		}
		return productsById;
	}
}
