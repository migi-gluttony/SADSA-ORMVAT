<template>
  <div class="terrain-visits-container p-4">
    <!-- Header -->
    <div class="flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold text-900">
          <i class="pi pi-map mr-2"></i>
          Visites Terrain - Commission {{ getCommissionTeamDisplayName() }}
        </h1>
        <p class="text-600" v-if="visits.length > 0">
          {{ visits.length }} visite(s) trouvée(s)
        </p>
      </div>
      <div class="flex gap-2">
        <Button 
          label="Dossiers" 
          icon="pi pi-folder" 
          @click="navigateToDossiers" 
          class="p-button-outlined" 
        />
        <Button 
          label="Actualiser" 
          icon="pi pi-refresh" 
          @click="loadVisits" 
          :loading="loading"
          class="p-button-outlined" 
        />
        <Button 
          label="Export Excel" 
          icon="pi pi-file-excel" 
          @click="exportToExcel"
          class="p-button-outlined" 
        />
      </div>
    </div>

    <!-- Dashboard Summary -->
    <div class="grid mb-4" v-if="dashboardSummary">
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-blue-500">{{ dashboardSummary.visitesAujourdHui || 0 }}</div>
              <div class="text-color-secondary">Visites aujourd'hui</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-orange-500">{{ dashboardSummary.visitesEnAttente || 0 }}</div>
              <div class="text-color-secondary">En attente</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-500">{{ dashboardSummary.statistiques?.visitesApprouvees || 0 }}</div>
              <div class="text-color-secondary">Approuvées</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3" v-if="(dashboardSummary.visitesEnRetard || 0) > 0">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-red-500">{{ dashboardSummary.visitesEnRetard }}</div>
              <div class="text-color-secondary">En retard</div>
            </div>
          </template>
        </Card>
      </div>
    </div>

    <!-- Filters -->
    <Card class="mb-4">
      <template #content>
        <div class="grid">
          <div class="col-12 md:col-3">
            <label class="block text-900 font-medium mb-2">Recherche</label>
            <InputText 
              v-model="searchTerm" 
              placeholder="Rechercher par référence, agriculteur..." 
              class="w-full"
              @input="handleSearch"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Statut</label>
            <Dropdown 
              v-model="filters.statut" 
              :options="statusOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les statuts" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Date début</label>
            <Calendar 
              v-model="filters.dateDebut" 
              dateFormat="dd/mm/yy"
              placeholder="Date début" 
              class="w-full"
              @date-select="applyFilters"
              showIcon
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Date fin</label>
            <Calendar 
              v-model="filters.dateFin" 
              dateFormat="dd/mm/yy"
              placeholder="Date fin" 
              class="w-full"
              @date-select="applyFilters"
              showIcon
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Filtres</label>
            <div class="flex gap-2">
              <Checkbox 
                v-model="filters.enRetard" 
                inputId="enRetard"
                @change="applyFilters"
              />
              <label for="enRetard" class="text-sm">En retard</label>
            </div>
            <div class="flex gap-2 mt-1">
              <Checkbox 
                v-model="filters.aCompleter" 
                inputId="aCompleter"
                @change="applyFilters"
              />
              <label for="aCompleter" class="text-sm">À compléter</label>
            </div>
          </div>
          <div class="col-12 md:col-1 flex align-items-end">
            <Button 
              label="Effacer" 
              icon="pi pi-times" 
              @click="clearFilters" 
              class="p-button-text w-full" 
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Loading State -->
    <div v-if="loading" class="text-center p-6">
      <ProgressSpinner size="50px" />
      <div class="mt-3">Chargement des visites terrain...</div>
    </div>

    <!-- Error State -->
    <Card v-else-if="error" class="text-center">
      <template #content>
        <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-3"></i>
        <h3>Erreur de chargement</h3>
        <p class="text-600">{{ error }}</p>
        <Button 
          label="Réessayer" 
          icon="pi pi-refresh" 
          @click="loadVisits" 
          class="p-button-outlined mt-3" 
        />
      </template>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="visits.length === 0" class="text-center">
      <template #content>
        <i class="pi pi-calendar text-4xl text-600 mb-3"></i>
        <h3>Aucune visite trouvée</h3>
        <p class="text-600" v-if="hasActiveFilters">
          Aucune visite ne correspond à vos critères de recherche.
        </p>
        <p class="text-600" v-else>
          Aucune visite terrain n'est programmée pour votre équipe.
        </p>
        <div class="mt-3">
          <Button 
            v-if="hasActiveFilters" 
            label="Effacer les filtres" 
            icon="pi pi-filter-slash" 
            @click="clearFilters"
            class="p-button-outlined" 
          />
        </div>
      </template>
    </Card>

    <!-- Visits Table -->
    <Card v-else>
      <template #content>
        <DataTable 
          :value="visits" 
          :paginator="true" 
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
          currentPageReportTemplate="Affichage de {first} à {last} sur {totalRecords} visites"
          responsiveLayout="scroll"
          :sortField="sortField"
          :sortOrder="sortOrder"
          @sort="onSort"
        >
          <Column field="dossierReference" header="Dossier" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.dossierReference }}</div>
                <div class="text-sm text-600">SABA: {{ slotProps.data.saba }}</div>
              </div>
            </template>
          </Column>

          <Column field="agriculteurNom" header="Agriculteur" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.agriculteurPrenom }} {{ slotProps.data.agriculteurNom }}</div>
                <div class="text-sm text-600">
                  <i class="pi pi-id-card mr-1"></i>
                  {{ slotProps.data.agriculteurCin }}
                </div>
                <div class="text-sm text-600" v-if="slotProps.data.agriculteurCommune">
                  <i class="pi pi-map-marker mr-1"></i>
                  {{ slotProps.data.agriculteurCommune }}{{ slotProps.data.agriculteurDouar ? ' - ' + slotProps.data.agriculteurDouar : '' }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="sousRubriqueDesignation" header="Type de Projet">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">{{ slotProps.data.sousRubriqueDesignation }}</div>
                <div class="text-sm text-600">{{ slotProps.data.rubriqueDesignation }}</div>
                <div class="text-sm text-600">{{ slotProps.data.antenneDesignation }}</div>
              </div>
            </template>
          </Column>

          <Column field="dateVisite" header="Date de Visite" :sortable="true">
            <template #body="slotProps">
              <div class="text-center">
                <div class="font-semibold">{{ formatDate(slotProps.data.dateVisite) }}</div>
                <div v-if="slotProps.data.daysUntilVisit !== null" class="text-sm" :class="getDaysUntilClass(slotProps.data)">
                  {{ getDaysUntilText(slotProps.data) }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="statut" header="Statut" :sortable="true">
            <template #body="slotProps">
              <div class="text-center">
                <Tag 
                  :value="getStatusLabel(slotProps.data.statut)" 
                  :severity="getStatusSeverity(slotProps.data.statut)" 
                />
                <div v-if="slotProps.data.dateConstat" class="text-sm text-600 mt-1">
                  Constat: {{ formatDate(slotProps.data.dateConstat) }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="photosCount" header="Photos">
            <template #body="slotProps">
              <div class="text-center">
                <div class="flex align-items-center justify-content-center gap-1">
                  <i class="pi pi-image"></i>
                  <span>{{ slotProps.data.photosCount || 0 }}</span>
                </div>
              </div>
            </template>
          </Column>

          <Column field="actions" header="Actions">
            <template #body="slotProps">
              <div class="flex gap-1 justify-content-center">
                <!-- View Details -->
                <Button 
                  icon="pi pi-eye" 
                  @click="viewVisitDetail(slotProps.data)"
                  class="p-button-outlined p-button-sm" 
                  v-tooltip.top="'Voir détails'" 
                />
                
                <!-- Complete Visit -->
                <Button 
                  v-if="slotProps.data.canComplete"
                  icon="pi pi-check-circle" 
                  @click="showCompleteVisitDialog(slotProps.data)"
                  class="p-button-success p-button-sm" 
                  v-tooltip.top="'Finaliser visite'" 
                />

                <!-- Edit Visit -->
                <Button 
                  v-if="slotProps.data.canModify"
                  icon="pi pi-pencil" 
                  @click="editVisit(slotProps.data)"
                  class="p-button-warning p-button-outlined p-button-sm" 
                  v-tooltip.top="'Modifier'" 
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Visit Detail Dialog -->
    <Dialog 
      v-model:visible="visitDetailDialog.visible" 
      modal 
      :header="visitDetailDialog.visit ? 'Détails Visite - ' + visitDetailDialog.visit.dossierReference : 'Détails de la Visite'"
      :style="{ width: '90vw', maxWidth: '1200px', height: '90vh' }"
      :maximizable="true"
    >
      <VisitDetailComponent
        v-if="visitDetailDialog.visit"
        :visit="visitDetailDialog.visit"
        @visit-updated="handleVisitUpdated"
        @close="visitDetailDialog.visible = false"
        @complete-visit="showCompleteVisitDialog"
      />
    </Dialog>    <!-- Complete Visit Dialog -->
    <CompleteTerrainVisitDialog 
      v-if="completeVisitDialog.visit"
      v-model:visible="completeVisitDialog.visible"
      :visit="completeVisitDialog.visit"
      @visit-completed="handleVisitCompleted"
      @close="completeVisitDialog.visible = false"
    />

    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// Components
import VisitDetailComponent from '@/components/agent_commission/VisitDetailComponent.vue';
import CompleteTerrainVisitDialog from '@/components/agent_commission/CompleteVisitComponent.vue';

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import Checkbox from 'primevue/checkbox';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const error = ref(null);
const visits = ref([]);
const dashboardSummary = ref(null);
const searchTerm = ref('');
const sortField = ref('dateVisite');
const sortOrder = ref(1);

// Filters
const filters = reactive({
  statut: null,
  dateDebut: null,
  dateFin: null,
  enRetard: null,
  aCompleter: null
});

// Filter options
const statusOptions = ref([
  { label: 'Nouvelle', value: 'NOUVELLE' },
  { label: 'Programmée', value: 'PROGRAMMEE' },
  { label: 'En cours', value: 'EN_COURS' },
  { label: 'En retard', value: 'EN_RETARD' },
  { label: 'Complétée', value: 'COMPLETEE' },
  { label: 'Approuvée', value: 'APPROUVEE' },
  { label: 'Rejetée', value: 'REJETEE' }
]);

// Dialogs
const visitDetailDialog = ref({
  visible: false,
  visit: null
});

const completeVisitDialog = ref({
  visible: false,
  visit: null
});

// Computed
const hasActiveFilters = computed(() => {
  return searchTerm.value?.trim() || 
         filters.statut || 
         filters.dateDebut || 
         filters.dateFin ||
         filters.enRetard || 
         filters.aCompleter;
});

// Methods
onMounted(() => {
  loadDashboard();
  loadVisits();
});

async function loadDashboard() {
  try {
    const response = await ApiService.get('/agent_commission/terrain-visits/dashboard');
    dashboardSummary.value = response;
  } catch (err) {
    console.error('Erreur lors du chargement du tableau de bord:', err);
  }
}

async function loadVisits() {
  try {
    loading.value = true;
    error.value = null;

    const params = {};
    
    // Add search and filters
    if (searchTerm.value?.trim()) params.searchTerm = searchTerm.value.trim();
    if (filters.statut) params.statut = filters.statut;
    if (filters.dateDebut) params.dateDebut = formatDateForAPI(filters.dateDebut);
    if (filters.dateFin) params.dateFin = formatDateForAPI(filters.dateFin);
    if (filters.enRetard) params.enRetard = filters.enRetard;
    if (filters.aCompleter) params.aCompleter = filters.aCompleter;
    
    // Add sorting
    params.sortBy = sortField.value;
    params.sortDirection = sortOrder.value === 1 ? 'ASC' : 'DESC';

    const response = await ApiService.get('/agent_commission/terrain-visits', params);
    visits.value = response.visites || [];
    
  } catch (err) {
    console.error('Erreur lors du chargement des visites:', err);
    error.value = err.message || 'Impossible de charger les visites';
    visits.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // Debounce search
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    applyFilters();
  }, 500);
}

let searchTimeout = null;

function applyFilters() {
  loadVisits();
}

function clearFilters() {
  Object.assign(filters, {
    statut: null,
    dateDebut: null,
    dateFin: null,
    enRetard: null,
    aCompleter: null
  });
  searchTerm.value = '';
  loadVisits();
}

function onSort(event) {
  sortField.value = event.sortField;
  sortOrder.value = event.sortOrder;
  loadVisits();
}

function getCommissionTeamDisplayName() {
  const currentUser = AuthService.getCurrentUser();
  const equipe = currentUser?.equipeCommission;
  
  if (!equipe) return 'Générale';
  
  const teamNames = {
    'FILIERES_VEGETALES': 'Filières Végétales',
    'FILIERES_ANIMALES': 'Filières Animales', 
    'AMENAGEMENT_HYDRO_AGRICOLE': 'Aménagement Hydro-Agricole'
  };
  
  return teamNames[equipe] || equipe;
}

function navigateToDossiers() {
  router.push('/agent_commission/dossiers');
}

async function viewVisitDetail(visit) {
  try {
    // Load detailed visit information
    const response = await ApiService.get(`/agent_commission/terrain-visits/${visit.id}`);
    visitDetailDialog.value = {
      visible: true,
      visit: response
    };
  } catch (err) {
    console.error('Erreur lors du chargement des détails:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les détails de la visite',
      life: 3000
    });
  }
}

function showCompleteVisitDialog(visit) {
  completeVisitDialog.value = {
    visible: true,
    visit: visit
  };
}

function editVisit(visit) {
  // Open edit dialog or navigate to edit page
  viewVisitDetail(visit);
}

function handleVisitUpdated() {
  // Reload the visits list
  loadVisits();
  loadDashboard();
}

function handleVisitCompleted() {
  completeVisitDialog.value.visible = false;
  handleVisitUpdated();
  
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain finalisée avec succès',
    life: 4000
  });
}

// Utility functions
function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}

function formatDateForAPI(date) {
  if (!date) return null;
  return date.toISOString().split('T')[0];
}

function getDaysUntilClass(visit) {
  if (visit.isOverdue) return 'text-red-500 font-semibold';
  if (visit.daysUntilVisit <= 1) return 'text-orange-500 font-semibold';
  return 'text-600';
}

function getDaysUntilText(visit) {
  if (visit.isOverdue) return 'En retard';
  if (visit.daysUntilVisit === 0) return 'Aujourd\'hui';
  if (visit.daysUntilVisit === 1) return 'Demain';
  if (visit.daysUntilVisit > 1) return `Dans ${visit.daysUntilVisit} jour(s)`;
  return '';
}

function getStatusLabel(status) {
  const statusMap = {
    'NOUVELLE': 'Nouvelle',
    'PROGRAMMEE': 'Programmée',
    'EN_COURS': 'En cours',
    'EN_RETARD': 'En retard',
    'COMPLETEE': 'Complétée',
    'APPROUVEE': 'Approuvée',
    'REJETEE': 'Rejetée'
  };
  return statusMap[status] || status;
}

function getStatusSeverity(status) {
  const severityMap = {
    'NOUVELLE': 'secondary',
    'PROGRAMMEE': 'info',
    'EN_COURS': 'warning',
    'EN_RETARD': 'danger',
    'COMPLETEE': 'warning',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severityMap[status] || 'secondary';
}

async function exportToExcel() {
  try {
    const response = await fetch('/api/agent_commission/terrain-visits/export', {
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
    link.setAttribute('download', `visites_terrain_${new Date().toISOString().split('T')[0]}.xlsx`);
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
      detail: 'Impossible d\'exporter les visites',
      life: 3000
    });
  }
}
</script>

<style scoped>
.terrain-visits-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
</style>