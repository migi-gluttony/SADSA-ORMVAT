<template>
  <Dialog 
    :visible="visible" 
    modal 
    header="Finaliser la Visite d'Implémentation"
    :style="{ width: '90vw', maxWidth: '900px' }"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
    @hide="$emit('close')"
    class="complete-implementation-visit-dialog"
  >
    <!-- Visit Summary -->
    <div class="visit-summary mb-4">
      <h4 class="mb-3">
        <i class="pi pi-info-circle mr-2"></i>
        Résumé de la Visite d'Implémentation
      </h4>      
      <div class="grid">
        <div class="col-12 md:col-6">
          <strong>Dossier:</strong> {{ visit.dossierReference }}
        </div>
        <div class="col-12 md:col-6">
          <strong>Agriculteur:</strong> {{ visit.agriculteurPrenom }} {{ visit.agriculteurNom }}
        </div>
        <div class="col-12 md:col-6">
          <strong>Type de projet:</strong> {{ visit.sousRubriqueDesignation }}
        </div>
        <div class="col-12 md:col-6">
          <strong>Date de visite:</strong> {{ formatDate(visit.dateVisite) }}
        </div>
        <div class="col-12 md:col-6">
          <strong>Avancement actuel:</strong> {{ visit.pourcentageAvancement || 0 }}%
        </div>
        <div class="col-12">
          <strong>Localisation:</strong> {{ getLocationText() }}
        </div>
      </div>
    </div>

    <!-- Completion Form -->
    <div class="completion-form">
      <div class="form-section mb-4">
        <h5 class="mb-3">Finalisation de la Visite</h5>
        
        <div class="grid">
          <div class="col-12 md:col-6">
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
          </div>

          
        </div>

        <div class="form-group">
          <label for="observations">Observations finales *</label>
          <Textarea 
            id="observations"
            v-model="formData.observations" 
            rows="4" 
            placeholder="Décrivez vos observations détaillées sur l'état d'avancement du projet..."
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
            placeholder="Vos recommandations techniques pour la suite du projet..."
            class="w-full"
          />
          <small class="char-count">{{ formData.recommandations?.length || 0 }} / 500 caractères</small>
        </div>

        <div class="form-group">
          <label for="coordinates">Coordonnées GPS du projet</label>
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
          <small class="help-text">Coordonnées GPS exactes du site du projet</small>
        </div>
      </div>

      <!-- Progress Section -->
      <div class="progress-section mb-4">
        <h5 class="mb-3">
          <i class="pi pi-chart-bar mr-2"></i>
          Avancement du Projet
        </h5>
        
        <div class="form-group">
          <label for="pourcentageAvancement">Pourcentage d'avancement *</label>
          <div class="progress-input">
            <InputNumber 
              id="pourcentageAvancement"
              v-model="formData.pourcentageAvancement" 
              suffix="%" 
              :min="0" 
              :max="100"
              :class="{ 'p-invalid': errors.pourcentageAvancement }"
              class="flex-1"
            />
            <div class="progress-indicators">
              <Button 
                v-for="preset in progressPresets" 
                :key="preset.value"
                :label="preset.label" 
                @click="formData.pourcentageAvancement = preset.value"
                class="p-button-outlined p-button-sm"
                :class="{ 'p-button-info': formData.pourcentageAvancement === preset.value }"
              />
            </div>
          </div>
          <small v-if="errors.pourcentageAvancement" class="p-error">{{ errors.pourcentageAvancement }}</small>
          <ProgressBar :value="formData.pourcentageAvancement || 0" class="mt-2" />
        </div>

        <div class="form-group">
          <label for="conclusion">Conclusion sur l'avancement</label>
          <Textarea 
            id="conclusion"
            v-model="formData.conclusion" 
            rows="3" 
            placeholder="Conclusion générale sur l'état d'avancement du projet..."
            class="w-full"
          />
        </div>
      </div>

      <!-- Decision Section -->
      <div class="decision-section mb-4">
        <h5 class="mb-3">
          <i class="pi pi-check-circle mr-2"></i>
          Décision sur l'Implémentation
        </h5>
        
        <div class="grid">
          <div class="col-12 md:col-6">
            <div 
              class="decision-card p-3 border-round cursor-pointer"
              :class="{ 'bg-green-50 border-green-500': formData.approuve === true, 'border-300': formData.approuve !== true }"
              @click="selectDecision(true)"
            >
              <div class="flex align-items-center">
                <i class="pi pi-check-circle text-green-500 text-2xl mr-3"></i>
                <div>
                  <h6 class="m-0 mb-1">Approuver l'Implémentation</h6>
                  <p class="m-0 text-sm text-600">Le projet avance conformément aux spécifications</p>
                </div>
                <RadioButton 
                  v-model="formData.approuve" 
                  :value="true" 
                  inputId="approve"
                  class="ml-auto"
                />
              </div>
            </div>
          </div>
          
          <div class="col-12 md:col-6">
            <div 
              class="decision-card p-3 border-round cursor-pointer"
              :class="{ 'bg-red-50 border-red-500': formData.approuve === false, 'border-300': formData.approuve !== false }"
              @click="selectDecision(false)"
            >
              <div class="flex align-items-center">
                <i class="pi pi-times-circle text-red-500 text-2xl mr-3"></i>
                <div>
                  <h6 class="m-0 mb-1">Signaler des Problèmes</h6>
                  <p class="m-0 text-sm text-600">Des problèmes nécessitent une attention</p>
                </div>
                <RadioButton 
                  v-model="formData.approuve" 
                  :value="false" 
                  inputId="reject"
                  class="ml-auto"
                />
              </div>
            </div>
          </div>
        </div>

        <small v-if="errors.approuve" class="p-error">{{ errors.approuve }}</small>
      </div>

      <!-- Problem Details (shown only if problems detected) -->
      <div v-if="formData.approuve === false" class="problem-details mb-4">
        <h5 class="mb-3 text-red-500">
          <i class="pi pi-exclamation-triangle mr-2"></i>
          Détails des Problèmes
        </h5>
        
        <div class="form-group">
          <label for="problemesDetectes">Problèmes détectés *</label>
          <Textarea 
            id="problemesDetectes"
            v-model="formData.problemesDetectes" 
            rows="4" 
            placeholder="Décrivez en détail les problèmes observés..."
            :class="{ 'p-invalid': errors.problemesDetectes }"
            class="w-full"
          />
          <small v-if="errors.problemesDetectes" class="p-error">{{ errors.problemesDetectes }}</small>
        </div>

        <div class="form-group">
          <label for="actionsCorrectives">Actions correctives recommandées *</label>
          <Textarea 
            id="actionsCorrectives"
            v-model="formData.actionsCorrectives" 
            rows="3" 
            placeholder="Actions correctives à entreprendre pour résoudre les problèmes..."
            :class="{ 'p-invalid': errors.actionsCorrectives }"
            class="w-full"
          />
          <small v-if="errors.actionsCorrectives" class="p-error">{{ errors.actionsCorrectives }}</small>
          <small class="help-text">
            Ces recommandations seront transmises pour suivi et correction
          </small>
        </div>
      </div>

      <!-- Approval Confirmation (shown only if approved and 100% complete) -->
      <div v-if="formData.approuve === true && formData.pourcentageAvancement === 100" class="completion-confirmation mb-4">
        <h5 class="mb-3 text-green-500">
          <i class="pi pi-check mr-2"></i>
          Confirmation de Finalisation
        </h5>
        
        <div class="completion-checklist">
          <div class="checklist-item">
            <Checkbox 
              v-model="completionChecks.implementationComplete" 
              inputId="implementationComplete"
            />
            <label for="implementationComplete">
              L'implémentation du projet est entièrement terminée
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="completionChecks.qualityStandards" 
              inputId="qualityStandards"
            />
            <label for="qualityStandards">
              Les standards de qualité sont respectés
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="completionChecks.functionalityTested" 
              inputId="functionalityTested"
            />
            <label for="functionalityTested">
              La fonctionnalité du projet a été testée
            </label>
          </div>
          
          <div class="checklist-item">
            <Checkbox 
              v-model="completionChecks.readyForHandover" 
              inputId="readyForHandover"
            />
            <label for="readyForHandover">
              Le projet est prêt pour la remise finale
            </label>
          </div>
        </div>

        <Message severity="success" :closable="false" class="mt-3">
          <p>
            <strong>Projet terminé !</strong><br>
            En confirmant à 100%, le dossier sera automatiquement envoyé au GUC pour validation finale et archivage.
          </p>
        </Message>
      </div>

      <!-- Progress Continuation (shown if approved but not 100%) -->
      <div v-if="formData.approuve === true && formData.pourcentageAvancement < 100" class="progress-continuation mb-4">
        <Message severity="info" :closable="false">
          <p>
            <strong>Avancement confirmé</strong><br>
            Le projet continue avec {{ formData.pourcentageAvancement }}% de progression. Une nouvelle visite pourra être programmée pour le suivi.
          </p>
        </Message>
      </div>
    </div>

    <!-- Form Actions -->
    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="$emit('close')"
        class="p-button-outlined"
      />
      <Button 
        :label="getSubmitButtonLabel()" 
        :icon="formData.approuve === true ? 'pi pi-check' : 'pi pi-exclamation-triangle'" 
        @click="completeVisit"
        :class="formData.approuve === true ? 'p-button-success' : 'p-button-warning'"
        :loading="loading"
        :disabled="!isFormValid"
      />
    </template>

    <!-- Toast Messages -->
    <Toast />
  </Dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';

// PrimeVue Components
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import Calendar from 'primevue/calendar';
import Textarea from 'primevue/textarea';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import RadioButton from 'primevue/radiobutton';
import Checkbox from 'primevue/checkbox';
import ProgressBar from 'primevue/progressbar';
import Message from 'primevue/message';
import Toast from 'primevue/toast';

const props = defineProps({
  visit: {
    type: Object,
    required: true
  },
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['visit-completed', 'close', 'update:visible']);

const toast = useToast();

// State
const loading = ref(false);
const gettingLocation = ref(false);
const errors = ref({});

// Form data
const formData = reactive({
  dateConstat: new Date(),
  observations: props.visit.observations || '',
  recommandations: props.visit.recommandations || '',
  coordonneesGPS: props.visit.coordonneesGPS || '',
  approuve: null,
  conclusion: props.visit.conclusion || '',
  problemesDetectes: props.visit.problemesDetectes || '',
  actionsCorrectives: props.visit.actionsCorrectives || '',
  pourcentageAvancement: props.visit.pourcentageAvancement || 0,
  dureeVisite: props.visit.dureeVisite || null
});

// Completion checks for 100% projects
const completionChecks = reactive({
  implementationComplete: false,
  qualityStandards: false,
  functionalityTested: false,
  readyForHandover: false
});

// Progress presets
const progressPresets = ref([
  { label: '25%', value: 25 },
  { label: '50%', value: 50 },
  { label: '75%', value: 75 },
  { label: '100%', value: 100 }
]);

// Computed
const maxDate = computed(() => new Date());

const isFormValid = computed(() => {
  const basicValid = formData.dateConstat &&
         formData.observations?.trim() &&
         formData.approuve !== null &&
         formData.pourcentageAvancement >= 0 && formData.pourcentageAvancement <= 100;

  if (!basicValid) return false;

  // Additional validations for problems
  if (formData.approuve === false) {
    return formData.problemesDetectes?.trim() && formData.actionsCorrectives?.trim();
  }

  // Additional validations for 100% completion
  if (formData.approuve === true && formData.pourcentageAvancement === 100) {
    return Object.values(completionChecks).every(check => check === true);
  }

  return true;
});

// Methods
function selectDecision(decision) {
  formData.approuve = decision;
  // Reset related fields when changing decision
  if (decision) {
    formData.problemesDetectes = '';
    formData.actionsCorrectives = '';
  } else {
    // Reset completion checks
    Object.keys(completionChecks).forEach(key => {
      completionChecks[key] = false;
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
    errors.value.approuve = 'Vous devez prendre une décision';
  }

  if (formData.pourcentageAvancement < 0 || formData.pourcentageAvancement > 100) {
    errors.value.pourcentageAvancement = 'Le pourcentage doit être entre 0 et 100';
  }
  
  if (formData.approuve === false) {
    if (!formData.problemesDetectes?.trim()) {
      errors.value.problemesDetectes = 'Les problèmes détectés sont requis';
    }
    if (!formData.actionsCorrectives?.trim()) {
      errors.value.actionsCorrectives = 'Les actions correctives sont requises';
    }
  }
  
  return Object.keys(errors.value).length === 0;
}

function getSubmitButtonLabel() {
  if (formData.approuve === true && formData.pourcentageAvancement === 100) {
    return 'Finaliser le Projet';
  } else if (formData.approuve === true) {
    return 'Confirmer l\'Avancement';
  } else {
    return 'Signaler les Problèmes';
  }
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
      conclusion: formData.conclusion?.trim() || null,
      problemesDetectes: formData.approuve === false ? formData.problemesDetectes?.trim() : null,
      actionsCorrectives: formData.approuve === false ? formData.actionsCorrectives?.trim() : null,
      pourcentageAvancement: formData.pourcentageAvancement,
      dureeVisite: formData.dureeVisite
    };
    
    console.log('Sending complete request for implementation visit ID:', props.visit.id);
    console.log('Complete data:', completeData);
    
    const response = await ApiService.post(`/service-technique/visits/${props.visit.id}/complete`, completeData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite d\'implémentation finalisée avec succès',
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

<style scoped>
.visit-summary {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.decision-card {
  border: 2px solid #dee2e6;
  transition: all 0.2s;
}

.decision-card:hover {
  border-color: #adb5bd;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #495057;
}

.coordinates-input {
  display: flex;
  gap: 0.5rem;
}

.progress-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-indicators {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.completion-checklist {
  display: grid;
  gap: 0.75rem;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.char-count {
  text-align: right;
  font-size: 0.85rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.help-text {
  font-size: 0.85rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

@media (max-width: 768px) {
  .coordinates-input {
    flex-direction: column;
  }
  
  .progress-indicators {
    justify-content: center;
  }
}
</style>