package co.edu.uniajc.rsu_auth_service.service;


import org.springframework.stereotype.Service;

import co.edu.uniajc.rsu_auth_service.model.AuditEntry;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.AuditEntryRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Service
public class AuditService {
    private final AuditEntryRepository auditEntryRepository;
    private final UserRepository userRepository;

    public AuditService(AuditEntryRepository auditEntryRepository, UserRepository userRepository) {
        this.auditEntryRepository = auditEntryRepository;
        this.userRepository = userRepository;
    }

    public void record(Long actorId, String action, String entity, Long entityId, String detail) {
        User actor = userRepository.getReferenceById(actorId);
        auditEntryRepository.save(new AuditEntry(actor, action, entity, entityId, detail));
    }
}
