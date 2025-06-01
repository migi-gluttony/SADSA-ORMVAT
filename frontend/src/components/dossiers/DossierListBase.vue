<template>
  <div class="dossier-list-container">
    <!-- Header -->
    <div class="list-header">
      <div class="header-info">
        <h1>
          <i class="pi pi-folder-open"></i>
          {{ pageTitle }}
        </h1>
        <p v-if="dossiersData">
          {{ dossiersData.totalCount }} dossier(s)
          <slot name="header-subtitle"></slot>
        </p>
      </div>
      <div class="header-actions">
        <slot name="header-actions"></slot>
        <Button label="Actualiser" icon="pi pi-refresh" @click="loadDossiers" :loading="loading"
          class="p-button-outlined" />
      </div>
    </div>

    <!-- Statistics -->
    <slot name="statistics" :statistics="dossiersData?.statistics"></slot>

    <!-- Filters -->
    <div class="filters-section">
      <div class="filters-grid">
        <!-- Search Input -->
        <div class="filter-group search-group">
          <label>RECHERCHE</label>
          <div class="search-input-wrapper">
            <i class="pi pi-search search-icon"></i>
            <InputText v-model="searchTerm" placeholder="Rechercher par SABA, CIN, nom..." class="search-input"
              @input="handleSearch" />
            <button v-if="searchTerm" class="clear-search-btn" @click="clearSearch" title="Effacer la recherche">
              <i class="pi pi-times"></i>
            </button>
          </div>
        </div>

        <!-- Status Filter -->
        <div class="filter-group">
          <label>STATUT</label>
          <Select v-model="filters.statut" :options="statusOptions" optionLabel="label" optionValue="value"
            placeholder="Tous les statuts" @change="applyFilters" class="filter-select" :clearable="true" />
        </div>

        <!-- Project Type Filter -->
        <div class="filter-group">
          <label>TYPE DE PROJET</label>
          <Select v-model="filters.sousRubriqueId" :options="sousRubriqueOptions" optionLabel="designation"
            optionValue="id" placeholder="Tous les types" @change="applyFilters" class="filter-select"
            :clearable="true" />
        </div>

        <!-- Role-specific filters -->
        <slot name="additional-filters"></slot>

        <!-- Clear Filters -->
        <div class="filter-actions">
          <Button label="Effacer" icon="pi pi-times" @click="clearFilters" class="p-button-text btn-clear" />
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement des dossiers...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Erreur de chargement</h3>
      <p>{{ error }}</p>
      <Button label="Réessayer" icon="pi pi-refresh" @click="loadDossiers" class="p-button-outlined" />
    </div>

    <!-- Empty State -->
    <div v-else-if="dossiers.length === 0" class="empty-state">
      <div class="empty-content">
        <i class="pi pi-folder-open empty-icon"></i>
        <h3>Aucun dossier trouvé</h3>
        <slot name="empty-message" :hasActiveFilters="hasActiveFilters"></slot>
        <div class="empty-actions">
          <Button v-if="hasActiveFilters" label="Effacer les filtres" icon="pi pi-filter-slash" @click="clearFilters"
            class="p-button-outlined" />
          <slot name="empty-actions"></slot>
        </div>
      </div>
    </div>

    <!-- Dossiers Table -->
    <div v-else class="table-container">
      <table class="dossiers-table">
        <thead>
          <tr>
            <th class="col-reference">
              Référence
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th class="col-agriculteur">
              Agriculteur
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th class="col-project">Type de Projet</th>
            <slot name="table-headers"></slot>
            <th class="col-status">
              Statut
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th class="col-progress">
              Avancement
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th class="col-details">Délais</th>
            <th class="col-actions">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dossier in dossiers" :key="dossier.id" class="dossier-row" :class="{
            'urgent': dossier.joursRestants <= 1,
            'warning': dossier.joursRestants <= 2 && dossier.joursRestants > 1
          }">
            <!-- Reference Column -->
            <td class="col-reference">
              <div class="reference-cell">
                <div class="reference-info">
                  <span class="reference-number">{{ dossier.reference }}</span>
                  <span class="saba-number">SABA: {{ dossier.saba }}</span>
                </div>
              </div>
            </td>

            <!-- Agriculteur Column -->
            <td class="col-agriculteur">
              <div class="agriculteur-cell">
                <div class="agriculteur-name">{{ dossier.agriculteurPrenom }} {{ dossier.agriculteurNom }}</div>
                <div class="agriculteur-cin">
                  <i class="pi pi-id-card"></i>
                  {{ dossier.agriculteurCin }}
                </div>
              </div>
            </td>

            <!-- Project Type Column -->
            <td class="col-project">
              <div class="project-cell">
                <div class="project-type">{{ dossier.sousRubriqueDesignation }}</div>
                <div class="project-amount" v-if="dossier.montantSubvention">
                  <i class="pi pi-money-bill"></i>
                  {{ formatCurrency(dossier.montantSubvention) }}
                </div>
              </div>
            </td>

            <!-- Role-specific columns -->
            <slot name="table-columns" :dossier="dossier"></slot>

            <!-- Status Column -->
            <td class="col-status">
              <Tag :value="dossier.statut" :severity="getStatusSeverity(dossier.statut)" class="status-tag" />
            </td>

            <!-- Progress Column -->
            <td class="col-progress">
              <div class="progress-cell">
                <div class="progress-info">
                  <span class="progress-text">{{ dossier.formsCompleted || 0 }}/{{ dossier.totalForms || 0 }}
                    formulaires</span>
                  <span class="progress-percentage">{{ Math.round(dossier.completionPercentage || 0) }}%</span>
                </div>
                <ProgressBar :value="dossier.completionPercentage || 0" :show-value="false" class="progress-bar" :class="{
                  'progress-complete': (dossier.completionPercentage || 0) === 100,
                  'progress-medium': (dossier.completionPercentage || 0) >= 50 && (dossier.completionPercentage || 0) < 100,
                  'progress-low': (dossier.completionPercentage || 0) < 50
                }" />
              </div>
            </td>

            <!-- Details Column -->
            <td class="col-details">
              <div class="details-cell">
                <div class="time-info">
                  <i class="pi pi-clock"></i>
                  <span v-if="(dossier.joursRestants || 0) > 0" class="time-remaining">
                    {{ dossier.joursRestants }} jour(s)
                  </span>
                  <span v-else class="time-expired">Délai dépassé</span>
                </div>
                <div class="creation-date">
                  Créé le {{ formatDate(dossier.dateCreation) }}
                </div>
              </div>
            </td>

            <!-- Actions Column -->
            <td class="col-actions">
              <div class="actions-cell">
                <Button icon="pi pi-eye" @click="viewDossierDetail(dossier.id)"
                  class="p-button-outlined p-button-sm action-btn" v-tooltip.top="'Voir détails'" />
                
                <slot name="dossier-actions" :dossier="dossier"></slot>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="dossiers.length > 0" class="pagination-section">
      <Paginator :rows="pageSize" :total-records="totalRecords" :first="currentPage * pageSize" @page="onPageChange"
        :rows-per-page-options="[5, 10, 20, 50]" />
    </div>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import Paginator from 'primevue/paginator';
import Toast from 'primevue/toast';

// Props
const props = defineProps({
  pageTitle: {
    type: String,
    required: true
  },
  statusOptions: {
    type: Array,
    default: () => []
  },
  userRole: {
    type: String,
    default: () => AuthService.getCurrentUser()?.role || ''
  }
});

// Emits
const emit = defineEmits(['dossier-selected', 'action-triggered']);

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const error = ref(null);
const dossiers = ref([]);
const dossiersData = ref(null);
const searchTerm = ref('');

// Pagination
const currentPage = ref(0);
const pageSize = ref(10);
const totalRecords = computed(() => dossiersData.value?.totalCount || 0);

// Filters
const filters = reactive({
  statut: null,
  sousRubriqueId: null
});

// Filter options
const sousRubriqueOptions = ref([]);

// Computed
const hasActiveFilters = computed(() => {
  const hasSearch = searchTerm.value && searchTerm.value.trim();
  const hasStatus = filters.statut;
  const hasSubRubrique = filters.sousRubriqueId;
  
  return hasSearch || hasStatus || hasSubRubrique;
});

// Methods
onMounted(async () => {
  try {
    await loadFilterOptions();
    await loadDossiers();
  } catch (err) {
    console.error('Error during component initialization:', err);
    error.value = 'Erreur lors de l\'initialisation du composant';
  }
});

async function loadDossiers() {
  try {
    loading.value = true;
    error.value = null;

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'dateCreation',
      sortDirection: 'DESC'
    };

    // Add search and filters
    if (searchTerm.value?.trim()) params.searchTerm = searchTerm.value.trim();
    if (filters.statut) params.statut = filters.statut;
    if (filters.sousRubriqueId) params.sousRubriqueId = filters.sousRubriqueId;

    const response = await ApiService.get('/dossiers', params);
    dossiersData.value = response;
    dossiers.value = response.dossiers || [];

  } catch (err) {
    console.error('Erreur lors du chargement des dossiers:', err);
    error.value = err.message || 'Impossible de charger les dossiers';
    
    // Initialize with empty data
    dossiersData.value = {
      dossiers: [],
      totalCount: 0,
      currentPage: 0,
      pageSize: pageSize.value,
      totalPages: 0,
      statistics: {
        totalDossiers: 0,
        dossiersEnCours: 0,
        dossiersApprouves: 0,
        dossiersRejetes: 0,
        dossiersEnRetard: 0
      }
    };
    dossiers.value = [];

  } finally {
    loading.value = false;
  }
}

async function loadFilterOptions() {
  try {
    sousRubriqueOptions.value = [
      { id: 1, designation: 'FILIERES VEGETALES - Équipements' },
      { id: 2, designation: 'FILIERES VEGETALES - Matériel' },
      { id: 3, designation: 'FILIERES ANIMALES - Équipements' },
      { id: 4, designation: 'FILIERES ANIMALES - Installations' },
      { id: 5, designation: 'AMENAGEMENT HYDRO-AGRICOLE' },
      { id: 6, designation: 'AMELIORATION FONCIERE' }
    ];
  } catch (err) {
    console.error('Erreur lors du chargement des options de filtre:', err);
    sousRubriqueOptions.value = [];
  }
}

function handleSearch() {
  currentPage.value = 0;
  loadDossiers();
}

function clearSearch() {
  searchTerm.value = '';
  handleSearch();
}

function applyFilters() {
  currentPage.value = 0;
  loadDossiers();
}

function clearFilters() {
  Object.assign(filters, {
    statut: null,
    sousRubriqueId: null
  });
  searchTerm.value = '';
  applyFilters();
}

function onPageChange(event) {
  currentPage.value = event.page;
  pageSize.value = event.rows;
  loadDossiers();
}

function viewDossierDetail(dossierId) {
  emit('dossier-selected', dossierId);
}

// Utility methods
function getStatusSeverity(status) {
  const severityMap = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'RETURNED_FOR_COMPLETION': 'warning',
    'PENDING_CORRECTION': 'warning'
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

// Expose methods for child components
defineExpose({
  loadDossiers,
  clearFilters
});
</script>

<style scoped>
.dossier-list-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  background: var(--background-color);
  min-height: 100vh;
}

/* Header Section */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 2rem;
  background: var(--header-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
}

.header-info h1 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
  font-size: 1.75rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  letter-spacing: -0.025em;
}

.header-info h1 i {
  font-size: 1.5rem;
}

.header-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.95rem;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Primary Button Styles */
:deep(.btn-primary) {
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  color: var(--text-on-primary) !important;
}

:deep(.btn-primary:hover) {
  background-color: var(--accent-color) !important;
  border-color: var(--accent-color) !important;
}

:deep(.btn-edit) {
  background-color: var(--accent-color) !important;
  border-color: var(--accent-color) !important;
  color: var(--text-on-primary) !important;
}

:deep(.btn-clear) {
  color: var(--primary-color) !important;
}

/* Filters Section */
.filters-section {
  margin-bottom: 1.5rem;
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
}

.filters-grid {
  display: grid;
  grid-template-columns: 1fr 200px 250px auto;
  gap: 1.25rem;
  align-items: end;
  padding: 1.5rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Search Input Styling */
.search-group {
  grid-column: 1;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
  z-index: 2;
  transition: color 0.3s ease;
}

.search-input {
  width: 100%;
  height: 36px;
  padding: 0 2.5rem 0 2.25rem;
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  background: var(--background-color);
  color: var(--text-color);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  background: var(--card-background);
  box-shadow: 0 0 0 2px rgba(var(--primary-color-rgb), 0.1);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--border-radius-sm);
  font-size: 0.75rem;
  transition: all 0.2s ease;
  z-index: 2;
}

.clear-search-btn:hover {
  color: var(--text-color);
  background: var(--clr-surface-tonal-a10);
}

/* Filter Components */
.filter-select {
  height: 36px;
}

:deep(.filter-select .p-select) {
  height: 36px;
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
}

.filter-actions {
  display: flex;
  align-items: end;
}

/* Loading and Empty States */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  gap: 1.5rem;
  color: var(--text-secondary);
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
}

.error-container i {
  font-size: 3rem;
  color: var(--danger-color);
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 4rem;
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
}

.empty-content {
  text-align: center;
  max-width: 450px;
}

.empty-icon {
  font-size: 4rem;
  color: var(--text-muted);
  margin-bottom: 1.5rem;
}

.empty-content h3 {
  color: var(--text-color);
  margin-bottom: 0.75rem;
  font-size: 1.25rem;
  font-weight: 600;
}

.empty-content p {
  color: var(--text-secondary);
  margin-bottom: 2rem;
  line-height: 1.6;
  font-size: 1rem;
}

.empty-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

/* Table Container */
.table-container {
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  margin-bottom: 2rem;
}

/* Table Styles */
.dossiers-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.dossiers-table thead {
  background: var(--section-background);
  border-bottom: 2px solid var(--card-border);
}

.dossiers-table th {
  padding: 1rem 0.75rem;
  text-align: left;
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  position: relative;
}

.dossiers-table th:hover {
  background: var(--clr-surface-tonal-a10);
  cursor: pointer;
}

.sort-icon {
  margin-left: 0.5rem;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.dossiers-table tbody tr {
  border-bottom: 1px solid var(--card-border);
  transition: background-color 0.2s ease;
}

.dossiers-table tbody tr:hover {
  background: var(--section-background);
}

.dossier-row.urgent {
  border-left: 4px solid var(--danger-color);
}

.dossier-row.warning {
  border-left: 4px solid var(--warning-color);
}

.dossiers-table td {
  padding: 1rem 0.75rem;
  vertical-align: top;
}

/* Column Specific Styles */
.col-reference {
  width: 12%;
  min-width: 150px;
}

.col-agriculteur {
  width: 18%;
  min-width: 180px;
}

.col-project {
  width: 20%;
  min-width: 200px;
}

.col-status {
  width: 12%;
  min-width: 120px;
}

.col-progress {
  width: 15%;
  min-width: 150px;
}

.col-details {
  width: 13%;
  min-width: 130px;
}

.col-actions {
  width: 15%;
  min-width: 180px;
}

/* Cell Content Styles */
.reference-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.reference-number {
  font-weight: 700;
  color: var(--primary-color);
  font-size: 0.9rem;
  letter-spacing: 0.025em;
}

.saba-number {
  font-size: 0.75rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.agriculteur-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.agriculteur-name {
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.875rem;
  line-height: 1.3;
}

.agriculteur-cin {
  font-size: 0.75rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.agriculteur-cin i {
  color: var(--primary-color);
}

.project-cell {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.project-type {
  font-weight: 500;
  color: var(--text-color);
  font-size: 0.875rem;
  line-height: 1.3;
}

.project-amount {
  font-size: 0.75rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.project-amount i {
  color: var(--success-color);
}

.status-tag {
  font-size: 0.75rem !important;
  font-weight: 600 !important;
  padding: 0.375rem 0.75rem !important;
  border-radius: var(--border-radius-sm) !important;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
}

.progress-text {
  color: var(--text-secondary);
  font-weight: 500;
}

.progress-percentage {
  font-weight: 700;
  color: var(--primary-color);
}

:deep(.progress-bar .p-progressbar) {
  height: 6px;
  border-radius: var(--border-radius-sm);
  background: var(--card-border);
}

:deep(.progress-complete .p-progressbar-value) {
  background: var(--success-color);
}

:deep(.progress-medium .p-progressbar-value) {
  background: var(--warning-color);
}

:deep(.progress-low .p-progressbar-value) {
  background: var(--danger-color);
}

.details-cell {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
}

.time-info i {
  color: var(--primary-color);
}

.time-remaining {
  color: var(--success-color);
  font-weight: 600;
}

.time-expired {
  color: var(--danger-color);
  font-weight: 600;
}

.creation-date {
  color: var(--text-secondary);
  font-size: 0.7rem;
}

.actions-cell {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
  align-items: center;
}

.action-btn {
  padding: 0.5rem !important;
  min-width: 2.5rem !important;
  height: 2.5rem !important;
}

/* Pagination */
.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
  padding: 1rem;
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
}

/* Responsive Design */
@media (max-width: 1200px) {
  .table-container {
    overflow-x: auto;
  }

  .dossiers-table {
    min-width: 1200px;
  }

  .filters-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}

@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    gap: 1.5rem;
    align-items: stretch;
    padding: 1.5rem;
  }

  .header-actions {
    justify-content: center;
  }

  .filters-grid {
    grid-template-columns: 1fr;
    padding: 1rem;
  }

  .header-info h1 {
    font-size: 1.5rem;
  }

  .dossiers-table th,
  .dossiers-table td {
    padding: 0.75rem 0.5rem;
  }
}

@media (max-width: 480px) {
  .dossier-list-container {
    padding: 0.5rem;
  }

  .filters-grid {
    gap: 1rem;
  }

  .actions-cell {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }

  .empty-actions {
    flex-direction: column;
  }
}
</style>