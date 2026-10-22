package co.edu.uniajc.rsu_auth_service.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.User;

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
@Table(name = "reservas")
public class Reservation {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_creador_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Space space;

    @Column(name = "hora_inicio", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "hora_fin", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "motivo", nullable = false)
    private String purpose;

    @Column(name = "asistentes", nullable = false)
    private int attendees;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private ReservationStatus status;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Reservation() {
    }

    public Reservation(User creator, Space space, OffsetDateTime startAt, OffsetDateTime endAt, String purpose,
            int attendees) {
        this.id = UUID.randomUUID();
        this.creator = creator;
        this.space = space;
        this.startAt = startAt;
        this.endAt = endAt;
        this.purpose = purpose;
        this.attendees = attendees;
        this.status = ReservationStatus.ACTIVE;
    }

    public void cancel(String reason) {
        status = ReservationStatus.CANCELLED;
        cancelledAt = OffsetDateTime.now();
        cancellationReason = reason;
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

    public UUID getId() {
        return id;
    }

    public User getCreator() {
        return creator;
    }

    public Space getSpace() {
        return space;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public String getPurpose() {
        return purpose;
    }

    public int getAttendees() {
        return attendees;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
