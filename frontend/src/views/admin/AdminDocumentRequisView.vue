<!-- AdminDocumentRequisView.vue -->
<template>
  <div class="document-requis-admin">
    <!-- Header -->
    <div class="component-card page-header">
      <div class="header-info">
        <h2><i class="pi pi-file-edit"></i> Gestion des Documents Requis</h2>
        <p>Gérez les documents requis pour chaque sous-rubrique</p>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid" v-if="statistics">
      <div class="component-card stat-card">
        <div class="stat-icon">
          <i class="pi pi-file"></i>
        </div>
        <div class="stat-content">
          <h3>{{ statistics.totalDocuments }}</h3>
          <p>Total Documents</p>
        </div>
      </div>
      
      <div class="component-card stat-card">
        <div class="stat-icon">
          <i class="pi pi-star-fill"></i>
        </div>
        <div class="stat-content">
          <h3>{{ statistics.totalObligatoires }}</h3>
          <p>Obligatoires</p>
        </div>
      </div>
      
      <div class="component-card stat-card">
        <div class="stat-icon">
          <i class="pi pi-star"></i>
        </div>
        <div class="stat-content">
          <h3>{{ statistics.totalOptionnels }}</h3>
          <p>Optionnels</p>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="content-area">
      <!-- Loading State -->
      <div v-if="loading" class="component-card loading-container">
        <ProgressBar mode="indeterminate" />
        <p>Chargement des données...</p>
      </div>

      <!-- Rubriques List -->
      <div v-else class="rubriques-container">
        <div 
          v-for="rubrique in rubriques" 
          :key="rubrique.id"
          class="component-card rubrique-card"
        >
          <div class="rubrique-header">
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

          <!-- Sous-Rubriques -->
          <div class="sous-rubriques">
            <div 
              v-for="sousRubrique in rubrique.sousRubriques"
              :key="sousRubrique.id"
              class="sous-rubrique-card"
            >
              <div class="sous-rubrique-header">
                <div class="sous-rubrique-info">
                  <h4>{{ sousRubrique.designation }}</h4>
                  <p v-if="sousRubrique.description">{{ sousRubrique.description }}</p>
                  <span class="document-count">
                    <i class="pi pi-file"></i>
                    {{ sousRubrique.documentsRequis.length }} document(s)
                  </span>
                </div>
                <Button 
                  icon="pi pi-plus" 
                  label="Ajouter Document"
                  @click="openCreateDialog(sousRubrique)"
                  class="p-button-sm btn-primary"
                />
              </div>

              <!-- Documents Table -->
              <div v-if="sousRubrique.documentsRequis.length > 0" class="documents-table">
                <DataTable 
                  :value="sousRubrique.documentsRequis" 
                  :paginator="false"
                  :responsive-layout="scroll"
                  class="p-datatable-sm"
                >
                  <Column field="nomDocument" header="Nom du Document">
                    <template #body="slotProps">
                      <div class="document-name">
                        <i class="pi pi-file"></i>
                        <strong>{{ slotProps.data.nomDocument }}</strong>
                      </div>
                    </template>
                  </Column>
                  
                  <Column field="description" header="Description">
                    <template #body="slotProps">
                      <span class="description-text">
                        {{ slotProps.data.description || 'Aucune description' }}
                      </span>
                    </template>
                  </Column>
                  
                  <Column field="obligatoire" header="Type">
                    <template #body="slotProps">
                      <Tag 
                        :value="slotProps.data.obligatoire ? 'Obligatoire' : 'Optionnel'"
                        :severity="slotProps.data.obligatoire ? 'danger' : 'info'"
                      />
                    </template>
                  </Column>
                  
                  <Column field="locationFormulaire" header="Fichier JSON">
                    <template #body="slotProps">
                      <div v-if="slotProps.data.locationFormulaire" class="json-file-info">
                        <i class="pi pi-file"></i>
                        <span class="file-name">{{ extractFileName(slotProps.data.locationFormulaire) }}</span>
                        <Button 
                          icon="pi pi-download" 
                          @click="downloadJsonFile(slotProps.data)"
                          class="p-button-rounded p-button-text p-button-sm"
                          v-tooltip="'Télécharger le fichier JSON'"
                        />
                      </div>
                      <span v-else class="no-file-text">
                        <i class="pi pi-minus"></i>
                        Aucun fichier
                      </span>
                    </template>
                  </Column>
                  
                  <Column header="Actions" :exportable="false">
                    <template #body="slotProps">
                      <div class="action-buttons">
                        <Button 
                          icon="pi pi-pencil" 
                          @click="openEditDialog(slotProps.data)"
                          class="p-button-rounded p-button-text p-button-info action-btn"
                          v-tooltip.top="'Modifier'"
                        />
                        <Button 
                          icon="pi pi-trash" 
                          @click="confirmDelete(slotProps.data)"
                          class="p-button-rounded p-button-text p-button-danger action-btn"
                          v-tooltip.top="'Supprimer'"
                        />
                      </div>
                    </template>
                  </Column>
                </DataTable>
              </div>

              <!-- Empty State -->
              <div v-else class="empty-documents">
                <div class="empty-content">
                  <i class="pi pi-inbox empty-icon"></i>
                  <h5>Aucun document requis</h5>
                  <p>Cette sous-rubrique n'a pas encore de documents requis</p>
                  <Button 
                    label="Ajouter le premier document" 
                    icon="pi pi-plus"
                    @click="openCreateDialog(sousRubrique)"
                    class="p-button-outlined"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <Dialog 
      v-model:visible="showDialog" 
      :header="dialogMode === 'create' ? 'Ajouter un Document Requis' : 'Modifier le Document Requis'"
      modal 
      :style="{ width: '600px' }"
      :closable="true"
    >
      <form @submit.prevent="submitForm" class="document-form">
        <div class="form-grid">
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
            <label for="description">Description</label>
            <Textarea
              id="description"
              v-model="documentForm.description"
              placeholder="Description détaillée du document"
              :rows="3"
              :class="{ 'p-invalid': formErrors.description }"
            />
            <small class="p-error">{{ formErrors.description }}</small>
          </div>

          <div class="form-group">
            <label for="obligatoire">Type de Document</label>
            <div class="form-check-group">
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

          <div class="form-group">
            <label for="jsonFile">Fichier de Configuration JSON</label>
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
              chooseLabel="Choisir un fichier JSON"
              class="custom-file-upload"
            />
            <small class="form-help">
              <i class="pi pi-info-circle"></i>
              Sélectionnez un fichier JSON contenant la configuration du formulaire (max 1MB)
            </small>
            <small class="p-error">{{ formErrors.jsonFile }}</small>
            
            <!-- Show current file info if editing -->
            <div v-if="dialogMode === 'edit' && currentJsonFileName" class="current-file-info">
              <div class="file-info-card">
                <i class="pi pi-file"></i>
                <span>Fichier actuel: {{ currentJsonFileName }}</span>
                <Button 
                  icon="pi pi-times" 
                  @click="clearCurrentFile"
                  class="p-button-rounded p-button-text p-button-sm p-button-danger"
                  v-tooltip="'Supprimer le fichier actuel'"
                />
              </div>
            </div>
          </div>

          <div class="form-group full-width">
            <label for="proprietes">Propriétés JSON (Optionnel)</label>
            <Textarea
              id="proprietes"
              v-model="jsonProprietesText"
              placeholder='{"validation": {"maxSize": "10MB", "formats": ["pdf", "jpg"]}, "required": true}'
              :rows="4"
              :class="{ 'p-invalid': formErrors.proprietes }"
              @blur="validateJson"
            />
            <small class="form-help">
              <i class="pi pi-info-circle"></i>
              Configuration JSON pour les propriétés avancées du document
            </small>
            <small class="p-error">{{ formErrors.proprietes }}</small>
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
.document-requis-admin {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  background: var(--background-color);
  min-height: 100vh;
}

/* Primary Button Styles */
:deep(.btn-primary) {
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  color: var(--text-on-primary) !important;
}

:deep(.btn-primary:hover) {
  background-color: var(--accent-color) !important;
  border-color: var(--accent-color) !important;
}

/* Header */
.page-header {
  margin-bottom: var(--component-spacing);
}

.header-info {
  text-align: center;
}

.header-info h2 {
  color: var(--primary-color);
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  font-family: 'Poppins', sans-serif;
}

.header-info p {
  color: var(--text-secondary);
  font-size: 1rem;
  margin: 0;
}

/* Statistics Cards */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--component-spacing);
  margin-bottom: var(--component-spacing);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: var(--component-spacing);
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: var(--border-radius-md);
  background: var(--clr-surface-tonal-a0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: var(--primary-color);
}

.stat-content h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-color);
  margin: 0;
  font-family: 'Poppins', sans-serif;
}

.stat-content p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

/* Loading */
.loading-container {
  text-align: center;
  padding: 3rem;
}

.loading-container p {
  margin-top: 1rem;
  color: var(--text-secondary);
}

/* Rubriques */
.rubriques-container {
  display: flex;
  flex-direction: column;
  gap: var(--component-spacing);
}

.rubrique-card {
  overflow: hidden;
}

.rubrique-header {
  background: var(--section-background);
  padding: var(--component-spacing);
  border-bottom: 1px solid var(--border-color);
}

.category-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.category-icon {
  width: 50px;
  height: 50px;
  background: var(--primary-color);
  border-radius: var(--border-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-on-primary);
  font-size: 1.5rem;
}

.category-info h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  font-family: 'Poppins', sans-serif;
}

.category-info p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 0.9rem;
}

/* Sous-Rubriques */
.sous-rubriques {
  padding: var(--component-spacing);
  display: flex;
  flex-direction: column;
  gap: var(--component-spacing);
}

.sous-rubrique-card {
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  overflow: hidden;
  background: var(--card-background);
}

.sous-rubrique-header {
  background: var(--section-background);
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid var(--border-color);
}

.sous-rubrique-info h4 {
  color: var(--text-color);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.25rem 0;
  font-family: 'Poppins', sans-serif;
}

.sous-rubrique-info p {
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin: 0 0 0.5rem 0;
}

.document-count {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--text-color);
  padding: 0.25rem 0.5rem;
  border-radius: var(--border-radius-sm);
  font-size: 0.75rem;
  font-weight: 500;
}

/* Documents Table */
.documents-table {
  padding: 0;
}

.document-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.document-name i {
  color: var(--primary-color);
}

.description-text {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.json-file-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.json-file-info i {
  color: var(--primary-color);
}

.file-name {
  font-family: monospace;
  font-size: 0.8rem;
  color: var(--text-color);
  flex: 1;
}

.no-file-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  font-style: italic;
  font-size: 0.875rem;
}

.no-file-text i {
  opacity: 0.5;
}

.action-buttons {
  display: flex;
  gap: 0.375rem;
}

.action-btn {
  padding: 0.5rem !important;
  min-width: 2.5rem !important;
  height: 2.5rem !important;
}

/* Empty State */
.empty-documents {
  padding: 2rem;
  display: flex;
  justify-content: center;
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  font-size: 3rem;
  color: var(--text-muted);
  margin-bottom: 1rem;
}

.empty-content h5 {
  color: var(--text-color);
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  font-family: 'Poppins', sans-serif;
}

.empty-content p {
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin: 0 0 1.5rem 0;
}

/* Form Styles */
.document-form {
  padding: 1rem 0;
}

.form-grid {
  display: grid;
  gap: var(--component-spacing);
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.form-group label.required::after {
  content: ' *';
  color: var(--danger-color);
}

.form-help {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin-top: 0.25rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.form-help i {
  color: var(--primary-color);
}

.form-check-group {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
}

.form-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-check label {
  margin: 0;
  cursor: pointer;
  font-weight: 400;
}

/* File Upload Styles */
.custom-file-upload {
  width: 100%;
}

:deep(.p-fileupload-basic) {
  width: 100%;
}

:deep(.p-fileupload-basic .p-button) {
  width: 100%;
  justify-content: flex-start;
  background: var(--surface-background);
  color: var(--text-color);
  border: 2px dashed var(--border-color);
  padding: 1rem;
}

:deep(.p-fileupload-basic .p-button:hover) {
  background: var(--section-background);
  border-color: var(--primary-color);
}

.current-file-info {
  margin-top: 0.75rem;
}

.file-info-card {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--clr-surface-tonal-a0);
  border: 1px solid var(--clr-primary-a20);
  border-radius: var(--border-radius-sm);
  font-size: 0.875rem;
}

.file-info-card i {
  color: var(--primary-color);
}

.file-info-card span {
  flex: 1;
  color: var(--text-color);
}

/* Responsive */
@media (max-width: 768px) {
  .document-requis-admin {
    padding: 0.5rem;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .sous-rubrique-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }
  
  .action-buttons {
    justify-content: center;
  }
  
  .category-header {
    flex-direction: column;
    text-align: center;
    gap: 0.75rem;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    gap: 1rem;
  }
  
  .stat-card {
    padding: 1rem;
  }
  
  .header-info h2 {
    font-size: 1.5rem;
  }
}
</style>