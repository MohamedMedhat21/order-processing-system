package io.github.mohamedmedhat21.order_processing_system.audit;

import io.github.mohamedmedhat21.order_processing_system.domain.AuditLog;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditLogWriter {

	private final AuditLogRepository auditLogRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public AuditLog write(String entityType, Long entityId, String action, String details, User actor) {
		AuditLog entry = new AuditLog();
		entry.setEntityType(entityType);
		entry.setEntityId(entityId);
		entry.setAction(action);
		entry.setDetails(details);
		entry.setActor(actor);
		return auditLogRepository.save(entry);
	}
}
