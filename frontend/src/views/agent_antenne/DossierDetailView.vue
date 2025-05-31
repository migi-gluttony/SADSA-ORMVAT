<template>
  <div class="dossier-detail-container">
    <UserInfoHeader 
      search-placeholder="Rechercher..."
      @search="handleSearch"
    />

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
            <span>Mes Dossiers</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ dossierDetail.dossier.reference }}</span>
          </div>
        </div>
        <div class="header-actions">
          <Button 
            v-if="dossierDetail.peutEtreEnvoye && canSendToGUC()"
            label="Envoyer au GUC" 
            icon="pi pi-send" 
            @click="confirmSendToGUC"
            class="p-button-success"
          />
          <Button 
            v-if="dossierDetail.peutEtreSupprime"
            label="Supprimer" 
            icon="pi pi-trash" 
            @click="confirmDelete"
            class="p-button-danger"
          />
        </div>
      </div>

      <!-- Dossier Summary -->
      <div class="dossier-summary component-card">
        <div class="summary-header">
          <h2>{{ dossierDetail.dossier.reference }}</h2>
          <Tag 
            :value="dossierDetail.dossier.statut" 
            :severity="getStatusSeverity(dossierDetail.dossier.statut)"
          />
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
            </div>
          </div>
          
          <div class="summary-section">
            <h4>Projet</h4>
            <div class="info-lines">
              <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
              <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
              <div><strong>CDA:</strong> {{ dossierDetail.dossier.cdaNom }}</div>
              <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>État du dossier</h4>
            <div class="info-lines">
              <div><strong>Statut:</strong> {{ dossierDetail.dossier.statut }}</div>
              <div class="time-info">
                <strong>Temps restant:</strong>
                <span :class="{
                  'time-critical': dossierDetail.joursRestants <= 1,
                  'time-warning': dossierDetail.joursRestants <= 2 && dossierDetail.joursRestants > 1,
                  'time-ok': dossierDetail.joursRestants > 2
                }">
                  {{ dossierDetail.joursRestants > 0 ? `${dossierDetail.joursRestants} jour(s)` : 'Dépassé' }}
                </span>
              </div>
              <div><strong>Modifiable:</strong> {{ dossierDetail.peutEtreModifie ? 'Oui' : 'Non' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Forms Section -->
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Documents et Formulaires</h2>
          <p>Complétez tous les documents requis pour ce type de projet</p>
        </div>

        <div class="forms-actions">
          <Button 
            label="Remplir les Documents" 
            icon="pi pi-file-edit" 
            @click="goToDocumentFilling"
            class="p-button-success p-button-lg"
            :disabled="!dossierDetail.peutEtreModifie"
          />
          <p class="forms-help">
            Cliquez pour accéder à l'interface de remplissage des documents et formulaires requis.
          </p>
        </div>

        <!-- Quick Forms Overview -->
        <div v-if="dossierDetail.availableForms && dossierDetail.availableForms.length > 0" class="forms-overview">
          <h3>Aperçu des formulaires ({{ dossierDetail.availableForms.length }})</h3>
          <div class="forms-grid">
            <div 
              v-for="form in dossierDetail.availableForms" 
              :key="form.formId"
              class="form-card"
              :class="{ 'form-completed': form.isCompleted }"
            >
              <div class="form-info">
                <h4>{{ form.title }}</h4>
                <Tag v-if="form.isCompleted" value="Complété" severity="success" />
                <Tag v-else value="En attente" severity="warning" />
              </div>
              <p v-if="form.description" class="form-description">{{ form.description }}</p>
              <div v-if="form.lastModified" class="form-meta">
                <small>Dernière modification: {{ formatDate(form.lastModified) }}</small>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Files Section -->
      <div class="files-section component-card">
        <div class="section-header">
          <h3><i class="pi pi-file"></i> Documents téléchargés</h3>
          <Button 
            label="Gérer les documents" 
            icon="pi pi-external-link" 
            @click="goToDocumentFilling"
            :disabled="!dossierDetail.peutEtreModifie"
            class="p-button-outlined p-button-sm"
          />
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
                <i class="pi pi-file-pdf"></i>
              </div>
              <div class="file-details">
                <div class="file-name">{{ file.customTitle || file.nomFichier }}</div>
                <div class="file-meta">
                  <span>{{ file.typeDocument }}</span>
                  <span>•</span>
                  <span>{{ formatDate(file.dateUpload) }}</span>
                  <Tag v-if="file.isOriginalDocument" value="Original" severity="info" />
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
            v-for="step in dossierDetail.historiqueWorkflow" 
            :key="step.id"
            class="history-step"
          >
            <div class="step-indicator"></div>
            <div class="step-content">
              <h4>{{ step.etapeDesignation }}</h4>
              <div class="step-meta">
                <span>{{ formatDate(step.dateEntree) }}</span>
                <span v-if="step.utilisateurNom">par {{ step.utilisateurNom }}</span>
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

    <!-- Send to GUC Confirmation Dialog -->
    <Dialog 
      v-model:visible="sendDialog.visible" 
      modal 
      header="Envoyer au GUC"
      :style="{ width: '500px' }"
    >
      <div class="send-confirmation">
        <i class="pi pi-send confirmation-icon"></i>
        <div class="confirmation-text">
          <p>Confirmer l'envoi de ce dossier au Guichet Unique Central ?</p>
          <div class="dossier-summary">
            <strong>{{ dossierDetail?.dossier?.reference }}</strong><br>
            {{ dossierDetail?.agriculteur?.prenom }} {{ dossierDetail?.agriculteur?.nom }}<br>
            <small>Type: {{ dossierDetail?.dossier?.sousRubriqueDesignation }}</small>
          </div>
          <p class="info-text">Une fois envoyé, le dossier ne pourra plus être modifié à l'antenne et sera traité par le GUC.</p>
        </div>
      </div>
      
      <div class="send-comment">
        <label for="sendComment">Commentaire pour le GUC (optionnel)</label>
        <Textarea 
          id="sendComment"
          v-model="sendDialog.comment" 
          rows="3" 
          placeholder="Commentaires ou instructions pour le GUC..."
        />
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="sendDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Envoyer au GUC" 
          icon="pi pi-send" 
          @click="sendDossierToGUC"
          class="p-button-success"
          :loading="sendDialog.loading"
        />
      </template>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog 
      v-model:visible="deleteConfirmDialog.visible" 
      modal 
      header="Confirmer la suppression"
      :style="{ width: '450px' }"
    >
      <div class="delete-confirmation">
        <i class="pi pi-exclamation-triangle warning-icon"></i>
        <div class="confirmation-text">
          <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
          <div class="dossier-summary">
            <strong>{{ dossierDetail?.dossier?.reference }}</strong><br>
            {{ dossierDetail?.agriculteur?.prenom }} {{ dossierDetail?.agriculteur?.nom }}
          </div>
          <p class="warning-text">Cette action est irréversible.</p>
        </div>
      </div>
      
      <div class="delete-comment">
        <label for="deleteComment">Motif de suppression (optionnel)</label>
        <Textarea 
          id="deleteComment"
          v-model="deleteConfirmDialog.comment" 
          rows="3" 
          placeholder="Raison de la suppression..."
        />
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="deleteConfirmDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Supprimer" 
          icon="pi pi-trash" 
          @click="deleteDossier"
          class="p-button-danger"
          :loading="deleteConfirmDialog.loading"
        />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import UserInfoHeader from '@/components/UserInfoHeader.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
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

// Send to GUC Dialog
const sendDialog = ref({
  visible: false,
  comment: '',
  loading: false
});

// Delete Confirmation Dialog
const deleteConfirmDialog = ref({
  visible: false,
  comment: '',
  loading: false
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
    
    const response = await ApiService.get(`/agent_antenne/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
  } catch (err) {
    console.error('Error loading dossier detail:', err);
    error.value = err.message || 'Impossible de charger les détails du dossier';
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/agent_antenne/dossiers');
}

function goToDocumentFilling() {
  router.push(`/agent_antenne/dossiers/documents/${dossierId.value}`);
}

function canSendToGUC() {
  if (!dossierDetail.value) return false;
  
  // Check if dossier can be sent to GUC
  // Must be in appropriate status and meet minimum requirements
  return dossierDetail.value.peutEtreEnvoye && 
         (dossierDetail.value.dossier.statut === 'Phase Antenne' || 
          dossierDetail.value.dossier.statut === 'DRAFT');
}

function confirmSendToGUC() {
  sendDialog.value = {
    visible: true,
    comment: '',
    loading: false
  };
}

async function sendDossierToGUC() {
  try {
    sendDialog.value.loading = true;
    
    const response = await ApiService.post(`/agent_antenne/dossiers/${dossierId.value}/send-to-guc`, {
      commentaire: sendDialog.value.comment
    });
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Dossier envoyé au GUC avec succès',
        life: 4000
      });
      
      sendDialog.value.visible = false;
      
      // Reload dossier detail to get updated status
      await loadDossierDetail();
    } else {
      throw new Error(response.message || 'Erreur lors de l\'envoi');
    }
    
  } catch (err) {
    console.error('Erreur lors de l\'envoi au GUC:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Impossible d\'envoyer le dossier au GUC',
      life: 4000
    });
  } finally {
    sendDialog.value.loading = false;
  }
}

function confirmDelete() {
  deleteConfirmDialog.value = {
    visible: true,
    comment: '',
    loading: false
  };
}

async function deleteDossier() {
  try {
    deleteConfirmDialog.value.loading = true;
    
    const response = await ApiService.delete(`/agent_antenne/dossiers/${dossierId.value}`, {
      motif: deleteConfirmDialog.value.comment
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier supprimé avec succès',
      life: 3000
    });
    
    // Navigate back to list
    goBack();
    
  } catch (err) {
    console.error('Error deleting dossier:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Impossible de supprimer le dossier',
      life: 4000
    });
  } finally {
    deleteConfirmDialog.value.loading = false;
    deleteConfirmDialog.value.visible = false;
  }
}

async function downloadFile(file) {
  try {
    const response = await fetch(`/api/agent_antenne/dossiers/${dossierId.value}/documents/piece-jointe/${file.id}/download`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Erreur de téléchargement');
    }
    
    // Create download link
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

function handleSearch(query) {
  console.log('Search:', query);
}

function getStatusSeverity(status) {
  const severityMap = {
    'Phase Antenne': 'info',
    'Phase GUC': 'warning',
    'Commission Technique': 'secondary',
    'Réalisation': 'success',
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success'
  };
  return severityMap[status] || 'info';
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
</script>

<style scoped>
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

/* Dossier Summary */
.dossier-summary {
  margin-bottom: 2rem;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.summary-header h2 {
  color: var(--primary-color);
  margin: 0;
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
  background: #f8f9fa;
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

.forms-overview {
  background: white;
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
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  transition: all 0.3s ease;
}

.form-card.form-completed {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.form-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.form-info h4 {
  margin: 0;
  color: #374151;
  font-size: 0.9rem;
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
  color: #dc2626;
  font-size: 1.5rem;
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
}

.file-actions {
  display: flex;
  gap: 0.5rem;
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
  gap: 1rem;
}

.history-step {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.step-indicator {
  width: 12px;
  height: 12px;
  background: var(--primary-color);
  border-radius: 50%;
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-content h4 {
  margin: 0 0 0.5rem 0;
  color: #374151;
  font-size: 0.9rem;
}

.step-meta {
  font-size: 0.8rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.step-comment {
  font-size: 0.85rem;
  color: #374151;
  margin: 0;
  font-style: italic;
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

/* Dialog Styles */
.send-confirmation,
.delete-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.warning-icon {
  color: #f59e0b;
  font-size: 2rem;
  margin-top: 0.25rem;
}

.confirmation-icon {
  color: var(--primary-color);
  font-size: 2rem;
  margin-top: 0.25rem;
}

.confirmation-text {
  flex: 1;
}

.dossier-summary {
  background: #f3f4f6;
  padding: 0.75rem;
  border-radius: 6px;
  margin: 0.75rem 0;
  font-size: 0.9rem;
}

.warning-text {
  color: #dc2626;
  font-weight: 500;
  font-size: 0.9rem;
  margin: 0;
}

.info-text {
  color: #6b7280;
  font-size: 0.9rem;
  margin: 0;
}

.send-comment,
.delete-comment {
  margin-top: 1rem;
}

.send-comment label,
.delete-comment label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
}

/* Dark Mode */
.dark-mode .detail-header,
.dark-mode .dossier-summary,
.dark-mode .forms-overview,
.dark-mode .file-item,
.dark-mode .files-section,
.dark-mode .workflow-history,
.dark-mode .validation-errors {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .summary-header {
  border-bottom-color: #374151;
}

.dark-mode .no-files {
  background-color: #374151;
}

.dark-mode .forms-actions {
  background-color: #374151;
}

.dark-mode .form-card {
  background-color: #374151;
  border-color: #4b5563;
}

.dark-mode .dossier-summary {
  background: #374151;
}

/* Responsive */
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
}
</style>