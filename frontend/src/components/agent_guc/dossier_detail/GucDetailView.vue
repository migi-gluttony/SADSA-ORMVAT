<template>
  <DossierDetailBase 
    :breadcrumb-root="'Dossiers GUC'"
    :user-role="'AGENT_GUC'"
    @dossier-loaded="handleDossierLoaded"
    ref="baseComponent"
  >
    <template #additional-status-tags="{ dossier }">
      <Tag 
        v-if="currentPhase"
        :value="getPhaseLabel()" 
        :severity="getPhaseSeverity()"
      />
    </template>

    <template #summary-header-right="{ dossier }">
      <div class="flex align-items-center gap-2 text-600">
        <i class="pi pi-map-marker"></i>
        <span>{{ getWorkflowLocationText() }}</span>
      </div>
    </template>

    <template #additional-admin-info="{ dossier }">
      <div><strong>Créé par:</strong> {{ dossier.utilisateurCreateurNom }}</div>
    </template>

    <template #header-actions="{ dossier }">
      <Button 
        label="Ajouter Note" 
        icon="pi pi-comment" 
        @click="showAddNoteDialog"
        severity="info"
      />
      
      <!-- Final Approval Button (Phase 4) -->
      <Button 
        v-if="needsFinalApproval()"
        label="Décision Finale" 
        icon="pi pi-gavel" 
        @click="goToFinalApproval(dossier)"
        severity="success"
      />
      
      <!-- View Fiche Button -->
      <Button 
        v-if="hasApprovedFiche()"
        label="Voir Fiche" 
        icon="pi pi-file-text" 
        @click="goToFiche(dossier)"
        severity="success"
      />
      
      <!-- Phase-based Actions -->
      <SplitButton 
        v-if="hasPhaseActions()"
        :model="getPhaseActionMenuItems()" 
        @click="handlePrimaryPhaseAction()"
        :label="getPrimaryPhaseActionLabel()"
      />
    </template>

    <template #forms-section="{ dossier, forms }">
      <Card class="mb-4">
        <template #title>
          <i class="pi pi-file-edit mr-2"></i>Documents et Formulaires
        </template>
        <template #subtitle>
          Documents soumis par l'antenne - lecture seule
        </template>
        <template #content>
          <div v-if="forms && forms.length > 0" class="grid">
            <div class="col-12 lg:col-6">
              <Panel header="Aperçu de complétion">
                <div class="flex justify-content-between align-items-center mb-3">
                  <div class="text-center">
                    <div class="text-2xl font-bold text-primary">{{ completedFormsCount }}/{{ totalFormsCount }}</div>
                    <div class="text-600">Formulaires complétés</div>
                  </div>
                  <div class="text-center">
                    <div class="text-2xl font-bold text-primary">{{ Math.round(completionPercentage) }}%</div>
                    <div class="text-600">Taux de complétion</div>
                  </div>
                </div>
                <ProgressBar :value="completionPercentage" />
              </Panel>
            </div>
            
            <div class="col-12">
              <div class="grid">
                <div 
                  v-for="form in forms" 
                  :key="form.formId"
                  class="col-12 md:col-6 lg:col-4"
                >
                  <Card class="h-full">
                    <template #header>
                      <div class="flex justify-content-between align-items-center p-3">
                        <h6 class="m-0">{{ form.title }}</h6>
                        <Tag 
                          :value="form.isCompleted ? 'Complété' : 'En attente'" 
                          :severity="form.isCompleted ? 'success' : 'warning'"
                        />
                      </div>
                    </template>
                    <template #content>
                      <p v-if="form.description" class="text-600 line-height-3">{{ form.description }}</p>
                      <small v-if="form.lastModified" class="text-500">
                        Modifié: {{ formatDate(form.lastModified) }}
                      </small>
                    </template>
                    <template #footer v-if="form.isCompleted">
                      <Button 
                        label="Voir les données" 
                        icon="pi pi-eye" 
                        @click="viewFormData(form)"
                        outlined
                        size="small"
                        class="w-full"
                      />
                    </template>
                  </Card>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="text-center py-4">
            <i class="pi pi-file text-4xl text-400 mb-3"></i>
            <p class="text-600">Aucun formulaire disponible</p>
          </div>
        </template>
      </Card>
    </template>

    <template #files-actions="{ files }">
      <Button 
        v-if="files.length > 0"
        label="Télécharger tout" 
        icon="pi pi-download" 
        @click="downloadAllDocuments"
        outlined
        size="small"
      />
    </template>
  </DossierDetailBase>

  <ActionDialogs 
    :dialogs="actionDialogs"
    @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed"
  />

  <FormDataViewerDialog 
    v-model:visible="formDataDialog.visible"
    :form="formDataDialog.form"
    :dossier="currentDossier"
  />
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierDetailBase from '@/components/dossiers/DossierDetailBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import FormDataViewerDialog from '@/components/dossiers/FormDataViewerDialog.vue';
import AuthService from '@/services/AuthService';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Tag from 'primevue/tag';
import Card from 'primevue/card';
import Panel from 'primevue/panel';
import ProgressBar from 'primevue/progressbar';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const baseComponent = ref();

const currentDossier = ref(null);
const currentDossierDetail = ref(null);
const currentPhase = ref(null);

const actionDialogs = reactive({
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false }
});

const formDataDialog = reactive({
  visible: false,
  form: null
});

const completedFormsCount = computed(() => {
  return currentDossierDetail.value?.availableForms?.filter(f => f.isCompleted).length || 0;
});

const totalFormsCount = computed(() => {
  return currentDossierDetail.value?.availableForms?.length || 0;
});

const completionPercentage = computed(() => {
  if (totalFormsCount.value === 0) return 100;
  return (completedFormsCount.value / totalFormsCount.value) * 100;
});

// Phase detection based on backend workflow
function detectPhase(dossierDetail) {
  // Use direct phase number from backend if available
  if (dossierDetail.phaseNumber) {
    return dossierDetail.phaseNumber;
  }
  
  // Use etape ID mapping if available
  if (dossierDetail.etapeId) {
    const phaseMap = {
      1: 1, // AP - Phase Antenne
      2: 2, // AP - Phase GUC (Initial)
      3: 3, // AP - Phase AHA-AF
      4: 4, // AP - Phase GUC (Final)
      5: 5, // RP - Phase Antenne
      6: 6, // RP - Phase GUC
      7: 7, // RP - Phase service technique
      8: 8  // RP - Phase GUC (Final)
    };
    return phaseMap[dossierDetail.etapeId] || 1;
  }
  
  // Fallback to text parsing (legacy)
  const etape = dossierDetail.etapeActuelle;
  const statut = dossierDetail.dossier?.statut;
  
  if (!etape) return 1;
  
  const phaseMatch = etape.match(/\(Phase (\d+):/);
  if (phaseMatch) {
    return parseInt(phaseMatch[1]);
  }
  
  return 1;
}

function getPhaseLabel() {
  const phaseLabels = {
    1: 'Phase 1: Création',
    2: 'Phase 2: Révision GUC',
    3: 'Phase 3: Commission',
    4: 'Phase 4: Approbation Finale',
    5: 'Phase 5: En attente',
    6: 'Phase 6: Révision Réalisation',
    7: 'Phase 7: Service Technique',
    8: 'Phase 8: Finalisation'
  };
  return phaseLabels[currentPhase.value] || 'Phase inconnue';
}

function getPhaseSeverity() {
  if ([2, 4, 6, 8].includes(currentPhase.value)) return 'warning'; // GUC phases
  if ([3, 7].includes(currentPhase.value)) return 'info'; // External phases
  return 'secondary';
}

function needsFinalApproval() {
  return currentPhase.value === 4 && !hasApprovedFiche();
}

function hasApprovedFiche() {
  const statut = currentDossier.value?.statut;
  return statut === 'APPROVED' || 
         statut === 'APPROVED_AWAITING_FARMER' ||
         statut === 'COMPLETED' ||
         currentDossier.value?.dateApprobation;
}

function hasPhaseActions() {
  return (currentPhase.value === 2 || currentPhase.value === 6) && !hasApprovedFiche();
}

function getPhaseActionMenuItems() {
  const items = [];
  
  if (currentPhase.value === 2) {
    items.push(
      {
        label: 'Envoyer à la Commission',
        icon: 'pi pi-forward',
        command: () => showActionDialog('sendToCommission')
      },
      {
        label: 'Retourner à l\'Antenne',
        icon: 'pi pi-undo',
        command: () => showActionDialog('returnToAntenne')
      },
      {
        label: 'Rejeter',
        icon: 'pi pi-times-circle',
        command: () => showActionDialog('reject')
      }
    );
  } else if (currentPhase.value === 6) {
    items.push({
      label: 'Envoyer au Service Technique',
      icon: 'pi pi-forward',
      command: () => processRealizationReview()
    });
  }
  
  return items;
}

function getPrimaryPhaseActionLabel() {
  if (currentPhase.value === 2) return 'Commission';
  if (currentPhase.value === 6) return 'Service Technique';
  return 'Actions';
}

function handlePrimaryPhaseAction() {
  if (currentPhase.value === 2) {
    showActionDialog('sendToCommission');
  } else if (currentPhase.value === 6) {
    processRealizationReview();
  }
}

function handleDossierLoaded(dossier) {
  currentDossier.value = dossier.dossier;
  currentDossierDetail.value = dossier;
  currentPhase.value = detectPhase(dossier);
}

function getWorkflowLocationText() {
  const emplacement = currentDossierDetail.value?.emplacementActuel;
  const locationMap = {
    'ANTENNE': 'Antenne',
    'GUC': 'Guichet Unique Central',
    'COMMISSION_AHA_AF': 'Commission Terrain',
    'SERVICE_TECHNIQUE': 'Service Technique'
  };
  return locationMap[emplacement] || 'Non défini';
}

function showAddNoteDialog() {
  // Handled by base component
}

function goToFinalApproval(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/final-approval`);
}

function goToFiche(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/fiche`);
}

function showActionDialog(action) {
  actionDialogs[action] = {
    visible: true,
    dossier: currentDossier.value,
    loading: false,
    comment: '',
    priority: 'NORMALE',
    reasons: [],
    definitive: false
  };
}

function viewFormData(form) {
  formDataDialog.visible = true;
  formDataDialog.form = form;
}

async function downloadAllDocuments() {
  try {
    const response = await fetch(`/api/dossiers/${route.params.dossierId}/documents/download-all`, {
      headers: {
        'Authorization': `Bearer ${AuthService.getToken()}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `dossier_${currentDossier.value.reference}_documents.zip`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Archive téléchargée avec succès',
      life: 3000
    });
    
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger l\'archive',
      life: 3000
    });
  }
}

async function processRealizationReview() {
  try {
    const response = await ApiService.post(`/dossiers/${currentDossier.value.id}/process-realization-review`, {
      commentaire: 'Révision réalisation approuvée'
    });

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Dossier envoyé au Service Technique',
        life: 3000
      });
      
      if (baseComponent.value) {
        baseComponent.value.loadDossierDetail();
      }
    }
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  }
}

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, data } = actionData;
    
    let endpoint = '';
    let payload = {};
    
    switch (action) {
      case 'sendToCommission':
        endpoint = `/dossiers/${dossier.id}/send-to-commission`;
        payload = { 
          commentaire: data.comment,
          priorite: data.priority
        };
        break;
      case 'returnToAntenne':
        endpoint = `/dossiers/${dossier.id}/return-to-antenne`;
        payload = { 
          motif: 'Complétion requise',
          commentaire: data.comment,
          documentsManquants: data.reasons
        };
        break;
      case 'reject':
        endpoint = `/dossiers/${dossier.id}/reject`;
        payload = { 
          motif: 'Rejet du dossier',
          commentaire: data.comment,
          definitif: data.definitive
        };
        break;
    }
    
    const response = await ApiService.post(endpoint, payload);
    
    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 4000
      });
      
      if (baseComponent.value) {
        baseComponent.value.loadDossierDetail();
      }
    }
    
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Une erreur est survenue',
      life: 4000
    });
  }
}

function handleDialogClosed() {
  Object.keys(actionDialogs).forEach(key => {
    actionDialogs[key].visible = false;
  });
}

function formatDate(date) {
  if (!date) return '';
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}
</script>