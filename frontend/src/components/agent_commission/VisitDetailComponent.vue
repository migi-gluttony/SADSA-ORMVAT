<template>
  <div class="visit-detail-container">
    <!-- Visit Header -->
    <div class="visit-header">
      <div class="header-content">
        <div class="visit-title">
          <h2>
            <i class="pi pi-map"></i>
            Visite Terrain - {{ visit.dossierReference }}
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
          label="Modifier la date" 
          icon="pi pi-calendar" 
          @click="showRescheduleDialog = true"
          class="p-button-warning"
        />
        <Button 
          label="Fermer" 
          icon="pi pi-times" 
          @click="$emit('close')"
          class="p-button-outlined"
        />
      </div>
    </div>

    <!-- Dossier Information -->
    <div class="info-section">
      <h3>
        <i class="pi pi-folder"></i>
        Informations du Dossier
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
          <h4>Commission</h4>
          <div class="info-details">
            <div class="detail-item">
              <span class="label">Agent Commission:</span>
              <span class="value">{{ visit.utilisateurCommissionNom }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Statut Dossier:</span>
              <span class="value">{{ formatDossierStatus(visit.dossierStatut) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Visit Notes Section -->
    <div class="notes-section">
      <div class="section-header">
        <h3>
          <i class="pi pi-file-edit"></i>
          Notes de Visite
        </h3>
        <div class="notes-actions">
          <Button 
            v-if="visit.canModify && !autoSaving"
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
            placeholder="Décrivez vos observations sur le terrain..."
            :readonly="!visit.canModify"
            @input="onNotesChange"
            class="w-full"
          />
          <small class="char-count">{{ formData.observations?.length || 0 }} / 1000 caractères</small>
        </div>

        <div class="form-group">
          <label for="recommendations">Recommandations</label>
          <Textarea 
            id="recommendations"
            v-model="formData.recommandations" 
            rows="3" 
            placeholder="Vos recommandations techniques..."
            :readonly="!visit.canModify"
            @input="onNotesChange"
            class="w-full"
          />
          <small class="char-count">{{ formData.recommandations?.length || 0 }} / 500 caractères</small>
        </div>

        <div class="form-group">
          <label for="coordinates">Coordonnées GPS</label>
          <div class="coordinates-input">
            <InputText 
              id="coordinates"
              v-model="formData.coordonneesGPS" 
              placeholder="Ex: 32.3578, -6.3491"
              :readonly="!visit.canModify"
              @input="onNotesChange"
              class="flex-1"
            />
            <Button 
              v-if="visit.canModify"
              icon="pi pi-map-marker" 
              @click="getCurrentLocation"
              class="p-button-outlined"
              v-tooltip="'Obtenir ma position'"
              :loading="gettingLocation"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="general-remarks">Remarques générales</label>
          <Textarea 
            id="general-remarks"
            v-model="formData.remarquesGenerales" 
            rows="2" 
            placeholder="Autres remarques ou commentaires..."
            :readonly="!visit.canModify"
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
          <i class="pi pi-image"></i>
          Photos de la Visite ({{ visit.photos?.length || 0 }})
        </h3>
        <Button 
          v-if="visit.canModify"
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

          <div v-if="visit.canModify" class="photo-actions">
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
          v-if="visit.canModify"
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
        <i class="pi pi-check-circle"></i>
        Résultat de la Visite
      </h3>
      
      <div class="result-content">
        <div class="result-decision">
          <Tag 
            :value="visit.approuve ? 'TERRAIN APPROUVÉ' : 'TERRAIN REJETÉ'" 
            :severity="visit.approuve ? 'success' : 'danger'"
            class="decision-tag"
          />
        </div>
        
        <div v-if="visit.motifRejet && !visit.approuve" class="rejection-details">
          <h4>Motif de rejet</h4>
          <p>{{ formatMotifRejet(visit.motifRejet) }}</p>
          
          <div v-if="visit.pointsNonConformes?.length > 0" class="non-conform-points">
            <h5>Points non conformes:</h5>
            <ul>
              <li v-for="point in visit.pointsNonConformes" :key="point">{{ point }}</li>
            </ul>
          </div>
        </div>

        <div v-if="visit.remarquesGenerales" class="general-remarks">
          <h4>Remarques générales</h4>
          <p>{{ visit.remarquesGenerales }}</p>
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

    <!-- Reschedule Dialog -->
    <Dialog 
      v-model:visible="showRescheduleDialog" 
      modal 
      header="Reprogrammer la Visite"
      :style="{ width: '400px' }"
    >
      <div class="reschedule-form">
        <div class="form-group">
          <label>Nouvelle date de visite *</label>
          <Calendar 
            v-model="newVisitDate" 
            dateFormat="dd/mm/yy"
            :minDate="minDate"
            showIcon
            class="w-full"
          />
        </div>
        
        <div class="form-group">
          <label>Motif de reprogrammation</label>
          <Textarea 
            v-model="rescheduleReason" 
            rows="3" 
            placeholder="Expliquer pourquoi la visite est reprogrammée..."
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="showRescheduleDialog = false"
          class="p-button-outlined"
        />
        <Button 
          label="Reprogrammer" 
          icon="pi pi-calendar" 
          @click="rescheduleVisit"
          :loading="rescheduling"
        />
      </template>
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
import Calendar from 'primevue/calendar';
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
const saving = ref(false);
const autoSaving = ref(false);
const uploadingPhotos = ref(false);
const uploadProgress = ref(0);
const gettingLocation = ref(false);
const rescheduling = ref(false);
const showPhotoViewer = ref(false);
const showRescheduleDialog = ref(false);
const selectedPhoto = ref(null);
const fileInput = ref(null);
const newVisitDate = ref(null);
const rescheduleReason = ref('');
const lastSaved = ref(null);
const autoSaveTimer = ref(null);

// Form data
const formData = reactive({
  observations: props.visit.observations || '',
  recommandations: props.visit.recommandations || '',
  coordonneesGPS: props.visit.coordonneesGPS || '',
  remarquesGenerales: props.visit.remarquesGenerales || ''
});

// Computed
const minDate = computed(() => new Date());

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

function onNotesChange() {
  if (!props.visit.canModify) return;
  
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
  if (!props.visit.canModify || autoSaving.value || saving.value) return;
  
  try {
    autoSaving.value = true;
    
    const updateData = {
      visiteId: props.visit.id,
      observations: formData.observations,
      recommandations: formData.recommandations,
      coordonneesGPS: formData.coordonneesGPS,
      remarquesGenerales: formData.remarquesGenerales,
      isAutoSave: true
    };
    
    const response = await ApiService.put(`/agent_commission/terrain-visits/${props.visit.id}/notes`, updateData);
    
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
      remarquesGenerales: formData.remarquesGenerales,
      isAutoSave: false
    };
    
    const response = await ApiService.put(`/agent_commission/terrain-visits/${props.visit.id}/notes`, updateData);
    
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

    const response = await ApiService.post(`/agent_commission/terrain-visits/${props.visit.id}/photos`, formData);

    clearInterval(progressInterval);
    uploadProgress.value = 100;

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || `${files.length} photo(s) ajoutée(s)`,
        life: 3000
      });

      emit('visit-updated');
    } else {
      throw new Error(response.message || 'Erreur lors de l\'upload');
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
    const response = await ApiService.delete(`/agent_commission/terrain-visits/photos/${photo.id}`);
    
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

async function rescheduleVisit() {
  if (!newVisitDate.value) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Veuillez sélectionner une nouvelle date',
      life: 4000
    });
    return;
  }

  try {
    rescheduling.value = true;
    
    const rescheduleData = {
      dossierId: props.visit.dossierId,
      dateVisite: formatDateForAPI(newVisitDate.value),
      observations: formData.observations,
      coordonneesGPS: formData.coordonneesGPS,
      recommandations: formData.recommandations
    };
    
    const response = await ApiService.post('/agent_commission/terrain-visits/schedule', rescheduleData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite reprogrammée avec succès',
        life: 3000
      });
      
      showRescheduleDialog.value = false;
      emit('visit-updated');
    } else {
      throw new Error(response.message || 'Erreur lors de la reprogrammation');
    }
    
  } catch (error) {
    console.error('Erreur reprogrammation:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de la reprogrammation',
      life: 4000
    });
  } finally {
    rescheduling.value = false;
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

function formatDateForAPI(date) {
  if (!date) return null;
  return date.toISOString().split('T')[0];
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
    'COMPLETED': 'Terminé'
  };
  return statusLabels[status] || status;
}

function formatMotifRejet(motif) {
  const motifLabels = {
    'TERRAIN_NON_CONFORME': 'Terrain non conforme aux spécifications',
    'ACCESSIBILITE_INSUFFISANTE': 'Accessibilité insuffisante',
    'PROBLEMES_ENVIRONNEMENTAUX': 'Problèmes environnementaux',
    'NORMES_SECURITE': 'Non-respect des normes de sécurité',
    'FAISABILITE_TECHNIQUE': 'Faisabilité technique douteuse',
    'CONFLIT_REGLEMENTAIRE': 'Conflit avec réglementations locales',
    'AUTRE': 'Autre motif'
  };
  return motifLabels[motif] || motif;
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
         formData.remarquesGenerales !== props.visit.remarquesGenerales;
}
</script>
