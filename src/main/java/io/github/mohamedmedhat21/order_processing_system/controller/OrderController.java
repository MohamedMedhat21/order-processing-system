package io.github.mohamedmedhat21.order_processing_system.controller;

import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderStatusResponse;
import io.github.mohamedmedhat21.order_processing_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create order and run processing pipeline (notification sent asynchronously)")
	public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
		return orderService.createOrder(request);
	}

	@GetMapping
	@Operation(summary = "List current user's orders")
	public List<OrderResponse> listOrders() {
		return orderService.listMyOrders();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get order details")
	public OrderResponse getOrder(@PathVariable Long id) {
		return orderService.getOrder(id);
	}

	@GetMapping("/{id}/status")
	@Operation(summary = "Get order status")
	public OrderStatusResponse getOrderStatus(@PathVariable Long id) {
		return orderService.getOrderStatus(id);
	}

	@PutMapping("/{id}/cancel")
	@Operation(summary = "Cancel order (CREATED status only)")
	public OrderResponse cancelOrder(@PathVariable Long id) {
		return orderService.cancelOrder(id);
	}
}
