package co.edu.uniajc.rsu_auth_service.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "espacios")
public class Space {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_espacio_id", nullable = false)
    private SpaceType spaceType;

    @Column(name = "nombre", nullable = false, unique = true)
    private String name;

    @Column(name = "ubicacion", nullable = false)
    private String location;

    @Column(name = "capacidad", nullable = false)
    private int capacity;

    @Column(name = "descripcion")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private SpaceStatus status;

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceSchedule> schedules = new ArrayList<>();

    protected Space() {
    }

    public Space(SpaceType spaceType, String name, String location, int capacity, String description, SpaceStatus status) {
        update(spaceType, name, location, capacity, description, status);
    }

    public void update(SpaceType spaceType, String name, String location, int capacity, String description,
            SpaceStatus status) {
        this.spaceType = spaceType;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
        this.status = status;
    }

    public void changeStatus(SpaceStatus status) {
        this.status = status;
    }

    public void replaceSchedules(List<SpaceSchedule> requestedSchedules) {
        Map<DayOfWeek, SpaceSchedule> existingByDay = new EnumMap<>(DayOfWeek.class);
        schedules.forEach(schedule -> existingByDay.put(schedule.getDayOfWeek(), schedule));
        Map<DayOfWeek, SpaceSchedule> requestedByDay = new EnumMap<>(DayOfWeek.class);
        requestedSchedules.forEach(schedule -> requestedByDay.put(schedule.getDayOfWeek(), schedule));

        schedules.removeIf(schedule -> !requestedByDay.containsKey(schedule.getDayOfWeek()));
        requestedSchedules.forEach(requested -> {
            SpaceSchedule existing = existingByDay.get(requested.getDayOfWeek());
            if (existing == null) {
                schedules.add(new SpaceSchedule(this, requested.getDayOfWeek(), requested.getOpensAt(),
                        requested.getClosesAt()));
            } else {
                existing.update(requested.getDayOfWeek(), requested.getOpensAt(), requested.getClosesAt());
            }
        });
    }

    public List<SpaceSchedule> getSchedules() {
        return List.copyOf(schedules);
    }

    public SpaceSchedule scheduleFor(DayOfWeek dayOfWeek) {
        return schedules.stream().filter(schedule -> schedule.getDayOfWeek() == dayOfWeek).findFirst().orElse(null);
    }

    public boolean isAvailableDuring(OffsetDateTime startAt, OffsetDateTime endAt, ZoneId zone) {
        var localStart = startAt.atZoneSameInstant(zone).toLocalDateTime();
        var localEnd = endAt.atZoneSameInstant(zone).toLocalDateTime();
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            return false;
        }
        SpaceSchedule schedule = scheduleFor(localStart.getDayOfWeek());
        if (schedule == null) {
            return false;
        }
        LocalTime startTime = localStart.toLocalTime();
        LocalTime endTime = localEnd.toLocalTime();
        return !startTime.isBefore(schedule.getOpensAt()) && !endTime.isAfter(schedule.getClosesAt());
    }

    public Long getId() {
        return id;
    }

    public SpaceType getSpaceType() {
        return spaceType;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public SpaceStatus getStatus() {
        return status;
    }
}
