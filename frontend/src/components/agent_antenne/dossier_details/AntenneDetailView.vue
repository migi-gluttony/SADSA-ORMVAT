<template>
  <DossierDetailBase :breadcrumb-root="'Mes Dossiers'" :user-role="'AGENT_ANTENNE'"
    @dossier-loaded="handleDossierLoaded" ref="baseComponent">
    <!-- Header Actions -->
    <template #header-actions="{ dossier }">
      <Button v-if="canSendToGUC(dossier)" label="Envoyer au GUC" icon="pi pi-send" @click="confirmSendToGUC(dossier)"
        class="p-button-success" />
      <Button v-if="canInitializeRealization(dossier)" label="Initialiser Réalisation" icon="pi pi-play"
        @click="confirmInitializeRealization(dossier)" class="p-button-info" />
      <Button v-if="canEdit(dossier)" label="Modifier" icon="pi pi-pencil" @click="goToEdit(dossier)"
        class="p-button-warning" />
      <Button v-if="canDelete(dossier)" label="Supprimer" icon="pi pi-trash" @click="confirmDelete(dossier)"
        class="p-button-danger" />
    </template>

    <!-- Forms Section -->
    <template #forms-section="{ dossier }">
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-file-edit"></i> Documents et Formulaires
        </template>
        <template #content>
          <div v-if="canEdit(dossier.dossier)" class="text-center p-4">
            <Button label="Remplir les Documents" icon="pi pi-file-edit" @click="goToDocumentFilling"
              class="p-button-success p-button-lg" size="large" />
            <p class="mt-3 text-sm text-color-secondary">
              Complétez tous les documents requis pour ce type de projet
            </p>
          </div>
          <div v-else class="text-center p-4 surface-50 border-round">
            <i class="pi pi-lock text-4xl text-color-secondary mb-3"></i>
            <p class="text-color-secondary">Documents en lecture seule à cette étape</p>
            <Button label="Voir les documents" icon="pi pi-eye" @click="goToDocumentFilling" class="p-button-outlined"
              size="small" />
          </div>
        </template>
      </Card>
    </template>

    <!-- Files Actions -->
    <template #files-actions="{ files }">
      <Button label="Gérer les documents" icon="pi pi-external-link" @click="goToDocumentFilling"
        class="p-button-outlined" size="small" />
    </template>

    <!-- File Actions -->
    <template #file-actions="{ file }">
      <Button v-if="canDeleteFile(file)" icon="pi pi-trash" @click="confirmDeleteFile(file)"
        class="p-button-danger p-button-outlined" size="small" v-tooltip.top="'Supprimer'" />
    </template>

    <!-- Additional Status Tags -->
    <template #additional-status-tags="{ dossier }">
      <Tag v-if="dossier.enRetard" value="En Retard" severity="danger" />
      <Tag v-if="isPhase1(dossier)" value="Phase 1" severity="info" />
    </template>
  </DossierDetailBase>

  <!-- Action Dialogs -->
  <ActionDialogs :dialogs="actionDialogs" @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed" />
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierDetailBase from '@/components/dossiers/DossierDetailBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import Card from 'primevue/card';
import Tag from 'primevue/tag';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const baseComponent = ref();

const currentDossier = ref(null);

const actionDialogs = reactive({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  initializeRealization: { visible: false, dossier: null, loading: false },
  delete: { visible: false, dossier: null, loading: false, comment: '' },
  deleteFile: { visible: false, file: null, loading: false }
});

// Permission methods based on phase and status
function canEdit(dossier) {
  return isPhase1(dossier) && (
    dossier?.statut === 'DRAFT' ||
    dossier?.statut === 'RETURNED_FOR_COMPLETION'
  );
}

function canSendToGUC(dossier) {
  return isPhase1(dossier) && canEdit(dossier);
}

function canDelete(dossier) {
  return isPhase1(dossier) && dossier?.statut === 'DRAFT';
}

function canInitializeRealization(dossier) {
  return dossier?.statut === 'APPROVED_AWAITING_FARMER';
}

function canDeleteFile(file) {
  return canEdit(currentDossier.value);
}

function isPhase1(dossier) {
  return dossier?.etapeActuelle?.includes('AP - Phase Antenne');
}

function handleDossierLoaded(dossier) {
  currentDossier.value = dossier.dossier;
}

function goToDocumentFilling() {
  router.push(`/agent_antenne/dossiers/documents/${route.params.dossierId}`);
}

function goToEdit(dossier) {
  router.push(`/agent_antenne/dossiers/${dossier.id}/edit`);
}

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

function confirmDelete(dossier) {
  actionDialogs.delete = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmDeleteFile(file) {
  actionDialogs.deleteFile = {
    visible: true,
    file: file,
    loading: false
  };
}

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, file, data } = actionData;

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
      case 'deleteFile':
        endpoint = `/agent_antenne/dossiers/${route.params.dossierId}/documents/piece-jointe/${file.id}`;
        method = 'delete';
        break;
    }

    const response = await ApiService[method](endpoint, payload);

    if (response.success || action === 'deleteFile') {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Action effectuée avec succès',
        life: 4000
      });

      if (action === 'delete') {
        router.push('/agent_antenne/dossiers');
      } else if (baseComponent.value) {
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
</script>

<style scoped>
.p-button-lg {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}
</style>

