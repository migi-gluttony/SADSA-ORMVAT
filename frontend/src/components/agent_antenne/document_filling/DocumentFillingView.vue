<template>
  <div class="document-filling-container">
    <div class="navigation-header">
      <Button 
        label="Retour au détails du dossier" 
        icon="pi pi-arrow-left" 
        @click="goBack"
        class="p-button-outlined"
      />
    </div>
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressBar mode="indeterminate" />
      <p>Chargement des informations du dossier...</p>
    </div>

    <!-- Main Content -->
    <div v-else-if="dossierData" class="main-content">
      
      <!-- Header with Dossier Info -->
      <div class="dossier-header">
        <div class="header-content">
          <div class="dossier-title">
            <h1>
              <i class="pi pi-folder"></i>
              Dossier {{ dossierData.dossier.numeroDossier }}
            </h1>
            <Tag 
              :value="getStatusLabel(dossierData.dossier.status)" 
              :severity="getStatusSeverity(dossierData.dossier.status)"
              class="status-tag"
            />
          </div>
          
          <div class="dossier-details">
            <div class="detail-grid">
              <div class="detail-item">
                <span class="label">SABA:</span>
                <span class="value">{{ dossierData.dossier.saba }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Agriculteur:</span>
                <span class="value">{{ dossierData.dossier.agriculteurPrenom }} {{ dossierData.dossier.agriculteurNom }}</span>
              </div>
              <div class="detail-item">
                <span class="label">CIN:</span>
                <span class="value">{{ dossierData.dossier.agriculteurCin }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Téléphone:</span>
                <span class="value">{{ dossierData.dossier.agriculteurTelephone }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Type de projet:</span>
                <span class="value">{{ dossierData.dossier.sousRubriqueDesignation }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Antenne:</span>
                <span class="value">{{ dossierData.dossier.antenneDesignation }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Progress Statistics -->
        <div v-if="dossierData.statistics" class="progress-section">
          <div class="progress-header">
            <h3>Progression des Documents</h3>
            <div class="progress-percentage">
              {{ Math.round(dossierData.statistics.pourcentageCompletion) }}%
            </div>
          </div>
          
          <ProgressBar 
            :value="dossierData.statistics.pourcentageCompletion" 
            class="progress-bar"
          />
          
          <div class="progress-stats">
            <div class="stat-item">
              <span class="stat-number">{{ dossierData.statistics.documentsCompletes }}</span>
              <span class="stat-label">Complétés</span>
            </div>
            <div class="stat-item">
              <span class="stat-number">{{ dossierData.statistics.documentsManquants }}</span>
              <span class="stat-label">Manquants</span>
            </div>
            <div class="stat-item">
              <span class="stat-number">{{ dossierData.statistics.totalDocuments }}</span>
              <span class="stat-label">Total</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Documents Section -->
      <div class="documents-section">
        <h2>
          <i class="pi pi-file-edit"></i>
          Documents Requis
        </h2>

        <div class="documents-list">
          <div 
            v-for="document in dossierData.documentsRequis" 
            :key="document.id"
            class="document-card"
            :class="{ 
              'document-complete': document.status === 'COMPLETE',
              'document-missing': document.status === 'MISSING',
              'document-optional': !document.obligatoire 
            }"
          >
            <!-- Document Header -->
            <div class="document-header">
              <div class="document-info">
                <h3>
                  {{ document.nomDocument }}
                  <Tag 
                    v-if="document.obligatoire" 
                    value="Obligatoire" 
                    severity="danger" 
                    class="required-tag"
                  />
                  <Tag 
                    v-else 
                    value="Optionnel" 
                    severity="info" 
                    class="optional-tag"
                  />
                </h3>
                <p v-if="document.description" class="document-description">
                  {{ document.description }}
                </p>
              </div>
              
              <div class="document-status">
                <Tag 
                  :value="getDocumentStatusLabel(document.status)" 
                  :severity="getDocumentStatusSeverity(document.status)"
                />
              </div>
            </div>

            <!-- File Upload Section -->
            <div class="upload-section">
              <h4>
                <i class="pi pi-upload"></i>
                Fichiers Originaux
              </h4>
              
              <!-- Upload Area -->
              <div class="upload-area">
                <FileUpload
                  :ref="`fileUpload_${document.id}`"
                  mode="basic"
                  accept=".pdf,.jpg,.jpeg,.png,.gif"
                  :maxFileSize="10000000"
                  :multiple="true"
                  @select="(event) => onFileSelect(event, document.id)"
                  :auto="false"
                  chooseLabel="Sélectionner fichier(s)"
                  class="upload-component"
                />
                <small class="upload-help">
                  <i class="pi pi-info-circle"></i>
                  Formats acceptés: PDF, JPG, PNG, GIF (max 10MB par fichier)
                </small>
              </div>

              <!-- Uploaded Files List -->
              <div v-if="document.fichiers && document.fichiers.length > 0" class="uploaded-files">
                <h5>Fichiers uploadés ({{ document.fichiers.length }})</h5>
                <div class="files-list">
                  <div 
                    v-for="file in document.fichiers" 
                    :key="file.id"
                    class="file-item"
                  >
                    <div class="file-info">
                      <i :class="getFileIcon(file.formatFichier)"></i>
                      <div class="file-details">
                        <span class="file-name">{{ file.nomFichier }}</span>
                        <div class="file-meta">
                          <span class="file-size">{{ formatFileSize(file.tailleFichier) }}</span>
                          <span class="file-date">{{ formatDate(file.dateUpload) }}</span>
                          <span class="file-user">par {{ file.utilisateurNom }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="file-actions">
                      <Button 
                        icon="pi pi-download" 
                        @click="downloadFile(file)"
                        class="p-button-rounded p-button-text p-button-info"
                        v-tooltip="'Télécharger'"
                      />
                      <Button 
                        icon="pi pi-trash" 
                        @click="confirmDeleteFile(file)"
                        class="p-button-rounded p-button-text p-button-danger"
                        v-tooltip="'Supprimer'"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Dynamic Form Section -->
            <div v-if="document.formStructure && Object.keys(document.formStructure).length > 0" class="form-section">
              <h4>
                <i class="pi pi-list"></i>
                Formulaire Dynamique
              </h4>
              
              <div class="dynamic-form">
                <DynamicForm
                  :formStructure="document.formStructure"
                  :formData="document.formData || {}"
                  @form-change="(data) => onFormDataChange(data, document.id)"
                  @form-save="(data) => saveFormData(data, document.id)"
                />
              </div>
            </div>

            <!-- No Form Message -->
            <div v-else class="no-form-message">
              <i class="pi pi-info-circle"></i>
              <span>Aucun formulaire configuré pour ce document</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-content">
        <i class="pi pi-exclamation-triangle"></i>
        <h3>Erreur de chargement</h3>
        <p>{{ error }}</p>
        <Button 
          label="Réessayer" 
          icon="pi pi-refresh" 
          @click="loadDossierData"
          class="retry-button"
        />
      </div>
    </div>

    <!-- Confirmation Dialogs -->
    <ConfirmDialog />
    
    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router'; 

import { useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';
import DynamicForm from '@/components/agent_antenne/document_filling/DynamicForm.vue';

// PrimeVue Components
import Button from 'primevue/button';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import FileUpload from 'primevue/fileupload';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';

const route = useRoute();
const toast = useToast();
const confirm = useConfirm();
const router = useRouter();

// Reactive data
const loading = ref(true);
const error = ref('');
const dossierData = ref(null);
const uploadingFiles = ref(new Set());

// Props
const dossierId = computed(() => parseInt(route.params.dossierId));

// Lifecycle
onMounted(() => {
  loadDossierData();
});

// API Methods
async function loadDossierData() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await ApiService.get(`/agent_antenne/dossiers/${dossierId.value}/documents`);
    dossierData.value = response;
    
  } catch (err) {
    console.error('Erreur lors du chargement des données:', err);
    error.value = err.message || 'Impossible de charger les données du dossier';
    
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.value,
      life: 5000
    });
  } finally {
    loading.value = false;
  }
}

async function uploadFile(file, documentId) {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await ApiService.uploadFiles(
      `/agent_antenne/dossiers/${dossierId.value}/documents/${documentId}/upload`,
      formData,
      (progress) => {
        // Could show upload progress here
        console.log('Upload progress:', progress);
      }
    );

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: `Fichier "${file.name}" uploadé avec succès`,
      life: 3000
    });

    // Reload data to show new file
    await loadDossierData();
    
  } catch (err) {
    console.error('Erreur upload:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur Upload',
      detail: err.message || 'Erreur lors de l\'upload du fichier',
      life: 5000
    });
  }
}

async function deleteFile(fileId) {
  try {
    await ApiService.delete(`/agent_antenne/dossiers/${dossierId.value}/documents/piece-jointe/${fileId}`);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Fichier supprimé avec succès',
      life: 3000
    });
    
    await loadDossierData();
    
  } catch (err) {
    console.error('Erreur suppression:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la suppression',
      life: 5000
    });
  }
}

async function saveFormData(formData, documentId) {
  try {
    await ApiService.post(
      `/agent_antenne/dossiers/${dossierId.value}/documents/${documentId}/form-data`,
      { formData }
    );
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Données du formulaire sauvegardées',
      life: 3000
    });
    
    // Update local data
    const document = dossierData.value.documentsRequis.find(d => d.id === documentId);
    if (document) {
      document.formData = formData;
      document.status = 'COMPLETE';
    }
    
  } catch (err) {
    console.error('Erreur sauvegarde formulaire:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la sauvegarde',
      life: 5000
    });
  }
}

// Event Handlers
async function onFileSelect(event, documentId) {
  const files = event.files;
  
  for (const file of files) {
    // Validate file type
    if (!isValidFileType(file.type)) {
      toast.add({
        severity: 'error',
        summary: 'Type de fichier invalide',
        detail: `Le fichier "${file.name}" n'est pas autorisé. Seuls les PDF et images sont acceptés.`,
        life: 5000
      });
      continue;
    }
    
    // Validate file size
    if (file.size > 10 * 1024 * 1024) {
      toast.add({
        severity: 'error',
        summary: 'Fichier trop volumineux',
        detail: `Le fichier "${file.name}" dépasse la taille maximum de 10MB.`,
        life: 5000
      });
      continue;
    }
    
    uploadingFiles.value.add(`${documentId}_${file.name}`);
    await uploadFile(file, documentId);
    uploadingFiles.value.delete(`${documentId}_${file.name}`);
  }
  
  // Clear the file upload component
  const fileUploadRef = this.$refs[`fileUpload_${documentId}`];
  if (fileUploadRef && fileUploadRef[0]) {
    fileUploadRef[0].clear();
  }
}

function onFormDataChange(formData, documentId) {
  // Auto-save could be implemented here
  console.log('Form data changed for document', documentId, formData);
}

function confirmDeleteFile(file) {
  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer le fichier "${file.nomFichier}" ?`,
    header: 'Confirmer la suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    accept: () => {
      deleteFile(file.id);
    }
  });
}

async function downloadFile(file) {
  try {
    const response = await fetch(`/api/agent_antenne/dossiers/${dossierId.value}/documents/piece-jointe/${file.id}/download`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Erreur de téléchargement');
    }
    
    // Create download link
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', file.nomFichier);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Fichier téléchargé avec succès',
      life: 3000
    });
    
  } catch (err) {
    console.error('Erreur téléchargement:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Impossible de télécharger le fichier',
      life: 3000
    });
  }
}

// Utility Functions
function isValidFileType(mimeType) {
  const validTypes = [
    'application/pdf',
    'image/jpeg',
    'image/jpg', 
    'image/png',
    'image/gif'
  ];
  return validTypes.includes(mimeType);
}

function getFileIcon(extension) {
  switch (extension?.toLowerCase()) {
    case 'pdf':
      return 'pi pi-file-pdf';
    case 'jpg':
    case 'jpeg':
    case 'png':
    case 'gif':
      return 'pi pi-image';
    default:
      return 'pi pi-file';
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString) {
  if (!dateString) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(dateString));
}

function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En cours',
    'APPROVED': 'Approuvé',
    'REJECTED': 'Rejeté',
    'COMPLETED': 'Complété'
  };
  return labels[status] || status;
}

function getStatusSeverity(status) {
  const severities = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success'
  };
  return severities[status] || 'secondary';
}

function getDocumentStatusLabel(status) {
  const labels = {
    'COMPLETE': 'Complet',
    'MISSING': 'Manquant',
    'PENDING': 'En attente'
  };
  return labels[status] || status;
}

function getDocumentStatusSeverity(status) {
  const severities = {
    'COMPLETE': 'success',
    'MISSING': 'danger',
    'PENDING': 'warning'
  };
  return severities[status] || 'secondary';
}


function goBack() {
  router.push(`/agent_antenne/dossiers/${dossierId.value}`);
}


</script>

<style scoped>
.document-filling-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1rem;
}


.navigation-header{
  padding-bottom:1rem;
}


/* Loading State */
.loading-container {
  text-align: center;
  padding: 3rem;
}

.loading-container p {
  margin-top: 1rem;
  color: var(--text-color-secondary);
}

/* Header */
.dossier-header {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--border-color);
}

.dossier-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.dossier-title h1 {
  color: var(--primary-color);
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-tag {
  font-size: 0.875rem;
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item .label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  font-weight: 500;
}

.detail-item .value {
  font-weight: 600;
  color: var(--text-color);
}

/* Progress Section */
.progress-section {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid var(--border-color);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.progress-header h3 {
  color: var(--primary-color);
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
}

.progress-percentage {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-color);
}

.progress-bar {
  height: 12px;
  margin-bottom: 1rem;
}

.progress-stats {
  display: flex;
  gap: 2rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.stat-number {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--primary-color);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

/* Documents Section */
.documents-section {
  margin-bottom: 2rem;
}

.documents-section h2 {
  color: var(--primary-color);
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.documents-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Document Cards */
.document-card {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.document-card.document-complete {
  border-left: 4px solid #10b981;
}

.document-card.document-missing {
  border-left: 4px solid #ef4444;
}

.document-card.document-optional {
  border-left: 4px solid #3b82f6;
}

.document-header {
  background: #f8f9fa;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.document-info h3 {
  color: var(--text-color);
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.document-description {
  color: var(--text-color-secondary);
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
}

.required-tag,
.optional-tag {
  font-size: 0.75rem;
}

/* Upload Section */
.upload-section {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.upload-section h4 {
  color: var(--primary-color);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.upload-area {
  margin-bottom: 1.5rem;
}

.upload-component {
  width: 100%;
}

:deep(.p-fileupload-basic .p-button) {
  width: 100%;
  padding: 1rem;
  border: 2px dashed var(--border-color);
  background: var(--surface-ground);
  color: var(--text-color);
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.p-fileupload-basic .p-button:hover) {
  border-color: var(--primary-color);
  background: rgba(var(--primary-color-rgb), 0.05);
}

.upload-help {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

/* Uploaded Files */
.uploaded-files h5 {
  color: var(--text-color);
  font-size: 0.9rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.file-info i {
  font-size: 1.5rem;
  color: var(--primary-color);
}

.file-details {
  flex: 1;
}

.file-name {
  font-weight: 600;
  color: var(--text-color);
  display: block;
  margin-bottom: 0.25rem;
}

.file-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--text-color-secondary);
}

.file-actions {
  display: flex;
  gap: 0.25rem;
}

/* Form Section */
.form-section {
  padding: 1.5rem;
}

.form-section h4 {
  color: var(--primary-color);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dynamic-form {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.no-form-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--text-color-secondary);
  font-style: italic;
  background: #f8f9fa;
  border-radius: 8px;
  margin: 1.5rem;
}

/* Error State */
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-content i {
  font-size: 3rem;
  color: #ef4444;
  margin-bottom: 1rem;
}

.error-content h3 {
  color: var(--text-color);
  margin-bottom: 1rem;
}

.error-content p {
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
}

.retry-button {
  background: var(--primary-color);
}

/* Responsive */
@media (max-width: 768px) {
  .document-filling-container {
    padding: 0.5rem;
  }
  
  .dossier-header {
    padding: 1.5rem;
  }
  
  .dossier-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .progress-stats {
    justify-content: space-around;
  }
  
  .document-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .file-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .file-actions {
    align-self: flex-end;
  }
}

/* Dark Mode */
.dark-mode .dossier-header,
.dark-mode .document-card {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .document-header {
  background-color: #374151;
  border-color: #4b5563;
}

.dark-mode .file-item,
.dark-mode .dynamic-form,
.dark-mode .no-form-message {
  background-color: #374151;
  border-color: #4b5563;
}
</style>