<template>
  <div class="create-dossier-container">
    <!-- Stepper -->
    <div class="component-card stepper-container">
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
        
        <!-- Project Type Search Bar -->
        <div class="search-section">
          <div class="search-container">
            <InputText 
              v-model="projectSearchTerm" 
              placeholder="Rechercher un type de projet..." 
              @input="searchProjectTypes"
              class="search-input"
            />
            <i class="pi pi-search search-icon"></i>
          </div>
          <Button 
            v-if="projectSearchTerm" 
            icon="pi pi-times" 
            @click="clearProjectSearch"
            class="p-button-text clear-search-btn"
            v-tooltip.top="'Effacer la recherche'"
          />
        </div>

        <!-- Search Results -->
        <div v-if="searchResults.length > 0 && projectSearchTerm" class="search-results">
          <h3>Résultats de recherche ({{ searchResults.length }})</h3>
          <div class="search-results-grid">
            <div 
              v-for="sousRubrique in searchResults"
              :key="sousRubrique.id"
              class="project-type search-result"
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

        <!-- All Project Types -->
        <div v-show="!projectSearchTerm || searchResults.length === 0" class="project-types-grid">
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
<div class="title-section">
    <h2><i class="pi pi-user"></i> Informations de l'agriculteur</h2>
    <Button 
      label="Réinitialiser le formulaire" 
      icon="pi pi-refresh" 
      @click="resetForm"
      class="p-button-danger p-button-outlined"
    />
  </div>        <div class="form-grid">
          <div class="form-group">
            <label for="cin" class="required">CIN</label>
            <div class="cin-input-group">
              <InputText 
                id="cin" 
                v-model="formData.agriculteur.cin" 
                placeholder="Numéro d'identité nationale"
                @input="onCinInput"
                :class="{ 'p-invalid': validationErrors.cin }"
                class="cin-input"
              />
              <Button 
                label="Vérifier" 
                icon="pi pi-search" 
                @click="checkFarmerExists"
                :loading="farmerCheckStatus.checking"
                :disabled="!formData.agriculteur.cin || formData.agriculteur.cin.trim().length < 5"
                class="p-button-outlined check-btn"
                size="small"
              />
            </div>
            <small class="p-error">{{ validationErrors.cin }}</small>
            
            <!-- Farmer Found Information -->
            <div v-if="farmerCheckStatus.found && farmerCheckStatus.data" class="farmer-info-card">
              <div class="farmer-info-header">
                <i class="pi pi-user"></i>
                <strong>Agriculteur existant</strong>
              </div>
              <div class="farmer-details">
                <p><strong>Nom:</strong> {{ farmerCheckStatus.data.nom }} {{ farmerCheckStatus.data.prenom }}</p>
                <p><strong>Téléphone:</strong> {{ farmerCheckStatus.data.telephone }}</p>
                <p v-if="farmerCheckStatus.previousDossiers.length > 0">
                  <strong>Dossiers précédents:</strong> {{ farmerCheckStatus.previousDossiers.length }}
                </p>
              </div>
              <Button 
                label="Utiliser ces informations" 
                icon="pi pi-check" 
                @click="useFarmerData"
                class="p-button-sm p-button-success"
                size="small"
              />
            </div>
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
            <Select 
              :key="`province-${refreshKey}`"
              id="province" 
              v-model="selectedProvince" 
              :options="provinces"
              optionLabel="designation"
              optionValue="id"
              placeholder="Sélectionner une province"
              @change="onProvinceChange"
              @update:modelValue="triggerAutoSave"
              :class="{ 'p-invalid': validationErrors.province }"
              class="w-full"
            />
            <small class="p-error">{{ validationErrors.province }}</small>
          </div>

          <div class="form-group">
            <label for="cercle" class="required">Cercle</label>
            <Select 
              :key="`cercle-${refreshKey}`"
              id="cercle" 
              v-model="selectedCercle" 
              :options="cercles"
              optionLabel="designation"
              optionValue="id"
              placeholder="Sélectionner un cercle"
              :disabled="!selectedProvince"
              @change="onCercleChange"
              @update:modelValue="triggerAutoSave"
              :class="{ 'p-invalid': validationErrors.cercle }"
              class="w-full"
            />
            <small class="p-error">{{ validationErrors.cercle }}</small>
          </div>

          <div class="form-group">
            <label for="commune" class="required">Commune Rurale</label>
            <Select 
              :key="`commune-${refreshKey}`"
              id="commune" 
              v-model="formData.agriculteur.communeRuraleId" 
              :options="communesRurales"
              optionLabel="designation"
              optionValue="id"
              placeholder="Sélectionner une commune rurale"
              :disabled="!selectedCercle"
              @change="onCommuneChange"
              @update:modelValue="triggerAutoSave"
              :class="{ 'p-invalid': validationErrors.communeRuraleId }"
              class="w-full"
            />
            <small class="p-error">{{ validationErrors.communeRuraleId }}</small>
          </div>

          <div class="form-group">
            <label for="douar">Douar</label>
            <Select 
              :key="`douar-${refreshKey}`"
              id="douar" 
              v-model="formData.agriculteur.douarId" 
              :options="douars"
              optionLabel="designation"
              optionValue="id"
              placeholder="Sélectionner un douar"
              :disabled="!formData.agriculteur.communeRuraleId"
              @update:modelValue="triggerAutoSave"
              class="w-full"
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
            <label for="antenne" class="required">Antenne</label>
            <InputText 
              id="antenne" 
              :value="userAntenne?.designation || 'Chargement...'" 
              readonly
              class="w-full"
            />
            <small class="form-help">Votre antenne par défaut</small>
          </div>

          <div class="form-group">
            <label for="dateDepot" class="required">Date de dépôt</label>
            <InputText 
              id="dateDepot" 
              :value="formatDate(formData.dossier.dateDepot)"
              readonly
              class="w-full"
            />
            <small class="form-help">Date du jour</small>
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
              
              <!-- Documents requis list -->
              <div v-if="selectedSousRubrique?.documentsRequis?.length > 0" class="documents-required-list">
                <h5>Documents requis:</h5>
                <ul>
                  <li v-for="document in selectedSousRubrique.documentsRequis" :key="document">
                    <i class="pi pi-file"></i>
                    <span>{{ document }}</span>
                  </li>
                </ul>
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
              <div><strong>Antenne:</strong> {{ getSelectedAntenneName() }}</div>
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
            class="p-button-success btn-primary"
            :loading="loading.create"
          />
        </div>
      </div>
    </div>

    <!-- Save notification for important events only -->
    <div v-if="saveNotification.show" class="save-notification" :class="saveNotification.type">
      <i :class="saveNotification.icon"></i>
      <span>{{ saveNotification.message }}</span>
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
            <div v-if="recepisse.reference" class="recepisse-row">
              <strong>Référence:</strong> {{ recepisse.reference }}
            </div>
            <div class="recepisse-row">
              <strong>Montant demandé:</strong> {{ formatCurrency(recepisse.montantDemande) }}
            </div>
            <div class="recepisse-row">
              <strong>Antenne:</strong> {{ recepisse.antenneName || recepisse.antenne }}
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
import { ref, onMounted, computed, watch, onBeforeUnmount, nextTick } from 'vue';
import { useToast } from 'primevue/usetoast';
import PrintableReceipt from '@/components/agent_antenne/dossier_create/PrintableReceipt.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Select from 'primevue/select';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const toast = useToast();

// Constants
const STORAGE_KEY = 'dossier-creation-draft';
const AUTO_SAVE_DELAY = 2000; // Increased delay for better UX
const SAVE_NOTIFICATION_DURATION = 3000;

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
    antenneId: null,
    dateDepot: new Date(),
    montantDemande: null
  }
});

// User's Antenne (auto-loaded)
const userAntenne = ref(null);
const antennes = ref([]);

// États de l'interface
const loading = ref({
  initialization: false,
  create: false,
  geographic: false
});

// Save notification system for important events only
const saveNotification = ref({
  show: false,
  type: 'success', // 'success', 'error', 'warning'
  message: '',
  icon: ''
});

const isRestoring = ref(false); // Flag to prevent conflicts during restoration
const refreshKey = ref(0); // Key to force component refresh
let autoSaveTimeout = null;
let saveNotificationTimeout = null;

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

// Search functionality
const projectSearchTerm = ref('');
const searchResults = ref([]);
const isSearching = ref(false);

// Farmer checking
const farmerCheckStatus = ref({
  checking: false,
  found: false,
  data: null,
  previousDossiers: []
});

// Computed properties
const isBasicInfoValid = computed(() => {
  return formData.value.agriculteur.cin && 
         formData.value.agriculteur.nom && 
         formData.value.agriculteur.prenom && 
         formData.value.agriculteur.telephone &&
         formData.value.agriculteur.communeRuraleId &&
         formData.value.dossier.saba &&
         formData.value.dossier.montantDemande &&
         selectedSousRubrique.value;
});

const selectedCommuneName = computed(() => {
  return communesRurales.value.find(c => c.id === formData.value.agriculteur.communeRuraleId)?.designation;
});

const selectedDouarName = computed(() => {
  return douars.value.find(d => d.id === formData.value.agriculteur.douarId)?.designation;
});

// Enhanced Auto-save functionality
const loadSavedData = async () => {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved) {
    try {
      isRestoring.value = true; // Prevent auto-save during restoration
      const parsedData = JSON.parse(saved);
      console.log('Loading saved data:', parsedData); // Debug log
      
      // Check if saved data is not too old (24 hours)
      const saveAge = Date.now() - (parsedData.timestamp || 0);
      const maxAge = 24 * 60 * 60 * 1000; // 24 hours
      
      if (saveAge > maxAge) {
        localStorage.removeItem(STORAGE_KEY);
        showSaveNotification('Brouillon expiré supprimé', 'warning', 'pi pi-exclamation-triangle');
        isRestoring.value = false;
        return;
      }
      
      // Restore form data first
      formData.value = { ...formData.value, ...parsedData.formData };
      
      // Restore step
      if (parsedData.currentStep) currentStep.value = parsedData.currentStep;
      
      // Restore sous-rubrique selection
      if (parsedData.selectedSousRubrique) {
        selectedSousRubrique.value = parsedData.selectedSousRubrique;
      }
      
      // Restore geographic cascade step by step
      await restoreGeographicSelections(parsedData);
      
      console.log('Data restoration completed'); // Debug log
      
      // Force refresh of Select components
      refreshKey.value++;
          
    } catch (e) {
      console.warn('Failed to load saved data:', e);
      localStorage.removeItem(STORAGE_KEY);
      showSaveNotification('Erreur lors de la restauration', 'error', 'pi pi-times');
    } finally {
      isRestoring.value = false; // Re-enable auto-save
    }
  }
};

const restoreGeographicSelections = async (parsedData) => {
  // Step 1: Restore province
  if (parsedData.selectedProvince && provinces.value.length > 0) {
    console.log('Restoring province:', parsedData.selectedProvince);
    
    // Load cercles for this province
    await loadCercles(parsedData.selectedProvince);
    
    // Wait for options to be available, then set province
    let attempts = 0;
    while (cercles.value.length === 0 && attempts < 10) {
      await new Promise(resolve => setTimeout(resolve, 100));
      attempts++;
    }
    
    selectedProvince.value = parsedData.selectedProvince;
    console.log('Province restored to:', selectedProvince.value);
    
    // Step 2: Restore cercle
    if (parsedData.selectedCercle) {
      console.log('Restoring cercle:', parsedData.selectedCercle);
      
      // Verify cercle exists in loaded options
      const cercleExists = cercles.value.some(c => c.id === parsedData.selectedCercle);
      if (cercleExists) {
        selectedCercle.value = parsedData.selectedCercle;
        console.log('Cercle restored to:', selectedCercle.value);
        
        // Load communes for this cercle
        await loadCommunes(parsedData.selectedCercle);
        
        // Wait for communes to be available
        attempts = 0;
        while (communesRurales.value.length === 0 && attempts < 10) {
          await new Promise(resolve => setTimeout(resolve, 100));
          attempts++;
        }
        
        // Step 3: Verify and restore commune (should already be set via formData)
        if (parsedData.formData?.agriculteur?.communeRuraleId) {
          console.log('Loading douars for commune:', parsedData.formData.agriculteur.communeRuraleId);
          
          // Verify commune exists in loaded options
          const communeExists = communesRurales.value.some(c => c.id === parsedData.formData.agriculteur.communeRuraleId);
          if (communeExists) {
            // Load douars for this commune
            await loadDouars(parsedData.formData.agriculteur.communeRuraleId);
            
            console.log('Final values:');
            console.log('- Province:', selectedProvince.value);
            console.log('- Cercle:', selectedCercle.value);
            console.log('- Commune:', formData.value.agriculteur.communeRuraleId);
            console.log('- Douar:', formData.value.agriculteur.douarId);
          } else {
            console.warn('Commune not found in loaded options:', parsedData.formData.agriculteur.communeRuraleId);
          }
        }
      } else {
        console.warn('Cercle not found in loaded options:', parsedData.selectedCercle);
      }
    }
  }
};

const autoSave = () => {
  // Don't auto-save during restoration
  if (isRestoring.value) return;
  
  if (autoSaveTimeout) clearTimeout(autoSaveTimeout);
  
  autoSaveTimeout = setTimeout(() => {
    try {
      const dataToSave = {
        formData: formData.value,
        selectedProvince: selectedProvince.value,
        selectedCercle: selectedCercle.value,
        selectedSousRubrique: selectedSousRubrique.value,
        currentStep: currentStep.value,
        timestamp: Date.now()
      };
      
      console.log('Saving data:', dataToSave); // Debug log
      localStorage.setItem(STORAGE_KEY, JSON.stringify(dataToSave));
      
    } catch (e) {
      console.warn('Failed to auto-save:', e);
      showSaveNotification('Erreur de sauvegarde automatique', 'error', 'pi pi-times');
    }
  }, AUTO_SAVE_DELAY);
};

const triggerAutoSave = () => {
  // Force trigger auto-save immediately for Select changes
  autoSave();
};

const showSaveNotification = (message, type = 'success', icon = 'pi pi-check') => {
  if (saveNotificationTimeout) clearTimeout(saveNotificationTimeout);
  
  saveNotification.value = {
    show: true,
    type,
    message,
    icon
  };
  
  saveNotificationTimeout = setTimeout(() => {
    saveNotification.value.show = false;
  }, SAVE_NOTIFICATION_DURATION);
};

const clearSavedData = () => {
  localStorage.removeItem(STORAGE_KEY);
  showSaveNotification('Brouillon supprimé', 'success', 'pi pi-trash');
};

// Watch for changes to auto-save
watch(formData, autoSave, { deep: true });
watch([selectedProvince, selectedCercle, selectedSousRubrique, currentStep], autoSave);

// Méthodes du cycle de vie
onMounted(async () => {
  await loadInitializationData();
  await loadSavedData();
});

onBeforeUnmount(() => {
  if (autoSaveTimeout) clearTimeout(autoSaveTimeout);
  if (saveNotificationTimeout) clearTimeout(saveNotificationTimeout);
});

// API Methods
async function loadInitializationData() {
  try {
    loading.value.initialization = true;
    const response = await ApiService.get('/agent_antenne/dossiers/initialization-data');
    
    // Set all data from single endpoint
    userAntenne.value = response.userAntenne;
    antennes.value = response.antennes || [];
    rubriques.value = response.rubriques || [];
    provinces.value = response.provinces || [];
    
    // Set generated SABA if not already set
    if (!formData.value.dossier.saba) {
      formData.value.dossier.saba = response.generatedSaba;
    }
    
    // Auto-select user's antenne
    if (userAntenne.value) {
      formData.value.dossier.antenneId = userAntenne.value.id;
    }
    
    // Auto-select province if only one available
    if (provinces.value.length === 1) {
      selectedProvince.value = provinces.value[0].id;
      await loadCercles(selectedProvince.value);
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
    loading.value.initialization = false;
  }
}

async function loadCercles(provinceId) {
  if (!provinceId) return;
  
  try {
    loading.value.geographic = true;
    const response = await ApiService.get(`/agent_antenne/dossiers/cercles/${provinceId}`);
    cercles.value = response || [];
    
    // Only reset dependent selections if we're not restoring saved data
    if (!isRestoring.value) {
      selectedCercle.value = null;
      communesRurales.value = [];
      douars.value = [];
      formData.value.agriculteur.communeRuraleId = null;
      formData.value.agriculteur.douarId = null;
    }
  } catch (error) {
    console.error('Erreur lors du chargement des cercles:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des cercles',
      life: 3000
    });
  } finally {
    loading.value.geographic = false;
  }
}

async function loadCommunes(cercleId) {
  if (!cercleId) return;
  
  try {
    loading.value.geographic = true;
    const response = await ApiService.get(`/agent_antenne/dossiers/communes/${cercleId}`);
    communesRurales.value = response || [];
    
    // Only reset dependent selections if we're not restoring saved data
    if (!isRestoring.value) {
      douars.value = [];
      formData.value.agriculteur.communeRuraleId = null;
      formData.value.agriculteur.douarId = null;
    }
  } catch (error) {
    console.error('Erreur lors du chargement des communes:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des communes',
      life: 3000
    });
  } finally {
    loading.value.geographic = false;
  }
}

async function loadDouars(communeId) {
  if (!communeId) return;
  
  try {
    loading.value.geographic = true;
    const response = await ApiService.get(`/agent_antenne/dossiers/douars/${communeId}`);
    douars.value = response || [];
    
    // Only reset douar selection if we're not restoring saved data
    if (!isRestoring.value && !formData.value.agriculteur.douarId) {
      formData.value.agriculteur.douarId = null;
    }
  } catch (error) {
    console.error('Erreur lors du chargement des douars:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des douars',
      life: 3000
    });
  } finally {
    loading.value.geographic = false;
  }
}

// Event handlers
async function onProvinceChange() {
  if (selectedProvince.value && !isRestoring.value) {
    await loadCercles(selectedProvince.value);
  }
}

async function onCercleChange() {
  if (selectedCercle.value && !isRestoring.value) {
    await loadCommunes(selectedCercle.value);
  }
}

async function onCommuneChange() {
  if (formData.value.agriculteur.communeRuraleId && !isRestoring.value) {
    await loadDouars(formData.value.agriculteur.communeRuraleId);
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
  
  if (!formData.value.dossier.montantDemande) {
    errors.montantDemande = 'Montant requis';
  }
  
  validationErrors.value = errors;
  return Object.keys(errors).length === 0;
}

async function createDossier() {
  try {
    loading.value.create = true;
    
    // Prepare request data according to backend DTO
    const requestData = {
      agriculteur: {
        cin: formData.value.agriculteur.cin,
        nom: formData.value.agriculteur.nom,
        prenom: formData.value.agriculteur.prenom,
        telephone: formData.value.agriculteur.telephone,
        communeRuraleId: formData.value.agriculteur.communeRuraleId,
        douarId: formData.value.agriculteur.douarId
      },
      dossier: {
        saba: formData.value.dossier.saba,
        reference: formData.value.dossier.reference,
        sousRubriqueId: formData.value.dossier.sousRubriqueId,
        antenneId: formData.value.dossier.antenneId,
        dateDepot: new Date().toISOString().split('T')[0],
        montantDemande: formData.value.dossier.montantDemande
      }
    };
    
    const response = await ApiService.post('/agent_antenne/dossiers/create', requestData);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier créé avec succès',
      life: 5000
    });
    
    // Clear saved data after successful creation
    clearSavedData();
    
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
  
  // Générer un récépissé temporaire avec référence
  const tempReference = generateTempReference();
  const tempRecepisse = {
    numeroRecepisse: `R-${new Date().getFullYear()}-${String(Date.now()).slice(-6)}`,
    dateDepot: new Date(),
    nomComplet: `${formData.value.agriculteur.prenom} ${formData.value.agriculteur.nom}`,
    cin: formData.value.agriculteur.cin,
    telephone: formData.value.agriculteur.telephone,
    typeProduit: selectedSousRubrique.value?.designation || '',
    saba: formData.value.dossier.saba,
    reference: tempReference, // Add reference
    montantDemande: formData.value.dossier.montantDemande,
    antenne: getSelectedAntenneName(),
    antenneName: getSelectedAntenneName(),
    cdaNom: getSelectedAntenneName(),
    cdaName: getSelectedAntenneName()
  };
  
  recepisse.value = tempRecepisse;
  showRecepisse.value = true;
}

// Generate temporary reference for preview
function generateTempReference() {
  const annee = new Date().getFullYear();
  const antenneCode = userAntenne.value ? String(userAntenne.value.id).padStart(3, '0') : '001';
  const sousRubriqueCode = selectedSousRubrique.value ? String(selectedSousRubrique.value.id).padStart(2, '0') : '01';
  const sequence = String(Date.now()).slice(-6);
  
  return `DOS-${annee}-${antenneCode}-${sousRubriqueCode}-${sequence}`;
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

async function checkFarmerExists() {
  if (!formData.value.agriculteur.cin || formData.value.agriculteur.cin.trim().length < 5) {
    farmerCheckStatus.value = { checking: false, found: false, data: null, previousDossiers: [] };
    return;
  }

  try {
    farmerCheckStatus.value.checking = true;
    console.log('Checking farmer with CIN:', formData.value.agriculteur.cin); // Debug log
    
    const response = await ApiService.get(`/agent_antenne/dossiers/check-farmer/${formData.value.agriculteur.cin.trim()}`);
    console.log('Farmer check response:', response); // Debug log
    
    if (response.exists) {
      farmerCheckStatus.value = {
        checking: false,
        found: true,
        data: response.agriculteur,
        previousDossiers: response.previousDossiers || []
      };
      
      toast.add({
        severity: 'info',
        summary: 'Agriculteur trouvé',
        detail: `${response.agriculteur.prenom} ${response.agriculteur.nom} - ${response.previousDossiers?.length || 0} dossier(s) précédent(s)`,
        life: 4000
      });
    } else {
      farmerCheckStatus.value = {
        checking: false,
        found: false,
        data: null,
        previousDossiers: []
      };
      
      toast.add({
        severity: 'warn',
        summary: 'Agriculteur non trouvé',
        detail: 'Aucun agriculteur trouvé avec ce CIN. Veuillez saisir les informations.',
        life: 3000
      });
    }
  } catch (error) {
    console.error('Erreur lors de la vérification de l\'agriculteur:', error);
    console.error('Error details:', error.response || error); // More detailed error log
    farmerCheckStatus.value.checking = false;
    
    let errorMessage = 'Erreur lors de la vérification de l\'agriculteur';
    if (error.response?.status === 403) {
      errorMessage = 'Accès non autorisé à cette fonctionnalité';
    } else if (error.response?.status === 404) {
      errorMessage = 'Service de vérification non disponible';
    }
    
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: errorMessage,
      life: 3000
    });
  }
}

function onCinInput() {
  // Reset farmer check status when CIN is modified
  farmerCheckStatus.value = { checking: false, found: false, data: null, previousDossiers: [] };
}

function useFarmerData() {
  if (farmerCheckStatus.value.data) {
    const farmer = farmerCheckStatus.value.data;
    formData.value.agriculteur.nom = farmer.nom;
    formData.value.agriculteur.prenom = farmer.prenom;
    formData.value.agriculteur.telephone = farmer.telephone;
    
    if (farmer.communeRuraleId) {
      formData.value.agriculteur.communeRuraleId = farmer.communeRuraleId;
      // Load geographic data if needed
      loadGeographicDataForFarmer(farmer);
    }
    
    if (farmer.douarId) {
      formData.value.agriculteur.douarId = farmer.douarId;
    }
    
    toast.add({
      severity: 'success',
      summary: 'Informations utilisées',
      detail: 'Les informations de l\'agriculteur ont été pré-remplies',
      life: 3000
    });
  }
}

async function loadGeographicDataForFarmer(farmer) {
  // This function would need to determine province and cercle from the commune
  // For now, we'll just set the commune and load the douars
  if (farmer.communeRuraleId) {
    await loadDouars(farmer.communeRuraleId);
  }
}

// Project search functionality
let searchTimeout = null;

async function searchProjectTypes() {
  if (!projectSearchTerm.value || projectSearchTerm.value.length < 3) {
    searchResults.value = [];
    return;
  }

  if (searchTimeout) clearTimeout(searchTimeout);
  
  searchTimeout = setTimeout(async () => {
    try {
      isSearching.value = true;
      const response = await ApiService.get('/agent_antenne/dossiers/search-project-types', {
        searchTerm: projectSearchTerm.value
      });
      
      searchResults.value = response || [];
    } catch (error) {
      console.error('Erreur lors de la recherche:', error);
      searchResults.value = [];
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur lors de la recherche de projets',
        life: 3000
      });
    } finally {
      isSearching.value = false;
    }
  }, 300);
}

function clearProjectSearch() {
  projectSearchTerm.value = '';
  searchResults.value = [];
}

function handleSearch(query) {
  console.log('Recherche:', query);
}

async function resetForm() {
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
      antenneId: null,
      dateDepot: null,
      montantDemande: null
    }
  };
  
  selectedSousRubrique.value = null;
  selectedProvince.value = null;
  selectedCercle.value = null;
  currentStep.value = 1;
  validationErrors.value = {};
  
  // Clear saved data
  clearSavedData();
  
  // Reload initialization data to get new SABA
  await loadInitializationData();
}

function getSelectedAntenneName() {
  if (formData.value.dossier.antenneId) {
    const selectedAntenne = antennes.value.find(a => a.id === formData.value.dossier.antenneId);
    return selectedAntenne?.designation || 'N/A';
  }
  return userAntenne.value?.designation || 'N/A';
}

// Utility functions
function formatCurrency(amount) {
  if (!amount) return '0 DH';
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD'
  }).format(amount);
}

function formatDate(date) {
  if (!date) return new Intl.DateTimeFormat('fr-FR').format(new Date());
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.create-dossier-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  background: var(--background-color);
  min-height: 100vh;
}




.title-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

/* Responsive adjustment */
@media (max-width: 768px) {
  .title-section {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
}

/* Save notification for important events */
.save-notification {
  position: fixed;
  top: 1rem;
  right: 1rem;
  color: white;
  padding: 0.75rem 1.25rem;
  border-radius: var(--border-radius-lg);
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  z-index: 1001;
  animation: slideInRight 0.3s ease-out;
  box-shadow: var(--shadow-lg);
}

.save-notification.success {
  background: var(--success-color);
}

.save-notification.error {
  background: var(--danger-color);
}

.save-notification.warning {
  background: var(--warning-color);
}

@keyframes slideInRight {
  0% { transform: translateX(100%); opacity: 0; }
  100% { transform: translateX(0); opacity: 1; }
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

/* Stepper */
.stepper-container {
  margin-bottom: var(--component-spacing);
}

.stepper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--component-spacing);
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
  background: var(--border-color);
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
  background: var(--border-color);
  color: var(--text-muted);
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
  color: var(--text-on-primary);
}

.step.completed .step-number {
  background: var(--success-color);
  color: var(--text-on-primary);
}

.step-label {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin-right: 1rem;
}

.step.active .step-label {
  color: var(--primary-color);
  font-weight: 600;
}

/* Search Section */
.search-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 2rem;
}

.search-container {
  position: relative;
  flex: 1;
  max-width: 500px;
}

.search-input {
  width: 100%;
  padding-right: 2.5rem;
  font-size: 1rem;
}

.search-icon {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.clear-search-btn {
  padding: 0.5rem;
  min-width: auto;
}

/* Search Results */
.search-results {
  margin-bottom: 2rem;
}

.search-results h3 {
  color: var(--primary-color);
  font-size: 1.1rem;
  margin-bottom: 1rem;
  font-family: 'Poppins', sans-serif;
}

.search-results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 1rem;
}

.search-result {
  border: 2px solid var(--primary-color);
  background: var(--clr-surface-tonal-a0);
}

/* CIN Input with Check Button */
.cin-input-group {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.cin-input {
  flex: 1;
}

.check-btn {
  flex-shrink: 0;
  min-width: auto;
  white-space: nowrap;
}

/* Farmer Info Card */
.farmer-info-card {
  background: var(--clr-surface-tonal-a0);
  border: 1px solid var(--success-color);
  border-radius: var(--border-radius-lg);
  padding: 1rem;
  margin-top: 1rem;
}

.farmer-info-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--success-color);
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.farmer-info-header i {
  font-size: 1.1rem;
}

.farmer-details {
  margin-bottom: 1rem;
}

.farmer-details p {
  margin: 0.25rem 0;
  font-size: 0.9rem;
  color: var(--text-color);
}

.farmer-details strong {
  color: var(--primary-color);
}

/* Project Types */
.project-types-grid {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.rubrique-card {
  background: var(--section-background);
  border-radius: var(--border-radius-lg);
  padding: var(--component-spacing);
  border: 1px solid var(--border-color);
}

.rubrique-card h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-color);
  font-family: 'Poppins', sans-serif;
}

.rubrique-description {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
  font-style: italic;
}

.sous-rubriques {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.project-type {
  background: var(--card-background);
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.project-type:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
}

.project-type.selected {
  border-color: var(--primary-color);
  background: var(--clr-surface-tonal-a0);
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
  color: var(--text-color);
  margin-bottom: 0.25rem;
  font-family: 'Poppins', sans-serif;
}

.project-info p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0 0 0.5rem 0;
}

.documents-list {
  margin-top: 0.5rem;
}

.documents-list small {
  color: var(--text-secondary);
  font-size: 0.75rem;
}

.documents-list ul {
  margin: 0.25rem 0 0 0;
  padding-left: 1rem;
  list-style-type: disc;
}

.documents-list li {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin: 0.125rem 0;
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
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.form-group label.required::after {
  content: ' *';
  color: var(--danger-color);
}

.form-help {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin-top: 0.25rem;
}

.selected-project-info {
  background: var(--clr-surface-tonal-a0);
  border: 1px solid var(--clr-primary-a20);
  border-radius: var(--border-radius-lg);
  padding: 1rem;
}

.selected-project-info h4 {
  color: var(--primary-color);
  margin-bottom: 0.5rem;
  font-family: 'Poppins', sans-serif;
}

.documents-required-list {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--clr-primary-a20);
}

.documents-required-list h5 {
  color: var(--primary-color);
  font-size: 0.9rem;
  font-weight: 600;
  margin: 0 0 0.75rem 0;
  font-family: 'Poppins', sans-serif;
}

.documents-required-list ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.documents-required-list li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0;
  font-size: 0.875rem;
  color: var(--text-color);
  border-bottom: 1px solid var(--clr-surface-tonal-a10);
}

.documents-required-list li:last-child {
  border-bottom: none;
}

.documents-required-list li i {
  color: var(--primary-color);
  font-size: 0.875rem;
  width: 16px;
  text-align: center;
}

/* Summary */
.summary-sections {
  display: flex;
  flex-direction: column;
  gap: var(--component-spacing);
}

.summary-section {
  background: var(--section-background);
  border-radius: var(--border-radius-lg);
  padding: var(--component-spacing);
  border: 1px solid var(--border-color);
}

.summary-section h3 {
  color: var(--primary-color);
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  font-family: 'Poppins', sans-serif;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 0.75rem;
}

.info-grid > div {
  font-size: 0.875rem;
  color: var(--text-color);
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
  color: var(--text-color);
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
  padding-top: var(--component-spacing);
  border-top: 1px solid var(--border-color);
}

.action-group {
  display: flex;
  gap: 0.75rem;
}

/* Récépissé */
.recepisse-content {
  text-align: center;
}

.recepisse-header {
  margin-bottom: var(--component-spacing);
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--primary-color);
}

.recepisse-header h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  font-family: 'Poppins', sans-serif;
}

.recepisse-body {
  text-align: left;
}

.recepisse-section {
  margin-bottom: var(--component-spacing);
}

.recepisse-section h4 {
  color: var(--primary-color);
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  font-family: 'Poppins', sans-serif;
}

.recepisse-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--clr-surface-tonal-a10);
  font-size: 0.875rem;
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

  .save-notification {
    left: 0.5rem;
    right: 0.5rem;
    width: auto;
  }
}

/* Additional responsive breakpoints */
@media (max-width: 480px) {
  .create-dossier-container {
    padding: 0.5rem;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>