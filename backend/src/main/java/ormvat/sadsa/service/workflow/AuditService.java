package ormvat.sadsa.service.workflow;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ormvat.sadsa.model.AuditTrail;
import ormvat.sadsa.repository.AuditTrailRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditTrailRepository auditTrailRepository;

    public void logAction(Long userId, String action, String entityType, Long entityId, 
                         String oldValue, String newValue, String details) {
        AuditTrail audit = new AuditTrail();
        audit.setTimestamp(LocalDateTime.now());
        audit.setUserId(userId);
        audit.setAction(action);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setDetails(details);
        
        auditTrailRepository.save(audit);
        log.info("Audit logged: {} by user {} on {} {}", action, userId, entityType, entityId);
    }

    public List<AuditTrail> getAuditHistory(Long entityId, String entityType) {
        return auditTrailRepository.findByEntityIdAndEntityTypeOrderByTimestampDesc(entityId, entityType);
    }
}
