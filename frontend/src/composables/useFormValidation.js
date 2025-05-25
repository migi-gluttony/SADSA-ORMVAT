import { ref, reactive } from 'vue';

export function useFormValidation() {
  const errors = reactive({});
  const isValidating = ref(false);

  function validateField(fieldName, value, rules = {}) {
    const fieldErrors = [];

    // Required validation
    if (rules.required && (!value || (typeof value === 'string' && value.trim() === ''))) {
      fieldErrors.push(`${rules.label || fieldName} est requis`);
    }

    // Type-specific validations
    if (value && fieldErrors.length === 0) {
      // Email validation
      if (rules.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        fieldErrors.push('Format email invalide');
      }

      // Phone validation
      if (rules.type === 'phone' && !/^[0-9+\s()-]{8,15}$/.test(value)) {
        fieldErrors.push('Format téléphone invalide');
      }

      // Number validations
      if (rules.type === 'number') {
        const numValue = parseFloat(value);
        
        if (isNaN(numValue)) {
          fieldErrors.push('Doit être un nombre valide');
        } else {
          if (rules.min !== undefined && numValue < rules.min) {
            fieldErrors.push(`Minimum: ${rules.min}`);
          }
          
          if (rules.max !== undefined && numValue > rules.max) {
            fieldErrors.push(`Maximum: ${rules.max}`);
          }
        }
      }

      // String validations
      if (typeof value === 'string') {
        if (rules.minLength && value.length < rules.minLength) {
          fieldErrors.push(`Minimum ${rules.minLength} caractères`);
        }
        
        if (rules.maxLength && value.length > rules.maxLength) {
          fieldErrors.push(`Maximum ${rules.maxLength} caractères`);
        }
        
        if (rules.pattern && !new RegExp(rules.pattern).test(value)) {
          fieldErrors.push(rules.patternMessage || 'Format invalide');
        }
      }
    }

    // Custom validation
    if (rules.validator && typeof rules.validator === 'function') {
      const customError = rules.validator(value);
      if (customError) {
        fieldErrors.push(customError);
      }
    }

    // Update errors
    if (fieldErrors.length > 0) {
      errors[fieldName] = fieldErrors[0];
    } else {
      delete errors[fieldName];
    }

    return fieldErrors.length === 0;
  }

  function validateForm(formData, validationRules) {
    isValidating.value = true;
    
    // Clear previous errors
    Object.keys(errors).forEach(key => delete errors[key]);
    
    let isValid = true;
    
    // Validate each field
    Object.keys(validationRules).forEach(fieldName => {
      const fieldValue = formData[fieldName];
      const fieldRules = validationRules[fieldName];
      
      if (!validateField(fieldName, fieldValue, fieldRules)) {
        isValid = false;
      }
    });
    
    isValidating.value = false;
    return isValid;
  }

  function clearErrors() {
    Object.keys(errors).forEach(key => delete errors[key]);
  }

  function setError(fieldName, message) {
    errors[fieldName] = message;
  }

  function clearError(fieldName) {
    delete errors[fieldName];
  }

  function hasErrors() {
    return Object.keys(errors).length > 0;
  }

  function getError(fieldName) {
    return errors[fieldName];
  }

  return {
    errors,
    isValidating,
    validateField,
    validateForm,
    clearErrors,
    setError,
    clearError,
    hasErrors,
    getError
  };
}