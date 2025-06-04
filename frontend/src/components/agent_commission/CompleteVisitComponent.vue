<template>
  <div class="complete-visit-container">
    <!-- Visit Summary -->
    <div class="visit-summary">
      <div class="summary-header">
        <h3>
          <i class="pi pi-info-circle"></i>
          Résumé de la Visite
        </h3>
      </div>
      
      <div class="summary-content">
        <div class="summary-item">
          <span class="label">Dossier:</span>
          <span class="value">{{ visit.dossierReference }}</span>
        </div>
        <div class="summary-item">
          <span class="label">Agriculteur:</span>
          <span class="value">{{ visit.agriculteurPrenom }} {{ visit.agriculteurNom }}</span>
        </div>
        <div class="summary-item">
          <span class="label">Type de projet:</span>
          <span class="value">{{ visit.sousRubriqueDesignation }}</span>
        </div>
        <div class="summary-item">
          <span class="label">Date de visite:</span>
          <span class="value">{{ formatDate(visit.dateVisite) }}</span>
        </div>
        <div class="summary-item">
          <span class="label">Localisation:</span>
          <span class="value">{{ getLocationText() }}</span>
        </div>
      </div>
    </div>

    <!-- Completion Form -->
    <div class="completion-form">
      <div class="form-section">
        <h4>Finalisation de la Visite</h4>
        
        <div class="form-group">
          <label for="dateConstat">Date de constat *</label>
          <Calendar 
            id="dateConstat"
            v-model="formData.dateConstat" 
            dateFormat="dd/mm/yy"
            :maxDate="maxDate"
            :minDate="visit.dateVisite ? new Date(visit.dateVisite) : null"
            :class="{ 'p-invalid': errors.dateConstat }"
            showIcon
            class="w-full"
          />
          <small v-if="errors.dateConstat" class="p-error">{{ errors.dateConstat }}</small>
        </div>

        <div class="form-group">
          <label for="observations">Observations finales *</label>
          <Textarea 
            id="observations"
            v-model="formData.observations" 
            rows="4" 
            placeholder="Décrivez vos observations détaillées sur le terrain visité..."
            :class="{ 'p-invalid': errors.observations }"
            class="w-full"
          />
          <small v-if="errors.observations" class="p-error">{{ errors.observations }}</small>
          <small class="char-count">{{ formData.observations?.length || 0 }} / 1000 caractères</small>
        </div>

        <div class="form-group">
          <label for="recommandations">Recommandations techniques</label>
          <Textarea 
            id="recommandations"
            v-model="formData.recommandations" 
            rows="3" 
            placeholder="Vos recommandations techniques pour ce projet..."
            class="w-full"
          />
          <small class="char-count">{{ formData.recommandations?.length || 0 }} / 500 caractères</small>
        </div>

        <div class="form-group">
          <label for="coordinates">Coordonnées GPS du terrain</label>
          <div class="coordinates-input">
            <InputText 
              id="coordinates"
              v-model="formData.coordonneesGPS" 
              placeholder="Ex: 32.3578, -6.3491"
              class="flex-1"
            />
            <Button 
              icon="pi pi-map-marker" 
              @click="getCurrentLocation"
              class="p-button-outlined"
              v-tooltip="'Obtenir ma position'"
              :loading="gettingLocation"
            />
          </div>
          <small class="help-text">Coordonnées GPS exactes du terrain visité</small>
        </div>
      </div>

      <!-- Decision Section -->
      <div class="decision-section">
        <h4>
          <i class="pi pi-check-circle"></i>
          Décision sur le Terrain
        </h4>
        
        <div class="decision-cards">
          <div 
            class="decision-card approve-card"
            :class="{ 'selected': formData.approuve === true }"
            @click="selectDecision(true)"
          >
            <div class="card-icon approve">
              <i class="pi pi-check-circle"></i>
            </div>
            <div class="card-content">
              <h5>Approuver le Terrain</h5>
              <p>Le terrain est conforme aux exigences du projet</p>
            </div>
            <div class="card-radio">
              <RadioButton 
                v-model="formData.approuve" 
                :value="true" 
                inputId="approve"
              />
            </div>
          </div>

          <div 
            class="decision-card reject-card"
            :class="{ 'selected': formData.approuve === false }"
            @click="selectDecision(false)"
          >
            <div class="card-icon reject">
              <i class="pi pi-times-circle"></i>
            </div>
            <div class="card-content">
              <h5>Rejeter le Terrain</h5>
              <p>Le terrain ne répond pas aux critères requis</p>
            </div>
            <div class="card-radio">
              <RadioButton 
                v-model="formData.approuve" 
                :value="false" 
                inputId="reject"
              />
            </div>
          </div>
        </div>

        <small v-if="errors.approuve" class="p-error">{{ errors.approuve }}</small>
      </div>

      <!-- Rejection Details (shown only if rejected) -->
      <div v-if="formData.approuve === false" class="rejection-details">
        <h4>
          <i class="pi pi-exclamation-triangle"></i>
          Détails du Rejet
        </h4>
        
        <div class="form-group">
          <label for="motifRejet">Motif principal de rejet *</label>
          <Dropdown 
            id="motifRejet"
            v-model="formData.motifRejet" 
            :options="motifRejetOptions" 
            optionLabel="label" 
            optionValue="value"
            placeholder="Sélectionner le motif principal"
            :class="{ 'p-invalid': errors.motifRejet }"
            class="w-full"
          />
          <small v-if="errors.motifRejet" class="p-error">{{ errors.motifRejet }}</small>
        </div>

        <div class="form-group">
          <label>Points non conformes</label>
          <div class="non-conform-points">
            <div 
              v-for="point in availableNonConformPoints" 
              :key="point.value"
              class="point-checkbox"
            >
              <Checkbox 
                v-model="formData.pointsNonConformes" 
                :inputId="point.value"
                :value="point.value"
              />
              <label :for="point.value">{{ point.label }}</label>
            </div>
          </div>
          
          <!-- Custom non-conform point -->
          <div class="custom-point">
            <InputText 
              v-model="customNonConformPoint" 
              placeholder="Ajouter un point non conforme personnalisé..."
              @keyup.enter="addCustomNonConformPoint"
              class="flex-1"
            />
            <Button 
              icon="pi pi-plus" 
              @click="addCustomNonConformPoint"
              class="p-button-outlined p-button-sm"
              :disabled="!customNonConformPoint.trim()"
            />
          </div>
          
          <!-- Selected custom points -->
          <div v-if="customPoints.length > 0" class="custom-points-display">
            <Chip 
              v-for="(point, index) in customPoints" 
              :key="index"
              :label="point" 
              removable 
              @remove="removeCustomPoint(index)"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="remarquesRejet">Remarques détaillées sur le rejet</label>
          <Textarea 
            id="remarquesRejet"
            v-model="formData.remarquesGenerales" 
            rows="3" 
            placeholder="Expliquez en détail les raisons du rejet et les actions correctives possibles..."
            class="w-full"
          />
          <small class="help-text">
            Ces remarques seront transmises à l'agriculteur et à l'antenne
          </small>
        </div>
      </div>

      <!-- Approval Details (shown only if approved) -->
      <div v-if="formData.approuve === true" class="approval-details">
        <h4>
          <i class="pi pi-check"></i>
          Confirmation d'Approbation
        </h4>
        
        <div class="approval-checklist">
          <div class="checklist-item">
            <Checkbox 
              v-model="approvalChecks.terrainConforme" 
              inputId="terrainConforme"
            />
            <label for="terrainConforme">
              Le terrain est conforme aux spécifications du projet
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="approvalChecks.accessibilite" 
              inputId="accessibilite"
            />
            <label for="accessibilite">
              L'accessibilité au terrain est satisfaisante
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="approvalChecks.faisabilite" 
              inputId="faisabilite"
            />
            <label for="faisabilite">
              La faisabilité technique du projet est confirmée
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="approvalChecks.conformiteReglementaire" 
              inputId="conformiteReglementaire"
            />
            <label for="conformiteReglementaire">
              Le projet respecte les normes réglementaires
            </label>
          </div>
        </div>

        <div class="form-group">
          <label for="remarquesApprobation">Remarques sur l'approbation</label>
          <Textarea 
            id="remarquesApprobation"
            v-model="formData.remarquesGenerales" 
            rows="2" 
            placeholder="Remarques ou recommandations pour la suite du projet..."
            class="w-full"
          />
        </div>
      </div>
    </div>

    <!-- Form Actions -->
    <div class="form-actions">
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="$emit('close')"
        class="p-button-outlined"
      />
      <Button 
        :label="formData.approuve === true ? 'Approuver le Terrain' : 'Rejeter le Terrain'" 
        :icon="formData.approuve === true ? 'pi pi-check' : 'pi pi-times'" 
        @click="completeVisit"
        :class="formData.approuve === true ? 'p-button-success' : 'p-button-danger'"
        :loading="loading"
        :disabled="!isFormValid"
      />
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';

// PrimeVue Components
import Button from 'primevue/button';
import Calendar from 'primevue/calendar';
import Textarea from 'primevue/textarea';
import InputText from 'primevue/inputtext';
import RadioButton from 'primevue/radiobutton';
import Checkbox from 'primevue/checkbox';
import Dropdown from 'primevue/dropdown';
import Chip from 'primevue/chip';
import Toast from 'primevue/toast';

const props = defineProps({
  visit: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['visit-completed', 'close']);

const toast = useToast();

// State
const loading = ref(false);
const gettingLocation = ref(false);
const errors = ref({});
const customNonConformPoint = ref('');
const customPoints = ref([]);

// Form data
const formData = reactive({
  dateConstat: new Date(),
  observations: props.visit.observations || '',
  recommandations: props.visit.recommandations || '',
  coordonneesGPS: props.visit.coordonneesGPS || '',
  approuve: null,
  motifRejet: '',
  pointsNonConformes: [],
  remarquesGenerales: ''
});

// Approval checks
const approvalChecks = reactive({
  terrainConforme: false,
  accessibilite: false,
  faisabilite: false,
  conformiteReglementaire: false
});

// Options
const motifRejetOptions = ref([
  { label: 'Terrain non conforme aux spécifications', value: 'TERRAIN_NON_CONFORME' },
  { label: 'Accessibilité insuffisante', value: 'ACCESSIBILITE_INSUFFISANTE' },
  { label: 'Problèmes environnementaux', value: 'PROBLEMES_ENVIRONNEMENTAUX' },
  { label: 'Non-respect des normes de sécurité', value: 'NORMES_SECURITE' },
  { label: 'Faisabilité technique douteuse', value: 'FAISABILITE_TECHNIQUE' },
  { label: 'Conflit avec réglementations locales', value: 'CONFLIT_REGLEMENTAIRE' },
  { label: 'Autre motif', value: 'AUTRE' }
]);

const availableNonConformPoints = ref([
  { label: 'Dimensions du terrain inadéquates', value: 'DIMENSIONS_INADEQUATES' },
  { label: 'Qualité du sol problématique', value: 'QUALITE_SOL' },
  { label: 'Pente excessive', value: 'PENTE_EXCESSIVE' },
  { label: 'Drainage insuffisant', value: 'DRAINAGE_INSUFFISANT' },
  { label: 'Accès routier difficile', value: 'ACCES_ROUTIER' },
  { label: 'Proximité de sources de pollution', value: 'SOURCES_POLLUTION' },
  { label: 'Conflits de voisinage', value: 'CONFLITS_VOISINAGE' },
  { label: 'Infrastructure manquante', value: 'INFRASTRUCTURE_MANQUANTE' }
]);

// Computed
const maxDate = computed(() => new Date());

const isFormValid = computed(() => {
  return formData.dateConstat &&
         formData.observations?.trim() &&
         formData.approuve !== null &&
         (formData.approuve === true || 
          (formData.approuve === false && formData.motifRejet));
});

// Methods
function selectDecision(decision) {
  formData.approuve = decision;
  // Reset other fields when changing decision
  if (decision) {
    formData.motifRejet = '';
    formData.pointsNonConformes = [];
    customPoints.value = [];
  } else {
    // Reset approval checks
    Object.keys(approvalChecks).forEach(key => {
      approvalChecks[key] = false;
    });
  }
}

async function getCurrentLocation() {
  if (!navigator.geolocation) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'La géolocalisation n\'est pas supportée',
      life: 4000
    });
    return;
  }

  try {
    gettingLocation.value = true;
    
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000
      });
    });

    const lat = position.coords.latitude.toFixed(6);
    const lng = position.coords.longitude.toFixed(6);
    formData.coordonneesGPS = `${lat}, ${lng}`;
    
    toast.add({
      severity: 'success',
      summary: 'Position obtenue',
      detail: 'Coordonnées GPS mises à jour',
      life: 3000
    });
    
  } catch (error) {
    console.error('Erreur géolocalisation:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'obtenir la position',
      life: 4000
    });
  } finally {
    gettingLocation.value = false;
  }
}

function addCustomNonConformPoint() {
  const point = customNonConformPoint.value.trim();
  if (point && !customPoints.value.includes(point)) {
    customPoints.value.push(point);
    formData.pointsNonConformes.push(point);
    customNonConformPoint.value = '';
  }
}

function removeCustomPoint(index) {
  const point = customPoints.value[index];
  customPoints.value.splice(index, 1);
  const pointIndex = formData.pointsNonConformes.indexOf(point);
  if (pointIndex > -1) {
    formData.pointsNonConformes.splice(pointIndex, 1);
  }
}

function validateForm() {
  errors.value = {};
  
  if (!formData.dateConstat) {
    errors.value.dateConstat = 'La date de constat est requise';
  }
  
  if (!formData.observations?.trim()) {
    errors.value.observations = 'Les observations sont requises';
  } else if (formData.observations.length > 1000) {
    errors.value.observations = 'Les observations ne doivent pas dépasser 1000 caractères';
  }
  
  if (formData.approuve === null) {
    errors.value.approuve = 'Vous devez prendre une décision d\'approbation ou de rejet';
  }
  
  if (formData.approuve === false && !formData.motifRejet) {
    errors.value.motifRejet = 'Le motif de rejet est requis';
  }
  
  return Object.keys(errors.value).length === 0;
}

function formatDateForAPI(date) {
  if (!date) return null;
  return date.toISOString().split('T')[0];
}

async function completeVisit() {
  if (!validateForm()) return;
  
  // Validate that we have a valid visit ID
  if (!props.visit || !props.visit.id) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'ID de visite manquant - impossible de finaliser',
      life: 4000
    });
    return;
  }
  
  try {
    loading.value = true;
    
    // Prepare complete request data
    const completeData = {
      visiteId: props.visit.id,
      dateConstat: formatDateForAPI(formData.dateConstat),
      observations: formData.observations.trim(),
      recommandations: formData.recommandations?.trim() || null,
      coordonneesGPS: formData.coordonneesGPS?.trim() || null,
      approuve: formData.approuve,
      motifRejet: formData.approuve === false ? formData.motifRejet : null,
      pointsNonConformes: formData.approuve === false ? formData.pointsNonConformes : [],
      remarquesGenerales: formData.remarquesGenerales?.trim() || null
    };
    
    console.log('Sending complete request for visit ID:', props.visit.id);
    console.log('Complete data:', completeData);
    
    const response = await ApiService.post(`/agent_commission/terrain-visits/${props.visit.id}/complete`, completeData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite terrain finalisée avec succès',
        life: 3000
      });
      
      emit('visit-completed');
    } else {
      throw new Error(response.message || 'Erreur lors de la finalisation');
    }
    
  } catch (error) {
    console.error('Erreur lors de la finalisation:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de la finalisation de la visite',
      life: 4000
    });
  } finally {
    loading.value = false;
  }
}

// Utility functions
function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}

function getLocationText() {
  const parts = [];
  if (props.visit.agriculteurDouar) parts.push(props.visit.agriculteurDouar);
  if (props.visit.agriculteurCommune) parts.push(props.visit.agriculteurCommune);
  return parts.join(' - ') || 'Non spécifié';
}
</script>

