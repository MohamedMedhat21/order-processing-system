package io.github.mohamedmedhat21.order_processing_system.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.domain.OrderItem;
import io.github.mohamedmedhat21.order_processing_system.domain.Product;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.exception.BadRequestException;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.ProductRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import io.github.mohamedmedhat21.order_processing_system.security.SecurityUtils;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderDraftService {

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Order createDraft(CreateOrderRequest request) {
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
		return orderRepository.save(order);
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
