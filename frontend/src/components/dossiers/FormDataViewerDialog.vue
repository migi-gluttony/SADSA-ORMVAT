<template>
  <Dialog 
    v-model:visible="dialogVisible" 
    modal 
    :header="dialogTitle"
    :style="{ width: '800px', maxHeight: '90vh' }"
    :maximizable="true"
  >
    <div class="form-viewer">
      <div v-if="form" class="form-header">
        <div class="form-info">
          <h3>{{ form.title }}</h3>
          <p v-if="form.description">{{ form.description }}</p>
          <div class="form-meta">
            <Tag 
              :value="form.isCompleted ? 'Complété' : 'En attente'" 
              :severity="form.isCompleted ? 'success' : 'warning'"
            />
            <span v-if="form.lastModified" class="last-modified">
              Dernière modification: {{ formatDate(form.lastModified) }}
            </span>
          </div>
        </div>
        
        <div class="form-actions">
          <Button 
            v-if="form.isCompleted"
            label="Exporter PDF" 
            icon="pi pi-file-pdf" 
            @click="exportToPDF"
            class="p-button-outlined p-button-sm"
          />
          <Button 
            label="Imprimer" 
            icon="pi pi-print" 
            @click="printForm"
            class="p-button-outlined p-button-sm"
          />
        </div>
      </div>

      <Divider />

      <!-- Form Data Display -->
      <div v-if="formDataExists" class="form-content">
        <div class="data-section">
          <h4><i class="pi pi-database"></i> Données du formulaire</h4>
          <div class="data-display">
            <div v-if="structuredData.length > 0" class="structured-data">
              <div 
                v-for="field in structuredData" 
                :key="field.key"
                class="data-field"
                :class="{ 'required-field': field.required, 'empty-field': !field.value }"
              >
                <div class="field-label">
                  <strong>{{ field.label }}</strong>
                  <span v-if="field.required" class="required-indicator">*</span>
                </div>
                <div class="field-value">
                  <span v-if="field.value" :class="getFieldValueClass(field.type)">
                    {{ formatFieldValue(field.value, field.type) }}
                  </span>
                  <span v-else class="empty-value">Non renseigné</span>
                </div>
                <div v-if="field.description" class="field-description">
                  {{ field.description }}
                </div>
              </div>
            </div>
            
            <div v-else class="raw-data">
              <div class="raw-data-header">
                <h5>Données brutes (JSON)</h5>
                <Button 
                  icon="pi pi-copy" 
                  @click="copyToClipboard"
                  class="p-button-text p-button-sm"
                  v-tooltip.top="'Copier'"
                />
              </div>
              <pre class="json-display">{{ formattedJsonData }}</pre>
            </div>
          </div>
        </div>

        <!-- Required Documents Section -->
        <div v-if="form.requiredDocuments && form.requiredDocuments.length > 0" class="documents-section">
          <h4><i class="pi pi-file"></i> Documents requis</h4>
          <div class="documents-list">
            <div 
              v-for="doc in form.requiredDocuments" 
              :key="doc"
              class="document-item"
            >
              <i class="pi pi-file-o"></i>
              <span>{{ doc }}</span>
            </div>
          </div>
        </div>

        <!-- Form Configuration -->
        <div v-if="showFormConfig && form.formConfig" class="config-section">
          <Accordion>
            <AccordionTab header="Configuration du formulaire (Technique)">
              <pre class="config-display">{{ formattedConfigData }}</pre>
            </AccordionTab>
          </Accordion>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state">
        <i class="pi pi-file-o empty-icon"></i>
        <h4>Aucune donnée disponible</h4>
        <p>Ce formulaire n'a pas encore été rempli ou les données ne sont pas disponibles.</p>
      </div>

      <!-- Validation Issues -->
      <div v-if="validationIssues.length > 0" class="validation-section">
        <h4><i class="pi pi-exclamation-triangle"></i> Problèmes de validation</h4>
        <div class="issues-list">
          <div 
            v-for="issue in validationIssues" 
            :key="issue.field"
            class="issue-item"
          >
            <i class="pi pi-times-circle issue-icon"></i>
            <div class="issue-content">
              <strong>{{ issue.field }}</strong>
              <span>{{ issue.message }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <Button 
        label="Fermer" 
        icon="pi pi-times" 
        @click="closeDialog"
        class="p-button-outlined"
      />
      <Button 
        v-if="canAddNote"
        label="Ajouter Note" 
        icon="pi pi-comment" 
        @click="addNoteAboutForm"
        class="p-button-info"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import Divider from 'primevue/divider';
import Accordion from 'primevue/accordion';
import AccordionTab from 'primevue/accordiontab';

// Props & Emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  form: {
    type: Object,
    default: null
  },
  dossier: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:visible', 'add-note']);

// State
const showFormConfig = ref(false);
const validationIssues = ref([]);

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const dialogTitle = computed(() => {
  return props.form ? `Formulaire: ${props.form.title}` : 'Données du formulaire';
});

const formDataExists = computed(() => {
  return props.form && (
    (props.form.formData && Object.keys(props.form.formData).length > 0) ||
    (props.form.isCompleted)
  );
});

const currentUser = computed(() => AuthService.getCurrentUser());

const canAddNote = computed(() => {
  return currentUser.value?.role === 'AGENT_GUC' || currentUser.value?.role === 'ADMIN';
});

const structuredData = computed(() => {
  if (!props.form?.formData || !props.form?.formConfig) {
    return [];
  }

  const config = props.form.formConfig;
  const data = props.form.formData;

  // If form config has fields definition, use it to structure the data
  if (config.fields && Array.isArray(config.fields)) {
    return config.fields.map(field => ({
      key: field.name || field.key,
      label: field.label || field.name || field.key,
      value: data[field.name || field.key],
      type: field.type || 'text',
      required: field.required || false,
      description: field.description || field.help
    }));
  }

  // Otherwise, create a simple structure from the data
  return Object.entries(data).map(([key, value]) => ({
    key,
    label: formatLabel(key),
    value,
    type: detectFieldType(value),
    required: false,
    description: null
  }));
});

const formattedJsonData = computed(() => {
  if (!props.form?.formData) return '{}';
  return JSON.stringify(props.form.formData, null, 2);
});

const formattedConfigData = computed(() => {
  if (!props.form?.formConfig) return '{}';
  return JSON.stringify(props.form.formConfig, null, 2);
});

// Watchers
watch(() => props.visible, (newValue) => {
  if (newValue && props.form) {
    validateFormData();
  }
});

// Methods
function formatLabel(key) {
  // Convert camelCase or snake_case to readable label
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/_/g, ' ')
    .replace(/^./, str => str.toUpperCase())
    .trim();
}

function detectFieldType(value) {
  if (typeof value === 'boolean') return 'boolean';
  if (typeof value === 'number') return 'number';
  if (value instanceof Date || /^\d{4}-\d{2}-\d{2}/.test(value)) return 'date';
  if (typeof value === 'string' && value.includes('@')) return 'email';
  if (typeof value === 'string' && /^\d{10,}$/.test(value)) return 'phone';
  if (Array.isArray(value)) return 'array';
  if (typeof value === 'object') return 'object';
  return 'text';
}

function formatFieldValue(value, type) {
  if (value === null || value === undefined) return '';
  
  switch (type) {
    case 'boolean':
      return value ? 'Oui' : 'Non';
    case 'date':
      return formatDate(value);
    case 'number':
      return new Intl.NumberFormat('fr-FR').format(value);
    case 'array':
      return Array.isArray(value) ? value.join(', ') : value;
    case 'object':
      return JSON.stringify(value, null, 2);
    default:
      return String(value);
  }
}

function getFieldValueClass(type) {
  return {
    'value-boolean': type === 'boolean',
    'value-number': type === 'number',
    'value-date': type === 'date',
    'value-email': type === 'email',
    'value-phone': type === 'phone',
    'value-array': type === 'array',
    'value-object': type === 'object'
  };
}

function validateFormData() {
  validationIssues.value = [];
  
  if (!props.form?.formData) return;
  
  const config = props.form.formConfig;
  const data = props.form.formData;
  
  if (config?.fields) {
    config.fields.forEach(field => {
      const value = data[field.name || field.key];
      
      // Check required fields
      if (field.required && (!value || (typeof value === 'string' && !value.trim()))) {
        validationIssues.value.push({
          field: field.label || field.name || field.key,
          message: 'Champ requis manquant'
        });
      }
      
      // Type validation
      if (value && field.type) {
        switch (field.type) {
          case 'email':
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
              validationIssues.value.push({
                field: field.label || field.name || field.key,
                message: 'Format email invalide'
              });
            }
            break;
          case 'number':
            if (isNaN(Number(value))) {
              validationIssues.value.push({
                field: field.label || field.name || field.key,
                message: 'Doit être un nombre'
              });
            }
            break;
        }
      }
    });
  }
}

function formatDate(date) {
  if (!date) return '';
  try {
    return new Intl.DateTimeFormat('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(date));
  } catch {
    return String(date);
  }
}

async function copyToClipboard() {
  try {
    await navigator.clipboard.writeText(formattedJsonData.value);
    // Could emit a toast success event here
  } catch (error) {
    console.error('Erreur lors de la copie:', error);
  }
}

function exportToPDF() {
  // Implementation for PDF export
  window.print();
}

function printForm() {
  // Implementation for printing
  window.print();
}

function addNoteAboutForm() {
  emit('add-note', {
    subject: `Formulaire: ${props.form?.title}`,
    relatedForm: props.form
  });
}

function closeDialog() {
  emit('update:visible', false);
}
</script>

