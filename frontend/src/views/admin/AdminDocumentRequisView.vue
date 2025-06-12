<!-- AdminDocumentRequisView.vue -->
<template>
  <div class="document-requis-admin compact-layout">
    <!-- Compact Header with Statistics -->
    <div class="component-card compact-header">
      <div class="header-content">
        <div class="header-info">
          <h2><i class="pi pi-file-edit"></i> Gestion des Documents Requis</h2>
          <p>Gérez les documents requis pour chaque sous-rubrique</p>
        </div>
        
        <!-- Inline Statistics -->
        <div class="inline-stats" v-if="statistics">
          <div class="stat-item">
            <div class="stat-icon"><i class="pi pi-file"></i></div>
            <div class="stat-content">
              <span class="stat-number">{{ statistics.totalDocuments }}</span>
              <span class="stat-label">Total</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon danger"><i class="pi pi-star-fill"></i></div>
            <div class="stat-content">
              <span class="stat-number">{{ statistics.totalObligatoires }}</span>
              <span class="stat-label">Obligatoires</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon info"><i class="pi pi-star"></i></div>
            <div class="stat-content">
              <span class="stat-number">{{ statistics.totalOptionnels }}</span>
              <span class="stat-label">Optionnels</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="content-area">
      <!-- Loading State -->
      <div v-if="loading" class="component-card loading-container">
        <ProgressBar mode="indeterminate" />
        <p>Chargement des données...</p>
      </div>      <!-- Rubriques List -->
      <div v-else class="rubriques-container">
        <div
          v-for="rubrique in rubriques" 
          :key="rubrique.id"
          class="component-card rubrique-card compact"
        >
          <div class="rubrique-header compact">
            <div class="category-header">
              <div class="category-icon">
                <i :class="getCategoryIcon(rubrique.designation)"></i>
              </div>
              <div class="category-info">
                <h3>{{ rubrique.designation }}</h3>
                <p v-if="rubrique.description">{{ rubrique.description }}</p>
              </div>
            </div>
          </div>

          <!-- Compact Sous-Rubriques Grid -->
          <div class="sous-rubriques compact-grid">
            <div 
              v-for="sousRubrique in rubrique.sousRubriques"
              :key="sousRubrique.id"
              class="sous-rubrique-card compact"
            >
              <div class="sous-rubrique-header compact">
                <div class="sous-rubrique-info">
                  <h4>{{ sousRubrique.designation }}</h4>
                  <div class="sous-rubrique-meta">
                    <span class="document-count">
                      <i class="pi pi-file"></i>
                      {{ sousRubrique.documentsRequis.length }} doc(s)
                    </span>
                    <Button 
                      icon="pi pi-plus" 
                      label="Ajouter"
                      @click="openCreateDialog(sousRubrique)"
                      class="p-button-sm btn-primary compact-btn"
                      size="small"
                    />
                  </div>
                </div>
              </div>

              <!-- Compact Documents List -->
              <div v-if="sousRubrique.documentsRequis.length > 0" class="documents-compact-list">
                <div 
                  v-for="document in sousRubrique.documentsRequis"
                  :key="document.id"
                  class="document-item-compact"
                >
                  <div class="document-info">
                    <div class="document-name">
                      <i class="pi pi-file"></i>
                      <strong>{{ document.nomDocument }}</strong>
                    </div>
                    <div class="document-meta">
                      <Tag 
                        :value="document.obligatoire ? 'Obligatoire' : 'Optionnel'"
                        :severity="document.obligatoire ? 'danger' : 'info'"
                        class="document-tag"
                      />
                      <div v-if="document.locationFormulaire" class="json-indicator">
                        <i class="pi pi-file-edit" v-tooltip="'Fichier JSON disponible'"></i>
                      </div>
                    </div>
                  </div>
                  <div class="document-actions">
                    <Button 
                      icon="pi pi-pencil" 
                      @click="openEditDialog(document)"
                      class="p-button-rounded p-button-text p-button-info action-btn-compact"
                      v-tooltip.top="'Modifier'"
                      size="small"
                    />
                    <Button 
                      icon="pi pi-trash" 
                      @click="confirmDelete(document)"
                      class="p-button-rounded p-button-text p-button-danger action-btn-compact"
                      v-tooltip.top="'Supprimer'"
                      size="small"
                    />
                    <Button 
                      v-if="document.locationFormulaire"
                      icon="pi pi-download" 
                      @click="downloadJsonFile(document)"
                      class="p-button-rounded p-button-text action-btn-compact"
                      v-tooltip.top="'Télécharger JSON'"
                      size="small"
                    />
                  </div>
                </div>
              </div>

              <!-- Compact Empty State -->
              <div v-else class="empty-documents compact">
                <div class="empty-content">
                  <i class="pi pi-inbox empty-icon"></i>
                  <span>Aucun document</span>
                  <Button 
                    label="Ajouter" 
                    icon="pi pi-plus"
                    @click="openCreateDialog(sousRubrique)"
                    class="p-button-outlined p-button-sm"
                    size="small"
                  />
                </div>
              </div>            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Dialog - Compact -->
    <Dialog 
      v-model:visible="showDialog" 
      :header="dialogMode === 'create' ? 'Ajouter un Document Requis' : 'Modifier le Document Requis'"
      modal 
      :style="{ width: '650px' }"
      :closable="true"
      class="compact-dialog"
    >
      <form @submit.prevent="submitForm" class="document-form compact">
        <div class="compact-form-layout">
          <!-- Row 1: Nom & Type -->
          <div class="form-row">
            <div class="form-group">
              <label for="nomDocument" class="required">Nom du Document</label>
              <InputText 
                id="nomDocument"
                v-model="documentForm.nomDocument"
                placeholder="Ex: Carte d'identité nationale"
                :class="{ 'p-invalid': formErrors.nomDocument }"
                required
              />
              <small class="p-error">{{ formErrors.nomDocument }}</small>
            </div>

            <div class="form-group">
              <label for="obligatoire">Type de Document</label>
              <div class="form-check-group horizontal">
                <div class="form-check">
                  <RadioButton 
                    id="obligatoire-oui"
                    v-model="documentForm.obligatoire"
                    :value="true"
                  />
                  <label for="obligatoire-oui">Obligatoire</label>
                </div>
                <div class="form-check">
                  <RadioButton 
                    id="obligatoire-non"
                    v-model="documentForm.obligatoire"
                    :value="false"
                  />
                  <label for="obligatoire-non">Optionnel</label>
                </div>
              </div>
            </div>
          </div>

          <!-- Row 2: Description -->
          <div class="form-row">
            <div class="form-group full-width">
              <label for="description">Description</label>
              <Textarea
                id="description"
                v-model="documentForm.description"
                placeholder="Description détaillée du document"
                :rows="2"
                :class="{ 'p-invalid': formErrors.description }"
              />
              <small class="p-error">{{ formErrors.description }}</small>
            </div>
          </div>

          <!-- Row 3: File Upload -->
          <div class="form-row">
            <div class="form-group">
              <label for="jsonFile">Fichier JSON</label>
              <FileUpload
                id="jsonFile"
                ref="fileUploadRef"
                mode="basic"
                accept=".json"
                :maxFileSize="1000000"
                :fileLimit="1"
                @select="onFileSelect"
                @remove="onFileRemove"
                :auto="false"
                chooseLabel="Choisir JSON"
                class="custom-file-upload compact"
              />
              <small class="form-help compact">
                <i class="pi pi-info-circle"></i>
                Fichier JSON pour la configuration (max 1MB)
              </small>
              <small class="p-error">{{ formErrors.jsonFile }}</small>
              
              <!-- Current file info -->
              <div v-if="dialogMode === 'edit' && currentJsonFileName" class="current-file-info compact">
                <div class="file-info-card">
                  <i class="pi pi-file"></i>
                  <span>{{ currentJsonFileName }}</span>
                  <Button 
                    icon="pi pi-times" 
                    @click="clearCurrentFile"
                    class="p-button-rounded p-button-text p-button-sm p-button-danger"
                    v-tooltip="'Supprimer le fichier actuel'"
                  />
                </div>
              </div>
            </div>

            <!-- Row 4: JSON Properties -->
            <div class="form-group">
              <label for="proprietes">Propriétés JSON</label>
              <Textarea
                id="proprietes"
                v-model="jsonProprietesText"
                placeholder='{"validation": {"maxSize": "10MB"}}'
                :rows="3"
                :class="{ 'p-invalid': formErrors.proprietes }"
                @blur="validateJson"
              />
              <small class="form-help compact">
                <i class="pi pi-info-circle"></i>
                Configuration JSON pour propriétés avancées
              </small>
              <small class="p-error">{{ formErrors.proprietes }}</small>
            </div>
          </div>
        </div>
      </form>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="closeDialog"
          class="p-button-outlined" 
        />
        <Button 
          :label="dialogMode === 'create' ? 'Créer' : 'Modifier'"
          :icon="dialogMode === 'create' ? 'pi pi-plus' : 'pi pi-check'"
          @click="submitForm"
          :loading="submitting"
          :disabled="!isFormValid"
          class="btn-primary"
        />
      </template>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog />

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import axios from 'axios';

// PrimeVue Components
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import RadioButton from 'primevue/radiobutton';
import Tag from 'primevue/tag';
import ProgressBar from 'primevue/progressbar';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';
import FileUpload from 'primevue/fileupload';

const toast = useToast();
const confirm = useConfirm();

// Reactive Data
const loading = ref(true);
const submitting = ref(false);
const rubriques = ref([]);
const statistics = ref(null);

// Dialog Management
const showDialog = ref(false);
const dialogMode = ref('create'); // 'create' or 'edit'
const selectedSousRubrique = ref(null);

// Form Data
const documentForm = ref({
  id: null,
  nomDocument: '',
  description: '',
  obligatoire: true,
  locationFormulaire: '',
  sousRubriqueId: null
});

const jsonProprietesText = ref('');
const selectedJsonFile = ref(null);
const currentJsonFileName = ref('');
const fileUploadRef = ref(null);
const formErrors = ref({});

// API Base URL  
const API_BASE = '/admin/documents-requis';

// Get auth token for requests
const getAuthHeaders = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// Computed Properties
const isFormValid = computed(() => {
  return documentForm.value.nomDocument.trim() !== '' && 
         !formErrors.value.nomDocument &&
         !formErrors.value.proprietes;
});

// Get category icon
function getCategoryIcon(categoryName) {
  const iconMap = {
    'FILIERES VEGETALES': 'pi pi-leaf',
    'FILIERES ANIMALES': 'pi pi-heart',
    'AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE': 'pi pi-wrench'
  };
  
  // Try exact match first
  if (iconMap[categoryName]) {
    return iconMap[categoryName];
  }
  
  // Fallback based on keywords
  const lowerName = categoryName.toLowerCase();
  if (lowerName.includes('vegetal') || lowerName.includes('plant')) {
    return 'pi pi-leaf';
  } else if (lowerName.includes('animal') || lowerName.includes('elevage')) {
    return 'pi pi-heart';
  } else if (lowerName.includes('amenagement') || lowerName.includes('hydro') || lowerName.includes('infrastructure')) {
    return 'pi pi-wrench';
  }
  
  // Default icon
  return 'pi pi-folder';
}

// API Methods
async function loadRubriquesWithDocuments() {
  try {
    loading.value = true;
    const response = await axios.get(API_BASE, {
      headers: getAuthHeaders()
    });
    
    rubriques.value = response.data.rubriques || [];
    statistics.value = {
      totalDocuments: response.data.totalDocuments || 0,
      totalObligatoires: response.data.totalObligatoires || 0,
      totalOptionnels: response.data.totalOptionnels || 0
    };
    
  } catch (error) {
    console.error('Erreur lors du chargement des rubriques:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les données',
      life: 5000
    });
  } finally {
    loading.value = false;
  }
}

async function createDocument() {
  try {
    submitting.value = true;
    
    // Create FormData for file upload
    const formData = new FormData();
    formData.append('nomDocument', documentForm.value.nomDocument.trim());
    formData.append('description', documentForm.value.description?.trim() || '');
    formData.append('obligatoire', documentForm.value.obligatoire);
    formData.append('sousRubriqueId', documentForm.value.sousRubriqueId);
    
    if (jsonProprietesText.value.trim()) {
      formData.append('proprietes', jsonProprietesText.value.trim());
    }
    
    if (selectedJsonFile.value) {
      formData.append('jsonFile', selectedJsonFile.value);
    }

    await axios.post(API_BASE, formData, {
      headers: {
        ...getAuthHeaders(),
        'Content-Type': 'multipart/form-data'
      }
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Document requis créé avec succès',
      life: 3000
    });
    
    closeDialog();
    await loadRubriquesWithDocuments();
    
  } catch (error) {
    console.error('Erreur lors de la création:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || 'Erreur lors de la création',
      life: 5000
    });
  } finally {
    submitting.value = false;
  }
}

async function updateDocument() {
  try {
    submitting.value = true;
    
    // Create FormData for file upload
    const formData = new FormData();
    formData.append('nomDocument', documentForm.value.nomDocument.trim());
    formData.append('description', documentForm.value.description?.trim() || '');
    formData.append('obligatoire', documentForm.value.obligatoire);
    
    if (jsonProprietesText.value.trim()) {
      formData.append('proprietes', jsonProprietesText.value.trim());
    }
    
    if (selectedJsonFile.value) {
      formData.append('jsonFile', selectedJsonFile.value);
    }

    await axios.put(`${API_BASE}/${documentForm.value.id}`, formData, {
      headers: {
        ...getAuthHeaders(),
        'Content-Type': 'multipart/form-data'
      }
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Document requis modifié avec succès',
      life: 3000
    });
    
    closeDialog();
    await loadRubriquesWithDocuments();
    
  } catch (error) {
    console.error('Erreur lors de la modification:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || 'Erreur lors de la modification',
      life: 5000
    });
  } finally {
    submitting.value = false;
  }
}

async function deleteDocument(documentId) {
  try {
    await axios.delete(`${API_BASE}/${documentId}`, {
      headers: getAuthHeaders()
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Document requis supprimé avec succès',
      life: 3000
    });
    
    await loadRubriquesWithDocuments();
    
  } catch (error) {
    console.error('Erreur lors de la suppression:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || 'Erreur lors de la suppression',
      life: 5000
    });
  }
}

// Dialog Methods
function openCreateDialog(sousRubrique) {
  dialogMode.value = 'create';
  selectedSousRubrique.value = sousRubrique;
  resetForm();
  documentForm.value.sousRubriqueId = sousRubrique.id;
  showDialog.value = true;
}

function openEditDialog(document) {
  dialogMode.value = 'edit';
  documentForm.value = {
    id: document.id,
    nomDocument: document.nomDocument,
    description: document.description || '',
    obligatoire: document.obligatoire,
    locationFormulaire: document.locationFormulaire || '',
    sousRubriqueId: document.sousRubriqueId
  };
  
  // Handle JSON properties
  if (document.proprietes) {
    jsonProprietesText.value = JSON.stringify(document.proprietes, null, 2);
  } else {
    jsonProprietesText.value = '';
  }
  
  // Show current file info if exists
  if (document.locationFormulaire) {
    currentJsonFileName.value = extractFileName(document.locationFormulaire);
  } else {
    currentJsonFileName.value = '';
  }
  
  // Reset file selection
  selectedJsonFile.value = null;
  if (fileUploadRef.value) {
    fileUploadRef.value.clear();
  }
  
  showDialog.value = true;
}

function closeDialog() {
  showDialog.value = false;
  resetForm();
  formErrors.value = {};
}

function resetForm() {
  documentForm.value = {
    id: null,
    nomDocument: '',
    description: '',
    obligatoire: true,
    locationFormulaire: '',
    sousRubriqueId: null
  };
  jsonProprietesText.value = '';
  selectedJsonFile.value = null;
  currentJsonFileName.value = '';
  
  // Clear file upload component
  if (fileUploadRef.value) {
    fileUploadRef.value.clear();
  }
}

// File handling methods
function onFileSelect(event) {
  const file = event.files[0];
  if (file) {
    // Validate file type
    if (!file.name.toLowerCase().endsWith('.json')) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Seuls les fichiers JSON sont acceptés',
        life: 3000
      });
      fileUploadRef.value.clear();
      return;
    }
    
    // Validate file size (1MB max)
    if (file.size > 1000000) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Le fichier est trop volumineux (max 1MB)',
        life: 3000
      });
      fileUploadRef.value.clear();
      return;
    }
    
    selectedJsonFile.value = file;
    delete formErrors.value.jsonFile;
    
    // Optionally validate JSON content
    validateJsonFile(file);
  }
}

function onFileRemove() {
  selectedJsonFile.value = null;
  delete formErrors.value.jsonFile;
}

function clearCurrentFile() {
  currentJsonFileName.value = '';
  documentForm.value.locationFormulaire = '';
}

function validateJsonFile(file) {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      JSON.parse(e.target.result);
      delete formErrors.value.jsonFile;
    } catch (error) {
      formErrors.value.jsonFile = 'Le fichier JSON n\'est pas valide';
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Le contenu du fichier JSON n\'est pas valide',
        life: 3000
      });
    }
  };
  reader.readAsText(file);
}

function extractFileName(filePath) {
  if (!filePath) return '';
  const parts = filePath.split('/');
  return parts[parts.length - 1];
}

// Download JSON file
async function downloadJsonFile(document) {
  try {
    const response = await axios.get(`${API_BASE}/${document.id}/download-json`, {
      headers: getAuthHeaders(),
      responseType: 'blob'
    });
    
    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${document.nomDocument}_form.json`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
  } catch (error) {
    console.error('Erreur lors du téléchargement:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger le fichier JSON',
      life: 3000
    });
  }
}

function submitForm() {
  if (!validateForm()) {
    return;
  }
  
  if (dialogMode.value === 'create') {
    createDocument();
  } else {
    updateDocument();
  }
}

function confirmDelete(document) {
  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer le document "${document.nomDocument}" ?`,
    header: 'Confirmer la suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    accept: () => {
      deleteDocument(document.id);
    }
  });
}

// Validation Methods
function validateForm() {
  formErrors.value = {};
  
  if (!documentForm.value.nomDocument.trim()) {
    formErrors.value.nomDocument = 'Le nom du document est requis';
  }
  
  // Validate JSON file if provided
  if (selectedJsonFile.value && formErrors.value.jsonFile) {
    // File validation error already set
  }
  
  validateJson();
  
  return Object.keys(formErrors.value).length === 0;
}

function validateJson() {
  if (!jsonProprietesText.value.trim()) {
    return; // Empty JSON is valid
  }
  
  try {
    JSON.parse(jsonProprietesText.value);
    delete formErrors.value.proprietes;
  } catch (e) {
    formErrors.value.proprietes = 'Format JSON invalide';
  }
}

function parseJsonProprietes() {
  if (!jsonProprietesText.value.trim()) {
    return null;
  }
  
  try {
    return JSON.parse(jsonProprietesText.value);
  } catch (e) {
    return null;
  }
}

// Lifecycle
onMounted(() => {
  loadRubriquesWithDocuments();
});

// Watchers
watch(jsonProprietesText, () => {
  validateJson();
});
</script>

<style scoped>
/* Base Layout */
.document-requis-admin {
  padding: 1.5rem;
  background: #f8f9fa;
  min-height: 100vh;
}

.document-requis-admin.compact-layout {
  padding: 1rem;
}

/* Component Card Base */
.component-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
}

/* Compact Header */
.compact-header {
  padding: 1.25rem !important;
  margin-bottom: 1rem !important;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-info h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 0.25rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-info p {
  color: #6b7280;
  margin: 0;
  font-size: 0.9rem;
}

/* Inline Statistics */
.inline-stats {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3b82f6;
  color: white;
}

.stat-icon.danger {
  background: #ef4444;
}

.stat-icon.info {
  background: #06b6d4;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 0.8rem;
  color: #6b7280;
  font-weight: 500;
}

/* Compact Rubrique Cards */
.rubrique-card.compact {
  padding: 1.25rem !important;
  margin-bottom: 1.25rem !important;
}

.rubrique-header.compact {
  margin-bottom: 1rem;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.category-icon {
  width: 40px;
  height: 40px;
  background: #3b82f6;
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-info h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 0.25rem 0;
}

.category-info p {
  color: #6b7280;
  margin: 0;
  font-size: 0.9rem;
}

/* Compact Sous-Rubriques Grid */
.sous-rubriques.compact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1rem;
}

.sous-rubrique-card.compact {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
}

.sous-rubrique-header.compact {
  margin-bottom: 0.75rem;
}

.sous-rubrique-info h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 0.5rem 0;
}

.sous-rubrique-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.document-count {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: #6b7280;
}

.compact-btn {
  padding: 0.5rem 0.75rem !important;
  font-size: 0.8rem !important;
}

/* Compact Documents List */
.documents-compact-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.document-item-compact {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.document-info {
  flex: 1;
  min-width: 0;
}

.document-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.document-name strong {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.9rem;
}

.document-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.document-tag {
  font-size: 0.75rem !important;
  padding: 0.25rem 0.5rem !important;
}

.json-indicator {
  display: flex;
  align-items: center;
  color: #3b82f6;
}

.document-actions {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.action-btn-compact {
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
}

/* Compact Empty State */
.empty-documents.compact {
  padding: 1rem;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.empty-icon {
  font-size: 1.5rem;
  color: #9ca3af;
}

.empty-content span {
  color: #6b7280;
  font-size: 0.9rem;
}

/* Compact Dialog */
.compact-dialog :deep(.p-dialog-content) {
  padding: 1.25rem !important;
}

.document-form.compact {
  margin: 0;
}

.compact-form-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.form-group label.required::after {
  content: ' *';
  color: #ef4444;
}

.form-check-group.horizontal {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 0.25rem;
}

.form-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-check label {
  margin: 0 !important;
  font-size: 0.9rem;
  cursor: pointer;
}

.form-help.compact {
  font-size: 0.8rem;
}

.current-file-info.compact {
  margin-top: 0.5rem;
}

.file-info-card {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: #f0f9ff;
  border: 1px solid #0ea5e9;
  border-radius: 4px;
  font-size: 0.85rem;
}

.custom-file-upload.compact :deep(.p-fileupload-choose) {
  padding: 0.5rem 0.75rem !important;
  font-size: 0.9rem !important;
}

/* Loading State */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: #6b7280;
}

/* Button Styles */
.btn-primary {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
  border-color: #2563eb;
}

/* Input Styles */
:deep(.p-inputtext),
:deep(.p-dropdown),
:deep(.p-calendar input) {
  border-radius: 6px;
  border: 1px solid #d1d5db;
  padding: 0.75rem;
  transition: border-color 0.2s ease;
}

:deep(.p-inputtext:focus),
:deep(.p-dropdown:focus),
:deep(.p-calendar input:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* Error States */
:deep(.p-inputtext.p-invalid),
:deep(.p-dropdown.p-invalid) {
  border-color: #dc2626;
}

.p-error {
  color: #dc2626;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.form-help {
  color: #6b7280;
  font-size: 0.875rem;
  margin-top: 0.25rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .document-requis-admin.compact-layout {
    padding: 0.75rem;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .inline-stats {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
    width: 100%;
  }
  
  .stat-item {
    width: 100%;
    justify-content: flex-start;
  }
  
  .sous-rubriques.compact-grid {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .sous-rubrique-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .document-item-compact {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .document-actions {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .compact-header {
    padding: 1rem !important;
  }
  
  .rubrique-card.compact {
    padding: 1rem !important;
  }
  
  .sous-rubrique-card.compact {
    padding: 0.75rem;
  }
  
  .inline-stats {
    gap: 0.5rem;
  }
  
  .stat-item {
    padding: 0.5rem 0.75rem;
  }
  
  .form-check-group.horizontal {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style>

