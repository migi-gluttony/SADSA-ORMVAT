<template>
  <div class="agent-antenne-dossier-detail">
    <!-- Navigation -->
    <div class="mb-4">
      <Button 
        label="Retour à mes dossiers" 
        icon="pi pi-arrow-left" 
        @click="goBack"
        class="p-button-outlined"
      />
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center p-6">
      <ProgressSpinner size="50px" />
      <p class="mt-3">Chargement du dossier...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center p-6">
      <Message severity="error" :closable="false">
        {{ error }}
      </Message>
      <Button 
        label="Réessayer" 
        icon="pi pi-refresh" 
        @click="loadDossierDetail"
        class="p-button-outlined mt-3"
      />
    </div>

    <!-- Main Content -->
    <div v-else-if="dossierDetail" class="space-y-6">
      
      <!-- Dossier Header -->
      <Card>
        <template #header>
          <div class="flex justify-content-between align-items-center p-4">
            <div>
              <h2 class="m-0">{{ dossierDetail.numeroDossier }}</h2>
              <div class="flex align-items-center gap-2 mt-2">
                <p class="m-0 text-color-secondary">{{ dossierDetail.saba }}</p>
                <Tag 
                  :value="getStatusLabel(dossierDetail.statut)" 
                  :severity="getStatusSeverity(dossierDetail.statut)"
                />
              </div>
            </div>
            <div class="flex gap-2">
              <!-- Edit Mode Buttons -->
              <Button 
                v-if="isEditing"
                label="Sauvegarder" 
                icon="pi pi-check"
                @click="saveEdit"
                class="p-button-success"
                :loading="loading"
              />
              <Button 
                v-if="isEditing"
                label="Annuler" 
                icon="pi pi-times"
                @click="cancelEdit"
                class="p-button-outlined"
              />
              <!-- Normal Action Buttons -->
              <Button 
                v-else
                v-for="action in dossierDetail.availableActions" 
                :key="action.action"
                :label="action.label"
                :icon="getActionIcon(action.action)"
                @click="executeAction(action)"
                :class="getActionClass(action.action)"
                :loading="actionLoading[action.action]"
              />
            </div>
          </div>
        </template>
        <template #content>
          <!-- Dossier Summary Info -->
          <div class="grid">
            <!-- Agriculteur Info -->
            <div class="col-12 md:col-6">
              <h4 class="mt-0 mb-3">Agriculteur</h4>
              
              <!-- View Mode -->
              <div v-if="!isEditing" class="space-y-2">
                <div><strong>Nom:</strong> {{ dossierDetail.agriculteur?.prenom }} {{ dossierDetail.agriculteur?.nom }}</div>
                <div><strong>CIN:</strong> {{ dossierDetail.agriculteur?.cin }}</div>
                <div><strong>Téléphone:</strong> {{ dossierDetail.agriculteur?.telephone }}</div>
                <div v-if="dossierDetail.agriculteur?.communeRurale">
                  <strong>Commune:</strong> {{ dossierDetail.agriculteur.communeRurale }}
                </div>
                <div v-if="dossierDetail.agriculteur?.douar">
                  <strong>Douar:</strong> {{ dossierDetail.agriculteur.douar }}
                </div>
              </div>
              
              <!-- Edit Mode -->
              <div v-else class="space-y-3">
                <div class="field">
                  <label>Prénom</label>
                  <InputText v-model="editForm.agriculteur.prenom" class="w-full" />
                </div>
                <div class="field">
                  <label>Nom</label>
                  <InputText v-model="editForm.agriculteur.nom" class="w-full" />
                </div>
                <div class="field">
                  <label>CIN</label>
                  <InputText v-model="editForm.agriculteur.cin" class="w-full" />
                </div>
                <div class="field">
                  <label>Téléphone</label>
                  <InputText v-model="editForm.agriculteur.telephone" class="w-full" />
                </div>
                <div class="field">
                  <label>Commune</label>
                  <InputText v-model="editForm.agriculteur.communeRurale" class="w-full" />
                </div>
                <div class="field">
                  <label>Douar</label>
                  <InputText v-model="editForm.agriculteur.douar" class="w-full" />
                </div>
              </div>
            </div>

            <!-- Projet Info -->
            <div class="col-12 md:col-6">
              <h4 class="mt-0 mb-3">Projet</h4>
              
              <!-- View Mode -->
              <div v-if="!isEditing" class="space-y-2">
                <div><strong>Type:</strong> {{ dossierDetail.projet?.sousRubrique }}</div>
                <div><strong>Rubrique:</strong> {{ dossierDetail.projet?.rubrique }}</div>
                <div><strong>Référence:</strong> {{ dossierDetail.reference }}</div>
                <div v-if="dossierDetail.montantSubvention">
                  <strong>Montant:</strong> {{ formatCurrency(dossierDetail.montantSubvention) }}
                </div>
              </div>
              
              <!-- Edit Mode -->
              <div v-else class="space-y-3">
                <div><strong>Type:</strong> {{ dossierDetail.projet?.sousRubrique }}</div>
                <div><strong>Rubrique:</strong> {{ dossierDetail.projet?.rubrique }}</div>
                <div class="field">
                  <label>Référence</label>
                  <InputText v-model="editForm.reference" class="w-full" />
                </div>
                <div class="field">
                  <label>SABA</label>
                  <InputText v-model="editForm.saba" class="w-full" />
                </div>
                <div class="field">
                  <label>Montant Subvention (MAD)</label>
                  <InputNumber 
                    v-model="editForm.montantSubvention" 
                    mode="currency" 
                    currency="MAD" 
                    locale="fr-MA"
                    class="w-full" 
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Timing Information -->
          <div v-if="dossierDetail.timing" class="mt-4 p-3 surface-50 border-round">
            <div class="flex justify-content-between align-items-center">
              <div>
                <h5 class="m-0">Étape Actuelle: {{ dossierDetail.timing.currentStep }}</h5>
                <p class="m-0 text-sm text-color-secondary mt-1">
                  Assigné à: {{ dossierDetail.timing.assignedTo }}
                </p>
              </div>
              <div class="text-right">
                <div class="text-2xl font-bold" :class="{
                  'text-red-500': dossierDetail.timing.enRetard,
                  'text-orange-500': dossierDetail.timing.joursRestants <= 2,
                  'text-green-500': dossierDetail.timing.joursRestants > 2
                }">
                  {{ dossierDetail.timing.enRetard ? 
                    `${dossierDetail.timing.joursRetard} jour(s) de retard` : 
                    `${dossierDetail.timing.joursRestants} jour(s) restants` 
                  }}
                </div>
                <p class="text-sm text-color-secondary">
                  Délai maximum: {{ dossierDetail.timing.delaiMaxJours }} jours
                </p>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Documents and Forms Section -->
      <Card>
        <template #title>
          <i class="pi pi-file-edit mr-2"></i>
          Documents et Formulaires
        </template>
        <template #content>
          <div v-if="canModifyDossier" class="text-center p-4">
            <Button 
              label="Remplir les Documents" 
              icon="pi pi-file-edit" 
              @click="goToDocumentFilling"
              class="p-button-success p-button-lg" 
              size="large" 
            />
            <p class="mt-3 text-sm text-color-secondary">
              Complétez tous les documents requis pour ce type de projet
            </p>
          </div>
          <div v-else class="text-center p-4 surface-50 border-round">
            <i class="pi pi-lock text-4xl text-color-secondary mb-3"></i>
            <p class="text-color-secondary">Documents en lecture seule à cette étape</p>
            <Button 
              label="Voir les documents" 
              icon="pi pi-eye" 
              @click="goToDocumentFilling" 
              class="p-button-outlined"
              size="small" 
            />
          </div>
        </template>
      </Card>

      <!-- Workflow History -->
      <Card v-if="dossierDetail.workflowHistory && dossierDetail.workflowHistory.length > 0">
        <template #title>
          <i class="pi pi-history mr-2"></i>
          Historique du traitement
        </template>
        <template #content>
          <div class="timeline-container">
            <div 
              v-for="(step, index) in dossierDetail.workflowHistory" 
              :key="step.id"
              class="timeline-item"
              :class="{ 'current-step': index === 0 }"
            >
              <div class="timeline-marker">
                <i v-if="index === 0" class="pi pi-clock"></i>
                <i v-else class="pi pi-check"></i>
              </div>
              <div class="timeline-content">
                <h5 class="m-0">{{ step.phaseNom }}</h5>
                <div class="timeline-meta mt-1">
                  <span class="mr-3">
                    <i class="pi pi-calendar mr-1"></i>
                    {{ formatDate(step.dateEntree) }}
                  </span>
                  <span v-if="step.userName" class="mr-3">
                    <i class="pi pi-user mr-1"></i>
                    {{ step.userName }}
                  </span>
                  <span v-if="step.dureeJours" class="text-color-secondary">
                    <i class="pi pi-clock mr-1"></i>
                    {{ step.dureeJours }} jour(s)
                  </span>
                </div>
                <p v-if="step.commentaire" class="mt-2 mb-0 text-color-secondary">
                  {{ step.commentaire }}
                </p>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Documents Uploaded -->
      <Card v-if="dossierDetail.documents && dossierDetail.documents.length > 0">
        <template #title>
          <i class="pi pi-file mr-2"></i>
          Documents archivés
        </template>
        <template #content>
          <div class="grid">
            <div 
              v-for="doc in dossierDetail.documents" 
              :key="doc.id"
              class="col-12 md:col-6 lg:col-4"
            >
              <div class="p-3 border-1 surface-border border-round">
                <div class="flex align-items-center mb-2">
                  <i class="pi pi-file text-primary mr-2"></i>
                  <span class="font-medium">{{ doc.nomDocument }}</span>
                </div>
                <div class="text-sm text-color-secondary">
                  <div>Statut: {{ doc.statut }}</div>
                  <div>{{ formatDate(doc.dateUpload) }}</div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Action Dialogs -->
    <ConfirmDialog />
    
    <!-- Custom Action Dialog -->
    <Dialog 
      v-model:visible="actionDialog.visible"
      :header="actionDialog.title"
      modal
      :style="{ width: '450px' }"
    >
      <div class="space-y-4">
        <div v-if="actionDialog.type === 'delete'">
          <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
          <div class="field">
            <label for="deleteComment" class="block mb-2">Motif de suppression</label>
            <Textarea 
              id="deleteComment"
              v-model="actionDialog.comment"
              rows="3"
              class="w-full"
              placeholder="Saisissez le motif de suppression..."
            />
          </div>
        </div>
        
        <div v-else-if="actionDialog.type === 'submit'">
          <p>Confirmer la soumission de ce dossier au GUC ?</p>
          <div class="field">
            <label for="submitComment" class="block mb-2">Commentaire (optionnel)</label>
            <Textarea 
              id="submitComment"
              v-model="actionDialog.comment"
              rows="2"
              class="w-full"
              placeholder="Commentaire sur la soumission..."
            />
          </div>
        </div>
      </div>
      
      <template #footer>
        <Button 
          label="Annuler" 
          @click="actionDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          :label="actionDialog.confirmLabel"
          @click="confirmAction"
          :loading="actionDialog.loading"
          :class="actionDialog.confirmClass"
        />
      </template>
    </Dialog>

    <!-- Start Realization Dialog -->
    <Dialog 
      v-model:visible="realizationDialog.visible"
      :header="realizationDialog.title"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div>
          <p>Confirmer le démarrage de la réalisation pour ce dossier ?</p>
          <div class="mt-2 p-3 bg-blue-50 border-round">
            <strong>{{ realizationDialog.dossier?.numeroDossier }}</strong><br>
            {{ realizationDialog.dossier?.agriculteurNom }}<br>
            <small>Type: {{ realizationDialog.dossier?.projet?.sousRubrique }}</small>
          </div>
        </div>
        
        <div class="field">
          <label for="realizationComment" class="block mb-2">Commentaire</label>
          <Textarea 
            id="realizationComment"
            v-model="realizationDialog.commentaire"
            rows="3"
            class="w-full"
            placeholder="Précisez le contexte du démarrage de réalisation (ex: dépôt attestation, notification directe, etc.)"
          />
        </div>

        <div class="field">
          <label for="receptionType" class="block mb-2">Type de réception</label>
          <Dropdown 
            id="receptionType"
            v-model="realizationDialog.typeReception"
            :options="receptionTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner le type de réception"
            class="w-full"
          />
        </div>

        <div class="p-3 bg-yellow-50 border-round border-left-3 border-yellow-500">
          <div class="flex align-items-center">
            <i class="pi pi-info-circle text-yellow-700 mr-2"></i>
            <div class="text-sm text-yellow-800">
              <strong>Information:</strong> Une fois la réalisation démarrée, le dossier sera automatiquement transmis au GUC pour traitement selon le type de projet.
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <Button 
          label="Annuler" 
          @click="realizationDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Démarrer Réalisation"
          @click="confirmStartRealization"
          :loading="realizationDialog.loading"
          class="p-button-success"
        />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';

import Button from 'primevue/button';
import Card from 'primevue/card';
import Tag from 'primevue/tag';
import ProgressSpinner from 'primevue/progressspinner';
import Message from 'primevue/message';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Dropdown from 'primevue/dropdown';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();

// State
const loading = ref(true);
const error = ref('');
const dossierDetail = ref(null);
const actionLoading = ref({});
const isEditing = ref(false);
const editForm = ref({
  agriculteur: {
    nom: '',
    prenom: '',
    cin: '',
    telephone: '',
    communeRurale: '',
    douar: ''
  },
  reference: '',
  saba: '',
  montantSubvention: 0
});

// Computed
const dossierId = computed(() => parseInt(route.params.dossierId));
const canModifyDossier = computed(() => {
  if (!dossierDetail.value) return false;
  return dossierDetail.value.statut === 'DRAFT' || 
         dossierDetail.value.statut === 'RETURNED_FOR_COMPLETION';
});

// Dialog state
const actionDialog = ref({
  visible: false,
  type: '',
  title: '',
  confirmLabel: '',
  confirmClass: '',
  comment: '',
  loading: false,
  action: null
});

const realizationDialog = ref({
  visible: false,
  title: 'Démarrer la Réalisation',
  dossier: null,
  commentaire: '',
  typeReception: '',
  loading: false
});

// Reception type options
const receptionTypeOptions = ref([
  { label: 'Dépôt attestation d\'approbation', value: 'DEPOT_ATTESTATION' },
  { label: 'Notification directe', value: 'NOTIFICATION_DIRECTE' },
  { label: 'Contact téléphonique', value: 'CONTACT_TELEPHONIQUE' },
  { label: 'Visite à l\'antenne', value: 'VISITE_ANTENNE' }
]);

onMounted(() => {
  loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await ApiService.get(`/agent_antenne/dossiers/detail/${dossierId.value}`);
    
    // Ensure we have a proper data structure
    if (response && typeof response === 'object') {
      dossierDetail.value = {
        ...response,
        agriculteur: response.agriculteur || {},
        projet: response.projet || {},
        workflowHistory: Array.isArray(response.workflowHistory) ? response.workflowHistory : [],
        documents: Array.isArray(response.documents) ? response.documents : [],
        availableActions: Array.isArray(response.availableActions) ? response.availableActions : []
      };
    } else {
      throw new Error('Invalid response format');
    }
    
  } catch (err) {
    console.error('Erreur lors du chargement:', err);
    error.value = err.message || 'Impossible de charger les données';
    
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.value,
      life: 5000
    });
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/agent_antenne/dossiers');
}

function goToDocumentFilling() {
  router.push(`/agent_antenne/dossiers/documents/${dossierId.value}`);
}

// Action handling
async function executeAction(action) {
  if (action.action === 'update') {
    enableEditMode();
    return;
  }
  
  if (action.action === 'delete') {
    showActionDialog('delete', 'Supprimer le dossier', 'Supprimer', 'p-button-danger', action);
    return;
  }
  
  if (action.action === 'submit') {
    showActionDialog('submit', 'Soumettre le dossier', 'Soumettre', 'p-button-success', action);
    return;
  }

  if (action.action === 'start_realization') {
    showRealizationDialog();
    return;
  }
  
  // Direct action execution for other actions
  await performAction(action);
}

function showRealizationDialog() {
  realizationDialog.value = {
    visible: true,
    title: 'Démarrer la Réalisation',
    dossier: dossierDetail.value,
    commentaire: '',
    typeReception: '',
    loading: false
  };
}

async function confirmStartRealization() {
  try {
    realizationDialog.value.loading = true;
    
    if (!realizationDialog.value.typeReception) {
      toast.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner le type de réception',
        life: 3000
      });
      return;
    }

    const payload = {
      commentaire: realizationDialog.value.commentaire || 'Démarrage de la réalisation',
      typeReception: realizationDialog.value.typeReception,
      observations: ''
    };
    
    const response = await ApiService.post(`/agent_antenne/dossiers/start-realization/${dossierId.value}`, payload);
    
    if (response && response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message,
        life: 3000
      });

      // Show additional info if available
      if (response.nextPhase) {
        setTimeout(() => {
          toast.add({
            severity: 'info',
            summary: 'Prochaine étape',
            detail: response.nextPhase,
            life: 5000
          });
        }, 1000);
      }
      
      realizationDialog.value.visible = false;
      await loadDossierDetail();
    }
    
  } catch (err) {
    console.error('Erreur lors du démarrage de réalisation:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors du démarrage de réalisation',
      life: 5000
    });
  } finally {
    realizationDialog.value.loading = false;
  }
}

function enableEditMode() {
  isEditing.value = true;
  // Initialize edit form with current data
  if (dossierDetail.value && typeof dossierDetail.value === 'object') {
    const agriculteur = dossierDetail.value.agriculteur || {};
    editForm.value = {
      agriculteur: {
        nom: agriculteur.nom || '',
        prenom: agriculteur.prenom || '',
        cin: agriculteur.cin || '',
        telephone: agriculteur.telephone || '',
        communeRurale: agriculteur.communeRurale || '',
        douar: agriculteur.douar || ''
      },
      reference: dossierDetail.value.reference || '',
      saba: dossierDetail.value.saba || '',
      montantSubvention: dossierDetail.value.montantSubvention || 0
    };
  }
}

function cancelEdit() {
  isEditing.value = false;
  editForm.value = {
    agriculteur: {
      nom: '',
      prenom: '',
      cin: '',
      telephone: '',
      communeRurale: '',
      douar: ''
    },
    reference: '',
    saba: '',
    montantSubvention: 0
  };
}

async function saveEdit() {
  try {
    loading.value = true;
    
    // Validate that we have the required data
    if (!editForm.value.agriculteur?.nom || !editForm.value.agriculteur?.prenom || !editForm.value.agriculteur?.cin) {
      throw new Error('Le nom, prénom et CIN de l\'agriculteur sont requis');
    }
    
    if (!editForm.value.reference || !editForm.value.saba) {
      throw new Error('La référence et le SABA sont requis');
    }

    // Create minimal update request with only fields that have valid values
    const updateData = {
      agriculteur: {
        nom: editForm.value.agriculteur.nom,
        prenom: editForm.value.agriculteur.prenom,
        cin: editForm.value.agriculteur.cin,
        telephone: editForm.value.agriculteur.telephone
      },
      reference: editForm.value.reference,
      saba: editForm.value.saba,
      montantSubvention: editForm.value.montantSubvention
    };

    await ApiService.put(`/agent_antenne/dossiers/update/${dossierId.value}`, updateData);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Dossier modifié avec succès',
      life: 3000
    });
    
    isEditing.value = false;
    await loadDossierDetail();
    
  } catch (err) {
    console.error('Erreur lors de la modification:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la modification',
      life: 5000
    });
  } finally {
    loading.value = false;
  }
}

function showActionDialog(type, title, confirmLabel, confirmClass, action) {
  actionDialog.value = {
    visible: true,
    type,
    title,
    confirmLabel,
    confirmClass,
    comment: '',
    loading: false,
    action
  };
}

async function confirmAction() {
  if (!actionDialog.value.action) return;
  
  try {
    actionDialog.value.loading = true;
    
    const action = actionDialog.value.action;
    let payload = {};
    
    if (actionDialog.value.type === 'delete') {
      payload.motif = actionDialog.value.comment;
    } else if (actionDialog.value.type === 'submit') {
      payload.commentaire = actionDialog.value.comment;
    }
    
    await performActionWithPayload(action, payload);
    actionDialog.value.visible = false;
    
  } catch (err) {
    console.error('Erreur lors de l\'action:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de l\'exécution',
      life: 5000
    });
  } finally {
    actionDialog.value.loading = false;
  }
}

async function performAction(action) {
  try {
    actionLoading.value[action.action] = true;
    
    let response;
    if (action.method === 'GET') {
      response = await ApiService.get(action.endpoint);
    } else if (action.method === 'POST') {
      response = await ApiService.post(action.endpoint);
    } else if (action.method === 'PUT') {
      response = await ApiService.put(action.endpoint);
    } else if (action.method === 'DELETE') {
      response = await ApiService.delete(action.endpoint);
    }
    
    if (response && response.success !== false) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Action exécutée avec succès',
        life: 3000
      });
      
      if (action.action === 'delete') {
        router.push('/agent_antenne/dossiers');
      } else {
        await loadDossierDetail();
      }
    }
    
  } catch (err) {
    console.error('Erreur lors de l\'action:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de l\'exécution',
      life: 5000
    });
  } finally {
    actionLoading.value[action.action] = false;
  }
}

async function performActionWithPayload(action, payload) {
  let response;
  
  if (action.method === 'POST') {
    response = await ApiService.post(action.endpoint, payload);
  } else if (action.method === 'PUT') {
    response = await ApiService.put(action.endpoint, payload);
  } else if (action.method === 'DELETE') {
    response = await ApiService.delete(action.endpoint, payload);
  } else {
    throw new Error(`Méthode ${action.method} non supportée avec payload`);
  }
  
  if (response && response.success !== false) {
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: response.message || 'Action exécutée avec succès',
      life: 3000
    });
    
    if (action.action === 'delete') {
      router.push('/agent_antenne/dossiers');
    } else {
      await loadDossierDetail();
    }
  }
}

// Utility functions
function getActionIcon(action) {
  const icons = {
    update: 'pi pi-pencil',
    submit: 'pi pi-send',
    delete: 'pi pi-trash',
    start_realization: 'pi pi-play'
  };
  return icons[action] || 'pi pi-cog';
}

function getActionClass(action) {
  const classes = {
    update: 'p-button-warning',
    submit: 'p-button-success',
    delete: 'p-button-danger',
    start_realization: 'p-button-info'
  };
  return classes[action] || '';
}

function formatDate(dateString) {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (error) {
    console.warn('Error formatting date:', error);
    return dateString;
  }
}

function formatCurrency(amount) {
  if (!amount) return '0 DH';
  try {
    return new Intl.NumberFormat('fr-MA', {
      style: 'currency',
      currency: 'MAD'
    }).format(amount);
  } catch (error) {
    console.warn('Error formatting currency:', error);
    return `${amount} DH`;
  }
}

function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En cours',
    'APPROVED': 'Approuvé',
    'AWAITING_FARMER': 'En attente agriculteur',
    'REALIZATION_IN_PROGRESS': 'Réalisation en cours',
    'REJECTED': 'Rejeté',
    'RETURNED_FOR_COMPLETION': 'Retourné',
    'COMPLETED': 'Terminé'
  };
  return labels[status] || status;
}

function getStatusSeverity(status) {
  const severities = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'AWAITING_FARMER': 'success',
    'REALIZATION_IN_PROGRESS': 'info',
    'REJECTED': 'danger',
    'RETURNED_FOR_COMPLETION': 'warning',
    'COMPLETED': 'success'
  };
  return severities[status] || 'secondary';
}
</script>

<style scoped>
.space-y-6 > * + * {
  margin-top: 1.5rem;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.space-y-3 > * + * {
  margin-top: 0.75rem;
}

.space-y-2 > * + * {
  margin-top: 0.5rem;
}

.timeline-container {
  position: relative;
  padding-left: 2rem;
}

.timeline-item {
  position: relative;
  padding-bottom: 1.5rem;
}

.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: -1.75rem;
  top: 2rem;
  bottom: -0.5rem;
  width: 2px;
  background: var(--surface-300);
}

.timeline-marker {
  position: absolute;
  left: -2rem;
  top: 0.25rem;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
}

.timeline-item.current-step .timeline-marker {
  background: var(--orange-500);
  animation: pulse 2s infinite;
}

.timeline-content {
  background: var(--surface-50);
  padding: 1rem;
  border-radius: 0.5rem;
  border-left: 3px solid var(--primary-color);
}

.timeline-item.current-step .timeline-content {
  border-left-color: var(--orange-500);
  background: var(--orange-50);
}

.timeline-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 152, 0, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(255, 152, 0, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 152, 0, 0);
  }
}

.p-button-lg {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}
</style>