<template>
  <DossierListBase :page-title="'Mes Dossiers'" :status-options="statusOptions" :user-role="'AGENT_ANTENNE'"
    @dossier-selected="viewDossierDetail">
    <!-- Header Subtitle -->
    <template #header-subtitle>
      <span v-if="dossiersData?.currentUserCDA">
        - CDA: {{ dossiersData.currentUserCDA }}
      </span>
    </template>

    <!-- Header Actions -->
    <template #header-actions>
      <Button label="Nouveau Dossier" icon="pi pi-plus" @click="navigateToCreate" class="p-button-success" />
    </template>

    <!-- Statistics -->
    <template #statistics="{ statistics }">
      <div v-if="statistics" class="grid">
        <div class="col-12 md:col-3">
          <Card>
            <template #content>
              <div class="text-center">
                <div class="text-3xl font-bold text-primary">{{ statistics.totalDossiers }}</div>
                <div class="text-color-secondary">Total dossiers</div>
              </div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3">
          <Card>
            <template #content>
              <div class="text-center">
                <div class="text-3xl font-bold text-blue-500">{{ statistics.dossiersEnCours }}</div>
                <div class="text-color-secondary">En cours</div>
              </div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3">
          <Card>
            <template #content>
              <div class="text-center">
                <div class="text-3xl font-bold text-green-500">{{ statistics.dossiersApprouves }}</div>
                <div class="text-color-secondary">Approuvés</div>
              </div>
            </template>
          </Card>
        </div>
        <div class="col-12 md:col-3" v-if="statistics.dossiersEnRetard > 0">
          <Card>
            <template #content>
              <div class="text-center">
                <div class="text-3xl font-bold text-orange-500">{{ statistics.dossiersEnRetard }}</div>
                <div class="text-color-secondary">En retard</div>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </template>

    <!-- Empty Message -->
    <template #empty-message="{ hasActiveFilters }">
      <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche.</p>
      <p v-else>Vous n'avez pas encore créé de dossier.</p>
    </template>

    <!-- Empty Actions -->
    <template #empty-actions>
      <Button label="Créer mon premier dossier" icon="pi pi-plus" @click="navigateToCreate" class="p-button-success" />
    </template>

    <!-- Dossier Actions -->
    <template #dossier-actions="{ dossier }">
      <!-- Phase 1: Edit/Continue -->
      <Button v-if="isPhase1(dossier) && canEdit(dossier)" icon="pi pi-pencil" @click="continueDossier(dossier.id)"
        class="p-button-outlined" size="small" v-tooltip.top="'Continuer'" />

      <!-- Phase 1: Send to GUC -->
      <Button v-if="isPhase1(dossier) && canSendToGUC(dossier)" icon="pi pi-send" @click="confirmSendToGUC(dossier)"
        class="p-button-success" size="small" v-tooltip.top="'Envoyer au GUC'" />

      <!-- Phase 1: Delete -->
      <Button v-if="isPhase1(dossier) && canDelete(dossier)" icon="pi pi-trash" @click="confirmDeleteDossier(dossier)"
        class="p-button-danger p-button-outlined" size="small" v-tooltip.top="'Supprimer'" />

      <!-- Approved: Initialize Realization -->
      <Button v-if="canInitializeRealization(dossier)" icon="pi pi-play" @click="confirmInitializeRealization(dossier)"
        class="p-button-info" size="small" v-tooltip.top="'Initialiser Réalisation'" />
    </template>
  </DossierListBase>

  <!-- Action Dialogs -->
  <ActionDialogs :dialogs="actionDialogs" @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed" />
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierListBase from '@/components/dossiers/DossierListBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import Card from 'primevue/card';

const router = useRouter();
const toast = useToast();

const dossiersData = ref(null);

const statusOptions = [
  { label: 'Brouillon', value: 'DRAFT' },
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Retourné pour complétion', value: 'RETURNED_FOR_COMPLETION' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Approuvé - En attente fermier', value: 'APPROVED_AWAITING_FARMER' },
  { label: 'Réalisation en cours', value: 'REALIZATION_IN_PROGRESS' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
];

const actionDialogs = reactive({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  initializeRealization: { visible: false, dossier: null, loading: false },
  delete: { visible: false, dossier: null, loading: false, comment: '' }
});

// Permission methods
function isPhase1(dossier) {
  return dossier.etapeActuelle?.includes('AP - Phase Antenne');
}

function canEdit(dossier) {
  return dossier.statut === 'DRAFT' || dossier.statut === 'RETURNED_FOR_COMPLETION';
}

function canSendToGUC(dossier) {
  return canEdit(dossier) && (dossier.completionPercentage || 0) >= 100;
}

function canDelete(dossier) {
  return dossier.statut === 'DRAFT';
}

function canInitializeRealization(dossier) {
  return dossier.statut === 'APPROVED_AWAITING_FARMER';
}

// Navigation methods
function navigateToCreate() {
  router.push('/agent_antenne/dossiers/create');
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}`);
}

function continueDossier(dossierId) {
  router.push(`/agent_antenne/dossiers/documents/${dossierId}`);
}

// Action confirmations
function confirmSendToGUC(dossier) {
  actionDialogs.sendToGUC = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmInitializeRealization(dossier) {
  actionDialogs.initializeRealization = {
    visible: true,
    dossier: dossier,
    loading: false
  };
}

function confirmDeleteDossier(dossier) {
  actionDialogs.delete = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, data } = actionData;

    let endpoint = '';
    let payload = {};
    let method = 'post';

    switch (action) {
      case 'sendToGUC':
        endpoint = `/dossiers/${dossier.id}/send-to-guc`;
        payload = { commentaire: data.comment };
        break;
      case 'initializeRealization':
        endpoint = `/dossiers/${dossier.id}/initialize-realization`;
        payload = {};
        break;
      case 'delete':
        endpoint = `/dossiers/${dossier.id}`;
        payload = { motif: data.comment };
        method = 'delete';
        break;
    }

    const response = await ApiService[method](endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });

      // Reload the list
      window.location.reload();
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
</script>
