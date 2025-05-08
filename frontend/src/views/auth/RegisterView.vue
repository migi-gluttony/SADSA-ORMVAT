<template>
  <div class="register-container">
    <div class="component-card register-form">
      <div class="logo-container">
        <img src="@/assets/logo.svg" alt="ORMVAT Logo" class="logo" />
      </div>
      <h1 class="text-center">Créer un compte SADSA</h1>
      <h3 class="text-center subtitle">Système Automatisé de Demande de Subventions Agricoles</h3>
      
      <div class="form-wrapper">
        <form @submit.prevent="handleRegister">
          <div class="form-group mb-3">
            <label for="prenom">Prénom</label>
            <input 
              id="prenom" 
              v-model="prenom" 
              type="text" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.prenom }" 
              placeholder="Entrez votre prénom"
              required    
            />
            <small class="p-error">{{ validationErrors.prenom }}</small>
          </div>

          <div class="form-group mb-3">
            <label for="nom">Nom</label>
            <input 
              id="nom" 
              v-model="nom" 
              type="text" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.nom }" 
              placeholder="Entrez votre nom"
              required    
            />
            <small class="p-error">{{ validationErrors.nom }}</small>
          </div>

          <div class="form-group mb-3">
            <label for="email">Adresse e-mail</label>
            <input 
              id="email" 
              v-model="email" 
              type="email" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.email }" 
              placeholder="Entrez votre adresse e-mail"
              required    
            />
            <small class="p-error">{{ validationErrors.email }}</small>
          </div>

          <div class="form-group mb-3">
            <label for="telephone">Téléphone (optionnel)</label>
            <input 
              id="telephone" 
              v-model="telephone" 
              type="tel" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.telephone }" 
              placeholder="Entrez votre numéro de téléphone"
            />
            <small class="p-error">{{ validationErrors.telephone }}</small>
          </div>

          <div class="form-group mb-3">
            <label for="password">Mot de passe</label>
            <input 
              id="password" 
              v-model="password" 
              type="password"
              class="p-inputtext p-component w-full" 
              :class="{ 'p-invalid': validationErrors.password }"
              placeholder="Entrez votre mot de passe"
              required
            />
            <small class="p-error">{{ validationErrors.password }}</small>
          </div>

          <div class="form-group mb-3">
            <label for="role">Rôle</label>
            <select 
              id="role" 
              v-model="role" 
              class="p-inputtext p-component w-full"
              :class="{ 'p-invalid': validationErrors.role }"
              required
            >
              <option value="" disabled>Sélectionnez un rôle</option>
              <option value="ADMIN">Administrateur</option>
              <option value="AGENT_ANTENNE">Agent d'Antenne</option>
              <option value="AGENT_GUC">Agent GUC</option>
              <option value="AGENT_COMMISSION">Agent de Commission</option>
            </select>
            <small class="p-error">{{ validationErrors.role }}</small>
          </div>

          <button 
            type="submit" 
            class="w-full register-button p-button p-component"
            :disabled="loading"
          >
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>S'inscrire</span>
          </button>
        </form>
        
        <div v-if="errorMessage" class="error-message mt-3">
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-message mt-3">
          {{ successMessage }}
        </div>

        <div class="mt-3 text-center">
          <a href="/login" class="login-link">Déjà inscrit? Se connecter</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import AuthService from '@/services/AuthService';

export default defineComponent({
  name: 'RegisterView',
  data() {
    return {
      nom: '',
      prenom: '',
      email: '',
      telephone: '',
      password: '',
      role: '',
      loading: false,
      errorMessage: '',
      successMessage: '',
      validationErrors: {
        nom: '',
        prenom: '',
        email: '',
        telephone: '',
        password: '',
        role: ''
      }
    };
  },
  methods: {
    validateForm() {
      let isValid = true;
      this.validationErrors = {
        nom: '',
        prenom: '',
        email: '',
        telephone: '',
        password: '',
        role: ''
      };

      // Nom validation
      if (!this.nom) {
        this.validationErrors.nom = 'Le nom est requis';
        isValid = false;
      }

      // Prénom validation
      if (!this.prenom) {
        this.validationErrors.prenom = 'Le prénom est requis';
        isValid = false;
      }

      // Email validation
      if (!this.email) {
        this.validationErrors.email = 'L\'adresse e-mail est requise';
        isValid = false;
      } else if (!/^\S+@\S+\.\S+$/.test(this.email)) {
        this.validationErrors.email = 'Veuillez entrer une adresse e-mail valide';
        isValid = false;
      }

      // Téléphone validation (optional)
      if (this.telephone && !/^[0-9+\s()-]{8,15}$/.test(this.telephone)) {
        this.validationErrors.telephone = 'Veuillez entrer un numéro de téléphone valide';
        isValid = false;
      }

      // Password validation
      if (!this.password) {
        this.validationErrors.password = 'Le mot de passe est requis';
        isValid = false;
      } else if (this.password.length < 6) {
        this.validationErrors.password = 'Le mot de passe doit contenir au moins 6 caractères';
        isValid = false;
      }

      // Role validation
      if (!this.role) {
        this.validationErrors.role = 'Le rôle est requis';
        isValid = false;
      }

      return isValid;
    },
    async handleRegister() {
      if (!this.validateForm()) {
        return;
      }

      try {
        this.loading = true;
        this.errorMessage = '';
        this.successMessage = '';

        // Prepare registration data
        const registerData = {
          nom: this.nom,
          prenom: this.prenom,
          email: this.email,
          telephone: this.telephone || null,
          motDePasse: this.password,
          role: this.role,
          // cdaId is optional and not included in this simplified form
        };

        // Call the AuthService register method
        await AuthService.register(registerData);
        
        // Show success message
        this.successMessage = 'Compte créé avec succès! Vous pouvez maintenant vous connecter.';
        
        // Reset form
        this.nom = '';
        this.prenom = '';
        this.email = '';
        this.telephone = '';
        this.password = '';
        this.role = '';
        
        // Redirect to login after 2 seconds
        setTimeout(() => {
          this.$router.push('/login');
        }, 2000);
        
      } catch (error) {
        console.error('Registration error:', error);
        
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
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--background-color);
  background-image: linear-gradient(135deg, rgba(46, 113, 69, 0.1) 0%, rgba(1, 114, 62, 0.1) 100%);
  padding: 2rem 0;
}

.register-form {
  max-width: 480px;
  width: 100%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.5s ease-out;
  background-color: var(--background-color);
  margin: 2rem 0;
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

.register-button {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  transition: background-color 0.3s, border-color 0.3s;
  color: white;
  border: none;
  padding: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
}

.register-button:hover {
  background-color: var(--accent-color);
  border-color: var(--accent-color);
}

.register-button:disabled {
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

.success-message {
  text-align: center;
  padding: 0.75rem;
  border-radius: 0.25rem;
  background-color: rgba(76, 175, 80, 0.1);
  color: #4caf50;
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

.login-link {
  color: var(--primary-color);
  text-decoration: none;
}

.login-link:hover {
  text-decoration: underline;
}

input, select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background-color: var(--background-color);
  color: var(--text-color);
}

input:focus, select:focus {
  outline: none;
  border-color: var(--primary-color);
}

input.p-invalid, select.p-invalid {
  border-color: #f44336;
}

.p-error {
  color: #f44336;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

/* Dark mode adjustments */
.dark-mode .register-container {
  background-image: linear-gradient(135deg, rgba(58, 135, 87, 0.1) 0%, rgba(1, 134, 74, 0.1) 100%);
}
</style>