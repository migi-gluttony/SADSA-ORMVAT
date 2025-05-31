<template>
  <div class="auth-container">
    <div class="auth-left">
      <div class="auth-left-content">
        <div class="brand-logo">
          <router-link to="/" class="logo-link">
            <img src="@/assets/logo/sadsa.svg" alt="Logo sadsa" class="logo-img" />
          </router-link>
          <div class="brand-text">
            <h1>SADSA</h1>
            <p>Système Automatisé de Demande de Subventions Agricoles</p>
          </div>
        </div>
        <div class="auth-illustration">
          <img src="@/assets/illustrations/Security On-amico.svg" alt="Agriculture et sécurité" />
        </div>
      
      </div>
    </div>
    
    <div class="auth-right">
      <div class="auth-card">
        <div class="auth-header">
          <h2>Bienvenue sur SADSA</h2>
          <p>Connectez-vous à votre espace de gestion des subventions agricoles</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="auth-form">
          <div class="form-group">
            <label for="email" class="form-label">
              <i class="pi pi-envelope"></i>
              Adresse e-mail
            </label>
            <InputText 
              id="email" 
              v-model="email" 
              type="email" 
              class="form-input"
              :class="{ 'p-invalid': validationErrors.email }" 
              aria-describedby="email-error"
              placeholder="votrenom@ormvat.ma"
              required    
            />
            <small id="email-error" class="p-error form-error">{{ validationErrors.email }}</small>
          </div>

          <div class="form-group">
            <label for="password" class="form-label">
              <i class="pi pi-lock"></i>
              Mot de passe
            </label>
            <Password 
              id="password" 
              v-model="password" 
              :feedback="false"
              toggleMask
              class="form-input password-input" 
              :class="{ 'p-invalid': validationErrors.password }"
              aria-describedby="password-error"
              placeholder="Entrez votre mot de passe"
              required
            />
            <small id="password-error" class="p-error form-error">{{ validationErrors.password }}</small>
          </div>

          <div class="form-options">
            <div class="remember-me">
              <Checkbox 
                v-model="rememberMe" 
                :binary="true" 
                inputId="rememberMe"
                class="custom-checkbox"
              />
              <label for="rememberMe" class="remember-label">Se souvenir de moi</label>
            </div>
            
          </div>

          <Button 
            type="submit" 
            :label="loading ? 'Connexion...' : 'Se connecter'" 
            icon="pi pi-sign-in" 
            class="submit-button" 
            :loading="loading"
            :disabled="loading"
          />
        </form>
        
        <div v-if="errorMessage" class="error-message">
          <i class="pi pi-exclamation-circle"></i>
          {{ errorMessage }}
        </div>

        
      </div>
    </div>
    
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import Checkbox from 'primevue/checkbox';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import Button from 'primevue/button';
import Toast from 'primevue/toast';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const email = ref('');
const password = ref('');
const rememberMe = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const validationErrors = ref({
  email: '',
  password: ''
});

onMounted(() => {
  if (AuthService.isAuthenticated()) {
    router.push('/dashboard');
  }
});

const validateForm = () => {
  let isValid = true;
  validationErrors.value = {
    email: '',
    password: ''
  };

  if (!email.value) {
    validationErrors.value.email = 'L\'adresse e-mail est requise';
    isValid = false;
  } else if (!/^\S+@\S+\.\S+$/.test(email.value)) {
    validationErrors.value.email = 'Veuillez entrer une adresse e-mail valide';
    isValid = false;
  }

  if (!password.value) {
    validationErrors.value.password = 'Le mot de passe est requis';
    isValid = false;
  }

  return isValid;
};

const handleLogin = async () => {
  if (!validateForm()) {
    return;
  }

  try {
    loading.value = true;
    errorMessage.value = '';

    await AuthService.login(email.value, password.value, rememberMe.value);
    
    toast.add({
      severity: 'success',
      summary: 'Connecté',
      detail: 'Bienvenue dans SADSA',
      life: 3000
    });
    
    const redirectTo = route.query.redirect || '/dashboard';
    router.push(redirectTo);
    
  } catch (error) {
    console.error('Login error:', error);
    
    if (error.message) {
      errorMessage.value = error.message;
    } else {
      errorMessage.value = 'Une erreur inattendue s\'est produite. Veuillez réessayer.';
    }
    
    toast.add({
      severity: 'error',
      summary: 'Échec de connexion',
      detail: errorMessage.value,
      life: 5000
    });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
:root {
  --primary-color: #01723e;
  --primary-color-rgb: 1, 114, 62;
  --secondary-color: #efb157;
  --accent-color: #2e7145;
  --text-color: #213547;
  --background-color: #ffffff;
}

.dark-mode {
  --primary-color: #01864a;
  --primary-color-rgb: 1, 134, 74;
  --secondary-color: #d7983f;
  --accent-color: #3a8757;
  --text-color: #e5e7eb;
  --background-color: #1e1e1e;
}

.auth-container {
  display: flex;
  min-height: 100vh;
  background-color: var(--background-color);
}

.auth-left {
  flex: 1;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 190%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative; 
  overflow: hidden;
}

.auth-left::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.08'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.auth-left-content {
  text-align: center;
  color: white;
  z-index: 1;
  padding: 2rem;
  max-width: 500px;
}

.brand-logo {
  margin-bottom: 2rem;
}

.logo-link {
  text-decoration: none;
}

.logo-img {
  height: 120px;
  margin-bottom: 1rem;
}

.brand-text h1 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 3.5rem;
  margin: 0 0 0.5rem 0;
  letter-spacing: 2px;
}

.brand-text p {
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
  line-height: 1.4;
}

.auth-illustration {
  margin: 2rem 0;
}

.auth-illustration img {
  max-width: 85%;
  width: 400px;
  filter: drop-shadow(0 10px 25px rgba(0,0,0,0.2));
}

.auth-quote {
  margin-top: 2rem;
}

.auth-quote blockquote {
  font-style: italic;
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
  padding: 1rem;
  border-left: 3px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0 5px 5px 0;
  line-height: 1.5;
}

.auth-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background-color: var(--background-color);
}

.auth-card {
  width: 100%;
  max-width: 450px;
  background: var(--background-color);
  border-radius: 5px;
  padding: 3rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(var(--primary-color-rgb), 0.1);
}

.auth-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.auth-header h2 {
  font-family: 'Poppins', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.auth-header p {
  color: var(--text-color-secondary);
  font-size: 1rem;
  opacity: 0.8;
  line-height: 1.4;
}

.auth-form {
  width: 100%;
}

.form-group {
  margin-bottom: 1.75rem;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
}

.form-label i {
  color: var(--primary-color);
  font-size: 1rem;
}

.form-input {
  width: 100%;
  padding: 0.875rem 1rem;
  border-radius: 5px;
  border: 2px solid var(--surface-border);
  background: var(--surface-ground);
  transition: all 0.2s ease;
}

.form-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
}

.form-error {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.85rem;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.remember-label {
  color: var(--text-color-secondary);
  font-size: 0.9rem;
  cursor: pointer;
}

.forgot-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}
.custom-checkbox{
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  border-radius: 5px;
}
.forgot-link:hover {
  text-decoration: underline;
}

.submit-button {
  width: 100%;
  padding: 0.875rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 5px;
  background: var(--primary-color);
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(var(--primary-color-rgb), 0.3);
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(var(--primary-color-rgb), 0.4);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: rgba(244, 67, 54, 0.1);
  border-radius: 5px;
  color: #f44336;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.auth-footer {
  margin-top: 2rem;
  text-align: center;
  padding-top: 2rem;
  border-top: 1px solid var(--surface-border);
}

.auth-footer p {
  color: var(--text-color-secondary);
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.contact-text {
  color: var(--primary-color);
  font-weight: 500;
}

.admin-contact {
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(var(--primary-color-rgb), 0.05);
  border-radius: 5px;
}

.admin-contact small {
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

/* Password input styling */
:deep(.p-password-input) {
  width: 100%;
  padding: 0.875rem 1rem;
  border-radius: 5px;
  border: 2px solid var(--surface-border);
  background: var(--surface-ground);
  transition: all 0.2s ease;
}

:deep(.p-password-input:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
}

:deep(.p-password .p-icon) {
  color: var(--text-color-secondary);
}

/* Checkbox styling */
:deep(.p-checkbox .p-checkbox-box) {
  border-radius: 5px;
  border: 2px solid var(--surface-border);
}

:deep(.p-checkbox .p-checkbox-box.p-highlight) {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

/* Dark mode adjustments */
.dark-mode .auth-card {
  background-color: #252424;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.dark-mode .auth-footer {
  border-top-color: #333;
}

/* Responsive design */
@media (max-width: 992px) {
  .auth-left {
    display: none;
  }
  
  .auth-right {
    flex: 1;
  }
  
  .auth-card {
    max-width: 100%;
  }
}

@media (max-width: 576px) {
  .auth-card {
    padding: 2rem 1.5rem;
  }
  
  .auth-header h2 {
    font-size: 1.75rem;
  }
  
  .form-options {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-card {
  animation: fadeIn 0.6s ease both;
}

.auth-left-content {
  animation: fadeIn 0.8s ease both;
  animation-delay: 0.2s;
}
</style>