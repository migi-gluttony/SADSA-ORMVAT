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

