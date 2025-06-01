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
          Visites Terrain - Commission AHA-AF
        </h1>

        <div v-if="dashboardData" class="dashboard-cards">
          <div class="summary-card urgent">
            <div class="card-icon">
              <i class="pi pi-exclamation-triangle"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesEnRetard }}</div>
              <div class="card-label">En retard</div>
            </div>
          </div>

          <div class="summary-card today">
            <div class="card-icon">
              <i class="pi pi-calendar"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesAujourdHui }}</div>
              <div class="card-label">Aujourd'hui</div>
            </div>
          </div>

          <div class="summary-card tomorrow">
            <div class="card-icon">
              <i class="pi pi-calendar-plus"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesDemain }}</div>
              <div class="card-label">Demain</div>
            </div>
          </div>

          <div class="summary-card pending">
            <div class="card-icon">
              <i class="pi pi-clock"></i>
            </div>
            <div class="card-content">
              <div class="card-number">{{ dashboardData.visitesEnAttente }}</div>
              <div class="card-label">En attente</div>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <Button 
            label="Programmer une visite" 
            icon="pi pi-calendar-plus" 
            @click="showScheduleDialog = true"
            class="p-button-success"
          />
          <Button 
            label="Visites urgentes" 
            icon="pi pi-exclamation-triangle" 
            @click="filterUrgent"
            class="p-button-warning"
            :badge="dashboardData?.visitesEnRetard || '0'"
          />
          <Button 
            label="Exporter rapport" 
            icon="pi pi-download" 
            @click="exportReport"
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
                @input="debounceFilter"
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
            <label>Type de projet</label>
            <Dropdown 
              v-model="filters.rubrique" 
              :options="projectTypeOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les types"
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
            <span class="stat-value">{{ visitData.statistics.totalVisites }}</span>
            <span class="stat-label">Total</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ visitData.statistics.visitesRealisees }}</span>
            <span class="stat-label">Réalisées</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ Math.round(visitData.statistics.tauxApprobation) }}%</span>
            <span class="stat-label">Taux approbation</span>
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
                    :value="visite.statutDisplay" 
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
                    {{ visite.agriculteurCommune }} - {{ visite.antenneDesignation }}
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
              @row-click="openVisitDetail"
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
                    :value="data.statutDisplay" 
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
      />
    </Dialog>

    <!-- Complete Visit Dialog -->
    <Dialog 
      v-model:visible="showCompleteDialog" 
      modal 
      header="Finaliser la Visite Terrain"
      :style="{ width: '600px' }"
    >
      <CompleteVisitComponent 
        v-if="selectedVisit && showCompleteDialog"
        :visit="selectedVisit"
        @visit-completed="onVisitCompleted"
        @close="showCompleteDialog = false"
      />
    </Dialog>

    <!-- Schedule Visit Dialog -->
    <ScheduleTerrainVisitDialog
      v-model:visible="showScheduleDialog"
      :dossier="null"
      @visit-scheduled="onVisitScheduled"
    />

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
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
import ScheduleTerrainVisitDialog from '@/components/agent_commission/ScheduleTerrainVisitDialog.vue';
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
const showScheduleDialog = ref(false);
const dateRange = ref(null);

// Filters
const filters = ref({
  searchTerm: '',
  statut: null,
  rubrique: null,
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
  { label: 'Programmée', value: 'PROGRAMMEE' },
  { label: 'En retard', value: 'EN_RETARD' },
  { label: 'Complétée', value: 'COMPLETEE' },
  { label: 'Approuvée', value: 'APPROUVEE' },
  { label: 'Rejetée', value: 'REJETEE' }
]);

const projectTypeOptions = ref([
  { label: 'Tous les types', value: null },
  { label: 'Filières Végétales', value: 'FILIERES VEGETALES' },
  { label: 'Filières Animales', value: 'FILIERES ANIMALES' },
  { label: 'Aménagement Hydro-Agricole', value: 'AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE' }
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
    rubrique: null,
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
  selectedVisit.value = visit;
  showCompleteDialog.value = true;
}

function onVisitUpdated() {
  loadVisits();
  loadDashboard();
  showDetailDialog.value = false;
}

function onVisitCompleted() {
  loadVisits();
  loadDashboard();
  showCompleteDialog.value = false;
  
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain finalisée avec succès',
    life: 3000
  });
}

function onVisitScheduled() {
  loadVisits();
  loadDashboard();
  showScheduleDialog.value = false;
  
  toast.add({
    severity: 'success',
    summary: 'Succès',
    detail: 'Visite terrain programmée avec succès',
    life: 3000
  });
}

async function exportReport() {
  try {
    // Implementation for exporting visits report
    toast.add({
      severity: 'info',
      summary: 'Export',
      detail: 'Fonctionnalité d\'export en cours de développement',
      life: 3000
    });
  } catch (error) {
    console.error('Erreur export:', error);
  }
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

function getDaysUntilVisitText(days) {
  if (days === null) return '';
  if (days < 0) return `${Math.abs(days)} jour(s) de retard`;
  if (days === 0) return 'Aujourd\'hui';
  if (days === 1) return 'Demain';
  return `Dans ${days} jour(s)`;
}
</script>

<style scoped>
.terrain-visits-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1rem;
}

/* Loading */
.loading-container {
  text-align: center;
  padding: 3rem;
}

/* Page Title */
.page-title {
  color: var(--primary-color);
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 1.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Dashboard Cards */
.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: transform 0.2s ease;
}

.summary-card:hover {
  transform: translateY(-2px);
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: white;
}

.summary-card.urgent .card-icon {
  background: var(--danger-color);
}

.summary-card.today .card-icon {
  background: var(--info-color);
}

.summary-card.tomorrow .card-icon {
  background: var(--warning-color);
}

.summary-card.pending .card-icon {
  background: var(--secondary-color);
}

.card-number {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-color);
}

.card-label {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

/* Filters */
.filters-section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border-color);
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.875rem;
}

.toggle-filters {
  display: flex;
  flex-direction: row;
  gap: 1rem;
  align-items: center;
}

.toggle-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

/* Section Header */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-header h2 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Statistics Bar */
.statistics-bar {
  display: flex;
  gap: 2rem;
  background: var(--section-background);
  padding: 1rem 1.5rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  border: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.stat-value {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--primary-color);
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

/* Visits Grid */
.visits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

.visit-card {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.visit-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.visit-card.overdue {
  border-left: 4px solid var(--danger-color);
}

.visit-card.approved {
  border-left: 4px solid var(--success-color);
}

.visit-card.rejected {
  border-left: 4px solid var(--danger-color);
}

.visit-header {
  background: var(--section-background);
  padding: 1rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.visit-date {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.visit-content {
  padding: 1rem;
}

.dossier-info h3 {
  color: var(--primary-color);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
}

.agriculteur-name {
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 0.25rem 0;
}

.project-type {
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.location {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.visit-meta {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--text-secondary);
  font-size: 0.75rem;
}

.visit-actions {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid var(--border-color);
  background: var(--section-background);
}

/* Data Table */
.visits-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.date-column,
.agriculteur-column {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.agriculteur-column small {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 3rem;
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.empty-state i {
  font-size: 3rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
}

.empty-state p {
  color: var(--text-secondary);
  margin: 0 0 1.5rem 0;
}

/* Responsive */
@media (max-width: 768px) {
  .terrain-visits-container {
    padding: 0.5rem;
  }
  
  .dashboard-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filters-grid {
    grid-template-columns: 1fr;
  }
  
  .visits-grid {
    grid-template-columns: 1fr;
  }
  
  .quick-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .statistics-bar {
    flex-wrap: wrap;
    justify-content: center;
  }
}

/* Dark mode */
.dark-mode .summary-card,
.dark-mode .filters-section,
.dark-mode .visit-card,
.dark-mode .visits-table,
.dark-mode .empty-state {
  background-color: var(--card-background);
  border-color: var(--card-border);
}

.dark-mode .visit-header,
.dark-mode .visit-actions,
.dark-mode .statistics-bar {
  background-color: var(--section-background);
  border-color: var(--card-border);
}
</style>