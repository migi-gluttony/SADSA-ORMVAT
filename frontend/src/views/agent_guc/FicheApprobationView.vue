<template>
  <div class="fiche-approbation-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>Chargement de la fiche...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Erreur</h3>
      <p>{{ error }}</p>
      <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" />
    </div>

    <!-- Main Content -->
    <div v-else-if="ficheData" class="fiche-content">
      <!-- Header -->
      <div class="fiche-header">
        <div class="header-nav">
          <Button 
            label="Retour" 
            icon="pi pi-arrow-left" 
            @click="goBack"
            class="p-button-outlined"
          />
          <div class="breadcrumb">
            <span>{{ getBreadcrumbRoot() }}</span>
            <i class="pi pi-angle-right"></i>
            <span>{{ ficheData.reference }}</span>
            <i class="pi pi-angle-right"></i>
            <span>Fiche d'Approbation</span>
          </div>
        </div>
        
        <div class="header-actions">
          <Button 
            label="Imprimer" 
            icon="pi pi-print" 
            @click="printFiche"
            class="p-button-info"
          />
          <Button 
            v-if="!ficheData.farmersRetrieved && canMarkRetrieved()"
            label="Marquer comme récupérée" 
            icon="pi pi-user-check" 
            @click="showRetrievalDialog"
            class="p-button-success"
          />
        </div>
      </div>

      <!-- Status Banner -->
      <div class="status-banner" :class="getStatusBannerClass()">
        <div class="status-info">
          <i :class="getStatusIcon()"></i>
          <div class="status-text">
            <h4>{{ getStatusTitle() }}</h4>
            <p>{{ getStatusDescription() }}</p>
          </div>
        </div>
        <div v-if="ficheData.farmersRetrieved" class="retrieval-info">
          <small>Récupérée le {{ formatDate(ficheData.dateRetrievalFermier) }}</small>
        </div>
      </div>

      <!-- Fiche Document -->
      <div class="fiche-document" ref="ficheDocument">
        <div class="document-header">
          <div class="logo-section">
            <div class="organization-logo">
              <!-- Organization logo would go here -->
              <div class="logo-placeholder">ORMVAT</div>
            </div>
            <div class="organization-info">
              <h1>OFFICE RÉGIONAL DE MISE EN VALEUR AGRICOLE DU TADLA</h1>
              <h2>FICHE D'APPROBATION</h2>
              <div class="fiche-number">N° {{ ficheData.numeroFiche }}</div>
            </div>
          </div>
          <div class="date-section">
            <div class="approval-date">
              <strong>Date d'approbation:</strong><br>
              {{ formatDate(ficheData.dateApprobation) }}
            </div>
          </div>
        </div>

        <div class="document-content">
          <!-- Beneficiary Section -->
          <div class="fiche-section">
            <h3><i class="pi pi-user"></i> BÉNÉFICIAIRE</h3>
            <div class="section-grid">
              <div class="info-group">
                <div class="info-item">
                  <strong>Nom et Prénom:</strong>
                  <span>{{ ficheData.agriculteurNom }} {{ ficheData.agriculteurPrenom }}</span>
                </div>
                <div class="info-item">
                  <strong>CIN:</strong>
                  <span>{{ ficheData.agriculteurCin }}</span>
                </div>
              </div>
              <div class="info-group">
                <div class="info-item">
                  <strong>Téléphone:</strong>
                  <span>{{ ficheData.agriculteurTelephone }}</span>
                </div>
                <div class="info-item">
                  <strong>Localisation:</strong>
                  <span>{{ getFullLocation() }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Project Section -->
          <div class="fiche-section">
            <h3><i class="pi pi-briefcase"></i> PROJET APPROUVÉ</h3>
            <div class="section-grid">
              <div class="info-group">
                <div class="info-item">
                  <strong>Référence Dossier:</strong>
                  <span>{{ ficheData.reference }}</span>
                </div>
                <div class="info-item">
                  <strong>SABA:</strong>
                  <span>{{ ficheData.saba }}</span>
                </div>
                <div class="info-item">
                  <strong>Type de projet:</strong>
                  <span>{{ ficheData.rubriqueDesignation }}</span>
                </div>
              </div>
              <div class="info-group">
                <div class="info-item">
                  <strong>Description détaillée:</strong>
                  <span>{{ ficheData.sousRubriqueDesignation }}</span>
                </div>
                <div class="info-item amount-section">
                  <strong>Montant approuvé:</strong>
                  <span class="amount-value">{{ formatCurrency(ficheData.montantApprouve) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Approval Details -->
          <div class="fiche-section">
            <h3><i class="pi pi-clipboard-check"></i> DÉTAILS DE L'APPROBATION</h3>
            <div class="approval-details">
              <div class="approval-status">
                <Tag value="APPROUVÉ" severity="success" class="status-tag-large" />
                <span class="validity-info">Valable jusqu'au {{ ficheData.validiteJusquau }}</span>
              </div>
              
              <div v-if="ficheData.commentaireApprobation" class="approval-comment">
                <strong>Commentaire d'approbation:</strong>
                <p>{{ ficheData.commentaireApprobation }}</p>
              </div>
              
              <div v-if="ficheData.conditionsSpecifiques" class="conditions">
                <strong>Conditions spécifiques:</strong>
                <p>{{ ficheData.conditionsSpecifiques }}</p>
              </div>
              
              <div v-if="ficheData.observationsCommission" class="commission-obs">
                <strong>Observations de la commission:</strong>
                <p>{{ ficheData.observationsCommission }}</p>
              </div>
            </div>
          </div>

          <!-- Administrative Info -->
          <div class="fiche-section">
            <h3><i class="pi pi-building"></i> INFORMATIONS ADMINISTRATIVES</h3>
            <div class="section-grid">
              <div class="info-group">
                <div class="info-item">
                  <strong>Antenne:</strong>
                  <span>{{ ficheData.antenneDesignation }}</span>
                </div>
                <div class="info-item">
                  <strong>CDA:</strong>
                  <span>{{ ficheData.cdaNom }}</span>
                </div>
              </div>
              <div class="info-group">
                <div class="info-item">
                  <strong>Province:</strong>
                  <span>{{ ficheData.agriculteurProvince }}</span>
                </div>
                <div class="info-item">
                  <strong>Date de génération:</strong>
                  <span>{{ formatDateTime(ficheData.dateGeneration) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Signatures Section -->
          <div class="signatures-section">
            <h3><i class="pi pi-pen-nib"></i> VALIDATION ET SIGNATURES</h3>
            <div class="signatures-grid">
              <div class="signature-block">
                <h4>Agent GUC</h4>
                <div class="signature-info">
                  <p><strong>{{ ficheData.agentGucNom }}</strong></p>
                  <p class="signature-line">{{ ficheData.agentGucSignature }}</p>
                  <div class="signature-space"></div>
                </div>
              </div>
              
              <div class="signature-block">
                <h4>Responsable ORMVAT</h4>
                <div class="signature-info">
                  <p><strong>{{ ficheData.responsableNom }}</strong></p>
                  <p class="signature-line">{{ ficheData.responsableSignature }}</p>
                  <div class="signature-space"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Important Notice -->
          <div class="important-notice">
            <h4><i class="pi pi-info-circle"></i> IMPORTANT</h4>
            <ul>
              <li>Cette fiche doit être présentée lors de la phase de réalisation du projet</li>
              <li>La validité de cette approbation est de 6 mois à compter de la date d'émission</li>
              <li>Tout changement dans les spécifications du projet nécessite une nouvelle approbation</li>
              <li>Le bénéficiaire doit respecter toutes les conditions mentionnées ci-dessus</li>
            </ul>
          </div>
        </div>

        <!-- Document Footer -->
        <div class="document-footer">
          <div class="footer-info">
            <p>Document généré automatiquement par le système SADSA-ORMVAT</p>
            <p>{{ formatDateTime(new Date()) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Farmer Retrieval Dialog -->
    <Dialog 
      v-model:visible="retrievalDialog.visible" 
      modal 
      :header="'Confirmation de Récupération'"
      :style="{ width: '500px' }"
    >
      <div class="retrieval-form">
        <div class="form-group">
          <label for="farmerCin" class="form-label">CIN de l'Agriculteur *</label>
          <InputText 
            id="farmerCin"
            v-model="retrievalDialog.farmerCin" 
            placeholder="Entrez le CIN pour confirmation"
            class="w-full"
          />
        </div>
        
        <div class="form-group">
          <label for="retrievedBy" class="form-label">Nom de la personne qui récupère *</label>
          <InputText 
            id="retrievedBy"
            v-model="retrievalDialog.retrievedBy" 
            placeholder="Nom complet"
            class="w-full"
          />
        </div>
        
        <div class="form-group">
          <label for="retrievalComment" class="form-label">Commentaire</label>
          <Textarea 
            id="retrievalComment"
            v-model="retrievalDialog.comment" 
            rows="2"
            placeholder="Commentaire sur la récupération"
            class="w-full"
          />
        </div>
      </div>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="retrievalDialog.visible = false"
          class="p-button-text"
        />
        <Button 
          label="Confirmer" 
          icon="pi pi-check" 
          @click="confirmRetrieval"
          :loading="retrievalDialog.loading"
          :disabled="!isRetrievalFormValid()"
        />
      </template>
    </Dialog>

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
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Toast from 'primevue/toast';

const router = useRouter();
const route = useRoute();
const toast = useToast();

// State
const loading = ref(true);
const error = ref(null);
const ficheData = ref(null);
const ficheDocument = ref(null);

// Retrieval dialog
const retrievalDialog = reactive({
  visible: false,
  farmerCin: '',
  retrievedBy: '',
  comment: '',
  loading: false
});

// Computed properties
const dossierId = computed(() => route.params.dossierId);

// Methods
onMounted(async () => {
  await loadFicheData();
});

async function loadFicheData() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get(`/fiche-approbation/${dossierId.value}/print`);
    ficheData.value = response;
    
  } catch (err) {
    console.error('Error loading fiche:', err);
    error.value = err.message || 'Impossible de charger la fiche';
  } finally {
    loading.value = false;
  }
}

function getStatusBannerClass() {
  if (ficheData.value?.farmersRetrieved) {
    return 'status-retrieved';
  }
  return 'status-waiting';
}

function getStatusIcon() {
  if (ficheData.value?.farmersRetrieved) {
    return 'pi pi-check-circle';
  }
  return 'pi pi-clock';
}

function getStatusTitle() {
  if (ficheData.value?.farmersRetrieved) {
    return 'Fiche Récupérée';
  }
  return 'En Attente de Récupération';
}

function getStatusDescription() {
  if (ficheData.value?.farmersRetrieved) {
    return 'Cette fiche a été récupérée par l\'agriculteur. Le dossier peut maintenant passer en phase de réalisation.';
  }
  return 'Cette fiche est prête à être récupérée par l\'agriculteur. Marquez-la comme récupérée une fois remise.';
}

function getFullLocation() {
  const parts = [];
  if (ficheData.value?.agriculteurDouar) parts.push(ficheData.value.agriculteurDouar);
  if (ficheData.value?.agriculteurCommune) parts.push(ficheData.value.agriculteurCommune);
  if (ficheData.value?.agriculteurProvince) parts.push(ficheData.value.agriculteurProvince);
  return parts.join(', ') || 'Non spécifiée';
}

function getBreadcrumbRoot() {
  const user = AuthService.getCurrentUser();
  switch (user?.role) {
    case 'AGENT_GUC':
      return 'Dossiers GUC';
    case 'AGENT_ANTENNE':
      return 'Mes Dossiers';
    default:
      return 'Dossiers';
  }
}

function canMarkRetrieved() {
  const user = AuthService.getCurrentUser();
  return user?.role === 'AGENT_GUC';
}

function showRetrievalDialog() {
  retrievalDialog.visible = true;
  retrievalDialog.farmerCin = ficheData.value?.agriculteurCin || '';
  retrievalDialog.retrievedBy = '';
  retrievalDialog.comment = '';
}

function isRetrievalFormValid() {
  return retrievalDialog.farmerCin && 
         retrievalDialog.farmerCin === ficheData.value?.agriculteurCin &&
         retrievalDialog.retrievedBy && 
         retrievalDialog.retrievedBy.trim();
}

async function confirmRetrieval() {
  if (!isRetrievalFormValid()) return;
  
  try {
    retrievalDialog.loading = true;
    
    const requestData = {
      dossierId: parseInt(dossierId.value),
      farmerCin: retrievalDialog.farmerCin,
      retrievedBy: retrievalDialog.retrievedBy,
      dateRetrieval: new Date().toISOString(),
      commentaire: retrievalDialog.comment
    };

    const response = await ApiService.post('/fiche-approbation/mark-retrieved', requestData);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Fiche marquée comme récupérée avec succès',
        life: 4000
      });
      
      retrievalDialog.visible = false;
      await loadFicheData(); // Reload to show updated status
    }
    
  } catch (err) {
    console.error('Error marking retrieval:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors du marquage',
      life: 4000
    });
  } finally {
    retrievalDialog.loading = false;
  }
}

function printFiche() {
  // Hide all UI elements except the fiche document
  const elementsToHide = document.querySelectorAll('body > *:not(.fiche-approbation-container)');
  const headerElement = document.querySelector('.fiche-header');
  const statusElement = document.querySelector('.status-banner');
  
  // Hide elements
  elementsToHide.forEach(el => el.style.display = 'none');
  if (headerElement) headerElement.style.display = 'none';
  if (statusElement) statusElement.style.display = 'none';
  
  // Print
  window.print();
  
  // Restore elements
  elementsToHide.forEach(el => el.style.display = '');
  if (headerElement) headerElement.style.display = '';
  if (statusElement) statusElement.style.display = '';
}

function goBack() {
  const user = AuthService.getCurrentUser();
  switch (user?.role) {
    case 'AGENT_GUC':
      router.push('/agent_guc/dossiers');
      break;
    case 'AGENT_ANTENNE':
      router.push('/agent_antenne/dossiers');
      break;
    default:
      router.push('/');
  }
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

function formatDateTime(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date));
}
</script>

<style scoped>
.fiche-approbation-container {
  max-width: 1000px;
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
.fiche-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Status Banner */
.status-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.status-banner.status-waiting {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid #f59e0b;
}

.status-banner.status-retrieved {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid #10b981;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-info i {
  font-size: 1.5rem;
}

.status-waiting .status-info i {
  color: #f59e0b;
}

.status-retrieved .status-info i {
  color: #10b981;
}

.status-text h4 {
  margin: 0 0 0.25rem 0;
  color: var(--text-color);
}

.status-text p {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.retrieval-info {
  color: #6b7280;
  font-size: 0.8rem;
}

/* Document Styles */
.fiche-document {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  font-family: 'Times New Roman', serif;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 3px solid var(--primary-color);
  padding-bottom: 1.5rem;
  margin-bottom: 2rem;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.logo-placeholder {
  width: 80px;
  height: 80px;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: bold;
  font-size: 1.2rem;
}

.organization-info h1 {
  margin: 0 0 0.5rem 0;
  color: var(--primary-color);
  font-size: 1.4rem;
  font-weight: bold;
  text-align: center;
}

.organization-info h2 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-size: 1.2rem;
  text-align: center;
}

.fiche-number {
  text-align: center;
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--primary-color);
  border: 2px solid var(--primary-color);
  padding: 0.5rem 1rem;
  border-radius: 4px;
}

.date-section {
  text-align: right;
  font-size: 0.9rem;
}

.approval-date {
  color: var(--text-color);
}

/* Document Content */
.document-content {
  line-height: 1.6;
}

.fiche-section {
  margin-bottom: 2rem;
  page-break-inside: avoid;
}

.fiche-section h3 {
  color: var(--primary-color);
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.5rem;
}

.section-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-item strong {
  color: var(--text-color);
  font-size: 0.9rem;
}

.info-item span {
  color: #374151;
  font-size: 0.9rem;
}

.amount-section .amount-value {
  font-size: 1.2rem !important;
  font-weight: bold !important;
  color: var(--success-color) !important;
}

/* Approval Details */
.approval-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.approval-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f0fdf4;
  border-radius: 6px;
}

.status-tag-large {
  font-size: 1rem !important;
  padding: 0.5rem 1rem !important;
  font-weight: bold !important;
}

.validity-info {
  color: #6b7280;
  font-size: 0.9rem;
}

.approval-comment,
.conditions,
.commission-obs {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid var(--primary-color);
}

.approval-comment p,
.conditions p,
.commission-obs p {
  margin: 0.5rem 0 0 0;
  color: #374151;
}

/* Signatures */
.signatures-section {
  margin-top: 2rem;
  page-break-inside: avoid;
}

.signatures-section h3 {
  color: var(--primary-color);
  margin: 0 0 1.5rem 0;
  font-size: 1.1rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.5rem;
}

.signatures-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3rem;
}

.signature-block {
  text-align: center;
}

.signature-block h4 {
  margin: 0 0 1rem 0;
  color: var(--primary-color);
  font-size: 1rem;
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
}

.signature-info p {
  margin: 0 0 0.5rem 0;
  font-size: 0.9rem;
}

.signature-line {
  font-style: italic;
  color: #6b7280;
}

.signature-space {
  height: 60px;
  border-bottom: 1px solid #000;
  margin: 1rem 0;
}

/* Important Notice */
.important-notice {
  margin-top: 2rem;
  padding: 1.5rem;
  background: #fffbeb;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  page-break-inside: avoid;
}

.important-notice h4 {
  margin: 0 0 1rem 0;
  color: #f59e0b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.important-notice ul {
  margin: 0;
  padding-left: 1.5rem;
}

.important-notice li {
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.9rem;
}

/* Document Footer */
.document-footer {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
  text-align: center;
  color: #6b7280;
  font-size: 0.8rem;
}

.footer-info p {
  margin: 0.25rem 0;
}

/* Retrieval Dialog */
.retrieval-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

/* Print Styles */
@media print {
  .fiche-header,
  .status-banner {
    display: none !important;
  }

  .fiche-document {
    box-shadow: none;
    border: none;
    padding: 1rem;
  }

  .section-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .signatures-grid {
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
  }

  .fiche-section {
    margin-bottom: 1.5rem;
  }

  .signature-space {
    height: 40px;
  }
}

/* Responsive Design */
@media (max-width: 768px) {
  .fiche-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .header-nav {
    justify-content: space-between;
  }

  .header-actions {
    justify-content: center;
  }

  .document-header {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }

  .section-grid {
    grid-template-columns: 1fr;
  }

  .signatures-grid {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .status-banner {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
}
</style>