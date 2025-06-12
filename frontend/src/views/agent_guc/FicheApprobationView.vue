<template>
  <div class="fiche-container p-4">
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner size="50px" />
      <div class="mt-3">{{ loadingMessage }}</div>
    </div>

    <div v-else-if="error" class="text-center py-8">
      <i class="pi pi-exclamation-triangle text-6xl text-red-500 mb-4"></i>
      <h3>Erreur</h3>
      <p class="text-600">{{ error }}</p>
      <div class="flex gap-2 justify-content-center">
        <Button label="Réessayer" icon="pi pi-refresh" @click="loadFicheData" outlined />
        <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" />
      </div>
    </div>

    <div v-else-if="ficheData" class="max-w-5xl mx-auto">
      <!-- Header Actions (no-print) -->
      <div class="flex justify-content-between align-items-center mb-4 no-print">
        <div class="flex align-items-center gap-3">
          <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" outlined />
          <Breadcrumb :model="breadcrumbItems" />
        </div>
        
        <div class="flex gap-2">
          <Button label="Imprimer" icon="pi pi-print" @click="printFiche" severity="success" />
        </div>
      </div>

      <!-- Status Banner -->
      <Message v-if="ficheData.farmersRetrieved" severity="success" class="mb-4 no-print">
        <div class="flex align-items-center gap-3">
          <i class="pi pi-check-circle text-xl"></i>
          <div>
            <strong>Fiche Générée</strong>
            <p class="mt-1 mb-0">Fiche d'approbation disponible</p>
          </div>
        </div>
      </Message>      <!-- Printable Document -->
      <div class="printable-fiche print-only">
        <!-- Header with logos and title -->
        <div class="print-header">
          <div class="header-logo left-logo">
            <img src="/src/assets/logo/logo_ministre.jpg" alt="Armoiries" />
          </div>
          <div class="header-text">
            <div class="header-title">
              <p>Royaume du Maroc</p>
              <p>Ministère de l'Agriculture, de la Pêche Maritime,</p>
              <p>du Développement Rural et des Eaux et Forêts</p>
              <p>Office Régional de Mise en Valeur Agricole du Tadla</p>
              <p><strong>Système Automatisé de Demande de Subventions Agricoles (SADSA)</strong></p>
            </div>
          </div>
          <div class="header-logo right-logo">
            <img src="/src/assets/logo/logo-ormvat-full-original.jpg" alt="ORMVAT" />
          </div>
        </div>
        
        <hr class="header-separator" />
        
        <!-- Document Title -->
        <div class="document-title">
          <h1>Fiche d'Approbation</h1>
          <h2>N° {{ ficheData.numeroFiche }}</h2>
          <h3>Date d'approbation: {{ formatDate(ficheData.dateApprobation) }}</h3>
        </div>
        
        <!-- Content Area -->
        <div class="content-area">
          <!-- Main Information Table -->
          <table class="fiche-table">
            <thead>
              <tr>
                <th colspan="4">INFORMATIONS DU BÉNÉFICIAIRE ET DU PROJET</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="label-cell">Nom et Prénom</td>
                <td>{{ ficheData.agriculteurNom }} {{ ficheData.agriculteurPrenom }}</td>
                <td class="label-cell">Référence Dossier</td>
                <td class="reference-cell">{{ ficheData.reference }}</td>
              </tr>
              <tr>
                <td class="label-cell">CIN</td>
                <td>{{ ficheData.agriculteurCin }}</td>
                <td class="label-cell">Numéro SABA</td>
                <td class="saba-cell">{{ ficheData.saba }}</td>
              </tr>
              <tr>
                <td class="label-cell">Téléphone</td>
                <td>{{ ficheData.agriculteurTelephone }}</td>
                <td class="label-cell">Type de projet</td>
                <td>{{ ficheData.rubriqueDesignation }}</td>
              </tr>
              <tr>
                <td class="label-cell">Localisation</td>
                <td>{{ getFullLocation() }}</td>
                <td class="label-cell">Description</td>
                <td>{{ ficheData.sousRubriqueDesignation }}</td>
              </tr>
            </tbody>
          </table>

          <!-- Financial Information Table -->
          <table class="fiche-table">
            <thead>
              <tr>
                <th colspan="4">INFORMATIONS FINANCIÈRES ET APPROBATION</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="label-cell">Montant demandé</td>
                <td>{{ formatCurrency(ficheData.montantDemande) }}</td>
                <td class="label-cell">Montant approuvé</td>
                <td class="approved-amount">{{ formatCurrency(ficheData.montantApprouve) }}</td>
              </tr>
              <tr>
                <td class="label-cell">Statut</td>
                <td class="status-cell">{{ ficheData.statutApprobation }}</td>
                <td class="label-cell">Validité jusqu'au</td>
                <td>{{ ficheData.validiteJusquau }}</td>
              </tr>
            </tbody>
          </table>

          <!-- Administrative Information Table -->
          <table class="fiche-table">
            <thead>
              <tr>
                <th colspan="4">INFORMATIONS ADMINISTRATIVES</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="label-cell">Antenne</td>
                <td>{{ ficheData.antenneDesignation }}</td>
                <td class="label-cell">CDA</td>
                <td>{{ ficheData.cdaNom }}</td>
              </tr>
              <tr>
                <td class="label-cell">Province</td>
                <td>{{ ficheData.agriculteurProvince }}</td>
                <td class="label-cell">Agent GUC</td>
                <td>{{ ficheData.agentGucNom }}</td>
              </tr>
            </tbody>
          </table>

          <!-- Comments and Conditions (if any) -->
          <div v-if="ficheData.commentaireApprobation || ficheData.conditionsSpecifiques">
            <table class="fiche-table">
              <thead>
                <tr>
                  <th>COMMENTAIRES ET CONDITIONS SPÉCIFIQUES</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="ficheData.commentaireApprobation">
                  <td>
                    <strong>Commentaire:</strong><br/>
                    {{ ficheData.commentaireApprobation }}
                  </td>
                </tr>
                <tr v-if="ficheData.conditionsSpecifiques">
                  <td>
                    <strong>Conditions spécifiques:</strong><br/>
                    {{ ficheData.conditionsSpecifiques }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Important Notice -->
          <table class="fiche-table notice-table">
            <thead>
              <tr>
                <th>INFORMATIONS IMPORTANTES</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <ul class="notice-list">
                    <li>Cette fiche atteste de l'approbation de votre demande de subvention agricole.</li>
                    <li>Conservez précieusement ce document pour la phase de réalisation de votre projet.</li>
                    <li>La validité de cette approbation est de 6 mois à compter de la date d'émission.</li>
                    <li>Tout changement dans les spécifications du projet nécessite une nouvelle approbation.</li>
                    <li>Vous devez respecter toutes les conditions mentionnées ci-dessus.</li>
                    <li>Présentez cette fiche lors de la mise en œuvre de votre projet.</li>
                  </ul>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Signature section -->
        <div class="signature-section">
          <div class="date-section">
            <p>Fait à {{ ficheData.antenneDesignation }}, le {{ formatDate(ficheData.dateApprobation) }}</p>
          </div>
          
          <div class="signatures-row">
            <div class="signature">
              <p>Signature de l'Agent GUC</p>
              <p class="name">{{ ficheData.agentGucNom }}</p>
              <div class="signature-line"></div>
            </div>
            <div class="signature">
              <p>Signature du Responsable ORMVAT</p>
              <p class="name">{{ ficheData.responsableNom }}</p>
              <div class="signature-line"></div>
            </div>
          </div>
          
          <div class="footer-info">
            <div class="contact-info">
              <strong>Pour toute information:</strong><br/>
              ORMVAT - BP 244, Fquih Ben Salah<br/>
              Tél: +212 5 23 43 50 23/35/48<br/>
              Email: sadsa@ormvatadla.ma
            </div>
            <div class="generation-info">
              Document généré automatiquement par SADSA<br/>
              {{ formatDateTime(ficheData.dateGeneration) }}
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

import Button from 'primevue/button';
import ProgressSpinner from 'primevue/progressspinner';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Toast from 'primevue/toast';
import Card from 'primevue/card';
import Panel from 'primevue/panel';
import Message from 'primevue/message';
import Breadcrumb from 'primevue/breadcrumb';
import Divider from 'primevue/divider';

const router = useRouter();
const route = useRoute();
const toast = useToast();

const loading = ref(true);
const error = ref(null);
const ficheData = ref(null);
const loadingMessage = ref('Chargement de la fiche...');

const dossierId = computed(() => route.params.dossierId);

const breadcrumbItems = computed(() => [
  { label: 'Dossiers GUC', route: '/agent_guc/dossiers' },
  { label: ficheData.value?.reference || 'Dossier' },
  { label: 'Fiche d\'Approbation' }
]);

onMounted(async () => {
  await loadFicheData();
});

async function loadFicheData() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await loadFicheWithRetry();
    ficheData.value = response;
    
  } catch (err) {
    if (err.message?.includes('dateApprobation')) {
      error.value = 'Le dossier n\'a pas encore été approuvé ou la fiche n\'est pas prête.';
    } else if (err.status === 404) {
      error.value = 'Fiche d\'approbation non trouvée. Assurez-vous que le dossier a été approuvé.';
    } else {
      error.value = err.message || 'Impossible de charger la fiche.';
    }
  } finally {
    loading.value = false;
  }
}

async function loadFicheWithRetry(maxRetries = 3, delay = 2000) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      loadingMessage.value = attempt === 1 ? 'Chargement de la fiche...' : `Tentative ${attempt}/${maxRetries}...`;
      
      const response = await ApiService.get(`/fiche-approbation/${dossierId.value}/print`);
      return response;
      
    } catch (err) {
      if (attempt === maxRetries) throw err;
      
      loadingMessage.value = `Nouvelle tentative dans ${Math.ceil(delay/1000)} secondes...`;
      await new Promise(resolve => setTimeout(resolve, delay));
      delay *= 1.5;
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

<style>
/* These styles will only apply when printing */
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
  
  /* Reset all styles */
  .printable-fiche * {
    color: black !important;
    background: white !important;
    font-family: Arial, sans-serif !important;
    box-shadow: none !important;
  }
  
  @page {
    size: A4;
    margin: 1cm;
  }
  
  .page-break {
    page-break-after: always;
    break-after: page;
  }
}

/* Document structure - matches PrintableNotes exactly */
.printable-fiche {
  position: relative;
  min-height: 100vh;
  padding: 20px;
  padding-bottom: 200px; /* Increased padding to make room for signature */
  box-sizing: border-box;
  page-break-after: always;
  background: white;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  margin: 20px 0;
}

/* Header styles - exactly like PrintableNotes */
.print-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.header-logo {
  width: 80px;
  height: 80px;
}

.header-logo img {
  width: 100%;
  height: auto;
}

.header-text {
  text-align: center;
}

.header-title p {
  margin: 2px 0;
  font-size: 12px;
}

.header-separator {
  border: none;
  border-top: 1px solid black;
  margin: 10px 0 20px 0;
}

/* Document title */
.document-title {
  text-align: center;
  margin-bottom: 30px;
}

.document-title h1 {
  font-size: 18px;
  margin-bottom: 5px;
}

.document-title h2, .document-title h3 {
  font-size: 14px;
  font-weight: normal;
  margin: 5px 0;
}

/* Content area */
.content-area {
  width: 100%;
  margin-bottom: 60px; /* Added margin to separate from signature */
}

/* Table styles - exactly like PrintableNotes */
.fiche-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 30px;
  page-break-inside: avoid;
}

.fiche-table th, .fiche-table td {
  border: 1px solid black;
  padding: 8px;
  text-align: center;
  font-size: 12px;
}

.fiche-table th {
  font-weight: bold;
  background-color: #f2f2f2;
}

.fiche-table tr {
  page-break-inside: avoid;
  page-break-after: auto;
}

/* Label cells */
.label-cell {
  background-color: #f9f9f9;
  font-weight: bold;
  text-align: left;
  width: 25%;
}

.fiche-table td:not(.label-cell) {
  text-align: left;
}

/* Special highlighting */
.reference-cell, .saba-cell {
  font-weight: bold;
  color: #2c3e50;
}

.approved-amount {
  background-color: #e8f5e8;
  font-weight: bold;
  color: #2d5016;
}

.status-cell {
  font-weight: bold;
  color: #27ae60;
}

/* Notice table - special styling */
.notice-table th {
  background-color: #fff3cd;
  border-color: #ffc107;
}

.notice-table td {
  border-color: #ffc107;
  text-align: left;
}

.notice-list {
  margin: 0;
  padding-left: 20px;
  list-style-type: disc;
}

.notice-list li {
  margin-bottom: 5px;
  line-height: 1.4;
}

/* Signature section at bottom of page - exactly like PrintableNotes */
.signature-section {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  background: white; /* Ensure signature section has white background */
}

.date-section {
  text-align: center;
  margin-bottom: 20px;
  font-weight: bold;
}

.signatures-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.signature {
  width: 45%;
  text-align: center;
  border: 1px solid #ddd;
  padding: 15px;
  background-color: #fafafa;
}

.signature p {
  margin: 5px 0;
  font-size: 12px;
}

.signature .name {
  font-weight: bold;
}

.signature-line {
  border-top: 1px solid black;
  height: 40px;
  margin-top: 20px;
}

.footer-info {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #666;
  border-top: 1px solid #ddd;
  padding-top: 10px;
}

.contact-info, .generation-info {
  line-height: 1.4;
}

.generation-info {
  text-align: right;
}

/* Print specific overrides */
@media print {
  .no-print { 
    display: none !important; 
  }
    .printable-fiche {
    box-shadow: none;
    border-radius: 0;
    margin: 0;
    padding: 15px;
    padding-bottom: 160px; /* Increased padding for print */
  }
  
  .fiche-table th {
    background-color: #f2f2f2 !important;
  }
  
  .label-cell {
    background-color: #f9f9f9 !important;
  }
  
  .approved-amount {
    background-color: #e8f5e8 !important;
  }
  
  .notice-table th {
    background-color: #fff3cd !important;
  }
  
  .signature {
    background-color: #fafafa !important;
  }
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .print-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .header-logo {
    width: 60px;
    height: 60px;
  }
  
  .fiche-table {
    font-size: 11px;
  }
  
  .signatures-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .signature {
    width: 100%;
  }
  
  .footer-info {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
}
</style>