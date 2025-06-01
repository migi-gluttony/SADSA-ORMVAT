<template>
  <div class="dynamic-form">
    <form @submit.prevent="handleSubmit" class="form-content">
      
      <!-- Form Header -->
      <div v-if="formConfig.formTitle || formConfig.formDescription" class="form-header">
        <h3 v-if="formConfig.formTitle" class="form-title">{{ formConfig.formTitle }}</h3>
        <p v-if="formConfig.formDescription" class="form-description">{{ formConfig.formDescription }}</p>
      </div>
      
      <!-- Render by sections if available -->
      <div v-if="formSections.length > 0" class="form-sections">
        <div 
          v-for="section in formSections" 
          :key="section.title"
          class="form-section"
        >
          <h4 class="section-title">{{ section.title }}</h4>
          <div class="section-fields">
            <div 
              v-for="fieldName in section.fields" 
              :key="fieldName"
            >
              <div v-if="getFieldByName(fieldName)" class="form-field" :class="{ 'form-field-required': getFieldByName(fieldName).required }">
                <!-- Text Input -->
                <template v-if="getFieldByName(fieldName).type === 'text'">
                  <div class="field-group">
                    <label :for="fieldName" class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <InputText
                      :id="fieldName"
                      v-model="localFormData[fieldName]"
                      :placeholder="getFieldByName(fieldName).placeholder"
                      :class="{ 'p-invalid': fieldErrors[fieldName] }"
                      @input="onFieldChange(fieldName, $event.target.value)"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Number Input -->
                <template v-else-if="getFieldByName(fieldName).type === 'number'">
                  <div class="field-group">
                    <label :for="fieldName" class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <InputNumber
                      :id="fieldName"
                      v-model="localFormData[fieldName]"
                      :placeholder="getFieldByName(fieldName).placeholder"
                      :min="getFieldByName(fieldName).min"
                      :max="getFieldByName(fieldName).max"
                      :class="{ 'p-invalid': fieldErrors[fieldName] }"
                      @input="onFieldChange(fieldName, $event.value)"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Textarea -->
                <template v-else-if="getFieldByName(fieldName).type === 'textarea'">
                  <div class="field-group">
                    <label :for="fieldName" class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <Textarea
                      :id="fieldName"
                      v-model="localFormData[fieldName]"
                      :placeholder="getFieldByName(fieldName).placeholder"
                      :rows="getFieldByName(fieldName).rows || 3"
                      :class="{ 'p-invalid': fieldErrors[fieldName] }"
                      @input="onFieldChange(fieldName, $event.target.value)"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Select/Dropdown -->
                <template v-else-if="getFieldByName(fieldName).type === 'select'">
                  <div class="field-group">
                    <label :for="fieldName" class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <Dropdown
                      :id="fieldName"
                      v-model="localFormData[fieldName]"
                      :options="getFieldByName(fieldName).options"
                      optionLabel="label"
                      optionValue="value"
                      :placeholder="getFieldByName(fieldName).placeholder || 'Sélectionner...'"
                      :class="{ 'p-invalid': fieldErrors[fieldName] }"
                      @change="onFieldChange(fieldName, $event.value)"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Radio Buttons -->
                <template v-else-if="getFieldByName(fieldName).type === 'radio'">
                  <div class="field-group">
                    <label class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <div class="radio-group">
                      <div 
                        v-for="option in getFieldByName(fieldName).options" 
                        :key="option.value"
                        class="radio-item"
                      >
                        <RadioButton
                          v-model="localFormData[fieldName]"
                          :inputId="`${fieldName}_${option.value}`"
                          :name="fieldName"
                          :value="option.value"
                        />
                        <label :for="`${fieldName}_${option.value}`" class="radio-label">
                          {{ option.label }}
                        </label>
                      </div>
                    </div>
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Checkbox -->
                <template v-else-if="getFieldByName(fieldName).type === 'checkbox'">
                  <div class="field-group">
                    <div class="checkbox-group">
                      <Checkbox
                        v-model="localFormData[fieldName]"
                        :inputId="fieldName"
                        :binary="true"
                      />
                      <label :for="fieldName" class="checkbox-label">
                        {{ getFieldByName(fieldName).label }}
                        <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                      </label>
                    </div>
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Date -->
                <template v-else-if="getFieldByName(fieldName).type === 'date'">
                  <div class="field-group">
                    <label :for="fieldName" class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <Calendar
                      :id="fieldName"
                      v-model="localFormData[fieldName]"
                      :placeholder="getFieldByName(fieldName).placeholder"
                      dateFormat="dd/mm/yy"
                      :class="{ 'p-invalid': fieldErrors[fieldName] }"
                      @date-select="onFieldChange(fieldName, $event)"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- File Upload -->
                <template v-else-if="getFieldByName(fieldName).type === 'file'">
                  <div class="field-group">
                    <label class="field-label">
                      {{ getFieldByName(fieldName).label }}
                      <span v-if="getFieldByName(fieldName).required" class="required-star">*</span>
                    </label>
                    <FileUpload
                      mode="basic"
                      :accept="getFieldByName(fieldName).accept || '.pdf,.jpg,.jpeg,.png'"
                      :maxFileSize="getFieldByName(fieldName).maxSize || 1000000"
                      @select="onFileSelect"
                      chooseLabel="Choisir un fichier"
                      :auto="false"
                    />
                    <small v-if="getFieldByName(fieldName).description" class="field-help">{{ getFieldByName(fieldName).description }}</small>
                    <small v-if="fieldErrors[fieldName]" class="p-error">{{ fieldErrors[fieldName] }}</small>
                  </div>
                </template>

                <!-- Unknown field type -->
                <template v-else>
                  <div class="field-group">
                    <label class="field-label">{{ getFieldByName(fieldName).label || fieldName }}</label>
                    <div class="unknown-field">
                      <i class="pi pi-question-circle"></i>
                      <span>Type de champ non supporté: {{ getFieldByName(fieldName).type }}</span>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Fallback: render all fields if no sections -->
      <div v-else-if="formFields.length > 0" class="form-fields">
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
              optionLabel="label"
              optionValue="value"
              :placeholder="field.placeholder || 'Sélectionner...'"
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
                  v-model="localFormData[field.name]"
                  :inputId="`${field.name}_${option.value}`"
                  :name="field.name"
                  :value="option.value"
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
                v-model="localFormData[field.name]"
                :inputId="field.name"
                :binary="true"
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

      <!-- Cache Status Indicator -->
      <div v-if="dataLoadedFromStorage" class="cache-indicator">
        <i class="pi pi-info-circle"></i>
        <span>Données restaurées depuis le cache du navigateur</span>
        <Button 
          type="button" 
          icon="pi pi-times" 
          class="p-button-text p-button-sm"
          @click="dataLoadedFromStorage = false"
        />
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
          type="button" 
          label="Vider le cache" 
          icon="pi pi-trash" 
          @click="clearStorageAndReset"
          class="p-button-outlined p-button-warning"
        />
        <Button 
          type="submit" 
          label="Sauvegarder" 
          icon="pi pi-save" 
          :loading="saving"
          :class="hasRequiredFieldsCompleted ? 'p-button-success' : 'p-button-info'"
        />
      </div>
    </form>
    
    <!-- Debug info (only in development) -->
    <div v-if="showDebug" class="debug-section">
      <details>
        <summary>Debug Info</summary>
        <div class="debug-content">
          <h5>Storage Info:</h5>
          <div class="debug-storage">
            <p><strong>Storage Key:</strong> {{ props.storageKey }}</p>
            <p><strong>Data Loaded from Cache:</strong> {{ dataLoadedFromStorage ? 'Yes' : 'No' }}</p>
          </div>
          
          <h5>Current Form Data:</h5>
          <pre>{{ JSON.stringify(localFormData, null, 2) }}</pre>
          
          <h5>Form Structure:</h5>
          <pre>{{ JSON.stringify(props.formStructure, null, 2) }}</pre>
          
          <h5>Field Errors:</h5>
          <pre>{{ JSON.stringify(fieldErrors, null, 2) }}</pre>
          
          <h5>Radio Button States:</h5>
          <div v-for="field in formFields.filter(f => f.type === 'radio')" :key="field.name">
            <strong>{{ field.name }}:</strong> {{ localFormData[field.name] }}
          </div>
          
          <h5>Checkbox States:</h5>
          <div v-for="field in formFields.filter(f => f.type === 'checkbox')" :key="field.name">
            <strong>{{ field.name }}:</strong> {{ localFormData[field.name] }}
          </div>
          
          <h5>Form Validation:</h5>
          <div class="debug-validation">
            <p><strong>Form Always Valid:</strong> {{ isFormValid }}</p>
            <p><strong>Required Fields Completed:</strong> {{ hasRequiredFieldsCompleted }}</p>
          </div>
        </div>
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
  },
  storageKey: {
    type: String,
    default: 'dynamic-form-data'
  }
});

// Emits
const emit = defineEmits(['form-change', 'form-save', 'file-select']);

// Reactive data
const localFormData = ref({});
const fieldErrors = ref({});
const saving = ref(false);
const dataLoadedFromStorage = ref(false);

// Form configuration - handle different structure formats
const formConfig = computed(() => {
  const structure = props.formStructure;
  
  // If it's already a well-formed config
  if (structure && (structure.formTitle || structure.formDescription || structure.fields)) {
    return structure;
  }
  
  // If it's just an array of fields
  if (Array.isArray(structure)) {
    return {
      formTitle: '',
      formDescription: '',
      fields: structure,
      sections: []
    };
  }
  
  // Default empty structure
  return {
    formTitle: '',
    formDescription: '',
    fields: [],
    sections: []
  };
});

// Form fields
const formFields = computed(() => {
  const config = formConfig.value;
  
  if (config && config.fields && Array.isArray(config.fields)) {
    return config.fields;
  }
  
  return [];
});

// Form sections
const formSections = computed(() => {
  const config = formConfig.value;
  
  if (config && config.sections && Array.isArray(config.sections) && config.sections.length > 0) {
    return config.sections;
  }
  
  return [];
});

const isFormValid = computed(() => {
  // Always allow saving, but check if required fields are filled for visual feedback
  return true; // Allow saving at any time
});

const hasRequiredFieldsCompleted = computed(() => {
  // Check if all required fields are filled for button styling
  const requiredFields = formFields.value.filter(field => field.required);
  return requiredFields.every(field => {
    const value = localFormData.value[field.name];
    if (value === null || value === undefined || value === '') {
      return false;
    }
    // For checkboxes, check if they're true when required
    if (field.type === 'checkbox' && field.required) {
      return value === true;
    }
    return true;
  });
});

// Watchers
watch(() => props.formData, (newData) => {
  if (newData && typeof newData === 'object' && !dataLoadedFromStorage.value) {
    // Only use props.formData if we haven't loaded from storage
    localFormData.value = { ...newData };
  }
}, { deep: true, immediate: true });

watch(localFormData, (newData, oldData) => {
  // Handle field changes and validation
  if (oldData && Object.keys(oldData).length > 0) {
    Object.keys(newData).forEach(fieldName => {
      if (newData[fieldName] !== oldData[fieldName]) {
        // Clear field error when value changes
        if (fieldErrors.value[fieldName]) {
          delete fieldErrors.value[fieldName];
        }
        
        // Validate field
        validateField(fieldName, newData[fieldName]);
      }
    });
  }
  
  // Save to localStorage whenever data changes (but only if we have actual data)
  if (Object.keys(newData).length > 0) {
    saveFormDataToStorage();
  }
  
  emit('form-change', newData);
}, { deep: true });

// Lifecycle
onMounted(() => {
  loadFormDataFromStorage();
  initializeFormData();
});

// Methods
function loadFormDataFromStorage() {
  try {
    const savedData = localStorage.getItem(props.storageKey);
    if (savedData) {
      const parsedData = JSON.parse(savedData);
      // Only load data if it's for the same form structure
      if (parsedData && typeof parsedData === 'object') {
        localFormData.value = { ...localFormData.value, ...parsedData };
        dataLoadedFromStorage.value = true;
      }
    }
  } catch (error) {
    console.warn('Failed to load form data from storage:', error);
  }
}

function saveFormDataToStorage() {
  try {
    localStorage.setItem(props.storageKey, JSON.stringify(localFormData.value));
  } catch (error) {
    console.warn('Failed to save form data to storage:', error);
  }
}

function clearFormDataFromStorage() {
  try {
    localStorage.removeItem(props.storageKey);
  } catch (error) {
    console.warn('Failed to clear form data from storage:', error);
  }
}

function initializeFormData() {
  // Initialize form data with default values
  const initialData = { ...props.formData };
  
  formFields.value.forEach(field => {
    if (!(field.name in initialData)) {
      // Explicitly handle checkbox initialization
      if (field.type === 'checkbox') {
        initialData[field.name] = field.default !== undefined ? field.default : false;
      } else {
        initialData[field.name] = getDefaultValue(field);
      }
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
      return field.default !== undefined ? field.default : null;
    case 'checkbox':
      return field.default !== undefined ? field.default : false;
    case 'select':
    case 'radio':
      return field.default !== undefined ? field.default : null;
    case 'date':
      return field.default ? new Date(field.default) : null;
    default:
      return field.default !== undefined ? field.default : null;
  }
}

function getFieldByName(fieldName) {
  return formFields.value.find(field => field.name === fieldName);
}

function onFieldChange(fieldName, value) {
  // Ensure we're setting the right value
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
  if (field.required) {
    if (field.type === 'checkbox') {
      if (!value) {
        errors.push(`${field.label} est requis`);
      }
    } else if (value === null || value === undefined || value === '') {
      errors.push(`${field.label} est requis`);
    }
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
  
  // Custom validation from form config
  const validationRules = formConfig.value.validationRules;
  if (validationRules && validationRules[fieldName]) {
    const rule = validationRules[fieldName];
    
    // Relative validation (e.g., field A must be <= field B)
    if (rule.maxRelativeTo) {
      const relativeValue = localFormData.value[rule.maxRelativeTo];
      if (relativeValue && value && parseFloat(value) > parseFloat(relativeValue)) {
        errors.push(rule.message || `Doit être inférieur ou égal à ${rule.maxRelativeTo}`);
      }
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
  console.log('Form submitted with data:', localFormData.value);
  
  // Always allow saving - validation is optional for draft saving
  saving.value = true;
  
  // Emit save event with current form data
  emit('form-save', { ...localFormData.value });
  
  // Reset saving state after a delay
  setTimeout(() => {
    saving.value = false;
  }, 1000);
}

function resetForm() {
  localFormData.value = {};
  fieldErrors.value = {};
  initializeFormData();
}

function clearStorageAndReset() {
  clearFormDataFromStorage();
  resetForm();
  dataLoadedFromStorage.value = false;
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
  gap: 2rem;
}

/* Form Header */
.form-header {
  text-align: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-color);
}

.form-title {
  color: var(--primary-color);
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
}

.form-description {
  color: var(--text-color-secondary);
  font-size: 0.9rem;
  margin: 0;
  line-height: 1.5;
}

/* Form Sections */
.form-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.form-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid var(--border-color);
}

.section-title {
  color: var(--primary-color);
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-color);
  display: inline-block;
}

.section-fields {
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
  font-style: italic;
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

/* Cache Indicator */
.cache-indicator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #e3f2fd;
  border: 1px solid #2196f3;
  border-radius: 6px;
  color: #1976d2;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.cache-indicator i {
  color: #2196f3;
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

.debug-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.debug-content h5 {
  margin: 0;
  color: var(--primary-color);
  font-size: 0.9rem;
}

.debug-storage,
.debug-validation {
  background: #f0f0f0;
  padding: 0.5rem;
  border-radius: 4px;
  margin-bottom: 0.5rem;
}

.debug-storage p,
.debug-validation p {
  margin: 0.25rem 0;
  font-size: 0.8rem;
}

.debug-section pre {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 1rem;
  font-size: 0.8rem;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .form-actions {
    flex-direction: column;
  }
  
  .radio-group {
    gap: 0.5rem;
  }
  
  .form-section {
    padding: 1rem;
  }
}

/* Dark Mode */
.dark-mode .cache-indicator {
  background: #1e3a8a;
  border-color: #3b82f6;
  color: #93c5fd;
}

.dark-mode .cache-indicator i {
  color: #60a5fa;
}

.dark-mode .form-section {
  background: #2d3748;
  border-color: #4a5568;
}

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

.dark-mode .debug-content h5 {
  color: #81c784;
}

.dark-mode .debug-storage,
.dark-mode .debug-validation {
  background: #374151;
}

.dark-mode .debug-section pre {
  background: #1a202c;
  border-color: #4a5568;
  color: #e2e8f0;
}
</style>