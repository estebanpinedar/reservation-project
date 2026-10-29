package co.edu.uniajc.rsu_auth_service.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "espacio_horarios", uniqueConstraints = @UniqueConstraint(
        name = "espacio_horarios_dia_uq", columnNames = { "espacio_id", "dia_semana" }))
public class SpaceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Space space;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime opensAt;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime closesAt;

    protected SpaceSchedule() {
    }

    public SpaceSchedule(Space space, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.space = space;
        update(dayOfWeek, opensAt, closesAt);
    }

    public void update(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public Long getId() {
        return id;
    }

    public Space getSpace() {
        return space;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }
}
