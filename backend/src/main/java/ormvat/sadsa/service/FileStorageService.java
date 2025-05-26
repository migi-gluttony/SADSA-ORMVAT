package ormvat.sadsa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final List<String> allowedExtensions = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "gif", "txt"
    );
    private final long maxFileSize = 10 * 1024 * 1024; // 10MB

    public FileStorageService(@Value("${app.upload.dir:./uploads/ormvat_sadsa}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store a file and return the file path
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        return storeFile(file, subDirectory, null);
    }

    /**
     * Store a file with a custom name
     */
    public String storeFile(MultipartFile file, String subDirectory, String customFileName) {
        // Validate file
        validateFile(file);

        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Generate unique filename if not provided
            if (customFileName == null) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                String extension = getFileExtension(fileName);
                customFileName = timestamp + "_" + uuid + "." + extension;
            }

            // Create subdirectory if it doesn't exist
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);
            
            // Final target path
            Path finalTargetPath = targetLocation.resolve(customFileName);

            // Copy file to the target location (Replacing existing file with the same name)
            Files.copy(file.getInputStream(), finalTargetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", finalTargetPath.toString());
            return finalTargetPath.toString();

        } catch (IOException ex) {
            log.error("Could not store file {}. Please try again!", fileName, ex);
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    /**
     * Load a file as Resource
     */
    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = Paths.get(filePath);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + filePath, ex);
        }
    }

    /**
     * Delete a file
     */
    public boolean deleteFile(String filePath) {
        try {
            Path file = Paths.get(filePath);
            return Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.error("Could not delete file {}", filePath, ex);
            return false;
        }
    }

    /**
     * Get file size in a human-readable format
     */
    public String getFileSizeFormatted(String filePath) {
        try {
            Path file = Paths.get(filePath);
            long bytes = Files.size(file);
            return formatFileSize(bytes);
        } catch (IOException ex) {
            return "Unknown";
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String filePath) {
        try {
            Path file = Paths.get(filePath);
            return Files.exists(file);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Get MIME type of a file
     */
    public String getMimeType(String filePath) {
        try {
            Path file = Paths.get(filePath);
            return Files.probeContentType(file);
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    // Private helper methods

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + formatFileSize(maxFileSize));
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new RuntimeException("File type not allowed. Allowed types: " + String.join(", ", allowedExtensions));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null;
        }
        
        return fileName.substring(lastDotIndex + 1);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Create a subdirectory for a specific dossier
     */
    public String createDossierDirectory(Long dossierId) {
        String subDirectory = "dossiers/" + dossierId;
        try {
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);
            return subDirectory;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create directory for dossier " + dossierId, ex);
        }
    }

    /**
     * Get the storage location path
     */
    public Path getStorageLocation() {
        return this.fileStorageLocation;
    }
}