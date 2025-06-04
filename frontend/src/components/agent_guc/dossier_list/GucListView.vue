<template>
  <DossierListBase 
    :page-title="'Dossiers - Guichet Unique Central'" 
    :status-options="statusOptions"
    :user-role="'AGENT_GUC'"
    @dossier-selected="viewDossierDetail"
    ref="baseComponent"
  >
    <template #header-subtitle>
      <span>- Tous les dossiers soumis</span>
    </template>

    <template #header-actions>
      <Button 
        label="Actualiser" 
        icon="pi pi-refresh" 
        @click="refreshDossierList"
        outlined
      />
      <Button 
        label="Export Excel" 
        icon="pi pi-file-excel" 
        @click="exportToExcel"
        outlined
      />
    </template>

    <template #statistics="{ statistics }">
      <div v-if="statistics" class="grid">
        <div class="col-12 md:col-3">
          <Card class="text-center">
            <template #content>
              <div class="text-2xl font-bold text-primary">{{ statistics.totalDossiers }}</div>
              <div class="text-600">Total dossiers</div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3">
          <Card class="text-center">
            <template #content>
              <div class="text-2xl font-bold text-orange-500">{{ statistics.dossiersAttenteTraitement }}</div>
              <div class="text-600">En attente traitement</div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3">
          <Card class="text-center">
            <template #content>
              <div class="text-2xl font-bold text-green-500">{{ statistics.dossiersApprouves }}</div>
              <div class="text-600">Approuvés</div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3">
          <Card class="text-center bg-red-50" v-if="statistics.dossiersEnRetard > 0">
            <template #content>
              <div class="text-2xl font-bold text-red-500">{{ statistics.dossiersEnRetard }}</div>
              <div class="text-600">En retard</div>
            </template>
          </Card>
        </div>
      </div>
    </template>

    <template #additional-filters>
      <div class="col-12 md:col-6 lg:col-3">
        <label class="block text-900 font-medium mb-2">ANTENNE</label>
        <Select 
          v-model="filters.antenneId" 
          :options="antenneOptions" 
          optionLabel="designation" 
          optionValue="id"
          placeholder="Toutes les antennes" 
          @change="applyFilters" 
          class="w-full"
          showClear
        />
      </div>
      <div class="col-12 md:col-6 lg:col-3">
        <label class="block text-900 font-medium mb-2">PRIORITÉ</label>
        <Select 
          v-model="filters.priorite" 
          :options="prioriteOptions" 
          optionLabel="label" 
          optionValue="value"
          placeholder="Toutes priorités" 
          @change="applyFilters" 
          class="w-full"
          showClear
        />
      </div>
    </template>

    <template #table-headers>
      <th>Antenne</th>
      <th>Phase</th>
    </template>

    <template #table-columns="{ dossier }">
      <td>
        <div class="flex flex-column gap-1">
          <div class="font-medium">{{ dossier.antenneDesignation }}</div>
          <small class="text-600">{{ dossier.cdaNom }}</small>
        </div>
      </td>
      <td>
        <div class="flex align-items-center gap-2">
          <Tag :value="getPhaseLabel(dossier)" :severity="getPhaseSeverity(dossier)" />
          <div v-if="dossier.notesCount > 0" class="flex align-items-center gap-1 text-600">
            <i class="pi pi-comment text-xs"></i>
            <span class="text-xs">{{ dossier.notesCount }}</span>
          </div>
        </div>
      </td>
    </template>

    <template #empty-message="{ hasActiveFilters }">
      <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche et filtres.</p>
      <p v-else>Aucun dossier n'a encore été soumis au GUC.</p>
    </template>

    <template #dossier-actions="{ dossier }">
      <Button 
        icon="pi pi-comment" 
        @click="showAddNoteDialog(dossier)"
        severity="info" 
        size="small" 
        outlined
        v-tooltip.top="'Ajouter note'" 
      />

      <!-- Final Approval Button (Phase 4) -->
      <Button 
        v-if="needsFinalApproval(dossier)"
        icon="pi pi-gavel" 
        @click="goToFinalApproval(dossier)"
        severity="success" 
        size="small"
        v-tooltip.top="'Décision Finale'" 
      />

      <!-- View Fiche Button (approved dossiers) -->
      <Button 
        v-if="hasApprovedFiche(dossier)"
        icon="pi pi-file-text" 
        @click="goToFiche(dossier)"
        severity="success" 
        size="small"
        v-tooltip.top="'Voir Fiche d\'Approbation'" 
      />

      <!-- GUC Actions for Phase 2 and 6 -->
      <SplitButton 
        v-if="hasGUCActions(dossier)"
        :model="getActionMenuItems(dossier)" 
        size="small"
        @click="handlePrimaryAction(dossier)"
      >
        {{ getPrimaryActionLabel(dossier) }}
      </SplitButton>
    </template>
  </DossierListBase>

  <ActionDialogs 
    :dialogs="actionDialogs" 
    @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed" 
  />

  <AddNoteDialog 
    v-model:visible="addNoteDialog.visible" 
    :dossier="addNoteDialog.dossier"
    @note-added="handleNoteAdded" 
  />
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierListBase from '@/components/dossiers/DossierListBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Select from 'primevue/select';
import Tag from 'primevue/tag';
import Card from 'primevue/card';

const router = useRouter();
const toast = useToast();
const baseComponent = ref();

const statusOptions = [
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'En attente fermier', value: 'APPROVED_AWAITING_FARMER' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
];

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

const actionDialogs = reactive({
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false }
});

const addNoteDialog = reactive({
  visible: false,
  dossier: null
});

function getPhaseNumber(dossier) {
  // Use direct phase number from backend if available
  if (dossier.phaseNumber) {
    return dossier.phaseNumber;
  }
  
  // Use etape ID mapping if available
  if (dossier.etapeId) {
    const phaseMap = {
      1: 1, // AP - Phase Antenne
      2: 2, // AP - Phase GUC (Initial)
      3: 3, // AP - Phase AHA-AF
      4: 4, // AP - Phase GUC (Final)
      5: 5, // RP - Phase Antenne
      6: 6, // RP - Phase GUC
      7: 7, // RP - Phase service technique
      8: 8  // RP - Phase GUC (Final)
    };
    return phaseMap[dossier.etapeId] || 1;
  }
  
  // Fallback to text parsing (legacy)
  const etape = dossier.etapeActuelle;
  if (!etape) return 1;
  
  const phaseMatch = etape.match(/\(Phase (\d+):/);
  if (phaseMatch) {
    return parseInt(phaseMatch[1]);
  }
  
  return 1;
}

function getPhaseLabel(dossier) {
  const phase = getPhaseNumber(dossier);
  const phaseLabels = {
    1: 'Phase 1: Antenne',
    2: 'Phase 2: GUC Initial',
    3: 'Phase 3: Commission',
    4: 'Phase 4: GUC Final',
    5: 'Phase 5: Réalisation',
    6: 'Phase 6: GUC Réalisation',
    7: 'Phase 7: Service Tech.',
    8: 'Phase 8: GUC Final'
  };
  return phaseLabels[phase] || 'Phase inconnue';
}

function getPhaseSeverity(dossier) {
  const phase = getPhaseNumber(dossier);
  if (phase === 2 || phase === 4 || phase === 6 || phase === 8) return 'warning'; // GUC phases
  if (phase === 3 || phase === 7) return 'info'; // Commission/Service technique
  if (phase === 5) return 'secondary'; // Antenne realization
  return 'secondary';
}

function needsFinalApproval(dossier) {
  const phase = getPhaseNumber(dossier);
  const isPhase4 = phase === 4;
  const notYetApproved = !hasApprovedFiche(dossier);
  
  console.log('needsFinalApproval:', dossier.reference, 'phase:', phase, 'isPhase4:', isPhase4, 'notYetApproved:', notYetApproved, 'result:', isPhase4 && notYetApproved);
  return isPhase4 && notYetApproved;
}

function hasApprovedFiche(dossier) {
  const result = dossier.statut === 'APPROVED' || 
         dossier.statut === 'APPROVED_AWAITING_FARMER' ||
         dossier.statut === 'COMPLETED' ||
         !!dossier.dateApprobation;
  
  console.log('hasApprovedFiche:', dossier.reference, 'statut:', dossier.statut, 'dateApprobation:', dossier.dateApprobation, 'result:', result);
  return result;
}

function hasGUCActions(dossier) {
  const phase = getPhaseNumber(dossier);
  const result = (phase === 2 || phase === 6) && !hasApprovedFiche(dossier);
  
  console.log('hasGUCActions:', dossier.reference, 'phase:', phase, 'result:', result);
  return result;
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_guc/dossiers/${dossierId}`);
}

function applyFilters() {
  baseComponent.value?.applyFilters();
}

function showAddNoteDialog(dossier) {
  addNoteDialog.visible = true;
  addNoteDialog.dossier = dossier;
}

function goToFinalApproval(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/final-approval`);
}

function goToFiche(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/fiche`);
}

function refreshDossierList() {
  baseComponent.value?.loadDossiers();
}

// Auto-refresh every 30 seconds
onMounted(() => {
  const refreshInterval = setInterval(() => {
    baseComponent.value?.loadDossiers();
  }, 30000);

  onUnmounted(() => {
    clearInterval(refreshInterval);
  });
});

function getActionMenuItems(dossier) {
  const items = [];
  const phase = getPhaseNumber(dossier);

  if (phase === 2) {
    items.push({
      label: 'Envoyer à la Commission',
      icon: 'pi pi-forward',
      command: () => showActionDialog('sendToCommission', dossier)
    });
    items.push({
      label: 'Retourner à l\'Antenne',
      icon: 'pi pi-undo',
      command: () => showActionDialog('returnToAntenne', dossier)
    });
    items.push({
      label: 'Rejeter',
      icon: 'pi pi-times-circle',
      command: () => showActionDialog('reject', dossier)
    });
  } else if (phase === 6) {
    items.push({
      label: 'Envoyer au Service Technique',
      icon: 'pi pi-forward',
      command: () => processRealizationReview(dossier)
    });
  }

  return items;
}

function getPrimaryActionLabel(dossier) {
  const phase = getPhaseNumber(dossier);
  if (phase === 2) return 'Commission';
  if (phase === 6) return 'Service Technique';
  return 'Actions';
}

function handlePrimaryAction(dossier) {
  const phase = getPhaseNumber(dossier);
  if (phase === 2) {
    showActionDialog('sendToCommission', dossier);
  } else if (phase === 6) {
    processRealizationReview(dossier);
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

async function processRealizationReview(dossier) {
  try {
    const response = await ApiService.post(`/dossiers/${dossier.id}/process-realization-review`, {
      commentaire: 'Révision réalisation approuvée'
    });

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Dossier envoyé au Service Technique',
        life: 3000
      });
      refreshDossierList();
    }
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
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
        life: 3000
      });
      setTimeout(() => refreshDossierList(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    const dialog = actionDialogs[actionData.action];
    if (dialog) dialog.loading = false;
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
  setTimeout(() => refreshDossierList(), 500);
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
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'exporter les dossiers',
      life: 3000
    });
  }
}
</script>