<template>
  <Dialog 
    v-model:visible="localVisible" 
    header="Ajouter une Note"
    modal 
    class="add-note-dialog"
    :style="{ width: '600px' }"
    @hide="resetForm"
  >
    <div class="dialog-content">
      <!-- Dossier Information -->
      <div class="dossier-info">
        <h4><i class="pi pi-folder"></i> Dossier concerné</h4>
        <div class="info-grid">
          <div><strong>Référence:</strong> {{ dossier?.reference }}</div>
          <div><strong>Agriculteur:</strong> {{ dossier?.agriculteurNom }}</div>
          <div><strong>Statut:</strong> {{ dossier?.statut }}</div>
        </div>
      </div>

      <!-- Note Form -->
      <div class="note-form">
        <div class="form-grid">
          <!-- Subject -->
          <div class="field-group">
            <label for="objet">Objet de la note *</label>
            <InputText 
              id="objet"
              v-model="formData.objet" 
              placeholder="Ex: Demande de complément d'information"
              :class="{ 'p-invalid': errors.objet }"
            />
            <small v-if="errors.objet" class="error-message">{{ errors.objet }}</small>
          </div>

          <!-- Priority -->
          <div class="field-group">
            <label for="priorite">Priorité</label>
            <Select 
              id="priorite"
              v-model="formData.priorite" 
              :options="prioriteOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner la priorité"
            />
          </div>

          <!-- Type -->
          <div class="field-group">
            <label for="typeNote">Type de note</label>
            <Select 
              id="typeNote"
              v-model="formData.typeNote" 
              :options="typeNoteOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le type"
            />
          </div>

          <!-- Visibility -->
          <div class="field-group">
            <label for="visibilite">Visibilité</label>
            <Select 
              id="visibilite"
              v-model="formData.visibilite" 
              :options="visibiliteOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Qui peut voir cette note"
            />
          </div>
        </div>

        <!-- Content -->
        <div class="field-group">
          <label for="contenu">Contenu de la note *</label>
          <Textarea 
            id="contenu"
            v-model="formData.contenu" 
            rows="4"
            placeholder="Décrivez votre note, observation ou demande..."
            :class="{ 'p-invalid': errors.contenu }"
          />
          <small v-if="errors.contenu" class="error-message">{{ errors.contenu }}</small>
        </div>

        <!-- Recipients (for notifications) -->
        <div v-if="recipientOptions.length > 0" class="field-group">
          <label for="destinataires">Notifier les personnes suivantes</label>
          <MultiSelect 
            id="destinataires"
            v-model="formData.destinataires" 
            :options="recipientOptions"
            optionLabel="nom"
            optionValue="id"
            placeholder="Sélectionner les destinataires"
            :maxSelectedLabels="3"
            selectedItemsLabel="{0} personnes sélectionnées"
          />
          <small class="field-help">Ces personnes recevront une notification par email</small>
        </div>

        <!-- Tags -->
        <div class="field-group">
          <label for="tags">Mots-clés (optionnel)</label>
          <Chips 
            id="tags"
            v-model="formData.tags" 
            placeholder="Ajouter des mots-clés pour faciliter la recherche"
            :max="5"
          />
          <small class="field-help">Appuyez sur Entrée pour ajouter un mot-clé</small>
        </div>

        <!-- Attachments -->
        <div class="field-group">
          <label for="attachments">Pièces jointes (optionnel)</label>
          <FileUpload 
            id="attachments"
            mode="advanced"
            :multiple="true"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
            :maxFileSize="5000000"
            :fileLimit="3"
            @select="onFilesSelected"
            @remove="onFileRemoved"
            :customUpload="true"
            :auto="false"
            chooseLabel="Choisir des fichiers"
            uploadLabel="Joindre"
            cancelLabel="Annuler"
          >
            <template #empty>
              <div class="upload-empty">
                <i class="pi pi-file"></i>
                <p>Glissez-déposez des fichiers ici ou cliquez pour sélectionner.</p>
                <p class="upload-help">Maximum 3 fichiers, 5MB par fichier</p>
              </div>
            </template>
          </FileUpload>
        </div>

        <!-- Follow-up Options -->
        <div class="field-group">
          <div class="checkbox-options">
            <div class="checkbox-item">
              <Checkbox 
                id="demandeReponse" 
                v-model="formData.demandeReponse" 
                binary 
              />
              <label for="demandeReponse">Demander une réponse</label>
            </div>
            
            <div class="checkbox-item">
              <Checkbox 
                id="rappelAutomatique" 
                v-model="formData.rappelAutomatique" 
                binary 
              />
              <label for="rappelAutomatique">Rappel automatique si pas de réponse sous 3 jours</label>
            </div>
          </div>
        </div>

        <!-- Date limite (if response requested) -->
        <div v-if="formData.demandeReponse" class="field-group">
          <label for="dateLimiteReponse">Date limite pour la réponse</label>
          <Calendar 
            id="dateLimiteReponse"
            v-model="formData.dateLimiteReponse" 
            :minDate="minDate"
            dateFormat="dd/mm/yy"
            placeholder="Sélectionner une date limite"
            :showIcon="true"
          />
        </div>
      </div>

      <!-- Preview -->
      <div v-if="showPreview" class="note-preview">
        <h4><i class="pi pi-eye"></i> Aperçu de la note</h4>
        <div class="preview-content">
          <div class="preview-header">
            <div class="preview-subject">{{ formData.objet || 'Objet non défini' }}</div>
            <Tag 
              :value="formData.priorite || 'NORMALE'" 
              :severity="getPrioritySeverity(formData.priorite)"
              class="priority-tag"
            />
          </div>
          <div class="preview-meta">
            <span><strong>Type:</strong> {{ getTypeLabel(formData.typeNote) }}</span>
            <span><strong>Visibilité:</strong> {{ getVisibilityLabel(formData.visibilite) }}</span>
          </div>
          <div class="preview-body">
            {{ formData.contenu || 'Contenu non défini' }}
          </div>
          <div v-if="formData.tags?.length > 0" class="preview-tags">
            <strong>Mots-clés:</strong>
            <Tag 
              v-for="tag in formData.tags" 
              :key="tag"
              :value="tag"
              severity="info"
              class="tag-item"
            />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="localVisible = false"
          class="p-button-text"
        />
        <Button 
          :label="showPreview ? 'Modifier' : 'Aperçu'" 
          :icon="showPreview ? 'pi pi-pencil' : 'pi pi-eye'" 
          @click="togglePreview"
          class="p-button-outlined"
        />
        <Button 
          label="Ajouter la note" 
          icon="pi pi-plus" 
          @click="addNote"
          :loading="loading"
          class="p-button-success"
          :disabled="!canSubmit"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Select from 'primevue/select';
import MultiSelect from 'primevue/multiselect';
import Calendar from 'primevue/calendar';
import Checkbox from 'primevue/checkbox';
import Chips from 'primevue/chips';
import FileUpload from 'primevue/fileupload';
import Tag from 'primevue/tag';

// Props
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

// Emits
const emit = defineEmits(['update:visible', 'note-added']);

const toast = useToast();

// State
const loading = ref(false);
const showPreview = ref(false);
const selectedFiles = ref([]);
const recipientOptions = ref([]);

// Form data
const formData = reactive({
  objet: '',
  contenu: '',
  priorite: 'NORMALE',
  typeNote: 'INFORMATION',
  visibilite: 'EQUIPE',
  destinataires: [],
  tags: [],
  demandeReponse: false,
  rappelAutomatique: false,
  dateLimiteReponse: null
});

// Validation errors
const errors = reactive({
  objet: '',
  contenu: ''
});

// Options
const prioriteOptions = ref([
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

const typeNoteOptions = ref([
  { label: 'Information', value: 'INFORMATION' },
  { label: 'Question', value: 'QUESTION' },
  { label: 'Demande de complément', value: 'DEMANDE_COMPLEMENT' },
  { label: 'Observation', value: 'OBSERVATION' },
  { label: 'Recommandation', value: 'RECOMMANDATION' },
  { label: 'Alerte', value: 'ALERTE' }
]);

const visibiliteOptions = ref([
  { label: 'Équipe uniquement', value: 'EQUIPE' },
  { label: 'Service complet', value: 'SERVICE' },
  { label: 'Tous les intervenants', value: 'TOUS' },
  { label: 'Privée', value: 'PRIVE' }
]);

// Computed
const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const minDate = computed(() => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow;
});

const canSubmit = computed(() => {
  return formData.objet.trim() && formData.contenu.trim();
});

// Watch for dialog opening
watch(() => props.visible, (newVisible) => {
  if (newVisible && props.dossier) {
    loadRecipients();
    initializeForm();
  }
});

// Methods
async function loadRecipients() {
  try {
    const response = await ApiService.get(`/dossiers/${props.dossier.id}/participants`);
    recipientOptions.value = response.filter(user => 
      user.id !== AuthService.getCurrentUser()?.id
    );
  } catch (err) {
    console.error('Error loading recipients:', err);
    recipientOptions.value = [];
  }
}

function initializeForm() {
  // Reset form
  Object.assign(formData, {
    objet: '',
    contenu: '',
    priorite: 'NORMALE',
    typeNote: 'INFORMATION',
    visibilite: 'EQUIPE',
    destinataires: [],
    tags: [],
    demandeReponse: false,
    rappelAutomatique: false,
    dateLimiteReponse: null
  });

  // Clear errors
  Object.keys(errors).forEach(key => {
    errors[key] = '';
  });

  selectedFiles.value = [];
  showPreview.value = false;
}

function validateForm() {
  let isValid = true;
  
  // Clear previous errors
  Object.keys(errors).forEach(key => {
    errors[key] = '';
  });

  // Required fields validation
  if (!formData.objet.trim()) {
    errors.objet = 'L\'objet est requis';
    isValid = false;
  }

  if (!formData.contenu.trim()) {
    errors.contenu = 'Le contenu est requis';
    isValid = false;
  }

  return isValid;
}

function togglePreview() {
  if (!showPreview.value && !validateForm()) {
    return;
  }
  showPreview.value = !showPreview.value;
}

function onFilesSelected(event) {
  selectedFiles.value = [...event.files];
}

function onFileRemoved(event) {
  const index = selectedFiles.value.findIndex(file => file.name === event.file.name);
  if (index > -1) {
    selectedFiles.value.splice(index, 1);
  }
}

async function addNote() {
  if (!validateForm()) {
    return;
  }

  try {
    loading.value = true;

    const payload = {
      dossierId: props.dossier.id,
      objet: formData.objet.trim(),
      contenu: formData.contenu.trim(),
      priorite: formData.priorite,
      typeNote: formData.typeNote,
      visibilite: formData.visibilite,
      destinataires: formData.destinataires,
      tags: formData.tags,
      demandeReponse: formData.demandeReponse,
      rappelAutomatique: formData.rappelAutomatique,
      dateLimiteReponse: formData.dateLimiteReponse?.toISOString() || null,
      attachments: selectedFiles.value
    };

    const response = await ApiService.post('/notes', payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Note ajoutée avec succès',
        life: 4000
      });

      emit('note-added', response.data);
      localVisible.value = false;
    }

  } catch (err) {
    console.error('Error adding note:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Impossible d\'ajouter la note',
      life: 4000
    });
  } finally {
    loading.value = false;
  }
}

function getPrioritySeverity(priority) {
  const severityMap = {
    'HAUTE': 'danger',
    'NORMALE': 'info',
    'FAIBLE': 'secondary'
  };
  return severityMap[priority] || 'info';
}

function getTypeLabel(type) {
  const option = typeNoteOptions.value.find(opt => opt.value === type);
  return option ? option.label : type;
}

function getVisibilityLabel(visibility) {
  const option = visibiliteOptions.value.find(opt => opt.value === visibility);
  return option ? option.label : visibility;
}

function resetForm() {
  if (!loading.value) {
    initializeForm();
  }
}
</script>

<style scoped>
:deep(.add-note-dialog .p-dialog-header) {
  background: var(--primary-color);
  color: white;
}

:deep(.add-note-dialog .p-dialog-header .p-dialog-title) {
  font-weight: 600;
}

:deep(.add-note-dialog .p-dialog-header .p-dialog-header-icon) {
  color: white;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-height: 70vh;
  overflow-y: auto;
}

/* Dossier Information */
.dossier-info {
  background: var(--section-background);
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid var(--card-border);
}

.dossier-info h4 {
  color: var(--primary-color);
  margin: 0 0 1rem 0;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
  font-size: 0.9rem;
}

/* Form */
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.field-group label {
  font-weight: 500;
  color: var(--text-color);
  font-size: 0.9rem;
}

.field-group .p-inputtext,
.field-group .p-textarea,
.field-group .p-select,
.field-group .p-multiselect,
.field-group .p-calendar,
.field-group .p-chips {
  width: 100%;
}

.error-message {
  color: var(--danger-color);
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.field-help {
  color: var(--text-secondary);
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.checkbox-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.checkbox-item label {
  margin: 0;
  font-size: 0.85rem;
  cursor: pointer;
}

/* File Upload */
:deep(.p-fileupload .p-fileupload-content) {
  background: var(--section-background);
  border: 1px dashed var(--card-border);
  border-radius: 8px;
}

.upload-empty {
  text-align: center;
  padding: 1.5rem;
  color: var(--text-secondary);
}

.upload-empty i {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  display: block;
  color: var(--primary-color);
}

.upload-empty p {
  margin: 0.25rem 0;
}

.upload-help {
  font-size: 0.8rem;
  color: var(--text-muted);
}

/* Preview */
.note-preview {
  background: var(--section-background);
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid var(--card-border);
}

.note-preview h4 {
  color: var(--primary-color);
  margin: 0 0 1rem 0;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.preview-content {
  background: var(--background-color);
  border-radius: 6px;
  padding: 1rem;
  border: 1px solid var(--card-border);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--card-border);
}

.preview-subject {
  font-weight: 600;
  color: var(--text-color);
  font-size: 1rem;
}

.priority-tag {
  font-size: 0.7rem !important;
}

.preview-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.75rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.preview-body {
  color: var(--text-color);
  line-height: 1.6;
  margin-bottom: 0.75rem;
  white-space: pre-wrap;
}

.preview-tags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  font-size: 0.8rem;
}

.tag-item {
  font-size: 0.7rem !important;
  padding: 0.125rem 0.375rem !important;
}

/* Dialog Footer */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  :deep(.add-note-dialog) {
    width: 95vw !important;
    max-width: none !important;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .preview-header {
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-start;
  }

  .preview-meta {
    flex-direction: column;
    gap: 0.5rem;
  }

  .dialog-footer {
    flex-direction: column;
  }
}
</style>