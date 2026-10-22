package co.edu.uniajc.rsu_auth_service.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "espacios")
public class Space {
    @Id
    private UUID id;

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

    protected Space() {
    }

    public Space(SpaceType spaceType, String name, String location, int capacity, String description, SpaceStatus status) {
        this.id = UUID.randomUUID();
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

    public UUID getId() {
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
