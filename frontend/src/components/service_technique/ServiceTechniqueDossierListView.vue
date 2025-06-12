<template>
  <div class="service-technique-dossier-list-container p-4">
    <!-- Header -->
    <div class="flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold text-900">
          <i class="pi pi-cog mr-2"></i>
          Dossiers - Service Technique
        </h1>
        <p class="text-600" v-if="dossiers.length > 0">
          {{ dossiers.length }} dossier(s) d'implémentation trouvé(s)
        </p>
      </div>
      <div class="flex gap-2">
        <Button 
          label="Visites Implémentation" 
          icon="pi pi-calendar" 
          @click="navigateToImplementationVisits" 
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
              <div class="text-3xl font-bold text-orange-500">{{ statistics.dossiersEnCours || 0 }}</div>
              <div class="text-color-secondary">En cours d'implémentation</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-500">{{ statistics.dossiersTermines || 0 }}</div>
              <div class="text-color-secondary">Implémentation terminée</div>
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
          <div class="col-12 md:col-4">
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
          <div class="col-12 md:col-2 flex align-items-end">
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
        <i class="pi pi-cog text-4xl text-600 mb-3"></i>
        <h3>Aucun dossier trouvé</h3>
        <p class="text-600" v-if="hasActiveFilters">
          Aucun dossier ne correspond à vos critères de recherche.
        </p>
        <p class="text-600" v-else>
          Aucun dossier d'implémentation n'est assigné au Service Technique.
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

          <Column header="Projet Infrastructure">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">Aménagement Hydro-Agricole</div>
                <div class="text-sm text-600" v-if="slotProps.data.montantSubvention">
                  <i class="pi pi-money-bill mr-1"></i>
                  {{ formatCurrency(slotProps.data.montantSubvention) }}
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
          </Column>

          <Column field="actions" header="Actions">
            <template #body="slotProps">
              <div class="flex gap-1">
                <!-- View Details -->
                <Button 
                  icon="pi pi-eye" 
                  @click="viewDossierDetail(slotProps.data.id)"
                  class="p-button-outlined p-button-sm" 
                  v-tooltip.top="'Voir détails'" 
                />
                
                <!-- Schedule Implementation Visit -->
                <Button 
                  v-if="canScheduleImplementationVisit(slotProps.data)"
                  icon="pi pi-calendar-plus" 
                  @click="showScheduleImplementationVisitDialog(slotProps.data)"
                  class="p-button-info p-button-sm" 
                  v-tooltip.top="'Programmer visite d\'implémentation'" 
                />

                <!-- View Implementation Visits -->
                <Button 
                  icon="pi pi-calendar" 
                  @click="viewImplementationVisits(slotProps.data)"
                  class="p-button-secondary p-button-outlined p-button-sm" 
                  v-tooltip.top="'Voir visites d\'implémentation'" 
                />

                <!-- Mark Complete -->
                <Button 
                  v-if="canMarkComplete(slotProps.data)"
                  icon="pi pi-check-circle" 
                  @click="showMarkCompleteDialog(slotProps.data)"
                  class="p-button-success p-button-sm" 
                  v-tooltip.top="'Marquer terminé'" 
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Schedule Implementation Visit Dialog -->
    <Dialog 
      v-model:visible="scheduleImplementationVisitDialog.visible" 
      modal 
      header="Programmer Visite d'Implémentation"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-calendar-plus text-2xl mr-3"></i>
        <div>
          <p>Programmer une visite d'implémentation pour ce projet ?</p>
          <div class="mt-2 p-2 bg-blue-50 border-round">
            <strong>{{ scheduleImplementationVisitDialog.dossier?.reference }}</strong><br>
            {{ scheduleImplementationVisitDialog.dossier?.agriculteurNom }}<br>
            <small>Type: Aménagement Hydro-Agricole</small>
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="visitDate">Date de visite *</label>
        <Calendar 
          id="visitDate"
          v-model="scheduleImplementationVisitDialog.dateVisite" 
          dateFormat="dd/mm/yy"
          :minDate="new Date()"
          placeholder="Sélectionner une date"
          class="w-full"
          :class="{ 'p-invalid': !scheduleImplementationVisitDialog.dateVisite }"
          showIcon
        />
      </div>

      
      
      <div class="field">
        <label for="visitComment">Observations *</label>
        <Textarea 
          id="visitComment"
          v-model="scheduleImplementationVisitDialog.observations" 
          rows="3" 
          placeholder="Observations pour la visite d'implémentation..."
          class="w-full"
          :class="{ 'p-invalid': !scheduleImplementationVisitDialog.observations?.trim() }"
        />
      </div>

      <p class="text-600 text-sm">
        La visite d'implémentation sera programmée pour superviser l'avancement du projet.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="scheduleImplementationVisitDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Programmer la Visite" 
          icon="pi pi-calendar-plus" 
          @click="scheduleImplementationVisit"
          class="p-button-info"
          :loading="scheduleImplementationVisitDialog.loading"
          :disabled="!scheduleImplementationVisitDialog.dateVisite || !scheduleImplementationVisitDialog.observations?.trim()"
        />
      </template>
    </Dialog>

    <!-- Mark Complete Dialog -->
    <Dialog 
      v-model:visible="markCompleteDialog.visible" 
      modal 
      header="Marquer comme Terminé"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-check-circle text-2xl mr-3"></i>
        <div>
          <p>Marquer l'implémentation de ce projet comme terminée ?</p>
          <div class="mt-2 p-2 bg-green-50 border-round">
            <strong>{{ markCompleteDialog.dossier?.reference }}</strong><br>
            {{ markCompleteDialog.dossier?.agriculteurNom }}
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="completionComment">Commentaire de finalisation *</label>
        <Textarea 
          id="completionComment"
          v-model="markCompleteDialog.commentaire" 
          rows="4" 
          placeholder="Commentaire sur la finalisation de l'implémentation..."
          class="w-full"
          :class="{ 'p-invalid': !markCompleteDialog.commentaire?.trim() }"
        />
      </div>

      <p class="text-600 text-sm">
        Cette action marquera l'implémentation comme terminée et enverra le dossier au GUC pour validation finale.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="markCompleteDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Marquer Terminé" 
          icon="pi pi-check-circle" 
          @click="markComplete"
          class="p-button-success"
          :loading="markCompleteDialog.loading"
          :disabled="!markCompleteDialog.commentaire?.trim()"
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

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Dropdown from 'primevue/dropdown';
import Textarea from 'primevue/textarea';
import Calendar from 'primevue/calendar';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
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
  antenneId: null,
  priorite: null
});

// Filter options
const statusOptions = ref([
  { label: 'En cours de réalisation', value: 'REALIZATION_IN_PROGRESS' },
  { label: 'Terminé', value: 'COMPLETED' }
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
const scheduleImplementationVisitDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  dateVisite: null,
  observations: '',
  pourcentageAvancement: 0
});

const markCompleteDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  commentaire: ''
});

// Computed
const hasActiveFilters = computed(() => {
  return searchTerm.value?.trim() || filters.statut || filters.antenneId || filters.priorite;
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
    if (filters.antenneId) params.antenneId = filters.antenneId;
    if (filters.priorite) params.priorite = filters.priorite;

    const response = await ApiService.get('/service-technique/dossiers', params);
    dossiers.value = response.dossiers || [];
    
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
  const enCours = dossiers.value.filter(d => d.statut === 'REALIZATION_IN_PROGRESS').length;
  const termines = dossiers.value.filter(d => d.statut === 'COMPLETED').length;
  const enRetard = dossiers.value.filter(d => d.enRetard).length;

  statistics.value = {
    totalDossiers: total,
    dossiersEnCours: enCours,
    dossiersTermines: termines,
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
    antenneId: null,
    priorite: null
  });
  searchTerm.value = '';
  loadDossiers();
}

function navigateToImplementationVisits() {
  router.push('/service_technique/implementation-visits');
}

function viewDossierDetail(dossierId) {
  router.push(`/service_technique/dossiers/${dossierId}`);
}

function viewImplementationVisits(dossier) {
  router.push(`/service_technique/implementation-visits?dossier=${dossier.id}`);
}

// Implementation visit management
function canScheduleImplementationVisit(dossier) {
  return dossier.statut === 'REALIZATION_IN_PROGRESS';
}

function canMarkComplete(dossier) {
  return dossier.statut === 'REALIZATION_IN_PROGRESS';
}

function getProgressPercentage(dossier) {
  // This would come from implementation visit data in a real implementation
  // For now, return a mock value based on status
  if (dossier.statut === 'COMPLETED') return 100;
  if (dossier.statut === 'REALIZATION_IN_PROGRESS') return 45; // Mock value
  return 0;
}

function getImplementationStatusLabel(dossier) {
  const progress = getProgressPercentage(dossier);
  if (progress === 100) return 'Terminé';
  if (progress >= 75) return 'Finition';
  if (progress >= 50) return 'En cours';
  if (progress >= 25) return 'Démarré';
  return 'À démarrer';
}

function getImplementationSeverity(dossier) {
  const progress = getProgressPercentage(dossier);
  if (progress === 100) return 'success';
  if (progress >= 75) return 'info';
  if (progress >= 50) return 'warning';
  if (progress >= 25) return 'secondary';
  return 'danger';
}

// Dialog functions
function showScheduleImplementationVisitDialog(dossier) {
  scheduleImplementationVisitDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    dateVisite: null,
    observations: '',
    pourcentageAvancement: 0
  };
}

function showMarkCompleteDialog(dossier) {
  markCompleteDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    commentaire: ''
  };
}

// Action methods
async function scheduleImplementationVisit() {
  try {
    scheduleImplementationVisitDialog.value.loading = true;
    
    const dossier = scheduleImplementationVisitDialog.value.dossier;
    const payload = { 
      dossierId: dossier.id,
      dateVisite: scheduleImplementationVisitDialog.value.dateVisite.toISOString().split('T')[0],
      observations: scheduleImplementationVisitDialog.value.observations,
      pourcentageAvancement: scheduleImplementationVisitDialog.value.pourcentageAvancement || 0
    };

    const response = await ApiService.post('/service-technique/visits/schedule', payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite d\'implémentation programmée',
        life: 3000
      });
      
      scheduleImplementationVisitDialog.value.visible = false;
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
    scheduleImplementationVisitDialog.value.loading = false;
  }
}

async function markComplete() {
  try {
    markCompleteDialog.value.loading = true;
    
    const dossier = markCompleteDialog.value.dossier;
    const payload = { 
      commentaire: markCompleteDialog.value.commentaire,
      observations: markCompleteDialog.value.commentaire
    };

    const response = await ApiService.post(`/service-technique/dossiers/complete/${dossier.id}`, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Implémentation marquée comme terminée',
        life: 3000
      });
      
      markCompleteDialog.value.visible = false;
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
    markCompleteDialog.value.loading = false;
  }
}

// Utility functions
function getStatusLabel(status) {
  const statusMap = {
    'REALIZATION_IN_PROGRESS': 'En cours de réalisation',
    'COMPLETED': 'Terminé'
  };
  return statusMap[status] || status;
}

function getStatusSeverity(status) {
  const severityMap = {
    'REALIZATION_IN_PROGRESS': 'warning',
    'COMPLETED': 'success'
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
    const response = await fetch('/api/service-technique/dossiers/export', {
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
    link.setAttribute('download', `dossiers_service_technique_${new Date().toISOString().split('T')[0]}.xlsx`);
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
.service-technique-dossier-list-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.implementation-progress {
  min-width: 150px;
}

.progress-info {
  margin-bottom: 0.5rem;
}

.progress-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-color-secondary);
}

.implementation-status {
  display: flex;
  justify-content: center;
}
</style>