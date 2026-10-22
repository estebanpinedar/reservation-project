package co.edu.uniajc.rsu_auth_service.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_espacio")
public class SpaceType {
    @Id
    private UUID id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String name;

    @Column(name = "descripcion")
    private String description;

    protected SpaceType() {
    }

    public SpaceType(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
