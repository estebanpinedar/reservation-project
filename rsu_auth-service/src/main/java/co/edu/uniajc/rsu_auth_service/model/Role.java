package co.edu.uniajc.rsu_auth_service.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true)
    private RoleName name;

    protected Role() {
    }

    public Role(UUID id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }
}
