package co.edu.uniajc.rsu_auth_service.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditorias")
public class AuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "accion", nullable = false)
    private String action;

    @Column(name = "entidad", nullable = false)
    private String entity;

    @Column(name = "entidad_id")
    private Long entityId;

    @Column(name = "detalle")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AuditEntry() {
    }

    public AuditEntry(User actor, String action, String entity, Long entityId, String detail) {
        this.actor = actor;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public String getDetail() { return detail; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
