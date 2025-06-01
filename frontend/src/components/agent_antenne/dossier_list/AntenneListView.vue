<template>
  <DossierListBase 
    :page-title="'Mes Dossiers'" 
    :status-options="statusOptions"
    :user-role="'AGENT_ANTENNE'"
    @dossier-selected="viewDossierDetail"
  >
    <!-- Header Subtitle -->
    <template #header-subtitle>
      <span v-if="dossiersData?.currentUserCDA">
        - CDA: {{ dossiersData.currentUserCDA }}
      </span>
    </template>

    <!-- Header Actions -->
    <template #header-actions>
      <Button label="Nouveau Dossier" icon="pi pi-plus" @click="navigateToCreate"
        class="p-button-success btn-primary" />
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
            <div class="stat-value">{{ statistics.dossiersEnCours }}</div>
            <div class="stat-label">En cours</div>
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

    <!-- Empty Message -->
    <template #empty-message="{ hasActiveFilters }">
      <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche et filtres.</p>
      <p v-else>Vous n'avez pas encore créé de dossier.</p>
    </template>

    <!-- Empty Actions -->
    <template #empty-actions>
      <Button label="Créer mon premier dossier" icon="pi pi-plus"
        @click="navigateToCreate" class="p-button-success btn-primary" />
    </template>

    <!-- Dossier Actions -->
    <template #dossier-actions="{ dossier }">
      <Button v-if="dossier.permissions?.peutEtreModifie && (dossier.completionPercentage || 0) < 100"
        icon="pi pi-pencil" @click="continueDossier(dossier.id)" class="p-button-sm action-btn btn-edit"
        v-tooltip.top="'Continuer'" />

      <Button v-if="dossier.permissions?.peutEtreEnvoye" icon="pi pi-send"
        @click="confirmSendToGUC(dossier)" class="p-button-success p-button-sm action-btn"
        v-tooltip.top="'Envoyer au GUC'" />

      <Button v-if="dossier.permissions?.peutEtreSupprime" icon="pi pi-trash"
        @click="confirmDeleteDossier(dossier)"
        class="p-button-danger p-button-outlined p-button-sm action-btn" v-tooltip.top="'Supprimer'" />
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

// PrimeVue components
import Button from 'primevue/button';

const router = useRouter();
const toast = useToast();

// Data
const dossiersData = ref(null);

// Status options for Antenne
const statusOptions = [
  { label: 'Brouillon', value: 'DRAFT' },
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Retourné pour complétion', value: 'RETURNED_FOR_COMPLETION' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
];

// Dialogs
const actionDialogs = reactive({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  delete: { visible: false, dossier: null, loading: false, comment: '' }
});

// Methods
function navigateToCreate() {
  router.push('/agent_antenne/dossiers/create');
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}`);
}

function continueDossier(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}/forms`);
}

function confirmSendToGUC(dossier) {
  actionDialogs.sendToGUC = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
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

    switch (action) {
      case 'sendToGUC':
        endpoint = `/dossiers/${dossier.id}/send-to-guc`;
        payload = { commentaire: data.comment };
        break;
      case 'delete':
        endpoint = `/dossiers/${dossier.id}`;
        payload = { motif: data.comment };
        break;
    }

    const method = action === 'delete' ? 'delete' : 'post';
    const response = await ApiService[method](endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });

      // Reload dossiers through the base component
      const baseComponent = document.querySelector('dossier-list-base');
      if (baseComponent) {
        baseComponent.loadDossiers();
      }
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

/* Responsive Design */
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