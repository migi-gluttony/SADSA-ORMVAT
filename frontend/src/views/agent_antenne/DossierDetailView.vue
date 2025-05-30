<template>
  <div class="dossier-detail-container">
    <UserInfoHeader 
      search-placeholder="Rechercher..."
      @search="handleSearch"
    />

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement du dossier...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Erreur</h3>
      <p>{{ error }}</p>
      <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" />
    </div>

    <!-- Dossier Detail -->
    <div v-else-if="dossierDetail" class="dossier-content">
      <!-- Header with navigation -->
      <div class="detail-header">
        <div class="header-nav">
          <Button 
            label="Retour à la liste" 
            icon="pi pi-arrow-left" 
            @click="goBack"
            class="p-button-outlined"
          />
          <div class="breadcrumb">
            <span>Mes Dossiers</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ dossierDetail.dossier.reference }}</span>
          </div>
        </div>
        <div class="header-actions">
          <Button 
            v-if="dossierDetail.peutEtreEnvoye"
            label="Envoyer au GUC" 
            icon="pi pi-send" 
            @click="confirmSendToGUC"
            class="p-button-success"
          />
          <Button 
            v-if="dossierDetail.peutEtreSupprime"
            label="Supprimer" 
            icon="pi pi-trash" 
            @click="confirmDelete"
            class="p-button-danger"
          />
        </div>
      </div>

      <!-- Dossier Summary -->
      <div class="dossier-summary component-card">
        <div class="summary-header">
          <h2>{{ dossierDetail.dossier.reference }}</h2>
          <Tag 
            :value="dossierDetail.dossier.statut" 
            :severity="getStatusSeverity(dossierDetail.dossier.statut)"
          />
        </div>
        
        <div class="summary-grid">
          <div class="summary-section">
            <h4>Agriculteur</h4>
            <div class="info-lines">
              <div><strong>Nom:</strong> {{ dossierDetail.agriculteur.prenom }} {{ dossierDetail.agriculteur.nom }}</div>
              <div><strong>CIN:</strong> {{ dossierDetail.agriculteur.cin }}</div>
              <div><strong>Téléphone:</strong> {{ dossierDetail.agriculteur.telephone }}</div>
              <div v-if="dossierDetail.agriculteur.communeRurale">
                <strong>Commune:</strong> {{ dossierDetail.agriculteur.communeRurale }}
              </div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>Projet</h4>
            <div class="info-lines">
              <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
              <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
              <div><strong>CDA:</strong> {{ dossierDetail.dossier.cdaNom }}</div>
              <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>État du dossier</h4>
            <div class="info-lines">
              <div><strong>Statut:</strong> {{ dossierDetail.dossier.statut }}</div>
              <div class="time-info">
                <strong>Temps restant:</strong>
                <span :class="{
                  'time-critical': dossierDetail.joursRestants <= 1,
                  'time-warning': dossierDetail.joursRestants <= 2 && dossierDetail.joursRestants > 1,
                  'time-ok': dossierDetail.joursRestants > 2
                }">
                  {{ dossierDetail.joursRestants }} jour(s)
                </span>
              </div>
              <div><strong>Modifiable:</strong> {{ dossierDetail.peutEtreModifie ? 'Oui' : 'Non' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Forms Section -->
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Formulaires à remplir</h2>
          <p>Complétez tous les formulaires requis pour ce type de projet</p>
        </div>

        <div v-if="dossierDetail.availableForms.length === 0" class="no-forms">
          <i class="pi pi-info-circle"></i>
          <p>Aucun formulaire configuré pour ce type de projet.</p>
        </div>

        <div v-else class="forms-grid">
          <div 
            v-for="form in dossierDetail.availableForms" 
            :key="form.formId"
            class="form-card"
            :class="{ 'form-completed': form.isCompleted }"
          >
            <div class="form-header">
              <div class="form-title">
                <h3>{{ form.title }}</h3>
                <Tag v-if="form.isCompleted" value="Complété" severity="success" />
                <Tag v-else value="En attente" severity="warning" />
              </div>
              <Button 
                :label="form.isCompleted ? 'Modifier' : 'Remplir'" 
                :icon="form.isCompleted ? 'pi pi-pencil' : 'pi pi-plus'" 
                @click="openFormDialog(form)"
                :disabled="!dossierDetail.peutEtreModifie"
                class="p-button-sm"
              />
            </div>
            
            <div class="form-description">
              <p>{{ form.description }}</p>
            </div>

            <div v-if="form.requiredDocuments && form.requiredDocuments.length > 0" class="required-docs">
              <h5>Documents requis:</h5>
              <ul>
                <li v-for="doc in form.requiredDocuments" :key="doc">{{ doc }}</li>
              </ul>
            </div>

            <div v-if="form.lastModified" class="form-meta">
              <small>Dernière modification: {{ formatDate(form.lastModified) }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- Files Section -->
      <div class="files-section component-card">
        <div class="section-header">
          <h3><i class="pi pi-file"></i> Documents téléchargés</h3>
          <Button 
            label="Télécharger un document" 
            icon="pi pi-upload" 
            @click="showUploadDialog = true"
            :disabled="!dossierDetail.peutEtreModifie"
            class="p-button-outlined p-button-sm"
          />
        </div>

        <div v-if="dossierDetail.pieceJointes.length === 0" class="no-files">
          <i class="pi pi-file"></i>
          <p>Aucun document téléchargé pour ce dossier.</p>
        </div>

        <div v-else class="files-list">
          <div 
            v-for="file in dossierDetail.pieceJointes" 
            :key="file.id"
            class="file-item"
          >
            <div class="file-info">
              <div class="file-icon">
                <i class="pi pi-file-pdf"></i>
              </div>
              <div class="file-details">
                <div class="file-name">{{ file.customTitle || file.fileName }}</div>
                <div class="file-meta">
                  <span>{{ file.documentType }}</span>
                  <span>•</span>
                  <span>{{ formatDate(file.dateUpload) }}</span>
                  <Tag v-if="file.isOriginalDocument" value="Original" severity="info" />
                </div>
              </div>
            </div>
            <div class="file-actions">
              <Button 
                icon="pi pi-download" 
                @click="downloadFile(file)"
                class="p-button-outlined p-button-sm"
                v-tooltip.top="'Télécharger'"
              />
              <Button 
                v-if="dossierDetail.peutEtreModifie"
                icon="pi pi-trash" 
                @click="confirmDeleteFile(file)"
                class="p-button-danger p-button-outlined p-button-sm"
                v-tooltip.top="'Supprimer'"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Validation Errors -->
      <div v-if="dossierDetail.validationErrors.length > 0" class="validation-errors component-card">
        <h3><i class="pi pi-exclamation-triangle"></i> Problèmes à résoudre</h3>
        <ul class="error-list">
          <li v-for="error in dossierDetail.validationErrors" :key="error">{{ error }}</li>
        </ul>
      </div>
    </div>

    <!-- Form Dialog -->
    <Dialog 
      v-model:visible="formDialog.visible" 
      modal 
      :header="formDialog.form?.title"
      :style="{ width: '90vw', maxWidth: '800px' }"
      :dismissable-mask="false"
    >
      <div v-if="formDialog.form" class="form-dialog-content">
        <DynamicForm 
          ref="dynamicFormRef"
          :config-path="`data:application/json,${encodeURIComponent(JSON.stringify(formDialog.form.formConfig))}`"
          v-model="formDialog.formData"
          @change="onFormDataChange"
        />

        <!-- File Upload Section -->
        <div class="file-upload-section">
          <h4>Documents à joindre</h4>
          <div class="upload-area">
            <FileUpload
              ref="fileUploadRef"
              mode="advanced"
              multiple
              accept="*"
              :max-file-size="10000000"
              :auto="false"
              choose-label="Choisir des fichiers"
              upload-label="Télécharger"
              cancel-label="Annuler"
              @select="onFilesSelect"
              @remove="onFileRemove"
            />
          </div>

          <!-- File Titles -->
          <div v-if="formDialog.selectedFiles.length > 0" class="file-titles">
            <h5>Titres des documents:</h5>
            <div 
              v-for="(file, index) in formDialog.selectedFiles" 
              :key="index"
              class="file-title-input"
            >
              <div class="file-name">{{ file.name }}</div>
              <InputText 
                v-model="formDialog.fileTitles[index]" 
                placeholder="Titre du document..."
                class="title-input"
              />
              <div class="file-options">
                <Checkbox 
                  v-model="formDialog.originalFlags[index]" 
                  :binary="true"
                  input-id="original_${index}"
                />
                <label :for="`original_${index}`">Document original</label>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="closeFormDialog"
          class="p-button-outlined"
        />
        <Button 
          label="Sauvegarder" 
          icon="pi pi-check" 
          @click="submitForm"
          :loading="formDialog.submitting"
          :disabled="!isFormValid"
        />
      </template>
    </Dialog>

    <!-- File Upload Dialog -->
    <Dialog 
      v-model:visible="showUploadDialog" 
      modal 
      header="Télécharger un document"
      :style="{ width: '500px' }"
    >
      <div class="upload-dialog-content">
        <FileUpload
          ref="standaloneUploadRef"
          mode="advanced"
          multiple
          accept="*"
          :max-file-size="10000000"
          :auto="false"
          choose-label="Choisir des fichiers"
          @select="onStandaloneFilesSelect"
        />

        <div v-if="standaloneFiles.length > 0" class="file-titles">
          <h5>Titres des documents:</h5>
          <div 
            v-for="(file, index) in standaloneFiles" 
            :key="index"
            class="file-title-input"
          >
            <div class="file-name">{{ file.name }}</div>
            <InputText 
              v-model="standaloneFileTitles[index]" 
              placeholder="Titre du document..."
              class="title-input"
            />
            <div class="file-options">
              <Checkbox 
                v-model="standaloneOriginalFlags[index]" 
                :binary="true"
                :input-id="`standalone_original_${index}`"
              />
              <label :for="`standalone_original_${index}`">Document original</label>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="closeUploadDialog"
          class="p-button-outlined"
        />
        <Button 
          label="Télécharger" 
          icon="pi pi-upload" 
          @click="uploadStandaloneFiles"
          :loading="uploadingStandalone"
          :disabled="standaloneFiles.length === 0"
        />
      </template>
    </Dialog>

    <!-- Confirmation Dialogs -->
    <Dialog 
      v-model:visible="deleteConfirmDialog.visible" 
      modal 
      header="Confirmer la suppression"
      :style="{ width: '450px' }"
    >
      <div class="confirmation-content">
        <i class="pi pi-exclamation-triangle warning-icon"></i>
        <div>
          <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
          <p class="warning-text">Cette action est irréversible.</p>
        </div>
      </div>
      <template #footer>
        <Button 
          label="Annuler" 
          @click="deleteConfirmDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Supprimer" 
          @click="deleteDossier"
          class="p-button-danger"
          :loading="deleteConfirmDialog.loading"
        />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import UserInfoHeader from '@/components/UserInfoHeader.vue';
import DynamicForm from '@/components/agent_antenne/dossier_details/DynamicForm.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import FileUpload from 'primevue/fileupload';
import InputText from 'primevue/inputtext';
import Checkbox from 'primevue/checkbox';
import ProgressSpinner from 'primevue/progressspinner';
import Toast from 'primevue/toast';

const router = useRouter();
const route = useRoute();
const toast = useToast();

// Props
const dossierId = computed(() => route.params.dossierId);

// State
const loading = ref(true);
const error = ref(null);
const dossierDetail = ref(null);

// Form Dialog
const formDialog = ref({
  visible: false,
  form: null,
  formData: {},
  selectedFiles: [],
  fileTitles: [],
  originalFlags: [],
  submitting: false
});

// Standalone file upload
const showUploadDialog = ref(false);
const standaloneFiles = ref([]);
const standaloneFileTitles = ref([]);
const standaloneOriginalFlags = ref([]);
const uploadingStandalone = ref(false);

// Confirmation dialogs
const deleteConfirmDialog = ref({
  visible: false,
  loading: false
});

// Refs
const dynamicFormRef = ref(null);
const fileUploadRef = ref(null);
const standaloneUploadRef = ref(null);

// Computed
const isFormValid = computed(() => {
  return formDialog.value.formData && Object.keys(formDialog.value.formData).length > 0;
});

// Methods
onMounted(() => {
  loadDossierDetail();
});

watch(() => route.params.dossierId, () => {
  if (route.params.dossierId) {
    loadDossierDetail();
  }
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get(`/agent_antenne/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
  } catch (err) {
    console.error('Error loading dossier detail:', err);
    error.value = err.message || 'Impossible de charger les détails du dossier';
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/agent_antenne/dossiers');
}

function openFormDialog(form) {
  formDialog.value = {
    visible: true,
    form: form,
    formData: form.formData || {},
    selectedFiles: [],
    fileTitles: [],
    originalFlags: [],
    submitting: false
  };
}

function closeFormDialog() {
  formDialog.value.visible = false;
  formDialog.value.form = null;
  formDialog.value.formData = {};
  formDialog.value.selectedFiles = [];
  formDialog.value.fileTitles = [];
  formDialog.value.originalFlags = [];
}

function onFormDataChange(data) {
  formDialog.value.formData = data;
}

function onFilesSelect(event) {
  formDialog.value.selectedFiles = event.files;
  // Initialize arrays for titles and flags
  formDialog.value.fileTitles = event.files.map(file => file.name);
  formDialog.value.originalFlags = event.files.map(() => false);
}

function onFileRemove(event) {
  // Remove corresponding title and flag
  const index = formDialog.value.selectedFiles.indexOf(event.file);
  if (index > -1) {
    formDialog.value.fileTitles.splice(index, 1);
    formDialog.value.originalFlags.splice(index, 1);
  }
}

async function submitForm() {
  try {
    formDialog.value.submitting = true;
    
    const formData = new FormData();
    formData.append('formId', formDialog.value.form.formId);
    formData.append('formDataJson', JSON.stringify(formDialog.value.formData));
    
    // Add files
    formDialog.value.selectedFiles.forEach((file, index) => {
      formData.append('files', file);
    });
    
    // Add file titles and flags
    formDialog.value.fileTitles.forEach((title, index) => {
      formData.append('fileTitles', title);
    });
    
    formDialog.value.originalFlags.forEach((flag, index) => {
      formData.append('originalFlags', flag);
    });

    await ApiService.post(`/agent_antenne/dossiers/${dossierId.value}/forms`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Formulaire soumis avec succès',
      life: 3000
    });
    
    closeFormDialog();
    loadDossierDetail();
    
  } catch (err) {
    console.error('Error submitting form:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de soumettre le formulaire',
      life: 3000
    });
  } finally {
    formDialog.value.submitting = false;
  }
}

function onStandaloneFilesSelect(event) {
  standaloneFiles.value = event.files;
  standaloneFileTitles.value = event.files.map(file => file.name);
  standaloneOriginalFlags.value = event.files.map(() => false);
}

function closeUploadDialog() {
  showUploadDialog.value = false;
  standaloneFiles.value = [];
  standaloneFileTitles.value = [];
  standaloneOriginalFlags.value = [];
}

async function uploadStandaloneFiles() {
  try {
    uploadingStandalone.value = true;
    
    const formData = new FormData();
    formData.append('formId', 'standalone_upload');
    
    standaloneFiles.value.forEach(file => {
      formData.append('files', file);
    });
    
    standaloneFileTitles.value.forEach(title => {
      formData.append('fileTitles', title);
    });
    
    standaloneOriginalFlags.value.forEach(flag => {
      formData.append('originalFlags', flag);
    });

    await ApiService.post(`/agent_antenne/dossiers/${dossierId.value}/forms`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Documents téléchargés avec succès',
      life: 3000
    });
    
    closeUploadDialog();
    loadDossierDetail();
    
  } catch (err) {
    console.error('Error uploading files:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger les documents',
      life: 3000
    });
  } finally {
    uploadingStandalone.value = false;
  }
}

function confirmSendToGUC() {
  // Implement send to GUC confirmation
  toast.add({
    severity: 'info',
    summary: 'Info',
    detail: 'Fonctionnalité à implémenter',
    life: 3000
  });
}

function confirmDelete() {
  deleteConfirmDialog.value.visible = true;
}

async function deleteDossier() {
  try {
    deleteConfirmDialog.value.loading = true;
    
    await ApiService.delete(`/agent_antenne/dossiers/${dossierId.value}`);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier supprimé avec succès',
      life: 3000
    });
    
    goBack();
    
  } catch (err) {
    console.error('Error deleting dossier:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de supprimer le dossier',
      life: 3000
    });
  } finally {
    deleteConfirmDialog.value.loading = false;
    deleteConfirmDialog.value.visible = false;
  }
}

function downloadFile(file) {
  // Implement file download
  toast.add({
    severity: 'info',
    summary: 'Info',
    detail: 'Téléchargement à implémenter',
    life: 3000
  });
}

function confirmDeleteFile(file) {
  // Implement file deletion confirmation
  toast.add({
    severity: 'info',
    summary: 'Info',
    detail: 'Suppression de fichier à implémenter',
    life: 3000
  });
}

function handleSearch(query) {
  console.log('Search:', query);
}

function getStatusSeverity(status) {
  const severityMap = {
    'Phase Antenne': 'info',
    'Phase GUC': 'warning',
    'Commission Technique': 'secondary',
    'Réalisation': 'success'
  };
  return severityMap[status] || 'info';
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.dossier-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
  color: #6b7280;
}

.error-container i {
  font-size: 3rem;
  color: #dc2626;
}

/* Header */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: var(--background-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #6b7280;
  font-size: 0.9rem;
}

.breadcrumb span:last-child {
  color: var(--primary-color);
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Dossier Summary */
.dossier-summary {
  margin-bottom: 2rem;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.summary-header h2 {
  color: var(--primary-color);
  margin: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.summary-section h4 {
  color: var(--primary-color);
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: 600;
}

.info-lines {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-lines > div {
  font-size: 0.9rem;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.time-critical {
  color: #dc2626;
  font-weight: 600;
}

.time-warning {
  color: #f59e0b;
  font-weight: 600;
}

.time-ok {
  color: #10b981;
  font-weight: 600;
}

/* Forms Section */
.forms-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1.5rem;
}

.section-header h2,
.section-header h3 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-header p {
  color: #6b7280;
  margin: 0;
}

.no-forms {
  text-align: center;
  padding: 2rem;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 8px;
}

.no-forms i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}

.forms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
}

.form-card {
  background: var(--background-color);
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s ease;
}

.form-card:hover {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.form-card.form-completed {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.02);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  gap: 1rem;
}

.form-title {
  flex: 1;
}

.form-title h3 {
  margin: 0 0 0.5rem 0;
  color: #374151;
  font-size: 1.1rem;
}

.form-description p {
  color: #6b7280;
  font-size: 0.9rem;
  margin: 0 0 1rem 0;
  line-height: 1.4;
}

.required-docs {
  margin-bottom: 1rem;
}

.required-docs h5 {
  color: #374151;
  font-size: 0.9rem;
  margin: 0 0 0.5rem 0;
}

.required-docs ul {
  margin: 0;
  padding-left: 1.5rem;
  color: #6b7280;
  font-size: 0.85rem;
}

.form-meta {
  color: #6b7280;
  font-size: 0.8rem;
}

/* Files Section */
.files-section {
  margin-bottom: 2rem;
}

.files-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.no-files {
  text-align: center;
  padding: 2rem;
  color: #6b7280;
}

.no-files i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.file-item:hover {
  border-color: var(--primary-color);
  background: rgba(1, 114, 62, 0.02);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.file-icon {
  color: #dc2626;
  font-size: 1.5rem;
}

.file-details {
  flex: 1;
}

.file-name {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.25rem;
}

.file-meta {
  font-size: 0.8rem;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.file-actions {
  display: flex;
  gap: 0.5rem;
}

/* Validation Errors */
.validation-errors {
  border-left: 4px solid #dc2626;
  background: rgba(220, 38, 38, 0.02);
}

.validation-errors h3 {
  color: #dc2626;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.error-list {
  margin: 0;
  padding-left: 1.5rem;
  color: #dc2626;
}

.error-list li {
  margin-bottom: 0.5rem;
}

/* Dialog Styles */
.form-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.file-upload-section h4 {
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.upload-area {
  margin-bottom: 1rem;
}

.file-titles h5 {
  color: #374151;
  margin-bottom: 1rem;
}

.file-title-input {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.file-name {
  font-weight: 500;
  color: #374151;
  min-width: 150px;
}

.title-input {
  flex: 1;
}

.file-options {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 150px;
}

.file-options label {
  font-size: 0.9rem;
  color: #6b7280;
}

.upload-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.confirmation-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.warning-icon {
  color: #f59e0b;
  font-size: 2rem;
}

.warning-text {
  color: #dc2626;
  font-weight: 500;
  margin: 0.5rem 0 0 0;
}

/* Dark Mode */
.dark-mode .detail-header,
.dark-mode .dossier-summary,
.dark-mode .form-card,
.dark-mode .file-item,
.dark-mode .files-section {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .summary-header {
  border-bottom-color: #374151;
}

.dark-mode .no-forms,
.dark-mode .no-files {
  background-color: #374151;
}

/* Responsive */
@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .header-nav {
    justify-content: space-between;
  }

  .header-actions {
    justify-content: center;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .forms-grid {
    grid-template-columns: 1fr;
  }

  .file-item {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .file-actions {
    justify-content: center;
  }

  .file-title-input {
    flex-direction: column;
    align-items: stretch;
    gap: 0.5rem;
  }
}
</style>