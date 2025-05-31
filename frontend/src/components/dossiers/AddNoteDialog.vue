<template>
  <Dialog 
    v-model:visible="dialogVisible" 
    modal 
    header="Ajouter une Note"
    :style="{ width: '600px' }"
    @hide="resetForm"
  >
    <div class="note-form">
      <div class="dossier-info">
        <h4>Dossier: {{ dossier?.reference }}</h4>
        <p>{{ getAgriculteurName() }} - {{ dossier?.sousRubriqueDesignation }}</p>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="noteSubject">Objet de la note *</label>
          <InputText 
            id="noteSubject"
            v-model="form.objet" 
            placeholder="Objet de la note..."
            :class="{ 'p-invalid': errors.objet }"
            class="w-full"
          />
          <small v-if="errors.objet" class="p-error">{{ errors.objet }}</small>
        </div>

        <div class="form-group">
          <label for="noteType">Type de note</label>
          <Select 
            id="noteType"
            v-model="form.typeNote" 
            :options="noteTypes"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner le type"
            class="w-full"
          />
        </div>

        <div class="form-group">
          <label for="notePriority">Priorité</label>
          <Select 
            id="notePriority"
            v-model="form.priorite" 
            :options="priorityOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner la priorité"
            class="w-full"
          />
        </div>

        <div v-if="showDestinataireField" class="form-group">
          <label for="noteDestinataire">Destinataire</label>
          <Select 
            id="noteDestinataire"
            v-model="form.utilisateurDestinataireId" 
            :options="availableUsers"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner un destinataire"
            class="w-full"
            filter
          />
        </div>
      </div>

      <div class="form-group">
        <label for="noteContent">Contenu de la note *</label>
        <Textarea 
          id="noteContent"
          v-model="form.contenu" 
          rows="6" 
          placeholder="Décrivez votre note en détail..."
          :class="{ 'p-invalid': errors.contenu }"
          class="w-full"
        />
        <small v-if="errors.contenu" class="p-error">{{ errors.contenu }}</small>
        <small class="char-count">{{ form.contenu?.length || 0 }} / 1000 caractères</small>
      </div>

      <div class="form-group">
        <div class="checkbox-item">
          <Checkbox 
            id="requiresResponse"
            v-model="form.requiresResponse" 
            :binary="true"
          />
          <label for="requiresResponse">Cette note nécessite une réponse</label>
        </div>
      </div>

      <!-- Note template suggestions -->
      <div v-if="noteTemplates.length > 0" class="note-templates">
        <h5>Modèles de notes</h5>
        <div class="template-buttons">
          <Button 
            v-for="template in noteTemplates" 
            :key="template.id"
            :label="template.name"
            @click="applyTemplate(template)"
            class="p-button-outlined p-button-sm template-btn"
            size="small"
          />
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
        label="Ajouter la Note" 
        icon="pi pi-check" 
        @click="submitNote"
        class="p-button-success"
        :loading="loading"
        :disabled="!isFormValid"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Select from 'primevue/select';
import Checkbox from 'primevue/checkbox';

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

const emit = defineEmits(['update:visible', 'note-added']);

// State
const loading = ref(false);
const availableUsers = ref([]);
const errors = ref({});

const form = ref({
  objet: '',
  contenu: '',
  typeNote: 'INFORMATION',
  priorite: 'NORMALE',
  utilisateurDestinataireId: null,
  requiresResponse: false
});

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const currentUser = computed(() => AuthService.getCurrentUser());

const showDestinataireField = computed(() => {
  return form.value.typeNote === 'QUESTION' || 
         form.value.typeNote === 'DEMANDE' ||
         form.value.requiresResponse;
});

const isFormValid = computed(() => {
  return form.value.objet?.trim() && 
         form.value.contenu?.trim() && 
         form.value.contenu.length <= 1000;
});

// Options
const noteTypes = ref([
  { label: 'Information', value: 'INFORMATION' },
  { label: 'Question', value: 'QUESTION' },
  { label: 'Demande', value: 'DEMANDE' },
  { label: 'Observation', value: 'OBSERVATION' },
  { label: 'Recommandation', value: 'RECOMMANDATION' },
  { label: 'Problème', value: 'PROBLEME' },
  { label: 'Suivi', value: 'SUIVI' }
]);

const priorityOptions = ref([
  { label: 'Faible', value: 'FAIBLE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Haute', value: 'HAUTE' }
]);

const noteTemplates = ref([
  {
    id: 1,
    name: 'Documents manquants',
    objet: 'Documents manquants',
    contenu: 'Les documents suivants sont manquants ou incomplets :\n\n- \n- \n\nMerci de compléter le dossier.',
    typeNote: 'PROBLEME',
    priorite: 'NORMALE'
  },
  {
    id: 2,
    name: 'Validation OK',
    objet: 'Dossier validé',
    contenu: 'Le dossier a été vérifié et validé. Tous les documents sont conformes et complets.',
    typeNote: 'INFORMATION',
    priorite: 'NORMALE'
  },
  {
    id: 3,
    name: 'Demande de clarification',
    objet: 'Clarification nécessaire',
    contenu: 'Des clarifications sont nécessaires concernant :\n\n- \n\nMerci de fournir des précisions.',
    typeNote: 'QUESTION',
    priorite: 'NORMALE'
  }
]);

// Watchers
watch(() => props.visible, (newValue) => {
  if (newValue) {
    loadAvailableUsers();
  }
});

watch(() => form.value.typeNote, (newType) => {
  // Auto-adjust priority based on note type
  if (newType === 'PROBLEME') {
    form.value.priorite = 'HAUTE';
  } else if (newType === 'QUESTION' || newType === 'DEMANDE') {
    form.value.requiresResponse = true;
  }
});

// Methods
async function loadAvailableUsers() {
  try {
    // Load users based on current user role and dossier context
    const userRole = currentUser.value?.role;
    
    if (userRole === 'AGENT_GUC') {
      // GUC can send notes to antenne agents
      const response = await ApiService.get('/users/antenne-agents', {
        antenneId: props.dossier?.antenneId
      });
      availableUsers.value = response.map(user => ({
        label: `${user.prenom} ${user.nom} (${user.antenne?.designation})`,
        value: user.id
      }));
    } else if (userRole === 'AGENT_ANTENNE') {
      // Antenne can send notes to GUC agents
      const response = await ApiService.get('/users/guc-agents');
      availableUsers.value = response.map(user => ({
        label: `${user.prenom} ${user.nom} (GUC)`,
        value: user.id
      }));
    }
  } catch (error) {
    console.error('Erreur lors du chargement des utilisateurs:', error);
    availableUsers.value = [];
  }
}

function getAgriculteurName() {
  if (!props.dossier) return '';
  return `${props.dossier.agriculteurPrenom || ''} ${props.dossier.agriculteurNom || ''}`.trim();
}

function applyTemplate(template) {
  form.value.objet = template.objet;
  form.value.contenu = template.contenu;
  form.value.typeNote = template.typeNote;
  form.value.priorite = template.priorite;
}

function validateForm() {
  errors.value = {};
  
  if (!form.value.objet?.trim()) {
    errors.value.objet = 'L\'objet est requis';
  }
  
  if (!form.value.contenu?.trim()) {
    errors.value.contenu = 'Le contenu est requis';
  } else if (form.value.contenu.length > 1000) {
    errors.value.contenu = 'Le contenu ne doit pas dépasser 1000 caractères';
  }
  
  return Object.keys(errors.value).length === 0;
}

async function submitNote() {
  if (!validateForm()) return;
  
  try {
    loading.value = true;
    
    const noteData = {
      dossierId: props.dossier.id,
      objet: form.value.objet.trim(),
      contenu: form.value.contenu.trim(),
      typeNote: form.value.typeNote,
      priorite: form.value.priorite,
      utilisateurDestinataireId: form.value.utilisateurDestinataireId,
      requiresResponse: form.value.requiresResponse
    };
    
    await ApiService.post(`/dossiers/${props.dossier.id}/notes`, noteData);
    
    emit('note-added');
    closeDialog();
    
  } catch (error) {
    console.error('Erreur lors de l\'ajout de la note:', error);
    
    if (error.response?.data?.fieldErrors) {
      errors.value = error.response.data.fieldErrors;
    } else {
      // Show general error - this would typically be handled by a toast in the parent
    }
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = {
    objet: '',
    contenu: '',
    typeNote: 'INFORMATION',
    priorite: 'NORMALE',
    utilisateurDestinataireId: null,
    requiresResponse: false
  };
  errors.value = {};
}

function closeDialog() {
  emit('update:visible', false);
}
</script>

<style scoped>
.note-form {
  padding: 0;
}

.dossier-info {
  background: var(--section-background);
  padding: 1rem;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--card-border);
  margin-bottom: 1.5rem;
}

.dossier-info h4 {
  margin: 0 0 0.5rem 0;
  color: var(--primary-color);
  font-size: 1rem;
}

.dossier-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--text-color);
  font-size: 0.9rem;
}

.form-group .w-full {
  width: 100%;
}

.char-count {
  display: block;
  text-align: right;
  color: var(--text-muted);
  font-size: 0.75rem;
  margin-top: 0.25rem;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.checkbox-item label {
  margin: 0;
  font-weight: 400;
  font-size: 0.9rem;
  cursor: pointer;
}

.note-templates {
  background: var(--section-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-sm);
  padding: 1rem;
  margin-top: 1.5rem;
}

.note-templates h5 {
  margin: 0 0 0.75rem 0;
  color: var(--text-color);
  font-size: 0.9rem;
  font-weight: 600;
}

.template-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.template-btn {
  font-size: 0.8rem;
  padding: 0.25rem 0.75rem;
}

/* Validation styles */
:deep(.p-invalid) {
  border-color: var(--danger-color) !important;
}

:deep(.p-invalid:focus) {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2) !important;
}

.p-error {
  color: var(--danger-color);
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

/* Dialog styling */
:deep(.p-dialog-content) {
  padding: 1.5rem;
}

:deep(.p-dialog-footer) {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--card-border);
  background: var(--section-background);
}

:deep(.p-dialog-footer .p-button) {
  margin-left: 0.5rem;
}

:deep(.p-dialog-footer .p-button:first-child) {
  margin-left: 0;
}

/* Responsive design */
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  
  .template-buttons {
    flex-direction: column;
  }
  
  .template-btn {
    width: 100%;
    justify-content: center;
  }
}

/* Dark mode adjustments */
.dark-mode .dossier-info {
  background: var(--clr-surface-a20);
  border-color: var(--clr-surface-a30);
}

.dark-mode .note-templates {
  background: var(--clr-surface-a20);
  border-color: var(--clr-surface-a30);
}

.dark-mode .dossier-info h4,
.dark-mode .note-templates h5 {
  color: var(--text-color);
}

.dark-mode .dossier-info p {
  color: var(--text-secondary);
}

/* Focus states */
:deep(.p-inputtext:focus),
:deep(.p-textarea:focus),
:deep(.p-dropdown:focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(var(--primary-color-rgb), 0.2) !important;
}

/* Select dropdown styling */
:deep(.p-dropdown-panel) {
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--card-border);
}

:deep(.p-dropdown-item:hover) {
  background: var(--section-background);
}

:deep(.p-dropdown-item.p-highlight) {
  background: var(--primary-color);
  color: var(--text-on-primary);
}

/* Textarea resize */
:deep(.p-textarea) {
  resize: vertical;
  min-height: 120px;
}

/* Character limit warning */
.char-count.warning {
  color: var(--warning-color);
  font-weight: 500;
}

.char-count.error {
  color: var(--danger-color);
  font-weight: 600;
}
</style>