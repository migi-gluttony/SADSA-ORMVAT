package ormvat.sadsa.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final EtapeRepository etapeRepository;
    private final JoursFerieRepository joursFerieRepository;
    private final AuditService auditService;

    @Transactional
    public void initializeWorkflow(Long dossierId, Long userId, String commentaire) {
        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setIdDossier(dossierId);
        workflow.setIdEtape(1L); // Start at Phase 1
        workflow.setDateEntree(LocalDateTime.now());
        workflow.setIdUser(userId);
        workflow.setCommentaire(commentaire);
        
        workflowInstanceRepository.save(workflow);
        
        auditService.logAction(userId, "WORKFLOW_INIT", "Dossier", dossierId, 
                              null, "Phase 1", "Workflow initialized");
    }

    @Transactional
    public void moveToStep(Long dossierId, Long targetStepId, Long userId, String commentaire) {
        // Close current step
        WorkflowInstance currentStep = getCurrentStep(dossierId);
        if (currentStep != null) {
            currentStep.setDateSortie(LocalDateTime.now());
            workflowInstanceRepository.save(currentStep);
        }

        // Create new step
        WorkflowInstance newStep = new WorkflowInstance();
        newStep.setIdDossier(dossierId);
        newStep.setIdEtape(targetStepId);
        newStep.setDateEntree(LocalDateTime.now());
        newStep.setIdUser(userId);
        newStep.setCommentaire(commentaire);
        
        workflowInstanceRepository.save(newStep);
        
        auditService.logAction(userId, "WORKFLOW_MOVE", "Dossier", dossierId,
                              currentStep != null ? "Phase " + currentStep.getIdEtape() : null,
                              "Phase " + targetStepId, commentaire);
    }

    public WorkflowInstance getCurrentStep(Long dossierId) {
        return workflowInstanceRepository.findByIdDossierAndDateSortieIsNull(dossierId)
                .orElse(null);
    }

    public List<WorkflowInstance> getWorkflowHistory(Long dossierId) {
        return workflowInstanceRepository.findByIdDossierOrderByDateEntreeDesc(dossierId);
    }

    public TimingDTO getTimingInfo(Long dossierId) {
        WorkflowInstance currentStep = getCurrentStep(dossierId);
        if (currentStep == null) {
            return TimingDTO.builder()
                    .currentStep("Non défini")
                    .enRetard(false)
                    .joursRestants(0)
                    .joursRetard(0)
                    .build();
        }

        Etape etape = etapeRepository.findById(currentStep.getIdEtape()).orElse(null);
        if (etape == null) {
            return TimingDTO.builder()
                    .currentStep("Phase " + currentStep.getIdEtape())
                    .enRetard(false)
                    .joursRestants(0)
                    .joursRetard(0)
                    .build();
        }

        LocalDateTime dateLimite = calculateDeadline(currentStep.getDateEntree(), etape.getDureeJours());
        int joursRestants = calculateRemainingWorkingDays(dateLimite);
        boolean enRetard = joursRestants < 0;
        int joursRetard = enRetard ? Math.abs(joursRestants) : 0;
        joursRestants = Math.max(0, joursRestants);

        return TimingDTO.builder()
                .currentStep(etape.getDesignation())
                .assignedTo(getAssignedRole(currentStep.getIdEtape()))
                .dateEntree(currentStep.getDateEntree())
                .delaiMaxJours(etape.getDureeJours())
                .joursRestants(joursRestants)
                .enRetard(enRetard)
                .joursRetard(joursRetard)
                .dateLimite(dateLimite)
                .build();
    }

    private LocalDateTime calculateDeadline(LocalDateTime startDate, int workingDays) {
        LocalDateTime current = startDate;
        int addedDays = 0;
        
        Set<LocalDateTime> holidays = joursFerieRepository.findAll().stream()
                .map(jf -> jf.getDate().atStartOfDay())
                .collect(Collectors.toSet());

        while (addedDays < workingDays) {
            current = current.plusDays(1);
            if (isWorkingDay(current, holidays)) {
                addedDays++;
            }
        }
        
        return current.withHour(17).withMinute(0).withSecond(0);
    }

    private int calculateRemainingWorkingDays(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(deadline)) {
            return -calculateWorkingDaysBetween(deadline, now);
        }
        return calculateWorkingDaysBetween(now, deadline);
    }

    private int calculateWorkingDaysBetween(LocalDateTime start, LocalDateTime end) {
        Set<LocalDateTime> holidays = joursFerieRepository.findAll().stream()
                .map(jf -> jf.getDate().atStartOfDay())
                .collect(Collectors.toSet());

        LocalDateTime current = start;
        int workingDays = 0;

        while (current.isBefore(end)) {
            current = current.plusDays(1);
            if (isWorkingDay(current, holidays)) {
                workingDays++;
            }
        }

        return workingDays;
    }

    private boolean isWorkingDay(LocalDateTime date, Set<LocalDateTime> holidays) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && 
               dayOfWeek != DayOfWeek.SUNDAY && 
               !holidays.contains(date.toLocalDate().atStartOfDay());
    }

    private String getAssignedRole(Long etapeId) {
        switch (etapeId.intValue()) {
            case 1:
            case 5:
                return "Agent Antenne";
            case 2:
            case 4:
            case 6:
            case 8:
                return "Agent GUC";
            case 3:
                return "Commission Visite Terrain";
            case 7:
                return "Service Technique";
            default:
                return "Non défini";
        }
    }
}
