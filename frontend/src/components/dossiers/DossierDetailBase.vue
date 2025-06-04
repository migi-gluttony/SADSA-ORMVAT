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
            <span>{{ breadcrumbRoot }}</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ dossierDetail.dossier.reference }}</span>
          </div>
        </div>
        
        <!-- Role-specific header actions -->
        <div class="header-actions">
          <slot name="header-actions" :dossier="dossierDetail.dossier"></slot>
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
              <slot name="additional-status-tags" :dossier="dossierDetail.dossier"></slot>
            </div>
          </div>
          
          <div class="header-right">
            <slot name="summary-header-right" :dossier="dossierDetail.dossier"></slot>
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
              <slot name="additional-admin-info" :dossier="dossierDetail.dossier"></slot>
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

      <!-- Role-specific forms section -->
      <slot name="forms-section" :dossier="dossierDetail" :forms="dossierDetail.availableForms"></slot>

      <!-- Files Section -->
      <div class="files-section component-card">
        <div class="section-header">
          <h3><i class="pi pi-file"></i> Documents téléchargés</h3>
          <div class="section-actions">
            <slot name="files-actions" :files="dossierDetail.pieceJointes"></slot>
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
              <slot name="file-actions" :file="file"></slot>
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

    <!-- Add Note Dialog -->
    <AddNoteDialog 
      v-model:visible="addNoteDialog.visible"
      :dossier="addNoteDialog.dossier"
      @note-added="handleNoteAdded"
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
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import ProgressSpinner from 'primevue/progressspinner';
import Toast from 'primevue/toast';

// Props
const props = defineProps({
  breadcrumbRoot: {
    type: String,
    required: true
  },
  userRole: {
    type: String,
    default: () => AuthService.getCurrentUser()?.role || ''
  }
});

// Emits
const emit = defineEmits(['dossier-loaded', 'action-triggered']);

const router = useRouter();
const route = useRoute();
const toast = useToast();

// Props
const dossierId = computed(() => route.params.dossierId);

// State
const loading = ref(true);
const error = ref(null);
const dossierDetail = ref(null);

// Dialogs
const addNoteDialog = ref({
  visible: false,
  dossier: null
});

// Computed properties
const sortedNotes = computed(() => {
  if (!dossierDetail.value?.notes) return [];
  return [...dossierDetail.value.notes].sort((a, b) => 
    new Date(b.dateCreation) - new Date(a.dateCreation)
  );
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
    
    const response = await ApiService.get(`/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
    emit('dossier-loaded', response);
    
  } catch (err) {
    console.error('Error loading dossier detail:', err);
    error.value = err.message || 'Impossible de charger les détails du dossier';
  } finally {
    loading.value = false;
  }
}

function goBack() {
  switch (props.userRole) {
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

function showAddNoteDialog() {
  addNoteDialog.value = {
    visible: true,
    dossier: dossierDetail.value?.dossier
  };
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

function getNotePrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
}

function getEmplacementLabel(emplacement) {
  const labels = {
    'ANTENNE': 'Antenne',
    'GUC': 'GUC',
    'COMMISSION_AHA_AF': 'Commission Terrain',
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

// Expose methods for child components
defineExpose({
  loadDossierDetail,
  downloadFile
});
</script>
