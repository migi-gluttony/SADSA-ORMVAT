function showDossierDebugInfo(dossier) {
  const info = {
    'ID': dossier.id,
    'Référence': dossier.reference,
    'Statut': dossier.statut,
    'Étape Actuelle': dossier.etapeActuelle,
    'Emplacement': dossier.emplacementActuel,
    'Date Approbation': dossier.dateApprobation,
    'Needs Final Approval': needsFinalApproval(dossier),
    'Has Approved Fiche': hasApprovedFiche(dossier)
  };
  
  let message = 'Informations de Debug:\n\n';
  Object.entries(info).forEach(([key, value]) => {
    message += `${key}: ${value || 'null'}\n`;
  });
  
  alert(message);
}<template>
  <DossierListBase 
    :page-title="'Dossiers - Guichet Unique Central'" 
    :status-options="statusOptions"
    :user-role="'AGENT_GUC'"
    @dossier-selected="viewDossierDetail"
    ref="baseComponent"
  >
    <!-- Header Subtitle -->
    <template #header-subtitle>
      <span>- Tous les dossiers soumis</span>
    </template>

    <!-- Header Actions -->
    <template #header-actions>
      <Button 
        label="Actualiser" 
        icon="pi pi-refresh" 
        @click="refreshDossierList"
        class="p-button-outlined mr-2" 
      />
      <Button label="Export Excel" icon="pi pi-file-excel" @click="exportToExcel"
        class="p-button-outlined" />
    </template>

    <!-- Statistics -->
    <template #statistics="{ statistics }">
      <div v-if="statistics" class="statistics-section">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ statistics.totalDossiers }}</div>
            <div class="stat-label">Total dossiers</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.dossiersAttenteTraitement }}</div>
            <div class="stat-label">En attente traitement</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.dossiersApprouves }}</div>
            <div class="stat-label">Approuvés</div>
          </div>
          <div class="stat-card warning" v-if="statistics.dossiersEnRetard > 0">
            <div class="stat-value">{{ statistics.dossiersEnRetard }}</div>
            <div class="stat-label">En retard</div>
          </div>
        </div>
      </div>
    </template>

    <!-- Additional Filters -->
    <template #additional-filters>
      <!-- Antenne Filter -->
      <div class="filter-group">
        <label>ANTENNE</label>
        <Select v-model="filters.antenneId" :options="antenneOptions" optionLabel="designation" optionValue="id"
          placeholder="Toutes les antennes" @change="applyFilters" class="filter-select" :clearable="true" />
      </div>

      <!-- Priority Filter -->
      <div class="filter-group">
        <label>PRIORITÉ</label>
        <Select v-model="filters.priorite" :options="prioriteOptions" optionLabel="label" optionValue="value"
          placeholder="Toutes priorités" @change="applyFilters" class="filter-select" :clearable="true" />
      </div>
    </template>

    <!-- Table Headers -->
    <template #table-headers>
      <th class="col-antenne">Antenne</th>
      <th class="col-priority">Priorité</th>
    </template>

    <!-- Table Columns -->
    <template #table-columns="{ dossier }">
      <!-- Antenne Column -->
      <td class="col-antenne">
        <div class="antenne-cell">
          <div class="antenne-name">{{ dossier.antenneDesignation }}</div>
          <div class="cda-name">{{ dossier.cdaNom }}</div>
        </div>
      </td>

      <!-- Priority Column -->
      <td class="col-priority">
        <Tag :value="dossier.priorite || 'NORMALE'" :severity="getPrioritySeverity(dossier.priorite)"
          class="priority-tag" />
        <div v-if="dossier.notesCount > 0" class="notes-indicator">
          <i class="pi pi-comment"></i>
          <span>{{ dossier.notesCount }}</span>
        </div>
      </td>
    </template>

    <!-- Empty Message -->
    <template #empty-message="{ hasActiveFilters }">
      <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche et filtres.</p>
      <p v-else>Aucun dossier n'a encore été soumis au GUC.</p>
    </template>

    <!-- Dossier Actions -->
    <template #dossier-actions="{ dossier }">
      <Button icon="pi pi-comment" @click="showAddNoteDialog(dossier)"
        class="p-button-info p-button-sm action-btn" v-tooltip.top="'Ajouter note'" />

      <!-- DEBUG: Show dossier status info -->
      <Button 
        icon="pi pi-info-circle" 
        @click="showDossierDebugInfo(dossier)"
        class="p-button-help p-button-sm action-btn" 
        v-tooltip.top="'Debug Info'" 
      />

      <!-- Final Approval Button (for dossiers back from commission) -->
      <Button 
        v-if="needsFinalApproval(dossier)"
        icon="pi pi-gavel" 
        @click="goToFinalApproval(dossier)"
        class="p-button-success p-button-sm action-btn" 
        v-tooltip.top="'Décision Finale'" 
      />

      <!-- View Fiche Button (for approved dossiers) -->
      <Button 
        v-if="hasApprovedFiche(dossier)"
        icon="pi pi-file-text" 
        @click="goToFiche(dossier)"
        class="p-button-success p-button-sm action-btn" 
        v-tooltip.top="'Voir Fiche d\'Approbation'" 
      />

      <!-- Standard GUC Actions (send to commission, return, reject) -->
      <SplitButton v-if="dossier.availableActions && dossier.availableActions.length > 0"
        :model="getActionMenuItems(dossier)" class="p-button-sm action-split-btn"
        @click="handlePrimaryAction(dossier)">
        {{ getPrimaryActionLabel(dossier) }}
      </SplitButton>
    </template>
  </DossierListBase>

  <!-- Action Dialogs -->
  <ActionDialogs :dialogs="actionDialogs" @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed" />

  <!-- Add Note Dialog -->
  <AddNoteDialog v-model:visible="addNoteDialog.visible" :dossier="addNoteDialog.dossier"
    @note-added="handleNoteAdded" />
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierListBase from '@/components/dossiers/DossierListBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Select from 'primevue/select';
import Tag from 'primevue/tag';

const router = useRouter();
const toast = useToast();
const baseComponent = ref();

// Status options for GUC
const statusOptions = [
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
];

// Additional filters for GUC
const filters = reactive({
  antenneId: null,
  priorite: null
});

const antenneOptions = ref([
  { id: 1, designation: 'Antenne Bni Amir' },
  { id: 2, designation: 'Antenne Souk Sebt' },
  { id: 3, designation: 'Antenne Ouldad M\'Bark' },
  { id: 4, designation: 'Antenne Dar Ouled Zidouh' },
  { id: 5, designation: 'Antenne Afourer' }
]);

const prioriteOptions = ref([
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

// Dialogs
const actionDialogs = reactive({
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false }
});

const addNoteDialog = reactive({
  visible: false,
  dossier: null
});

// Methods
function viewDossierDetail(dossierId) {
  router.push(`/agent_guc/dossiers/${dossierId}`);
}

function applyFilters() {
  if (baseComponent.value) {
    baseComponent.value.applyFilters();
  }
}

function showAddNoteDialog(dossier) {
  addNoteDialog.visible = true;
  addNoteDialog.dossier = dossier;
}

function debugNeedsFinalApproval(dossier) {
  // Debug function: show button for any dossier that could potentially need final approval
  // This helps us see all candidates and debug the real conditions
  const couldNeedApproval = (
    // Not yet approved
    dossier.statut !== 'APPROVED' && 
    dossier.statut !== 'REJECTED' &&
    // Contains GUC or commission keywords
    (dossier.etapeActuelle?.includes('GUC') || 
     dossier.etapeActuelle?.includes('Commission') ||
     dossier.statut?.includes('Commission') ||
     // Or is in review/submitted state
     dossier.statut === 'IN_REVIEW' ||
     dossier.statut === 'SUBMITTED')
  );
  
  if (couldNeedApproval) {
    console.log('DEBUG: Potential candidate for final approval:', {
      id: dossier.id,
      reference: dossier.reference,
      etapeActuelle: dossier.etapeActuelle,
      statut: dossier.statut,
      emplacementActuel: dossier.emplacementActuel
    });
  }
  
  return couldNeedApproval;
}

function needsFinalApproval(dossier) {
  // Debug logging to see actual dossier state
  console.log('Checking needsFinalApproval for dossier:', {
    id: dossier.id,
    reference: dossier.reference,
    etapeActuelle: dossier.etapeActuelle,
    statut: dossier.statut,
    emplacementActuel: dossier.emplacementActuel,
    dateApprobation: dossier.dateApprobation,
    hasApprovedFiche: hasApprovedFiche(dossier)
  });

  // First check: if already approved, don't show final approval button
  if (hasApprovedFiche(dossier)) {
    console.log('Dossier already approved, no final approval needed');
    return false;
  }

  // Check if dossier is back from commission and needs final GUC approval
  // More flexible conditions to catch different possible states
  const isInFinalGUCPhase = dossier.etapeActuelle && 
    (dossier.etapeActuelle.includes('AP - Phase GUC') || 
     dossier.etapeActuelle.includes('GUC'));
  
  const needsApproval = dossier.statut === 'IN_REVIEW' || 
                       dossier.statut === 'SUBMITTED' ||
                       dossier.statut === 'Commission Terrain Approuvé' ||
                       (dossier.etapeActuelle && dossier.etapeActuelle.includes('GUC'));
  
  const notYetApproved = !hasApprovedFiche(dossier);
  
  const result = isInFinalGUCPhase && needsApproval && notYetApproved;
  
  console.log('needsFinalApproval result:', result, {
    isInFinalGUCPhase,
    needsApproval,
    notYetApproved
  });
  
  return result;
}

function hasApprovedFiche(dossier) {
  // Check if dossier is approved and has a fiche (dateApprobation exists)
  const isApproved = dossier.statut === 'APPROVED';
  const hasApprovalDate = dossier.dateApprobation && dossier.dateApprobation !== null && dossier.dateApprobation !== undefined;
  
  // Also check if the workflow shows completion or realization phase
  const isInApprovedState = dossier.etapeActuelle && 
    (dossier.etapeActuelle.includes('Approuvé') || 
     dossier.etapeActuelle.includes('APPROVED') ||
     dossier.etapeActuelle.includes('terminé') ||
     dossier.etapeActuelle.includes('Terminé') ||
     dossier.etapeActuelle.includes('RP -') || // Realization phase
     dossier.etapeActuelle.includes('Réalisation'));
  
  // Check if status indicates completion
  const hasCompletedStatus = dossier.statut === 'COMPLETED' || 
                            dossier.statut === 'PENDING_CORRECTION' ||
                            dossier.statut === 'REALIZED';
  
  const result = isApproved || hasApprovalDate || isInApprovedState || hasCompletedStatus;
  
  console.log('hasApprovedFiche check:', {
    id: dossier.id,
    reference: dossier.reference,
    statut: dossier.statut,
    dateApprobation: dossier.dateApprobation,
    etapeActuelle: dossier.etapeActuelle,
    isApproved,
    hasApprovalDate,
    isInApprovedState,
    hasCompletedStatus,
    result
  });
  
  return result;
}

function goToFinalApproval(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/final-approval`);
}

function goToFiche(dossier) {
  console.log('Navigating to fiche for dossier:', dossier.id);
  router.push(`/agent_guc/dossiers/${dossier.id}/fiche`);
}

// Add method to refresh list manually
function refreshDossierList() {
  console.log('Manually refreshing dossier list...');
  if (baseComponent.value) {
    baseComponent.value.loadDossiers();
  }
}

// Auto-refresh every 30 seconds to catch status updates
onMounted(() => {
  // Set up auto-refresh interval
  const refreshInterval = setInterval(() => {
    if (baseComponent.value) {
      console.log('Auto-refreshing dossier list...');
      baseComponent.value.loadDossiers();
    }
  }, 30000); // 30 seconds

  // Clean up interval on unmount
  onUnmounted(() => {
    clearInterval(refreshInterval);
  });
});

function getActionMenuItems(dossier) {
  const items = [];

  if (dossier.availableActions?.includes('send_to_commission')) {
    items.push({
      label: 'Envoyer à la Commission',
      icon: 'pi pi-forward',
      command: () => showActionDialog('sendToCommission', dossier)
    });
  }

  if (dossier.availableActions?.includes('return_to_antenne')) {
    items.push({
      label: 'Retourner à l\'Antenne',
      icon: 'pi pi-undo',
      command: () => showActionDialog('returnToAntenne', dossier)
    });
  }

  if (dossier.availableActions?.includes('reject')) {
    items.push({
      label: 'Rejeter',
      icon: 'pi pi-times-circle',
      command: () => showActionDialog('reject', dossier)
    });
  }

  return items;
}

function getPrimaryActionLabel(dossier) {
  if (dossier.availableActions?.includes('send_to_commission')) {
    return 'Commission';
  }
  return 'Actions';
}

function handlePrimaryAction(dossier) {
  if (dossier.availableActions?.includes('send_to_commission')) {
    showActionDialog('sendToCommission', dossier);
  }
}

function showActionDialog(action, dossier) {
  actionDialogs[action] = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: '',
    priority: 'NORMALE',
    reasons: [],
    definitive: false
  };
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
        life: 3000
      });

      // Immediately refresh to show updated buttons
      setTimeout(() => {
        refreshDossierList();
      }, 500);
    }

  } catch (err) {
    console.error(`Erreur lors de l'action ${actionData.action}:`, err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    const dialog = actionDialogs[actionData.action];
    if (dialog) {
      dialog.loading = false;
    }
  }
}

function handleDialogClosed() {
  Object.keys(actionDialogs).forEach(key => {
    actionDialogs[key].visible = false;
  });
}

async function handleNoteAdded() {
  addNoteDialog.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Note ajoutée avec succès',
    life: 3000
  });

  // Refresh list to show updated note count
  setTimeout(() => {
    refreshDossierList();
  }, 500);
}

async function exportToExcel() {
  try {
    const response = await fetch('/api/dossiers/export', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${AuthService.getToken()}`
      },
      body: JSON.stringify(filters)
    });

    if (!response.ok) throw new Error('Erreur d\'export');

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `dossiers_${new Date().toISOString().split('T')[0]}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Export Excel généré avec succès',
      life: 3000
    });

  } catch (err) {
    console.error('Erreur lors de l\'export:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'exporter les dossiers',
      life: 3000
    });
  }
}

function getPrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
}
</script>
<style scoped>
/* Statistics Section */
.statistics-section {
  margin-bottom: 1.5rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.stat-card {
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  padding: 1.5rem;
  text-align: center;
  box-shadow: var(--shadow-sm);
}

.stat-card.warning {
  border-color: var(--warning-color);
  background: rgba(245, 158, 11, 0.05);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

.stat-card.warning .stat-value {
  color: var(--warning-color);
}

.stat-label {
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
}

/* Column Specific Styles */
.col-antenne {
  width: 15%;
  min-width: 150px;
}

.col-priority {
  width: 10%;
  min-width: 100px;
}

.antenne-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.antenne-name {
  font-weight: 500;
  color: var(--text-color);
  font-size: 0.875rem;
}

.cda-name {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.priority-tag {
  font-size: 0.7rem !important;
  font-weight: 600 !important;
  padding: 0.25rem 0.5rem !important;
  border-radius: var(--border-radius-sm) !important;
  margin-bottom: 0.25rem !important;
  display: block !important;
}

.notes-indicator {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--info-color);
}

.action-split-btn {
  height: 2.5rem !important;
}

:deep(.action-split-btn .p-button) {
  padding: 0.5rem 0.75rem !important;
  font-size: 0.75rem !important;
}

/* Adjust filters grid for additional filters */
:deep(.filters-grid) {
  grid-template-columns: 1fr 150px 200px 150px 150px auto;
}

/* Responsive Design */
@media (max-width: 1200px) {
  :deep(.filters-grid) {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>