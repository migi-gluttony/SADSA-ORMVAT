<template>
  <Dialog 
    v-model:visible="dialogVisible" 
    modal 
    header="Ajouter une note"
    :style="{ width: '600px' }"
    :closable="true"
  >
    <div class="note-form">
      <div class="form-field">
        <label for="objet" class="required">Objet</label>
        <InputText 
          id="objet"
          v-model="noteForm.objet"
          placeholder="Objet de la note"
          :class="{ 'p-invalid': errors.objet }"
          class="w-full"
        />
        <small v-if="errors.objet" class="p-error">{{ errors.objet }}</small>
      </div>

      <div class="form-field">
        <label for="typeNote">Type de note</label>
        <Dropdown 
          id="typeNote"
          v-model="noteForm.typeNote"
          :options="typeNoteOptions"
          option-label="label"
          option-value="value"
          placeholder="Sélectionner le type"
          class="w-full"
        />
      </div>

      <div class="form-field">
        <label for="priorite">Priorité</label>
        <Dropdown 
          id="priorite"
          v-model="noteForm.priorite"
          :options="prioriteOptions"
          option-label="label"
          option-value="value"
          placeholder="Sélectionner la priorité"
          class="w-full"
        />
      </div>

      <div class="form-field">
        <label for="contenu" class="required">Contenu</label>
        <Textarea 
          id="contenu"
          v-model="noteForm.contenu"
          rows="5"
          placeholder="Contenu de la note"
          :class="{ 'p-invalid': errors.contenu }"
          class="w-full"
          autoResize
        />
        <small v-if="errors.contenu" class="p-error">{{ errors.contenu }}</small>
      </div>

      <div v-if="canAssignUser" class="form-field">
        <label for="destinataire">Destinataire (optionnel)</label>
        <Dropdown 
          id="destinataire"
          v-model="noteForm.utilisateurDestinataireId"
          :options="availableUsers"
          option-label="fullName"
          option-value="id"
          placeholder="Assigner à un utilisateur"
          class="w-full"
          filter
          :loading="loadingUsers"
        />
        <small>Laissez vide pour une note générale</small>
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
        label="Ajouter la note" 
        icon="pi pi-check" 
        @click="submitNote"
        :loading="submitting"
        :disabled="!isFormValid"
        class="p-button-primary"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Dropdown from 'primevue/dropdown';

// Props & Emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  dossier: {
    type: Object,
    default: null
  },
  prefilledData: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['update:visible', 'note-added']);

const toast = useToast();

// State
const submitting = ref(false);
const loadingUsers = ref(false);
const availableUsers = ref([]);
const errors = ref({});

const noteForm = ref({
  objet: '',
  contenu: '',
  typeNote: 'OBSERVATION',
  priorite: 'NORMALE',
  utilisateurDestinataireId: null
});

// Options
const typeNoteOptions = [
  { label: 'Observation', value: 'OBSERVATION' },
  { label: 'Question', value: 'QUESTION' },
  { label: 'Demande', value: 'DEMANDE' },
  { label: 'Information', value: 'INFORMATION' },
  { label: 'Alerte', value: 'ALERTE' }
];

const prioriteOptions = [
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Faible', value: 'FAIBLE' }
];

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const currentUser = computed(() => AuthService.getCurrentUser());

const canAssignUser = computed(() => {
  return currentUser.value?.role === 'AGENT_GUC' || currentUser.value?.role === 'ADMIN';
});

const isFormValid = computed(() => {
  return noteForm.value.objet.trim() && noteForm.value.contenu.trim();
});

// Watchers
watch(() => props.visible, (newValue) => {
  if (newValue) {
    resetForm();
    if (props.prefilledData) {
      Object.assign(noteForm.value, props.prefilledData);
    }
    if (canAssignUser.value) {
      loadAvailableUsers();
    }
  }
});

// Methods
function resetForm() {
  noteForm.value = {
    objet: '',
    contenu: '',
    typeNote: 'OBSERVATION',
    priorite: 'NORMALE',
    utilisateurDestinataireId: null
  };
  errors.value = {};
}

async function loadAvailableUsers() {
  if (!canAssignUser.value) return;
  
  loadingUsers.value = true;
  try {
    const response = await ApiService.get('/users/active');
    availableUsers.value = response.map(user => ({
      id: user.id,
      fullName: `${user.prenom} ${user.nom} (${user.role})`,
      role: user.role
    }));
  } catch (error) {
    console.error('Erreur lors du chargement des utilisateurs:', error);
  } finally {
    loadingUsers.value = false;
  }
}

function validateForm() {
  errors.value = {};

  if (!noteForm.value.objet.trim()) {
    errors.value.objet = 'L\'objet est requis';
  }

  if (!noteForm.value.contenu.trim()) {
    errors.value.contenu = 'Le contenu est requis';
  }

  return Object.keys(errors.value).length === 0;
}

async function submitNote() {
  if (!validateForm()) {
    return;
  }

  if (!props.dossier) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Aucun dossier sélectionné',
      life: 3000
    });
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      dossierId: props.dossier.id,
      objet: noteForm.value.objet.trim(),
      contenu: noteForm.value.contenu.trim(),
      typeNote: noteForm.value.typeNote,
      priorite: noteForm.value.priorite,
      utilisateurDestinataireId: noteForm.value.utilisateurDestinataireId
    };

    const response = await ApiService.post(`/dossiers/${props.dossier.id}/notes`, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Note ajoutée avec succès',
        life: 3000
      });

      emit('note-added', response);
      closeDialog();
    }

  } catch (error) {
    console.error('Erreur lors de l\'ajout de la note:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Impossible d\'ajouter la note',
      life: 3000
    });
  } finally {
    submitting.value = false;
  }
}

function closeDialog() {
  emit('update:visible', false);
}
</script>
