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
          <i class="pi pi-clipboard-check mr-2"></i>Décision Finale d'Approbation
        </template>
        <template #content>
          <div class="grid">
            <div class="col-12 md:col-6">
              <Panel header="Informations Dossier">
                <div class="flex flex-column gap-2">
                  <div><strong>Référence:</strong> {{ dossierDetail.dossier.reference }}</div>
                  <div><strong>SABA:</strong> {{ dossierDetail.dossier.saba }}</div>
                  <div><strong>Agriculteur:</strong> {{ dossierDetail.agriculteur.prenom }} {{ dossierDetail.agriculteur.nom }}</div>
                  <div><strong>CIN:</strong> {{ dossierDetail.agriculteur.cin }}</div>
                </div>
              </Panel>
            </div>
            
            <div class="col-12 md:col-6">
              <Panel header="Projet">
                <div class="flex flex-column gap-2">
                  <div><strong>Type:</strong> {{ dossierDetail.dossier.sousRubriqueDesignation }}</div>
                  <div><strong>Rubrique:</strong> {{ dossierDetail.dossier.rubriqueDesignation }}</div>
                  <div><strong>Montant demandé:</strong> {{ formatCurrency(dossierDetail.dossier.montantSubvention) }}</div>
                  <div><strong>Antenne:</strong> {{ dossierDetail.dossier.antenneDesignation }}</div>
                </div>
              </Panel>
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
          <div class="flex justify-content-between align-items-center mb-3">
            <Tag value="TERRAIN APPROUVÉ" severity="success" />
            <span class="text-sm text-500">Visite effectuée</span>
          </div>
          <p class="mb-2"><strong>Observations:</strong> Terrain conforme aux exigences du projet.</p>
          <p class="mb-0"><strong>Recommandations:</strong> Procéder à l'approbation du projet.</p>
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
                  />
                </div>
              </div>
            </div>

            <div v-if="formData.decision === 'reject'" class="field">
              <label for="motifRejet">Motif de Rejet *</label>
              <Textarea 
                id="motifRejet"
                v-model="formData.motifRejet" 
                rows="3"
                class="w-full"
              />
            </div>

            <div class="field">
              <label for="observationsCommission">Résumé des Observations Commission</label>
              <Textarea 
                id="observationsCommission"
                v-model="formData.observationsCommission" 
                rows="2"
                class="w-full"
              />
            </div>

            <div class="flex justify-content-end gap-2 pt-3">
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
    </div>

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

const router = useRouter();
const route = useRoute();
const toast = useToast();

const loading = ref(true);
const error = ref(null);
const submitting = ref(false);
const dossierDetail = ref(null);

const formData = reactive({
  decision: null,
  montantApprouve: null,
  montantDemande: 0,
  commentaireApprobation: '',
  conditionsSpecifiques: '',
  motifRejet: '',
  observationsCommission: 'Terrain conforme selon visite commission'
});

const decisionOptions = [
  { label: 'Approuver le dossier', value: 'approve' },
  { label: 'Rejeter le dossier', value: 'reject' }
];

const dossierId = computed(() => route.params.dossierId);

const breadcrumbItems = computed(() => [
  { label: 'Dossiers GUC', route: '/agent_guc/dossiers' },
  { label: dossierDetail.value?.dossier?.reference || 'Dossier' },
  { label: 'Décision Finale' }
]);

onMounted(async () => {
  await loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await ApiService.get(`/dossiers/${dossierId.value}`);
    dossierDetail.value = response;
    
    // Check if already approved
    if (isAlreadyApproved()) {
      toast.add({
        severity: 'info',
        summary: 'Information',
        detail: 'Ce dossier a déjà été approuvé',
        life: 3000
      });
    }
    
    // Initialize form
    formData.montantDemande = response.dossier.montantSubvention;
    formData.montantApprouve = response.dossier.montantSubvention;
    
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

    const response = await ApiService.post('/fiche-approbation/final-approval', requestData);
    
    if (response.success !== false) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Décision prise avec succès',
        life: 3000
      });
      
      if (formData.decision === 'approve') {
        setTimeout(() => {
          router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
        }, 2000);
      } else {
        router.push('/agent_guc/dossiers');
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
  
  return dossierDetail.value.dossier.statut === 'APPROVED' || 
         dossierDetail.value.dossier.statut === 'APPROVED_AWAITING_FARMER' ||
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