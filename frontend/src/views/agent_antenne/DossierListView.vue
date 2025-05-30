<template>
  <div class="dossier-list-container">
    <UserInfoHeader 
      search-placeholder="Rechercher par SABA, CIN, nom..."
      @search="handleSearch"
    />

    <!-- Header with actions -->
    <div class="list-header">
      <div class="header-info">
        <h1><i class="pi pi-folder-open"></i> Mes Dossiers</h1>
        <p v-if="dossiersData">{{ dossiersData.totalCount }} dossier(s) - CDA: {{ dossiersData.currentUserCDA }}</p>
      </div>
      <div class="header-actions">
        <Button 
          label="Nouveau Dossier" 
          icon="pi pi-plus" 
          @click="navigateToCreate"
          class="p-button-success"
        />
        <Button 
          label="Actualiser" 
          icon="pi pi-refresh" 
          @click="loadDossiers"
          :loading="loading"
          class="p-button-outlined"
        />
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-section component-card">
      <div class="filters-grid">
        <div class="filter-group">
          <label>Statut</label>
          <Dropdown 
            v-model="filters.statut" 
            :options="statutOptions"
            option-label="label"
            option-value="value"
            placeholder="Tous les statuts"
            @change="applyFilters"
          />
        </div>
        <div class="filter-group">
          <label>Type de projet</label>
          <Dropdown 
            v-model="filters.sousRubriqueId" 
            :options="sousRubriqueOptions"
            option-label="designation"
            option-value="id"
            placeholder="Tous les types"
            @change="applyFilters"
          />
        </div>
        <div class="filter-group">
          <label>Période</label>
          <Calendar 
            v-model="filters.dateRange" 
            selection-mode="range"
            date-format="dd/mm/yy"
            placeholder="Sélectionner une période"
            @date-select="applyFilters"
          />
        </div>
        <div class="filter-actions">
          <Button 
            label="Effacer" 
            icon="pi pi-times" 
            @click="clearFilters"
            class="p-button-text"
          />
        </div>
      </div>
    </div>

    <!-- Dossiers List -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement des dossiers...</span>
    </div>

    <div v-else-if="dossiers.length === 0" class="empty-state">
      <div class="empty-content">
        <i class="pi pi-folder-open empty-icon"></i>
        <h3>Aucun dossier trouvé</h3>
        <p>Vous n'avez pas encore créé de dossier ou aucun dossier ne correspond à vos critères de recherche.</p>
        <Button 
          label="Créer mon premier dossier" 
          icon="pi pi-plus" 
          @click="navigateToCreate"
          class="p-button-success"
        />
      </div>
    </div>

    <div v-else class="dossiers-grid">
      <div 
        v-for="dossier in dossiers" 
        :key="dossier.id"
        class="dossier-card"
        :class="{ 
          'urgent': dossier.joursRestants <= 1,
          'warning': dossier.joursRestants <= 2 && dossier.joursRestants > 1
        }"
      >
        <!-- Card Header -->
        <div class="card-header">
          <div class="dossier-meta">
            <span class="dossier-reference">{{ dossier.reference }}</span>
            <span class="dossier-saba">SABA: {{ dossier.saba }}</span>
          </div>
          <div class="dossier-status">
            <Tag 
              :value="dossier.statut" 
              :severity="getStatusSeverity(dossier.statut)"
            />
          </div>
        </div>

        <!-- Card Content -->
        <div class="card-content">
          <div class="agriculteur-info">
            <h3>{{ dossier.agriculteurPrenom }} {{ dossier.agriculteurNom }}</h3>
            <p><i class="pi pi-id-card"></i> {{ dossier.agriculteurCin }}</p>
          </div>

          <div class="project-info">
            <p class="project-type">
              <i class="pi pi-tags"></i>
              {{ dossier.sousRubriqueDesignation }}
            </p>
            <p class="project-amount" v-if="dossier.montantDemande">
              <i class="pi pi-money-bill"></i>
              {{ formatCurrency(dossier.montantDemande) }}
            </p>
          </div>

          <!-- Progress Section -->
          <div class="progress-section">
            <div class="progress-header">
              <span>Complété: {{ dossier.formsCompleted }}/{{ dossier.totalForms }} formulaires</span>
              <span class="percentage">{{ dossier.completionPercentage }}%</span>
            </div>
            <ProgressBar 
              :value="dossier.completionPercentage" 
              :show-value="false"
              :class="{
                'progress-complete': dossier.completionPercentage === 100,
                'progress-medium': dossier.completionPercentage >= 50 && dossier.completionPercentage < 100,
                'progress-low': dossier.completionPercentage < 50
              }"
            />
          </div>

          <!-- Time Section -->
          <div class="time-section">
            <div class="time-info">
              <i class="pi pi-clock"></i>
              <span v-if="dossier.joursRestants > 0" class="time-remaining">
                {{ dossier.joursRestants }} jour(s) restant(s)
              </span>
              <span v-else class="time-expired">Délai dépassé</span>
            </div>
            <div class="creation-date">
              Créé le {{ formatDate(dossier.dateCreation) }}
            </div>
          </div>
        </div>

        <!-- Card Actions -->
        <div class="card-actions">
          <Button 
            label="Voir détails" 
            icon="pi pi-eye" 
            @click="viewDossierDetail(dossier.id)"
            class="p-button-outlined p-button-sm"
          />
          
          <Button 
            v-if="dossier.peutEtreModifie && dossier.completionPercentage < 100" 
            label="Continuer" 
            icon="pi pi-pencil" 
            @click="continueDossier(dossier.id)"
            class="p-button-sm"
          />

          <Button 
            v-if="dossier.peutEtreEnvoye" 
            label="Envoyer au GUC" 
            icon="pi pi-send" 
            @click="confirmSendToGUC(dossier)"
            class="p-button-success p-button-sm"
          />

          <Button 
            v-if="dossier.peutEtreSupprime" 
            icon="pi pi-trash" 
            @click="confirmDeleteDossier(dossier)"
            class="p-button-danger p-button-outlined p-button-sm"
            v-tooltip.top="'Supprimer'"
          />
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="dossiers.length > 0" class="pagination-section">
      <Paginator 
        :rows="pageSize" 
        :total-records="totalRecords" 
        :first="currentPage * pageSize"
        @page="onPageChange"
        :rows-per-page-options="[5, 10, 20, 50]"
      />
    </div>

    <!-- Delete Confirmation Dialog -->
    <Dialog 
      v-model:visible="deleteDialog.visible" 
      modal 
      header="Confirmer la suppression"
      :style="{ width: '450px' }"
    >
      <div class="delete-confirmation">
        <i class="pi pi-exclamation-triangle warning-icon"></i>
        <div class="confirmation-text">
          <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
          <div class="dossier-summary">
            <strong>{{ deleteDialog.dossier?.reference }}</strong><br>
            {{ deleteDialog.dossier?.agriculteurPrenom }} {{ deleteDialog.dossier?.agriculteurNom }}
          </div>
          <p class="warning-text">Cette action est irréversible.</p>
        </div>
      </div>
      
      <div class="delete-comment">
        <label for="deleteComment">Commentaire (optionnel)</label>
        <Textarea 
          id="deleteComment"
          v-model="deleteDialog.comment" 
          rows="3" 
          placeholder="Raison de la suppression..."
        />
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="deleteDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Supprimer" 
          icon="pi pi-trash" 
          @click="deleteDossier"
          class="p-button-danger"
          :loading="deleteDialog.loading"
        />
      </template>
    </Dialog>

    <!-- Send to GUC Confirmation Dialog -->
    <Dialog 
      v-model:visible="sendDialog.visible" 
      modal 
      header="Envoyer au GUC"
      :style="{ width: '450px' }"
    >
      <div class="send-confirmation">
        <i class="pi pi-send confirmation-icon"></i>
        <div class="confirmation-text">
          <p>Confirmer l'envoi de ce dossier au Guichet Unique Central ?</p>
          <div class="dossier-summary">
            <strong>{{ sendDialog.dossier?.reference }}</strong><br>
            {{ sendDialog.dossier?.agriculteurPrenom }} {{ sendDialog.dossier?.agriculteurNom }}<br>
            <small>Complété à {{ sendDialog.dossier?.completionPercentage }}%</small>
          </div>
          <p class="info-text">Une fois envoyé, le dossier ne pourra plus être modifié.</p>
        </div>
      </div>
      
      <div class="send-comment">
        <label for="sendComment">Commentaire (optionnel)</label>
        <Textarea 
          id="sendComment"
          v-model="sendDialog.comment" 
          rows="3" 
          placeholder="Commentaires pour le GUC..."
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
          label="Envoyer" 
          icon="pi pi-send" 
          @click="sendDossierToGUC"
          class="p-button-success"
          :loading="sendDialog.loading"
        />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import UserInfoHeader from '@/components/UserInfoHeader.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import Paginator from 'primevue/paginator';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const dossiers = ref([]);
const dossiersData = ref(null);
const searchTerm = ref('');

// Pagination
const currentPage = ref(0);
const pageSize = ref(10);
const totalRecords = computed(() => dossiersData.value?.totalCount || 0);

// Filters
const filters = ref({
  statut: null,
  sousRubriqueId: null,
  dateRange: null
});

const statutOptions = ref([
  { label: 'Phase Antenne', value: 'Phase Antenne' },
  { label: 'Phase GUC', value: 'Phase GUC' },
  { label: 'Commission Technique', value: 'Commission Technique' },
  { label: 'Réalisation', value: 'Réalisation' }
]);

const sousRubriqueOptions = ref([]);

// Dialogs
const deleteDialog = ref({
  visible: false,
  dossier: null,
  comment: '',
  loading: false
});

const sendDialog = ref({
  visible: false,
  dossier: null,
  comment: '',
  loading: false
});

// Methods
onMounted(() => {
  loadDossiers();
});

async function loadDossiers() {
  try {
    loading.value = true;
    
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'dateCreation',
      sortDirection: 'DESC'
    };

    if (searchTerm.value) params.searchTerm = searchTerm.value;
    if (filters.value.statut) params.statut = filters.value.statut;
    if (filters.value.sousRubriqueId) params.sousRubriqueId = filters.value.sousRubriqueId;

    const response = await ApiService.get('/agent_antenne/dossiers', params);
    
    dossiersData.value = response;
    dossiers.value = response.dossiers || [];
    
    // Load sous-rubriques for filter if not already loaded
    if (sousRubriqueOptions.value.length === 0) {
      await loadSousRubriques();
    }
    
  } catch (error) {
    console.error('Erreur lors du chargement des dossiers:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les dossiers',
      life: 3000
    });
  } finally {
    loading.value = false;
  }
}


async function loadSousRubriques() {
  try {
    const response = await ApiService.get('/agent_antenne/dossiers/initialization-data');
    if (response.rubriques) {
      const allSousRubriques = [];
      response.rubriques.forEach(rubrique => {
        rubrique.sousRubriques.forEach(sr => {
          allSousRubriques.push({
            id: sr.id,
            designation: sr.designation
          });
        });
      });
      sousRubriqueOptions.value = allSousRubriques;
    }
  } catch (error) {
    console.error('Erreur lors du chargement des sous-rubriques:', error);
  }
}



function handleSearch(query) {
  searchTerm.value = query;
  currentPage.value = 0;
  loadDossiers();
}

function applyFilters() {
  currentPage.value = 0;
  loadDossiers();
}

function clearFilters() {
  filters.value = {
    statut: null,
    sousRubriqueId: null,
    dateRange: null
  };
  searchTerm.value = '';
  applyFilters();
}

function onPageChange(event) {
  currentPage.value = event.page;
  pageSize.value = event.rows;
  loadDossiers();
}

function navigateToCreate() {
  router.push('/agent_antenne/dossiers/create');
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}`);
}

function continueDossier(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}/forms`);
}

function confirmDeleteDossier(dossier) {
  deleteDialog.value = {
    visible: true,
    dossier: dossier,
    comment: '',
    loading: false
  };
}

async function deleteDossier() {
  try {
    deleteDialog.value.loading = true;
    
    await ApiService.delete(`/agent_antenne/dossiers/${deleteDialog.value.dossier.id}`, {
      comment: deleteDialog.value.comment
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier supprimé avec succès',
      life: 3000
    });
    
    deleteDialog.value.visible = false;
    loadDossiers();
    
  } catch (error) {
    console.error('Erreur lors de la suppression:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de supprimer le dossier',
      life: 3000
    });
  } finally {
    deleteDialog.value.loading = false;
  }
}

function confirmSendToGUC(dossier) {
  sendDialog.value = {
    visible: true,
    dossier: dossier,
    comment: '',
    loading: false
  };
}

async function sendDossierToGUC() {
  try {
    sendDialog.value.loading = true;
    
    await ApiService.post(`/agent_antenne/dossiers/${sendDialog.value.dossier.id}/send-to-guc`, {
      comment: sendDialog.value.comment
    });
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier envoyé au GUC avec succès',
      life: 3000
    });
    
    sendDialog.value.visible = false;
    loadDossiers();
    
  } catch (error) {
    console.error('Erreur lors de l\'envoi:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'envoyer le dossier',
      life: 3000
    });
  } finally {
    sendDialog.value.loading = false;
  }
}

function getStatusSeverity(status) {
  const severityMap = {
    'Phase Antenne': 'info',
    'Phase GUC': 'warning',
    'Commission Technique': 'secondary',
    'Réalisation': 'success'
  };
  return severityMap[status] || 'info';
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
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.dossier-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
}

/* Header */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  background: var(--background-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-info h1 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-info p {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Filters */
.filters-section {
  margin-bottom: 1.5rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 500;
  color: #374151;
  font-size: 0.875rem;
}

.filter-actions {
  display: flex;
  align-items: end;
}

/* Loading and Empty States */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
  color: #6b7280;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  font-size: 4rem;
  color: #d1d5db;
  margin-bottom: 1rem;
}

.empty-content h3 {
  color: #374151;
  margin-bottom: 0.5rem;
}

.empty-content p {
  color: #6b7280;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

/* Dossiers Grid */
.dossiers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.dossier-card {
  background: var(--background-color);
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  padding: 1.5rem;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dossier-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-color);
}

.dossier-card.urgent {
  border-color: #dc2626;
  background: rgba(220, 38, 38, 0.02);
}

.dossier-card.warning {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.02);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.dossier-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.dossier-reference {
  font-weight: 600;
  color: var(--primary-color);
  font-size: 0.9rem;
}

.dossier-saba {
  font-size: 0.8rem;
  color: #6b7280;
}

/* Card Content */
.agriculteur-info h3 {
  margin: 0 0 0.5rem 0;
  color: #374151;
  font-size: 1.1rem;
}

.agriculteur-info p {
  margin: 0;
  color: #6b7280;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.project-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.project-type,
.project-amount {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.project-type i,
.project-amount i {
  color: var(--primary-color);
}

/* Progress Section */
.progress-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
}

.percentage {
  font-weight: 600;
  color: var(--primary-color);
}

:deep(.progress-complete .p-progressbar-value) {
  background: #10b981;
}

:deep(.progress-medium .p-progressbar-value) {
  background: #f59e0b;
}

:deep(.progress-low .p-progressbar-value) {
  background: #dc2626;
}

/* Time Section */
.time-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.time-remaining {
  color: #10b981;
  font-weight: 500;
}

.time-expired {
  color: #dc2626;
  font-weight: 500;
}

.creation-date {
  color: #6b7280;
}

/* Card Actions */
.card-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: auto;
}

/* Pagination */
.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

/* Dialog Styles */
.delete-confirmation,
.send-confirmation {
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

.delete-comment,
.send-comment {
  margin-top: 1rem;
}

.delete-comment label,
.send-comment label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
}

/* Dark Mode */
.dark-mode .list-header,
.dark-mode .filters-section,
.dark-mode .dossier-card {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .dossier-card.urgent {
  background: rgba(220, 38, 38, 0.1);
}

.dark-mode .dossier-card.warning {
  background: rgba(245, 158, 11, 0.1);
}

.dark-mode .dossier-summary {
  background: #374151;
}

/* Responsive */
@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .dossiers-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    justify-content: center;
  }

  .filters-grid {
    grid-template-columns: 1fr;
  }
}
</style>