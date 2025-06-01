<template>
  <Dialog 
    v-model:visible="localVisible" 
    :header="dialogTitle"
    modal 
    class="photo-viewer-dialog"
    :style="{ width: '90vw', height: '90vh' }"
    :maximizable="true"
    @hide="resetViewer"
  >
    <div v-if="photo" class="photo-viewer-content">
      <!-- Photo Display -->
      <div class="photo-display">
        <div class="photo-container">
          <img 
            ref="photoImg"
            :src="currentPhotoUrl" 
            :alt="photo.description || 'Photo d\'inspection'"
            @load="onImageLoad"
            @error="onImageError"
            :style="imageStyle"
          />
          
          <!-- Loading overlay -->
          <div v-if="imageLoading" class="loading-overlay">
            <ProgressSpinner size="40px" />
            <span>Chargement de l'image...</span>
          </div>

          <!-- Error overlay -->
          <div v-if="imageError" class="error-overlay">
            <i class="pi pi-exclamation-triangle"></i>
            <span>Impossible de charger l'image</span>
          </div>

          <!-- Zoom controls -->
          <div class="zoom-controls">
            <Button 
              icon="pi pi-search-minus" 
              @click="zoomOut"
              :disabled="zoomLevel <= 0.25"
              class="p-button-rounded p-button-outlined p-button-sm"
              v-tooltip.top="'Zoom arrière'"
            />
            <span class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</span>
            <Button 
              icon="pi pi-search-plus" 
              @click="zoomIn"
              :disabled="zoomLevel >= 3"
              class="p-button-rounded p-button-outlined p-button-sm"
              v-tooltip.top="'Zoom avant'"
            />
            <Button 
              icon="pi pi-refresh" 
              @click="resetZoom"
              class="p-button-rounded p-button-outlined p-button-sm"
              v-tooltip.top="'Réinitialiser'"
            />
            <Button 
              icon="pi pi-arrows-alt" 
              @click="fitToContainer"
              class="p-button-rounded p-button-outlined p-button-sm"
              v-tooltip.top="'Ajuster à la fenêtre'"
            />
          </div>
        </div>
      </div>

      <!-- Photo Information -->
      <div class="photo-info">
        <div class="info-section">
          <h4><i class="pi pi-info-circle"></i> Informations</h4>
          
          <div class="info-grid">
            <div class="info-item">
              <strong>Nom du fichier:</strong>
              <span>{{ photo.nomFichier || 'Non spécifié' }}</span>
            </div>
            
            <div v-if="photo.description" class="info-item">
              <strong>Description:</strong>
              <span>{{ photo.description }}</span>
            </div>
            
            <div v-if="photo.dateCreation" class="info-item">
              <strong>Date de prise:</strong>
              <span>{{ formatDate(photo.dateCreation) }}</span>
            </div>
            
            <div v-if="photo.utilisateurNom" class="info-item">
              <strong>Prise par:</strong>
              <span>{{ photo.utilisateurNom }}</span>
            </div>
            
            <div v-if="photo.taille" class="info-item">
              <strong>Taille:</strong>
              <span>{{ formatFileSize(photo.taille) }}</span>
            </div>
            
            <div v-if="photo.resolution" class="info-item">
              <strong>Résolution:</strong>
              <span>{{ photo.resolution }}</span>
            </div>
          </div>
        </div>

        <!-- GPS Coordinates -->
        <div v-if="photo.latitude && photo.longitude" class="info-section">
          <h4><i class="pi pi-map-marker"></i> Localisation GPS</h4>
          
          <div class="gps-info">
            <div class="gps-coordinates">
              <div>
                <strong>Latitude:</strong> {{ photo.latitude.toFixed(6) }}
              </div>
              <div>
                <strong>Longitude:</strong> {{ photo.longitude.toFixed(6) }}
              </div>
            </div>
            
            <div class="gps-actions">
              <Button 
                label="Voir sur la carte" 
                icon="pi pi-map" 
                @click="openInMap"
                class="p-button-outlined p-button-sm"
              />
              <Button 
                label="Copier coordonnées" 
                icon="pi pi-copy" 
                @click="copyCoordinates"
                class="p-button-outlined p-button-sm"
              />
            </div>
          </div>
        </div>

        <!-- EXIF Data (if available) -->
        <div v-if="photo.exifData" class="info-section">
          <h4><i class="pi pi-cog"></i> Données techniques</h4>
          
          <div class="exif-grid">
            <div v-if="photo.exifData.camera" class="exif-item">
              <strong>Appareil:</strong>
              <span>{{ photo.exifData.camera }}</span>
            </div>
            
            <div v-if="photo.exifData.lens" class="exif-item">
              <strong>Objectif:</strong>
              <span>{{ photo.exifData.lens }}</span>
            </div>
            
            <div v-if="photo.exifData.iso" class="exif-item">
              <strong>ISO:</strong>
              <span>{{ photo.exifData.iso }}</span>
            </div>
            
            <div v-if="photo.exifData.aperture" class="exif-item">
              <strong>Ouverture:</strong>
              <span>f/{{ photo.exifData.aperture }}</span>
            </div>
            
            <div v-if="photo.exifData.shutterSpeed" class="exif-item">
              <strong>Vitesse:</strong>
              <span>{{ photo.exifData.shutterSpeed }}</span>
            </div>
            
            <div v-if="photo.exifData.focalLength" class="exif-item">
              <strong>Focale:</strong>
              <span>{{ photo.exifData.focalLength }}mm</span>
            </div>
          </div>
        </div>

        <!-- Comments/Notes -->
        <div v-if="photo.commentaires?.length > 0" class="info-section">
          <h4><i class="pi pi-comments"></i> Commentaires</h4>
          
          <div class="comments-list">
            <div 
              v-for="comment in photo.commentaires" 
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <strong>{{ comment.utilisateurNom }}</strong>
                <span class="comment-date">{{ formatDate(comment.dateCreation) }}</span>
              </div>
              <p class="comment-content">{{ comment.contenu }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <Button 
          label="Télécharger" 
          icon="pi pi-download" 
          @click="downloadPhoto"
          class="p-button-outlined"
        />
        <Button 
          v-if="canEditPhoto"
          label="Modifier" 
          icon="pi pi-pencil" 
          @click="editPhoto"
          class="p-button-outlined"
        />
        <Button 
          v-if="canDeletePhoto"
          label="Supprimer" 
          icon="pi pi-trash" 
          @click="confirmDeletePhoto"
          class="p-button-danger p-button-outlined"
        />
        <Button 
          label="Fermer" 
          icon="pi pi-times" 
          @click="localVisible = false"
          class="p-button-outlined"
        />
      </div>
    </template>
  </Dialog>

  <!-- Delete Confirmation Dialog -->
  <ConfirmDialog />
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import ProgressSpinner from 'primevue/progressspinner';
import ConfirmDialog from 'primevue/confirmdialog';

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  photo: {
    type: Object,
    default: null
  }
});

// Emits
const emit = defineEmits(['update:visible', 'photo-deleted', 'photo-updated']);

const toast = useToast();
const confirm = useConfirm();

// State
const imageLoading = ref(true);
const imageError = ref(false);
const zoomLevel = ref(1);
const photoImg = ref(null);

// Computed
const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const dialogTitle = computed(() => {
  return props.photo?.description || props.photo?.nomFichier || 'Photo d\'inspection';
});

const currentPhotoUrl = computed(() => {
  return props.photo?.url || props.photo?.fullUrl || '';
});

const imageStyle = computed(() => {
  return {
    transform: `scale(${zoomLevel.value})`,
    transformOrigin: 'center center',
    transition: 'transform 0.3s ease',
    maxWidth: zoomLevel.value === 1 ? '100%' : 'none',
    maxHeight: zoomLevel.value === 1 ? '100%' : 'none',
    cursor: zoomLevel.value > 1 ? 'move' : 'default'
  };
});

const canEditPhoto = computed(() => {
  const currentUser = AuthService.getCurrentUser();
  return currentUser?.role === 'AGENT_COMMISSION_TERRAIN' && 
         props.photo?.utilisateurId === currentUser?.id;
});

const canDeletePhoto = computed(() => {
  const currentUser = AuthService.getCurrentUser();
  return currentUser?.role === 'AGENT_COMMISSION_TERRAIN' && 
         props.photo?.utilisateurId === currentUser?.id;
});

// Watch for photo changes
watch(() => props.photo, (newPhoto) => {
  if (newPhoto) {
    resetViewer();
  }
});

// Methods
function onImageLoad() {
  imageLoading.value = false;
  imageError.value = false;
  fitToContainer();
}

function onImageError() {
  imageLoading.value = false;
  imageError.value = true;
}

function resetViewer() {
  imageLoading.value = true;
  imageError.value = false;
  zoomLevel.value = 1;
}

function zoomIn() {
  if (zoomLevel.value < 3) {
    zoomLevel.value = Math.min(3, zoomLevel.value * 1.25);
  }
}

function zoomOut() {
  if (zoomLevel.value > 0.25) {
    zoomLevel.value = Math.max(0.25, zoomLevel.value / 1.25);
  }
}

function resetZoom() {
  zoomLevel.value = 1;
}

function fitToContainer() {
  zoomLevel.value = 1;
}

function openInMap() {
  if (props.photo?.latitude && props.photo?.longitude) {
    const url = `https://www.google.com/maps?q=${props.photo.latitude},${props.photo.longitude}`;
    window.open(url, '_blank');
  }
}

async function copyCoordinates() {
  if (props.photo?.latitude && props.photo?.longitude) {
    const coordinates = `${props.photo.latitude}, ${props.photo.longitude}`;
    
    try {
      await navigator.clipboard.writeText(coordinates);
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Coordonnées copiées dans le presse-papiers',
        life: 3000
      });
    } catch (err) {
      console.error('Error copying coordinates:', err);
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Impossible de copier les coordonnées',
        life: 3000
      });
    }
  }
}

async function downloadPhoto() {
  if (!props.photo) return;

  try {
    const response = await fetch(props.photo.downloadUrl || props.photo.url, {
      headers: {
        'Authorization': `Bearer ${AuthService.getToken()}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', props.photo.nomFichier || 'photo_inspection.jpg');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Photo téléchargée avec succès',
      life: 3000
    });
    
  } catch (err) {
    console.error('Error downloading photo:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger la photo',
      life: 3000
    });
  }
}

function editPhoto() {
  // Open edit dialog or navigate to edit page
  console.log('Edit photo:', props.photo);
  // Implementation would depend on requirements
}

function confirmDeletePhoto() {
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer cette photo ?',
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptClass: 'p-button-danger',
    accept: deletePhoto,
    reject: () => {}
  });
}

async function deletePhoto() {
  try {
    // Implementation for deleting photo
    emit('photo-deleted', props.photo);
    localVisible.value = false;
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Photo supprimée avec succès',
      life: 3000
    });
    
  } catch (err) {
    console.error('Error deleting photo:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de supprimer la photo',
      life: 3000
    });
  }
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date));
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B';
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
}
</script>

<style scoped>
:deep(.photo-viewer-dialog) {
  --p-dialog-content-padding: 0;
}

:deep(.photo-viewer-dialog .p-dialog-header) {
  background: var(--primary-color);
  color: white;
}

:deep(.photo-viewer-dialog .p-dialog-header .p-dialog-title) {
  font-weight: 600;
}

:deep(.photo-viewer-dialog .p-dialog-header .p-dialog-header-icon) {
  color: white;
}

:deep(.photo-viewer-dialog .p-dialog-content) {
  padding: 0;
  overflow: hidden;
}

.photo-viewer-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  height: calc(90vh - 120px); /* Adjust for header and footer */
}

/* Photo Display */
.photo-display {
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.photo-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.loading-overlay,
.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  gap: 1rem;
}

.error-overlay i {
  font-size: 3rem;
  color: #ef4444;
}

/* Zoom Controls */
.zoom-controls {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(0, 0, 0, 0.7);
  padding: 0.5rem;
  border-radius: 25px;
}

.zoom-level {
  color: white;
  font-size: 0.9rem;
  min-width: 50px;
  text-align: center;
}

:deep(.zoom-controls .p-button) {
  width: 32px;
  height: 32px;
  color: white;
  border-color: rgba(255, 255, 255, 0.3);
}

:deep(.zoom-controls .p-button:not(:disabled):hover) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

/* Photo Information */
.photo-info {
  background: var(--background-color);
  border-left: 1px solid var(--card-border);
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-section h4 {
  color: var(--primary-color);
  margin: 0;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--card-border);
}

.info-grid,
.exif-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-item,
.exif-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-item strong,
.exif-item strong {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item span,
.exif-item span {
  color: var(--text-color);
  font-size: 0.9rem;
}

/* GPS Info */
.gps-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.gps-coordinates {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.gps-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* Comments */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.comment-item {
  background: var(--section-background);
  border: 1px solid var(--card-border);
  border-radius: 6px;
  padding: 0.75rem;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-size: 0.8rem;
}

.comment-date {
  color: var(--text-secondary);
}

.comment-content {
  margin: 0;
  color: var(--text-color);
  font-size: 0.85rem;
  line-height: 1.4;
}

/* Dialog Footer */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--card-border);
  background: var(--background-color);
}

/* Responsive Design */
@media (max-width: 1024px) {
  .photo-viewer-content {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr auto;
  }

  .photo-info {
    border-left: none;
    border-top: 1px solid var(--card-border);
    max-height: 250px;
  }
}

@media (max-width: 768px) {
  :deep(.photo-viewer-dialog) {
    width: 100vw !important;
    height: 100vh !important;
    max-width: none !important;
    max-height: none !important;
    margin: 0 !important;
  }

  .photo-viewer-content {
    height: calc(100vh - 120px);
  }

  .photo-info {
    padding: 1rem;
  }

  .dialog-footer {
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .zoom-controls {
    bottom: 0.5rem;
    gap: 0.25rem;
    padding: 0.25rem;
  }

  :deep(.zoom-controls .p-button) {
    width: 28px;
    height: 28px;
  }

  .zoom-level {
    font-size: 0.8rem;
    min-width: 40px;
  }
}
</style>