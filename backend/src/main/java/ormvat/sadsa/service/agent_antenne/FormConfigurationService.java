package ormvat.sadsa.service.agent_antenne;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import ormvat.sadsa.model.SousRubrique;
import ormvat.sadsa.repository.SousRubriqueRepository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormConfigurationService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final SousRubriqueRepository sousRubriqueRepository;
    
    private Map<String, List<Map<String, Object>>> sousRubriqueFormConfigs = new HashMap<>();

    @PostConstruct
    public void loadFormConfigurations() {
        try {
            // Load from classpath resources
            Resource formsResource = resourceLoader.getResource("classpath:json_forms");
            
            if (formsResource.exists()) {
                loadFormsFromResource(formsResource);
            } else {
                log.warn("Forms directory not found in classpath, loading from filesystem");
                loadFormsFromFilesystem();
            }
            
        } catch (Exception e) {
            log.error("Failed to load form configurations", e);
        }
    }

    private void loadFormsFromFilesystem() {
        try {
            Path formsPath = Paths.get("src/main/resources/json_forms");
            if (!Files.exists(formsPath)) {
                log.warn("Forms directory not found: {}", formsPath);
                return;
            }
            
            walkDirectoryAndLoadForms(formsPath);
            
        } catch (Exception e) {
            log.error("Error loading forms from filesystem", e);
        }
    }

    private void loadFormsFromResource(Resource resource) {
        // This would be used when running from JAR
        // For now, we'll focus on filesystem loading during development
        loadFormsFromFilesystem();
    }

    private void walkDirectoryAndLoadForms(Path rootPath) throws IOException {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".json"))
                 .forEach(this::loadFormFile);
        }
    }

    private void loadFormFile(Path formPath) {
        try {
            String content = Files.readString(formPath);
            Map<String, Object> formConfig = objectMapper.readValue(content, Map.class);
            
            // Extract category and subcategory from path
            String pathStr = formPath.toString().replace("\\", "/");
            String[] pathParts = pathStr.split("/");
            
            // Find the relevant sous-rubrique based on path
            String categoryName = extractCategoryFromPath(pathParts);
            String subCategoryName = extractSubCategoryFromPath(pathParts);
            
            if (categoryName != null && subCategoryName != null) {
                String key = categoryName + "|" + subCategoryName;
                sousRubriqueFormConfigs.computeIfAbsent(key, k -> new ArrayList<>()).add(formConfig);
                
                log.debug("Loaded form: {} for category: {}", formPath.getFileName(), key);
            }
            
        } catch (Exception e) {
            log.error("Failed to load form file: {}", formPath, e);
        }
    }

    private String extractCategoryFromPath(String[] pathParts) {
        for (int i = 0; i < pathParts.length; i++) {
            if ("json_forms".equals(pathParts[i]) && i + 1 < pathParts.length) {
                return pathParts[i + 1];
            }
        }
        return null;
    }

    private String extractSubCategoryFromPath(String[] pathParts) {
        for (int i = 0; i < pathParts.length; i++) {
            if ("json_forms".equals(pathParts[i]) && i + 2 < pathParts.length) {
                return pathParts[i + 2];
            }
        }
        return null;
    }

    /**
     * Get form configurations for a specific sous-rubrique
     */
    public List<Map<String, Object>> getFormsForSousRubrique(Long sousRubriqueId) {
        try {
            Optional<SousRubrique> sousRubriqueOpt = sousRubriqueRepository.findById(sousRubriqueId);
            if (sousRubriqueOpt.isEmpty()) {
                return Collections.emptyList();
            }
            
            SousRubrique sousRubrique = sousRubriqueOpt.get();
            String rubriqueDesignation = sousRubrique.getRubrique().getDesignation();
            String sousRubriqueDesignation = sousRubrique.getDesignation();
            
            // Try different key combinations to find forms
            String[] possibleKeys = {
                rubriqueDesignation + "|" + sousRubriqueDesignation,
                mapToFolderName(rubriqueDesignation) + "|" + mapToFolderName(sousRubriqueDesignation)
            };
            
            for (String key : possibleKeys) {
                List<Map<String, Object>> forms = sousRubriqueFormConfigs.get(key);
                if (forms != null && !forms.isEmpty()) {
                    return forms;
                }
            }
            
            // If no exact match, try partial matching
            return sousRubriqueFormConfigs.entrySet().stream()
                    .filter(entry -> entry.getKey().toLowerCase().contains(sousRubriqueDesignation.toLowerCase()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(Collections.emptyList());
                    
        } catch (Exception e) {
            log.error("Error getting forms for sous-rubrique: {}", sousRubriqueId, e);
            return Collections.emptyList();
        }
    }

    private String mapToFolderName(String designation) {
        // Map database designations to folder names
        Map<String, String> mappings = new HashMap<>();
        mappings.put("FILIERES VEGETALES", "FILIERES VEGETALES");
        mappings.put("FILIERES ANIMALES", "FILIERES ANIMALES");
        mappings.put("AMENAGEMENT HYDRO-AGRICOLE", "AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE");
        
        // Add specific mappings for sous-rubriques
        mappings.put("Arboriculture fruitière", "Arboriculture fruitière");
        mappings.put("Serres agricoles", "Acquisition et installation des serres destinées à la production agricole");
        mappings.put("Matériel agricole", "Équipement des exploitations en matériel agricole");
        
        return mappings.getOrDefault(designation, designation);
    }

    /**
     * Get all available form configurations (for debugging)
     */
    public Map<String, List<Map<String, Object>>> getAllFormConfigurations() {
        return new HashMap<>(sousRubriqueFormConfigs);
    }
}