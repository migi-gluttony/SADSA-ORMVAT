<template>
  <div class="dynamic-form">
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="30px" />
      <span>Chargement du formulaire...</span>
    </div>
    
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <span>Erreur lors du chargement: {{ error }}</span>
    </div>
    
    <div v-else-if="formConfig" class="form-container">
      <div v-if="formConfig.title" class="form-title">
        <h4>{{ formConfig.title }}</h4>
        <p v-if="formConfig.description">{{ formConfig.description }}</p>
      </div>
      
      <div class="form-fields">
        <div 
          v-for="field in formConfig.fields" 
          :key="field.name"
          class="form-field"
          :class="{ 
            'field-error': fieldErrors[field.name],
            'field-required': field.required 
          }"
        >
          <label :for="field.name" class="field-label">
            {{ field.label }}
            <span v-if="field.required" class="required-indicator">*</span>
          </label>
          
          <!-- Text Input -->
          <InputText 
            v-if="field.type === 'text'"
            :id="field.name"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || ''"
            :disabled="field.disabled"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            @input="onFieldChange(field.name, $event.target.value)"
          />
          
          <!-- Number Input -->
          <InputNumber 
            v-else-if="field.type === 'number'"
            :id="field.name"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || ''"
            :disabled="field.disabled"
            :min="field.min"
            :max="field.max"
            :step="field.step"
            :mode="field.mode || 'decimal'"
            :currency="field.currency"
            :locale="field.locale"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            @input="onFieldChange(field.name, $event.value)"
          />
          
          <!-- Textarea -->
          <Textarea 
            v-else-if="field.type === 'textarea'"
            :id="field.name"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || ''"
            :disabled="field.disabled"
            :rows="field.rows || 3"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            @input="onFieldChange(field.name, $event.target.value)"
          />
          
          <!-- Select/Dropdown -->
          <Dropdown 
            v-else-if="field.type === 'select'"
            :id="field.name"
            v-model="formData[field.name]"
            :options="field.options"
            :option-label="field.optionLabel || 'label'"
            :option-value="field.optionValue || 'value'"
            :placeholder="field.placeholder || 'Sélectionner...'"
            :disabled="field.disabled"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            @change="onFieldChange(field.name, $event.value)"
          />
          
          <!-- Date -->
          <Calendar 
            v-else-if="field.type === 'date'"
            :id="field.name"
            v-model="formData[field.name]"
            :placeholder="field.placeholder || ''"
            :disabled="field.disabled"
            :date-format="field.dateFormat || 'dd/mm/yy'"
            :show-time="field.showTime"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            @date-select="onFieldChange(field.name, $event)"
          />
          
          <!-- Checkbox -->
          <div v-else-if="field.type === 'checkbox'" class="checkbox-container">
            <Checkbox 
              :id="field.name"
              v-model="formData[field.name]"
              :binary="true"
              :disabled="field.disabled"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @change="onFieldChange(field.name, $event.checked)"
            />
            <label :for="field.name" class="checkbox-label">{{ field.checkboxLabel || field.label }}</label>
          </div>
          
          <!-- Radio Group -->
          <div v-else-if="field.type === 'radio'" class="radio-group">
            <div 
              v-for="option in field.options" 
              :key="option.value"
              class="radio-option"
            >
              <RadioButton 
                :id="`${field.name}_${option.value}`"
                v-model="formData[field.name]"
                :value="option.value"
                :disabled="field.disabled"
                @change="onFieldChange(field.name, $event.value)"
              />
              <label :for="`${field.name}_${option.value}`" class="radio-label">
                {{ option.label }}
              </label>
            </div>
          </div>
          
          <!-- File Upload -->
          <div v-else-if="field.type === 'file'" class="file-upload-container">
            <FileUpload 
              :id="field.name"
              mode="basic"
              :name="field.name"
              :accept="field.accept || '*'"
              :max-file-size="field.maxFileSize || 10000000"
              :disabled="field.disabled"
              choose-label="Choisir un fichier"
              @select="onFileSelect(field.name, $event)"
              @remove="onFileRemove(field.name)"
            />
            
            <div v-if="uploadedFiles[field.name]" class="uploaded-file">
              <i class="pi pi-file"></i>
              <span>{{ uploadedFiles[field.name].name }}</span>
              <Button 
                icon="pi pi-times" 
                class="p-button-text p-button-sm"
                @click="removeFile(field.name)"
              />
            </div>
          </div>
          
          <!-- Help Text -->
          <small v-if="field.help" class="field-help">{{ field.help }}</small>
          
          <!-- Error Message -->
          <small v-if="fieldErrors[field.name]" class="field-error-message">
            {{ fieldErrors[field.name] }}
          </small>
        </div>
      </div>
      
      <!-- Form Actions (if defined in config) -->
      <div v-if="formConfig.actions" class="form-actions">
        <Button 
          v-for="action in formConfig.actions"
          :key="action.name"
          :label="action.label"
          :icon="action.icon"
          :class="action.class"
          :disabled="action.disabled"
          @click="onActionClick(action)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';

// PrimeVue components
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';
import Checkbox from 'primevue/checkbox';
import RadioButton from 'primevue/radiobutton';
import FileUpload from 'primevue/fileupload';
import Button from 'primevue/button';
import ProgressSpinner from 'primevue/progressspinner';

// Props
const props = defineProps({
  configPath: {
    type: String,
    required: true
  },
  modelValue: {
    type: Object,
    default: () => ({})
  },
  readonly: {
    type: Boolean,
    default: false
  },
  showActions: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['update:modelValue', 'action', 'change', 'validate']);

// State
const loading = ref(false);
const error = ref(null);
const formConfig = ref(null);
const formData = ref({});
const fieldErrors = ref({});
const uploadedFiles = ref({});

// Computed
const isValid = computed(() => {
  return Object.keys(fieldErrors.value).length === 0;
});

// Watchers
watch(() => props.modelValue, (newValue) => {
  if (newValue && typeof newValue === 'object') {
    formData.value = { ...newValue };
  }
}, { immediate: true, deep: true });

watch(() => props.configPath, () => {
  loadFormConfig();
}, { immediate: true });

watch(formData, (newValue) => {
  emit('update:modelValue', newValue);
  emit('change', newValue);
}, { deep: true });

// Methods
onMounted(() => {
  loadFormConfig();
});

async function loadFormConfig() {
  if (!props.configPath) {
    error.value = 'Chemin de configuration non spécifié';
    return;
  }
  
  try {
    loading.value = true;
    error.value = null;
    
    // Load JSON configuration
    const response = await fetch(props.configPath);
    
    if (!response.ok) {
      throw new Error(`Erreur HTTP: ${response.status}`);
    }
    
    const config = await response.json();
    
    // Validate config structure
    if (!config.fields || !Array.isArray(config.fields)) {
      throw new Error('Configuration invalide: champs manquants ou invalides');
    }
    
    formConfig.value = config;
    
    // Initialize form data with default values
    initializeFormData();
    
    console.log('Configuration de formulaire chargée:', config);
    
  } catch (err) {
    console.error('Erreur lors du chargement de la configuration:', err);
    error.value = err.message || 'Erreur lors du chargement de la configuration';
  } finally {
    loading.value = false;
  }
}

function initializeFormData() {
  if (!formConfig.value) return;
  
  const initialData = { ...props.modelValue };
  
  formConfig.value.fields.forEach(field => {
    if (initialData[field.name] === undefined) {
      // Set default values based on field type
      switch (field.type) {
        case 'checkbox':
          initialData[field.name] = field.value || false;
          break;
        case 'number':
          initialData[field.name] = field.value || null;
          break;
        case 'date':
          initialData[field.name] = field.value ? new Date(field.value) : null;
          break;
        default:
          initialData[field.name] = field.value || '';
      }
    }
  });
  
  formData.value = initialData;
}

function onFieldChange(fieldName, value) {
  formData.value[fieldName] = value;
  
  // Clear field error when user starts typing
  if (fieldErrors.value[fieldName]) {
    delete fieldErrors.value[fieldName];
  }
  
  // Validate field
  validateField(fieldName, value);
}

function validateField(fieldName, value) {
  const field = formConfig.value?.fields.find(f => f.name === fieldName);
  if (!field) return;
  
  const errors = [];
  
  // Required validation
  if (field.required && (!value || (typeof value === 'string' && value.trim() === ''))) {
    errors.push(`${field.label} est requis`);
  }
  
  // Type-specific validations
  if (value) {
    switch (field.type) {
      case 'number':
        if (field.min !== undefined && value < field.min) {
          errors.push(`${field.label} doit être supérieur ou égal à ${field.min}`);
        }
        if (field.max !== undefined && value > field.max) {
          errors.push(`${field.label} doit être inférieur ou égal à ${field.max}`);
        }
        break;
        
      case 'text':
        if (field.minLength && value.length < field.minLength) {
          errors.push(`${field.label} doit contenir au moins ${field.minLength} caractères`);
        }
        if (field.maxLength && value.length > field.maxLength) {
          errors.push(`${field.label} ne peut pas dépasser ${field.maxLength} caractères`);
        }
        if (field.pattern && !new RegExp(field.pattern).test(value)) {
          errors.push(field.patternMessage || `${field.label} ne respecte pas le format requis`);
        }
        break;
    }
  }
  
  // Custom validation
  if (field.validation && typeof field.validation === 'function') {
    const customError = field.validation(value, formData.value);
    if (customError) {
      errors.push(customError);
    }
  }
  
  // Update field errors
  if (errors.length > 0) {
    fieldErrors.value[fieldName] = errors[0]; // Show first error
  } else {
    delete fieldErrors.value[fieldName];
  }
}

function validateForm() {
  fieldErrors.value = {};
  
  if (!formConfig.value) return false;
  
  formConfig.value.fields.forEach(field => {
    validateField(field.name, formData.value[field.name]);
  });
  
  const isFormValid = Object.keys(fieldErrors.value).length === 0;
  
  emit('validate', {
    isValid: isFormValid,
    errors: fieldErrors.value,
    data: formData.value
  });
  
  return isFormValid;
}

function onFileSelect(fieldName, event) {
  const file = event.files[0];
  if (file) {
    uploadedFiles.value[fieldName] = file;
    formData.value[fieldName] = file.name; // Store filename in form data
    onFieldChange(fieldName, file.name);
  }
}

function onFileRemove(fieldName) {
  delete uploadedFiles.value[fieldName];
  formData.value[fieldName] = '';
  onFieldChange(fieldName, '');
}

function removeFile(fieldName) {
  onFileRemove(fieldName);
}

function onActionClick(action) {
  emit('action', {
    name: action.name,
    action: action,
    formData: formData.value,
    isValid: isValid.value
  });
}

function reset() {
  formData.value = {};
  fieldErrors.value = {};
  uploadedFiles.value = {};
  initializeFormData();
}

function getFormData() {
  return { ...formData.value };
}

function setFormData(data) {
  formData.value = { ...data };
}

function getValidationErrors() {
  return { ...fieldErrors.value };
}

function clearErrors() {
  fieldErrors.value = {};
}

// Expose methods for parent component
defineExpose({
  validateForm,
  reset,
  getFormData,
  setFormData,
  getValidationErrors,
  clearErrors,
  isValid
});
</script>
