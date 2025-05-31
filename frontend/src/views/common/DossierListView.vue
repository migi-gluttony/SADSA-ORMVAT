<template>
  <div class="dossier-list-container">
    <!-- Header with role-specific title and actions -->
    <div class="list-header">
      <div class="header-info">
        <h1>
          <i class="pi pi-folder-open"></i> 
          {{ getPageTitle() }}
        </h1>
        <p v-if="dossiersData">
          {{ dossiersData.totalCount }} dossier(s) 
          <span v-if="dossiersData.currentUserRole === 'AGENT_ANTENNE'">
            - CDA: {{ dossiersData.currentUserCDA }}
          </span>
          <span v-else-if="dossiersData.currentUserRole === 'AGENT_GUC'">
            - Tous les dossiers soumis
          </span>
        </p>
      </div>
      <div class="header-actions">
        <Button 
          v-if="userRole === 'AGENT_ANTENNE'"
          label="Nouveau Dossier" 
          icon="pi pi-plus" 
          @click="navigateToCreate"
          class="p-button-success btn-primary"
        />
        <Button 
          label="Actualiser" 
          icon="pi pi-refresh" 
          @click="loadDossiers"
          :loading="loading"
          class="p-button-outlined"
        />
        <Button 
          v-if="userRole === 'AGENT_GUC'"
          label="Export Excel" 
          icon="pi pi-file-excel" 
          @click="exportToExcel"
          class="p-button-outlined"
        />
      </div>
    </div>

    <!-- Role-specific statistics -->
    <div v-if="dossiersData?.statistics" class="statistics-section">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ dossiersData.statistics.totalDossiers }}</div>
          <div class="stat-label">Total dossiers</div>
        </div>
        <div class="stat-card" v-if="userRole === 'AGENT_ANTENNE'">
          <div class="stat-value">{{ dossiersData.statistics.dossiersEnCours }}</div>
          <div class="stat-label">En cours</div>
        </div>
        <div class="stat-card" v-if="userRole === 'AGENT_GUC'">
          <div class="stat-value">{{ dossiersData.statistics.dossiersAttenteTraitement }}</div>
          <div class="stat-label">En attente traitement</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ dossiersData.statistics.dossiersApprouves }}</div>
          <div class="stat-label">Approuvés</div>
        </div>
        <div class="stat-card warning" v-if="dossiersData.statistics.dossiersEnRetard > 0">
          <div class="stat-value">{{ dossiersData.statistics.dossiersEnRetard }}</div>
          <div class="stat-label">En retard</div>
        </div>
      </div>
    </div>

    <!-- Enhanced filters with role-specific options -->
    <div class="filters-section">
      <div class="filters-grid">
        <!-- Search Input -->
        <div class="filter-group search-group">
          <label>RECHERCHE</label>
          <div class="search-input-wrapper">
            <i class="pi pi-search search-icon"></i>
            <InputText 
              v-model="searchTerm" 
              placeholder="Rechercher par SABA, CIN, nom..."
              class="search-input"
              @input="handleSearch"
            />
            <button 
              v-if="searchTerm" 
              class="clear-search-btn" 
              @click="clearSearch"
              title="Effacer la recherche"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>
        </div>

        <!-- Status Filter -->
        <div class="filter-group">
          <label>STATUT</label>
          <Select 
            v-model="filters.statut" 
            :options="getStatusOptions()"
            optionLabel="label"
            optionValue="value"
            placeholder="Tous les statuts"
            @change="applyFilters"
            class="filter-select"
            :clearable="true"
          />
        </div>

        <!-- Project Type Filter -->
        <div class="filter-group">
          <label>TYPE DE PROJET</label>
          <Select 
            v-model="filters.sousRubriqueId" 
            :options="sousRubriqueOptions"
            optionLabel="designation"
            optionValue="id"
            placeholder="Tous les types"
            @change="applyFilters"
            class="filter-select"
            :clearable="true"
          />
        </div>

        <!-- Antenne Filter (GUC only) -->
        <div v-if="userRole === 'AGENT_GUC'" class="filter-group">
          <label>ANTENNE</label>
          <Select 
            v-model="filters.antenneId" 
            :options="antenneOptions"
            optionLabel="designation"
            optionValue="id"
            placeholder="Toutes les antennes"
            @change="applyFilters"
            class="filter-select"
            :clearable="true"
          />
        </div>

        <!-- Priority Filter (GUC only) -->
        <div v-if="userRole === 'AGENT_GUC'" class="filter-group">
          <label>PRIORITÉ</label>
          <Select 
            v-model="filters.priorite" 
            :options="prioriteOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Toutes priorités"
            @change="applyFilters"
            class="filter-select"
            :clearable="true"
          />
        </div>

        <!-- Clear Filters -->
        <div class="filter-actions">
          <Button 
            label="Effacer" 
            icon="pi pi-times" 
            @click="clearFilters"
            class="p-button-text btn-clear"
          />
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement des dossiers...</span>
    </div>

    <!-- Empty State -->
    <div v-else-if="dossiers.length === 0" class="empty-state">
      <div class="empty-content">
        <i class="pi pi-folder-open empty-icon"></i>
        <h3>Aucun dossier trouvé</h3>
        <p v-if="hasActiveFilters">Aucun dossier ne correspond à vos critères de recherche et filtres.</p>
        <p v-else-if="userRole === 'AGENT_ANTENNE'">Vous n'avez pas encore créé de dossier.</p>
        <p v-else-if="userRole === 'AGENT_GUC'">Aucun dossier n'a encore été soumis au GUC.</p>
        <div class="empty-actions">
          <Button 
            v-if="hasActiveFilters"
            label="Effacer les filtres" 
            icon="pi pi-filter-slash" 
            @click="clearFilters"
            class="p-button-outlined"
          />
          <Button 
            v-if="userRole === 'AGENT_ANTENNE'"
            label="Créer mon premier dossier" 
            icon="pi pi-plus" 
            @click="navigateToCreate"
            class="p-button-success btn-primary"
          />
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
            <th v-if="userRole === 'AGENT_GUC'" class="col-antenne">Antenne</th>
            <th class="col-status">
              Statut
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th class="col-progress">
              Avancement
              <i class="pi pi-sort-alt sort-icon"></i>
            </th>
            <th v-if="userRole === 'AGENT_GUC'" class="col-priority">Priorité</th>
            <th class="col-details">Délais</th>
            <th class="col-actions">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="dossier in dossiers" 
            :key="dossier.id"
            class="dossier-row"
            :class="{ 
              'urgent': dossier.joursRestants <= 1,
              'warning': dossier.joursRestants <= 2 && dossier.joursRestants > 1
            }"
          >
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

            <!-- Antenne Column (GUC only) -->
            <td v-if="userRole === 'AGENT_GUC'" class="col-antenne">
              <div class="antenne-cell">
                <div class="antenne-name">{{ dossier.antenneDesignation }}</div>
                <div class="cda-name">{{ dossier.cdaNom }}</div>
              </div>
            </td>

            <!-- Status Column -->
            <td class="col-status">
              <Tag 
                :value="dossier.statut" 
                :severity="getStatusSeverity(dossier.statut)"
                class="status-tag"
              />
            </td>

            <!-- Progress Column -->
            <td class="col-progress">
              <div class="progress-cell">
                <div class="progress-info">
                  <span class="progress-text">{{ dossier.formsCompleted }}/{{ dossier.totalForms }} formulaires</span>
                  <span class="progress-percentage">{{ Math.round(dossier.completionPercentage) }}%</span>
                </div>
                <ProgressBar 
                  :value="dossier.completionPercentage" 
                  :show-value="false"
                  class="progress-bar"
                  :class="{
                    'progress-complete': dossier.completionPercentage === 100,
                    'progress-medium': dossier.completionPercentage >= 50 && dossier.completionPercentage < 100,
                    'progress-low': dossier.completionPercentage < 50
                  }"
                />
              </div>
            </td>

            <!-- Priority Column (GUC only) -->
            <td v-if="userRole === 'AGENT_GUC'" class="col-priority">
              <Tag 
                :value="dossier.priorite || 'NORMALE'" 
                :severity="getPrioritySeverity(dossier.priorite)"
                class="priority-tag"
              />
              <div v-if="dossier.notesCount > 0" class="notes-indicator">
                <i class="pi pi-comment"></i>
                <span>{{ dossier.notesCount }}</span>
              </div>
            </td>

            <!-- Details Column -->
            <td class="col-details">
              <div class="details-cell">
                <div class="time-info">
                  <i class="pi pi-clock"></i>
                  <span v-if="dossier.joursRestants > 0" class="time-remaining">
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
                <Button 
                  icon="pi pi-eye" 
                  @click="viewDossierDetail(dossier.id)"
                  class="p-button-outlined p-button-sm action-btn"
                  v-tooltip.top="'Voir détails'"
                />
                
                <!-- Agent Antenne specific actions -->
                <template v-if="userRole === 'AGENT_ANTENNE'">
                  <Button 
                    v-if="dossier.permissions?.peutEtreModifie && dossier.completionPercentage < 100" 
                    icon="pi pi-pencil" 
                    @click="continueDossier(dossier.id)"
                    class="p-button-sm action-btn btn-edit"
                    v-tooltip.top="'Continuer'"
                  />

                  <Button 
                    v-if="dossier.permissions?.peutEtreEnvoye" 
                    icon="pi pi-send" 
                    @click="confirmSendToGUC(dossier)"
                    class="p-button-success p-button-sm action-btn"
                    v-tooltip.top="'Envoyer au GUC'"
                  />

                  <Button 
                    v-if="dossier.permissions?.peutEtreSupprime" 
                    icon="pi pi-trash" 
                    @click="confirmDeleteDossier(dossier)"
                    class="p-button-danger p-button-outlined p-button-sm action-btn"
                    v-tooltip.top="'Supprimer'"
                  />
                </template>

                <!-- Agent GUC specific actions -->
                <template v-if="userRole === 'AGENT_GUC'">
                  <Button 
                    icon="pi pi-comment" 
                    @click="showAddNoteDialog(dossier)"
                    class="p-button-info p-button-sm action-btn"
                    v-tooltip.top="'Ajouter note'"
                  />

                  <SplitButton 
                    v-if="dossier.availableActions && dossier.availableActions.length > 0"
                    :model="getActionMenuItems(dossier)" 
                    class="p-button-sm action-split-btn"
                    @click="handlePrimaryAction(dossier)"
                  >
                    {{ getPrimaryActionLabel(dossier) }}
                  </SplitButton>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
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

    <!-- Action Dialogs -->
    <ActionDialogs 
      :dialogs="actionDialogs"
      @action-confirmed="handleActionConfirmed"
      @dialog-closed="handleDialogClosed"
    />

    <!-- Add Note Dialog (GUC) -->
    <AddNoteDialog 
      v-model:visible="addNoteDialog.visible"
      :dossier="addNoteDialog.dossier"
      @note-added="handleNoteAdded"
    />

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// Import child components
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import AddNoteDialog from '@/components/dossiers/AddNoteDialog.vue';

// PrimeVue components
import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import Paginator from 'primevue/paginator';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const dossiers = ref([]);
const dossiersData = ref(null);
const searchTerm = ref('');
const userRole = ref(AuthService.getCurrentUser()?.role || '');

// Pagination
const currentPage = ref(0);
const pageSize = ref(10);
const totalRecords = computed(() => dossiersData.value?.totalCount || 0);

// Filters
const filters = ref({
  statut: null,
  sousRubriqueId: null,
  antenneId: null,
  priorite: null
});

// Filter options
const sousRubriqueOptions = ref([]);
const antenneOptions = ref([]);
const prioriteOptions = ref([
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

// Dialogs
const actionDialogs = ref({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false },
  delete: { visible: false, dossier: null, loading: false, comment: '' }
});

const addNoteDialog = ref({
  visible: false,
  dossier: null
});

// Computed
const hasActiveFilters = computed(() => {
  const hasSearch = searchTerm.value && searchTerm.value.trim();
  const hasStatus = filters.value.statut;
  const hasSubRubrique = filters.value.sousRubriqueId;
  const hasAntenne = filters.value.antenneId;
  const hasPriority = filters.value.priorite;
  
  return hasSearch || hasStatus || hasSubRubrique || hasAntenne || hasPriority;
});

// Methods
onMounted(() => {
  loadDossiers();
  loadFilterOptions();
});

function getPageTitle() {
  switch (userRole.value) {
    case 'AGENT_ANTENNE':
      return 'Mes Dossiers';
    case 'AGENT_GUC':
      return 'Dossiers - Guichet Unique Central';
    case 'ADMIN':
      return 'Tous les Dossiers';
    default:
      return 'Dossiers';
  }
}

function getStatusOptions() {
  const commonOptions = [
    { label: 'Soumis', value: 'SUBMITTED' },
    { label: 'En révision', value: 'IN_REVIEW' },
    { label: 'Approuvé', value: 'APPROVED' },
    { label: 'Rejeté', value: 'REJECTED' },
    { label: 'Terminé', value: 'COMPLETED' }
  ];

  if (userRole.value === 'AGENT_ANTENNE') {
    return [
      { label: 'Brouillon', value: 'DRAFT' },
      { label: 'Retourné pour complétion', value: 'RETURNED_FOR_COMPLETION' },
      ...commonOptions
    ];
  }

  return commonOptions;
}

async function loadDossiers() {
  try {
    loading.value = true;
    
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'dateCreation',
      sortDirection: 'DESC'
    };

    // Add search and filters
    if (searchTerm.value?.trim()) params.searchTerm = searchTerm.value.trim();
    if (filters.value.statut) params.statut = filters.value.statut;
    if (filters.value.sousRubriqueId) params.sousRubriqueId = filters.value.sousRubriqueId;
    if (filters.value.antenneId) params.antenneId = filters.value.antenneId;
    if (filters.value.priorite) params.priorite = filters.value.priorite;

    console.log('Loading dossiers with params:', params);

    const response = await ApiService.get('/dossiers', params);
    
    dossiersData.value = response;
    dossiers.value = response.dossiers || [];
    
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

async function loadFilterOptions() {
  try {
    // Load sous-rubriques for filter
    const initResponse = await ApiService.get('/agent_antenne/dossiers/initialization-data');
    if (initResponse.rubriques) {
      const allSousRubriques = [];
      initResponse.rubriques.forEach(rubrique => {
        rubrique.sousRubriques.forEach(sr => {
          allSousRubriques.push({
            id: sr.id,
            designation: sr.designation
          });
        });
      });
      sousRubriqueOptions.value = allSousRubriques;
    }

    // Load antennes for GUC filter
    if (userRole.value === 'AGENT_GUC') {
      antenneOptions.value = initResponse.antennes || [];
    }
  } catch (error) {
    console.error('Erreur lors du chargement des options de filtre:', error);
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
  filters.value = {
    statut: null,
    sousRubriqueId: null,
    antenneId: null,
    priorite: null
  };
  searchTerm.value = '';
  applyFilters();
}

function onPageChange(event) {
  currentPage.value = event.page;
  pageSize.value = event.rows;
  loadDossiers();
}

// Navigation methods
function navigateToCreate() {
  router.push('/agent_antenne/dossiers/create');
}

function viewDossierDetail(dossierId) {
  const route = userRole.value === 'AGENT_ANTENNE' 
    ? `/agent_antenne/dossiers/${dossierId}`
    : `/agent_guc/dossiers/${dossierId}`;
  router.push(route);
}

function continueDossier(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}/forms`);
}

// Action methods
function confirmSendToGUC(dossier) {
  actionDialogs.value.sendToGUC = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmDeleteDossier(dossier) {
  actionDialogs.value.delete = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function showAddNoteDialog(dossier) {
  addNoteDialog.value = {
    visible: true,
    dossier: dossier
  };
}

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
  actionDialogs.value[action] = {
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
      case 'sendToGUC':
        endpoint = `/dossiers/${dossier.id}/send-to-guc`;
        payload = { commentaire: data.comment };
        break;
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
      
      loadDossiers();
    }
    
  } catch (error) {
    console.error(`Erreur lors de l'action ${actionData.action}:`, error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Une erreur est survenue',
      life: 3000
    });
  }
}

function handleDialogClosed() {
  // Reset all dialogs
  Object.keys(actionDialogs.value).forEach(key => {
    actionDialogs.value[key].visible = false;
  });
}

async function handleNoteAdded() {
  addNoteDialog.value.visible = false;
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Note ajoutée avec succès',
    life: 3000
  });
  
  // Reload dossiers to update notes count
  await loadDossiers();
}

async function exportToExcel() {
  try {
    const response = await fetch('/api/dossiers/export', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${AuthService.getToken()}`
      },
      body: JSON.stringify(filters.value)
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
    
  } catch (error) {
    console.error('Erreur lors de l\'export:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'exporter les dossiers',
      life: 3000
    });
  }
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

function getPrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
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
/* Copy all styles from the original DossierListView.vue and add some new ones */

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

/* Adjust for GUC filters */
.filters-grid:has(.filter-group:nth-child(5)) {
  grid-template-columns: 1fr 150px 200px 150px 150px auto;
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

.search-input:focus + .search-icon {
  color: var(--primary-color);
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
.loading-container {
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

.col-antenne {
  width: 15%;
  min-width: 150px;
}

.col-status {
  width: 12%;
  min-width: 120px;
}

.col-progress {
  width: 15%;
  min-width: 150px;
}

.col-priority {
  width: 10%;
  min-width: 100px;
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

.status-tag {
  font-size: 0.75rem !important;
  font-weight: 600 !important;
  padding: 0.375rem 0.75rem !important;
  border-radius: var(--border-radius-sm) !important;
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

.action-split-btn {
  height: 2.5rem !important;
}

:deep(.action-split-btn .p-button) {
  padding: 0.5rem 0.75rem !important;
  font-size: 0.75rem !important;
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
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>