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
      </Message>

      <!-- Printable Document -->
      <Card class="print-document">
        <template #content>
          <!-- Print Header -->
          <div class="print-header text-center mb-4">
            <div class="flex justify-content-between align-items-center mb-3">
              <img src="/src/assets/logo/logo_ministre.jpg" alt="Armoiries" class="h-4rem" />
              <div class="text-center">
                <div class="font-bold">Royaume du Maroc</div>
                <div class="text-sm">Ministère de l'Agriculture, de la Pêche Maritime</div>
                <div class="text-sm">et du Développement Rural et des Eaux et Forêts</div>
                <div class="font-bold">OFFICE RÉGIONAL DE MISE EN VALEUR AGRICOLE DU TADLA</div>
              </div>
              <img src="/src/assets/logo/logo-ormvat-full-original.jpg" alt="ORMVAT" class="h-4rem" />
            </div>
            
            <Divider />
            
            <div class="my-4">
              <h1 class="text-xl font-bold">Système Automatisé de Demande de Subventions Agricoles (SADSA)</h1>
              <h2 class="text-lg font-bold mt-2">FICHE D'APPROBATION</h2>
              <div class="inline-block border-2 border-primary p-2 mt-2">
                <strong>N° {{ ficheData.numeroFiche }}</strong>
              </div>
              <p class="mt-2 font-bold">Date d'approbation: {{ formatDate(ficheData.dateApprobation) }}</p>
            </div>
          </div>

          <!-- Content Sections -->
          <div class="grid">
            <!-- Beneficiary Info -->
            <div class="col-12 mb-4">
              <Panel header="INFORMATIONS DU BÉNÉFICIAIRE">
                <div class="grid">
                  <div class="col-6"><strong>Nom et Prénom:</strong></div>
                  <div class="col-6">{{ ficheData.agriculteurNom }} {{ ficheData.agriculteurPrenom }}</div>
                  <div class="col-6"><strong>CIN:</strong></div>
                  <div class="col-6">{{ ficheData.agriculteurCin }}</div>
                  <div class="col-6"><strong>Téléphone:</strong></div>
                  <div class="col-6">{{ ficheData.agriculteurTelephone }}</div>
                  <div class="col-6"><strong>Localisation:</strong></div>
                  <div class="col-6">{{ getFullLocation() }}</div>
                </div>
              </Panel>
            </div>

            <!-- Project Info -->
            <div class="col-12 mb-4">
              <Panel header="INFORMATIONS DU PROJET">
                <div class="grid">
                  <div class="col-6"><strong>Référence Dossier:</strong></div>
                  <div class="col-6">{{ ficheData.reference }}</div>
                  <div class="col-6"><strong>Numéro SABA:</strong></div>
                  <div class="col-6"><strong>{{ ficheData.saba }}</strong></div>
                  <div class="col-6"><strong>Type de projet:</strong></div>
                  <div class="col-6">{{ ficheData.rubriqueDesignation }}</div>
                  <div class="col-6"><strong>Description détaillée:</strong></div>
                  <div class="col-6">{{ ficheData.sousRubriqueDesignation }}</div>
                  <div class="col-6"><strong>Montant demandé:</strong></div>
                  <div class="col-6">{{ formatCurrency(ficheData.montantDemande) }}</div>
                  <div class="col-6"><strong>Montant approuvé:</strong></div>
                  <div class="col-6 text-green-600 font-bold">{{ formatCurrency(ficheData.montantApprouve) }}</div>
                </div>
              </Panel>
            </div>

            <!-- Approval Details -->
            <div class="col-12 mb-4">
              <Panel header="DÉTAILS DE L'APPROBATION">
                <div class="grid">
                  <div class="col-6"><strong>Statut:</strong></div>
                  <div class="col-6"><strong>{{ ficheData.statutApprobation }}</strong></div>
                  <div class="col-6"><strong>Date d'approbation:</strong></div>
                  <div class="col-6">{{ formatDate(ficheData.dateApprobation) }}</div>
                  <div class="col-6"><strong>Validité:</strong></div>
                  <div class="col-6">{{ ficheData.validiteJusquau }}</div>
                  <div v-if="ficheData.commentaireApprobation" class="col-6"><strong>Commentaire:</strong></div>
                  <div v-if="ficheData.commentaireApprobation" class="col-6">{{ ficheData.commentaireApprobation }}</div>
                  <div v-if="ficheData.conditionsSpecifiques" class="col-6"><strong>Conditions spécifiques:</strong></div>
                  <div v-if="ficheData.conditionsSpecifiques" class="col-6">{{ ficheData.conditionsSpecifiques }}</div>
                </div>
              </Panel>
            </div>

            <!-- Administrative Info -->
            <div class="col-12 mb-4">
              <Panel header="INFORMATIONS ADMINISTRATIVES">
                <div class="grid">
                  <div class="col-6"><strong>Antenne:</strong></div>
                  <div class="col-6">{{ ficheData.antenneDesignation }}</div>
                  <div class="col-6"><strong>CDA:</strong></div>
                  <div class="col-6">{{ ficheData.cdaNom }}</div>
                  <div class="col-6"><strong>Province:</strong></div>
                  <div class="col-6">{{ ficheData.agriculteurProvince }}</div>
                  <div class="col-6"><strong>Agent GUC:</strong></div>
                  <div class="col-6">{{ ficheData.agentGucNom }}</div>
                </div>
              </Panel>
            </div>

            <!-- Important Notice -->
            <div class="col-12 mb-4">
              <Panel header="INFORMATIONS IMPORTANTES" class="border-2 border-primary">
                <ul class="list-disc pl-4 text-sm line-height-3">
                  <li>Cette fiche atteste de l'approbation de votre demande de subvention agricole.</li>
                  <li>Conservez précieusement ce document pour la phase de réalisation de votre projet.</li>
                  <li>La validité de cette approbation est de 6 mois à compter de la date d'émission.</li>
                  <li>Tout changement dans les spécifications du projet nécessite une nouvelle approbation.</li>
                  <li>Vous devez respecter toutes les conditions mentionnées ci-dessus.</li>
                  <li>Présentez cette fiche lors de la mise en œuvre de votre projet.</li>
                </ul>
              </Panel>
            </div>

            <!-- Signatures -->
            <div class="col-12">
              <div class="grid">
                <div class="col-6 text-center">
                  <div class="border-1 border-300 p-3">
                    <strong>Agent GUC</strong>
                    <p class="mt-2">{{ ficheData.agentGucNom }}</p>
                    <p class="text-sm italic">{{ ficheData.agentGucSignature }}</p>
                    <div class="h-3rem border-top-1 border-900 mt-3"></div>
                  </div>
                </div>
                <div class="col-6 text-center">
                  <div class="border-1 border-300 p-3">
                    <strong>Responsable ORMVAT</strong>
                    <p class="mt-2">{{ ficheData.responsableNom }}</p>
                    <p class="text-sm italic">{{ ficheData.responsableSignature }}</p>
                    <div class="h-3rem border-top-1 border-900 mt-3"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="flex justify-content-between align-items-end mt-4 pt-3 border-top-1 border-300">
            <div class="text-xs">
              <strong>Pour toute information:</strong><br/>
              ORMVAT - BP 244, Fquih Ben Salah<br/>
              Tél: +212 5 23 43 50 23/35/48<br/>
              Email: sadsa@ormvatadla.ma
            </div>
            <div class="text-xs text-right">
              Fait à {{ ficheData.antenneDesignation }}, le {{ formatDate(ficheData.dateApprobation) }}<br/>
              Document généré automatiquement par SADSA<br/>
              {{ formatDateTime(ficheData.dateGeneration) }}
            </div>
          </div>
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
@media print {
  .no-print { display: none !important; }
  .print-document { box-shadow: none !important; border: none !important; }
  body * { visibility: hidden; }
  .print-document, .print-document * { visibility: visible; }
  .print-document { position: absolute; left: 0; top: 0; width: 100%; }
  @page { size: A4; margin: 1.5cm; }
}
</style>