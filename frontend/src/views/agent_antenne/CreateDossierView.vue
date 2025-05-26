<template>
  <div class="create-dossier-container">
    <UserInfoHeader 
      search-placeholder="Rechercher un agriculteur..."
      @search="handleSearch"
    />

    <div class="stepper-container">
      <div class="stepper">
        <div 
          v-for="(step, index) in steps" 
          :key="index"
          class="step" 
          :class="{ 
            active: currentStep === index + 1,
            completed: currentStep > index + 1 
          }"
        >
          <div class="step-number">{{ index + 1 }}</div>
          <div class="step-label">{{ step.label }}</div>
        </div>
      </div>
    </div>

    <!-- Étape 1: Sélection du type de projet -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="component-card">
        <h2><i class="pi pi-tags"></i> Sélectionnez le type de projet</h2>
        <div class="project-types-grid">
          <div 
            v-for="rubrique in rubriques" 
            :key="rubrique.id"
            class="rubrique-card"
          >
            <h3>{{ rubrique.designation }}</h3>
            <p v-if="rubrique.description" class="rubrique-description">{{ rubrique.description }}</p>
            <div class="sous-rubriques">
              <div 
                v-for="sousRubrique in rubrique.sousRubriques"
                :key="sousRubrique.id"
                class="project-type"
                :class="{ selected: selectedSousRubrique?.id === sousRubrique.id }"
                @click="selectSousRubrique(sousRubrique)"
              >
                <div class="project-icon">
                  <i class="pi pi-folder"></i>
                </div>
                <div class="project-info">
                  <h4>{{ sousRubrique.designation }}</h4>
                  <p>{{ sousRubrique.description || 'Description du projet' }}</p>
                  <div v-if="sousRubrique.documentsRequis?.length > 0" class="documents-list">
                    <small><strong>Documents requis:</strong></small>
                    <ul>
                      <li v-for="doc in sousRubrique.documentsRequis" :key="doc">{{ doc }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Étape 2: Informations de base -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="component-card">
        <h2><i class="pi pi-user"></i> Informations de l'agriculteur</h2>
        <div class="form-grid">
          <div class="form-group">
            <label for="cin" class="required">CIN</label>
            <InputText 
              id="cin" 
              v-model="formData.agriculteur.cin" 
              placeholder="Numéro d'identité nationale"
              @blur="searchAgriculteur"
              :class="{ 'p-invalid': validationErrors.cin }"
            />
            <small class="p-error">{{ validationErrors.cin }}</small>
          </div>

          <div class="form-group">
            <label for="nom" class="required">Nom</label>
            <InputText 
              id="nom" 
              v-model="formData.agriculteur.nom" 
              placeholder="Nom de famille"
              :class="{ 'p-invalid': validationErrors.nom }"
            />
            <small class="p-error">{{ validationErrors.nom }}</small>
          </div>

          <div class="form-group">
            <label for="prenom" class="required">Prénom</label>
            <InputText 
              id="prenom" 
              v-model="formData.agriculteur.prenom" 
              placeholder="Prénom"
              :class="{ 'p-invalid': validationErrors.prenom }"
            />
            <small class="p-error">{{ validationErrors.prenom }}</small>
          </div>

          <div class="form-group">
            <label for="telephone" class="required">Téléphone</label>
            <InputText 
              id="telephone" 
              v-model="formData.agriculteur.telephone" 
              placeholder="+212 6XX XXX XXX"
              :class="{ 'p-invalid': validationErrors.telephone }"
            />
            <small class="p-error">{{ validationErrors.telephone }}</small>
          </div>

          <div class="form-group">
            <label for="province" class="required">Province</label>
            <Dropdown 
              id="province" 
              v-model="selectedProvince" 
              :options="provinces"
              option-label="designation"
              option-value="id"
              placeholder="Province"
              @change="onProvinceChange"
              :class="{ 'p-invalid': validationErrors.province }"
            />
            <small class="p-error">{{ validationErrors.province }}</small>
          </div>

          <div class="form-group">
            <label for="cercle" class="required">Cercle</label>
            <Dropdown 
              id="cercle" 
              v-model="selectedCercle" 
              :options="cercles"
              option-label="designation"
              option-value="id"
              placeholder="Sélectionner un cercle"
              :disabled="!selectedProvince"
              @change="onCercleChange"
              :class="{ 'p-invalid': validationErrors.cercle }"
            />
            <small class="p-error">{{ validationErrors.cercle }}</small>
          </div>

          <div class="form-group">
            <label for="commune" class="required">Commune Rurale</label>
            <Dropdown 
              id="commune" 
              v-model="formData.agriculteur.communeRuraleId" 
              :options="communesRurales"
              option-label="designation"
              option-value="id"
              placeholder="Sélectionner une commune rurale"
              :disabled="!selectedCercle"
              @change="onCommuneChange"
              :class="{ 'p-invalid': validationErrors.communeRuraleId }"
            />
            <small class="p-error">{{ validationErrors.communeRuraleId }}</small>
          </div>

          <div class="form-group">
            <label for="douar">Douar</label>
            <Dropdown 
              id="douar" 
              v-model="formData.agriculteur.douarId" 
              :options="douars"
              option-label="designation"
              option-value="id"
              placeholder="Sélectionner un douar"
              :disabled="!formData.agriculteur.communeRuraleId"
            />
          </div>
        </div>
      </div>

      <div class="component-card">
        <h2><i class="pi pi-folder"></i> Informations du dossier</h2>
        <div class="form-grid">
          <div class="form-group">
            <label for="saba" class="required">SABA (généré automatiquement)</label>
            <InputText 
              id="saba" 
              v-model="formData.dossier.saba" 
              placeholder="000000/2025/001"
              readonly
              :class="{ 'p-invalid': validationErrors.saba }"
            />
            <small class="form-help">Numéro généré automatiquement</small>
            <small class="p-error">{{ validationErrors.saba }}</small>
          </div>

          <div class="form-group">
            <label>CDA (automatique)</label>
            <div class="cda-info">
              <div v-if="userCDA" class="user-cda-display">
                <i class="pi pi-building"></i>
                <div>
                  <strong>{{ userCDA.description }}</strong>
                  <small>{{ userCDA.antenneNom }}</small>
                </div>
              </div>
              <div v-else class="loading-cda">
                <i class="pi pi-spin pi-spinner"></i>
                Chargement du CDA...
              </div>
            </div>
          </div>

          <div class="form-group">
            <label for="dateDepot" class="required">Date de dépôt</label>
            <Calendar 
              id="dateDepot" 
              v-model="formData.dossier.dateDepot" 
              date-format="dd/mm/yy"
              :class="{ 'p-invalid': validationErrors.dateDepot }"
            />
            <small class="p-error">{{ validationErrors.dateDepot }}</small>
          </div>

          <div class="form-group">
            <label for="montant" class="required">Montant demandé (DH)</label>
            <InputNumber 
              id="montant" 
              v-model="formData.dossier.montantDemande" 
              mode="currency" 
              currency="MAD" 
              locale="fr-MA"
              :class="{ 'p-invalid': validationErrors.montantDemande }"
            />
            <small class="p-error">{{ validationErrors.montantDemande }}</small>
          </div>

          <div class="form-group full-width">
            <div class="selected-project-info">
              <h4>Projet sélectionné:</h4>
              <p><strong>{{ selectedSousRubrique?.designation }}</strong></p>
              <small>{{ selectedSousRubrique?.documentsRequis?.length || 0 }} document(s) requis</small>
            </div>
          </div>
        </div>

        <div class="step-actions">
          <Button 
            label="Retour" 
            icon="pi pi-arrow-left" 
            @click="previousStep"
            class="p-button-outlined"
          />
          <div class="action-group">
            <Button 
              label="Générer récépissé" 
              icon="pi pi-file-pdf" 
              @click="generateRecepisse"
              class="p-button-secondary"
              :disabled="!isBasicInfoValid"
            />
            <Button 
              label="Continuer" 
              icon="pi pi-arrow-right" 
              @click="nextStep"
              :disabled="!isBasicInfoValid"
              class="btn-primary"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Étape 3: Aperçu et finalisation -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="component-card">
        <h2><i class="pi pi-eye"></i> Aperçu du dossier</h2>
        
        <div class="summary-sections">
          <div class="summary-section">
            <h3>Informations agriculteur</h3>
            <div class="info-grid">
              <div><strong>Nom complet:</strong> {{ formData.agriculteur.prenom }} {{ formData.agriculteur.nom }}</div>
              <div><strong>CIN:</strong> {{ formData.agriculteur.cin }}</div>
              <div><strong>Téléphone:</strong> {{ formData.agriculteur.telephone }}</div>
              <div v-if="selectedCommuneName"><strong>Commune Rurale:</strong> {{ selectedCommuneName }}</div>
              <div v-if="selectedDouarName"><strong>Douar:</strong> {{ selectedDouarName }}</div>
            </div>
          </div>

          <div class="summary-section">
            <h3>Informations projet</h3>
            <div class="info-grid">
              <div><strong>Type:</strong> {{ selectedSousRubrique?.designation }}</div>
              <div><strong>SABA:</strong> {{ formData.dossier.saba }}</div>
              <div><strong>Montant:</strong> {{ formatCurrency(formData.dossier.montantDemande) }}</div>
              <div><strong>CDA:</strong> {{ userCDA?.description }}</div>
            </div>
          </div>

          <div class="summary-section" v-if="selectedSousRubrique?.documentsRequis?.length > 0">
            <h3>Documents requis</h3>
            <div class="documents-summary">
              <div 
                v-for="document in selectedSousRubrique.documentsRequis"
                :key="document"
                class="document-item"
              >
                <i class="pi pi-file"></i>
                <span>{{ document }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="step-actions">
          <Button 
            label="Retour" 
            icon="pi pi-arrow-left" 
            @click="previousStep"
            class="p-button-outlined"
          />
          <Button 
            label="Créer le dossier" 
            icon="pi pi-check" 
            @click="createDossier"
            class="p-button-success"
            :loading="loading.create"
          />
        </div>
      </div>
    </div>

    <!-- Auto-save indicator -->
    <div v-if="autoSaving" class="auto-save-indicator">
      <i class="pi pi-save"></i>
      Sauvegarde automatique...
    </div>

    <!-- Printable Receipt Component (hidden, only for printing) -->
    <PrintableReceipt 
      v-if="recepisse" 
      :receipt="recepisse" 
      style="display: none;"
      id="printable-receipt"
    />

    <!-- Composant de récépissé (modal for preview) -->
    <Dialog 
      v-model:visible="showRecepisse" 
      modal 
      header="Récépissé de dépôt"
      :style="{ width: '600px' }"
    >
      <div v-if="recepisse" class="recepisse-preview">
        <div class="recepisse-body">
          <div class="recepisse-section">
            <h4>Informations du demandeur</h4>
            <div class="recepisse-row">
              <strong>Agriculteur:</strong> {{ recepisse.nomComplet }}
            </div>
            <div class="recepisse-row">
              <strong>CIN:</strong> {{ recepisse.cin }}
            </div>
            <div class="recepisse-row">
              <strong>Téléphone:</strong> {{ recepisse.telephone }}
            </div>
          </div>

          <div class="recepisse-section">
            <h4>Informations du dossier</h4>
            <div class="recepisse-row">
              <strong>Type de projet:</strong> {{ recepisse.typeProduit }}
            </div>
            <div class="recepisse-row">
              <strong>SABA:</strong> {{ recepisse.saba }}
            </div>
            <div class="recepisse-row">
              <strong>Montant demandé:</strong> {{ formatCurrency(recepisse.montantDemande) }}
            </div>
            <div class="recepisse-row">
              <strong>CDA:</strong> {{ recepisse.cdaNom }}
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <Button label="Imprimer" icon="pi pi-print" @click="printRecepisse" class="p-button-primary" />
        <Button label="Fermer" icon="pi pi-times" @click="showRecepisse = false" class="p-button-outlined" />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useToast } from 'primevue/usetoast';
import UserInfoHeader from '@/components/UserInfoHeader.vue';
import PrintableReceipt from '@/components/agent_antenne/PrintableReceipt.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const toast = useToast();

// Constants
const STORAGE_KEY = 'dossier-creation-draft';
const AUTO_SAVE_DELAY = 1000;

// État des étapes
const currentStep = ref(1);
const steps = [
  { label: 'Type de projet' },
  { label: 'Informations de base' },
  { label: 'Finalisation' }
];

// Données du formulaire avec auto-save
const formData = ref({
  agriculteur: {
    cin: '',
    nom: '',
    prenom: '',
    telephone: '',
    communeRuraleId: null,
    douarId: null
  },
  dossier: {
    saba: '',
    reference: '',
    sousRubriqueId: null,
    dateDepot: new Date(),
    montantDemande: null
  }
});

// User's CDA (auto-loaded)
const userCDA = ref(null);

// États de l'interface
const loading = ref({
  rubriques: false,
  create: false,
  geographic: false
});

const autoSaving = ref(false);
let autoSaveTimeout = null;

// Données
const rubriques = ref([]);
const provinces = ref([]);
const cercles = ref([]);
const communesRurales = ref([]);
const douars = ref([]);

// Sélections
const selectedSousRubrique = ref(null);
const selectedProvince = ref(null);
const selectedCercle = ref(null);

const validationErrors = ref({});
const showRecepisse = ref(false);
const recepisse = ref(null);

// Computed properties
const isBasicInfoValid = computed(() => {
  return formData.value.agriculteur.cin && 
         formData.value.agriculteur.nom && 
         formData.value.agriculteur.prenom && 
         formData.value.agriculteur.telephone &&
         formData.value.agriculteur.communeRuraleId &&
         formData.value.dossier.saba &&
         userCDA.value &&
         formData.value.dossier.montantDemande;
});

const selectedCommuneName = computed(() => {
  return communesRurales.value.find(c => c.id === formData.value.agriculteur.communeRuraleId)?.designation;
});

const selectedDouarName = computed(() => {
  return douars.value.find(d => d.id === formData.value.agriculteur.douarId)?.designation;
});

// Auto-save functionality
const loadSavedData = () => {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved) {
    try {
      const parsedData = JSON.parse(saved);
      formData.value = { ...formData.value, ...parsedData.formData };
      
      // Restore selections
      if (parsedData.selectedProvince) selectedProvince.value = parsedData.selectedProvince;
      if (parsedData.selectedCercle) selectedCercle.value = parsedData.selectedCercle;
      if (parsedData.selectedSousRubrique) selectedSousRubrique.value = parsedData.selectedSousRubrique;
      if (parsedData.currentStep) currentStep.value = parsedData.currentStep;
      
      // Restore dependent dropdowns
      if (parsedData.selectedProvince) onProvinceChange();
      if (parsedData.selectedCercle) onCercleChange();
      if (parsedData.formData?.agriculteur?.communeRuraleId) onCommuneChange();
      
    } catch (e) {
      console.warn('Failed to load saved data:', e);
    }
  }
};

const autoSave = () => {
  if (autoSaveTimeout) clearTimeout(autoSaveTimeout);
  
  autoSaveTimeout = setTimeout(() => {
    try {
      autoSaving.value = true;
      const dataToSave = {
        formData: formData.value,
        selectedProvince: selectedProvince.value,
        selectedCercle: selectedCercle.value,
        selectedSousRubrique: selectedSousRubrique.value,
        currentStep: currentStep.value,
        timestamp: Date.now()
      };
      
      localStorage.setItem(STORAGE_KEY, JSON.stringify(dataToSave));
      
      setTimeout(() => {
        autoSaving.value = false;
      }, 500);
    } catch (e) {
      console.warn('Failed to auto-save:', e);
      autoSaving.value = false;
    }
  }, AUTO_SAVE_DELAY);
};

// Watch for changes to auto-save
watch(formData, autoSave, { deep: true });
watch([selectedProvince, selectedCercle, selectedSousRubrique, currentStep], autoSave);

// Méthodes du cycle de vie
onMounted(async () => {
  loadSavedData();
  await loadInitializationData();
});

// Méthodes
async function loadInitializationData() {
  try {
    loading.value.rubriques = true;
    const response = await ApiService.get('/agent_antenne/dossiers/initialization-data');
    
    // Set all data from single endpoint
    userCDA.value = response.userCDA;
    rubriques.value = response.rubriques || [];
    provinces.value = response.provinces || [];
    
    // Set generated SABA if not already set
    if (!formData.value.dossier.saba) {
      formData.value.dossier.saba = response.generatedSaba;
    }
    
    // Auto-select province if only one available
    if (provinces.value.length === 1) {
      selectedProvince.value = provinces.value[0].id;
      onProvinceChange();
    }
  } catch (error) {
    console.error('Erreur lors du chargement des données:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les données nécessaires',
      life: 3000
    });
  } finally {
    loading.value.rubriques = false;
  }
}

async function onProvinceChange() {
  if (selectedProvince.value) {
    try {
      const response = await ApiService.get(`/agent_antenne/dossiers/cercles/${selectedProvince.value}`);
      cercles.value = response || [];
      
      // Reset dependent selections
      selectedCercle.value = null;
      communesRurales.value = [];
      douars.value = [];
      formData.value.agriculteur.communeRuraleId = null;
      formData.value.agriculteur.douarId = null;
    } catch (error) {
      console.error('Erreur lors du chargement des cercles:', error);
    }
  }
}

async function onCercleChange() {
  if (selectedCercle.value) {
    try {
      const response = await ApiService.get(`/agent_antenne/dossiers/communes/${selectedCercle.value}`);
      communesRurales.value = response || [];
      
      // Reset dependent selections
      douars.value = [];
      formData.value.agriculteur.communeRuraleId = null;
      formData.value.agriculteur.douarId = null;
    } catch (error) {
      console.error('Erreur lors du chargement des communes:', error);
    }
  }
}

async function onCommuneChange() {
  if (formData.value.agriculteur.communeRuraleId) {
    try {
      const response = await ApiService.get(`/agent_antenne/dossiers/douars/${formData.value.agriculteur.communeRuraleId}`);
      douars.value = response || [];
      
      // Reset douar selection
      formData.value.agriculteur.douarId = null;
    } catch (error) {
      console.error('Erreur lors du chargement des douars:', error);
    }
  }
}

function selectSousRubrique(sousRubrique) {
  selectedSousRubrique.value = sousRubrique;
  formData.value.dossier.sousRubriqueId = sousRubrique.id;
  
  // Auto-advance to next step
  nextStep();
}

function nextStep() {
  if (validateCurrentStep()) {
    currentStep.value++;
  }
}

function previousStep() {
  if (currentStep.value > 1) {
    currentStep.value--;
  }
}

function validateCurrentStep() {
  validationErrors.value = {};
  
  switch (currentStep.value) {
    case 1:
      if (!selectedSousRubrique.value) {
        toast.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Veuillez sélectionner un type de projet',
          life: 3000
        });
        return false;
      }
      break;
      
    case 2:
      return validateBasicInfo();
      
    default:
      return true;
  }
  
  return true;
}

function validateBasicInfo() {
  const errors = {};
  
  if (!formData.value.agriculteur.cin) {
    errors.cin = 'CIN requis';
  }
  
  if (!formData.value.agriculteur.nom) {
    errors.nom = 'Nom requis';
  }
  
  if (!formData.value.agriculteur.prenom) {
    errors.prenom = 'Prénom requis';
  }
  
  if (!formData.value.agriculteur.telephone) {
    errors.telephone = 'Téléphone requis';
  }
  
  if (!formData.value.agriculteur.communeRuraleId) {
    errors.communeRuraleId = 'Commune rurale requise';
  }
  
  if (!formData.value.dossier.saba) {
    errors.saba = 'SABA requis';
  }
  
  if (!userCDA.value) {
    errors.cda = 'CDA non chargé';
  }
  
  if (!formData.value.dossier.montantDemande) {
    errors.montantDemande = 'Montant requis';
  }
  
  validationErrors.value = errors;
  return Object.keys(errors).length === 0;
}

async function createDossier() {
  try {
    loading.value.create = true;
    
    const response = await ApiService.post('/agent_antenne/dossiers/create', formData.value);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier créé avec succès',
      life: 5000
    });
    
    // Clear saved data after successful creation
    localStorage.removeItem(STORAGE_KEY);
    
    // Afficher le récépissé
    recepisse.value = response.recepisse;
    showRecepisse.value = true;
    
    // Réinitialiser le formulaire après un délai
    setTimeout(() => {
      resetForm();
    }, 2000);
    
  } catch (error) {
    console.error('Erreur lors de la création du dossier:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Impossible de créer le dossier',
      life: 5000
    });
  } finally {
    loading.value.create = false;
  }
}

async function generateRecepisse() {
  if (!isBasicInfoValid.value) {
    toast.add({
      severity: 'warn',
      summary: 'Attention',
      detail: 'Veuillez compléter toutes les informations requises',
      life: 3000
    });
    return;
  }
  
  // Générer un récépissé temporaire
  const tempRecepisse = {
    numeroRecepisse: `R-${new Date().getFullYear()}-${String(Date.now()).slice(-6)}`,
    dateDepot: new Date(),
    nomComplet: `${formData.value.agriculteur.prenom} ${formData.value.agriculteur.nom}`,
    cin: formData.value.agriculteur.cin,
    telephone: formData.value.agriculteur.telephone,
    typeProduit: selectedSousRubrique.value?.designation || '',
    saba: formData.value.dossier.saba,
    montantDemande: formData.value.dossier.montantDemande,
    cdaNom: userCDA.value?.description || '',
    antenne: userCDA.value?.antenneNom || ''
  };
  
  recepisse.value = tempRecepisse;
  showRecepisse.value = true;
}

function printRecepisse() {
  // Focus on the printable component
  const printElement = document.getElementById('printable-receipt');
  if (printElement) {
    printElement.style.display = 'block';
    
    // Trigger print
    setTimeout(() => {
      window.print();
      
      // Hide after print
      setTimeout(() => {
        printElement.style.display = 'none';
      }, 100);
    }, 100);
  }
}

async function searchAgriculteur() {
  if (formData.value.agriculteur.cin.length >= 8) {
    try {
      console.log('Recherche agriculteur:', formData.value.agriculteur.cin);
    } catch (error) {
      console.error('Erreur lors de la recherche:', error);
    }
  }
}

function handleSearch(query) {
  console.log('Recherche:', query);
}

function resetForm() {
  formData.value = {
    agriculteur: {
      cin: '',
      nom: '',
      prenom: '',
      telephone: '',
      communeRuraleId: null,
      douarId: null
    },
    dossier: {
      saba: '',
      reference: '',
      sousRubriqueId: null,
      dateDepot: new Date(),
      montantDemande: null
    }
  };
  
  selectedSousRubrique.value = null;
  selectedProvince.value = null;
  selectedCercle.value = null;
  currentStep.value = 1;
  validationErrors.value = {};
  
  // Clear saved data
  localStorage.removeItem(STORAGE_KEY);
  
  // Reload initialization data to get new SABA
  loadInitializationData();
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
.create-dossier-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
}

/* Auto-save indicator */
.auto-save-indicator {
  position: fixed;
  bottom: 1rem;
  left: 1rem;
  background: var(--primary-color);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  z-index: 1000;
  animation: fadeInOut 2s ease-in-out;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

/* Stepper */
.stepper-container {
  margin-bottom: var(--component-spacing);
}

.stepper {
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--background-color);
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  gap: 2rem;
}

.step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
}

.step:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -1.5rem;
  width: 2rem;
  height: 2px;
  background: #e5e7eb;
  z-index: 1;
}

.step.active:not(:last-child)::after,
.step.completed:not(:last-child)::after {
  background: var(--primary-color);
}

.step-number {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  position: relative;
  z-index: 2;
}

.step.active .step-number {
  background: var(--primary-color);
  color: white;
}

.step.completed .step-number {
  background: #10b981;
  color: white;
}

.step-label {
  font-weight: 500;
  color: #6b7280;
  font-size: 0.875rem;
  margin-right: 1rem;
}

.step.active .step-label {
  color: var(--primary-color);
  font-weight: 600;
}

/* Project Types */
.project-types-grid {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.rubrique-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid var(--border-color);
}

.rubrique-card h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-color);
}

.rubrique-description {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 1rem;
  font-style: italic;
}

.sous-rubriques {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.project-type {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.project-type:hover {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(1, 114, 62, 0.1);
}

.project-type.selected {
  border-color: var(--primary-color);
  background: rgba(1, 114, 62, 0.05);
}

.project-icon {
  font-size: 2rem;
  color: var(--primary-color);
  min-width: 2rem;
  margin-top: 0.25rem;
}

/* Project Info with Document List */
.project-info h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.25rem;
}

.project-info p {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0 0 0.5rem 0;
}

.documents-list {
  margin-top: 0.5rem;
}

.documents-list small {
  color: #6b7280;
  font-size: 0.75rem;
}

.documents-list ul {
  margin: 0.25rem 0 0 0;
  padding-left: 1rem;
  list-style-type: disc;
}

.documents-list li {
  font-size: 0.75rem;
  color: #6b7280;
  margin: 0.125rem 0;
}

/* CDA Display */
.cda-info {
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #f8f9fa;
}

.user-cda-display {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-cda-display i {
  color: var(--primary-color);
  font-size: 1.25rem;
}

.user-cda-display div {
  display: flex;
  flex-direction: column;
}

.user-cda-display strong {
  color: var(--text-color);
  font-weight: 600;
}

.user-cda-display small {
  color: #6b7280;
  font-size: 0.8rem;
}

.loading-cda {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.dark-mode .cda-info {
  background: #374151;
  border-color: #4b5563;
}

/* Form Styles */
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.form-group label.required::after {
  content: ' *';
  color: #dc2626;
}

.form-help {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

.selected-project-info {
  background: rgba(1, 114, 62, 0.05);
  border: 1px solid rgba(1, 114, 62, 0.2);
  border-radius: 8px;
  padding: 1rem;
}

.selected-project-info h4 {
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

/* Summary */
.summary-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.summary-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.summary-section h3 {
  color: var(--primary-color);
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 0.75rem;
}

.info-grid > div {
  font-size: 0.875rem;
  color: #374151;
}

.documents-summary {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #374151;
}

.document-item i {
  color: var(--primary-color);
}

/* Actions */
.step-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.action-group {
  display: flex;
  gap: 0.75rem;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%);
  border: none;
  color: white;
}

/* Récépissé */
.recepisse-content {
  text-align: center;
}

.recepisse-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--primary-color);
}

.recepisse-header h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.recepisse-body {
  text-align: left;
}

.recepisse-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.recepisse-row:last-child {
  border-bottom: none;
}

/* Responsive */
@media (max-width: 768px) {
  .stepper {
    flex-direction: column;
    gap: 1rem;
  }
  
  .step:not(:last-child)::after {
    display: none;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .sous-rubriques {
    grid-template-columns: 1fr;
  }
  
  .step-actions {
    flex-direction: column;
    gap: 1rem;
  }
  
  .action-group {
    width: 100%;
    justify-content: center;
  }
}

/* Dark mode support */
.dark-mode .stepper,
.dark-mode .component-card,
.dark-mode .project-type,
.dark-mode .summary-section,
.dark-mode .rubrique-card {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .step-number {
  background-color: #374151;
  color: #9ca3af;
}

.dark-mode .step.active .step-number {
  background-color: var(--primary-color);
  color: white;
}
</style>