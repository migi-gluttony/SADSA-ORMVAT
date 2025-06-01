<template>
  <DossierListBase 
    :page-title="`Dossiers - Commission Vérification Terrain`" 
    :status-options="statusOptions"
    :user-role="'AGENT_COMMISSION_TERRAIN'"
    @dossier-selected="viewDossierDetail"
    ref="baseComponent"
  >
    <!-- Header Subtitle -->
    <template #header-subtitle>
      <span>- Dossiers {{ getCommissionTeamDisplayName() }}</span>
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
            <div class="stat-value">{{ statistics.dossiersEnCommission || 0 }}</div>
            <div class="stat-label">En commission</div>
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

    <!-- Table Headers -->
    <template #table-headers>
      <th class="col-visite">Inspection Terrain</th>
    </template>

    <!-- Table Columns -->
    <template #table-columns="{ dossier }">
      <!-- Visite Terrain Column -->
      <td class="col-visite">
        <div class="visite-cell">
          <div v-if="dossier.visiteTerrainStatus" class="visite-status">
            <Tag :value="dossier.visiteTerrainStatus"
              :severity="getVisiteSeverity(dossier.visiteTerrainStatus)" />
          </div>
          <div v-else class="visite-pending">
            <Tag value="À programmer" severity="warning" />
          </div>
          <div v-if="dossier.dateVisite" class="visite-date">
            <i class="pi pi-calendar"></i>
            {{ formatDate(dossier.dateVisite) }}
          </div>
        </div>
      </td>
    </template>

    <!-- Empty Message -->
    <template #empty-message="{ hasActiveFilters }">
      <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche et filtres.</p>
      <p v-else>
        Aucun dossier n'est assigné à votre équipe ({{ getCommissionTeamDisplayName() }}) pour visite terrain.
      </p>
    </template>

    <!-- Dossier Actions -->
    <template #dossier-actions="{ dossier }">
      <Button 
        v-if="!dossier.visiteTerrainStatus || dossier.visiteTerrainStatus === 'À programmer'"
        icon="pi pi-calendar-plus" 
        @click="scheduleTerrainInspection(dossier)"
        class="p-button-info p-button-sm action-btn" 
        v-tooltip.top="'Programmer inspection terrain'"
      />

      <Button 
        v-if="dossier.visiteTerrainStatus === 'PROGRAMMEE'" 
        icon="pi pi-check-square"
        @click="completeTerrainInspection(dossier)" 
        class="p-button-success p-button-sm action-btn"
        v-tooltip.top="'Finaliser inspection'"
      />

      <Button 
        icon="pi pi-comment" 
        @click="showAddNoteDialog(dossier)"
        class="p-button-outlined p-button-sm action-btn" 
        v-tooltip.top="'Ajouter note'"
      />
    </template>
  </DossierListBase>

  <!-- Add Note Dialog -->
  <AddNoteDialog v-model:visible="addNoteDialog.visible" :dossier="addNoteDialog.dossier"
    @note-added="handleNoteAdded" />
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierListBase from '@/components/dossiers/DossierListBase.vue';
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';

const router = useRouter();
const toast = useToast();
const baseComponent = ref();

// Status options for Commission
const statusOptions = [
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
];

// Dialogs
const addNoteDialog = reactive({
  visible: false,
  dossier: null
});

// Methods
function getCommissionTeamDisplayName() {
  const currentUser = AuthService.getCurrentUser();
  const equipe = currentUser?.equipeCommission;
  
  if (!equipe) return 'Commission Générale';
  
  const teamNames = {
    'FILIERES_VEGETALES': 'Équipe Filières Végétales',
    'FILIERES_ANIMALES': 'Équipe Filières Animales', 
    'AMENAGEMENT_HYDRO_AGRICOLE': 'Équipe Aménagement Hydro-Agricole'
  };
  
  return teamNames[equipe] || equipe;
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_commission/dossiers/${dossierId}`);
}

function scheduleTerrainInspection(dossier) {
  // Navigate to dossier detail where scheduling can be done
  viewDossierDetail(dossier.id);
}

function completeTerrainInspection(dossier) {
  // Navigate to dossier detail where completion can be done
  viewDossierDetail(dossier.id);
}

function showAddNoteDialog(dossier) {
  addNoteDialog.visible = true;
  addNoteDialog.dossier = dossier;
}

function getVisiteSeverity(status) {
  const severityMap = {
    'À programmer': 'warning',
    'PROGRAMMEE': 'info',
    'COMPLETEE': 'success',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severityMap[status] || 'secondary';
}

async function handleNoteAdded() {
  addNoteDialog.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Note ajoutée avec succès',
    life: 3000
  });

  if (baseComponent.value) {
    baseComponent.value.loadDossiers();
  }
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
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
.col-visite {
  width: 15%;
  min-width: 150px;
}

.visite-cell {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.visite-status,
.visite-pending {
  display: flex;
  justify-content: center;
}

.visite-date {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--text-secondary);
  justify-content: center;
}

.visite-date i {
  color: var(--primary-color);
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