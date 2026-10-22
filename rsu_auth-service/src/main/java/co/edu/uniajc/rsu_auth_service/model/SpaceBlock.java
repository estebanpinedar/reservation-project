package co.edu.uniajc.rsu_auth_service.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "bloqueos")
public class SpaceBlock {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_creador_id", nullable = false)
    private User creator;

    @Column(name = "hora_inicio", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "hora_fin", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "motivo", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private BlockStatus status;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SpaceBlock() {
    }

    public SpaceBlock(Space space, User creator, OffsetDateTime startAt, OffsetDateTime endAt, String reason) {
        this.id = UUID.randomUUID();
        this.space = space;
        this.creator = creator;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
        this.status = BlockStatus.ACTIVE;
    }

    public void cancel() {
        status = BlockStatus.CANCELLED;
        cancelledAt = OffsetDateTime.now();
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Space getSpace() { return space; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public String getReason() { return reason; }
    public BlockStatus getStatus() { return status; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
