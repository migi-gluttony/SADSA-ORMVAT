<template>
  <DossierDetailBase 
    :breadcrumb-root="breadcrumbRoot"
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
    v-if="scheduleVisitDialog.dossier"
    v-model:visible="scheduleVisitDialog.visible"
    :dossier="scheduleVisitDialog.dossier"
    @visit-scheduled="handleVisitScheduled"
  />

  <!-- Complete Terrain Visit Dialog -->
  <CompleteTerrainVisitDialog 
    v-if="completeVisitDialog.visit"
    v-model:visible="completeVisitDialog.visible"
    :visit="completeVisitDialog.visit"
    @visit-completed="handleVisitCompleted"
  />

  <!-- Form Data Viewer Dialog -->
  <FormDataViewerDialog 
    v-if="formDataDialog.form && currentDossier"
    v-model:visible="formDataDialog.visible"
    :form="formDataDialog.form"
    :dossier="currentDossier"
  />

  <!-- Photo Viewer Dialog -->
  <PhotoViewerDialog 
    v-if="photoDialog.photo"
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

// Computed properties
const breadcrumbRoot = computed(() => {
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
});

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

