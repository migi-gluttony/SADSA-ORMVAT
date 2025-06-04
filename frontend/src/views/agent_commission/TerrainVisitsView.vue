<template>
  <div class="terrain-visits-container">
    <!-- Loading State -->
    <div v-if="loading && !dashboardData" class="loading-container">
      <ProgressBar mode="indeterminate" />
      <p>Chargement des visites terrain...</p>
    </div>

    <!-- Main Content -->
    <div v-else class="main-content">
      
      <!-- Dashboard Summary -->
      <div class="dashboard-section">
        <h1 class="page-title">
          <i class="pi pi-map"></i>
          Visites Terrain - Commission Vérification Terrain
        </h1>

        <div v-if="dashboardData" class="dashboard-cards">
          <div class="summary-card urgent">
            <div class="card-icon">
              <i class="pi pi-exclamation-triangle"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesEnRetard || 0 }}</div>
              <div class="card-label">En retard</div>
            </div>
          </div>

          <div class="summary-card today">
            <div class="card-icon">
              <i class="pi pi-calendar"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesAujourdHui || 0 }}</div>
              <div class="card-label">Aujourd'hui</div>
            </div>
          </div>

          <div class="summary-card tomorrow">
            <div class="card-icon">
              <i class="pi pi-calendar-plus"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesDemain || 0 }}</div>
              <div class="card-label">Demain</div>
            </div>
          </div>

          <div class="summary-card pending">
            <div class="card-icon">
              <i class="pi pi-clock"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesEnAttente || 0 }}</div>
              <div class="card-label">En attente</div>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <Button 
            label="Visites urgentes" 
            icon="pi pi-exclamation-triangle" 
            @click="filterUrgent"
            class="p-button-warning"
            :badge="(dashboardData?.visitesEnRetard || 0).toString()"
          />
          <Button 
            label="Actualiser" 
            icon="pi pi-refresh" 
            @click="refreshData"
            class="p-button-outlined"
          />
        </div>
      </div>

      <!-- Filters Section -->
      <div class="filters-section">
        <div class="filters-grid">
          <div class="filter-group">
            <label>Recherche</label>
            <span class="p-input-icon-left">
              <i class="pi pi-search"></i>
              <InputText 
                v-model="filters.searchTerm" 
                placeholder="CIN, nom, référence..."
                 size="normal"
                @input="debounceFilter"
                class="p-inputtext-sm"
              />
            </span>
          </div>

          <div class="filter-group">
            <label>Statut</label>
            <Dropdown 
              v-model="filters.statut" 
              :options="statusOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les statuts"
              @change="applyFilters"
            />
          </div>

          <div class="filter-group">
            <label>Sous-type de projet</label>
            <Dropdown 
              v-model="filters.sousRubrique" 
              :options="sousRubriqueOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les sous-types"
              @change="applyFilters"
            />
          </div>

          <div class="filter-group">
            <label>Antenne</label>
            <Dropdown 
              v-model="filters.antenne" 
              :options="antenneOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Toutes les antennes"
              @change="applyFilters"
            />
          </div>

          <div class="filter-group">
            <label>Période</label>
            <Calendar 
              v-model="dateRange" 
              selectionMode="range" 
              dateFormat="dd/mm/yy"
              placeholder="Sélectionner période"
              @date-select="onDateRangeChange"
              showIcon
            />
          </div>

          <div class="filter-group toggle-filters">
            <div class="toggle-item">
              <Checkbox 
                v-model="filters.enRetard" 
                inputId="retard" 
                @change="applyFilters"
              />
              <label for="retard">En retard uniquement</label>
            </div>
            <div class="toggle-item">
              <Checkbox 
                v-model="filters.aCompleter" 
                inputId="completer" 
                @change="applyFilters"
              />
              <label for="completer">À compléter</label>
            </div>
          </div>
        </div>

        <div class="filter-actions">
          <Button 
            label="Réinitialiser" 
            icon="pi pi-refresh" 
            @click="resetFilters"
            class="p-button-outlined p-button-sm"
          />
        </div>
      </div>

      <!-- Visits List -->
      <div class="visits-section">
        <div class="section-header">
          <h2>
            <i class="pi pi-list"></i>
            Mes Visites Terrain
          </h2>
          <div class="list-actions">
            <Button 
              :icon="viewMode === 'grid' ? 'pi pi-th-large' : 'pi pi-list'" 
              @click="toggleViewMode"
              class="p-button-outlined p-button-sm"
              v-tooltip="viewMode === 'grid' ? 'Vue liste' : 'Vue grille'"
            />
          </div>
        </div>

        <!-- Statistics Bar -->
        <div v-if="visitData?.statistics" class="statistics-bar">
          <div class="stat-item">
            <span class="stat-value">{{ visitData.statistics.totalVisites || 0 }}</span>
            <span class="stat-label">Total</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ visitData.statistics.visitesRealisees || 0 }}</span>
            <span class="stat-label">Réalisées</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ Math.round(visitData.statistics.tauxApprobation || 0) }}%</span>
            <span class="stat-label">Taux approbation</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ visitData.statistics.visitesApprouvees || 0 }}</span>
            <span class="stat-label">Approuvées</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ visitData.statistics.visitesRejetees || 0 }}</span>
            <span class="stat-label">Rejetées</span>
          </div>
        </div>

        <!-- Visits Grid/List -->
        <div v-if="visitData?.visites" class="visits-content">
          <!-- Grid View -->
          <div v-if="viewMode === 'grid'" class="visits-grid">
            <div 
              v-for="visite in visitData.visites" 
              :key="visite.id"
              class="visit-card"
              :class="getVisitCardClass(visite)"
              @click="openVisitDetail(visite)"
            >
              <div class="visit-header">
                <div class="visit-status">
                  <Tag 
                    :value="visite.statutDisplay || visite.statut" 
                    :severity="getStatusSeverity(visite.statut)"
                  />
                  <Tag 
                    v-if="visite.isOverdue" 
                    value="RETARD" 
                    severity="danger"
                    class="ml-2"
                  />
                </div>
                <div class="visit-date">
                  <i class="pi pi-calendar"></i>
                  {{ formatDate(visite.dateVisite) }}
                </div>
              </div>

              <div class="visit-content">
                <div class="dossier-info">
                  <h3>{{ visite.dossierReference }}</h3>
                  <p class="agriculteur-name">
                    {{ visite.agriculteurPrenom }} {{ visite.agriculteurNom }}
                  </p>
                  <p class="project-type">{{ visite.sousRubriqueDesignation }}</p>
                  <p class="location">
                    <i class="pi pi-map-marker"></i>
                    {{ getLocationText(visite) }}
                  </p>
                </div>

                <div class="visit-meta">
                  <div class="meta-item" v-if="visite.photosCount > 0">
                    <i class="pi pi-image"></i>
                    {{ visite.photosCount }} photo(s)
                  </div>
                  <div class="meta-item" v-if="visite.hasNotes">
                    <i class="pi pi-file-edit"></i>
                    Avec notes
                  </div>
                  <div class="meta-item" v-if="visite.daysUntilVisit !== null">
                    <i class="pi pi-clock"></i>
                    {{ getDaysUntilVisitText(visite.daysUntilVisit) }}
                  </div>
                </div>
              </div>

              <div class="visit-actions">
                <Button 
                  v-if="visite.canComplete"
                  icon="pi pi-check-circle" 
                  @click.stop="openCompleteDialog(visite)"
                  class="p-button-success p-button-sm"
                  v-tooltip="'Finaliser la visite'"
                />
                <Button 
                  v-if="visite.canModify"
                  icon="pi pi-pencil" 
                  @click.stop="openVisitDetail(visite)"
                  class="p-button-info p-button-sm"
                  v-tooltip="'Modifier'"
                />
                <Button 
                  icon="pi pi-eye" 
                  @click.stop="openVisitDetail(visite)"
                  class="p-button-outlined p-button-sm"
                  v-tooltip="'Voir détails'"
                />
              </div>
            </div>
          </div>

          <!-- List View -->
          <div v-else class="visits-list">
            <DataTable 
              :value="visitData.visites" 
              :paginator="true" 
              :rows="20"
              :loading="loading"
              responsiveLayout="scroll"
              @row-click="onRowClick"
              class="visits-table"
            >
              <Column field="dateVisite" header="Date visite" sortable>
                <template #body="{ data }">
                  <div class="date-column">
                    {{ formatDate(data.dateVisite) }}
                    <Tag v-if="data.isOverdue" value="RETARD" severity="danger" class="ml-2" />
                  </div>
                </template>
              </Column>

              <Column field="dossierReference" header="Référence" sortable />

              <Column field="agriculteurNom" header="Agriculteur" sortable>
                <template #body="{ data }">
                  <div class="agriculteur-column">
                    <strong>{{ data.agriculteurPrenom }} {{ data.agriculteurNom }}</strong>
                    <small>{{ data.agriculteurCin }}</small>
                  </div>
                </template>
              </Column>

              <Column field="sousRubriqueDesignation" header="Type projet" />

              <Column field="antenneDesignation" header="Antenne" />

              <Column field="statut" header="Statut">
                <template #body="{ data }">
                  <Tag 
                    :value="data.statutDisplay || data.statut" 
                    :severity="getStatusSeverity(data.statut)"
                  />
                </template>
              </Column>

              <Column header="Actions" class="actions-column">
                <template #body="{ data }">
                  <div class="action-buttons">
                    <Button 
                      v-if="data.canComplete"
                      icon="pi pi-check-circle" 
                      @click.stop="openCompleteDialog(data)"
                      class="p-button-success p-button-sm"
                      v-tooltip="'Finaliser'"
                    />
                    <Button 
                      icon="pi pi-eye" 
                      @click.stop="openVisitDetail(data)"
                      class="p-button-info p-button-sm"
                      v-tooltip="'Détails'"
                    />
                  </div>
                </template>
              </Column>
            </DataTable>
          </div>

          <!-- Empty State -->
          <div v-if="visitData.visites.length === 0" class="empty-state">
            <i class="pi pi-map"></i>
            <h3>Aucune visite terrain trouvée</h3>
            <p>Aucune visite ne correspond aux critères de recherche.</p>
            <Button 
              label="Réinitialiser les filtres" 
              icon="pi pi-refresh" 
              @click="resetFilters"
              class="p-button-outlined"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Visit Detail Dialog -->
    <Dialog 
      v-model:visible="showDetailDialog" 
      modal 
      :header="selectedVisit ? `Visite Terrain - ${selectedVisit.dossierReference}` : 'Détail Visite'"
      :style="{ width: '90vw', maxWidth: '1200px' }"
      maximizable
    >
      <VisitDetailComponent 
        v-if="selectedVisit && showDetailDialog"
        :visit="selectedVisit"
        @visit-updated="onVisitUpdated"
        @close="showDetailDialog = false"
        @complete-visit="onCompleteFromDetail"
      />
    </Dialog>

    <!-- Complete Visit Dialog -->
    <Dialog 
      v-model:visible="showCompleteDialog" 
      modal 
      header="Finaliser la Visite Terrain"
      :style="{ width: '90vw', maxWidth: '800px' }"
      maximizable
    >
      <CompleteVisitComponent 
        v-if="selectedVisit && showCompleteDialog"
        :visit="selectedVisit"
        @visit-completed="onVisitCompleted"
        @close="showCompleteDialog = false"
      />
    </Dialog>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useToast } from 'primevue/usetoast';
import { useRouter } from 'vue-router';
import ApiService from '@/services/ApiService';

// PrimeVue Components
import Button from 'primevue/button';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import Checkbox from 'primevue/checkbox';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

// Local Components
import VisitDetailComponent from '@/components/agent_commission/VisitDetailComponent.vue';
import CompleteVisitComponent from '@/components/agent_commission/CompleteVisitComponent.vue';

const toast = useToast();
const router = useRouter();

// State
const loading = ref(false);
const dashboardData = ref(null);
const visitData = ref(null);
const selectedVisit = ref(null);
const viewMode = ref('grid');
const showDetailDialog = ref(false);
const showCompleteDialog = ref(false);
const dateRange = ref(null);

// Filters
const filters = ref({
  searchTerm: '',
  statut: null,
  sousRubrique: null,
  antenne: null,
  dateDebut: null,
  dateFin: null,
  enRetard: false,
  aCompleter: false,
  sortBy: 'dateVisite',
  sortDirection: 'ASC',
  page: 0,
  size: 20
});

// Options
const statusOptions = ref([
  { label: 'Tous les statuts', value: null },
  { label: 'Nouvelle', value: 'NOUVELLE' },
  { label: 'Programmée', value: 'PROGRAMMEE' },
  { label: 'En retard', value: 'EN_RETARD' },
  { label: 'Complétée', value: 'COMPLETEE' },
  { label: 'Approuvée', value: 'APPROUVEE' },
  { label: 'Rejetée', value: 'REJETEE' }
]);

const sousRubriqueOptions = ref([
  { label: 'Tous les sous-types', value: null },
  // Filières Végétales
  { label: 'Acquisition et installation des serres', value: 'Acquisition et installation des serres destinées à la production agricole' },
  { label: 'Arboriculture fruitière', value: 'Arboriculture fruitière' },
  { label: 'Équipement des exploitations en matériel agricole', value: 'Équipement des exploitations en matériel agricole' },
  { label: 'Filets de protection des cultures maraichères', value: 'Filets de protection des cultures maraichères sous serre' },
  { label: 'Filets de protection contre la grêle', value: 'Filets de protection des plantations fruitières contre la grêle' },
  { label: 'Unités de valorisation végétale', value: 'Unités de valorisation de la production végétale' },
  // Filières Animales
  { label: 'Acquisition du matériel d\'élevage', value: 'Acquisition du matériel d\'élevage' },
  { label: 'Centres de collecte de lait', value: 'Centres de collecte de lait' },
  { label: 'Unités de valorisation animale', value: 'Unités de valorisation de la production animale' },
  // Aménagement Hydro-Agricole
  { label: 'Aménagement Hydro-Agricole et Amélioration Foncière', value: 'AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE' }
]);

const antenneOptions = ref([
  { label: 'Toutes les antennes', value: null },
  { label: 'Antenne Bni Amir', value: 'Antenne Bni Amir' },
  { label: 'Antenne Souk Sebt', value: 'Antenne Souk Sebt' },
  { label: 'Antenne Ouldad M\'Bark', value: 'Antenne Ouldad M\'Bark' },
  { label: 'Antenne Dar Ouled Zidouh', value: 'Antenne Dar Ouled Zidouh' },
  { label: 'Antenne Afourer', value: 'Antenne Afourer' }
]);

// Lifecycle
onMounted(() => {
  loadDashboard();
  loadVisits();
});

// Methods
async function loadDashboard() {
  try {
    const response = await ApiService.get('/agent_commission/terrain-visits/dashboard');
    dashboardData.value = response;
  } catch (error) {
    console.error('Erreur chargement dashboard:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger le tableau de bord',
      life: 4000
    });
  }
}

async function loadVisits() {
  try {
    loading.value = true;
    
    // Build query parameters
    const params = new URLSearchParams();
    Object.entries(filters.value).forEach(([key, value]) => {
      if (value !== null && value !== '' && value !== false) {
        params.append(key, value);
      }
    });

    const response = await ApiService.get(`/agent_commission/terrain-visits?${params}`);
    visitData.value = response;
    
  } catch (error) {
    console.error('Erreur chargement visites:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les visites terrain',
      life: 4000
    });
  } finally {
    loading.value = false;
  }
}

function debounceFilter() {
  clearTimeout(window.debounceTimer);
  window.debounceTimer = setTimeout(() => {
    applyFilters();
  }, 500);
}

function applyFilters() {
  filters.value.page = 0; // Reset to first page
  loadVisits();
}

function resetFilters() {
  filters.value = {
    searchTerm: '',
    statut: null,
    sousRubrique: null,
    antenne: null,
    dateDebut: null,
    dateFin: null,
    enRetard: false,
    aCompleter: false,
    sortBy: 'dateVisite',
    sortDirection: 'ASC',
    page: 0,
    size: 20
  };
  dateRange.value = null;
  loadVisits();
}

function filterUrgent() {
  filters.value.enRetard = true;
  filters.value.statut = 'EN_RETARD';
  applyFilters();
}

function onDateRangeChange() {
  if (dateRange.value && dateRange.value.length === 2) {
    filters.value.dateDebut = formatDateForAPI(dateRange.value[0]);
    filters.value.dateFin = formatDateForAPI(dateRange.value[1]);
    applyFilters();
  }
}

function toggleViewMode() {
  viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid';
}

async function openVisitDetail(visit) {
  try {
    // Load full visit details
    const response = await ApiService.get(`/agent_commission/terrain-visits/${visit.id}`);
    selectedVisit.value = response;
    showDetailDialog.value = true;
    
  } catch (error) {
    console.error('Erreur chargement détail visite:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les détails de la visite',
      life: 4000
    });
  }
}

function openCompleteDialog(visit) {
  if (!visit || !visit.id) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'ouvrir la finalisation - données de visite manquantes',
      life: 4000
    });
    return;
  }
  
  selectedVisit.value = { ...visit };
  showCompleteDialog.value = true;
}

function onCompleteFromDetail(visit) {
  selectedVisit.value = visit;
  showDetailDialog.value = false;
  showCompleteDialog.value = true;
}

function onRowClick(event) {
  openVisitDetail(event.data);
}

function onVisitUpdated() {
  loadVisits();
  loadDashboard();
  // Don't close dialog automatically for updates
}

function onVisitCompleted() {
  loadVisits();
  loadDashboard();
  showCompleteDialog.value = false;
  showDetailDialog.value = false;
  
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain finalisée avec succès',
    life: 3000
  });
}

function refreshData() {
  loadDashboard();
  loadVisits();
  toast.add({
    severity: 'success',
    summary: 'Actualisé',
    detail: 'Données mises à jour',
    life: 2000
  });
}

// Utility Functions
function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}

function formatDateForAPI(date) {
  if (!date) return null;
  return date.toISOString().split('T')[0];
}

function getStatusSeverity(status) {
  const severities = {
    'NOUVELLE': 'secondary',
    'PROGRAMMEE': 'info',
    'EN_RETARD': 'danger',
    'COMPLETEE': 'warning',
    'APPROUVEE': 'success',
    'REJETEE': 'danger'
  };
  return severities[status] || 'secondary';
}

function getVisitCardClass(visit) {
  const classes = ['visit-card'];
  if (visit.isOverdue) classes.push('overdue');
  if (visit.statut === 'APPROUVEE') classes.push('approved');
  if (visit.statut === 'REJETEE') classes.push('rejected');
  return classes.join(' ');
}

function getLocationText(visit) {
  const parts = [];
  if (visit.agriculteurDouar) parts.push(visit.agriculteurDouar);
  if (visit.agriculteurCommune) parts.push(visit.agriculteurCommune);
  if (visit.antenneDesignation) parts.push(visit.antenneDesignation);
  return parts.join(' - ') || 'Non spécifié';
}

function getDaysUntilVisitText(days) {
  if (days === null) return '';
  if (days < 0) return `${Math.abs(days)} jour(s) de retard`;
  if (days === 0) return 'Aujourd\'hui';
  if (days === 1) return 'Demain';
  return `Dans ${days} jour(s)`;
}
</script>

