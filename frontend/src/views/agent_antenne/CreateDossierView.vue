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
                    <i class="pi pi-search search-icon"></i>

        <!-- Project Type Search Bar -->
        <div class="search-section">

          <div class="search-container">
            
            <InputText 
              v-model="projectSearchTerm" 
              placeholder="Rechercher un type de projet..." 
              @input="searchProjectTypes"
              class="search-input"
            />
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
    </div>    <!-- Étape 2: Informations de base -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="component-card compact-form">
        <div class="title-section">
          <h2><i class="pi pi-user"></i> Informations de l'agriculteur</h2>
          <Button 
            label="Réinitialiser le formulaire" 
            icon="pi pi-refresh" 
            @click="resetForm"
            class="p-button-danger p-button-outlined"
          />
        </div>
        
        <!-- Compact form layout for easy screenshot -->
        <div class="compact-form-layout">
          <!-- Row 1: CIN with verification -->
          <div class="form-row">
            <div class="form-group cin-group">
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
            </div>
          </div>

          <!-- Farmer Found Information - Compact -->
          <div v-if="farmerCheckStatus.found && farmerCheckStatus.data" class="farmer-info-card compact">
            <div class="farmer-info-header">
              <i class="pi pi-user"></i>
              <strong>Agriculteur existant</strong>
            </div>
            <div class="farmer-details-compact">
              <span><strong>{{ farmerCheckStatus.data.nom }} {{ farmerCheckStatus.data.prenom }}</strong></span>
              <span>{{ farmerCheckStatus.data.telephone }}</span>
              <span v-if="farmerCheckStatus.previousDossiers.length > 0">
                {{ farmerCheckStatus.previousDossiers.length }} dossier(s)
              </span>
              <Button 
                label="Utiliser" 
                icon="pi pi-check" 
                @click="useFarmerData"
                class="p-button-sm p-button-success"
                size="small"
              />
            </div>
          </div>

          <!-- Row 2: Nom & Prénom -->
          <div class="form-row">
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
          </div>

          <!-- Row 3: Téléphone -->
          <div class="form-row">
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
          </div>

          <!-- Row 4: Province & Cercle -->
          <div class="form-row">
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
          </div>

          <!-- Row 5: Commune & Douar -->
          <div class="form-row">
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
      </div>      <div class="component-card compact-form">
        <h2><i class="pi pi-folder"></i> Informations du dossier</h2>
        
        <div class="compact-form-layout">
          <!-- Row 1: SABA & Antenne -->
          <div class="form-row">
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
          </div>

          <!-- Row 2: Date & Montant -->
          <div class="form-row">
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
          </div>

          <!-- Row 3: Selected Project Info -->
          <div class="form-row">
            <div class="selected-project-info-compact">
              <div class="project-header">
                <h4><i class="pi pi-folder"></i> Projet sélectionné:</h4>
                <span class="project-name">{{ selectedSousRubrique?.designation }}</span>
              </div>
              
              <!-- Documents requis in compact format -->
              <div v-if="selectedSousRubrique?.documentsRequis?.length > 0" class="documents-required-compact">
                <h5><i class="pi pi-file"></i> Documents requis ({{ selectedSousRubrique.documentsRequis.length }}):</h5>
                <div class="documents-tags">
                  <span 
                    v-for="document in selectedSousRubrique.documentsRequis" 
                    :key="document"
                    class="document-tag"
                  >
                    {{ document }}
                  </span>
                </div>
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
    </div>    <!-- Étape 3: Aperçu et finalisation -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="component-card preview-container">
        <div class="preview-header">
          <h2><i class="pi pi-eye"></i> Aperçu du dossier</h2>
          <div class="preview-status">
            <i class="pi pi-check-circle"></i>
            <span>Prêt à créer</span>
          </div>
        </div>
        
        <div class="preview-grid">
          <!-- Agriculteur Card -->
          <div class="preview-card farmer-card">
            <div class="card-header">
              <div class="card-icon farmer">
                <i class="pi pi-user"></i>
              </div>
              <h3>Agriculteur</h3>
            </div>
            <div class="card-content">
              <div class="info-row primary">
                <div class="info-item">
                  <span class="label">Nom complet</span>
                  <span class="value">{{ formData.agriculteur.prenom }} {{ formData.agriculteur.nom }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">CIN</span>
                  <span class="value">{{ formData.agriculteur.cin }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Téléphone</span>
                  <span class="value">{{ formData.agriculteur.telephone }}</span>
                </div>
              </div>
              <div class="info-row" v-if="selectedCommuneName || selectedDouarName">
                <div class="info-item" v-if="selectedCommuneName">
                  <span class="label">Commune</span>
                  <span class="value">{{ selectedCommuneName }}</span>
                </div>
                <div class="info-item" v-if="selectedDouarName">
                  <span class="label">Douar</span>
                  <span class="value">{{ selectedDouarName }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Projet Card -->
          <div class="preview-card project-card">
            <div class="card-header">
              <div class="card-icon project">
                <i class="pi pi-folder"></i>
              </div>
              <h3>Projet</h3>
            </div>
            <div class="card-content">
              <div class="info-row primary">
                <div class="info-item full-width">
                  <span class="label">Type de projet</span>
                  <span class="value project-type">{{ selectedSousRubrique?.designation }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">SABA</span>
                  <span class="value code">{{ formData.dossier.saba }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Antenne</span>
                  <span class="value">{{ getSelectedAntenneName() }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item amount">
                  <span class="label">Montant demandé</span>
                  <span class="value amount-value">{{ formatCurrency(formData.dossier.montantDemande) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Documents Card -->
          <div class="preview-card documents-card" v-if="selectedSousRubrique?.documentsRequis?.length > 0">
            <div class="card-header">
              <div class="card-icon documents">
                <i class="pi pi-file-edit"></i>
              </div>
              <h3>Documents requis</h3>
              <div class="docs-count">
                <span>{{ selectedSousRubrique.documentsRequis.length }}</span>
              </div>
            </div>
            <div class="card-content">
              <div class="documents-grid">
                <div 
                  v-for="(document, index) in selectedSousRubrique.documentsRequis"
                  :key="document"
                  class="document-badge"
                >
                  <div class="doc-number">{{ index + 1 }}</div>
                  <div class="doc-info">
                    <i class="pi pi-file"></i>
                    <span>{{ document }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Bar -->
        <div class="preview-actions">
          <div class="action-left">
            <Button 
              label="Retour" 
              icon="pi pi-arrow-left" 
              @click="previousStep"
              class="p-button-outlined action-btn"
            />
          </div>
          <div class="action-right">
            <Button 
              label="Créer le dossier" 
              icon="pi pi-check" 
              @click="createDossier"
              class="p-button-success btn-primary action-btn create-btn"
              :loading="loading.create"
            />
          </div>
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
    const response = await ApiService.get('/agent_antenne/dossier-creation/initialization-data');
    
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
    const response = await ApiService.get(`/agent_antenne/dossier-creation/cercles/${provinceId}`);
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
    const response = await ApiService.get(`/agent_antenne/dossier-creation/communes/${cercleId}`);
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
    const response = await ApiService.get(`/agent_antenne/dossier-creation/douars/${communeId}`);
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
    
    const response = await ApiService.post('/agent_antenne/dossier-creation/create', requestData);
    
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
    
    const response = await ApiService.get(`/agent_antenne/dossier-creation/check-farmer/${formData.value.agriculteur.cin.trim()}`);
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
      const response = await ApiService.get('/agent_antenne/dossier-creation/search-project-types', {
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
  padding: 1.5rem;
  background: #f8f9fa;
  min-height: 100vh;
}

/* Component Card Base */
.component-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
}

/* Stepper Styling */
.stepper-container {
  padding: 1rem 1.5rem;
  margin-bottom: 2rem;
}

.stepper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.stepper::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  height: 2px;
  background: #e5e7eb;
  z-index: 1;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
  flex: 1;
  max-width: 200px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 0.5rem;
  transition: all 0.3s ease;
}

.step.active .step-number {
  background: #3b82f6;
  color: white;
}

.step.completed .step-number {
  background: #10b981;
  color: white;
}

.step-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #6b7280;
  text-align: center;
  line-height: 1.2;
}

.step.active .step-label {
  color: #3b82f6;
  font-weight: 600;
}

.step.completed .step-label {
  color: #10b981;
}

/* Section Headers */
h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #374151;
  margin: 0 0 1rem 0;
}

/* Search Section */
.search-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.search-container {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-input {
  width: 100%;
  padding-left: 2.5rem;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #6b7280;
}

.clear-search-btn {
  min-width: auto;
  padding: 0.5rem;
}

/* Project Types Grid */
.project-types-grid {
  display: grid;
  gap: 1.5rem;
}

.rubrique-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.25rem;
  background: #f9fafb;
}

.rubrique-card h3 {
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.rubrique-description {
  color: #6b7280;
  font-size: 0.9rem;
  margin-bottom: 1rem;
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
  transition: all 0.2s ease;
  display: flex;
  gap: 0.75rem;
}

.project-type:hover {
  border-color: #3b82f6;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.project-type.selected {
  border-color: #3b82f6;
  background: #eff6ff;
  box-shadow: 0 0 0 1px #3b82f6;
}

.project-icon {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  flex-shrink: 0;
}

.project-type.selected .project-icon {
  background: #3b82f6;
  color: white;
}

.project-info h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 0.25rem 0;
}

.project-info p {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.documents-list {
  margin-top: 0.5rem;
}

.documents-list small {
  color: #374151;
  font-weight: 500;
}

.documents-list ul {
  margin: 0.25rem 0 0 0;
  padding-left: 1rem;
  list-style-type: disc;
}

.documents-list li {
  font-size: 0.8rem;
  color: #6b7280;
  margin-bottom: 0.125rem;
}

/* Search Results */
.search-results {
  margin-bottom: 1.5rem;
}

.search-results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 1rem;
}

.search-result {
  border: 2px solid #fbbf24;
  background: #fffbeb;
}

.search-result:hover {
  border-color: #f59e0b;
}

.search-result.selected {
  border-color: #f59e0b;
  background: #fef3c7;
}

/* Form Sections */
.form-section {
  margin-bottom: 2rem;
}

.form-section:last-child {
  margin-bottom: 0;
}

/* Compact Form Layout for Screenshots */
.compact-form {
  padding: 1.25rem !important;
}

.compact-form-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 0;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.form-group label.required::after {
  content: ' *';
  color: #ef4444;
}

/* Compact CIN Group */
.cin-group {
  grid-column: 1 / -1;
}

.cin-input-group {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.cin-input {
  flex: 1;
}

.check-btn {
  flex-shrink: 0;
  height: 42px;
}

/* Compact Farmer Info Card */
.farmer-info-card.compact {
  background: #f0f9ff;
  border: 1px solid #0ea5e9;
  border-radius: 6px;
  padding: 0.75rem;
  margin-top: 0.5rem;
  grid-column: 1 / -1;
}

.farmer-info-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  color: #0369a1;
}

.farmer-details-compact {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.farmer-details-compact span {
  font-size: 0.875rem;
  color: #374151;
}

/* Project Info Compact */
.selected-project-info-compact {
  grid-column: 1 / -1;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 1rem;
}

.project-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.project-header h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.project-name {
  font-weight: 600;
  color: #3b82f6;
}

.documents-required-compact h5 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.documents-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.document-tag {
  background: #3b82f6;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* Title Section */
.title-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

:deep(.p-inputtext),
:deep(.p-dropdown),
:deep(.p-calendar input) {
  border-radius: 6px;
  border: 1px solid #d1d5db;
  padding: 0.75rem;
  transition: border-color 0.2s ease;
}

:deep(.p-inputtext:focus),
:deep(.p-dropdown:focus),
:deep(.p-calendar input:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

:deep(.p-dropdown .p-dropdown-label) {
  padding: 0.75rem;
}

/* Preview Container */
.preview-container {
  padding: 1.5rem !important;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e5e7eb;
}

.preview-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 1.5rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.preview-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: #dcfce7;
  border: 1px solid #16a34a;
  border-radius: 6px;
  color: #16a34a;
  font-weight: 500;
  font-size: 0.9rem;
}

/* Preview Grid */
.preview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.preview-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.preview-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.documents-card {
  grid-column: 1 / -1;
}

/* Card Headers */
.card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  position: relative;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.1rem;
}

.card-icon.farmer {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.card-icon.project {
  background: linear-gradient(135deg, #059669, #047857);
}

.card-icon.documents {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
}

.card-header h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  flex: 1;
}

.docs-count {
  background: #3b82f6;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
}

/* Card Content */
.card-content {
  padding: 1.25rem;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row.primary {
  background: #f8fafc;
  margin: -1.25rem -1.25rem 1rem -1.25rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item.amount {
  grid-column: 1 / -1;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  padding: 0.75rem;
  text-align: center;
}

.label {
  font-size: 0.8rem;
  font-weight: 500;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.value {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
  word-break: break-word;
}

.value.project-type {
  color: #059669;
  font-size: 1rem;
}

.value.code {
  font-family: 'Courier New', monospace;
  background: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
}

.value.amount-value {
  font-size: 1.25rem;
  color: #f59e0b;
  font-weight: 700;
}

/* Documents Grid */
.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 0.75rem;
}

.document-badge {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.document-badge:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.doc-number {
  width: 24px;
  height: 24px;
  background: #3b82f6;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  flex-shrink: 0;
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
}

.doc-info i {
  color: #6b7280;
  flex-shrink: 0;
}

.doc-info span {
  font-size: 0.9rem;
  color: #374151;
  font-weight: 500;
  line-height: 1.3;
}

/* Preview Actions */
.preview-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1.5rem;
  border-top: 2px solid #e5e7eb;
}

.action-left,
.action-right {
  display: flex;
  gap: 1rem;
}

.action-btn {
  padding: 0.75rem 1.5rem !important;
  font-weight: 500 !important;
  border-radius: 8px !important;
  transition: all 0.2s ease !important;
}

.create-btn {
  background: linear-gradient(135deg, #16a34a, #15803d) !important;
  border-color: #16a34a !important;
  box-shadow: 0 2px 4px rgba(22, 163, 74, 0.2) !important;
}

.create-btn:hover {
  background: linear-gradient(135deg, #15803d, #166534) !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 8px rgba(22, 163, 74, 0.3) !important;
}

/* Summary Section (Legacy - keeping for compatibility) */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.summary-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.25rem;
}

.summary-card h4 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-label {
  font-weight: 500;
  color: #6b7280;
}

.summary-value {
  font-weight: 600;
  color: #1f2937;
}

/* Responsive Design for Preview */
@media (max-width: 768px) {
  .preview-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .preview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .info-row {
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }
  
  .documents-grid {
    grid-template-columns: 1fr;
  }
  
  .preview-actions {
    flex-direction: column;
    gap: 1rem;
  }
  
  .action-left,
  .action-right {
    width: 100%;
    justify-content: center;
  }
}

/* Navigation Buttons */
.step-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.nav-buttons {
  display: flex;
  gap: 1rem;
}

/* Loading and Error States */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: #6b7280;
}

.loading-text {
  margin-top: 1rem;
  font-size: 0.95rem;
}

.error-container {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

/* Validation Errors */
.error-message {
  color: #dc2626;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

:deep(.p-inputtext.p-invalid),
:deep(.p-dropdown.p-invalid) {
  border-color: #dc2626;
}

/* Toast Messages */
:deep(.p-toast) {
  z-index: 1050;
}

/* Responsive Design */
@media (max-width: 768px) {
  .create-dossier-container {
    padding: 1rem;
  }
  
  .component-card {
    padding: 1rem;
  }
  
  .stepper {
    flex-direction: column;
    gap: 1rem;
  }
  
  .stepper::before {
    display: none;
  }
  
  .step {
    flex-direction: row;
    justify-content: flex-start;
    width: 100%;
    max-width: none;
  }
  
  .step-number {
    margin-bottom: 0;
    margin-right: 0.75rem;
  }
  
  .step-label {
    text-align: left;
  }
  
  .sous-rubriques {
    grid-template-columns: 1fr;
  }
  
  .search-results-grid {
    grid-template-columns: 1fr;
  }
    .form-row {
    grid-template-columns: 1fr;
  }
  
  .compact-form-layout .form-row {
    grid-template-columns: 1fr;
  }
  
  .farmer-details-compact {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .title-section {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .documents-tags {
    flex-direction: column;
  }
  
  .summary-grid {
    grid-template-columns: 1fr;
  }
  
  .step-navigation {
    flex-direction: column;
    gap: 1rem;
  }
  
  .nav-buttons {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .project-type {
    flex-direction: column;
    text-align: center;
  }
  
  .project-icon {
    align-self: center;
  }
}
</style>

