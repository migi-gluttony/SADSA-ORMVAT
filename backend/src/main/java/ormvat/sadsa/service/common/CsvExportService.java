package ormvat.sadsa.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.workflow.WorkflowService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvExportService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowService workflowService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Export dossiers to CSV based on user role
     */
    public ResponseEntity<Resource> exportDossiersToCsv(String userEmail, String exportType) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Dossier> dossiers = getDossiersForExport(utilisateur, exportType);
            
            String csvContent = createDossierCsvContent(dossiers, utilisateur, exportType);
            
            String filename = generateCsvFilename(exportType, utilisateur);
            
            ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors de l'export CSV", e);
            throw new RuntimeException("Erreur lors de l'export CSV: " + e.getMessage());
        }
    }

    /**
     * Get dossiers for export based on user role
     */
    private List<Dossier> getDossiersForExport(Utilisateur utilisateur, String exportType) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                if (utilisateur.getAntenne() != null) {
                    return dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
                }
                return List.of();

            case AGENT_GUC:
                return dossierRepository.findAll().stream()
                        .filter(d -> !d.getStatus().equals(Dossier.DossierStatus.DRAFT))
                        .collect(java.util.stream.Collectors.toList());

            case AGENT_COMMISSION_TERRAIN:
                return dossierRepository.findAll().stream()
                        .filter(d -> d.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                                d.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                                d.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                                d.getStatus().equals(Dossier.DossierStatus.REJECTED))
                        .collect(java.util.stream.Collectors.toList());

            case ADMIN:
                return dossierRepository.findAll();

            default:
                return List.of();
        }
    }

    /**
     * Create CSV content with dossier data
     */
    private String createDossierCsvContent(List<Dossier> dossiers, Utilisateur utilisateur, String exportType) {
        StringBuilder csv = new StringBuilder();
        
        // Add BOM for proper UTF-8 display in Excel
        csv.append('\uFEFF');
        
        // Add header
        csv.append(createCsvHeader()).append("\n");
        
        // Add data rows
        for (Dossier dossier : dossiers) {
            csv.append(createCsvRow(dossier)).append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Create CSV header row
     */
    private String createCsvHeader() {
        StringJoiner header = new StringJoiner(",");
        
        header.add("\"ID\"")
              .add("\"Numéro Dossier\"")
              .add("\"SABA\"")
              .add("\"Référence\"")
              .add("\"Statut\"")
              .add("\"Date Création\"")
              .add("\"Agriculteur (Nom)\"")
              .add("\"Agriculteur (Prénom)\"")
              .add("\"CIN\"")
              .add("\"Téléphone\"")
              .add("\"Commune Rurale\"")
              .add("\"Douar\"")
              .add("\"Rubrique\"")
              .add("\"Sous-Rubrique\"")
              .add("\"Antenne\"")
              .add("\"CDA\"")
              .add("\"Montant Subvention\"")
              .add("\"Utilisateur Créateur\"")
              .add("\"Date Soumission\"")
              .add("\"Date Approbation\"")
              .add("\"Étape Actuelle\"")
              .add("\"Emplacement Actuel\"")
              .add("\"Jours Restants\"")
              .add("\"En Retard\"");
        
        return header.toString();
    }

    /**
     * Create CSV row for a dossier
     */
    private String createCsvRow(Dossier dossier) {
        StringJoiner row = new StringJoiner(",");
          // Get workflow information
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByIdDossierOrderByDateEntreeDesc(dossier.getId());
        WorkflowInstance currentWorkflow = workflows.isEmpty() ? null : workflows.get(0);
        
        // Get timing information from WorkflowService
        TimingDTO timingInfo = null;
        try {
            timingInfo = workflowService.getTimingInfo(dossier.getId());
        } catch (Exception e) {
            log.warn("Could not get timing info for dossier {}: {}", dossier.getId(), e.getMessage());
        }
        
        // Basic dossier info
        row.add(csvValue(dossier.getId()))
           .add(csvValue(dossier.getNumeroDossier()))
           .add(csvValue(dossier.getSaba()))
           .add(csvValue(dossier.getReference()))
           .add(csvValue(dossier.getStatus().name()))
           .add(csvValue(dossier.getDateCreation() != null ? 
                   dossier.getDateCreation().format(DATE_FORMATTER) : ""));

        // Agriculteur info
        Agriculteur agriculteur = dossier.getAgriculteur();
        row.add(csvValue(agriculteur.getNom()))
           .add(csvValue(agriculteur.getPrenom()))
           .add(csvValue(agriculteur.getCin()))
           .add(csvValue(agriculteur.getTelephone()))
           .add(csvValue(agriculteur.getCommuneRurale() != null ? 
                   agriculteur.getCommuneRurale().getDesignation() : ""))
           .add(csvValue(agriculteur.getDouar() != null ? 
                   agriculteur.getDouar().getDesignation() : ""));

        // Project info
        row.add(csvValue(dossier.getSousRubrique().getRubrique().getDesignation()))
           .add(csvValue(dossier.getSousRubrique().getDesignation()))
           .add(csvValue(dossier.getAntenne().getDesignation()))
           .add(csvValue(dossier.getAntenne().getCda() != null ? 
                   dossier.getAntenne().getCda().getDescription() : "N/A"));

        // Financial info
        row.add(csvValue(dossier.getMontantSubvention() != null ? 
                   dossier.getMontantSubvention().toString() : ""));

        // User info
        row.add(csvValue(dossier.getUtilisateurCreateur() != null ? 
                   dossier.getUtilisateurCreateur().getPrenom() + " " + 
                   dossier.getUtilisateurCreateur().getNom() : ""));

        // Dates
        row.add(csvValue(dossier.getDateSubmission() != null ? 
                   dossier.getDateSubmission().format(DATE_FORMATTER) : ""))
           .add(csvValue(dossier.getDateApprobation() != null ? 
                   dossier.getDateApprobation().format(DATE_FORMATTER) : ""));        // Workflow info
        if (currentWorkflow != null && timingInfo != null) {
            row.add(csvValue(timingInfo.getCurrentStep()))
               .add(csvValue(timingInfo.getAssignedTo()))
               .add(csvValue(timingInfo.getJoursRestants().toString()))
               .add(csvValue(timingInfo.getEnRetard() ? "Oui" : "Non"));
        } else if (currentWorkflow != null) {
            row.add(csvValue("Phase " + currentWorkflow.getIdEtape()))
               .add(csvValue("Non défini"))
               .add(csvValue("0"))
               .add(csvValue("Non"));
        } else {
            row.add(csvValue("Phase Antenne"))
               .add(csvValue("ANTENNE"))
               .add(csvValue("3"))
               .add(csvValue("Non"));
        }

        return row.toString();
    }

    /**
     * Properly escape CSV values
     */
    private String csvValue(Object value) {
        if (value == null) {
            return "\"\"";
        }
        
        String str = value.toString();
        
        // Escape quotes by doubling them and wrap in quotes
        if (str.contains("\"") || str.contains(",") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }
        
        return "\"" + str + "\"";
    }

    private String generateCsvFilename(String exportType, Utilisateur utilisateur) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String role = utilisateur.getRole().name().toLowerCase();
        return String.format("dossiers_%s_%s_%s.csv", role, exportType, timestamp);
    }
}