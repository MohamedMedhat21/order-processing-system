package io.github.mohamedmedhat21.order_processing_system.repository;

import io.github.mohamedmedhat21.order_processing_system.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
