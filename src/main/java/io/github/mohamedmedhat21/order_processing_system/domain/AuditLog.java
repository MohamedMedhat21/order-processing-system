package io.github.mohamedmedhat21.order_processing_system.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "entity_type", nullable = false)
	private String entityType;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(nullable = false)
	private String action;

	@Column(columnDefinition = "TEXT")
	private String details;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
