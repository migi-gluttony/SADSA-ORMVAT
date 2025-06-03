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

      <!-- Dossier Summary -->
      <div class="dossier-summary component-card">
        <h2><i class="pi pi-clipboard-check"></i> Décision Finale d'Approbation</h2>
        <div class="summary-grid">
          <div class="summary-section">
            <h4>Informations Dossier</h4>
            <div class="info-lines">
              <div><strong>Référence:</strong> {{ dossierDetail.dossier.reference }}</div>
              <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
              <div><strong>Agriculteur:</strong> {{ dossierDetail.agriculteur.prenom }} {{ dossierDetail.agriculteur.nom }}</div>
              <div><strong>CIN:</strong> {{ dossierDetail.agriculteur.cin }}</div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>Projet</h4>
            <div class="info-lines">
              <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
              <div><strong>Rubrique:</strong> {{ dossierDetail.dossier.rubriqueDesignation }}</div>
              <div><strong>Montant demandé:</strong> {{ formatCurrency(dossierDetail.dossier.montantSubvention) }}</div>
              <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Commission Feedback -->
      <div class="commission-feedback component-card">
        <h3><i class="pi pi-chat-square-text"></i> Retour de la Commission AHA-AF</h3>
        <div class="feedback-content">
          <!-- This would be populated with actual commission feedback -->
          <div class="feedback-summary">
            <div class="feedback-status">
              <Tag value="TERRAIN APPROUVÉ" severity="success" class="status-tag" />
              <span class="visit-date">Visite effectuée le 15/01/2025</span>
            </div>
            <div class="feedback-details">
              <p><strong>Agent Commission:</strong> Mohammed ALAMI (Équipe Filières Végétales)</p>
              <p><strong>Observations:</strong> Terrain conforme aux exigences du projet. Infrastructure existante adéquate pour l'installation des équipements. Accès facilité pour la livraison. Aucun obstacle technique identifié.</p>
              <p><strong>Recommandations:</strong> Procéder à l'approbation du projet. Respecter les spécifications techniques lors de l'installation.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Decision Form -->
      <div class="decision-form component-card">
        <h3><i class="pi pi-gavel"></i> Votre Décision</h3>
        <form @submit.prevent="submitDecision">
          <div class="form-grid">
            <div class="form-group">
              <label for="decision" class="form-label">Décision *</label>
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

            <div v-if="formData.decision === 'approve'" class="form-group">
              <label for="montantApprouve" class="form-label">Montant Approuvé (DH) *</label>
              <InputNumber 
                id="montantApprouve"
                v-model="formData.montantApprouve" 
                :min="0"
                :max="formData.montantDemande * 1.1"
                :step="0.01"
                placeholder="Montant à approuver"
                class="w-full"
              />
              <small class="form-hint">Montant demandé: {{ formatCurrency(formData.montantDemande) }}</small>
            </div>
          </div>

          <div v-if="formData.decision === 'approve'" class="approval-fields">
            <div class="form-group">
              <label for="commentaireApprobation" class="form-label">Commentaire d'Approbation</label>
              <Textarea 
                id="commentaireApprobation"
                v-model="formData.commentaireApprobation" 
                rows="3"
                placeholder="Commentaire sur l'approbation du dossier"
                class="w-full"
              />
            </div>

            <div class="form-group">
              <label for="conditionsSpecifiques" class="form-label">Conditions Spécifiques</label>
              <Textarea 
                id="conditionsSpecifiques"
                v-model="formData.conditionsSpecifiques" 
                rows="2"
                placeholder="Conditions particulières pour l'approbation"
                class="w-full"
              />
            </div>
          </div>

          <div v-if="formData.decision === 'reject'" class="rejection-fields">
            <div class="form-group">
              <label for="motifRejet" class="form-label">Motif de Rejet *</label>
              <Textarea 
                id="motifRejet"
                v-model="formData.motifRejet" 
                rows="3"
                placeholder="Raison du rejet du dossier"
                class="w-full"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="observationsCommission" class="form-label">Résumé des Observations Commission</label>
            <Textarea 
              id="observationsCommission"
              v-model="formData.observationsCommission" 
              rows="2"
              placeholder="Résumé des observations de la commission"
              class="w-full"
            />
          </div>

          <div class="form-actions">
            <Button 
              type="submit" 
              :label="getSubmitButtonLabel()"
              :icon="getSubmitButtonIcon()"
              :class="getSubmitButtonClass()"
              :loading="submitting"
              :disabled="!isFormValid()"
            />
            <Button 
              label="Annuler" 
              icon="pi pi-times" 
              @click="goBack"
              class="p-button-outlined"
              :disabled="submitting"
            />
          </div>
        </form>
      </div>

      <!-- Preview Section (when approving) -->
      <div v-if="formData.decision === 'approve' && isFormValid()" class="fiche-preview component-card">
        <h3><i class="pi pi-file-text"></i> Aperçu de la Fiche d'Approbation</h3>
        <div class="preview-content">
          <div class="fiche-header">
            <h4>OFFICE RÉGIONAL DE MISE EN VALEUR AGRICOLE DU TADLA</h4>
            <h5>FICHE D'APPROBATION</h5>
            <p><strong>N° {{ generateFicheNumber() }}</strong></p>
          </div>
          
          <div class="fiche-details">
            <div class="detail-section">
              <h6>BÉNÉFICIAIRE</h6>
              <p><strong>Nom et Prénom:</strong> {{ dossierDetail.agriculteur.nom }} {{ dossierDetail.agriculteur.prenom }}</p>
              <p><strong>CIN:</strong> {{ dossierDetail.agriculteur.cin }}</p>
              <p><strong>Téléphone:</strong> {{ dossierDetail.agriculteur.telephone }}</p>
            </div>
            
            <div class="detail-section">
              <h6>PROJET APPROUVÉ</h6>
              <p><strong>Type:</strong> {{ dossierDetail.dossier.rubriqueDesignation }}</p>
              <p><strong>Description:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</p>
              <p><strong>Montant approuvé:</strong> <span class="amount-approved">{{ formatCurrency(formData.montantApprouve) }}</span></p>
            </div>
            
            <div class="detail-section">
              <h6>VALIDATION</h6>
              <p><strong>Date d'approbation:</strong> {{ formatDate(new Date()) }}</p>
              <p><strong>Agent GUC:</strong> {{ currentUser?.prenom }} {{ currentUser?.nom }}</p>
              <p><strong>Validité:</strong> 6 mois</p>
            </div>
          </div>
        </div>
      </div>
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
import Select from 'primevue/select';
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Toast from 'primevue/toast';

const router = useRouter();
const route = useRoute();
const toast = useToast();

// State
const loading = ref(true);
const error = ref(null);
const submitting = ref(false);
const dossierDetail = ref(null);
const currentUser = ref(AuthService.getCurrentUser());

// Form data
const formData = reactive({
  decision: null,
  montantApprouve: null,
  montantDemande: 0,
  commentaireApprobation: '',
  conditionsSpecifiques: '',
  motifRejet: '',
  observationsCommission: ''
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
    
    // Initialize form with dossier data
    formData.montantDemande = response.dossier.montantSubvention;
    formData.montantApprouve = response.dossier.montantSubvention;
    
    // Pre-fill commission observations if available
    formData.observationsCommission = "Terrain conforme selon visite commission du 15/01/2025";
    
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

    const response = await ApiService.post(`/dossiers/${dossierId.value}/final-approval`, requestData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 4000
      });
      
      // If approved, redirect to fiche view
      if (formData.decision === 'approve') {
        router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
      } else {
        // If rejected, go back to list
        router.push('/agent_guc/dossiers');
      }
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

function generateFicheNumber() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const id = dossierDetail.value?.dossier?.id || 1;
  
  return `FA-${year}-${month}-${String(id).padStart(6, '0')}`;
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
  padding: 0;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
  color: #6b7280;
}

.error-container i {
  font-size: 3rem;
  color: #dc2626;
}

/* Header */
.approval-header {
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: var(--background-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  color: #6b7280;
  font-size: 0.9rem;
}

.breadcrumb span:last-child {
  color: var(--primary-color);
  font-weight: 500;
}

/* Component Card */
.component-card {
  background: var(--background-color);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  margin-bottom: 2rem;
}

.component-card h2,
.component-card h3 {
  color: var(--primary-color);
  margin: 0 0 1.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Dossier Summary */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.summary-section h4 {
  color: var(--primary-color);
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: 600;
}

.info-lines {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-lines > div {
  font-size: 0.9rem;
}

/* Commission Feedback */
.feedback-content {
  background: #f8fffe;
  border: 1px solid #10b981;
  border-radius: 8px;
  padding: 1.5rem;
}

.feedback-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.status-tag {
  font-size: 0.8rem !important;
  font-weight: 600 !important;
}

.visit-date {
  color: #6b7280;
  font-size: 0.9rem;
}

.feedback-details p {
  margin: 0 0 0.75rem 0;
  line-height: 1.6;
}

/* Decision Form */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.9rem;
}

.form-hint {
  color: #6b7280;
  font-size: 0.8rem;
}

.approval-fields,
.rejection-fields {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

/* Fiche Preview */
.preview-content {
  background: white;
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 2rem;
  margin-top: 1rem;
}

.fiche-header {
  text-align: center;
  border-bottom: 2px solid var(--primary-color);
  padding-bottom: 1rem;
  margin-bottom: 2rem;
}

.fiche-header h4 {
  margin: 0 0 0.5rem 0;
  color: var(--primary-color);
  font-size: 1.1rem;
}

.fiche-header h5 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-size: 1rem;
}

.fiche-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.detail-section h6 {
  color: var(--primary-color);
  margin: 0 0 0.75rem 0;
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-section p {
  margin: 0 0 0.5rem 0;
  font-size: 0.85rem;
  line-height: 1.4;
}

.amount-approved {
  color: var(--success-color);
  font-weight: 700;
  font-size: 1rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .fiche-details {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .feedback-status {
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-start;
  }
}
</style>