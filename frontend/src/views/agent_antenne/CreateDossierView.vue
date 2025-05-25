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
            <div class="sous-rubriques">
              <div 
                v-for="sousRubrique in rubrique.sousRubriques"
                :key="sousRubrique.id"
                class="project-type"
                :class="{ selected: selectedSousRubrique?.id === sousRubrique.id }"
                @click="selectSousRubrique(sousRubrique)"
              >
                <div class="project-icon">
                  <i :class="getProjectIcon(sousRubrique.codeType)"></i>
                </div>
                <div class="project-info">
                  <h4>{{ sousRubrique.designation }}</h4>
                  <p>{{ getProjectDescription(sousRubrique.codeType) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="step-actions">
          <Button 
            label="Continuer" 
            icon="pi pi-arrow-right" 
            @click="nextStep"
            :disabled="!selectedSousRubrique"
            class="btn-primary"
          />
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
            <label for="commune">Commune</label>
            <InputText 
              id="commune" 
              v-model="formData.agriculteur.commune" 
              placeholder="Commune"
            />
          </div>

          <div class="form-group">
            <label for="province">Province</label>
            <InputText 
              id="province" 
              v-model="formData.agriculteur.province" 
              placeholder="Province"
            />
          </div>
        </div>
      </div>

      <div class="component-card">
        <h2><i class="pi pi-folder"></i> Informations du dossier</h2>
        <div class="form-grid">
          <div class="form-group">
            <label for="saba" class="required">SABA</label>
            <InputText 
              id="saba" 
              v-model="formData.dossier.saba" 
              placeholder="000XXX/YYYY/ZZZ"
              @blur="checkSabaUniqueness"
              :class="{ 'p-invalid': validationErrors.saba }"
            />
            <small class="form-help">Format: 000XXX/YYYY/ZZZ</small>
            <small class="p-error">{{ validationErrors.saba }}</small>
          </div>

          <div class="form-group">
            <label for="reference">Référence</label>
            <InputText 
              id="reference" 
              v-model="formData.dossier.reference" 
              placeholder="Référence (optionnelle)"
            />
          </div>

          <div class="form-group">
            <label for="cda" class="required">CDA</label>
            <Dropdown 
              id="cda" 
              v-model="formData.dossier.cdaId" 
              :options="cdas"
              option-label="description"
              option-value="id"
              placeholder="Sélectionner un CDA"
              :class="{ 'p-invalid': validationErrors.cdaId }"
            />
            <small class="p-error">{{ validationErrors.cdaId }}</small>
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

    <!-- Étape 3: Formulaires spécifiques -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="component-card">
        <h2><i class="pi pi-file-edit"></i> Formulaires spécifiques</h2>
        <div v-if="selectedSousRubrique?.documentsRequis" class="dynamic-forms">
          <div 
            v-for="(document, index) in selectedSousRubrique.documentsRequis"
            :key="index"
            class="document-section"
          >
            <div class="document-header" @click="toggleDocument(index)">
              <h3>{{ document.nom }}</h3>
              <i class="pi" :class="expandedDocuments[index] ? 'pi-chevron-up' : 'pi-chevron-down'"></i>
            </div>
            
            <div v-if="expandedDocuments[index]" class="document-content">
              <!-- Upload de fichier si requis -->
              <div v-if="document.uploadRequired" class="file-upload-section">
                <h4>Document scanné</h4>
                <FileUpload 
                  mode="basic" 
                  name="document"
                  :url="`/api/upload/${index}`"
                  accept=".pdf,.jpg,.jpeg,.png"
                  :max-file-size="10000000"
                  @upload="onFileUpload($event, index)"
                  choose-label="Sélectionner le fichier"
                />
              </div>

              <!-- Formulaire dynamique si requis -->
              <div v-if="document.formRequired" class="dynamic-form">
                <h4>Informations à compléter</h4>
                <DynamicForm 
                  :config-path="document.formConfigPath"
                  :model-value="formData.formulairesDynamiques[document.nom] || {}"
                  @update:model-value="updateDynamicForm(document.nom, $event)"
                />
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
            label="Continuer" 
            icon="pi pi-arrow-right" 
            @click="nextStep"
            class="btn-primary"
          />
        </div>
      </div>
    </div>

    <!-- Étape 4: Aperçu et finalisation -->
    <div v-if="currentStep === 4" class="step-content">
      <div class="component-card">
        <h2><i class="pi pi-eye"></i> Aperçu du dossier</h2>
        
        <div v-if="dossierSummary" class="summary-sections">
          <div class="summary-section">
            <h3>Informations agriculteur</h3>
            <div class="info-grid">
              <div><strong>Nom complet:</strong> {{ dossierSummary.agriculteur.prenom }} {{ dossierSummary.agriculteur.nom }}</div>
              <div><strong>CIN:</strong> {{ dossierSummary.agriculteur.cin }}</div>
              <div><strong>Téléphone:</strong> {{ dossierSummary.agriculteur.telephone }}</div>
              <div v-if="dossierSummary.agriculteur.commune"><strong>Commune:</strong> {{ dossierSummary.agriculteur.commune }}</div>
            </div>
          </div>

          <div class="summary-section">
            <h3>Informations projet</h3>
            <div class="info-grid">
              <div><strong>Type:</strong> {{ dossierSummary.sousRubriqueNom }}</div>
              <div><strong>SABA:</strong> {{ dossierSummary.dossier.saba }}</div>
              <div><strong>Montant:</strong> {{ formatCurrency(dossierSummary.dossier.montantDemande) }}</div>
              <div><strong>CDA:</strong> {{ dossierSummary.cdaNom }}</div>
            </div>
          </div>

          <div class="summary-section" v-if="dossierSummary.formulairesRemplis?.length > 0">
            <h3>Formulaires complétés</h3>
            <div class="forms-summary">
              <div 
                v-for="formulaire in dossierSummary.formulairesRemplis"
                :key="formulaire.nomFormulaire"
                class="form-summary"
                :class="{ complete: formulaire.isComplete, incomplete: !formulaire.isComplete }"
              >
                <i class="pi" :class="formulaire.isComplete ? 'pi-check-circle' : 'pi-exclamation-triangle'"></i>
                <span>{{ formulaire.nomFormulaire }}</span>
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
              label="Sauvegarder brouillon" 
              icon="pi pi-save" 
              @click="saveDraft"
              class="p-button-secondary"
              :loading="loading.saveDraft"
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
    </div>

    <!-- Composant de récépissé -->
    <Dialog 
      v-model:visible="showRecepisse" 
      modal 
      header="Récépissé de dépôt"
      :style="{ width: '500px' }"
    >
      <div v-if="recepisse" class="recepisse-content">
        <div class="recepisse-header">
          <h3>RÉCÉPISSÉ N° {{ recepisse.numeroRecepisse }}</h3>
          <p>{{ formatDate(recepisse.dateDepot) }}</p>
        </div>
        
        <div class="recepisse-body">
          <div class="recepisse-row">
            <strong>Agriculteur:</strong> {{ recepisse.nomComplet }}
          </div>
          <div class="recepisse-row">
            <strong>CIN:</strong> {{ recepisse.cin }}
          </div>
          <div class="recepisse-row">
            <strong>Téléphone:</strong> {{ recepisse.telephone }}
          </div>
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
      
      <template #footer>
        <Button label="Imprimer" icon="pi pi-print" @click="printRecepisse" />
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
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import FileUpload from 'primevue/fileupload';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const toast = useToast();

// État des étapes
const currentStep = ref(1);
const steps = [
  { label: 'Type de projet' },
  { label: 'Informations de base' },
  { label: 'Formulaires spécifiques' },
  { label: 'Finalisation' }
];

// Données du formulaire
const formData = ref({
  agriculteur: {
    cin: '',
    nom: '',
    prenom: '',
    telephone: '',
    commune: '',
    province: ''
  },
  dossier: {
    saba: '',
    reference: '',
    cdaId: null,
    sousRubriqueId: null,
    dateDepot: new Date(),
    montantDemande: null
  },
  formulairesDynamiques: {}
});

// États de l'interface
const loading = ref({
  rubriques: false,
  cdas: false,
  create: false,
  saveDraft: false,
  validation: false
});

// Données
const rubriques = ref([]);
const cdas = ref([]);
const selectedSousRubrique = ref(null);
const expandedDocuments = ref({});
const validationErrors = ref({});
const dossierSummary = ref(null);
const showRecepisse = ref(false);
const recepisse = ref(null);

// Computed properties
const isBasicInfoValid = computed(() => {
  return formData.value.agriculteur.cin && 
         formData.value.agriculteur.nom && 
         formData.value.agriculteur.prenom && 
         formData.value.agriculteur.telephone &&
         formData.value.dossier.saba &&
         formData.value.dossier.cdaId &&
         formData.value.dossier.montantDemande;
});

// Méthodes du cycle de vie
onMounted(async () => {
  await loadInitialData();
});

// Méthodes
async function loadInitialData() {
  await Promise.all([
    loadRubriques(),
    loadCDAs()
  ]);
}

async function loadRubriques() {
  try {
    loading.value.rubriques = true;
    const response = await ApiService.get('/agent_antenne/dossiers/rubriques');
    rubriques.value = response.rubriques || [];
  } catch (error) {
    console.error('Erreur lors du chargement des rubriques:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les types de projets',
      life: 3000
    });
  } finally {
    loading.value.rubriques = false;
  }
}

async function loadCDAs() {
  try {
    loading.value.cdas = true;
    const response = await ApiService.get('/agent_antenne/dossiers/cdas');
    cdas.value = response || [];
  } catch (error) {
    console.error('Erreur lors du chargement des CDAs:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les CDAs',
      life: 3000
    });
  } finally {
    loading.value.cdas = false;
  }
}

function selectSousRubrique(sousRubrique) {
  selectedSousRubrique.value = sousRubrique;
  formData.value.dossier.sousRubriqueId = sousRubrique.id;
  
  // Réinitialiser les formulaires dynamiques
  formData.value.formulairesDynamiques = {};
  expandedDocuments.value = {};
}

function nextStep() {
  if (validateCurrentStep()) {
    if (currentStep.value === 3) {
      generatePreview();
    }
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
      
    case 3:
      return validateDynamicForms();
      
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
  
  if (!formData.value.dossier.saba) {
    errors.saba = 'SABA requis';
  } else if (!isValidSabaFormat(formData.value.dossier.saba)) {
    errors.saba = 'Format SABA invalide (000XXX/YYYY/ZZZ)';
  }
  
  if (!formData.value.dossier.cdaId) {
    errors.cdaId = 'CDA requis';
  }
  
  if (!formData.value.dossier.montantDemande) {
    errors.montantDemande = 'Montant requis';
  }
  
  validationErrors.value = errors;
  return Object.keys(errors).length === 0;
}

function validateDynamicForms() {
  // TODO: Implémenter la validation des formulaires dynamiques
  return true;
}

async function generatePreview() {
  try {
    const response = await ApiService.post('/agent_antenne/dossiers/preview', formData.value);
    dossierSummary.value = response;
  } catch (error) {
    console.error('Erreur lors de la génération de l\'aperçu:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de générer l\'aperçu',
      life: 3000
    });
  }
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

async function saveDraft() {
  try {
    loading.value.saveDraft = true;
    
    await ApiService.post('/agent_antenne/dossiers/save-draft', formData.value);
    
    toast.add({
      severity: 'info',
      summary: 'Sauvegardé',
      detail: 'Brouillon sauvegardé avec succès',
      life: 3000
    });
    
  } catch (error) {
    console.error('Erreur lors de la sauvegarde:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de sauvegarder le brouillon',
      life: 3000
    });
  } finally {
    loading.value.saveDraft = false;
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
    cdaNom: cdas.value.find(c => c.id === formData.value.dossier.cdaId)?.description || '',
    antenne: 'BENI AMIR'
  };
  
  recepisse.value = tempRecepisse;
  showRecepisse.value = true;
}

function printRecepisse() {
  window.print();
}

async function searchAgriculteur() {
  if (formData.value.agriculteur.cin.length >= 8) {
    try {
      // TODO: Implémenter la recherche d'agriculteur
      console.log('Recherche agriculteur:', formData.value.agriculteur.cin);
    } catch (error) {
      console.error('Erreur lors de la recherche:', error);
    }
  }
}

async function checkSabaUniqueness() {
  if (formData.value.dossier.saba && isValidSabaFormat(formData.value.dossier.saba)) {
    try {
      const response = await ApiService.get('/agent_antenne/dossiers/check-saba', {
        saba: formData.value.dossier.saba
      });
      
      if (!response.isUnique) {
        validationErrors.value.saba = 'Ce numéro SABA est déjà utilisé';
      } else {
        delete validationErrors.value.saba;
      }
    } catch (error) {
      console.error('Erreur lors de la vérification SABA:', error);
    }
  }
}

function toggleDocument(index) {
  expandedDocuments.value[index] = !expandedDocuments.value[index];
}

function updateDynamicForm(documentName, data) {
  formData.value.formulairesDynamiques[documentName] = data;
}

function onFileUpload(event, index) {
  console.log('Fichier uploadé:', event, index);
  toast.add({
    severity: 'success',
    summary: 'Fichier uploadé',
    detail: 'Document ajouté avec succès',
    life: 3000
  });
}

function handleSearch(query) {
  // TODO: Implémenter la recherche
  console.log('Recherche:', query);
}

function resetForm() {
  formData.value = {
    agriculteur: {
      cin: '',
      nom: '',
      prenom: '',
      telephone: '',
      commune: '',
      province: ''
    },
    dossier: {
      saba: '',
      reference: '',
      cdaId: null,
      sousRubriqueId: null,
      dateDepot: new Date(),
      montantDemande: null
    },
    formulairesDynamiques: {}
  };
  
  selectedSousRubrique.value = null;
  currentStep.value = 1;
  validationErrors.value = {};
  expandedDocuments.value = {};
}

// Fonctions utilitaires
function isValidSabaFormat(saba) {
  return /^\d{6}\/\d{4}\/\d{3}$/.test(saba);
}

function getProjectIcon(codeType) {
  const icons = {
    'acquisition-et-installation-des-serres': 'pi pi-home',
    'arboriculture-fruitiere': 'pi pi-apple',
    'equipement-des-exploitations-en-materiel-agricole': 'pi pi-cog',
    'filets-de-protection': 'pi pi-shield',
    'unites-de-valorisation': 'pi pi-building',
    'acquisition-du-materiel-d-elevage': 'pi pi-heart',
    'centres-de-collecte-de-lait': 'pi pi-database',
    'amenagement-hydro-agricole': 'pi pi-tint',
    'amelioration-fonciere': 'pi pi-map'
  };
  
  return icons[codeType] || 'pi pi-file';
}

function getProjectDescription(codeType) {
  const descriptions = {
    'acquisition-et-installation-des-serres': 'Installation de serres pour la production agricole',
    'arboriculture-fruitiere': 'Plantation et développement d\'arbres fruitiers',
    'equipement-des-exploitations-en-materiel-agricole': 'Acquisition de matériel et équipements',
    'filets-de-protection': 'Protection des cultures contre les intempéries',
    'unites-de-valorisation': 'Transformation et valorisation des produits',
    'acquisition-du-materiel-d-elevage': 'Matériel et équipements d\'élevage',
    'centres-de-collecte-de-lait': 'Infrastructure de collecte de lait',
    'amenagement-hydro-agricole': 'Aménagement des systèmes d\'irrigation',
    'amelioration-fonciere': 'Amélioration de la qualité des terres'
  };
  
  return descriptions[codeType] || 'Description du projet';
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

/* Stepper */
.stepper-container {
  margin-bottom: 2rem;
}

.stepper {
  display: flex;
  justify-content: center;
  align-items: center;
  background: white;
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
  border: 1px solid #e5e7eb;
}

.rubrique-card h3 {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-color);
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
  align-items: center;
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
}

.project-info h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.25rem;
}

.project-info p {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
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

/* Dynamic Forms */
.document-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 1rem;
  overflow: hidden;
}

.document-header {
  background: #f8f9fa;
  padding: 1rem;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
}

.document-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #374151;
}

.document-content {
  padding: 1.5rem;
}

.file-upload-section {
  margin-bottom: 1.5rem;
}

.file-upload-section h4,
.dynamic-form h4 {
  color: var(--primary-color);
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 1rem;
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

.forms-summary {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-summary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.form-summary.complete {
  color: #10b981;
}

.form-summary.incomplete {
  color: #f59e0b;
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
.dark-mode .document-section,
.dark-mode .summary-section,
.dark-mode .rubrique-card {
  background-color: #1f2937;
  border-color: #374151;
}

.dark-mode .document-header,
.dark-mode .summary-section {
  background-color: #111827;
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