package co.edu.uniajc.rsu_auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

import co.edu.uniajc.rsu_auth_service.model.AuditEntry;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
    @EntityGraph(attributePaths = "actor")
    List<AuditEntry> findTop100ByOrderByCreatedAtDesc();
}
