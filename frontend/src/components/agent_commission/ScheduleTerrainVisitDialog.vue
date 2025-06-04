<template>
  <Dialog 
    v-model:visible="dialogVisible" 
    modal 
    header="Programmer une Visite Terrain"
    :style="{ width: '600px' }"
    @hide="resetForm"
  >
    <div class="schedule-visit-form">
      <div v-if="dossier" class="dossier-info">
        <h4>Dossier: {{ dossier?.reference || dossier?.numeroDossier }}</h4>
        <p>{{ getAgriculteurName() }} - {{ dossier?.sousRubriqueDesignation }}</p>
        <p class="location-info">
          <i class="pi pi-map-marker"></i>
          {{ getLocationInfo() }}
        </p>
      </div>

      <div v-else class="no-dossier-info">
        <Message severity="info" :closable="false">
          Sélectionnez d'abord un dossier depuis la liste des dossiers disponibles
        </Message>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="visitDate">Date de visite *</label>
          <Calendar 
            id="visitDate"
            v-model="form.dateVisite" 
            dateFormat="dd/mm/yy"
            :minDate="minDate"
            :maxDate="maxDate"
            :class="{ 'p-invalid': errors.dateVisite }"
            class="w-full"
            showIcon
            placeholder="Sélectionner une date"
          />
          <small v-if="errors.dateVisite" class="p-error">{{ errors.dateVisite }}</small>
          <small class="help-text">La visite doit être programmée dans les 15 jours ouvrables</small>
        </div>

        <div class="form-group">
          <label for="coordinates">Coordonnées GPS (optionnel)</label>
          <div class="coordinates-input">
            <InputText 
              id="coordinates"
              v-model="form.coordonneesGPS" 
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
          <small class="help-text">Format: latitude, longitude</small>
        </div>
      </div>

      <div class="form-group">
        <label for="observations">Observations initiales *</label>
        <Textarea 
          id="observations"
          v-model="form.observations" 
          rows="4" 
          placeholder="Observations préliminaires, points à vérifier sur le terrain..."
          :class="{ 'p-invalid': errors.observations }"
          class="w-full"
        />
        <small v-if="errors.observations" class="p-error">{{ errors.observations }}</small>
        <small class="char-count">{{ form.observations?.length || 0 }} / 500 caractères</small>
      </div>

      <div class="form-group">
        <label for="recommendations">Recommandations préliminaires</label>
        <Textarea 
          id="recommendations"
          v-model="form.recommandations" 
          rows="3" 
          placeholder="Recommandations pour la visite terrain..."
          class="w-full"
        />
        <small class="char-count">{{ form.recommandations?.length || 0 }} / 300 caractères</small>
      </div>

      <!-- Planning Information -->
      <div class="planning-info">
        <h5>Informations de planification</h5>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">Délai réglementaire:</span>
            <span class="value">15 jours ouvrables</span>
          </div>
          <div class="info-item">
            <span class="label">Date limite:</span>
            <span class="value warning">{{ formatDate(deadlineDate) }}</span>
          </div>
          <div class="info-item">
            <span class="label">Jours restants:</span>
            <span :class="getDaysRemainingClass()">{{ daysRemaining }} jours</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog"
        class="p-button-outlined"
      />
      <Button 
        label="Programmer la visite" 
        icon="pi pi-calendar-plus" 
        @click="submitSchedule"
        class="p-button-success"
        :loading="loading"
        :disabled="!isFormValid"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Calendar from 'primevue/calendar';
import Message from 'primevue/message';

const toast = useToast();

// Props & Emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  dossier: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:visible', 'visit-scheduled']);

// State
const loading = ref(false);
const gettingLocation = ref(false);
const errors = ref({});

const form = ref({
  dateVisite: null,
  observations: '',
  coordonneesGPS: '',
  recommandations: ''
});

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const minDate = computed(() => {
  const today = new Date();
  return today;
});

const maxDate = computed(() => {
  const today = new Date();
  const deadline = new Date(today);
  // Add 15 working days (approximately 21 calendar days)
  deadline.setDate(today.getDate() + 21);
  return deadline;
});

const deadlineDate = computed(() => {
  const today = new Date();
  const deadline = new Date(today);
  deadline.setDate(today.getDate() + 15); // 15 working days simplified
  return deadline;
});

const daysRemaining = computed(() => {
  const today = new Date();
  const deadline = deadlineDate.value;
  const diffTime = deadline - today;
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return Math.max(0, diffDays);
});

const isFormValid = computed(() => {
  return props.dossier && 
         form.value.dateVisite && 
         form.value.observations?.trim() && 
         form.value.observations.length <= 500;
});

// Methods
function getAgriculteurName() {
  if (!props.dossier) return '';
  return `${props.dossier.agriculteurPrenom || ''} ${props.dossier.agriculteurNom || ''}`.trim();
}

function getLocationInfo() {
  if (!props.dossier) return '';
  const parts = [];
  if (props.dossier.agriculteurCommune) parts.push(props.dossier.agriculteurCommune);
  if (props.dossier.agriculteurDouar) parts.push(props.dossier.agriculteurDouar);
  if (props.dossier.antenneDesignation) parts.push(props.dossier.antenneDesignation);
  return parts.join(' - ');
}

function getDaysRemainingClass() {
  const days = daysRemaining.value;
  if (days <= 3) return 'value danger';
  if (days <= 7) return 'value warning';
  return 'value success';
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(date);
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
    form.value.coordonneesGPS = `${lat}, ${lng}`;
    
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
  
  if (!props.dossier) {
    errors.value.dossier = 'Aucun dossier sélectionné';
    return false;
  }
  
  if (!form.value.dateVisite) {
    errors.value.dateVisite = 'La date de visite est requise';
  } else {
    const selectedDate = new Date(form.value.dateVisite);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (selectedDate < today) {
      errors.value.dateVisite = 'La date de visite ne peut pas être dans le passé';
    } else if (selectedDate > maxDate.value) {
      errors.value.dateVisite = 'La date de visite dépasse le délai réglementaire';
    }
  }
  
  if (!form.value.observations?.trim()) {
    errors.value.observations = 'Les observations sont requises';
  } else if (form.value.observations.length > 500) {
    errors.value.observations = 'Les observations ne doivent pas dépasser 500 caractères';
  }
  
  if (form.value.recommandations && form.value.recommandations.length > 300) {
    errors.value.recommandations = 'Les recommandations ne doivent pas dépasser 300 caractères';
  }
  
  return Object.keys(errors.value).length === 0;
}

async function submitSchedule() {
  if (!validateForm()) {
    if (!props.dossier) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez sélectionner un dossier',
        life: 4000
      });
    }
    return;
  }
  
  try {
    loading.value = true;
    
    const scheduleData = {
      dossierId: props.dossier.id,
      dateVisite: formatDateForAPI(form.value.dateVisite),
      observations: form.value.observations.trim(),
      coordonneesGPS: form.value.coordonneesGPS?.trim() || null,
      recommandations: form.value.recommandations?.trim() || null
    };
    
    const response = await ApiService.post('/agent_commission/terrain-visits/schedule', scheduleData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Visite terrain programmée avec succès',
        life: 3000
      });
      
      emit('visit-scheduled');
      closeDialog();
    } else {
      throw new Error(response.message || 'Erreur lors de la programmation');
    }
    
  } catch (error) {
    console.error('Erreur lors de la programmation de la visite:', error);
    
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.response?.data?.message || error.message || 'Erreur lors de la programmation de la visite',
      life: 4000
    });
  } finally {
    loading.value = false;
  }
}

function formatDateForAPI(date) {
  if (!date) return null;
  // Convert to ISO date string (YYYY-MM-DD)
  return date.toISOString().split('T')[0];
}

function resetForm() {
  form.value = {
    dateVisite: null,
    observations: '',
    coordonneesGPS: '',
    recommandations: ''
  };
  errors.value = {};
}

function closeDialog() {
  emit('update:visible', false);
}

// Watch for dossier changes to reset form
watch(() => props.dossier, () => {
  if (props.dossier) {
    resetForm();
  }
});
</script>

