<template>
  <div class="dossier-list-container p-4">
    <!-- Header -->
    <div class="flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold text-900">
          <i class="pi pi-map mr-2"></i>
          Dossiers - Commission Visite Terrain
        </h1>
        <p class="text-600" v-if="dossiers.length > 0">
          {{ dossiers.length }} dossier(s) trouvé(s) - {{ getCommissionTeamDisplayName() }}
        </p>
      </div>
      <div class="flex gap-2">
        <Button 
          label="Visites Terrain" 
          icon="pi pi-calendar" 
          @click="navigateToVisits" 
          class="p-button-info" 
        />
        <Button 
          label="Actualiser" 
          icon="pi pi-refresh" 
          @click="loadDossiers" 
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

    <!-- Statistics Cards -->
    <div class="grid mb-4" v-if="statistics">
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-primary">{{ statistics.totalDossiers || 0 }}</div>
              <div class="text-color-secondary">Total dossiers</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-orange-500">{{ statistics.dossiersEnCommission || 0 }}</div>
              <div class="text-color-secondary">En commission</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-500">{{ statistics.dossiersApprouves || 0 }}</div>
              <div class="text-color-secondary">Terrain approuvé</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3" v-if="statistics.dossiersEnRetard > 0">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-red-500">{{ statistics.dossiersEnRetard }}</div>
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
              placeholder="Rechercher par SABA, CIN, nom..." 
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
            <label class="block text-900 font-medium mb-2">Type de Projet</label>
            <Dropdown 
              v-model="filters.sousRubriqueId" 
              :options="sousRubriqueOptions" 
              optionLabel="designation" 
              optionValue="id"
              placeholder="Tous les types" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Antenne</label>
            <Dropdown 
              v-model="filters.antenneId" 
              :options="antenneOptions" 
              optionLabel="designation" 
              optionValue="id"
              placeholder="Toutes les antennes" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Priorité</label>
            <Dropdown 
              v-model="filters.priorite" 
              :options="prioriteOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Toutes priorités" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
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
      <div class="mt-3">Chargement des dossiers...</div>
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
          @click="loadDossiers" 
          class="p-button-outlined mt-3" 
        />
      </template>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="dossiers.length === 0" class="text-center">
      <template #content>
        <i class="pi pi-map text-4xl text-600 mb-3"></i>
        <h3>Aucun dossier trouvé</h3>
        <p class="text-600" v-if="hasActiveFilters">
          Aucun dossier ne correspond à vos critères de recherche.
        </p>
        <p class="text-600" v-else>
          Aucun dossier n'est assigné à votre équipe ({{ getCommissionTeamDisplayName() }}) pour visite terrain.
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

    <!-- Dossiers Table -->
    <Card v-else>
      <template #content>
        <DataTable 
          :value="dossiers" 
          :paginator="true" 
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
          currentPageReportTemplate="Affichage de {first} à {last} sur {totalRecords} dossiers"
          responsiveLayout="scroll"
        >
          <Column field="reference" header="Référence" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.reference }}</div>
                <div class="text-sm text-600">SABA: {{ slotProps.data.saba }}</div>
              </div>
            </template>
          </Column>

          <Column field="agriculteurNom" header="Agriculteur" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.agriculteurNom }}</div>
                <div class="text-sm text-600">
                  <i class="pi pi-id-card mr-1"></i>
                  {{ slotProps.data.agriculteurCin }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="sousRubriqueDesignation" header="Type de Projet">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">{{ slotProps.data.sousRubriqueDesignation }}</div>
                <div class="text-sm text-600" v-if="slotProps.data.montantSubvention">
                  <i class="pi pi-money-bill mr-1"></i>
                  {{ formatCurrency(slotProps.data.montantSubvention) }}
                </div>
              </div>
            </template>
          </Column>

          <Column header="Antenne">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">{{ slotProps.data.antenneDesignation }}</div>
                <div class="text-sm text-600">{{ slotProps.data.cdaNom }}</div>
              </div>
            </template>
          </Column>

          <Column header="Inspection Terrain">
            <template #body="slotProps">
              <div class="inspection-cell">
                <div v-if="slotProps.data.visiteTerrainStatus" class="inspection-status">
                  <Tag 
                    :value="getInspectionStatusLabel(slotProps.data.visiteTerrainStatus)"
                    :severity="getInspectionSeverity(slotProps.data.visiteTerrainStatus)" 
                  />
                </div>
                <div v-else class="inspection-pending">
                  <Tag value="À programmer" severity="warning" />
                </div>
                <div v-if="slotProps.data.dateVisite" class="inspection-date">
                  <i class="pi pi-calendar mr-1"></i>
                  {{ formatDate(slotProps.data.dateVisite) }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="statut" header="Statut" :sortable="true">
            <template #body="slotProps">
              <Tag 
                :value="getStatusLabel(slotProps.data.statut)" 
                :severity="getStatusSeverity(slotProps.data.statut)" 
              />
            </template>
          </Column>

          <Column field="timing" header="Délais">
            <template #body="slotProps">
              <div class="text-sm">
                <div class="flex align-items-center mb-1">
                  <i class="pi pi-clock mr-1"></i>
                  <span 
                    v-if="slotProps.data.joursRestants > 0" 
                    class="text-600"
                  >
                    {{ slotProps.data.joursRestants }} jour(s) restant(s)
                  </span>
                  <span 
                    v-else-if="slotProps.data.enRetard" 
                    class="text-red-500 font-semibold"
                  >
                    En retard ({{ slotProps.data.joursRetard }} jour(s))
                  </span>
                  <span v-else class="text-600">Délai respecté</span>
                </div>
                <div class="text-xs text-500">
                  Créé le {{ formatDate(slotProps.data.dateCreation) }}
                </div>
              </div>
            </template>
          </Column>          <Column field="actions" header="Actions">
            <template #body="slotProps">
              <div class="flex gap-1">
                <!-- View Details -->
                <Button 
                  icon="pi pi-eye" 
                  @click="viewDossierDetail(slotProps.data.id)"
                  class="p-button-outlined p-button-sm" 
                  v-tooltip.top="'Voir détails'" 
                />
                
                <!-- Add Note -->
                <Button 
                  icon="pi pi-comment" 
                  @click="showAddNoteDialog(slotProps.data)"
                  class="p-button-info p-button-outlined p-button-sm" 
                  v-tooltip.top="'Ajouter note'" 
                />

                <!-- Schedule Visit Button - only if no visit is scheduled or needs to be programmed -->
                <Button 
                  v-if="canScheduleVisit(slotProps.data)"
                  icon="pi pi-calendar-plus" 
                  @click="confirmScheduleVisit(slotProps.data)"
                  class="p-button-info p-button-sm" 
                  v-tooltip.top="'Programmer visite terrain'" 
                />

                <!-- Complete/Finalize Visit Button - when visit is scheduled but not completed -->
                <Button 
                  v-if="canFinalizeVisit(slotProps.data)"
                  icon="pi pi-check-circle" 
                  @click="showFinalizeVisitDialog(slotProps.data)"
                  class="p-button-success p-button-sm" 
                  v-tooltip.top="'Finaliser visite terrain'" 
                />

                <!-- Visit Status Indicator -->
                <div v-if="slotProps.data.visiteTerrainStatus === 'APPROUVEE'" class="ml-2">
                  <i class="pi pi-check-circle text-green-500" v-tooltip.top="'Terrain approuvé'"></i>
                </div>
                <div v-else-if="slotProps.data.visiteTerrainStatus === 'REJETEE'" class="ml-2">
                  <i class="pi pi-times-circle text-red-500" v-tooltip.top="'Terrain rejeté'"></i>
                </div>
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Schedule Visit Dialog -->
    <Dialog 
      v-model:visible="scheduleVisitDialog.visible" 
      modal 
      header="Programmer Visite Terrain"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-calendar-plus text-2xl mr-3"></i>
        <div>
          <p>Programmer une visite terrain pour ce dossier ?</p>
          <div class="mt-2 p-2 bg-blue-50 border-round">
            <strong>{{ scheduleVisitDialog.dossier?.reference }}</strong><br>
            {{ scheduleVisitDialog.dossier?.agriculteurNom }}<br>
            <small>Type: {{ scheduleVisitDialog.dossier?.sousRubriqueDesignation }}</small>
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="visitDate">Date de visite *</label>
        <Calendar 
          id="visitDate"
          v-model="scheduleVisitDialog.dateVisite" 
          dateFormat="dd/mm/yy"
          :minDate="new Date()"
          placeholder="Sélectionner une date"
          class="w-full"
          :class="{ 'p-invalid': !scheduleVisitDialog.dateVisite }"
          showIcon
        />
      </div>
      
      <div class="field">
        <label for="visitComment">Commentaire *</label>
        <Textarea 
          id="visitComment"
          v-model="scheduleVisitDialog.comment" 
          rows="3" 
          placeholder="Instructions pour la visite terrain..."
          class="w-full"
          :class="{ 'p-invalid': !scheduleVisitDialog.comment?.trim() }"
        />
      </div>

      <p class="text-600 text-sm">
        La visite terrain sera programmée pour évaluer la conformité du projet.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="scheduleVisitDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Programmer la Visite" 
          icon="pi pi-calendar-plus" 
          @click="scheduleVisit"
          class="p-button-info"
          :loading="scheduleVisitDialog.loading"
          :disabled="!scheduleVisitDialog.dateVisite || !scheduleVisitDialog.comment?.trim()"
        />
      </template>
    </Dialog>    <!-- Complete Visit Dialog -->
    <CompleteVisitComponent 
      v-if="finalizeVisitDialog.visit"
      v-model:visible="finalizeVisitDialog.visible"
      :visit="finalizeVisitDialog.visit"
      @visit-completed="handleVisitCompleted"
      @close="finalizeVisitDialog.visible = false"
    />

    <!-- Add Note Dialog -->
    <Dialog 
      v-model:visible="addNoteDialog.visible" 
      modal 
      header="Ajouter une Note"
      :style="{ width: '500px' }"
    >
      <div class="field">
        <label for="noteContent">Note *</label>
        <Textarea 
          id="noteContent"
          v-model="addNoteDialog.content" 
          rows="4" 
          placeholder="Entrez votre note..."
          class="w-full"
          :class="{ 'p-invalid': !addNoteDialog.content?.trim() }"
        />
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="addNoteDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Ajouter" 
          icon="pi pi-plus" 
          @click="addNote"
          class="p-button-primary"
          :loading="addNoteDialog.loading"
          :disabled="!addNoteDialog.content?.trim()"
        />
      </template>
    </Dialog>

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
import CompleteVisitComponent from '@/components/agent_commission/CompleteVisitComponent.vue';

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import Textarea from 'primevue/textarea';
import Calendar from 'primevue/calendar';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const error = ref(null);
const dossiers = ref([]);
const statistics = ref(null);
const searchTerm = ref('');

// Filters
const filters = reactive({
  statut: null,
  sousRubriqueId: null,
  antenneId: null,
  priorite: null
});

// Filter options
const statusOptions = ref([
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
]);

const sousRubriqueOptions = ref([
  { id: 1, designation: 'FILIERES VEGETALES - Équipements' },
  { id: 2, designation: 'FILIERES VEGETALES - Matériel' },
  { id: 3, designation: 'FILIERES ANIMALES - Équipements' },
  { id: 4, designation: 'FILIERES ANIMALES - Installations' },
  { id: 5, designation: 'AMENAGEMENT HYDRO-AGRICOLE' },
  { id: 6, designation: 'AMELIORATION FONCIERE' }
]);

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

// Dialogs
const scheduleVisitDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  dateVisite: null,
  comment: ''
});

const finalizeVisitDialog = ref({
  visible: false,
  visit: null
});

const addNoteDialog = ref({
  visible: false,
  dossier: null,
  content: '',
  loading: false
});

// Computed
const hasActiveFilters = computed(() => {
  return searchTerm.value?.trim() || filters.statut || filters.sousRubriqueId || filters.antenneId || filters.priorite;
});

// Methods
onMounted(() => {
  loadDossiers();
});

async function loadDossiers() {
  try {
    loading.value = true;
    error.value = null;

    const params = {};
    
    // Add search and filters
    if (searchTerm.value?.trim()) params.searchTerm = searchTerm.value.trim();
    if (filters.statut) params.statut = filters.statut;
    if (filters.sousRubriqueId) params.sousRubriqueId = filters.sousRubriqueId;
    if (filters.antenneId) params.antenneId = filters.antenneId;
    if (filters.priorite) params.priorite = filters.priorite;

    const response = await ApiService.get('/agent-commission/dossiers', params);
    dossiers.value = response.dossiers || [];
    
    // Calculate statistics from loaded data
    calculateStatistics();
    
  } catch (err) {
    console.error('Erreur lors du chargement des dossiers:', err);
    error.value = err.message || 'Impossible de charger les dossiers';
    dossiers.value = [];
    statistics.value = null;
  } finally {
    loading.value = false;
  }
}

function calculateStatistics() {
  const total = dossiers.value.length;
  const enCommission = dossiers.value.filter(d => d.statut === 'IN_REVIEW').length;  const approuves = dossiers.value.filter(d => d.statut === 'APPROVED' || d.statut === 'COMPLETED').length;
  const enRetard = dossiers.value.filter(d => d.enRetard).length;

  statistics.value = {
    totalDossiers: total,
    dossiersEnCommission: enCommission,
    dossiersApprouves: approuves,
    dossiersEnRetard: enRetard
  };
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
  loadDossiers();
}

function clearFilters() {
  Object.assign(filters, {
    statut: null,
    sousRubriqueId: null,
    antenneId: null,
    priorite: null
  });
  searchTerm.value = '';
  loadDossiers();
}

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

function navigateToVisits() {
  router.push('/agent_commission/terrain-visits');
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_commission/dossiers/${dossierId}`);
}

// Visit scheduling logic
function hasScheduledVisit(dossier) {
  return dossier.visiteTerrainStatus && dossier.visiteTerrainStatus !== 'A_PROGRAMMER';
}

function canScheduleVisit(dossier) {
  return dossier.statut === 'IN_REVIEW' && 
         (!dossier.visiteTerrainStatus || dossier.visiteTerrainStatus === 'A_PROGRAMMER');
}

function canFinalizeVisit(dossier) {
  return dossier.visiteTerrainStatus && 
         (dossier.visiteTerrainStatus === 'PROGRAMMEE' || 
          dossier.visiteTerrainStatus === 'EN_COURS' ||
          dossier.visiteTerrainStatus === 'COMPLETEE');
}

// Dialog functions
function confirmScheduleVisit(dossier) {
  scheduleVisitDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    dateVisite: null,
    comment: ''  };
}

function showAddNoteDialog(dossier) {
  addNoteDialog.value = {
    visible: true,
    dossier: dossier,
    content: '',
    loading: false
  };
}

// Action methods
async function scheduleVisit() {
  try {
    scheduleVisitDialog.value.loading = true;
    
    const dossier = scheduleVisitDialog.value.dossier;
    const endpoint = `/agent_commission/terrain-visits/schedule`;
    const payload = { 
      dossierId: dossier.id,
      dateVisite: scheduleVisitDialog.value.dateVisite.toISOString().split('T')[0],
      observations: scheduleVisitDialog.value.comment 
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite terrain programmée',
        life: 3000
      });
      
      scheduleVisitDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    scheduleVisitDialog.value.loading = false;
  }
}

function handleVisitCompleted() {
  finalizeVisitDialog.value.visible = false;
  loadDossiers();
  
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain finalisée avec succès',
    life: 4000
  });
}

async function showFinalizeVisitDialog(dossier) {
  try {
    // Load the visit data for this dossier
    const response = await ApiService.get(`/agent_commission/terrain-visits/by-dossier/${dossier.id}`);
    
    if (response && response.id) {
      finalizeVisitDialog.value = {
        visible: true,
        visit: response
      };
    } else {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Impossible de charger les données de la visite terrain',
        life: 4000
      });
    }
  } catch (err) {
    console.error('Erreur lors du chargement de la visite:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les données de la visite terrain',
      life: 4000
    });
  }
}

async function addNote() {
  try {
    addNoteDialog.value.loading = true;

    // Simulate note API call - replace with actual endpoint when available
    await new Promise(resolve => setTimeout(resolve, 1000));

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Note ajoutée avec succès',
      life: 3000
    });

    addNoteDialog.value.visible = false;
    setTimeout(() => loadDossiers(), 500);

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'ajouter la note',
      life: 3000
    });
  } finally {
    addNoteDialog.value.loading = false;
  }
}

// Utility functions
function getInspectionStatusLabel(status) {
  const statusMap = {
    'A_PROGRAMMER': 'À programmer',
    'PROGRAMMEE': 'Programmée',
    'EN_COURS': 'En cours',
    'COMPLETEE': 'Complétée',
    'APPROUVEE': 'Approuvée',
    'REJETEE': 'Rejetée'
  };
  return statusMap[status] || status;
}

function getInspectionSeverity(status) {
  const severityMap = {
    'A_PROGRAMMER': 'warning',
    'PROGRAMMEE': 'info',
    'EN_COURS': 'info',
    'COMPLETEE': 'warning',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severityMap[status] || 'secondary';
}

function getStatusLabel(status) {
  const statusMap = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En révision',
    'APPROVED': 'Approuvé',
    'APPROVED_AWAITING_FARMER': 'En attente fermier',
    'REJECTED': 'Rejeté',
    'COMPLETED': 'Terminé',
    'RETURNED_FOR_COMPLETION': 'Retourné'
  };
  return statusMap[status] || status;
}

function getStatusSeverity(status) {
  const severityMap = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'APPROVED_AWAITING_FARMER': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'RETURNED_FOR_COMPLETION': 'warning'
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

async function exportToExcel() {
  try {
    const response = await fetch('/api/agent-commission/dossiers/export', {
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
    link.setAttribute('download', `dossiers_commission_${new Date().toISOString().split('T')[0]}.xlsx`);
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

<style scoped>
.dossier-list-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.inspection-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  align-items: center;
}

.inspection-status,
.inspection-pending {
  display: flex;
  justify-content: center;
}

.inspection-date {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--text-color-secondary);
  justify-content: center;
}

.inspection-date i {
  color: var(--primary-color);
}

.decision-section h4 {
  margin-bottom: 1rem;
}

.decision-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.finalize-visit-form .form-group {
  margin-bottom: 1rem;
}

.finalize-visit-form .form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
}
</style>