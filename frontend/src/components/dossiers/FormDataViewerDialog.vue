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

<style scoped>
.form-viewer {
  padding: 0;
  max-height: 70vh;
  overflow-y: auto;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.form-info h3 {
  margin: 0 0 0.5rem 0;
  color: var(--primary-color);
  font-size: 1.25rem;
}

.form-info p {
  margin: 0 0 1rem 0;
  color: var(--text-secondary);
  line-height: 1.4;
}

.form-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.last-modified {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.form-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.form-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.data-section h4,
.documents-section h4,
.config-section h4,
.validation-section h4 {
  color: var(--primary-color);
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
}

/* Structured Data Display */
.structured-data {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.data-field {
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-sm);
  padding: 1rem;
  transition: all 0.2s ease;
}

.data-field:hover {
  border-color: var(--primary-color);
  background: rgba(var(--primary-color-rgb), 0.02);
}

.data-field.empty-field {
  background: rgba(var(--warning-color), 0.05);
  border-color: var(--warning-color);
}

.data-field.required-field {
  border-left: 4px solid var(--primary-color);
}

.field-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.required-indicator {
  color: var(--danger-color);
  font-weight: bold;
}

.field-value {
  font-size: 1rem;
  margin-bottom: 0.25rem;
}

.field-value .empty-value {
  color: var(--text-muted);
  font-style: italic;
}

.field-description {
  font-size: 0.8rem;
  color: var(--text-secondary);
  line-height: 1.3;
}

/* Value type styling */
.value-boolean {
  font-weight: 600;
}

.value-number {
  font-family: 'Courier New', monospace;
  font-weight: 500;
}

.value-date {
  color: var(--info-color);
}

.value-email {
  color: var(--info-color);
  text-decoration: underline;
}

.value-phone {
  font-family: 'Courier New', monospace;
}

.value-array,
.value-object {
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  background: var(--section-background);
  padding: 0.5rem;
  border-radius: 4px;
  white-space: pre-wrap;
}

/* Raw Data Display */
.raw-data {
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-sm);
  overflow: hidden;
}

.raw-data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--section-background);
  border-bottom: 1px solid var(--card-border);
}

.raw-data-header h5 {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-color);
}

.json-display {
  background: var(--background-color);
  padding: 1rem;
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 0.8rem;
  line-height: 1.4;
  color: var(--text-color);
  white-space: pre-wrap;
  overflow-x: auto;
}

/* Documents Section */
.documents-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: var(--section-background);
  border-radius: var(--border-radius-sm);
  font-size: 0.9rem;
}

.document-item i {
  color: var(--primary-color);
}

/* Configuration Section */
.config-display {
  background: var(--background-color);
  padding: 1rem;
  font-family: 'Courier New', monospace;
  font-size: 0.8rem;
  line-height: 1.4;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--card-border);
  overflow-x: auto;
  margin: 0;
}

/* Validation Issues */
.issues-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  background: rgba(var(--danger-color), 0.05);
  border: 1px solid rgba(var(--danger-color), 0.2);
  border-radius: var(--border-radius-sm);
}

.issue-icon {
  color: var(--danger-color);
  font-size: 1rem;
  margin-top: 0.125rem;
  flex-shrink: 0;
}

.issue-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.issue-content strong {
  color: var(--danger-color);
  font-size: 0.9rem;
}

.issue-content span {
  color: var(--text-color);
  font-size: 0.8rem;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  display: block;
}

.empty-state h4 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
}

.empty-state p {
  margin: 0;
  line-height: 1.4;
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

/* Accordion styling */
:deep(.p-accordion-header-link) {
  font-size: 0.9rem;
  padding: 0.75rem 1rem;
}

:deep(.p-accordion-content) {
  padding: 1rem;
}

/* Responsive design */
@media (max-width: 768px) {
  .form-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .form-actions {
    justify-content: center;
  }
  
  .form-meta {
    justify-content: center;
  }
  
  .structured-data {
    gap: 0.75rem;
  }
  
  .data-field {
    padding: 0.75rem;
  }
  
  .json-display,
  .config-display {
    font-size: 0.7rem;
  }
}

/* Print styles */
@media print {
  .form-actions,
  :deep(.p-dialog-footer) {
    display: none !important;
  }
  
  .form-viewer {
    max-height: none;
    overflow: visible;
  }
  
  .data-field {
    break-inside: avoid;
    margin-bottom: 0.5rem;
  }
}

/* Dark mode adjustments */
.dark-mode .data-field {
  background: var(--clr-surface-a10);
  border-color: var(--clr-surface-a20);
}

.dark-mode .data-field:hover {
  background: var(--clr-surface-a20);
}

.dark-mode .json-display,
.dark-mode .config-display {
  background: var(--clr-surface-a10);
  border-color: var(--clr-surface-a20);
}

.dark-mode .raw-data-header {
  background: var(--clr-surface-a20);
  border-color: var(--clr-surface-a30);
}

.dark-mode .document-item {
  background: var(--clr-surface-a20);
}
</style>