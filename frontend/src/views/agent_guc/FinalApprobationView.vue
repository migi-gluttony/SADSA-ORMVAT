<template>
  <div class="final-approval-container p-4">
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner size="50px" />
      <div class="mt-3">Chargement du dossier...</div>
    </div>

    <div v-else-if="error" class="text-center py-8">
      <i class="pi pi-exclamation-triangle text-6xl text-red-500 mb-4"></i>
      <h3>Erreur</h3>
      <p class="text-600">{{ error }}</p>
      <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" outlined />
    </div>

    <div v-else-if="dossierDetail" class="max-w-6xl mx-auto">
      <!-- Header -->
      <div class="flex justify-content-between align-items-center mb-4">
        <div class="flex align-items-center gap-3">
          <Button 
            label="Retour aux dossiers" 
            icon="pi pi-arrow-left" 
            @click="goBack"
            outlined
          />
          <Breadcrumb :model="breadcrumbItems" />
        </div>
      </div>

      <!-- Already Approved Alert -->
      <Message v-if="isAlreadyApproved()" severity="success" class="mb-4">
        <div class="flex justify-content-between align-items-center">
          <div>
            <strong>Dossier déjà approuvé</strong>
            <p class="mt-1 mb-0">Vous pouvez consulter la fiche d'approbation.</p>
          </div>
          <Button 
            label="Voir la Fiche" 
            icon="pi pi-file-text" 
            @click="router.push(`/agent_guc/dossiers/${dossierId}/fiche`)"
            size="small"
          />
        </div>
      </Message>

      <!-- Dossier Summary -->
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-gavel mr-2"></i>Décision Finale d'Approbation
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
                  <div><strong>Montant demandé:</strong> {{ formatCurrency(dossierDetail.montantSubvention) }}</div>
                  <div><strong>Antenne:</strong> {{ dossierDetail.antenne?.designation }}</div>
                  <div><strong>Localisation:</strong> {{ getFullLocation() }}</div>
                </div>
              </Panel>
            </div>
          </div>
        </template>
      </Card>

      <!-- Commission Feedback -->
      <Card class="mb-4" v-if="dossierDetail.commissionFeedback">
        <template #title>
          <i class="pi pi-map-marker mr-2"></i>Retour de la Commission Visite Terrain
        </template>
        <template #content>
          <div v-if="dossierDetail.commissionFeedback.decisionMade">
            <!-- Commission Decision -->
            <div class="flex justify-content-between align-items-center mb-4">
              <div class="flex align-items-center gap-3">
                <Tag 
                  :value="dossierDetail.commissionFeedback.approved ? 'TERRAIN APPROUVÉ' : 'TERRAIN REJETÉ'" 
                  :severity="dossierDetail.commissionFeedback.approved ? 'success' : 'danger'"
                  class="text-lg font-bold"
                />
                <div class="text-sm text-600">
                  <i class="pi pi-calendar mr-1"></i>
                  Visite effectuée le {{ formatDate(dossierDetail.commissionFeedback.dateVisite) }}
                </div>
              </div>
              <div class="text-sm text-600" v-if="dossierDetail.commissionFeedback.agentCommissionName">
                <i class="pi pi-user mr-1"></i>
                Par: {{ dossierDetail.commissionFeedback.agentCommissionName }}
              </div>
            </div>

            <!-- Commission Details -->
            <div class="grid">
              <div class="col-12 md:col-6" v-if="dossierDetail.commissionFeedback.observations">
                <h5>Observations Terrain</h5>
                <div class="p-3 bg-gray-50 border-round">
                  {{ dossierDetail.commissionFeedback.observations }}
                </div>
              </div>
              
              <div class="col-12 md:col-6" v-if="dossierDetail.commissionFeedback.recommandations">
                <h5>Recommandations</h5>
                <div class="p-3 bg-blue-50 border-round">
                  {{ dossierDetail.commissionFeedback.recommandations }}
                </div>
              </div>

              <div class="col-12" v-if="dossierDetail.commissionFeedback.conclusion">
                <h5>Conclusion de la Commission</h5>
                <div class="p-3 border-round" :class="{
                  'bg-green-50': dossierDetail.commissionFeedback.approved,
                  'bg-red-50': !dossierDetail.commissionFeedback.approved
                }">
                  {{ dossierDetail.commissionFeedback.conclusion }}
                </div>
              </div>

              <!-- Technical Details -->
              <div class="col-12" v-if="hasVisitDetails()">
                <h5>Détails de la Visite</h5>
                <div class="grid">
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.coordonneesGPS">
                    <strong>Coordonnées GPS:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.coordonneesGPS }}</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.dureeVisite">
                    <strong>Durée:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.dureeVisite }} minutes</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.conditionsMeteo">
                    <strong>Conditions météo:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.conditionsMeteo }}</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Photos Section -->
            <div v-if="dossierDetail.commissionFeedback.photosUrls && dossierDetail.commissionFeedback.photosUrls.length > 0" class="mt-4">
              <h5>Photos du Terrain</h5>
              <div class="flex gap-2 flex-wrap">
                <div v-for="(photo, index) in dossierDetail.commissionFeedback.photosUrls" :key="index" class="border-round overflow-hidden">
                  <img :src="photo" :alt="`Photo terrain ${index + 1}`" class="w-8rem h-6rem object-cover cursor-pointer" 
                       @click="openPhotoDialog(photo)" />
                </div>
              </div>
            </div>
          </div>
          
          <!-- No Commission Decision Yet -->
          <div v-else class="text-center py-4">
            <i class="pi pi-clock text-4xl text-orange-500 mb-3"></i>
            <h4>En attente de la décision de la Commission</h4>
            <p class="text-600">La commission n'a pas encore rendu sa décision concernant la visite terrain.</p>
            <Tag value="VISITE EN COURS" severity="warning" />
          </div>
        </template>
      </Card>

      <!-- Decision Form -->
      <Card v-if="!isAlreadyApproved() && canMakeDecision()">
        <template #title>
          <i class="pi pi-balance-scale mr-2"></i>Votre Décision Finale
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
                    mode="currency"
                    currency="MAD"
                    locale="fr-MA"
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
                    class="w-full"
                    placeholder="Commentaires sur l'approbation..."
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
                    class="w-full"
                    placeholder="Conditions particulières à respecter..."
                  />
                </div>
              </div>
            </div>

            <div v-if="formData.decision === 'reject'" class="field">
              <label for="motifRejet">Motif de Rejet *</label>
              <Textarea 
                id="motifRejet"
                v-model="formData.motifRejet" 
                rows="4"
                class="w-full"
                placeholder="Expliquez clairement les motifs du rejet..."
                :class="{ 'p-invalid': formData.decision === 'reject' && !formData.motifRejet?.trim() }"
              />
            </div>

            <div class="field">
              <label for="observationsCommission">Résumé des Observations Commission</label>
              <Textarea 
                id="observationsCommission"
                v-model="formData.observationsCommission" 
                rows="2"
                class="w-full"
                placeholder="Synthèse des observations de la commission..."
              />
            </div>

            <!-- Decision Summary -->
            <div v-if="formData.decision" class="mt-4 p-3 border-round" :class="{
              'bg-green-50 border-green-200': formData.decision === 'approve',
              'bg-red-50 border-red-200': formData.decision === 'reject'
            }">
              <h5 class="mt-0">Résumé de la décision</h5>
              <div v-if="formData.decision === 'approve'">
                <p class="mb-2"><strong>Action:</strong> Approbation du dossier</p>
                <p class="mb-2"><strong>Montant approuvé:</strong> {{ formatCurrency(formData.montantApprouve) }}</p>
                <p class="mb-0"><strong>Prochaine étape:</strong> Génération de la fiche d'approbation et envoi à l'antenne pour la phase de réalisation</p>
              </div>
              <div v-else>
                <p class="mb-2"><strong>Action:</strong> Rejet définitif du dossier</p>
                <p class="mb-0"><strong>Prochaine étape:</strong> Génération de la fiche de rejet et notification à l'antenne</p>
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

      <!-- Cannot Make Decision (Commission not done) -->
      <Card v-else-if="!isAlreadyApproved() && !canMakeDecision()">
        <template #content>
          <div class="text-center py-6">
            <i class="pi pi-clock text-6xl text-orange-500 mb-4"></i>
            <h3>Décision en attente</h3>
            <p class="text-600 mb-4">
              Vous ne pouvez pas prendre de décision finale tant que la Commission n'a pas rendu son verdict sur la visite terrain.
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
    <Dialog v-model:visible="photoDialog.visible" modal :style="{ width: '80vw', height: '80vh' }" header="Photo du Terrain">
      <img :src="photoDialog.url" alt="Photo terrain" class="w-full h-full object-contain" />
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
import InputNumber from 'primevue/inputnumber';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
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

const photoDialog = ref({
  visible: false,
  url: ''
});

const formData = reactive({
  decision: null,
  montantApprouve: null,
  montantDemande: 0,
  commentaireApprobation: '',
  conditionsSpecifiques: '',
  motifRejet: '',
  observationsCommission: ''
});

const decisionOptions = [
  { label: 'Approuver le dossier', value: 'approve' },
  { label: 'Rejeter le dossier', value: 'reject' }
];

const dossierId = computed(() => route.params.dossierId);

const breadcrumbItems = computed(() => [
  { label: 'Dossiers GUC', route: '/agent_guc/dossiers' },
  { label: dossierDetail.value?.reference || 'Dossier' },
  { label: 'Décision Finale' }
]);

onMounted(async () => {
  await loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get(`/agent-guc/dossiers/detail/${dossierId.value}`);
    dossierDetail.value = response;
    
    // Check if already approved
    if (isAlreadyApproved()) {
      toast.add({
        severity: 'info',
        summary: 'Information',
        detail: 'Ce dossier a déjà été traité',
        life: 3000
      });
    }
    
    // Initialize form with commission observations
    formData.montantDemande = response.montantSubvention;
    formData.montantApprouve = response.montantSubvention;
    
    if (response.commissionFeedback?.observations) {
      formData.observationsCommission = `Observations terrain: ${response.commissionFeedback.observations}`;
      if (response.commissionFeedback.recommandations) {
        formData.observationsCommission += `\nRecommandations: ${response.commissionFeedback.recommandations}`;
      }
    }
    
  } catch (err) {
    error.value = err.message || 'Impossible de charger le dossier';
  } finally {
    loading.value = false;
  }
}

function onDecisionChange() {
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
    return formData.motifRejet?.trim();
  }
}

function canMakeDecision() {
  return dossierDetail.value?.commissionFeedback?.decisionMade;
}

function getSubmitButtonLabel() {
  return formData.decision === 'approve' ? 'Approuver et Générer Fiche' : 'Rejeter le Dossier';
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
      motifRejet: formData.motifRejet,
      montantApprouve: formData.montantApprouve,
      conditionsSpecifiques: formData.conditionsSpecifiques,
      observationsCommission: formData.observationsCommission
    };

    const response = await ApiService.post('/agent-guc/dossiers/final-approval', requestData);
    
    if (response.success !== false) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Décision prise avec succès',
        life: 4000
      });
      
      if (formData.decision === 'approve') {
        setTimeout(() => {
          router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
        }, 2000);
      } else {
        setTimeout(() => {
          router.push('/agent_guc/dossiers');
        }, 2000);
      }
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

function isAlreadyApproved() {
  if (!dossierDetail.value) return false;
  
  return dossierDetail.value.statut === 'APPROVED' || 
         dossierDetail.value.statut === 'AWAITING_FARMER' ||
         dossierDetail.value.dateApprobation || 
         dossierDetail.value.statut === 'COMPLETED' ||
         dossierDetail.value.statut === 'REJECTED';
}

function hasVisitDetails() {
  const feedback = dossierDetail.value?.commissionFeedback;
  return feedback?.coordonneesGPS || feedback?.dureeVisite || feedback?.conditionsMeteo;
}

function getFullLocation() {
  const agriculteur = dossierDetail.value?.agriculteur;
  if (!agriculteur) return 'Non spécifiée';
  
  const parts = [];
  if (agriculteur.douar) parts.push(agriculteur.douar);
  if (agriculteur.communeRurale) parts.push(agriculteur.communeRurale);
  return parts.join(', ') || 'Non spécifiée';
}

function openPhotoDialog(photoUrl) {
  photoDialog.value.url = photoUrl;
  photoDialog.value.visible = true;
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
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(date));
}
</script>

<style scoped>
.final-approval-container {
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