<template>
  <div class="implementation-visit-detail-container">
    <!-- Visit Header -->
    <div class="visit-header">
      <div class="header-content">
        <div class="visit-title">
          <h2>
            <i class="pi pi-cog mr-2"></i>
            Visite d'Implémentation - {{ visit.dossierReference }}
          </h2>
          <div class="status-badges">
            <Tag 
              :value="getStatusLabel(visit.statut)" 
              :severity="getStatusSeverity(visit.statut)"
            />
            <Tag 
              v-if="visit.isOverdue" 
              value="EN RETARD" 
              severity="danger"
              class="ml-2"
            />
          </div>
        </div>
        
        <div class="visit-dates">
          <div class="date-item">
            <label>Date programmée:</label>
            <span>{{ formatDate(visit.dateVisite) }}</span>
          </div>
          <div v-if="visit.dateConstat" class="date-item">
            <label>Date de constat:</label>
            <span>{{ formatDate(visit.dateConstat) }}</span>
          </div>
          <div v-if="visit.daysUntilVisit !== null" class="date-item">
            <label>Échéance:</label>
            <span :class="getDaysUntilClass()">
              {{ getDaysUntilText() }}
            </span>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="header-actions">
        <Button 
          v-if="visit.canComplete"
          label="Finaliser" 
          icon="pi pi-check-circle" 
          @click="$emit('complete-visit', visit)"
          class="p-button-success"
        />
        <Button 
          v-if="visit.canModify"
          label="Modifier" 
          icon="pi pi-pencil" 
          @click="toggleEditMode"
          :class="editMode ? 'p-button-warning' : 'p-button-outlined'"
        />
        <Button 
          label="Fermer" 
          icon="pi pi-times" 
          @click="$emit('close')"
          class="p-button-outlined"
        />
      </div>
    </div>

    <!-- Progress Summary -->
    <div class="progress-summary">
      <h3>
        <i class="pi pi-chart-bar mr-2"></i>
        Avancement du Projet
      </h3>
      <div class="progress-content">
        <div class="progress-circle">
          <div class="progress-text">
            <div class="progress-percentage">{{ visit.pourcentageAvancement || 0 }}%</div>
            <div class="progress-label">Terminé</div>
          </div>
        </div>
        <div class="progress-info">
          <div class="info-item">
            <span class="label">Durée visite:</span>
            <span class="value">{{ visit.dureeVisite || 'Non définie' }} minutes</span>
          </div>
          <div class="info-item">
            <span class="label">Phase:</span>
            <span class="value">{{ getProjectPhase() }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Dossier Information -->
    <div class="info-section">
      <h3>
        <i class="pi pi-folder mr-2"></i>
        Informations du Projet
      </h3>
      
      <div class="info-grid">
        <div class="info-group">
          <h4>Agriculteur</h4>
          <div class="info-details">
            <div class="detail-item">
              <span class="label">Nom complet:</span>
              <span class="value">{{ visit.agriculteurPrenom }} {{ visit.agriculteurNom }}</span>
            </div>
            <div class="detail-item">
              <span class="label">CIN:</span>
              <span class="value">{{ visit.agriculteurCin }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Téléphone:</span>
              <span class="value">{{ visit.agriculteurTelephone }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Localisation:</span>
              <span class="value">{{ getLocationText() }}</span>
            </div>
          </div>
        </div>

        <div class="info-group">
          <h4>Projet</h4>
          <div class="info-details">
            <div class="detail-item">
              <span class="label">Type:</span>
              <span class="value">{{ visit.rubriqueDesignation }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Sous-type:</span>
              <span class="value">{{ visit.sousRubriqueDesignation }}</span>
            </div>
            <div class="detail-item">
              <span class="label">SABA:</span>
              <span class="value">{{ visit.dossierSaba }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Antenne:</span>
              <span class="value">{{ visit.antenneDesignation }}</span>
            </div>
            <div v-if="visit.cdaNom" class="detail-item">
              <span class="label">CDA:</span>
              <span class="value">{{ visit.cdaNom }}</span>
            </div>
          </div>
        </div>

        <div class="info-group">
          <h4>Service Technique</h4>
          <div class="info-details">
            <div class="detail-item">
              <span class="label">Agent Service Technique:</span>
              <span class="value">{{ visit.utilisateurServiceTechniqueNom }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Statut Dossier:</span>
              <span class="value">{{ formatDossierStatus(visit.dossierStatut) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Implementation Notes Section -->
    <div class="notes-section">
      <div class="section-header">
        <h3>
          <i class="pi pi-file-edit mr-2"></i>
          Notes d'Implémentation
        </h3>
        <div class="notes-actions">
          <Button 
            v-if="visit.canModify && !autoSaving && editMode"
            label="Sauvegarder" 
            icon="pi pi-save" 
            @click="saveNotes"
            class="p-button-sm"
            :loading="saving"
          />
          <span v-if="autoSaving" class="auto-save-indicator">
            <i class="pi pi-spin pi-spinner"></i>
            Sauvegarde automatique...
          </span>
          <span v-if="lastSaved" class="last-saved">
            Dernière sauvegarde: {{ formatDateTime(lastSaved) }}
          </span>
        </div>
      </div>

      <div class="notes-content">
        <div class="form-group">
          <label for="observations">Observations *</label>
          <Textarea 
            id="observations"
            v-model="formData.observations" 
            rows="4" 
            placeholder="Décrivez vos observations sur l'avancement du projet..."
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
          <small class="char-count">{{ formData.observations?.length || 0 }} / 1000 caractères</small>
        </div>

        <div class="form-group">
          <label for="recommendations">Recommandations techniques</label>
          <Textarea 
            id="recommendations"
            v-model="formData.recommandations" 
            rows="3" 
            placeholder="Vos recommandations techniques..."
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
          <small class="char-count">{{ formData.recommandations?.length || 0 }} / 500 caractères</small>
        </div>

        <div class="form-group">
          <label for="coordinates">Coordonnées GPS du projet</label>
          <div class="coordinates-input">
            <InputText 
              id="coordinates"
              v-model="formData.coordonneesGPS" 
              placeholder="Ex: 32.3578, -6.3491"
              :readonly="!visit.canModify || !editMode"
              @input="onNotesChange"
              class="flex-1"
            />
            <Button 
              v-if="visit.canModify && editMode"
              icon="pi pi-map-marker" 
              @click="getCurrentLocation"
              class="p-button-outlined"
              v-tooltip="'Obtenir ma position'"
              :loading="gettingLocation"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="conclusion">Conclusion générale</label>
          <Textarea 
            id="conclusion"
            v-model="formData.conclusion" 
            rows="3" 
            placeholder="Conclusion générale sur l'état d'avancement..."
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
        </div>

        <div class="form-group">
          <label for="issues">Problèmes détectés</label>
          <Textarea 
            id="issues"
            v-model="formData.problemesDetectes" 
            rows="3" 
            placeholder="Problèmes ou difficultés rencontrés..."
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
        </div>

        <div class="form-group">
          <label for="actions">Actions correctives</label>
          <Textarea 
            id="actions"
            v-model="formData.actionsCorrectives" 
            rows="3" 
            placeholder="Actions correctives recommandées..."
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
        </div>

        <div class="form-group">
          <label for="progress">Pourcentage d'avancement</label>
          <InputNumber 
            id="progress"
            v-model="formData.pourcentageAvancement" 
            suffix="%" 
            :min="0" 
            :max="100"
            placeholder="0"
            :readonly="!visit.canModify || !editMode"
            @input="onNotesChange"
            class="w-full"
          />
        </div>
      </div>
    </div>

    <!-- Photos Section -->
    <div class="photos-section">
      <div class="section-header">
        <h3>
          <i class="pi pi-image mr-2"></i>
          Photos du Projet ({{ visit.photos?.length || 0 }})
        </h3>
        <Button 
          v-if="visit.canModify && editMode"
          label="Ajouter photos" 
          icon="pi pi-plus" 
          @click="triggerFileUpload"
          class="p-button-success p-button-sm"
        />
      </div>

      <!-- File Upload (Hidden) -->
      <input 
        ref="fileInput"
        type="file" 
        multiple 
        accept="image/*,application/pdf"
        @change="onFilesSelected"
        style="display: none"
      />

      <!-- Upload Progress -->
      <div v-if="uploadingPhotos" class="upload-progress">
        <ProgressBar :value="uploadProgress" />
        <small>Upload en cours... {{ uploadProgress }}%</small>
      </div>

      <!-- Photos Grid -->
      <div v-if="visit.photos?.length > 0" class="photos-grid">
        <div 
          v-for="photo in visit.photos" 
          :key="photo.id"
          class="photo-item"
        >
          <div class="photo-preview" @click="openPhotoViewer(photo)">
            <img 
              v-if="isImageFile(photo.nomFichier)"
              :src="getPhotoUrl(photo)" 
              :alt="photo.description || photo.nomFichier"
              loading="lazy"
            />
            <div v-else class="file-preview">
              <i class="pi pi-file-pdf"></i>
              <span>{{ photo.nomFichier }}</span>
            </div>
          </div>
          
          <div class="photo-info">
            <div class="photo-title">{{ photo.nomFichier }}</div>
            <div v-if="photo.description" class="photo-description">
              {{ photo.description }}
            </div>
            <div class="photo-meta">
              <small>{{ formatDateTime(photo.datePrise) }}</small>
              <div v-if="photo.coordonneesGPS" class="photo-coordinates">
                <i class="pi pi-map-marker"></i>
                {{ photo.coordonneesGPS }}
              </div>
            </div>
          </div>

          <div v-if="visit.canModify && editMode" class="photo-actions">
            <Button 
              icon="pi pi-download" 
              @click="downloadPhoto(photo)"
              class="p-button-outlined p-button-sm"
              v-tooltip="'Télécharger'"
            />
            <Button 
              icon="pi pi-trash" 
              @click="confirmDeletePhoto(photo)"
              class="p-button-danger p-button-outlined p-button-sm"
              v-tooltip="'Supprimer'"
            />
          </div>
        </div>
      </div>

      <!-- Empty Photos State -->
      <div v-else class="empty-photos">
        <i class="pi pi-image"></i>
        <p>Aucune photo ajoutée</p>
        <Button 
          v-if="visit.canModify && editMode"
          label="Ajouter la première photo" 
          icon="pi pi-plus" 
          @click="triggerFileUpload"
          class="p-button-outlined"
        />
      </div>
    </div>

    <!-- Result Section (if visit completed) -->
    <div v-if="visit.dateConstat" class="result-section">
      <h3>
        <i class="pi pi-check-circle mr-2"></i>
        Résultat de la Visite
      </h3>
      
      <div class="result-content">
        <div class="result-decision">
          <Tag 
            :value="visit.approuve ? 'IMPLÉMENTATION APPROUVÉE' : 'IMPLÉMENTATION REJETÉE'" 
            :severity="visit.approuve ? 'success' : 'danger'"
            class="decision-tag"
          />
        </div>
        
        <div v-if="visit.conclusion" class="conclusion">
          <h4>Conclusion</h4>
          <p>{{ visit.conclusion }}</p>
        </div>

        <div v-if="visit.problemesDetectes" class="problems">
          <h4>Problèmes détectés</h4>
          <p>{{ visit.problemesDetectes }}</p>
        </div>

        <div v-if="visit.actionsCorrectives" class="corrective-actions">
          <h4>Actions correctives</h4>
          <p>{{ visit.actionsCorrectives }}</p>
        </div>
      </div>
    </div>

    <!-- Photo Viewer Dialog -->
    <Dialog 
      v-model:visible="showPhotoViewer" 
      modal 
      :header="selectedPhoto?.nomFichier"
      :style="{ width: '80vw', maxWidth: '800px' }"
    >
      <div v-if="selectedPhoto" class="photo-viewer">
        <img 
          v-if="isImageFile(selectedPhoto.nomFichier)"
          :src="getPhotoUrl(selectedPhoto)" 
          :alt="selectedPhoto.description"
          class="full-photo"
        />
        <div v-else class="file-viewer">
          <i class="pi pi-file-pdf"></i>
          <p>{{ selectedPhoto.nomFichier }}</p>
          <Button 
            label="Télécharger" 
            icon="pi pi-download" 
            @click="downloadPhoto(selectedPhoto)"
          />
        </div>
        
        <div v-if="selectedPhoto.description" class="photo-description-full">
          <h4>Description</h4>
          <p>{{ selectedPhoto.description }}</p>
        </div>
        
        <div v-if="selectedPhoto.coordonneesGPS" class="photo-coordinates-full">
          <h4>Coordonnées GPS</h4>
          <p>{{ selectedPhoto.coordonneesGPS }}</p>
        </div>
      </div>
    </Dialog>

    <!-- Confirmation Dialogs -->
    <ConfirmDialog />
    
    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';

// PrimeVue Components
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import Textarea from 'primevue/textarea';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import ProgressBar from 'primevue/progressbar';
import Dialog from 'primevue/dialog';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';

const props = defineProps({
  visit: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['visit-updated', 'close', 'complete-visit']);

const toast = useToast();
const confirm = useConfirm();

// State
const editMode = ref(false);
const saving = ref(false);
const autoSaving = ref(false);
const uploadingPhotos = ref(false);
const uploadProgress = ref(0);
const gettingLocation = ref(false);
const showPhotoViewer = ref(false);
const selectedPhoto = ref(null);
const fileInput = ref(null);
const lastSaved = ref(null);
const autoSaveTimer = ref(null);

// Form data
const formData = reactive({
  observations: props.visit.observations || '',
  recommandations: props.visit.recommandations || '',
  coordonneesGPS: props.visit.coordonneesGPS || '',
  conclusion: props.visit.conclusion || '',
  problemesDetectes: props.visit.problemesDetectes || '',
  actionsCorrectives: props.visit.actionsCorrectives || '',
  pourcentageAvancement: props.visit.pourcentageAvancement || 0
});

// Lifecycle
onMounted(() => {
  // Setup auto-save for modifications
  if (props.visit.canModify) {
    setupAutoSave();
  }
});

onUnmounted(() => {
  cleanupAutoSave();
});

// Methods
function setupAutoSave() {
  // Save unsaved changes before page unload
  window.addEventListener('beforeunload', handleBeforeUnload);
}

function cleanupAutoSave() {
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value);
  }
  window.removeEventListener('beforeunload', handleBeforeUnload);
}

function handleBeforeUnload(event) {
  if (hasUnsavedChanges()) {
    // Attempt final auto-save
    autoSaveNotes();
  }
}

function toggleEditMode() {
  editMode.value = !editMode.value;
}

function onNotesChange() {
  if (!props.visit.canModify || !editMode.value) return;
  
  // Clear existing timer
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value);
  }
  
  // Set new timer for auto-save
  autoSaveTimer.value = setTimeout(() => {
    autoSaveNotes();
  }, 2000);
}

async function autoSaveNotes() {
  if (!props.visit.canModify || autoSaving.value || saving.value || !editMode.value) return;
  
  try {
    autoSaving.value = true;
    
    const updateData = {
      visiteId: props.visit.id,
      observations: formData.observations,
      recommandations: formData.recommandations,
      coordonneesGPS: formData.coordonneesGPS,
      conclusion: formData.conclusion,
      problemesDetectes: formData.problemesDetectes,
      actionsCorrectives: formData.actionsCorrectives,
      pourcentageAvancement: formData.pourcentageAvancement,
      isAutoSave: true
    };
    
    const response = await ApiService.put(`/service-technique/visits/${props.visit.id}/notes`, updateData);
    
    if (response.success) {
      lastSaved.value = new Date();
    }
    
  } catch (error) {
    console.error('Erreur sauvegarde automatique:', error);
  } finally {
    autoSaving.value = false;
  }
}

async function saveNotes() {
  if (!props.visit.canModify) return;
  
  try {
    saving.value = true;
    
    const updateData = {
      visiteId: props.visit.id,
      observations: formData.observations,
      recommandations: formData.recommandations,
      coordonneesGPS: formData.coordonneesGPS,
      conclusion: formData.conclusion,
      problemesDetectes: formData.problemesDetectes,
      actionsCorrectives: formData.actionsCorrectives,
      pourcentageAvancement: formData.pourcentageAvancement,
      isAutoSave: false
    };
    
    const response = await ApiService.put(`/service-technique/visits/${props.visit.id}/notes`, updateData);
    
    if (response.success) {
      lastSaved.value = new Date();
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Notes sauvegardées avec succès',
        life: 3000
      });
      
      emit('visit-updated');
    } else {
      throw new Error(response.message || 'Erreur lors de la sauvegarde');
    }
    
  } catch (error) {
    console.error('Erreur sauvegarde notes:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de la sauvegarde',
      life: 4000
    });
  } finally {
    saving.value = false;
  }
}

async function getCurrentLocation() {
  if (!navigator.geolocation) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'La géolocalisation n\'est pas supportée',
      life: 4000
    });
    return;
  }

  try {
    gettingLocation.value = true;
    
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000
      });
    });

    const lat = position.coords.latitude.toFixed(6);
    const lng = position.coords.longitude.toFixed(6);
    formData.coordonneesGPS = `${lat}, ${lng}`;
    
    onNotesChange();
    
    toast.add({
      severity: 'success',
      summary: 'Position obtenue',
      detail: 'Coordonnées GPS mises à jour',
      life: 3000
    });
    
  } catch (error) {
    console.error('Erreur géolocalisation:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'obtenir la position',
      life: 4000
    });
  } finally {
    gettingLocation.value = false;
  }
}

function triggerFileUpload() {
  fileInput.value.click();
}

async function onFilesSelected(event) {
  const files = Array.from(event.target.files);
  if (files.length === 0) return;

  try {
    uploadingPhotos.value = true;
    uploadProgress.value = 0;

    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });

    // Simulate progress since we don't have a real progress callback
    const progressInterval = setInterval(() => {
      uploadProgress.value = Math.min(uploadProgress.value + 10, 90);
    }, 200);

    // Use fetch for multipart request
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const response = await fetch(`/api/service-technique/visits/${props.visit.id}/photos`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
        // Don't set Content-Type - let browser set it with boundary
      },
      body: formData
    });

    clearInterval(progressInterval);
    uploadProgress.value = 100;

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: result.message || `${files.length} photo(s) ajoutée(s)`,
        life: 3000
      });

      emit('visit-updated');
    } else {
      throw new Error(result.message || 'Erreur lors de l\'upload');
    }

  } catch (error) {
    console.error('Erreur upload photos:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de l\'upload des photos',
      life: 4000
    });
  } finally {
    uploadingPhotos.value = false;
    uploadProgress.value = 0;
    // Reset file input
    event.target.value = '';
  }
}

function openPhotoViewer(photo) {
  selectedPhoto.value = photo;
  showPhotoViewer.value = true;
}

async function downloadPhoto(photo) {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const response = await fetch(photo.downloadUrl, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', photo.nomFichier);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
  } catch (error) {
    console.error('Erreur téléchargement photo:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger la photo',
      life: 4000
    });
  }
}

function confirmDeletePhoto(photo) {
  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer la photo "${photo.nomFichier}" ?`,
    header: 'Confirmer la suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    accept: () => {
      deletePhoto(photo);
    }
  });
}

async function deletePhoto(photo) {
  try {
    const response = await ApiService.delete(`/service-technique/visits/photos/${photo.id}`);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Photo supprimée avec succès',
        life: 3000
      });
      
      emit('visit-updated');
    } else {
      throw new Error(response.message || 'Erreur lors de la suppression');
    }
    
  } catch (error) {
    console.error('Erreur suppression photo:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de la suppression',
      life: 4000
    });
  }
}

// Utility functions
function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}

function formatDateTime(dateTime) {
  if (!dateTime) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(dateTime));
}

function getLocationText() {
  const parts = [];
  if (props.visit.agriculteurDouar) parts.push(props.visit.agriculteurDouar);
  if (props.visit.agriculteurCommune) parts.push(props.visit.agriculteurCommune);
  return parts.join(' - ') || 'Non spécifié';
}

function getStatusLabel(status) {
  const labels = {
    'NOUVELLE': 'Nouvelle',
    'PROGRAMMEE': 'Programmée',
    'EN_RETARD': 'En retard',
    'COMPLETEE': 'Complétée',
    'APPROUVEE': 'Approuvée',
    'REJETEE': 'Rejetée'
  };
  return labels[status] || status;
}

function getStatusSeverity(status) {
  const severities = {
    'NOUVELLE': 'secondary',
    'PROGRAMMEE': 'info',
    'EN_RETARD': 'danger',
    'COMPLETEE': 'warning',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severities[status] || 'secondary';
}

function getDaysUntilClass() {
  if (props.visit.daysUntilVisit === null) return '';
  if (props.visit.daysUntilVisit < 0) return 'overdue';
  if (props.visit.daysUntilVisit <= 1) return 'urgent';
  return 'normal';
}

function getDaysUntilText() {
  const days = props.visit.daysUntilVisit;
  if (days === null) return '';
  if (days < 0) return `${Math.abs(days)} jour(s) de retard`;
  if (days === 0) return 'Aujourd\'hui';
  if (days === 1) return 'Demain';
  return `Dans ${days} jour(s)`;
}

function formatDossierStatus(status) {
  const statusLabels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En révision',
    'APPROVED': 'Approuvé',
    'REJECTED': 'Rejeté',
    'REALIZATION_IN_PROGRESS': 'En cours de réalisation',
    'COMPLETED': 'Terminé'
  };
  return statusLabels[status] || status;
}

function getProjectPhase() {
  const progress = props.visit.pourcentageAvancement || 0;
  if (progress === 100) return 'Terminé';
  if (progress >= 75) return 'Finition';
  if (progress >= 50) return 'Avancement';
  if (progress >= 25) return 'Démarrage';
  return 'Préparation';
}

function isImageFile(filename) {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
  return imageExtensions.some(ext => filename.toLowerCase().endsWith(ext));
}

function getPhotoUrl(photo) {
  // In real implementation, this would return the actual photo URL
  // For now, we'll use the download URL as a placeholder
  return photo.downloadUrl || '#';
}

function hasUnsavedChanges() {
  return formData.observations !== props.visit.observations ||
         formData.recommandations !== props.visit.recommandations ||
         formData.coordonneesGPS !== props.visit.coordonneesGPS ||
         formData.conclusion !== props.visit.conclusion ||
         formData.problemesDetectes !== props.visit.problemesDetectes ||
         formData.actionsCorrectives !== props.visit.actionsCorrectives ||
         formData.pourcentageAvancement !== props.visit.pourcentageAvancement;
}
</script>

<style scoped>
.implementation-visit-detail-container {
  padding: 1rem;
}

.visit-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.header-content {
  flex: 1;
}

.visit-title h2 {
  margin: 0 0 1rem 0;
  color: #495057;
}

.status-badges {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.visit-dates {
  display: flex;
  gap: 2rem;
}

.date-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.date-item label {
  font-weight: 600;
  color: #6c757d;
  font-size: 0.875rem;
}

.date-item span {
  font-size: 1rem;
  color: #495057;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.progress-summary {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.progress-summary h3 {
  margin: 0 0 1rem 0;
  color: #495057;
}

.progress-content {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.progress-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: conic-gradient(#28a745 var(--progress-angle, 0deg), #e9ecef 0deg);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.progress-circle::before {
  content: '';
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  background: white;
}

.progress-text {
  position: relative;
  z-index: 1;
  text-align: center;
}

.progress-percentage {
  font-size: 1.5rem;
  font-weight: bold;
  color: #28a745;
}

.progress-label {
  font-size: 0.875rem;
  color: #6c757d;
}

.progress-info {
  flex: 1;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.info-item .label {
  font-weight: 600;
  color: #6c757d;
}

.info-item .value {
  color: #495057;
}

.info-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.info-section h3 {
  margin: 0 0 1.5rem 0;
  color: #495057;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.info-group h4 {
  margin: 0 0 1rem 0;
  color: #495057;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
}

.info-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
}

.detail-item .label {
  font-weight: 600;
  color: #6c757d;
  min-width: 40%;
}

.detail-item .value {
  color: #495057;
  text-align: right;
}

.notes-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h3 {
  margin: 0;
  color: #495057;
}

.notes-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.auto-save-indicator {
  color: #6c757d;
  font-size: 0.875rem;
}

.last-saved {
  color: #28a745;
  font-size: 0.875rem;
}

.notes-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 600;
  color: #495057;
}

.coordinates-input {
  display: flex;
  gap: 0.5rem;
}

.char-count {
  text-align: right;
  font-size: 0.75rem;
  color: #6c757d;
}

.photos-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.photo-item {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.photo-item:hover {
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.photo-preview {
  aspect-ratio: 4/3;
  overflow: hidden;
  cursor: pointer;
  position: relative;
}

.photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f8f9fa;
  color: #6c757d;
  gap: 0.5rem;
}

.photo-info {
  padding: 0.75rem;
}

.photo-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.photo-description {
  font-size: 0.75rem;
  color: #6c757d;
  margin-bottom: 0.5rem;
}

.photo-meta {
  font-size: 0.75rem;
  color: #6c757d;
}

.photo-coordinates {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: 0.25rem;
}

.photo-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-top: 1px solid #e9ecef;
}

.empty-photos {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.empty-photos i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.result-section {
  padding: 1.5rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.result-section h3 {
  margin: 0 0 1.5rem 0;
  color: #495057;
}

.result-decision {
  text-align: center;
  margin-bottom: 1.5rem;
}

.decision-tag {
  font-size: 1.1rem;
  padding: 0.75rem 1.5rem;
}

.result-content h4 {
  margin: 1.5rem 0 0.75rem 0;
  color: #495057;
}

.result-content p {
  margin: 0 0 1rem 0;
  color: #6c757d;
  line-height: 1.6;
}

.upload-progress {
  margin-bottom: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.photo-viewer .full-photo {
  width: 100%;
  height: auto;
  max-height: 60vh;
  object-fit: contain;
}

.file-viewer {
  text-align: center;
  padding: 2rem;
}

.photo-description-full,
.photo-coordinates-full {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e9ecef;
}

.overdue {
  color: #dc3545;
  font-weight: 600;
}

.urgent {
  color: #fd7e14;
  font-weight: 600;
}

.normal {
  color: #6c757d;
}

@media (max-width: 768px) {
  .visit-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .visit-dates {
    flex-direction: column;
    gap: 1rem;
  }
  
  .progress-content {
    flex-direction: column;
    text-align: center;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .notes-content {
    grid-template-columns: 1fr;
  }
  
  .coordinates-input {
    flex-direction: column;
  }
}
</style>