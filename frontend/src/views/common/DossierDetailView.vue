<template>
  <div class="dossier-detail-container">
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
            <span>{{ getBreadcrumbRoot() }}</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ dossierDetail.dossier.reference }}</span>
          </div>
        </div>
        
        <!-- Role-specific header actions -->
        <div class="header-actions">
          <!-- Agent Antenne Actions -->
          <template v-if="userRole === 'AGENT_ANTENNE'">
            <Button 
              v-if="canEdit()"
              label="Envoyer au GUC" 
              icon="pi pi-send" 
              @click="confirmSendToGUC"
              class="p-button-success"
            />
            <Button 
              v-if="canDelete()"
              label="Supprimer" 
              icon="pi pi-trash" 
              @click="confirmDelete"
              class="p-button-danger"
            />
          </template>

          <!-- Agent GUC Actions -->
          <template v-if="userRole === 'AGENT_GUC'">
            <Button 
              label="Ajouter Note" 
              icon="pi pi-comment" 
              @click="showAddNoteDialog"
              class="p-button-info"
            />
            
            <SplitButton 
              v-if="hasGUCActions()"
              :model="getGUCActionMenuItems()" 
              class="action-split-btn"
              @click="handlePrimaryGUCAction"
            >
              {{ getPrimaryGUCActionLabel() }}
            </SplitButton>
          </template>

          <!-- Agent Commission Actions -->
          <template v-if="userRole === 'AGENT_COMMISSION_TERRAIN'">
            <Button 
              v-if="canScheduleTerrainVisit()"
              label="Programmer Visite Terrain" 
              icon="pi pi-calendar-plus" 
              @click="showScheduleVisitDialog"
              class="p-button-info"
            />
            <Button 
              v-if="canCompleteTerrainVisit()"
              label="Compléter Visite" 
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
        </div>
      </div>

      <!-- Dossier Summary -->
      <div class="dossier-summary component-card">
        <div class="summary-header">
          <div class="header-left">
            <h2>{{ dossierDetail.dossier.reference }}</h2>
            <div class="status-info">
              <Tag 
                :value="dossierDetail.dossier.statut" 
                :severity="getStatusSeverity(dossierDetail.dossier.statut)"
              />
              <Tag 
                v-if="userRole === 'AGENT_GUC' && dossierDetail.dossier.priorite"
                :value="dossierDetail.dossier.priorite" 
                :severity="getPrioritySeverity(dossierDetail.dossier.priorite)"
                class="priority-tag"
              />
            </div>
          </div>
          
          <div v-if="userRole === 'AGENT_GUC'" class="header-right">
            <div class="workflow-location">
              <i class="pi pi-map-marker"></i>
              <span>{{ getWorkflowLocationText() }}</span>
            </div>
          </div>
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
              <div v-if="dossierDetail.agriculteur.douar">
                <strong>Douar:</strong> {{ dossierDetail.agriculteur.douar }}
              </div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>Projet</h4>
            <div class="info-lines">
              <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
              <div><strong>Rubrique:</strong> {{ dossierDetail.dossier.rubriqueDesignation }}</div>
              <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
              <div v-if="dossierDetail.dossier.montantSubvention">
                <strong>Montant:</strong> {{ formatCurrency(dossierDetail.dossier.montantSubvention) }}
              </div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>Localisation Administrative</h4>
            <div class="info-lines">
              <div><strong>CDA:</strong> {{ dossierDetail.dossier.cdaNom }}</div>
              <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
              <div v-if="userRole === 'AGENT_GUC'">
                <strong>Créé par:</strong> {{ dossierDetail.dossier.utilisateurCreateurNom }}
              </div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>État du dossier</h4>
            <div class="info-lines">
              <div><strong>Étape:</strong> {{ dossierDetail.etapeActuelle }}</div>
              <div class="time-info">
                <strong>Temps restant:</strong>
                <span :class="{
                  'time-critical': dossierDetail.joursRestants <= 1,
                  'time-warning': dossierDetail.joursRestants <= 2 && dossierDetail.joursRestants > 1,
                  'time-ok': dossierDetail.joursRestants > 2
                }">
                  {{ dossierDetail.joursRestants > 0 ? `${dossierDetail.joursRestants} jour(s)` : 'Délai dépassé' }}
                </span>
              </div>
              <div><strong>Créé le:</strong> {{ formatDate(dossierDetail.dossier.dateCreation) }}</div>
              <div v-if="dossierDetail.dossier.dateSubmission">
                <strong>Soumis le:</strong> {{ formatDate(dossierDetail.dossier.dateSubmission) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Forms Section -->
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Documents et Formulaires</h2>
          <p v-if="userRole === 'AGENT_ANTENNE'">Complétez tous les documents requis pour ce type de projet</p>
          <p v-else-if="userRole === 'AGENT_GUC'">Documents soumis par l'antenne - lecture seule</p>
        </div>

        <!-- Agent Antenne: Editable forms -->
        <div v-if="userRole === 'AGENT_ANTENNE'" class="forms-actions">
          <Button 
            label="Remplir les Documents" 
            icon="pi pi-file-edit" 
            @click="goToDocumentFilling"
            class="p-button-success p-button-lg"
            :disabled="!canEdit()"
          />
          <p class="forms-help">
            Cliquez pour accéder à l'interface de remplissage des documents et formulaires requis.
          </p>
        </div>

        <!-- Agent GUC: Read-only forms overview -->
        <div v-else-if="userRole === 'AGENT_GUC'" class="forms-readonly">
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
        <div v-if="dossierDetail.availableForms && dossierDetail.availableForms.length > 0" class="forms-overview">
          <h3>Aperçu des formulaires ({{ dossierDetail.availableForms.length }})</h3>
          <div class="forms-grid">
            <div 
              v-for="form in dossierDetail.availableForms" 
              :key="form.formId"
              class="form-card"
              :class="{ 
                'form-completed': form.isCompleted,
                'form-readonly': form.isReadOnly
              }"
            >
              <div class="form-info">
                <h4>{{ form.title }}</h4>
                <div class="form-tags">
                  <Tag v-if="form.isCompleted" value="Complété" severity="success" />
                  <Tag v-else value="En attente" severity="warning" />
                  <Tag v-if="form.isReadOnly" value="Lecture seule" severity="info" />
                </div>
              </div>
              <p v-if="form.description" class="form-description">{{ form.description }}</p>
              <div v-if="form.lastModified" class="form-meta">
                <small>Dernière modification: {{ formatDate(form.lastModified) }}</small>
              </div>
              <div v-if="userRole === 'AGENT_GUC' && form.isCompleted" class="form-actions">
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

      <!-- Files Section -->
      <div class="files-section component-card">
        <div class="section-header">
          <h3><i class="pi pi-file"></i> Documents téléchargés</h3>
          <div class="section-actions">
            <Button 
              v-if="userRole === 'AGENT_ANTENNE'"
              label="Gérer les documents" 
              icon="pi pi-external-link" 
              @click="goToDocumentFilling"
              :disabled="!canEdit()"
              class="p-button-outlined p-button-sm"
            />
            <Button 
              v-if="userRole === 'AGENT_GUC' && dossierDetail.pieceJointes.length > 0"
              label="Télécharger tout" 
              icon="pi pi-download" 
              @click="downloadAllDocuments"
              class="p-button-outlined p-button-sm"
            />
          </div>
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
                <i :class="getFileIcon(file.formatFichier)"></i>
              </div>
              <div class="file-details">
                <div class="file-name">{{ file.customTitle || file.nomFichier }}</div>
                <div class="file-meta">
                  <span>{{ file.typeDocument }}</span>
                  <span>•</span>
                  <span>{{ formatDate(file.dateUpload) }}</span>
                  <span>•</span>
                  <span>{{ formatFileSize(file.tailleFichier) }}</span>
                  <Tag v-if="file.isOriginalDocument" value="Original" severity="info" />
                </div>
                <div v-if="file.utilisateurNom" class="file-uploader">
                  Téléchargé par: {{ file.utilisateurNom }}
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
                v-if="canDeleteFile(file)"
                icon="pi pi-trash" 
                @click="confirmDeleteFile(file)"
                class="p-button-danger p-button-outlined p-button-sm"
                v-tooltip.top="'Supprimer'"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Notes Section -->
      <div class="notes-section component-card">
        <div class="section-header">
          <h3><i class="pi pi-comments"></i> Notes et Communications</h3>
          <Button 
            label="Ajouter une note" 
            icon="pi pi-plus" 
            @click="showAddNoteDialog"
            class="p-button-success p-button-sm"
          />
        </div>

        <div v-if="dossierDetail.notes.length === 0" class="no-notes">
          <i class="pi pi-comments"></i>
          <p>Aucune note pour ce dossier.</p>
        </div>

        <div v-else class="notes-list">
          <div 
            v-for="note in sortedNotes" 
            :key="note.id"
            class="note-item"
            :class="{ 
              'note-unread': !note.isRead,
              'note-priority': note.priorite === 'HAUTE'
            }"
          >
            <div class="note-header">
              <div class="note-info">
                <h4>{{ note.objet }}</h4>
                <div class="note-meta">
                  <span class="note-author">{{ note.utilisateurExpediteurNom }}</span>
                  <span>•</span>
                  <span class="note-date">{{ formatDate(note.dateCreation) }}</span>
                  <Tag 
                    v-if="note.priorite && note.priorite !== 'NORMALE'" 
                    :value="note.priorite" 
                    :severity="getNotePrioritySeverity(note.priorite)"
                    class="note-priority-tag"
                  />
                </div>
              </div>
              <div class="note-actions">
                <Button 
                  v-if="note.canReply"
                  icon="pi pi-reply" 
                  @click="replyToNote(note)"
                  class="p-button-text p-button-sm"
                  v-tooltip.top="'Répondre'"
                />
              </div>
            </div>
            <div class="note-content">
              <p>{{ note.contenu }}</p>
            </div>
            <div v-if="note.reponse" class="note-response">
              <div class="response-header">
                <strong>Réponse:</strong>
                <span class="response-date">{{ formatDate(note.dateReponse) }}</span>
              </div>
              <p>{{ note.reponse }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Workflow History -->
      <div v-if="dossierDetail.historiqueWorkflow && dossierDetail.historiqueWorkflow.length > 0" 
           class="workflow-history component-card">
        <h3><i class="pi pi-history"></i> Historique du traitement</h3>
        <div class="history-timeline">
          <div 
            v-for="(step, index) in dossierDetail.historiqueWorkflow" 
            :key="step.id"
            class="history-step"
            :class="{ 'current-step': index === 0 }"
          >
            <div class="step-indicator">
              <i v-if="index === 0" class="pi pi-clock"></i>
              <i v-else class="pi pi-check"></i>
            </div>
            <div class="step-content">
              <h4>{{ step.etapeDesignation }}</h4>
              <div class="step-meta">
                <span><i class="pi pi-map-marker"></i> {{ getEmplacementLabel(step.emplacementType) }}</span>
                <span><i class="pi pi-calendar"></i> {{ formatDate(step.dateEntree) }}</span>
                <span v-if="step.utilisateurNom"><i class="pi pi-user"></i> {{ step.utilisateurNom }}</span>
                <span v-if="step.dureeJours" class="step-duration">
                  <i class="pi pi-clock"></i> {{ step.dureeJours }} jour(s)
                </span>
              </div>
              <p v-if="step.commentaire" class="step-comment">{{ step.commentaire }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Validation Errors -->
      <div v-if="dossierDetail.validationErrors && dossierDetail.validationErrors.length > 0" 
           class="validation-errors component-card">
        <h3><i class="pi pi-exclamation-triangle"></i> Problèmes à résoudre</h3>
        <ul class="error-list">
          <li v-for="error in dossierDetail.validationErrors" :key="error">{{ error }}</li>
        </ul>
      </div>
    </div>

    <!-- Action Dialogs -->
    <ActionDialogs 
      :dialogs="actionDialogs"
      @action-confirmed="handleActionConfirmed"
      @dialog-closed="handleDialogClosed"
    />

    <!-- Add Note Dialog -->
    <AddNoteDialog 
      v-model:visible="addNoteDialog.visible"
      :dossier="addNoteDialog.dossier"
      @note-added="handleNoteAdded"
    />

    <!-- Form Data Viewer Dialog (GUC) -->
    <FormDataViewerDialog 
      v-model:visible="formDataDialog.visible"
      :form="formDataDialog.form"
      :dossier="dossierDetail?.dossier"
    />

    <!-- Schedule Terrain Visit Dialog (Commission) -->
    <ScheduleTerrainVisitDialog 
      v-model:visible="scheduleVisitDialog.visible"
      :dossier="scheduleVisitDialog.dossier"
      @visit-scheduled="handleVisitScheduled"
    />

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// Import components
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';
import FormDataViewerDialog from '@/components/dossiers/FormDataViewerDialog.vue';
import ScheduleTerrainVisitDialog from '@/components/agent_commission/ScheduleTerrainVisitDialog.vue';

// PrimeVue components
import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Tag from 'primevue/tag';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
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
const userRole = ref(AuthService.getCurrentUser()?.role || '');

// Dialogs
const actionDialogs = ref({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false },
  delete: { visible: false, dossier: null, loading: false, comment: '' },
  deleteFile: { visible: false, file: null, loading: false }
});

const addNoteDialog = ref({
  visible: false,
  dossier: null
});

const formDataDialog = ref({
  visible: false,
  form: null
});

const scheduleVisitDialog = ref({
  visible: false,
  dossier: null
});

// Computed properties
const completedFormsCount = computed(() => {
  return dossierDetail.value?.availableForms?.filter(f => f.isCompleted).length || 0;
});

const totalFormsCount = computed(() => {
  return dossierDetail.value?.availableForms?.length || 0;
});

const completionPercentage = computed(() => {
  if (totalFormsCount.value === 0) return 100;
  return (completedFormsCount.value / totalFormsCount.value) * 100;
});

const sortedNotes = computed(() => {
  if (!dossierDetail.value?.notes) return [];
  return [...dossierDetail.value.notes].sort((a, b) => 
    new Date(b.dateCreation) - new Date(a.dateCreation)
  );
});

// Permission methods
function canEdit() {
  const status = dossierDetail.value?.dossier?.statut;
  return userRole.value === 'AGENT_ANTENNE' && 
         (status === 'DRAFT' || status === 'Brouillon' || status === 'RETURNED_FOR_COMPLETION' || status === 'Retourné pour complétion');
}

function canDelete() {
  const status = dossierDetail.value?.dossier?.statut;
  return userRole.value === 'AGENT_ANTENNE' && 
         (status === 'DRAFT' || status === 'Brouillon');
}

function canDeleteFile(file) {
  return userRole.value === 'AGENT_ANTENNE' && canEdit();
}

function hasGUCActions() {
  if (userRole.value !== 'AGENT_GUC') return false;
  const status = dossierDetail.value?.dossier?.statut;
  return status === 'SUBMITTED' || status === 'Soumis au GUC' || status === 'IN_REVIEW' || status === 'En cours d\'examen';
}

function canScheduleTerrainVisit() {
  if (userRole.value !== 'AGENT_COMMISSION_TERRAIN') return false;
  const status = dossierDetail.value?.dossier?.statut;
  return status === 'SUBMITTED' || status === 'Soumis au GUC' || status === 'IN_REVIEW' || status === 'En cours d\'examen';
}

function canCompleteTerrainVisit() {
  if (userRole.value !== 'AGENT_COMMISSION_TERRAIN') return false;
  // Check if there's an existing terrain visit that can be completed
  return dossierDetail.value?.visitesTerrain?.some(v => v.dateVisite && !v.dateConstat);
}

function getGUCActionMenuItems() {
  const items = [];
  const status = dossierDetail.value?.dossier?.statut;
  
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

function getPrimaryGUCActionLabel() {
  const status = dossierDetail.value?.dossier?.statut;
  if (status === 'SUBMITTED' || status === 'Soumis au GUC') {
    return 'Commission';
  }
  return 'Actions';
}

function handlePrimaryGUCAction() {
  const status = dossierDetail.value?.dossier?.statut;
  if (status === 'SUBMITTED' || status === 'Soumis au GUC') {
    showActionDialog('sendToCommission');
  }
}

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
    
    const response = await ApiService.get(`/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
  } catch (err) {
    console.error('Error loading dossier detail:', err);
    error.value = err.message || 'Impossible de charger les détails du dossier';
  } finally {
    loading.value = false;
  }
}

function getBreadcrumbRoot() {
  switch (userRole.value) {
    case 'AGENT_ANTENNE':
      return 'Mes Dossiers';
    case 'AGENT_GUC':
      return 'Dossiers GUC';
    case 'AGENT_COMMISSION_TERRAIN':
      return 'Dossiers Commission';
    case 'ADMIN':
      return 'Tous les Dossiers';
    default:
      return 'Dossiers';
  }
}

function goBack() {
  switch (userRole.value) {
    case 'AGENT_ANTENNE':
      router.push('/agent_antenne/dossiers');
      break;
    case 'AGENT_GUC':
      router.push('/agent_guc/dossiers');
      break;
    case 'AGENT_COMMISSION_TERRAIN':
      router.push('/agent_commission/dossiers');
      break;
    default:
      router.push('/');
  }
}

function goToDocumentFilling() {
  router.push(`/agent_antenne/dossiers/documents/${dossierId.value}`);
}

function getWorkflowLocationText() {
  const emplacement = dossierDetail.value?.emplacementActuel;
  switch (emplacement) {
    case 'ANTENNE':
      return 'Antenne';
    case 'GUC':
      return 'Guichet Unique Central';
    case 'COMMISSION_AHA_AF':
      return 'Commission AHA-AF';
    case 'SERVICE_TECHNIQUE':
      return 'Service Technique';
    default:
      return 'Non défini';
  }
}

// Action methods
function confirmSendToGUC() {
  actionDialogs.value.sendToGUC = {
    visible: true,
    dossier: dossierDetail.value?.dossier,
    loading: false,
    comment: ''
  };
}

function confirmDelete() {
  actionDialogs.value.delete = {
    visible: true,
    dossier: dossierDetail.value?.dossier,
    loading: false,
    comment: ''
  };
}

function confirmDeleteFile(file) {
  actionDialogs.value.deleteFile = {
    visible: true,
    file: file,
    loading: false
  };
}

function showAddNoteDialog() {
  addNoteDialog.value = {
    visible: true,
    dossier: dossierDetail.value?.dossier
  };
}

function showActionDialog(action) {
  actionDialogs.value[action] = {
    visible: true,
    dossier: dossierDetail.value?.dossier,
    loading: false,
    comment: '',
    priority: 'NORMALE',
    reasons: [],
    definitive: false
  };
}

function showScheduleVisitDialog() {
  scheduleVisitDialog.value = {
    visible: true,
    dossier: dossierDetail.value?.dossier
  };
}

function showCompleteVisitDialog() {
  // Navigate to terrain visit completion - would need separate dialog
  console.log('Complete terrain visit for dossier:', dossierDetail.value?.dossier?.id);
  toast.add({
    severity: 'info',
    summary: 'Info',
    detail: 'Fonctionnalité de complétion en cours de développement',
    life: 3000
  });
}

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, file, data } = actionData;
    
    let endpoint = '';
    let payload = {};
    
    switch (action) {
      case 'sendToGUC':
        endpoint = `/dossiers/${dossier.id}/send-to-guc`;
        payload = { commentaire: data.comment };
        break;
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
      case 'delete':
        endpoint = `/dossiers/${dossier.id}`;
        payload = { motif: data.comment };
        break;
      case 'deleteFile':
        endpoint = `/dossiers/${dossierId.value}/documents/piece-jointe/${file.id}`;
        payload = {};
        break;
    }
    
    const method = (action === 'delete' || action === 'deleteFile') ? 'delete' : 'post';
    const response = await ApiService[method](endpoint, payload);
    
    if (response.success || action === 'deleteFile') {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Action effectuée avec succès',
        life: 4000
      });
      
      if (action === 'delete') {
        goBack();
      } else {
        await loadDossierDetail();
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
  Object.keys(actionDialogs.value).forEach(key => {
    actionDialogs.value[key].visible = false;
  });
}

async function handleNoteAdded() {
  addNoteDialog.value.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Note ajoutée avec succès',
    life: 3000
  });
  
  await loadDossierDetail();
}

async function handleVisitScheduled() {
  scheduleVisitDialog.value.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain programmée avec succès',
    life: 4000
  });
  
  await loadDossierDetail();
}

function viewFormData(form) {
  formDataDialog.value = {
    visible: true,
    form: form
  };
}

function replyToNote(note) {
  console.log('Reply to note:', note);
}

async function downloadFile(file) {
  try {
    const response = await fetch(`/api/dossiers/${dossierId.value}/documents/piece-jointe/${file.id}/download`, {
      headers: {
        'Authorization': `Bearer ${AuthService.getToken()}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
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

async function downloadAllDocuments() {
  try {
    const response = await fetch(`/api/dossiers/${dossierId.value}/documents/download-all`, {
      headers: {
        'Authorization': `Bearer ${AuthService.getToken()}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `dossier_${dossierDetail.value.dossier.reference}_documents.zip`);
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

// Utility functions
function getStatusSeverity(status) {
  const severityMap = {
    'Phase Antenne': 'info',
    'Phase GUC': 'warning',
    'Commission Technique': 'secondary',
    'Réalisation': 'success',
    'DRAFT': 'secondary',
    'Brouillon': 'secondary',
    'SUBMITTED': 'info',
    'Soumis au GUC': 'info',
    'IN_REVIEW': 'warning',
    'En cours d\'examen': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'RETURNED_FOR_COMPLETION': 'warning',
    'Retourné pour complétion': 'warning'
  };
  return severityMap[status] || 'info';
}

function getPrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
}

function getNotePrioritySeverity(priority) {
  return getPrioritySeverity(priority);
}

function getEmplacementLabel(emplacement) {
  const labels = {
    'ANTENNE': 'Antenne',
    'GUC': 'GUC',
    'COMMISSION_AHA_AF': 'Commission',
    'SERVICE_TECHNIQUE': 'Service Technique'
  };
  return labels[emplacement] || emplacement;
}

function getFileIcon(format) {
  const iconMap = {
    'pdf': 'pi pi-file-pdf',
    'doc': 'pi pi-file-word',
    'docx': 'pi pi-file-word',
    'xls': 'pi pi-file-excel',
    'xlsx': 'pi pi-file-excel',
    'jpg': 'pi pi-image',
    'jpeg': 'pi pi-image',
    'png': 'pi pi-image',
    'gif': 'pi pi-image'
  };
  return iconMap[format?.toLowerCase()] || 'pi pi-file';
}

function formatCurrency(amount) {
  if (!amount) return '0 DH';
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD'
  }).format(amount);
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
/* Copy all the styles from the original component - keeping the same styling */
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

/* Component Card */
.component-card {
  background: var(--background-color);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

/* Dossier Summary */
.dossier-summary {
  margin-bottom: 2rem;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.header-left h2 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
}

.status-info {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.priority-tag {
  font-size: 0.7rem !important;
}

.header-right {
  text-align: right;
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

.forms-actions {
  text-align: center;
  padding: 2rem;
  background: var(--section-background);
  border-radius: 12px;
  border: 2px dashed var(--primary-color);
  margin-bottom: 2rem;
}

.forms-actions .p-button-lg {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}

.forms-help {
  margin-top: 1rem;
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.4;
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
  background: var(--section-background);
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
  background: var(--section-background);
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

.section-actions {
  display: flex;
  gap: 0.5rem;
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
  font-size: 1.5rem;
}

.file-icon .pi-file-pdf {
  color: #dc2626;
}

.file-icon .pi-file-word {
  color: #2563eb;
}

.file-icon .pi-file-excel {
  color: #059669;
}

.file-icon .pi-image {
  color: #7c3aed;
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
  margin-bottom: 0.25rem;
}

.file-uploader {
  font-size: 0.75rem;
  color: #9ca3af;
}

.file-actions {
  display: flex;
  gap: 0.5rem;
}

/* Notes Section */
.notes-section {
  margin-bottom: 2rem;
}

.notes-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.no-notes {
  text-align: center;
  padding: 2rem;
  color: #6b7280;
}

.no-notes i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.note-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  transition: all 0.2s ease;
}

.note-item.note-unread {
  border-color: var(--info-color);
  background: rgba(59, 130, 246, 0.02);
}

.note-item.note-priority {
  border-color: var(--danger-color);
  background: rgba(239, 68, 68, 0.02);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.note-info h4 {
  margin: 0 0 0.25rem 0;
  color: #374151;
  font-size: 1rem;
}

.note-meta {
  font-size: 0.8rem;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.note-priority-tag {
  font-size: 0.7rem !important;
  padding: 0.125rem 0.375rem !important;
}

.note-actions {
  display: flex;
  gap: 0.25rem;
}

.note-content {
  color: #374151;
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.note-content p {
  margin: 0;
}

.note-response {
  background: #f3f4f6;
  border-radius: 6px;
  padding: 0.75rem;
  border-left: 3px solid var(--primary-color);
}

.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-size: 0.8rem;
}

.response-date {
  color: #6b7280;
}

.note-response p {
  margin: 0;
  color: #374151;
}

/* Workflow History */
.workflow-history {
  margin-bottom: 2rem;
}

.workflow-history h3 {
  color: var(--primary-color);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.history-timeline {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.history-step {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  position: relative;
}

.history-step:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 15px;
  top: 40px;
  width: 2px;
  height: calc(100% + 0.5rem);
  background: #e5e7eb;
}

.history-step.current-step::after {
  background: var(--primary-color);
}

.step-indicator {
  width: 32px;
  height: 32px;
  background: var(--primary-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.8rem;
  flex-shrink: 0;
  z-index: 1;
}

.history-step.current-step .step-indicator {
  background: var(--warning-color);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.step-content {
  flex: 1;
}

.step-content h4 {
  margin: 0 0 0.5rem 0;
  color: #374151;
  font-size: 1rem;
  font-weight: 600;
}

.step-meta {
  font-size: 0.8rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.step-meta span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.step-duration {
  color: var(--warning-color) !important;
  font-weight: 500;
}

.step-comment {
  font-size: 0.85rem;
  color: var();
  margin: 0;
  font-style: italic;
  background: var(--section-background);
  padding: 0.5rem;
  border-radius: 4px;
  border-left: 3px solid var(--primary-color);
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

/* Responsive Design */
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

  .files-section .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .notes-section .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .step-meta {
    flex-direction: column;
    gap: 0.5rem;
  }

  .completion-stats {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>