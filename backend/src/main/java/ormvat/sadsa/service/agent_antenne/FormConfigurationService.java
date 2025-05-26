package ormvat.sadsa.service.agent_antenne;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ormvat.sadsa.model.DocumentRequis;
import ormvat.sadsa.repository.DocumentRequisRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormConfigurationService {

    private final DocumentRequisRepository documentRequisRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get form configurations for a specific sous-rubrique from database
     */
    public List<Map<String, Object>> getFormsForSousRubrique(Long sousRubriqueId) {
        try {
            List<DocumentRequis> documents = documentRequisRepository.findBySousRubriqueId(sousRubriqueId);
            List<Map<String, Object>> formConfigs = new ArrayList<>();

            for (DocumentRequis doc : documents) {
                if (doc.getConfigFormulaireJson() != null && !doc.getConfigFormulaireJson().trim().isEmpty()) {
                    try {
                        Map<String, Object> config = objectMapper.readValue(doc.getConfigFormulaireJson(), Map.class);
                        
                        // Add metadata
                        config.put("formId", generateFormId(doc.getNomDocument()));
                        config.put("documentId", doc.getId());
                        config.put("required", doc.getObligatoire());
                        config.put("documentName", doc.getNomDocument());
                        config.put("documentDescription", doc.getDescription());
                        
                        formConfigs.add(config);
                        
                    } catch (Exception e) {
                        log.error("Error parsing form config for document: {}", doc.getNomDocument(), e);
                    }
                }
            }

            return formConfigs;
            
        } catch (Exception e) {
            log.error("Error getting forms for sous-rubrique: {}", sousRubriqueId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific form configuration
     */
    public Optional<Map<String, Object>> getFormConfiguration(Long documentId) {
        try {
            Optional<DocumentRequis> docOpt = documentRequisRepository.findById(documentId);
            
            if (docOpt.isPresent() && docOpt.get().getConfigFormulaireJson() != null) {
                DocumentRequis doc = docOpt.get();
                Map<String, Object> config = objectMapper.readValue(doc.getConfigFormulaireJson(), Map.class);
                
                // Add metadata
                config.put("formId", generateFormId(doc.getNomDocument()));
                config.put("documentId", doc.getId());
                config.put("required", doc.getObligatoire());
                config.put("documentName", doc.getNomDocument());
                
                return Optional.of(config);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error getting form configuration for document: {}", documentId, e);
            return Optional.empty();
        }
    }

    /**
     * Generate a unique form ID from document name
     */
    private String generateFormId(String documentName) {
        return documentName.toLowerCase()
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Check if a form configuration exists for a sous-rubrique
     */
    public boolean hasFormsForSousRubrique(Long sousRubriqueId) {
        return !documentRequisRepository.findBySousRubriqueId(sousRubriqueId).isEmpty();
    }

    /**
     * Get required documents count for a sous-rubrique
     */
    public int getRequiredDocumentsCount(Long sousRubriqueId) {
        return (int) documentRequisRepository.findBySousRubriqueId(sousRubriqueId)
                .stream()
                .filter(DocumentRequis::getObligatoire)
                .count();
    }

    /**
     * Get all document names for a sous-rubrique
     */
    public List<String> getDocumentNames(Long sousRubriqueId) {
        return documentRequisRepository.findBySousRubriqueId(sousRubriqueId)
                .stream()
                .map(DocumentRequis::getNomDocument)
                .toList();
    }
}