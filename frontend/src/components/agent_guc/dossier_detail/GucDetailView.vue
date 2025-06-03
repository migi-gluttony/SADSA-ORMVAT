<template>
  <DossierDetailBase 
    :breadcrumb-root="'Dossiers GUC'"
    :user-role="'AGENT_GUC'"
    @dossier-loaded="handleDossierLoaded"
    ref="baseComponent"
  >
    <!-- Additional Status Tags -->
    <template #additional-status-tags="{ dossier }">
      <Tag 
        v-if="dossier.priorite"
        :value="dossier.priorite" 
        :severity="getPrioritySeverity(dossier.priorite)"
        class="priority-tag"
      />
    </template>

    <!-- Summary Header Right -->
    <template #summary-header-right="{ dossier }">
      <div class="workflow-location">
        <i class="pi pi-map-marker"></i>
        <span>{{ getWorkflowLocationText() }}</span>
      </div>
    </template>

    <!-- Additional Admin Info -->
    <template #additional-admin-info="{ dossier }">
      <div><strong>Créé par:</strong> {{ dossier.utilisateurCreateurNom }}</div>
    </template>

    <!-- Header Actions -->
    <template #header-actions="{ dossier }">
      <Button 
        label="Ajouter Note" 
        icon="pi pi-comment" 
        @click="showAddNoteDialog"
        class="p-button-info"
      />
      
      <!-- Final Approval Button -->
      <Button 
        v-if="isFinalApprovalStage(dossier)"
        label="Décision Finale" 
        icon="pi pi-gavel" 
        @click="goToFinalApproval(dossier)"
        class="p-button-success"
      />
      
      <!-- View Fiche Button -->
      <Button 
        v-if="isApprovedWithFiche(dossier)"
        label="Voir Fiche" 
        icon="pi pi-file-text" 
        @click="goToFiche(dossier)"
        class="p-button-info"
      />
      
      <SplitButton 
        v-if="hasGUCActions(dossier)"
        :model="getGUCActionMenuItems(dossier)" 
        class="action-split-btn"
        @click="handlePrimaryGUCAction(dossier)"
      >
        {{ getPrimaryGUCActionLabel(dossier) }}
      </SplitButton>
    </template>

    <!-- Forms Section -->
    <template #forms-section="{ dossier, forms }">
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Documents et Formulaires</h2>
          <p>Documents soumis par l'antenne - lecture seule</p>
        </div>

        <!-- Read-only forms overview -->
        <div class="forms-readonly">
          <div class="completion-overview">
            <div class="completion-stats">
              <div class="stat">
                <span class="value">{{ completedFormsCount }}/{{ totalFormsCount }}</span>
                <span class="label">Formulaires complétés</span>
              </div>
              <div class="stat">
                <span class="value">{{ Math.round(completionPercentage) }}%</span>
                <span class="label">Taux de complétion</span>
              </div>
            </div>
            <ProgressBar 
              :value="completionPercentage" 
              class="completion-bar"
            />
          </div>
        </div>

        <!-- Forms Overview -->
        <div v-if="forms && forms.length > 0" class="forms-overview">
          <h3>Aperçu des formulaires ({{ forms.length }})</h3>
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
                  label="Voir les données" 
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
        label="Télécharger tout" 
        icon="pi pi-download" 
        @click="downloadAllDocuments"
        class="p-button-outlined p-button-sm"
      />
    </template>
  </DossierDetailBase>

  <!-- Action Dialogs -->
  <ActionDialogs 
    :dialogs="actionDialogs"
    @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed"
  />

  <!-- Form Data Viewer Dialog -->
  <FormDataViewerDialog 
    v-model:visible="formDataDialog.visible"
    :form="formDataDialog.form"
    :dossier="currentDossier"
  />
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierDetailBase from '@/components/dossiers/DossierDetailBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import FormDataViewerDialog from '@/components/dossiers/FormDataViewerDialog.vue';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Tag from 'primevue/tag';
import ProgressBar from 'primevue/progressbar';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const baseComponent = ref();

// State
const currentDossier = ref(null);
const currentDossierDetail = ref(null);

// Dialogs
const actionDialogs = reactive({
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false }
});

const formDataDialog = reactive({
  visible: false,
  form: null
});

// Computed properties
const completedFormsCount = computed(() => {
  return currentDossierDetail.value?.availableForms?.filter(f => f.isCompleted).length || 0;
});

const totalFormsCount = computed(() => {
  return currentDossierDetail.value?.availableForms?.length || 0;
});

const completionPercentage = computed(() => {
  if (totalFormsCount.value === 0) return 100;
  return (completedFormsCount.value / totalFormsCount.value) * 100;
});

// Permission methods
function hasGUCActions(dossier) {
  const status = dossier?.statut;
  return status === 'SUBMITTED' || status === 'Soumis au GUC' || status === 'IN_REVIEW' || status === 'En cours d\'examen';
}

function isFinalApprovalStage(dossier) {
  // Check if dossier is back from commission and needs final approval
  const etapeActuelle = currentDossierDetail.value?.etapeActuelle;
  return etapeActuelle === 'AP - Phase GUC' && 
         currentDossierDetail.value?.ordre === 4 &&
         (dossier?.statut === 'Commission Terrain Approuvé' || dossier?.statut === 'IN_REVIEW');
}

function isApprovedWithFiche(dossier) {
  // Check if dossier is approved and has a fiche
  return dossier?.statut === 'APPROVED' || dossier?.statut === 'Approuvé';
}

function getGUCActionMenuItems(dossier) {
  const items = [];
  const status = dossier?.statut;
  
  if (status === 'SUBMITTED' || status === 'Soumis au GUC') {
    items.push({
      label: 'Envoyer à la Commission',
      icon: 'pi pi-forward',
      command: () => showActionDialog('sendToCommission')
    });
  }
  
  if (status === 'SUBMITTED' || status === 'Soumis au GUC' || status === 'IN_REVIEW' || status === 'En cours d\'examen') {
    items.push({
      label: 'Retourner à l\'Antenne',
      icon: 'pi pi-undo',
      command: () => showActionDialog('returnToAntenne')
    });
    
    items.push({
      label: 'Rejeter',
      icon: 'pi pi-times-circle',
      command: () => showActionDialog('reject')
    });
  }
  
  return items;
}

function getPrimaryGUCActionLabel(dossier) {
  const status = dossier?.statut;
  if (status === 'SUBMITTED' || status === 'Soumis au GUC') {
    return 'Commission';
  }
  return 'Actions';
}

function handlePrimaryGUCAction(dossier) {
  const status = dossier?.statut;
  if (status === 'SUBMITTED' || status === 'Soumis au GUC') {
    showActionDialog('sendToCommission');
  }
}

// Methods
function handleDossierLoaded(dossier) {
  currentDossier.value = dossier.dossier;
  currentDossierDetail.value = dossier;
}

function getWorkflowLocationText() {
  const emplacement = currentDossierDetail.value?.emplacementActuel;
  switch (emplacement) {
    case 'ANTENNE':
      return 'Antenne';
    case 'GUC':
      return 'Guichet Unique Central';
    case 'COMMISSION_AHA_AF':
      return 'Commission Vérification Terrain';
    case 'SERVICE_TECHNIQUE':
      return 'Service Technique';
    default:
      return 'Non défini';
  }
}

function showAddNoteDialog() {
  // This will be handled by the base component
}

function goToFinalApproval(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/final-approval`);
}

function goToFiche(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/fiche`);
}

function showActionDialog(action) {
  actionDialogs[action] = {
    visible: true,
    dossier: currentDossier.value,
    loading: false,
    comment: '',
    priority: 'NORMALE',
    reasons: [],
    definitive: false
  };
}

function viewFormData(form) {
  formDataDialog.visible = true;
  formDataDialog.form = form;
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

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, data } = actionData;
    
    let endpoint = '';
    let payload = {};
    
    switch (action) {
      case 'sendToCommission':
        endpoint = `/dossiers/${dossier.id}/send-to-commission`;
        payload = { 
          commentaire: data.comment,
          priorite: data.priority
        };
        break;
      case 'returnToAntenne':
        endpoint = `/dossiers/${dossier.id}/return-to-antenne`;
        payload = { 
          motif: 'Complétion requise',
          commentaire: data.comment,
          documentsManquants: data.reasons
        };
        break;
      case 'reject':
        endpoint = `/dossiers/${dossier.id}/reject`;
        payload = { 
          motif: 'Rejet du dossier',
          commentaire: data.comment,
          definitif: data.definitive
        };
        break;
    }
    
    const response = await ApiService.post(endpoint, payload);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 4000
      });
      
      if (baseComponent.value) {
        baseComponent.value.loadDossierDetail();
      }
    }
    
  } catch (error) {
    console.error(`Erreur lors de l'action ${actionData.action}:`, error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Une erreur est survenue',
      life: 4000
    });
  }
}

function handleDialogClosed() {
  Object.keys(actionDialogs).forEach(key => {
    actionDialogs[key].visible = false;
  });
}

function getPrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.priority-tag {
  font-size: 0.7rem !important;
}

.workflow-location {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.workflow-location i {
  color: var(--primary-color);
}

.action-split-btn {
  height: 2.5rem !important;
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

.forms-readonly {
  margin-bottom: 2rem;
}

.completion-overview {
  background: var(--section-background);
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid var(--card-border);
}

.completion-stats {
  display: flex;
  gap: 2rem;
  margin-bottom: 1rem;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat .value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-color);
}

.stat .label {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-align: center;
}

.completion-bar {
  height: 8px;
  border-radius: 4px;
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
  .completion-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .forms-grid {
    grid-template-columns: 1fr;
  }
}
</style>