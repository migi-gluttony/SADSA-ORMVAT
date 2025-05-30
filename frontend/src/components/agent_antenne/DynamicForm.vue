<template>
  <div class="dynamic-form">
    <form @submit.prevent="handleSubmit" class="form-content">
      
      <!-- Render form fields based on structure -->
      <div v-if="formFields.length > 0" class="form-fields">
        <div 
          v-for="field in formFields" 
          :key="field.name"
          class="form-field"
          :class="{ 'form-field-required': field.required }"
        >
          
          <!-- Text Input -->
          <div v-if="field.type === 'text'" class="field-group">
            <label :for="field.name" class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <InputText
              :id="field.name"
              v-model="localFormData[field.name]"
              :placeholder="field.placeholder"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @input="onFieldChange(field.name, $event.target.value)"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Number Input -->
          <div v-else-if="field.type === 'number'" class="field-group">
            <label :for="field.name" class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <InputNumber
              :id="field.name"
              v-model="localFormData[field.name]"
              :placeholder="field.placeholder"
              :min="field.min"
              :max="field.max"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @input="onFieldChange(field.name, $event.value)"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Textarea -->
          <div v-else-if="field.type === 'textarea'" class="field-group">
            <label :for="field.name" class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <Textarea
              :id="field.name"
              v-model="localFormData[field.name]"
              :placeholder="field.placeholder"
              :rows="field.rows || 3"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @input="onFieldChange(field.name, $event.target.value)"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Select/Dropdown -->
          <div v-else-if="field.type === 'select'" class="field-group">
            <label :for="field.name" class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <Dropdown
              :id="field.name"
              v-model="localFormData[field.name]"
              :options="field.options"
              :optionLabel="field.optionLabel || 'label'"
              :optionValue="field.optionValue || 'value'"
              :placeholder="field.placeholder"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @change="onFieldChange(field.name, $event.value)"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Radio Buttons -->
          <div v-else-if="field.type === 'radio'" class="field-group">
            <label class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <div class="radio-group">
              <div 
                v-for="option in field.options" 
                :key="option.value"
                class="radio-item"
              >
                <RadioButton
                  :id="`${field.name}_${option.value}`"
                  v-model="localFormData[field.name]"
                  :value="option.value"
                  @change="onFieldChange(field.name, $event.value)"
                />
                <label :for="`${field.name}_${option.value}`" class="radio-label">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Checkbox -->
          <div v-else-if="field.type === 'checkbox'" class="field-group">
            <div class="checkbox-group">
              <Checkbox
                :id="field.name"
                v-model="localFormData[field.name]"
                :binary="true"
                @change="onFieldChange(field.name, $event.checked)"
              />
              <label :for="field.name" class="checkbox-label">
                {{ field.label }}
                <span v-if="field.required" class="required-star">*</span>
              </label>
            </div>
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Date -->
          <div v-else-if="field.type === 'date'" class="field-group">
            <label :for="field.name" class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <Calendar
              :id="field.name"
              v-model="localFormData[field.name]"
              :placeholder="field.placeholder"
              dateFormat="dd/mm/yy"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @date-select="onFieldChange(field.name, $event)"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- File Upload (Special case) -->
          <div v-else-if="field.type === 'file'" class="field-group">
            <label class="field-label">
              {{ field.label }}
              <span v-if="field.required" class="required-star">*</span>
            </label>
            <FileUpload
              mode="basic"
              :accept="field.accept || '.pdf,.jpg,.jpeg,.png'"
              :maxFileSize="field.maxSize || 1000000"
              @select="onFileSelect"
              chooseLabel="Choisir un fichier"
              :auto="false"
            />
            <small v-if="field.description" class="field-help">{{ field.description }}</small>
            <small v-if="fieldErrors[field.name]" class="p-error">{{ fieldErrors[field.name] }}</small>
          </div>

          <!-- Unknown field type fallback -->
          <div v-else class="field-group">
            <label class="field-label">{{ field.label || field.name }}</label>
            <div class="unknown-field">
              <i class="pi pi-question-circle"></i>
              <span>Type de champ non supporté: {{ field.type }}</span>
            </div>
          </div>

        </div>
      </div>

      <!-- No fields message -->
      <div v-else class="no-fields-message">
        <i class="pi pi-info-circle"></i>
        <span>Aucun champ configuré dans ce formulaire</span>
      </div>

      <!-- Form Actions -->
      <div v-if="formFields.length > 0" class="form-actions">
        <Button 
          type="button" 
          label="Effacer" 
          icon="pi pi-refresh" 
          @click="resetForm"
          class="p-button-outlined p-button-secondary"
        />
        <Button 
          type="submit" 
          label="Sauvegarder" 
          icon="pi pi-save" 
          :loading="saving"
          :disabled="!isFormValid"
          class="p-button-success"
        />
      </div>
    </form>
    
    <!-- Debug info (only in development) -->
    <div v-if="showDebug" class="debug-section">
      <details>
        <summary>Debug Info</summary>
        <pre>{{ JSON.stringify({ formStructure, localFormData, fieldErrors }, null, 2) }}</pre>
      </details>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';

// PrimeVue Components
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import Dropdown from 'primevue/dropdown';
import RadioButton from 'primevue/radiobutton';
import Checkbox from 'primevue/checkbox';
import Calendar from 'primevue/calendar';
import FileUpload from 'primevue/fileupload';
import Button from 'primevue/button';

// Props
const props = defineProps({
  formStructure: {
    type: Object,
    default: () => ({})
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  showDebug: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['form-change', 'form-save', 'file-select']);

// Reactive data
const localFormData = ref({});
const fieldErrors = ref({});
const saving = ref(false);

// Computed properties
const formFields = computed(() => {
  const structure = props.formStructure;
  
  // Handle different JSON structure formats
  if (structure.fields && Array.isArray(structure.fields)) {
    return structure.fields;
  } else if (structure.form && Array.isArray(structure.form)) {
    return structure.form;
  } else if (Array.isArray(structure)) {
    return structure;
  } else if (structure.properties) {
    // Convert JSON Schema format to our format
    return Object.keys(structure.properties).map(key => {
      const prop = structure.properties[key];
      return {
        name: key,
        label: prop.title || key,
        type: mapJsonSchemaType(prop.type),
        required: structure.required?.includes(key) || false,
        description: prop.description,
        ...prop
      };
    });
  }
  
  return [];
});

const isFormValid = computed(() => {
  // Check if all required fields are filled
  const requiredFields = formFields.value.filter(field => field.required);
  return requiredFields.every(field => {
    const value = localFormData.value[field.name];
    return value !== null && value !== undefined && value !== '';
  });
});

// Watchers
watch(() => props.formData, (newData) => {
  localFormData.value = { ...newData };
}, { deep: true, immediate: true });

watch(localFormData, (newData) => {
  emit('form-change', newData);
}, { deep: true });

// Lifecycle
onMounted(() => {
  initializeFormData();
});

// Methods
function initializeFormData() {
  // Initialize form data with default values
  const initialData = { ...props.formData };
  
  formFields.value.forEach(field => {
    if (!(field.name in initialData)) {
      initialData[field.name] = getDefaultValue(field);
    }
  });
  
  localFormData.value = initialData;
}

function getDefaultValue(field) {
  switch (field.type) {
    case 'text':
    case 'textarea':
      return field.default || '';
    case 'number':
      return field.default || null;
    case 'checkbox':
      return field.default || false;
    case 'select':
    case 'radio':
      return field.default || null;
    case 'date':
      return field.default ? new Date(field.default) : null;
    default:
      return field.default || null;
  }
}

function mapJsonSchemaType(schemaType) {
  const typeMap = {
    'string': 'text',
    'number': 'number',
    'integer': 'number',
    'boolean': 'checkbox',
    'array': 'select', // Assuming multiselect for arrays
    'object': 'text' // Fallback
  };
  return typeMap[schemaType] || 'text';
}

function onFieldChange(fieldName, value) {
  localFormData.value[fieldName] = value;
  
  // Clear field error when user starts typing
  if (fieldErrors.value[fieldName]) {
    delete fieldErrors.value[fieldName];
  }
  
  // Validate field
  validateField(fieldName, value);
}

function validateField(fieldName, value) {
  const field = formFields.value.find(f => f.name === fieldName);
  if (!field) return;
  
  const errors = [];
  
  // Required validation
  if (field.required && (value === null || value === undefined || value === '')) {
    errors.push(`${field.label} est requis`);
  }
  
  // Type-specific validations
  if (value && value !== '') {
    switch (field.type) {
      case 'number':
        const numValue = parseFloat(value);
        if (isNaN(numValue)) {
          errors.push('Doit être un nombre valide');
        } else {
          if (field.min !== undefined && numValue < field.min) {
            errors.push(`Minimum: ${field.min}`);
          }
          if (field.max !== undefined && numValue > field.max) {
            errors.push(`Maximum: ${field.max}`);
          }
        }
        break;
        
      case 'text':
      case 'textarea':
        if (field.minLength && value.length < field.minLength) {
          errors.push(`Minimum ${field.minLength} caractères`);
        }
        if (field.maxLength && value.length > field.maxLength) {
          errors.push(`Maximum ${field.maxLength} caractères`);
        }
        if (field.pattern && !new RegExp(field.pattern).test(value)) {
          errors.push(field.patternMessage || 'Format invalide');
        }
        break;
    }
  }
  
  // Custom validation
  if (field.validator && typeof field.validator === 'function') {
    const customError = field.validator(value);
    if (customError) {
      errors.push(customError);
    }
  }
  
  // Update errors
  if (errors.length > 0) {
    fieldErrors.value[fieldName] = errors[0];
  } else {
    delete fieldErrors.value[fieldName];
  }
}

function validateForm() {
  fieldErrors.value = {};
  
  formFields.value.forEach(field => {
    const value = localFormData.value[field.name];
    validateField(field.name, value);
  });
  
  return Object.keys(fieldErrors.value).length === 0;
}

function handleSubmit() {
  if (!validateForm()) {
    return;
  }
  
  saving.value = true;
  
  // Emit save event
  emit('form-save', localFormData.value);
  
  // Reset saving state after a delay (this would normally be done when the save operation completes)
  setTimeout(() => {
    saving.value = false;
  }, 1000);
}

function resetForm() {
  localFormData.value = {};
  fieldErrors.value = {};
  initializeFormData();
}

function onFileSelect(event) {
  emit('file-select', event);
}
</script>

<style scoped>
.dynamic-form {
  width: 100%;
}

.form-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Form Fields */
.form-fields {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  width: 100%;
}

.form-field-required {
  position: relative;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* Labels */
.field-label {
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.required-star {
  color: #dc2626;
  font-weight: 700;
}

.field-help {
  color: var(--text-color-secondary);
  font-size: 0.8rem;
  line-height: 1.4;
}

/* Form Controls */
.form-field :deep(.p-inputtext),
.form-field :deep(.p-inputnumber-input),
.form-field :deep(.p-dropdown),
.form-field :deep(.p-calendar),
.form-field :deep(.p-inputtextarea) {
  width: 100%;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  padding: 0.75rem;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.form-field :deep(.p-inputtext:focus),
.form-field :deep(.p-inputnumber-input:focus),
.form-field :deep(.p-dropdown:focus),
.form-field :deep(.p-calendar:focus),
.form-field :deep(.p-inputtextarea:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-color-rgb), 0.2);
}

/* Radio Group */
.radio-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.radio-label {
  font-size: 0.9rem;
  color: var(--text-color);
  cursor: pointer;
  margin: 0;
}

/* Checkbox Group */
.checkbox-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0;
}

.checkbox-label {
  font-size: 0.9rem;
  color: var(--text-color);
  cursor: pointer;
  margin: 0;
}

/* Unknown Field */
.unknown-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 6px;
  color: #856404;
  font-size: 0.9rem;
}

/* No Fields Message */
.no-fields-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--text-color-secondary);
  font-style: italic;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed var(--border-color);
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

/* Debug Section */
.debug-section {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

.debug-section details {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 1rem;
}

.debug-section summary {
  cursor: pointer;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.debug-section pre {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 1rem;
  font-size: 0.8rem;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}

/* Responsive */
@media (max-width: 768px) {
  .form-actions {
    flex-direction: column;
  }
  
  .radio-group {
    gap: 0.5rem;
  }
}

/* Dark Mode */
.dark-mode .unknown-field {
  background: #2d3748;
  border-color: #4a5568;
  color: #e2e8f0;
}

.dark-mode .no-fields-message {
  background: #2d3748;
  border-color: #4a5568;
}

.dark-mode .debug-section details {
  background: #2d3748;
}

.dark-mode .debug-section pre {
  background: #1a202c;
  border-color: #4a5568;
  color: #e2e8f0;
}
</style>