package io.github.mohamedmedhat21.order_processing_system.repository;

import io.github.mohamedmedhat21.order_processing_system.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
