package io.github.mohamedmedhat21.order_processing_system.dto.admin;

import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusUpdateRequest(@NotNull OrderStatus status) {
}
