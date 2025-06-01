<template>
  <div class="profile-view">
    <Toast />
    <ConfirmDialog />

    <div class="page-header">
      <h1 class="page-title">Mon Profil</h1>
    </div>

    <div class="content-container">
      <!-- User Information Card -->
      <Card class="user-info-card">
        <template #title>
          <div class="card-title">
            <i class="pi pi-user" style="font-size: 1.5rem; margin-right: 0.5rem"></i>
            Informations Personnelles
          </div>
        </template>
        <template #content>
          <div class="user-details">
            <div class="avatar-container">
              <div class="avatar">
                <span>{{ getInitials() }}</span>
              </div>
              <div class="user-role">
                <Tag :value="formatRole(user.role)" severity="info" />
              </div>
            </div>
            
            <div class="user-info-grid">
              <div class="info-group">
                <label>Nom</label>
                <div class="info-value">{{ user.nom || (userProfile ? userProfile.nom : '-') }}</div>
              </div>
              
              <div class="info-group">
                <label>Prénom</label>
                <div class="info-value">{{ user.prenom || (userProfile ? userProfile.prenom : '-') }}</div>
              </div>
              
              <div class="info-group">
                <label>Email</label>
                <div class="info-value">{{ user.email || (userProfile ? userProfile.email : '-') }}</div>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Password Change Card -->
      <Card class="password-card">
        <template #title>
          <div class="card-title">
            <i class="pi pi-key" style="font-size: 1.5rem; margin-right: 0.5rem"></i>
            Changer le Mot de Passe
          </div>
        </template>
        <template #content>
          <form @submit.prevent="changePassword" class="password-form">
            <div class="form-field">
              <label for="oldPassword">Mot de passe actuel</label>
              <Password 
                id="oldPassword" 
                v-model="passwordForm.oldPassword" 
                :feedback="false" 
                toggleMask 
                class="w-full"
                :class="{'p-invalid': errors.oldPassword}"
              />
              <small v-if="errors.oldPassword" class="p-error">
                {{ errors.oldPassword }}
              </small>
            </div>
            
            <div class="form-field">
              <label for="newPassword">Nouveau mot de passe</label>
              <Password 
                id="newPassword" 
                v-model="passwordForm.newPassword" 
                :feedback="true" 
                toggleMask 
                class="w-full"
                :class="{'p-invalid': errors.newPassword}"
                @input="checkPasswordStrength"
              />
              <small v-if="errors.newPassword" class="p-error">
                {{ errors.newPassword }}
              </small>
            </div>
            
            <div class="form-field">
              <label for="confirmPassword">Confirmer le nouveau mot de passe</label>
              <Password 
                id="confirmPassword" 
                v-model="passwordForm.confirmPassword" 
                :feedback="false" 
                toggleMask 
                class="w-full"
                :class="{'p-invalid': errors.confirmPassword}"
                @input="checkPasswordsMatch"
              />
              <small v-if="errors.confirmPassword" class="p-error">
                {{ errors.confirmPassword }}
              </small>
            </div>
            
            <div class="password-requirements">
              <h4>Le mot de passe doit contenir:</h4>
              <ul>
                <li :class="{ 'requirement-met': passwordStrength.length }">Au moins 8 caractères</li>
                <li :class="{ 'requirement-met': passwordStrength.uppercase }">Au moins une majuscule</li>
                <li :class="{ 'requirement-met': passwordStrength.lowercase }">Au moins une minuscule</li>
                <li :class="{ 'requirement-met': passwordStrength.number }">Au moins un chiffre</li>
                <li :class="{ 'requirement-met': passwordStrength.special }">Au moins un caractère spécial</li>
              </ul>
            </div>
            
            <div class="form-actions">
              <Button 
                type="submit" 
                label="Changer le mot de passe" 
                icon="pi pi-check" 
                :loading="submitting"
              />
            </div>
          </form>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// PrimeVue Components
import Card from 'primevue/card';
import Button from 'primevue/button';
import Password from 'primevue/password';
import Toast from 'primevue/toast';
import ConfirmDialog from 'primevue/confirmdialog';
import Tag from 'primevue/tag';

// UI Services
const toast = useToast();
const confirm = useConfirm();

// User Information
const user = ref(AuthService.getCurrentUser() || {});
const userProfile = ref(null);
const loadingProfile = ref(false);

// Password Form State
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// Validation errors
const errors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// Password strength checks
const passwordStrength = computed(() => {
  const password = passwordForm.newPassword;
  return {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[^A-Za-z0-9]/.test(password)
  };
});

// Check if new password is strong enough
function checkPasswordStrength() {
  const password = passwordForm.newPassword;
  
  // Reset error
  errors.newPassword = '';
  
  // Check length
  if (!password) {
    errors.newPassword = 'Veuillez entrer un nouveau mot de passe';
    return false;
  }
  
  if (password.length < 8) {
    errors.newPassword = 'Le mot de passe doit contenir au moins 8 caractères';
    return false;
  }
  
  // Check if different from old password
  if (password === passwordForm.oldPassword) {
    errors.newPassword = 'Le nouveau mot de passe doit être différent de l\'ancien';
    return false;
  }
  
  // Check complexity
  if (!/[A-Z]/.test(password)) {
    errors.newPassword = 'Le mot de passe doit contenir au moins une majuscule';
    return false;
  }
  
  if (!/[a-z]/.test(password)) {
    errors.newPassword = 'Le mot de passe doit contenir au moins une minuscule';
    return false;
  }
  
  if (!/[0-9]/.test(password)) {
    errors.newPassword = 'Le mot de passe doit contenir au moins un chiffre';
    return false;
  }
  
  if (!/[^A-Za-z0-9]/.test(password)) {
    errors.newPassword = 'Le mot de passe doit contenir au moins un caractère spécial';
    return false;
  }
  
  // If we get here, password is valid
  return true;
}

// Check if passwords match
function checkPasswordsMatch() {
  errors.confirmPassword = '';
  
  if (!passwordForm.confirmPassword) {
    errors.confirmPassword = 'Veuillez confirmer votre nouveau mot de passe';
    return false;
  }
  
  if (passwordForm.confirmPassword !== passwordForm.newPassword) {
    errors.confirmPassword = 'Les mots de passe ne correspondent pas';
    return false;
  }
  
  return true;
}

// Form Submission State
const submitting = ref(false);

// Get user initials for avatar
function getInitials() {
  if (!user.value) return '?';
  
  const prenom = user.value.prenom || '';
  const nom = user.value.nom || '';
  
  if (!prenom && !nom) return '?';
  
  return (prenom.charAt(0) + nom.charAt(0)).toUpperCase();
}

// Format role for display
function formatRole(role) {
  if (!role) return 'Utilisateur';
  
  // Map roles to French display names
  const roleMapping = {
 'AGENT_ANTENNE': 'Agent Antenne',
    'AGENT_GUC': 'Agent GUC',
    'AGENT_COMMISSION_TERRAIN': 'Agent Commission Terrain',
    'ADMIN': 'Administrateur'  };
  
  return roleMapping[role] || role;
}

// Handle password change
async function changePassword() {
  // Reset errors
  errors.oldPassword = '';
  errors.newPassword = '';
  errors.confirmPassword = '';
  
  // Check old password
  if (!passwordForm.oldPassword) {
    errors.oldPassword = 'Veuillez entrer votre mot de passe actuel';
    return;
  }
  
  // Validate new password
  if (!checkPasswordStrength()) {
    return;
  }
  
  // Validate password confirmation
  if (!checkPasswordsMatch()) {
    return;
  }

  // Confirm before submitting
  confirm.require({
    message: 'Êtes-vous sûr de vouloir changer votre mot de passe?',
    header: 'Confirmation',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      submitting.value = true;
      
      try {
        // Create payload
        const payload = {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        };
        
        // Make API call to change password
        await ApiService.post('/auth/change-password', payload);
        
        // Show success message
        toast.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Votre mot de passe a été modifié avec succès',
          life: 3000
        });
        
        // Reset form
        passwordForm.oldPassword = '';
        passwordForm.newPassword = '';
        passwordForm.confirmPassword = '';
        // Clear any errors
        errors.oldPassword = '';
        errors.newPassword = '';
        errors.confirmPassword = '';
      } catch (error) {
        // Handle errors
        let errorMessage = 'Une erreur s\'est produite lors du changement de mot de passe';
        
        if (error.message) {
          errorMessage = error.message;
        } else if (error.response && error.response.data && error.response.data.message) {
          errorMessage = error.response.data.message;
        }
        
        toast.add({
          severity: 'error',
          summary: 'Erreur',
          detail: errorMessage,
          life: 5000
        });
      } finally {
        submitting.value = false;
      }
    }
  });
}
</script>

<style scoped>
.profile-view {
  width: 65%;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 600;
  color: var(--primary-color);
  margin: 0;
}

.content-container {
  display: grid;
  grid-template-columns: 1fr; 
  gap: 2rem;
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 1.2rem;
  font-weight: 600;
}

/* User Info Card Styles */
.user-details {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 2.5rem;
  font-weight: bold;
}

.user-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-group label {
  font-weight: 600;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.info-value {
  font-size: 1rem;
  word-break: break-word;
}

/* Password Form Styles */
.password-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 600;
}

.password-requirements {
  margin-top: 0.5rem;
  border: 1px solid var(--surface-border);
  border-radius: 4px;
  padding: 1rem;
  background-color: var(--surface-ground);
}

.password-requirements h4 {
  margin: 0 0 0.5rem 0;
  font-size: 0.95rem;
  color: var(--text-color-secondary);
}

.password-requirements ul {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.password-requirements li {
  display: flex;
  align-items: center;
  padding: 0.25rem 0;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.password-requirements li::before {
  content: "✗";
  margin-right: 0.5rem;
  color: var(--red-500);
}

.password-requirements li.requirement-met {
  color: var(--text-color);
}

.password-requirements li.requirement-met::before {
  content: "✓";
  color: var(--green-500);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

/* Responsive Styles */
@media (max-width: 992px) {
  .content-container {
    grid-template-columns: 1fr;
  }
  
  .user-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
