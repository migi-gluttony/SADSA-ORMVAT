<template>
  <DossierDetailBase 
    :breadcrumb-root="getBreadcrumbRoot()"
    :user-role="'AGENT_COMMISSION_TERRAIN'"
    @dossier-loaded="handleDossierLoaded"
    ref="baseComponent"
  >
    <!-- Header Actions -->
    <template #header-actions="{ dossier }">
      <Button 
        v-if="canScheduleTerrainInspection(dossier)"
        label="Programmer Inspection Terrain" 
        icon="pi pi-calendar-plus" 
        @click="showScheduleVisitDialog"
        class="p-button-info"
      />
      <Button 
        v-if="canCompleteTerrainInspection(dossier)"
        label="Finaliser Inspection" 
        icon="pi pi-check-square" 
        @click="showCompleteVisitDialog"
        class="p-button-success"
      />
      <Button 
        label="Ajouter Note" 
        icon="pi pi-comment" 
        @click="showAddNoteDialog"
        class="p-button-outlined"
      />
    </template>

    <!-- Summary Header Right -->
    <template #summary-header-right="{ dossier }">
      <div class="terrain-inspection-status">
        <h4>Inspection Terrain</h4>
        <div v-if="currentTerrainVisit" class="visit-info">
          <Tag 
            :value="currentTerrainVisit.statut || 'À programmer'"
            :severity="getVisitSeverity(currentTerrainVisit.statut)"
          />
          <div v-if="currentTerrainVisit.dateVisite" class="visit-date">
            <i class="pi pi-calendar"></i>
            Programmée le {{ formatDate(currentTerrainVisit.dateVisite) }}
          </div>
          <div v-if="currentTerrainVisit.dateConstat" class="completion-date">
            <i class="pi pi-check"></i>
            Finalisée le {{ formatDate(currentTerrainVisit.dateConstat) }}
          </div>
        </div>
        <div v-else class="no-visit">
          <Tag value="À programmer" severity="warning" />
        </div>
      </div>
    </template>

    <!-- Forms Section -->
    <template #forms-section="{ dossier, forms }">
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Documents et Formulaires</h2>
          <p>Documents soumis pour vérification terrain</p>
        </div>

        <!-- Terrain Inspection Form -->
        <div v-if="currentTerrainVisit" class="inspection-form">
          <div class="inspection-header">
            <h3><i class="pi pi-map-marker"></i> Rapport d'Inspection Terrain</h3>
            <div class="inspection-actions">
              <Button 
                v-if="canEditInspection()"
                label="Modifier le rapport" 
                icon="pi pi-pencil" 
                @click="editInspectionReport"
                class="p-button-outlined p-button-sm"
              />
              <Button 
                v-if="canViewInspection()"
                label="Voir le rapport" 
                icon="pi pi-eye" 
                @click="viewInspectionReport"
                class="p-button-outlined p-button-sm"
              />
            </div>
          </div>
          
          <div v-if="currentTerrainVisit.observations" class="inspection-summary">
            <h4>Observations</h4>
            <p>{{ currentTerrainVisit.observations }}</p>
          </div>
          
          <div v-if="currentTerrainVisit.photos?.length > 0" class="inspection-photos">
            <h4>Photos de l'inspection ({{ currentTerrainVisit.photos.length }})</h4>
            <div class="photos-grid">
              <div 
                v-for="photo in currentTerrainVisit.photos" 
                :key="photo.id"
                class="photo-item"
                @click="viewPhoto(photo)"
              >
                <img :src="photo.thumbnailUrl" :alt="photo.description" />
                <div class="photo-overlay">
                  <i class="pi pi-eye"></i>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Standard Forms Overview -->
        <div v-if="forms && forms.length > 0" class="forms-overview">
          <h3>Documents du dossier ({{ forms.length }})</h3>
          <div class="forms-grid">
            <div 
              v-for="form in forms" 
              :key="form.formId"
              class="form-card form-readonly"
              :class="{ 'form-completed': form.isCompleted }"
            >
              <div class="form-info">
                <h4>{{ form.title }}</h4>
                <div class="form-tags">
                  <Tag v-if="form.isCompleted" value="Complété" severity="success" />
                  <Tag v-else value="En attente" severity="warning" />
                  <Tag value="Lecture seule" severity="info" />
                </div>
              </div>
              <p v-if="form.description" class="form-description">{{ form.description }}</p>
              <div v-if="form.lastModified" class="form-meta">
                <small>Dernière modification: {{ formatDate(form.lastModified) }}</small>
              </div>
              <div v-if="form.isCompleted" class="form-actions">
                <Button 
                  label="Consulter" 
                  icon="pi pi-eye" 
                  @click="viewFormData(form)"
                  class="p-button-outlined p-button-sm"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Files Actions -->
    <template #files-actions="{ files }">
      <Button 
        v-if="files.length > 0"
        label="Télécharger documents" 
        icon="pi pi-download" 
        @click="downloadAllDocuments"
        class="p-button-outlined p-button-sm"
      />
    </template>
  </DossierDetailBase>

  <!-- Schedule Terrain Visit Dialog -->
  <ScheduleTerrainVisitDialog 
    v-model:visible="scheduleVisitDialog.visible"
    :dossier="scheduleVisitDialog.dossier"
    @visit-scheduled="handleVisitScheduled"
  />

  <!-- Complete Terrain Visit Dialog -->
  <CompleteTerrainVisitDialog 
    v-model:visible="completeVisitDialog.visible"
    :visit="completeVisitDialog.visit"
    @visit-completed="handleVisitCompleted"
  />

  <!-- Form Data Viewer Dialog -->
  <FormDataViewerDialog 
    v-model:visible="formDataDialog.visible"
    :form="formDataDialog.form"
    :dossier="currentDossier"
  />

  <!-- Photo Viewer Dialog -->
  <PhotoViewerDialog 
    v-model:visible="photoDialog.visible"
    :photo="photoDialog.photo"
  />
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierDetailBase from '@/components/dossiers/DossierDetailBase.vue';
import ScheduleTerrainVisitDialog from '@/components/agent_commission/ScheduleTerrainVisitDialog.vue';
import CompleteTerrainVisitDialog from '@/components/agent_commission/CompleteVisitComponent.vue';
import FormDataViewerDialog from '@/components/dossiers/FormDataViewerDialog.vue';
import PhotoViewerDialog from '@/components/dossiers/PhotoViewerDialog.vue';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const baseComponent = ref();

// State
const currentDossier = ref(null);
const currentDossierDetail = ref(null);

// Dialogs
const scheduleVisitDialog = reactive({
  visible: false,
  dossier: null
});

const completeVisitDialog = reactive({
  visible: false,
  visit: null
});

const formDataDialog = reactive({
  visible: false,
  form: null
});

const photoDialog = reactive({
  visible: false,
  photo: null
});

// Computed
const currentTerrainVisit = computed(() => {
  const visits = currentDossierDetail.value?.visitesTerrain;
  return visits && visits.length > 0 ? visits[visits.length - 1] : null;
});

// Permission methods
function canScheduleTerrainInspection(dossier) {
  const status = dossier?.statut;
  return status === 'SUBMITTED' || status === 'Soumis au GUC' || status === 'IN_REVIEW' || status === 'En cours d\'examen';
}

function canCompleteTerrainInspection(dossier) {
  return currentTerrainVisit.value?.dateVisite && !currentTerrainVisit.value?.dateConstat;
}

function canEditInspection() {
  return currentTerrainVisit.value && !currentTerrainVisit.value.dateConstat;
}

function canViewInspection() {
  return currentTerrainVisit.value?.dateVisite;
}

// Methods
function getBreadcrumbRoot() {
  const currentUser = AuthService.getCurrentUser();
  const equipe = currentUser?.equipeCommission;
  
  if (equipe) {
    const teamNames = {
      'FILIERES_VEGETALES': 'Commission Filières Végétales',
      'FILIERES_ANIMALES': 'Commission Filières Animales',
      'AMENAGEMENT_HYDRO_AGRICOLE': 'Commission Aménagement'
    };
    return teamNames[equipe] || 'Commission Terrain';
  }
  return 'Commission Terrain';
}

function handleDossierLoaded(dossier) {
  currentDossier.value = dossier.dossier;
  currentDossierDetail.value = dossier;
}

function showAddNoteDialog() {
  // This will be handled by the base component
}

function showScheduleVisitDialog() {
  scheduleVisitDialog.visible = true;
  scheduleVisitDialog.dossier = currentDossier.value;
}

function showCompleteVisitDialog() {
  completeVisitDialog.visible = true;
  completeVisitDialog.visit = currentTerrainVisit.value;
}

function editInspectionReport() {
  router.push(`/agent_commission/terrain-visits/${currentTerrainVisit.value.id}/edit`);
}

function viewInspectionReport() {
  router.push(`/agent_commission/terrain-visits/${currentTerrainVisit.value.id}`);
}

function viewFormData(form) {
  formDataDialog.visible = true;
  formDataDialog.form = form;
}

function viewPhoto(photo) {
  photoDialog.visible = true;
  photoDialog.photo = photo;
}

async function downloadAllDocuments() {
  try {
    const response = await fetch(`/api/dossiers/${route.params.dossierId}/documents/download-all`, {
      headers: {
        'Authorization': `Bearer ${AuthService.getToken()}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `dossier_${currentDossier.value.reference}_documents.zip`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Archive téléchargée avec succès',
      life: 3000
    });
    
  } catch (err) {
    console.error('Erreur téléchargement archive:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger l\'archive',
      life: 3000
    });
  }
}

async function handleVisitScheduled() {
  scheduleVisitDialog.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain programmée avec succès',
    life: 4000
  });
  
  if (baseComponent.value) {
    baseComponent.value.loadDossierDetail();
  }
}

async function handleVisitCompleted() {
  completeVisitDialog.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Inspection terrain finalisée avec succès',
    life: 4000
  });
  
  if (baseComponent.value) {
    baseComponent.value.loadDossierDetail();
  }
}

function getVisitSeverity(status) {
  const severityMap = {
    'À programmer': 'warning',
    'PROGRAMMEE': 'info',
    'COMPLETEE': 'success',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severityMap[status] || 'secondary';
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.terrain-inspection-status {
  text-align: right;
}

.terrain-inspection-status h4 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
}

.visit-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-end;
}

.visit-date,
.completion-date {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.visit-date i,
.completion-date i {
  color: var(--primary-color);
}

/* Forms Section */
.forms-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1.5rem;
}

.section-header h2 {
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

.inspection-form {
  background: var(--section-background);
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid var(--card-border);
  margin-bottom: 2rem;
}

.inspection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--card-border);
}

.inspection-header h3 {
  color: var(--primary-color);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.inspection-actions {
  display: flex;
  gap: 0.5rem;
}

.inspection-summary {
  margin-bottom: 1.5rem;
}

.inspection-summary h4 {
  color: var(--text-color);
  margin: 0 0 0.75rem 0;
  font-size: 1rem;
}

.inspection-summary p {
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.inspection-photos {
  margin-bottom: 1.5rem;
}

.inspection-photos h4 {
  color: var(--text-color);
  margin: 0 0 1rem 0;
  font-size: 1rem;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 1rem;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.photo-item:hover {
  transform: scale(1.02);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  color: white;
  font-size: 1.5rem;
}

.photo-item:hover .photo-overlay {
  opacity: 1;
}

.forms-overview {
  background: var(--section-background);
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.forms-overview h3 {
  color: var(--primary-color);
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.forms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.form-card {
  background: var(--background-color);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  transition: all 0.3s ease;
}

.form-card.form-completed {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.form-card.form-readonly {
  border-style: dashed;
}

.form-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.form-info h4 {
  margin: 0;
  color: #374151;
  font-size: 0.9rem;
  flex: 1;
}

.form-tags {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.form-description {
  color: #6b7280;
  font-size: 0.8rem;
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.form-meta {
  color: #6b7280;
  font-size: 0.75rem;
  margin-bottom: 0.5rem;
}

.form-actions {
  margin-top: 0.5rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .terrain-inspection-status {
    text-align: left;
  }

  .visit-info {
    align-items: flex-start;
  }

  .inspection-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .inspection-actions {
    justify-content: center;
  }

  .photos-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }

  .forms-grid {
    grid-template-columns: 1fr;
  }
}
</style>