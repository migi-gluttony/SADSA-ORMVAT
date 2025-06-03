<template>
  <div class="fiche-approbation-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <ProgressSpinner size="50px" />
      <span>{{ loadingMessage }}</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Erreur</h3>
      <p>{{ error }}</p>
      <div class="error-actions">
        <Button 
          label="Réessayer" 
          icon="pi pi-refresh" 
          @click="loadFicheData"
          class="p-button-outlined mr-2"
        />
        <Button 
          label="Retour" 
          icon="pi pi-arrow-left" 
          @click="goBack" 
        />
      </div>
    </div>

    <!-- Main Content -->
    <div v-else-if="ficheData" class="fiche-content">
      <!-- Header Actions (hidden in print) -->
      <div class="fiche-header no-print">
        <div class="header-nav">
          <Button 
            label="Retour" 
            icon="pi pi-arrow-left" 
            @click="goBack"
            class="p-button-outlined"
          />
          <div class="breadcrumb">
            <span>Dossiers GUC</span>
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
            class="p-button-success"
          />
          <Button 
            v-if="!ficheData.farmersRetrieved && canMarkRetrieved()"
            label="Marquer comme récupérée" 
            icon="pi pi-user-check" 
            @click="showRetrievalDialog"
            class="p-button-info"
          />
        </div>
      </div>

      <!-- Status Banner (hidden in print) -->
      <div v-if="ficheData.farmersRetrieved" class="status-banner no-print">
        <div class="status-info">
          <i class="pi pi-check-circle"></i>
          <div class="status-text">
            <h4>Fiche Récupérée</h4>
            <p>Cette fiche a été récupérée par l'agriculteur le {{ formatDate(ficheData.dateRetrievalFermier) }}</p>
          </div>
        </div>
      </div>

      <!-- Printable Fiche Document -->
      <div class="printable-fiche print-only">
        <div class="fiche-document">
          <!-- Header with logos and title (based on PrintableReceipt) -->
          <div class="print-header">
            <div class="header-logo left-logo">
              <img src="/src/assets/logo/logo_ministre.jpg" alt="Armoiries du Maroc" />
            </div>
            <div class="header-text">
              <div class="header-title">
                <p><strong>Royaume du Maroc</strong></p>
                <p>Ministère de l'Agriculture, de la Pêche Maritime</p>
                <p>et du Développement Rural et des Eaux et Forêts</p>
                <p><strong>OFFICE RÉGIONAL DE MISE EN VALEUR AGRICOLE DU TADLA</strong></p>
              </div>
            </div>
            <div class="header-logo right-logo">
              <img src="/src/assets/logo/logo-ormvat-full-original.jpg" alt="Logo ORMVAT" />
            </div>
          </div>
          
          <hr class="header-separator" />
          
          <!-- Document Title -->
          <div class="document-title">
            <h1>Système Automatisé de Demande de Subventions Agricoles (SADSA)</h1>
            <h2>FICHE D'APPROBATION</h2>
            <h3>N° {{ ficheData.numeroFiche }}</h3>
            <p class="approval-date">Date d'approbation: {{ formatDate(ficheData.dateApprobation) }}</p>
          </div>
          
          <!-- Fiche Content -->
          <div class="content-area">
            <!-- Beneficiary Section -->
            <div class="fiche-section">
              <h3>INFORMATIONS DU BÉNÉFICIAIRE</h3>
              <table class="info-table">
                <tr>
                  <td class="label">Nom et Prénom :</td>
                  <td class="value">{{ ficheData.agriculteurNom }} {{ ficheData.agriculteurPrenom }}</td>
                </tr>
                <tr>
                  <td class="label">CIN :</td>
                  <td class="value">{{ ficheData.agriculteurCin }}</td>
                </tr>
                <tr>
                  <td class="label">Téléphone :</td>
                  <td class="value">{{ ficheData.agriculteurTelephone }}</td>
                </tr>
                <tr>
                  <td class="label">Localisation :</td>
                  <td class="value">{{ getFullLocation() }}</td>
                </tr>
              </table>
            </div>

            <!-- Project Section -->
            <div class="fiche-section">
              <h3>INFORMATIONS DU PROJET</h3>
              <table class="info-table">
                <tr>
                  <td class="label">Référence Dossier :</td>
                  <td class="value">{{ ficheData.reference }}</td>
                </tr>
                <tr>
                  <td class="label">Numéro SABA :</td>
                  <td class="value"><strong>{{ ficheData.saba }}</strong></td>
                </tr>
                <tr>
                  <td class="label">Type de projet :</td>
                  <td class="value">{{ ficheData.rubriqueDesignation }}</td>
                </tr>
                <tr>
                  <td class="label">Description détaillée :</td>
                  <td class="value">{{ ficheData.sousRubriqueDesignation }}</td>
                </tr>
                <tr>
                  <td class="label">Montant demandé :</td>
                  <td class="value">{{ formatCurrency(ficheData.montantDemande) }}</td>
                </tr>
                <tr>
                  <td class="label">Montant approuvé :</td>
                  <td class="value amount-approved"><strong>{{ formatCurrency(ficheData.montantApprouve) }}</strong></td>
                </tr>
              </table>
            </div>

            <!-- Approval Section -->
            <div class="fiche-section">
              <h3>DÉTAILS DE L'APPROBATION</h3>
              <table class="info-table">
                <tr>
                  <td class="label">Statut :</td>
                  <td class="value"><strong>{{ ficheData.statutApprobation }}</strong></td>
                </tr>
                <tr>
                  <td class="label">Date d'approbation :</td>
                  <td class="value">{{ formatDate(ficheData.dateApprobation) }}</td>
                </tr>
                <tr>
                  <td class="label">Validité :</td>
                  <td class="value">{{ ficheData.validiteJusquau }}</td>
                </tr>
                <tr v-if="ficheData.commentaireApprobation">
                  <td class="label">Commentaire :</td>
                  <td class="value">{{ ficheData.commentaireApprobation }}</td>
                </tr>
                <tr v-if="ficheData.conditionsSpecifiques">
                  <td class="label">Conditions spécifiques :</td>
                  <td class="value">{{ ficheData.conditionsSpecifiques }}</td>
                </tr>
              </table>
            </div>

            <!-- Administrative Section -->
            <div class="fiche-section">
              <h3>INFORMATIONS ADMINISTRATIVES</h3>
              <table class="info-table">
                <tr>
                  <td class="label">Antenne :</td>
                  <td class="value">{{ ficheData.antenneDesignation }}</td>
                </tr>
                <tr>
                  <td class="label">CDA :</td>
                  <td class="value">{{ ficheData.cdaNom }}</td>
                </tr>
                <tr>
                  <td class="label">Province :</td>
                  <td class="value">{{ ficheData.agriculteurProvince }}</td>
                </tr>
                <tr>
                  <td class="label">Agent GUC :</td>
                  <td class="value">{{ ficheData.agentGucNom }}</td>
                </tr>
              </table>
            </div>

            <!-- Important Notice -->
            <div class="important-notice">
              <h3>INFORMATIONS IMPORTANTES</h3>
              <ul>
                <li>Cette fiche atteste de l'approbation de votre demande de subvention agricole.</li>
                <li>Conservez précieusement ce document pour la phase de réalisation de votre projet.</li>
                <li>La validité de cette approbation est de 6 mois à compter de la date d'émission.</li>
                <li>Tout changement dans les spécifications du projet nécessite une nouvelle approbation.</li>
                <li>Vous devez respecter toutes les conditions mentionnées ci-dessus.</li>
                <li>Présentez cette fiche lors de la mise en œuvre de votre projet.</li>
              </ul>
            </div>

            <!-- Signatures Section -->
            <div class="signatures-section">
              <div class="signature-grid">
                <div class="signature-block">
                  <p><strong>Agent GUC</strong></p>
                  <p>{{ ficheData.agentGucNom }}</p>
                  <p class="signature-line">{{ ficheData.agentGucSignature }}</p>
                  <div class="signature-space"></div>
                </div>
                
                <div class="signature-block">
                  <p><strong>Responsable ORMVAT</strong></p>
                  <p>{{ ficheData.responsableNom }}</p>
                  <p class="signature-line">{{ ficheData.responsableSignature }}</p>
                  <div class="signature-space"></div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Footer section -->
          <div class="footer-section">
            <div class="contact-info">
              <p><strong>Pour toute information :</strong></p>
              <p>ORMVAT - BP 244, Fquih Ben Salah</p>
              <p>Tél : +212 5 23 43 50 23/35/48</p>
              <p>Email : sadsa@ormvatadla.ma</p>
            </div>
            <div class="signature-area">
              <p>Fait à {{ ficheData.antenneDesignation }}, le {{ formatDate(ficheData.dateApprobation) }}</p>
              <div class="signature">
                <p>Document généré automatiquement par SADSA</p>
                <p>{{ formatDateTime(ficheData.dateGeneration) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Farmer Retrieval Dialog -->
    <Dialog 
      v-model:visible="retrievalDialog.visible" 
      modal 
      header="Confirmation de Récupération"
      style="width: 500px"
    >
      <div class="retrieval-form">
        <div class="field">
          <label for="farmerCin">CIN de l'Agriculteur *</label>
          <InputText 
            id="farmerCin"
            v-model="retrievalDialog.farmerCin" 
            placeholder="Entrez le CIN pour confirmation"
            class="w-full"
          />
        </div>
        
        <div class="field">
          <label for="retrievedBy">Nom de la personne qui récupère *</label>
          <InputText 
            id="retrievedBy"
            v-model="retrievalDialog.retrievedBy" 
            placeholder="Nom complet"
            class="w-full"
          />
        </div>
        
        <div class="field">
          <label for="retrievalComment">Commentaire</label>
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
const loadingMessage = ref('Chargement de la fiche...');

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
    loadingMessage.value = 'Chargement de la fiche...';
    
    console.log('Loading fiche data for dossier:', dossierId.value);
    
    // Try to load the fiche with retry logic
    const response = await loadFicheWithRetry();
    
    console.log('Fiche data loaded successfully:', response);
    ficheData.value = response;
    
  } catch (err) {
    console.error('Error loading fiche:', err);
    
    // More specific error handling
    if (err.message && err.message.includes('dateApprobation')) {
      error.value = 'Le dossier n\'a pas encore été approuvé ou la fiche n\'est pas prête. Veuillez réessayer dans quelques instants.';
    } else if (err.status === 404) {
      error.value = 'Fiche d\'approbation non trouvée. Assurez-vous que le dossier a été approuvé.';
    } else {
      error.value = err.message || 'Impossible de charger la fiche. Assurez-vous que le dossier a été approuvé.';
    }
    
    // Don't auto-redirect on error, let user choose to go back
  } finally {
    loading.value = false;
  }
}

async function loadFicheWithRetry(maxRetries = 3, delay = 2000) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      if (attempt === 1) {
        loadingMessage.value = 'Chargement de la fiche...';
      } else {
        loadingMessage.value = `Tentative ${attempt}/${maxRetries}...`;
      }
      
      console.log(`Attempt ${attempt} to load fiche data...`);
      
      const response = await ApiService.get(`/fiche-approbation/${dossierId.value}/print`);
      
      // If we get here, the request succeeded
      return response;
      
    } catch (err) {
      console.error(`Attempt ${attempt} failed:`, err);
      
      // If this is the last attempt, throw the error
      if (attempt === maxRetries) {
        throw err;
      }
      
      // Wait before retrying
      loadingMessage.value = `Nouvelle tentative dans ${Math.ceil(delay/1000)} secondes...`;
      console.log(`Waiting ${delay}ms before retry...`);
      await new Promise(resolve => setTimeout(resolve, delay));
      
      // Increase delay for next attempt
      delay = delay * 1.5;
    }
  }
}

function getFullLocation() {
  const parts = [];
  if (ficheData.value?.agriculteurDouar) parts.push(ficheData.value.agriculteurDouar);
  if (ficheData.value?.agriculteurCommune) parts.push(ficheData.value.agriculteurCommune);
  if (ficheData.value?.agriculteurProvince) parts.push(ficheData.value.agriculteurProvince);
  return parts.join(', ') || 'Non spécifiée';
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
  window.print();
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
/* Based on PrintableReceipt component */
@media print {
  body * {
    visibility: hidden;
  }
  
  .print-only, .print-only * {
    visibility: visible;
  }
  
  .printable-fiche {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
  
  .no-print {
    display: none !important;
  }
  
  @page {
    size: A4;
    margin: 1.5cm;
  }
}

.fiche-approbation-container {
  max-width: 1000px;
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

.fiche-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: 8px;
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
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.status-banner {
  padding: 1rem;
  margin-bottom: 1.5rem;
  background: var(--green-50);
  border: 1px solid var(--green-200);
  border-radius: 8px;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-info i {
  color: var(--green-500);
  font-size: 1.5rem;
}

/* Document styles based on PrintableReceipt */
.fiche-document {
  padding: 20px;
  font-family: Arial, sans-serif;
  background: white;
}

.print-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.header-logo {
  width: 80px;
  height: 80px;
}

.header-logo img {
  width: 100%;
  height: auto;
  object-fit: contain;
}

.header-text {
  text-align: center;
  flex: 1;
}

.header-title p {
  margin: 3px 0;
  font-size: 11px;
  line-height: 1.2;
}

.header-separator {
  border: none;
  border-top: 2px solid black;
  margin: 15px 0 25px 0;
}

.document-title {
  text-align: center;
  margin-bottom: 40px;
}

.document-title h1 {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  text-decoration: underline;
}

.document-title h2 {
  font-size: 14px;
  font-weight: bold;
  margin: 5px 0;
}

.document-title h3 {
  font-size: 12px;
  font-weight: bold;
  margin: 10px 0;
  border: 2px solid black;
  padding: 8px;
  display: inline-block;
}

.approval-date {
  font-weight: bold;
  margin-top: 10px;
}

.fiche-section {
  margin-bottom: 25px;
}

.fiche-section h3 {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 10px;
  text-decoration: underline;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 15px;
}

.info-table td {
  padding: 6px 8px;
  border: 1px solid black;
  font-size: 11px;
}

.info-table .label {
  background-color: #f0f0f0;
  font-weight: bold;
  width: 35%;
}

.info-table .value {
  width: 65%;
}

.amount-approved {
  color: #059669;
  font-weight: bold;
}

.important-notice {
  border: 2px solid black;
  padding: 15px;
  background-color: #f9f9f9;
  margin: 20px 0;
}

.important-notice h3 {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 10px;
  text-align: center;
  text-decoration: underline;
}

.important-notice ul {
  margin: 0;
  padding-left: 20px;
  font-size: 10px;
  line-height: 1.4;
}

.signatures-section {
  margin-top: 30px;
}

.signature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-top: 20px;
}

.signature-block {
  text-align: center;
}

.signature-block p {
  margin: 5px 0;
  font-size: 10px;
}

.signature-line {
  font-style: italic;
  font-size: 9px;
}

.signature-space {
  height: 50px;
  border-top: 1px solid black;
  margin: 15px 20px;
}

.footer-section {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
  padding-top: 15px;
  border-top: 1px solid black;
  font-size: 9px;
}

.contact-info p,
.signature-area p {
  margin: 2px 0;
}
</style>