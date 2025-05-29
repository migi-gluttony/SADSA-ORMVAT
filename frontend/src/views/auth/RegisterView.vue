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
            <p>Office Régional de Mise en Valeur Agricole du Tadla</p>
          </div>
        </div>
        <div class="auth-illustration">
          <img src="@/assets/illustrations/Security On-amico.svg" alt="Équipe agricole" />
        </div>
      </div>
    </div>
    
    <div class="auth-right">
      <div class="auth-card">
        <div class="auth-header">
          <h2>Rejoindre SADSA</h2>
          <p>Créez votre compte pour accéder au système de gestion des subventions agricoles</p>
        </div>
        
        <form @submit.prevent="handleRegister" class="auth-form">
          <div class="form-row">
            <div class="form-group half-width">
              <label for="prenom" class="form-label">
                <i class="pi pi-user"></i>
                Prénom
              </label>
              <InputText 
                id="prenom" 
                v-model="prenom" 
                type="text" 
                class="form-input"
                :class="{ 'p-invalid': validationErrors.prenom }" 
                aria-describedby="prenom-error"
                placeholder="Votre prénom"
                required    
              />
              <small id="prenom-error" class="p-error form-error">{{ validationErrors.prenom }}</small>
            </div>

            <div class="form-group half-width">
              <label for="nom" class="form-label">
                <i class="pi pi-user"></i>
                Nom
              </label>
              <InputText 
                id="nom" 
                v-model="nom" 
                type="text" 
                class="form-input"
                :class="{ 'p-invalid': validationErrors.nom }" 
                aria-describedby="nom-error"
                placeholder="Votre nom"
                required    
              />
              <small id="nom-error" class="p-error form-error">{{ validationErrors.nom }}</small>
            </div>
          </div>

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
            <label for="telephone" class="form-label">
              <i class="pi pi-phone"></i>
              Téléphone <span class="optional">(optionnel)</span>
            </label>
            <InputText 
              id="telephone" 
              v-model="telephone" 
              type="tel" 
              class="form-input"
              :class="{ 'p-invalid': validationErrors.telephone }"
              aria-describedby="telephone-error"
              placeholder="+212 6XX XXX XXX"
            />
            <small id="telephone-error" class="p-error form-error">{{ validationErrors.telephone }}</small>
          </div>

          <div class="form-group">
            <label for="role" class="form-label">
              <i class="pi pi-briefcase"></i>
              Rôle dans l'organisation
            </label>
            <Dropdown 
              id="role" 
              v-model="role" 
              :options="roleOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionnez votre rôle"
              class="form-input dropdown-input"
              :class="{ 'p-invalid': validationErrors.role }"
              aria-describedby="role-error"
              @change="onRoleChange"
            />
            <small id="role-error" class="p-error form-error">{{ validationErrors.role }}</small>
          </div>

          <!-- Antenne Selection - Only show for AGENT_ANTENNE -->
          <div v-if="role === 'AGENT_ANTENNE'" class="form-group">
            <label for="antenne" class="form-label">
              <i class="pi pi-building"></i>
              Antenne ORMVAT
            </label>
            <Dropdown 
              id="antenne" 
              v-model="antenneId" 
              :options="antenneOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionnez votre antenne"
              class="form-input dropdown-input"
              :class="{ 'p-invalid': validationErrors.antenneId }"
              aria-describedby="antenne-error"
            />
            <small id="antenne-error" class="p-error form-error">{{ validationErrors.antenneId }}</small>
            <small class="form-help">
              <i class="pi pi-info-circle"></i>
              L'antenne à laquelle vous êtes rattaché
            </small>
          </div>

          <div class="form-group">
            <label for="password" class="form-label">
              <i class="pi pi-lock"></i>
              Mot de passe
            </label>
            <Password 
              id="password" 
              v-model="password" 
              :feedback="true"
              toggleMask
              class="form-input password-input" 
              :class="{ 'p-invalid': validationErrors.password }"
              aria-describedby="password-error"
              placeholder="Créez un mot de passe sécurisé"
              required
            />
            <small id="password-error" class="p-error form-error">{{ validationErrors.password }}</small>
          </div>

          <div class="form-group">
            <label for="confirmPassword" class="form-label">
              <i class="pi pi-lock"></i>
              Confirmer le mot de passe
            </label>
            <Password 
              id="confirmPassword" 
              v-model="confirmPassword" 
              :feedback="false"
              toggleMask
              class="form-input password-input" 
              :class="{ 'p-invalid': validationErrors.confirmPassword }"
              aria-describedby="confirm-password-error"
              placeholder="Confirmez votre mot de passe"
              required
            />
            <small id="confirm-password-error" class="p-error form-error">{{ validationErrors.confirmPassword }}</small>
          </div>



          <Button 
            type="submit" 
            :label="loading ? 'Création en cours...' : 'Créer mon compte'" 
            icon="pi pi-user-plus" 
            class="submit-button" 
            :loading="loading"
            :disabled="loading"
          />
        </form>
        
        <div v-if="errorMessage" class="error-message">
          <i class="pi pi-exclamation-circle"></i>
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-message">
          <i class="pi pi-check-circle"></i>
          {{ successMessage }}
        </div>

        <div class="auth-footer">
          <p>Déjà inscrit ?</p>
          <router-link to="/login" class="login-link">Se connecter à SADSA</router-link>
          
          <div class="admin-contact">
            <small>
              <i class="pi pi-info-circle"></i>
              Votre compte sera activé par un administrateur après validation
            </small>
          </div>
        </div>
      </div>
    </div>
    
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';
import Checkbox from 'primevue/checkbox';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import Button from 'primevue/button';
import Dropdown from 'primevue/dropdown';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

const nom = ref('');
const prenom = ref('');
const email = ref('');
const telephone = ref('');
const password = ref('');
const confirmPassword = ref('');
const role = ref('');
const antenneId = ref(null);
const loading = ref(false);
const errorMessage = ref('');

const validationErrors = ref({
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
  password: '',
  confirmPassword: '',
  role: '',
  antenneId: ''
});

const roleOptions = ref([
  { label: 'Agent d\'Antenne (CDA)', value: 'AGENT_ANTENNE' },
  { label: 'Agent Guichet Unique Central', value: 'AGENT_GUC' },
  { label: 'Agent de Commission Technique', value: 'AGENT_COMMISSION' },
  { label: 'Commission AHA-AF', value: 'COMMISSION_AHA_AF' },
  { label: 'Administrateur', value: 'ADMIN' }
]);

const antenneOptions = ref([
  { label: 'Antenne Bni Amir - CDA Bni Amir', value: 1 },
  { label: 'Antenne Souk Sebt - CDA Souk Sebt', value: 2 },
  { label: 'Antenne Ouldad M\'Bark - CDA Ouldad M\'Bark', value: 3 },
  { label: 'Antenne Dar Ouled Zidouh - CDA Dar Ouled Zidouh', value: 4 },
  { label: 'Antenne Afourer - CDA Afourer', value: 5 }
]);

onMounted(() => {
  if (AuthService.isAuthenticated()) {
    router.push('/dashboard');
  }
});

const onRoleChange = () => {
  // Reset antenne selection when role changes
  antenneId.value = null;
  validationErrors.value.antenneId = '';
};

const validateForm = () => {
  let isValid = true;
  validationErrors.value = {
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    password: '',
    confirmPassword: '',
    role: '',
    cdaId: ''
  };

  // Nom validation
  if (!nom.value) {
    validationErrors.value.nom = 'Le nom est requis';
    isValid = false;
  }

  // Prénom validation
  if (!prenom.value) {
    validationErrors.value.prenom = 'Le prénom est requis';
    isValid = false;
  }

  // Email validation
  if (!email.value) {
    validationErrors.value.email = 'L\'adresse e-mail est requise';
    isValid = false;
  } else if (!/^\S+@\S+\.\S+$/.test(email.value)) {
    validationErrors.value.email = 'Veuillez entrer une adresse e-mail valide';
    isValid = false;
  }

  // Téléphone validation (optional)
  if (telephone.value && !/^[0-9+\s()-]{8,15}$/.test(telephone.value)) {
    validationErrors.value.telephone = 'Veuillez entrer un numéro de téléphone valide';
    isValid = false;
  }

  // Password validation
  if (!password.value) {
    validationErrors.value.password = 'Le mot de passe est requis';
    isValid = false;
  } else if (password.value.length < 6) {
    validationErrors.value.password = 'Le mot de passe doit contenir au moins 6 caractères';
    isValid = false;
  }

  // Confirm password validation
  if (!confirmPassword.value) {
    validationErrors.value.confirmPassword = 'La confirmation du mot de passe est requise';
    isValid = false;
  } else if (password.value !== confirmPassword.value) {
    validationErrors.value.confirmPassword = 'Les mots de passe ne correspondent pas';
    isValid = false;
  }

  // Role validation
  if (!role.value) {
    validationErrors.value.role = 'Le rôle est requis';
    isValid = false;
  }

  // CDA validation for AGENT_ANTENNE
  if (role.value === 'AGENT_ANTENNE' && !cdaId.value) {
    validationErrors.value.cdaId = 'Le CDA est requis pour les agents d\'antenne';
    isValid = false;
  }

  return isValid;
};

const handleRegister = async () => {
  if (!validateForm()) {
    return;
  }

  try {
    loading.value = true;
    errorMessage.value = '';
    successMessage.value = '';

    // Prepare registration data
    const registerData = {
      nom: nom.value,
      prenom: prenom.value,
      email: email.value,
      telephone: telephone.value || null,
      motDePasse: password.value,
      role: role.value,
      antenneId: role.value === 'AGENT_ANTENNE' ? antenneId.value : null
    };

    // Call the AuthService register method
    await AuthService.register(registerData);
    
    // Show success message
    successMessage.value = 'Compte créé avec succès! Un administrateur activera votre compte prochainement.';
    
    toast.add({
      severity: 'success',
      summary: 'Inscription réussie',
      detail: 'Votre compte a été créé avec succès',
      life: 5000
    });
    
    // Reset form
    nom.value = '';
    prenom.value = '';
    email.value = '';
    telephone.value = '';
    password.value = '';
    confirmPassword.value = '';
    role.value = '';
    antenneId.value = null;
    
    // Redirect to login after 3 seconds
    setTimeout(() => {
      router.push('/login');
    }, 3000);
    
  } catch (error) {
    console.error('Registration error:', error);
    
    // Handle specific error messages from the backend
    if (error.message) {
      errorMessage.value = error.message;
    } else {
      errorMessage.value = 'Une erreur inattendue s\'est produite. Veuillez réessayer.';
    }
    
    toast.add({
      severity: 'error',
      summary: 'Échec de l\'inscription',
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
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 50%, var(--secondary-color) 100%);
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

.auth-right {
  flex: 1.2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background-color: var(--background-color);
  overflow-y: auto;
}

.auth-card {
  width: 100%;
  max-width: 500px;
  background: var(--background-color);
  border-radius: 20px;
  padding: 2.5rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(var(--primary-color-rgb), 0.1);
  margin: 2rem 0;
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.auth-header h2 {
  font-family: 'Poppins', sans-serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.auth-header p {
  color: var(--text-color-secondary);
  font-size: 0.95rem;
  opacity: 0.8;
  line-height: 1.4;
}

.auth-form {
  width: 100%;
}

.form-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group.half-width {
  flex: 1;
  margin-bottom: 0;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}

.form-label i {
  color: var(--primary-color);
  font-size: 0.9rem;
}

.optional {
  font-weight: 400;
  color: var(--text-color-secondary);
  font-size: 0.8rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 2px solid var(--surface-border);
  background: var(--surface-ground);
  transition: all 0.2s ease;
  font-size: 0.9rem;
}

.form-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
}

.dropdown-input {
  padding: 0;
}

.form-error {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.8rem;
}

.form-help {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: var(--text-color-secondary);
  opacity: 0.8;
}

.form-help i {
  color: var(--primary-color);
}

.form-options {
  margin-bottom: 1.5rem;
}

.terms-acceptance {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.terms-label {
  color: var(--text-color-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  line-height: 1.4;
}

.terms-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.terms-link:hover {
  text-decoration: underline;
}

.submit-button {
  width: 100%;
  padding: 0.875rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%);
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
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.error-message {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: rgba(244, 67, 54, 0.1);
  border-radius: 10px;
  color: #f44336;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.success-message {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: rgba(76, 175, 80, 0.1);
  border-radius: 10px;
  color: #4caf50;
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

.login-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.95rem;
}

.login-link:hover {
  text-decoration: underline;
}

.admin-contact {
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(var(--primary-color-rgb), 0.05);
  border-radius: 8px;
}

.admin-contact small {
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  line-height: 1.3;
}

/* Password input styling */
:deep(.p-password-input) {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 2px solid var(--surface-border);
  background: var(--surface-ground);
  transition: all 0.2s ease;
  font-size: 0.9rem;
}

:deep(.p-password-input:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
}

:deep(.p-password .p-icon) {
  color: var(--text-color-secondary);
}

/* Dropdown styling */
:deep(.p-dropdown) {
  width: 100%;
  border: 2px solid var(--surface-border);
  border-radius: 8px;
}

:deep(.p-dropdown:not(.p-disabled).p-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.1);
}

:deep(.p-dropdown .p-dropdown-label) {
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
}

/* Checkbox styling */
:deep(.p-checkbox .p-checkbox-box) {
  border-radius: 6px;
  border: 2px solid var(--surface-border);
  width: 1.2rem;
  height: 1.2rem;
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
@media (max-width: 1200px) {
  .auth-right {
    flex: 1;
  }
}

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

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  
  .form-group.half-width {
    margin-bottom: 1.5rem;
  }
}

@media (max-width: 576px) {
  .auth-card {
    padding: 2rem 1.5rem;
  }
  
  .auth-header h2 {
    font-size: 1.6rem;
  }
  
  .terms-acceptance {
    align-items: flex-start;
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