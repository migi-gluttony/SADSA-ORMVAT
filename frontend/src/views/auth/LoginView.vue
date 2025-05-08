<template>
  <div class="login-container">
    <div class="component-card login-form">
      <div class="logo-container">
        <img src="@/assets/logo.svg" alt="ORMVAT Logo" class="logo" />
      </div>
      <h1 class="text-center">Connexion SADSA</h1>
      <h3 class="text-center subtitle">Système Automatisé de Demande de Subventions Agricoles</h3>
      
      <div class="form-wrapper">
        <form @submit.prevent="handleLogin">
          <div class="form-group mb-3">
            <label for="email">Adresse e-mail</label>
            <input 
              id="email" 
              v-model="email" 
              type="email" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.email }" 
              aria-describedby="email-error"
              placeholder="Entrez votre adresse e-mail"
              required    
            />
            <small id="email-error" class="p-error">{{ validationErrors.email }}</small>
          </div>

          <div class="form-group mb-3">
            <div class="flex justify-content-between">
              <label for="password">Mot de passe</label>
            </div>
            <input 
              id="password" 
              v-model="password" 
              type="password"
              class="p-inputtext p-component w-full" 
              :class="{ 'p-invalid': validationErrors.password }"
              aria-describedby="password-error"
              placeholder="Entrez votre mot de passe"
              required
            />
            <small id="password-error" class="p-error">{{ validationErrors.password }}</small>
          </div>

          <div class="form-group mb-3">
            <div class="p-field-checkbox">
                            <label for="rememberMe" class="ml-2">Rester connecté</label>

              <input 
                id="rememberMe" 
                v-model="rememberMe" 
                type="checkbox"
                class="p-checkbox-box"
              />
            </div>
          </div>

          <button 
            type="submit" 
            class="w-full login-button p-button p-component"
            :disabled="loading"
          >
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>Se connecter</span>
          </button>
        </form>
        
        <div v-if="errorMessage" class="error-message mt-3">
          {{ errorMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import AuthService from '@/services/AuthService';

export default defineComponent({
  name: 'LoginView',
  data() {
    return {
      email: '',
      password: '',
      rememberMe: false,
      loading: false,
      errorMessage: '',
      validationErrors: {
        email: '',
        password: ''
      }
    };
  },
  mounted() {
    // If already logged in, redirect to dashboard
    if (AuthService.isAuthenticated()) {
      this.$router.push('/dashboard');
    }
  },
  methods: {
    validateForm() {
      let isValid = true;
      this.validationErrors = {
        email: '',
        password: ''
      };

      // Email validation
      if (!this.email) {
        this.validationErrors.email = 'L\'adresse e-mail est requise';
        isValid = false;
      } else if (!/^\S+@\S+\.\S+$/.test(this.email)) {
        this.validationErrors.email = 'Veuillez entrer une adresse e-mail valide';
        isValid = false;
      }

      // Password validation
      if (!this.password) {
        this.validationErrors.password = 'Le mot de passe est requis';
        isValid = false;
      }

      return isValid;
    },
    async handleLogin() {
      if (!this.validateForm()) {
        return;
      }

      try {
        this.loading = true;
        this.errorMessage = '';

        // Call the AuthService login method
        await AuthService.login(this.email, this.password, this.rememberMe);
        
        // Redirect to the dashboard or the intended route
        const redirectTo = this.$route.query.redirect || '/dashboard';
        this.$router.push(redirectTo);
        
      } catch (error) {
        console.error('Login error:', error);
        
        // Handle specific error messages from the backend
        if (error.message) {
          this.errorMessage = error.message;
        } else {
          this.errorMessage = 'Une erreur inattendue s\'est produite. Veuillez réessayer.';
        }
      } finally {
        this.loading = false;
      }
    }
  }
});
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--background-color);
  background-image: linear-gradient(135deg, rgba(46, 113, 69, 0.1) 0%, rgba(1, 114, 62, 0.1) 100%);
}

.login-form {
  max-width: 480px;
  width: 100%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.5s ease-out;
  background-color: var(--background-color);
}

.logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.logo {
  max-width: 150px;
  height: auto;
}

h1 {
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

.subtitle {
  color: var(--text-color);
  opacity: 0.8;
  font-weight: normal;
  font-size: 1rem;
  margin-bottom: 2rem;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.form-wrapper {
  padding: 1.5rem 1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--text-color);
}

.login-button {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  transition: background-color 0.3s, border-color 0.3s;
  color: white;
  border: none;
  padding: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
}

.login-button:hover {
  background-color: var(--accent-color);
  border-color: var(--accent-color);
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  text-align: center;
  padding: 0.75rem;
  border-radius: 0.25rem;
  background-color: rgba(244, 67, 54, 0.1);
  color: #f44336;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #ffffff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.dark-mode .login-container {
  background-image: linear-gradient(135deg, rgba(58, 135, 87, 0.1) 0%, rgba(1, 134, 74, 0.1) 100%);
}

input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background-color: var(--background-color);
  color: var(--text-color);
}

input:focus {
  outline: none;
  border-color: var(--primary-color);
}

input.p-invalid {
  border-color: #f44336;
}

.p-error {
  color: #f44336;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}
</style>