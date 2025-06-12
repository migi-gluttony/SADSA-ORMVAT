<template>
  <div class="dossier-list-container p-4">
    <!-- Header -->
    <div class="flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold text-900">
          <i class="pi pi-folder-open mr-2"></i>
          Mes Dossiers
        </h1>
        <p class="text-600" v-if="dossiers.length > 0">
          {{ dossiers.length }} dossier(s) trouvé(s)
        </p>
      </div>
      <div class="flex gap-2">
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

    <!-- Statistics Cards -->
    <div class="grid mb-4" v-if="statistics">
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-primary">{{ statistics.total }}</div>
              <div class="text-color-secondary">Total dossiers</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-blue-500">{{ statistics.enCours }}</div>
              <div class="text-color-secondary">En cours</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-500">{{ statistics.approuves }}</div>
              <div class="text-color-secondary">Approuvés</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3" v-if="statistics.enRetard > 0">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-orange-500">{{ statistics.enRetard }}</div>
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
          <div class="col-12 md:col-3">
            <label class="block text-900 font-medium mb-2">Statut</label>
            <Dropdown 
              v-model="selectedStatus" 
              :options="statusOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les statuts" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-3">
            <label class="block text-900 font-medium mb-2">Type de Projet</label>
            <Dropdown 
              v-model="selectedProjectType" 
              :options="projectTypeOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les types" 
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
        <i class="pi pi-folder-open text-4xl text-600 mb-3"></i>
        <h3>Aucun dossier trouvé</h3>
        <p class="text-600" v-if="hasActiveFilters">
          Aucun dossier ne correspond à vos critères de recherche.
        </p>
        <p class="text-600" v-else>
          Vous n'avez pas encore créé de dossier.
        </p>
        <div class="mt-3">
          <Button 
            v-if="hasActiveFilters" 
            label="Effacer les filtres" 
            icon="pi pi-filter-slash" 
            @click="clearFilters"
            class="p-button-outlined mr-2" 
          />
          <Button 
            label="Créer mon premier dossier" 
            icon="pi pi-plus" 
            @click="navigateToCreate" 
            class="p-button-success" 
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
          <Column field="numeroDossier" header="Référence" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.numeroDossier }}</div>
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

          <Column field="currentStep" header="Étape Actuelle">
            <template #body="slotProps">
              <div class="text-sm">{{ slotProps.data.currentStep }}</div>
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

          <Column field="montantSubvention" header="Montant" :sortable="true">
            <template #body="slotProps">
              <div class="text-sm font-semibold" v-if="slotProps.data.montantSubvention">
                {{ formatCurrency(slotProps.data.montantSubvention) }}
              </div>
              <div class="text-xs text-500" v-else>Non défini</div>
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
                
                <!-- Available Actions from Backend -->
                <template v-for="action in slotProps.data.availableActions" :key="action.action">
                  <Button 
                    v-if="action.action === 'update'"
                    icon="pi pi-pencil" 
                    @click="editDossier(slotProps.data.id)"
                    class="p-button-outlined p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  <Button 
                    v-if="action.action === 'submit'"
                    icon="pi pi-send" 
                    @click="confirmSubmitDossier(slotProps.data)"
                    class="p-button-success p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  <Button 
                    v-if="action.action === 'delete'"
                    icon="pi pi-trash" 
                    @click="confirmDeleteDossier(slotProps.data)"
                    class="p-button-danger p-button-outlined p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  <Button 
                    v-if="action.action === 'start_realization'"
                    icon="pi pi-play" 
                    @click="confirmStartRealization(slotProps.data)"
                    class="p-button-info p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                </template>
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Submit Confirmation Dialog -->
    <Dialog 
      v-model:visible="submitDialog.visible" 
      modal 
      header="Confirmer la soumission"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-send text-2xl mr-3"></i>
        <div>
          <p>Confirmer la soumission de ce dossier au GUC ?</p>
          <div class="mt-2 p-2 bg-blue-50 border-round">
            <strong>{{ submitDialog.dossier?.numeroDossier }}</strong><br>
            {{ submitDialog.dossier?.agriculteurNom }}<br>
            <small>SABA: {{ submitDialog.dossier?.saba }}</small>
          </div>
        </div>
      </div>
      <p class="text-600 text-sm">
        Une fois soumis, le dossier ne pourra plus être modifié à l'antenne.
      </p>
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="submitDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Soumettre" 
          icon="pi pi-send" 
          @click="submitDossier"
          class="p-button-success"
          :loading="submitDialog.loading"
        />
      </template>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog 
      v-model:visible="deleteDialog.visible" 
      modal 
      header="Confirmer la suppression"
      :style="{ width: '450px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-exclamation-triangle text-2xl text-orange-500 mr-3"></i>
        <div>
          <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
          <div class="mt-2 p-2 bg-orange-50 border-round">
            <strong>{{ deleteDialog.dossier?.numeroDossier }}</strong><br>
            {{ deleteDialog.dossier?.agriculteurNom }}
          </div>
        </div>
      </div>
      <p class="text-red-600 text-sm font-semibold">
        Cette action est irréversible.
      </p>
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

    <!-- Start Realization Confirmation Dialog -->
    <Dialog 
      v-model:visible="realizationDialog.visible" 
      modal 
      header="Démarrer la réalisation"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-play text-2xl text-blue-500 mr-3"></i>
        <div>
          <p>Confirmer le démarrage de la réalisation pour ce dossier ?</p>
          <div class="mt-2 p-2 bg-blue-50 border-round">
            <strong>{{ realizationDialog.dossier?.numeroDossier }}</strong><br>
            {{ realizationDialog.dossier?.agriculteurNom }}<br>
            <small>SABA: {{ realizationDialog.dossier?.saba }}</small>
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="realizationComment" class="block mb-2">Commentaire</label>
        <Textarea 
          id="realizationComment"
          v-model="realizationDialog.commentaire"
          rows="2"
          class="w-full"
          placeholder="Précisez le contexte (ex: dépôt attestation, contact direct, etc.)"
        />
      </div>

      <div class="field">
        <label for="receptionType" class="block mb-2">Type de réception</label>
        <Dropdown 
          id="receptionType"
          v-model="realizationDialog.typeReception"
          :options="receptionTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Sélectionner le type"
          class="w-full"
        />
      </div>

      <div class="p-3 bg-blue-50 border-round">
        <p class="text-600 text-sm">
          Le dossier sera automatiquement transmis au GUC pour traitement selon le type de projet.
        </p>
      </div>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="realizationDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Démarrer" 
          icon="pi pi-play" 
          @click="startRealization"
          class="p-button-success"
          :loading="realizationDialog.loading"
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
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';
import Textarea from 'primevue/textarea';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const error = ref(null);
const dossiers = ref([]);
const searchTerm = ref('');
const selectedStatus = ref(null);
const selectedProjectType = ref(null);

// Dialogs
const submitDialog = ref({
  visible: false,
  dossier: null,
  loading: false
});

const deleteDialog = ref({
  visible: false,
  dossier: null,
  loading: false
});

const realizationDialog = ref({
  visible: false,
  dossier: null,
  commentaire: '',
  typeReception: '',
  loading: false
});

// Options
const statusOptions = ref([
  { label: 'Brouillon', value: 'DRAFT' },
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Retourné pour complétion', value: 'RETURNED_FOR_COMPLETION' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'Approuvé - En attente fermier', value: 'AWAITING_FARMER' },
  { label: 'Réalisation en cours', value: 'REALIZATION_IN_PROGRESS' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
]);

const projectTypeOptions = ref([
  { label: 'Filières Végétales', value: 'FILIERES_VEGETALES' },
  { label: 'Filières Animales', value: 'FILIERES_ANIMALES' },
  { label: 'Aménagement Hydro-Agricole', value: 'AMENAGEMENT_HYDRO_AGRICOLE' }
]);

const receptionTypeOptions = ref([
  { label: 'Dépôt attestation d\'approbation', value: 'DEPOT_ATTESTATION' },
  { label: 'Notification directe', value: 'NOTIFICATION_DIRECTE' },
  { label: 'Contact téléphonique', value: 'CONTACT_TELEPHONIQUE' },
  { label: 'Visite à l\'antenne', value: 'VISITE_ANTENNE' }
]);

// Computed
const hasActiveFilters = computed(() => {
  return searchTerm.value?.trim() || selectedStatus.value || selectedProjectType.value;
});

const statistics = computed(() => {
  if (dossiers.value.length === 0) return null;
  
  const total = dossiers.value.length;
  const enCours = dossiers.value.filter(d => 
    ['SUBMITTED', 'IN_REVIEW', 'RETURNED_FOR_COMPLETION'].includes(d.statut)
  ).length;
  const approuves = dossiers.value.filter(d => 
    ['APPROVED', 'AWAITING_FARMER', 'COMPLETED'].includes(d.statut)
  ).length;
  const enRetard = dossiers.value.filter(d => d.enRetard).length;
  
  return { total, enCours, approuves, enRetard };
});

// Methods
onMounted(() => {
  loadDossiers();
});

async function loadDossiers() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get('/agent_antenne/dossiers');
    dossiers.value = response.dossiers || [];
    
  } catch (err) {
    console.error('Erreur lors du chargement des dossiers:', err);
    error.value = err.message || 'Impossible de charger les dossiers';
    dossiers.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // In a real implementation, you would call the backend with search parameters
  // For now, we'll just trigger a reload
  loadDossiers();
}

function applyFilters() {
  // In a real implementation, you would call the backend with filter parameters
  loadDossiers();
}

function clearFilters() {
  searchTerm.value = '';
  selectedStatus.value = null;
  selectedProjectType.value = null;
  loadDossiers();
}

function navigateToCreate() {
  router.push('/agent_antenne/dossiers/create');
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_antenne/dossiers/${dossierId}`);
}

function editDossier(dossierId) {
  router.push(`/agent_antenne/dossiers/documents/${dossierId}`);
}

function confirmSubmitDossier(dossier) {
  submitDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false
  };
}

function confirmDeleteDossier(dossier) {
  deleteDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false
  };
}

function confirmStartRealization(dossier) {
  realizationDialog.value = {
    visible: true,
    dossier: dossier,
    commentaire: '',
    typeReception: '',
    loading: false
  };
}

async function submitDossier() {
  try {
    submitDialog.value.loading = true;
    
    const response = await ApiService.post(`/agent_antenne/dossiers/submit/${submitDialog.value.dossier.id}`);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });
      
      submitDialog.value.visible = false;
      loadDossiers();
    }
    
  } catch (err) {
    console.error('Erreur lors de la soumission:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la soumission',
      life: 3000
    });
  } finally {
    submitDialog.value.loading = false;
  }
}

async function deleteDossier() {
  try {
    deleteDialog.value.loading = true;
    
    const response = await ApiService.delete(`/agent_antenne/dossiers/delete/${deleteDialog.value.dossier.id}`);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });
      
      deleteDialog.value.visible = false;
      loadDossiers();
    }
    
  } catch (err) {
    console.error('Erreur lors de la suppression:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la suppression',
      life: 3000
    });
  } finally {
    deleteDialog.value.loading = false;
  }
}

async function startRealization() {
  try {
    realizationDialog.value.loading = true;
    
    if (!realizationDialog.value.typeReception) {
      toast.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner le type de réception',
        life: 3000
      });
      return;
    }

    const payload = {
      commentaire: realizationDialog.value.commentaire || 'Démarrage de la réalisation',
      typeReception: realizationDialog.value.typeReception,
      observations: ''
    };
    
    const response = await ApiService.post(`/agent_antenne/dossiers/start-realization/${realizationDialog.value.dossier.id}`, payload);
    
    if (response && response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });

      // Show additional info if available
      if (response.nextPhase) {
        setTimeout(() => {
          toast.add({
            severity: 'info',
            summary: 'Prochaine étape',
            detail: response.nextPhase,
            life: 5000
          });
        }, 1000);
      }
      
      realizationDialog.value.visible = false;
      loadDossiers();
    }
    
  } catch (err) {
    console.error('Erreur lors du démarrage de réalisation:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors du démarrage de réalisation',
      life: 3000
    });
  } finally {
    realizationDialog.value.loading = false;
  }
}

// Utility methods
function getStatusLabel(status) {
  const option = statusOptions.value.find(opt => opt.value === status);
  return option ? option.label : status;
}

function getStatusSeverity(status) {
  const severityMap = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'AWAITING_FARMER': 'success',
    'REALIZATION_IN_PROGRESS': 'info',
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
</script>

<style scoped>
.dossier-list-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
</style>