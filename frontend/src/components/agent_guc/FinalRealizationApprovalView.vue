<template>
  <div class="final-realization-approval-container p-4">
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner size="50px" />
      <div class="mt-3">{{ loadingMessage }}</div>
    </div>

    <div v-else-if="error" class="text-center py-8">
      <i class="pi pi-exclamation-triangle text-6xl text-red-500 mb-4"></i>
      <h3>Erreur</h3>
      <p class="text-600">{{ error }}</p>
      <div class="flex gap-2 justify-content-center">
        <Button label="Réessayer" icon="pi pi-refresh" @click="loadDossierDetail" outlined />
        <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" />
      </div>
    </div>

    <div v-else-if="dossierDetail" class="max-w-6xl mx-auto">
      <!-- Header Actions (no-print) -->
      <div class="flex justify-content-between align-items-center mb-4 no-print">
        <div class="flex align-items-center gap-3">
          <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" outlined />
          <Breadcrumb :model="breadcrumbItems" />
        </div>
      </div>

      <!-- Already Completed Alert -->
      <Message v-if="isAlreadyCompleted()" severity="success" class="mb-4">
        <div class="flex justify-content-between align-items-center">
          <div>
            <strong>Dossier déjà traité</strong>
            <p class="mt-1 mb-0">Ce dossier a déjà été finalisé et archivé.</p>
          </div>
          <Button 
            label="Voir Archives" 
            icon="pi pi-archive" 
            @click="goToArchives"
            size="small"
          />
        </div>
      </Message>

      <!-- Dossier Summary -->
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-check-square mr-2"></i>Approbation Finale de la Réalisation
        </template>
        <template #content>
          <div class="grid">
            <div class="col-12 md:col-6">
              <Panel header="Informations Dossier">
                <div class="flex flex-column gap-2">
                  <div><strong>Référence:</strong> {{ dossierDetail.reference }}</div>
                  <div><strong>SABA:</strong> {{ dossierDetail.saba }}</div>
                  <div><strong>Agriculteur:</strong> {{ dossierDetail.agriculteur?.prenom }} {{ dossierDetail.agriculteur?.nom }}</div>
                  <div><strong>CIN:</strong> {{ dossierDetail.agriculteur?.cin }}</div>
                  <div><strong>Téléphone:</strong> {{ dossierDetail.agriculteur?.telephone }}</div>
                </div>
              </Panel>
            </div>
            
            <div class="col-12 md:col-6">
              <Panel header="Projet">
                <div class="flex flex-column gap-2">
                  <div><strong>Type:</strong> {{ dossierDetail.projet?.sousRubrique }}</div>
                  <div><strong>Rubrique:</strong> {{ dossierDetail.projet?.rubrique }}</div>
                  <div><strong>Montant approuvé:</strong> {{ formatCurrency(dossierDetail.montantSubvention) }}</div>
                  <div><strong>Antenne:</strong> {{ dossierDetail.antenne?.designation }}</div>
                  <div><strong>Localisation:</strong> {{ getFullLocation() }}</div>
                </div>
              </Panel>
            </div>
          </div>
        </template>
      </Card>

      <!-- Commission Feedback (Quick Summary) -->
      <Card class="mb-4" v-if="dossierDetail.commissionFeedback?.decisionMade">
        <template #title>
          <i class="pi pi-map-marker mr-2"></i>Résumé - Commission Visite Terrain
        </template>
        <template #content>
          <div class="flex justify-content-between align-items-center">
            <div class="flex align-items-center gap-3">
              <Tag 
                :value="dossierDetail.commissionFeedback.approved ? 'TERRAIN APPROUVÉ' : 'TERRAIN REJETÉ'" 
                :severity="dossierDetail.commissionFeedback.approved ? 'success' : 'danger'"
                class="text-lg font-bold"
              />
              <div class="text-sm text-600">
                <i class="pi pi-calendar mr-1"></i>
                {{ formatDate(dossierDetail.commissionFeedback.dateVisite) }}
              </div>
            </div>
            <div class="text-sm text-600" v-if="dossierDetail.commissionFeedback.agentCommissionName">
              <i class="pi pi-user mr-1"></i>
              {{ dossierDetail.commissionFeedback.agentCommissionName }}
            </div>
          </div>
          <div v-if="dossierDetail.commissionFeedback.conclusion" class="mt-3 p-3 bg-gray-50 border-round">
            <strong>Conclusion Commission:</strong> {{ dossierDetail.commissionFeedback.conclusion }}
          </div>
        </template>
      </Card>

      <!-- Service Technique Feedback (Main Focus) -->
      <Card class="mb-4" v-if="dossierDetail.serviceTechniqueFeedback">
        <template #title>
          <i class="pi pi-cog mr-2"></i>Rapport du Service Technique
        </template>
        <template #content>
          <div v-if="dossierDetail.serviceTechniqueFeedback.decisionMade">
            <!-- Service Technique Decision -->
            <div class="flex justify-content-between align-items-center mb-4">
              <div class="flex align-items-center gap-3">
                <Tag 
                  :value="dossierDetail.serviceTechniqueFeedback.approved ? 'RÉALISATION APPROUVÉE' : 'RÉALISATION REJETÉE'" 
                  :severity="dossierDetail.serviceTechniqueFeedback.approved ? 'success' : 'danger'"
                  class="text-lg font-bold"
                />
                <div class="text-sm text-600">
                  <i class="pi pi-calendar mr-1"></i>
                  Visite effectuée le {{ formatDate(dossierDetail.serviceTechniqueFeedback.dateVisite) }}
                </div>
              </div>
              <div class="text-sm text-600" v-if="dossierDetail.serviceTechniqueFeedback.agentServiceTechniqueName">
                <i class="pi pi-user mr-1"></i>
                {{ dossierDetail.serviceTechniqueFeedback.agentServiceTechniqueName }}
              </div>
            </div>

            <!-- Service Technique Details -->
            <div class="grid">
              <!-- Progress and Status -->
              <div class="col-12 md:col-4" v-if="dossierDetail.serviceTechniqueFeedback.pourcentageAvancement">
                <h5>Avancement du Projet</h5>
                <div class="flex align-items-center gap-2 mb-2">
                  <ProgressBar 
                    :value="dossierDetail.serviceTechniqueFeedback.pourcentageAvancement" 
                    class="flex-1"
                    :pt="{ value: { style: 'background: linear-gradient(to right, #4CAF50, #8BC34A)' } }"
                  />
                  <span class="font-bold">{{ dossierDetail.serviceTechniqueFeedback.pourcentageAvancement }}%</span>
                </div>
                <div class="text-sm text-600">
                  Statut: {{ getStatutVisiteLabel(dossierDetail.serviceTechniqueFeedback.statutVisite) }}
                </div>
              </div>

              <!-- Observations -->
              <div class="col-12 md:col-8" v-if="dossierDetail.serviceTechniqueFeedback.observations">
                <h5>Observations de la Visite</h5>
                <div class="p-3 bg-blue-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.observations }}
                </div>
              </div>

              <!-- Recommendations -->
              <div class="col-12 md:col-6" v-if="dossierDetail.serviceTechniqueFeedback.recommandations">
                <h5>Recommandations</h5>
                <div class="p-3 bg-green-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.recommandations }}
                </div>
              </div>

              <!-- Problems Detected -->
              <div class="col-12 md:col-6" v-if="dossierDetail.serviceTechniqueFeedback.problemesDetectes">
                <h5>Problèmes Détectés</h5>
                <div class="p-3 bg-orange-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.problemesDetectes }}
                </div>
              </div>

              <!-- Corrective Actions -->
              <div class="col-12" v-if="dossierDetail.serviceTechniqueFeedback.actionsCorrectives">
                <h5>Actions Correctives Prises</h5>
                <div class="p-3 bg-purple-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.actionsCorrectives }}
                </div>
              </div>

              <!-- Conclusion -->
              <div class="col-12" v-if="dossierDetail.serviceTechniqueFeedback.conclusion">
                <h5>Conclusion du Service Technique</h5>
                <div class="p-3 border-round" :class="{
                  'bg-green-50': dossierDetail.serviceTechniqueFeedback.approved,
                  'bg-red-50': !dossierDetail.serviceTechniqueFeedback.approved
                }">
                  {{ dossierDetail.serviceTechniqueFeedback.conclusion }}
                </div>
              </div>

              <!-- Technical Details -->
              <div class="col-12" v-if="hasVisitDetails()">
                <h5>Détails de la Visite</h5>
                <div class="grid">
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.coordonneesGPS">
                    <strong>Coordonnées GPS:</strong><br>
                    <small>{{ dossierDetail.serviceTechniqueFeedback.coordonneesGPS }}</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.dureeVisite">
                    <strong>Durée:</strong><br>
                    <small>{{ dossierDetail.serviceTechniqueFeedback.dureeVisite }} minutes</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.dateConstat">
                    <strong>Date constat:</strong><br>
                    <small>{{ formatDate(dossierDetail.serviceTechniqueFeedback.dateConstat) }}</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Photos Section -->
            <div v-if="dossierDetail.serviceTechniqueFeedback.photosUrls && dossierDetail.serviceTechniqueFeedback.photosUrls.length > 0" class="mt-4">
              <h5>Photos de la Réalisation</h5>
              <div class="flex gap-2 flex-wrap">
                <div v-for="(photo, index) in dossierDetail.serviceTechniqueFeedback.photosUrls" :key="index" class="border-round overflow-hidden">
                  <img :src="photo" :alt="`Photo réalisation ${index + 1}`" class="w-8rem h-6rem object-cover cursor-pointer" 
                       @click="openPhotoDialog(photo)" />
                </div>
              </div>
            </div>
          </div>
          
          <!-- No Service Technique Decision Yet -->
          <div v-else class="text-center py-4">
            <i class="pi pi-clock text-4xl text-orange-500 mb-3"></i>
            <h4>En attente de la décision du Service Technique</h4>
            <p class="text-600">Le Service Technique n'a pas encore rendu sa décision concernant la réalisation du projet.</p>
            <Tag value="RÉALISATION EN COURS" severity="warning" />
          </div>
        </template>
      </Card>

      <!-- Decision Form -->
      <Card v-if="!isAlreadyCompleted() && canMakeDecision()">
        <template #title>
          <i class="pi pi-gavel mr-2"></i>Votre Décision Finale sur la Réalisation
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

              <div class="col-12 md:col-6" v-if="formData.decision">
                <div class="field">
                  <label for="notesArchivage">Notes d'Archivage</label>
                  <InputText 
                    id="notesArchivage"
                    v-model="formData.notesArchivage" 
                    placeholder="Notes pour l'archivage..."
                    class="w-full"
                  />
                </div>
              </div>
            </div>

            <div class="grid">
              <div class="col-12 md:col-6">
                <div class="field">
                  <label for="commentaireApprobation">{{ formData.decision === 'approve' ? 'Commentaire d\'Approbation' : 'Motif du Rejet' }} *</label>
                  <Textarea 
                    id="commentaireApprobation"
                    v-model="formData.commentaireApprobation" 
                    rows="4"
                    class="w-full"
                    :placeholder="formData.decision === 'approve' ? 'Commentaires sur l\'approbation finale...' : 'Motifs du rejet de la réalisation...'"
                    :class="{ 'p-invalid': !formData.commentaireApprobation?.trim() }"
                  />
                </div>
              </div>

              <div class="col-12 md:col-6">
                <div class="field">
                  <label for="observationsFinales">Observations Finales</label>
                  <Textarea 
                    id="observationsFinales"
                    v-model="formData.observationsFinales" 
                    rows="4"
                    class="w-full"
                    placeholder="Observations générales sur l'ensemble du processus..."
                  />
                </div>
              </div>
            </div>

            <!-- Decision Summary -->
            <div v-if="formData.decision" class="mt-4 p-3 border-round" :class="{
              'bg-green-50 border-green-200': formData.decision === 'approve',
              'bg-red-50 border-red-200': formData.decision === 'reject'
            }">
              <h5 class="mt-0">Résumé de la décision finale</h5>
              <div v-if="formData.decision === 'approve'">
                <p class="mb-2"><strong>Action:</strong> Approbation finale de la réalisation</p>
                <p class="mb-2"><strong>Service Technique a dit:</strong> {{ getServiceTechniqueDecisionSummary() }}</p>
                <p class="mb-0"><strong>Prochaine étape:</strong> Archivage définitif du dossier - Processus terminé</p>
              </div>
              <div v-else>
                <p class="mb-2"><strong>Action:</strong> Rejet final de la réalisation</p>
                <p class="mb-2"><strong>Service Technique avait dit:</strong> {{ getServiceTechniqueDecisionSummary() }}</p>
                <p class="mb-0"><strong>Prochaine étape:</strong> Archivage en rejet - Dossier fermé</p>
              </div>
            </div>

            <div class="flex justify-content-end gap-2 pt-4">
              <Button 
                label="Annuler" 
                icon="pi pi-times" 
                @click="goBack"
                outlined
                :disabled="submitting"
              />
              <Button 
                type="submit" 
                :label="getSubmitButtonLabel()"
                :icon="getSubmitButtonIcon()"
                :severity="getSubmitButtonSeverity()"
                :loading="submitting"
                :disabled="!isFormValid()"
              />
            </div>
          </form>
        </template>
      </Card>

      <!-- Cannot Make Decision (Service Technique not done) -->
      <Card v-else-if="!isAlreadyCompleted() && !canMakeDecision()">
        <template #content>
          <div class="text-center py-6">
            <i class="pi pi-clock text-6xl text-orange-500 mb-4"></i>
            <h3>Décision en attente</h3>
            <p class="text-600 mb-4">
              Vous ne pouvez pas prendre de décision finale tant que le Service Technique n'a pas terminé son évaluation de la réalisation.
            </p>
            <Button 
              label="Actualiser" 
              icon="pi pi-refresh" 
              @click="loadDossierDetail"
              class="p-button-outlined"
            />
          </div>
        </template>
      </Card>
    </div>

    <!-- Photo Dialog -->
    <Dialog v-model:visible="photoDialog.visible" modal :style="{ width: '80vw', height: '80vh' }" header="Photo de la Réalisation">
      <img :src="photoDialog.url" alt="Photo réalisation" class="w-full h-full object-contain" />
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import Card from 'primevue/card';
import Panel from 'primevue/panel';
import Select from 'primevue/select';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import Toast from 'primevue/toast';
import Message from 'primevue/message';
import Breadcrumb from 'primevue/breadcrumb';
import Dialog from 'primevue/dialog';

const router = useRouter();
const route = useRoute();
const toast = useToast();

const loading = ref(true);
const error = ref(null);
const submitting = ref(false);
const dossierDetail = ref(null);
const loadingMessage = ref('Chargement du dossier...');

const photoDialog = ref({
  visible: false,
  url: ''
});

const formData = reactive({
  decision: null,
  commentaireApprobation: '',
  observationsFinales: '',
  notesArchivage: ''
});

const decisionOptions = [
  { label: 'Approuver la réalisation', value: 'approve' },
  { label: 'Rejeter la réalisation', value: 'reject' }
];

const dossierId = computed(() => route.params.dossierId);

const breadcrumbItems = computed(() => [
  { label: 'Dossiers GUC', route: '/agent_guc/dossiers' },
  { label: dossierDetail.value?.reference || 'Dossier' },
  { label: 'Approbation Finale Réalisation' }
]);

onMounted(async () => {
  await loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await loadDossierWithRetry();
    dossierDetail.value = response;
    
    // Check if already completed
    if (isAlreadyCompleted()) {
      toast.add({
        severity: 'info',
        summary: 'Information',
        detail: 'Ce dossier a déjà été traité et archivé',
        life: 3000
      });
    }
    
    // Initialize form based on Service Technique feedback
    if (response.serviceTechniqueFeedback?.observations) {
      formData.observationsFinales = `Service Technique: ${response.serviceTechniqueFeedback.observations}`;
      if (response.serviceTechniqueFeedback.recommandations) {
        formData.observationsFinales += `\nRecommandations: ${response.serviceTechniqueFeedback.recommandations}`;
      }
    }
    
  } catch (err) {
    if (err.message?.includes('phase')) {
      error.value = 'Le dossier n\'est pas en phase d\'approbation finale de la réalisation.';
    } else if (err.status === 404) {
      error.value = 'Dossier non trouvé.';
    } else {
      error.value = err.message || 'Impossible de charger le dossier.';
    }
  } finally {
    loading.value = false;
  }
}

async function loadDossierWithRetry(maxRetries = 3, delay = 2000) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      loadingMessage.value = attempt === 1 ? 'Chargement du dossier...' : `Tentative ${attempt}/${maxRetries}...`;
      
      const response = await ApiService.get(`/agent-guc/dossiers/detail/${dossierId.value}`);
      return response;
      
    } catch (err) {
      if (attempt === maxRetries) throw err;
      
      loadingMessage.value = `Nouvelle tentative dans ${Math.ceil(delay/1000)} secondes...`;
      await new Promise(resolve => setTimeout(resolve, delay));
      delay *= 1.5;
    }
  }
}

function onDecisionChange() {
  if (formData.decision === 'approve') {
    formData.commentaireApprobation = '';
  } else {
    formData.commentaireApprobation = '';
  }
}

function isFormValid() {
  return formData.decision && formData.commentaireApprobation?.trim();
}

function canMakeDecision() {
  return dossierDetail.value?.serviceTechniqueFeedback?.decisionMade;
}

function isAlreadyCompleted() {
  if (!dossierDetail.value) return false;
  
  return dossierDetail.value.statut === 'COMPLETED' || 
         dossierDetail.value.statut === 'REJECTED';
}

function hasVisitDetails() {
  const feedback = dossierDetail.value?.serviceTechniqueFeedback;
  return feedback?.coordonneesGPS || feedback?.dureeVisite || feedback?.dateConstat;
}

function getFullLocation() {
  const agriculteur = dossierDetail.value?.agriculteur;
  if (!agriculteur) return 'Non spécifiée';
  
  const parts = [];
  if (agriculteur.douar) parts.push(agriculteur.douar);
  if (agriculteur.communeRurale) parts.push(agriculteur.communeRurale);
  return parts.join(', ') || 'Non spécifiée';
}

function getServiceTechniqueDecisionSummary() {
  const feedback = dossierDetail.value?.serviceTechniqueFeedback;
  if (!feedback?.decisionMade) return 'Pas encore d\'évaluation';
  
  const status = feedback.approved ? 'Approuvé' : 'Rejeté';
  const progress = feedback.pourcentageAvancement ? ` (${feedback.pourcentageAvancement}% réalisé)` : '';
  return `${status}${progress}`;
}

function getStatutVisiteLabel(statut) {
  const labels = {
    'PROGRAMMEE': 'Programmée',
    'EN_COURS': 'En cours',
    'TERMINEE': 'Terminée',
    'REPORTEE': 'Reportée',
    'ANNULEE': 'Annulée'
  };
  return labels[statut] || statut;
}

function getSubmitButtonLabel() {
  return formData.decision === 'approve' ? 'Approuver et Archiver' : 'Rejeter et Archiver';
}

function getSubmitButtonIcon() {
  return formData.decision === 'approve' ? 'pi pi-check-circle' : 'pi pi-times-circle';
}

function getSubmitButtonSeverity() {
  return formData.decision === 'approve' ? 'success' : 'danger';
}

async function submitDecision() {
  if (!isFormValid()) return;
  
  try {
    submitting.value = true;
    
    const requestData = {
      dossierId: parseInt(dossierId.value),
      approved: formData.decision === 'approve',
      commentaireApprobation: formData.commentaireApprobation,
      motifRejet: formData.decision === 'reject' ? formData.commentaireApprobation : null,
      observationsFinales: formData.observationsFinales,
      notesArchivage: formData.notesArchivage
    };

    const response = await ApiService.post('/agent-guc/dossiers/final-realization-approval', requestData);
    
    if (response.success !== false) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Décision prise avec succès',
        life: 4000
      });
      
      setTimeout(() => {
        router.push('/agent_guc/dossiers');
      }, 2000);
    }
    
  } catch (err) {
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

function openPhotoDialog(photoUrl) {
  photoDialog.value.url = photoUrl;
  photoDialog.value.visible = true;
}

function goBack() {
  router.push('/agent_guc/dossiers');
}

function goToArchives() {
  // TODO: Implement archives view
  toast.add({
    severity: 'info',
    summary: 'Fonctionnalité à venir',
    detail: 'La consultation des archives sera disponible prochainement',
    life: 3000
  });
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
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(date));
}
</script>

<style scoped>
.final-realization-approval-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.field-group > div {
  margin-bottom: 0.5rem;
}

.field-group strong {
  color: var(--text-color);
}

@media print {
  .no-print {
    display: none !important;
  }
}
</style>