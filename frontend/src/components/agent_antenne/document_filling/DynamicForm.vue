<template>
  <div class="dynamic-form">
    <form @submit.prevent="handleSubmit">
      
      <!-- Form Header -->
      <div v-if="formConfig.formTitle || formConfig.formDescription" class="mb-4">
        <h4 v-if="formConfig.formTitle" class="mt-0">{{ formConfig.formTitle }}</h4>
        <p v-if="formConfig.formDescription" class="mt-0 text-color-secondary">{{ formConfig.formDescription }}</p>
      </div>
      
      <!-- Form Fields -->
      <div v-if="formFields.length > 0" class="space-y-3">
        <div 
          v-for="field in formFields" 
          :key="field.name"
          class="field"
        >
          <label :for="field.name" class="font-medium text-900">
            {{ field.label }}
            <span v-if="field.required" class="text-red-500 ml-1">*</span>
          </label>
          
          <!-- Text Input -->
          <InputText 
            v-if="field.type === 'text'"
            :id="field.name"
            v-model="localFormData[field.name]"
            :placeholder="field.placeholder"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            class="w-full mt-1"
            @input="onFieldChange(field.name, $event.target.value)"
          />
          
          <!-- Number Input -->
          <InputNumber 
            v-else-if="field.type === 'number'"
            :id="field.name"
            v-model="localFormData[field.name]"
            :placeholder="field.placeholder"
            :min="field.min"
            :max="field.max"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            class="w-full mt-1"
            @input="onFieldChange(field.name, $event.value)"
          />
          
          <!-- Textarea -->
          <Textarea 
            v-else-if="field.type === 'textarea'"
            :id="field.name"
            v-model="localFormData[field.name]"
            :placeholder="field.placeholder"
            :rows="field.rows || 3"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            class="w-full mt-1"
            @input="onFieldChange(field.name, $event.target.value)"
          />
          
          <!-- Select/Dropdown -->
          <Select 
            v-else-if="field.type === 'select'"
            :id="field.name"
            v-model="localFormData[field.name]"
            :options="field.options"
            optionLabel="label"
            optionValue="value"
            :placeholder="field.placeholder || 'Sélectionner...'"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            class="w-full mt-1"
            @change="onFieldChange(field.name, $event.value)"
          />
          
          <!-- Date -->
          <DatePicker 
            v-else-if="field.type === 'date'"
            :id="field.name"
            v-model="localFormData[field.name]"
            :placeholder="field.placeholder"
            dateFormat="dd/mm/yy"
            :class="{ 'p-invalid': fieldErrors[field.name] }"
            class="w-full mt-1"
            @date-select="onFieldChange(field.name, $event)"
          />
          
          <!-- Checkbox -->
          <div v-else-if="field.type === 'checkbox'" class="flex align-items-center mt-2">
            <Checkbox 
              :id="field.name"
              v-model="localFormData[field.name]"
              :binary="true"
              :class="{ 'p-invalid': fieldErrors[field.name] }"
              @change="onFieldChange(field.name, $event.checked)"
            />
            <label :for="field.name" class="ml-2">{{ field.checkboxLabel || field.label }}</label>
          </div>
          
          <!-- Radio Group -->
          <div v-else-if="field.type === 'radio'" class="mt-2">
            <div class="space-y-2">
              <div 
                v-for="option in field.options" 
                :key="option.value"
                class="flex align-items-center"
              >
                <RadioButton 
                  :id="`${field.name}_${option.value}`"
                  v-model="localFormData[field.name]"
                  :value="option.value"
                  @change="onFieldChange(field.name, $event.value)"
                />
                <label :for="`${field.name}_${option.value}`" class="ml-2">
                  {{ option.label }}
                </label>
              </div>
            </div>
          </div>
          
          <!-- Unknown field type -->
          <div v-else class="p-3 surface-100 border-round mt-1">
            <i class="pi pi-question-circle mr-2"></i>
            <span>Type de champ non supporté: {{ field.type }}</span>
          </div>
          
          <!-- Help Text -->
          <small v-if="field.description" class="block mt-1 text-color-secondary">
            {{ field.description }}
          </small>
          
          <!-- Error Message -->
          <small v-if="fieldErrors[field.name]" class="block mt-1 text-red-500">
            {{ fieldErrors[field.name] }}
          </small>
        </div>
      </div>

      <!-- No fields message -->
      <div v-else class="text-center p-4 surface-100 border-round">
        <i class="pi pi-info-circle text-color-secondary mr-2"></i>
        <span class="text-color-secondary">Aucun champ configuré dans ce formulaire</span>
      </div>

      <!-- Form Actions -->
      <div v-if="formFields.length > 0" class="flex justify-content-end gap-2 mt-4 pt-3 border-top-1 surface-border">
        <Button 
          type="button" 
          label="Effacer" 
          icon="pi pi-refresh" 
          @click="resetForm"
          class="p-button-outlined"
          size="small"
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
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';

import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import Checkbox from 'primevue/checkbox';
import RadioButton from 'primevue/radiobutton';
import Button from 'primevue/button';

const props = defineProps({
  formStructure: {
    type: Object,
    default: () => ({})
  },
  formData: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['form-change', 'form-save']);

const localFormData = ref({});
const fieldErrors = ref({});
const saving = ref(false);

// Form configuration
const formConfig = computed(() => {
  const structure = props.formStructure;
  
  if (structure && (structure.formTitle || structure.formDescription || structure.fields)) {
    return structure;
  }
  
  if (Array.isArray(structure)) {
    return {
      formTitle: '',
      formDescription: '',
      fields: structure
    };
  }
  
  return {
    formTitle: '',
    formDescription: '',
    fields: []
  };
});

const formFields = computed(() => {
  const config = formConfig.value;
  return config?.fields || [];
});

const hasRequiredFieldsCompleted = computed(() => {
  const requiredFields = formFields.value.filter(field => field.required);
  return requiredFields.every(field => {
    const value = localFormData.value[field.name];
    if (value === null || value === undefined || value === '') {
      return false;
    }
    if (field.type === 'checkbox' && field.required) {
      return value === true;
    }
    return true;
  });
});

// Watchers
watch(() => props.formData, (newData) => {
  if (newData && typeof newData === 'object') {
    localFormData.value = { ...newData };
  }
}, { deep: true, immediate: true });

watch(localFormData, (newData) => {
  emit('form-change', newData);
}, { deep: true });

onMounted(() => {
  initializeFormData();
});

function initializeFormData() {
  const initialData = { ...props.formData };
  
  formFields.value.forEach(field => {
    if (!(field.name in initialData)) {
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

function onFieldChange(fieldName, value) {
  localFormData.value[fieldName] = value;
  
  if (fieldErrors.value[fieldName]) {
    delete fieldErrors.value[fieldName];
  }
  
  validateField(fieldName, value);
}

function validateField(fieldName, value) {
  const field = formFields.value.find(f => f.name === fieldName);
  if (!field) return;
  
  const errors = [];
  
  if (field.required) {
    if (field.type === 'checkbox') {
      if (!value) {
        errors.push(`${field.label} est requis`);
      }
    } else if (value === null || value === undefined || value === '') {
      errors.push(`${field.label} est requis`);
    }
  }
  
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
        break;
    }
  }
  
  if (errors.length > 0) {
    fieldErrors.value[fieldName] = errors[0];
  } else {
    delete fieldErrors.value[fieldName];
  }
}

function handleSubmit() {
  saving.value = true;
  
  emit('form-save', { ...localFormData.value });
  
  setTimeout(() => {
    saving.value = false;
  }, 1000);
}

function resetForm() {
  localFormData.value = {};
  fieldErrors.value = {};
  initializeFormData();
}
</script>

<style scoped>
.space-y-3 > * + * {
  margin-top: 0.75rem;
}

.space-y-2 > * + * {
  margin-top: 0.5rem;
}

.field {
  display: flex;
  flex-direction: column;
}
</style>

