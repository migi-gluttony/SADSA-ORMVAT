<template>
  <div class="final-approval-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement du dossier...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Erreur</h3>
      <p>{{ error }}</p>
      <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" />
    </div>

    <!-- Main Content -->
    <div v-else-if="dossierDetail" class="approval-content">
      <!-- Header -->
      <div class="approval-header">
        <div class="header-nav">
          <Button 
            label="Retour aux dossiers" 
            icon="pi pi-arrow-left" 
            @click="goBack"
            class="p-button-outlined"
          />
          <div class="breadcrumb">
            <span>Dossiers GUC</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ dossierDetail.dossier.reference }}</span>
            <i class="pi pi-angle-right"></i>
            <span>Décision Finale</span>
          </div>
        </div>
      </div>

      <!-- Status Alert for Already Approved Dossiers -->
      <div v-if="isAlreadyApproved()" class="status-alert mb-4">
        <Message severity="success" :closable="false">
          <template #icon>
            <i class="pi pi-check-circle"></i>
          </template>
          Ce dossier a déjà été approuvé. Vous pouvez consulter la fiche d'approbation.
          <Button 
            label="Voir la Fiche" 
            icon="pi pi-file-text" 
            @click="router.push(`/agent_guc/dossiers/${dossierId}/fiche`)"
            class="p-button-sm p-button-success ml-2"
          />
        </Message>
      </div>

      <!-- Dossier Summary -->
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-clipboard-check mr-2"></i>Décision Finale d'Approbation
        </template>
        <template #content>
          <div class="grid">
            <div class="col-12 md:col-6">
              <h5>Informations Dossier</h5>
              <div class="field-group">
                <div><strong>Référence:</strong> {{ dossierDetail.dossier.reference }}</div>
                <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
                <div><strong>Agriculteur:</strong> {{ dossierDetail.agriculteur.prenom }} {{ dossierDetail.agriculteur.nom }}</div>
                <div><strong>CIN:</strong> {{ dossierDetail.agriculteur.cin }}</div>
              </div>
            </div>
            
            <div class="col-12 md:col-6">
              <h5>Projet</h5>
              <div class="field-group">
                <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
                <div><strong>Rubrique:</strong> {{ dossierDetail.dossier.rubriqueDesignation }}</div>
                <div><strong>Montant demandé:</strong> {{ formatCurrency(dossierDetail.dossier.montantSubvention) }}</div>
                <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Commission Feedback -->
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-chat-square-text mr-2"></i>Retour de la Commission AHA-AF
        </template>
        <template #content>
          <div class="commission-feedback">
            <div class="feedback-status flex justify-content-between align-items-center mb-3">
              <Tag value="TERRAIN APPROUVÉ" severity="success" />
              <span class="text-sm text-500">Visite effectuée le {{ formatDate(new Date()) }}</span>
            </div>
            <p class="mb-2"><strong>Observations:</strong> Terrain conforme aux exigences du projet. Infrastructure existante adéquate pour l'installation des équipements.</p>
            <p class="mb-0"><strong>Recommandations:</strong> Procéder à l'approbation du projet. Respecter les spécifications techniques lors de l'installation.</p>
          </div>
        </template>
      </Card>

      <!-- Decision Form -->
      <Card v-if="!isAlreadyApproved()">
        <template #title>
          <i class="pi pi-gavel mr-2"></i>Votre Décision
        </template>
        <template #content>
          <form @submit.prevent="submitDecision">
            <div class="grid">
              <div class="col-12 md:col-6">
                <div class="field">
                  <label for="decision">Décision *</label>
                  <Select 
                    id="decision"
                    v-model="formData.decision" 
                    :options="decisionOptions" 
                    optionLabel="label" 
                    optionValue="value"
                    placeholder="Sélectionnez votre décision"
                    class="w-full"
                    @change="onDecisionChange"
                  />
                </div>
              </div>

              <div v-if="formData.decision === 'approve'" class="col-12 md:col-6">
                <div class="field">
                  <label for="montantApprouve">Montant Approuvé (DH) *</label>
                  <InputNumber 
                    id="montantApprouve"
                    v-model="formData.montantApprouve" 
                    :min="0"
                    :max="formData.montantDemande * 1.1"
                    :step="0.01"
                    placeholder="Montant à approuver"
                    class="w-full"
                  />
                  <small class="text-500">Montant demandé: {{ formatCurrency(formData.montantDemande) }}</small>
                </div>
              </div>
            </div>

            <div v-if="formData.decision === 'approve'" class="grid">
              <div class="col-12 md:col-6">
                <div class="field">
                  <label for="commentaireApprobation">Commentaire d'Approbation</label>
                  <Textarea 
                    id="commentaireApprobation"
                    v-model="formData.commentaireApprobation" 
                    rows="3"
                    placeholder="Commentaire sur l'approbation du dossier"
                    class="w-full"
                  />
                </div>
              </div>

              <div class="col-12 md:col-6">
                <div class="field">
                  <label for="conditionsSpecifiques">Conditions Spécifiques</label>
                  <Textarea 
                    id="conditionsSpecifiques"
                    v-model="formData.conditionsSpecifiques" 
                    rows="3"
                    placeholder="Conditions particulières pour l'approbation"
                    class="w-full"
                  />
                </div>
              </div>
            </div>

            <div v-if="formData.decision === 'reject'" class="grid">
              <div class="col-12">
                <div class="field">
                  <label for="motifRejet">Motif de Rejet *</label>
                  <Textarea 
                    id="motifRejet"
                    v-model="formData.motifRejet" 
                    rows="3"
                    placeholder="Raison du rejet du dossier"
                    class="w-full"
                  />
                </div>
              </div>
            </div>

            <div class="grid">
              <div class="col-12">
                <div class="field">
                  <label for="observationsCommission">Résumé des Observations Commission</label>
                  <Textarea 
                    id="observationsCommission"
                    v-model="formData.observationsCommission" 
                    rows="2"
                    placeholder="Résumé des observations de la commission"
                    class="w-full"
                  />
                </div>
              </div>
            </div>

            <div class="flex justify-content-end gap-2 pt-3">
              <Button 
                label="Annuler" 
                icon="pi pi-times" 
                @click="goBack"
                class="p-button-outlined"
                :disabled="submitting"
              />
              <Button 
                type="submit" 
                :label="getSubmitButtonLabel()"
                :icon="getSubmitButtonIcon()"
                :class="getSubmitButtonClass()"
                :loading="submitting"
                :disabled="!isFormValid()"
              />
            </div>
          </form>
        </template>
      </Card>
    </div>

    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import Select from 'primevue/select';
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Toast from 'primevue/toast';
import Message from 'primevue/message';

const router = useRouter();
const route = useRoute();
const toast = useToast();

// State
const loading = ref(true);
const error = ref(null);
const submitting = ref(false);
const dossierDetail = ref(null);

// Form data
const formData = reactive({
  decision: null,
  montantApprouve: null,
  montantDemande: 0,
  commentaireApprobation: '',
  conditionsSpecifiques: '',
  motifRejet: '',
  observationsCommission: 'Terrain conforme selon visite commission'
});

// Decision options
const decisionOptions = [
  { label: 'Approuver le dossier', value: 'approve' },
  { label: 'Rejeter le dossier', value: 'reject' }
];

// Computed properties
const dossierId = computed(() => route.params.dossierId);

// Methods
onMounted(async () => {
  await loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get(`/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
    // Check if dossier is already approved
    const isAlreadyApproved = response.dossier.statut === 'APPROVED' || 
                             response.dossier.dateApprobation || 
                             response.dossier.statut === 'COMPLETED';
    
    if (isAlreadyApproved) {
      console.log('Dossier already approved, redirecting to fiche view');
      toast.add({
        severity: 'info',
        summary: 'Information',
        detail: 'Ce dossier a déjà été approuvé',
        life: 3000
      });
      
      // Redirect to fiche view instead
      router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
      return;
    }
    
    // Initialize form with dossier data
    formData.montantDemande = response.dossier.montantSubvention;
    formData.montantApprouve = response.dossier.montantSubvention;
    
  } catch (err) {
    console.error('Error loading dossier:', err);
    error.value = err.message || 'Impossible de charger le dossier';
  } finally {
    loading.value = false;
  }
}

function onDecisionChange() {
  // Reset form fields when decision changes
  if (formData.decision === 'approve') {
    formData.motifRejet = '';
    formData.montantApprouve = formData.montantDemande;
  } else {
    formData.commentaireApprobation = '';
    formData.conditionsSpecifiques = '';
    formData.montantApprouve = null;
  }
}

function isFormValid() {
  if (!formData.decision) return false;
  
  if (formData.decision === 'approve') {
    return formData.montantApprouve > 0;
  } else {
    return formData.motifRejet && formData.motifRejet.trim();
  }
}

function getSubmitButtonLabel() {
  return formData.decision === 'approve' ? 'Approuver et Générer Fiche' : 'Rejeter le Dossier';
}

function getSubmitButtonIcon() {
  return formData.decision === 'approve' ? 'pi pi-check-circle' : 'pi pi-times-circle';
}

function getSubmitButtonClass() {
  return formData.decision === 'approve' ? 'p-button-success' : 'p-button-danger';
}

async function submitDecision() {
  if (!isFormValid()) return;
  
  try {
    submitting.value = true;
    
    const requestData = {
      dossierId: parseInt(dossierId.value),
      approved: formData.decision === 'approve',
      commentaireApprobation: formData.commentaireApprobation,
      motifRejet: formData.motifRejet,
      montantApprouve: formData.montantApprouve,
      conditionsSpecifiques: formData.conditionsSpecifiques,
      observationsCommission: formData.observationsCommission
    };

    const response = await ApiService.post('/fiche-approbation/final-approval', requestData);
    
    console.log('Final approval response:', response);
    
    // Check if response indicates success (could be response.success or just a successful HTTP response)
    const isSuccess = response.success === true || response.success !== false;
    
    if (isSuccess) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Décision prise avec succès',
        life: 3000
      });
      
      // If approved, redirect to fiche view with a small delay to allow backend processing
      if (formData.decision === 'approve') {
        console.log('Waiting 2 seconds before redirecting to fiche view...');
        
        // Force refresh parent window if it exists (for list view)
        if (window.opener && window.opener.location) {
          try {
            window.opener.location.reload();
          } catch (e) {
            console.log('Could not refresh parent window:', e);
          }
        }
        
        setTimeout(() => {
          console.log('Redirecting to fiche view for dossier:', dossierId.value);
          router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
        }, 2000); // Wait 2 seconds for backend to process
      } else {
        // If rejected, go back to list immediately
        console.log('Redirecting to dossier list after rejection');
        router.push('/agent_guc/dossiers');
      }
    } else {
      throw new Error(response.message || 'Erreur lors de la décision');
    }
    
  } catch (err) {
    console.error('Error submitting decision:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la soumission',
      life: 4000
    });
  } finally {
    submitting.value = false;
  }
}

function isAlreadyApproved() {
  if (!dossierDetail.value) return false;
  
  return dossierDetail.value.dossier.statut === 'APPROVED' || 
         dossierDetail.value.dossier.dateApprobation || 
         dossierDetail.value.dossier.statut === 'COMPLETED';
}

function goBack() {
  router.push('/agent_guc/dossiers');
}

function formatCurrency(amount) {
  if (!amount) return '0 DH';
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD'
  }).format(amount);
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>

<style scoped>
.final-approval-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
}

.approval-header {
  margin-bottom: 1.5rem;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.9rem;
}

.breadcrumb span:last-child {
  color: var(--primary-color);
  font-weight: 500;
}

.field-group > div {
  margin-bottom: 0.5rem;
}

.field-group strong {
  color: var(--text-color);
}
</style>