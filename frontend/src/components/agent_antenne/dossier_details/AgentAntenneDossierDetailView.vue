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
            </div>            <div class="flex gap-2">
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
          <div class="grid">            <!-- Agriculteur Info -->
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
      <Card v-if="showDocuments">
        <template #title>
          <div class="flex justify-content-between align-items-center">
            <div>
              <i class="pi pi-file-edit mr-2"></i>
              Documents et Formulaires
            </div>
            <Button 
              :label="documentsCollapsed ? 'Afficher' : 'Masquer'"
              :icon="documentsCollapsed ? 'pi pi-chevron-down' : 'pi pi-chevron-up'"
              @click="documentsCollapsed = !documentsCollapsed"
              class="p-button-text"
            />
          </div>
        </template>
        <template #content>
          <div v-if="!documentsCollapsed">
            <!-- Documents Loading -->
            <div v-if="documentsLoading" class="text-center p-4">
              <ProgressSpinner size="30px" />
              <p class="mt-2">Chargement des documents...</p>
            </div>

            <!-- Documents Content -->
            <div v-else-if="documentsData" class="space-y-4">
              <!-- Progress Overview -->
              <div v-if="documentsData.statistics" class="p-3 surface-100 border-round">
                <div class="flex justify-content-between align-items-center mb-2">
                  <h5 class="m-0">Progression des Documents</h5>
                  <span class="text-xl font-bold text-primary">
                    {{ Math.round(documentsData.statistics.pourcentageCompletion) }}%
                  </span>
                </div>
                <ProgressBar :value="documentsData.statistics.pourcentageCompletion" />
                <div class="flex justify-content-between text-sm text-color-secondary mt-2">
                  <span>{{ documentsData.statistics.documentsCompletes }} complétés</span>
                  <span>{{ documentsData.statistics.totalDocuments }} total</span>
                </div>
              </div>              <!-- Documents List -->
              <div class="space-y-4">
                <div 
                  v-for="document in documentsData.documentsRequis" 
                  :key="document.id"
                  class="border-1 surface-border border-round p-4"
                >
                  <!-- Document Header -->
                  <div class="flex justify-content-between align-items-start mb-3">
                    <div class="flex-1">
                      <h5 class="m-0 mb-2">
                        {{ document.nomDocument }}
                        <Tag 
                          v-if="document.obligatoire" 
                          value="Obligatoire" 
                          severity="danger" 
                          class="ml-2"
                        />
                        <Tag 
                          v-else 
                          value="Optionnel" 
                          severity="info" 
                          class="ml-2"
                        />
                      </h5>
                      <p v-if="document.description" class="m-0 text-color-secondary">
                        {{ document.description }}
                      </p>
                    </div>
                    <Tag 
                      :value="getDocumentStatusLabel(document.status)" 
                      :severity="getDocumentStatusSeverity(document.status)"
                    />
                  </div>                  <!-- File Upload Section -->
                  <div class="mb-4" v-if="canModifyDossier">
                    <h6 class="mb-2">
                      <i class="pi pi-upload mr-1"></i> Fichiers
                    </h6>
                    
                    <FileUpload
                      mode="basic"
                      accept=".pdf,.jpg,.jpeg,.png,.gif"
                      :maxFileSize="10000000"
                      :multiple="true"
                      @select="(event) => onFileSelect(event, document.id)"
                      :auto="false"
                      chooseLabel="Sélectionner fichier(s)"
                      class="mb-2"
                    />
                    <small class="block text-color-secondary">
                      Formats acceptés: PDF, JPG, PNG, GIF (max 10MB par fichier)
                    </small>
                  </div>

                  <!-- Uploaded Files -->
                  <div v-if="getUploadedFiles(document).length > 0" class="mb-4">
                    <h6 class="mb-2">Fichiers uploadés ({{ getUploadedFiles(document).length }})</h6>
                    <div class="space-y-2">
                      <div 
                        v-for="file in getUploadedFiles(document)" 
                        :key="file.id"
                        class="flex justify-content-between align-items-center p-2 surface-50 border-round"
                      >
                        <div class="flex align-items-center">
                          <i :class="getFileIcon(file.formatFichier)" class="text-primary mr-2"></i>
                          <div>
                            <div class="font-medium">{{ file.nomFichier }}</div>
                            <div class="text-sm text-color-secondary">
                              {{ formatFileSize(file.tailleFichier) }} • 
                              {{ formatDate(file.dateUpload) }}
                            </div>
                          </div>
                        </div>
                        <div class="flex gap-2">
                          <Button 
                            icon="pi pi-download" 
                            @click="downloadFile(file)"
                            class="p-button-outlined p-button-sm"
                            v-tooltip.top="'Télécharger'"
                          />
                          <Button 
                            v-if="canModifyDossier"
                            icon="pi pi-trash" 
                            @click="confirmDeleteFile(file)"
                            class="p-button-danger p-button-outlined p-button-sm"
                            v-tooltip.top="'Supprimer'"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Form Section -->
                  <div v-if="document.formStructure && Object.keys(document.formStructure).length > 0">
                    <h6 class="mb-2">
                      <i class="pi pi-list mr-1"></i> Formulaire
                    </h6>
                    <div class="surface-50 p-3 border-round">
                      <DynamicForm
                        :formStructure="document.formStructure"
                        :formData="document.formData || {}"
                        @form-save="(data) => saveFormData(data, document.id)"
                        :readonly="!canModifyDossier"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
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

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';
import DynamicForm from '@/components/agent_antenne/document_filling/DynamicForm.vue';

import Button from 'primevue/button';
import Card from 'primevue/card';
import Tag from 'primevue/tag';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Message from 'primevue/message';
import FileUpload from 'primevue/fileupload';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();

// State
const loading = ref(true);
const error = ref('');
const dossierDetail = ref(null);
const documentsData = ref(null);
const documentsLoading = ref(false);
const documentsCollapsed = ref(false);
const actionLoading = ref({});
const isEditing = ref(false);
const editForm = ref({});

// Computed
const dossierId = computed(() => parseInt(route.params.dossierId));
const showDocuments = computed(() => canModifyDossier.value || 
  (documentsData.value && documentsData.value.documentsRequis.length > 0));
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

onMounted(() => {
  loadDossierDetail();
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await ApiService.get(`/agent-antenne/dossiers/detail/${dossierId.value}`);
    dossierDetail.value = response;
    
    if (showDocuments.value) {
      await loadDocuments();
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

async function loadDocuments() {
  try {
    documentsLoading.value = true;
    const response = await ApiService.get(`/agent-antenne/dossiers/${dossierId.value}/documents`);
    documentsData.value = response;
  } catch (err) {
    console.error('Erreur lors du chargement des documents:', err);
    toast.add({
      severity: 'warn',
      summary: 'Attention',
      detail: 'Impossible de charger les documents',
      life: 3000
    });
  } finally {
    documentsLoading.value = false;
  }
}

function goBack() {
  router.push('/agent_antenne/dossiers');
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
  
  // Direct action execution for other actions
  await performAction(action);
}

function enableEditMode() {
  isEditing.value = true;
  // Initialize edit form with current data
  if (dossierDetail.value) {    editForm.value = {
      agriculteur: {
        nom: dossierDetail.value.agriculteur?.nom || '',
        prenom: dossierDetail.value.agriculteur?.prenom || '',
        cin: dossierDetail.value.agriculteur?.cin || '',
        telephone: dossierDetail.value.agriculteur?.telephone || '',
        communeRurale: dossierDetail.value.agriculteur?.communeRurale || '',
        douar: dossierDetail.value.agriculteur?.douar || ''
      },
      reference: dossierDetail.value.reference || '',
      saba: dossierDetail.value.saba || '',
      montantSubvention: dossierDetail.value.montantSubvention || 0
    };
  }
}

function cancelEdit() {
  isEditing.value = false;
  editForm.value = {};
}

async function saveEdit() {  try {
    loading.value = true;    // Validate that we have the required data
    if (!editForm.value.agriculteur?.nom || !editForm.value.agriculteur?.prenom || !editForm.value.agriculteur?.cin) {
      throw new Error('Le nom, prénom et CIN de l\'agriculteur sont requis');
    }
    
    if (!editForm.value.reference || !editForm.value.saba) {
      throw new Error('La référence et le SABA sont requis');
    }// Create minimal update request with only fields that have valid values
    const updateData = {
      agriculteur: {
        nom: editForm.value.agriculteur.nom,
        prenom: editForm.value.agriculteur.prenom,
        cin: editForm.value.agriculteur.cin,
        telephone: editForm.value.agriculteur.telephone
      },
      reference: editForm.value.reference,
      saba: editForm.value.saba,
      montantSubvention: editForm.value.montantSubvention};

    console.log('Sending minimal update data (no null IDs):', updateData);
    await ApiService.put(`/agent-antenne/dossiers/update/${dossierId.value}`, updateData);
    
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
        router.push('/dossiers');
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
      router.push('/dossiers');
    } else {
      await loadDossierDetail();
    }
  }
}

// Document handling
function getUploadedFiles(document) {
  return document.fichierGroups?.flatMap(group => group.files || []) || [];
}

async function onFileSelect(event, documentId) {
  const files = event.files;
  
  for (const file of files) {
    if (!isValidFileType(file.type)) {
      toast.add({
        severity: 'error',
        summary: 'Type de fichier invalide',
        detail: `Le fichier "${file.name}" n'est pas autorisé.`,
        life: 5000
      });
      continue;
    }
    
    if (file.size > 10 * 1024 * 1024) {
      toast.add({
        severity: 'error',
        summary: 'Fichier trop volumineux',
        detail: `Le fichier "${file.name}" dépasse 10MB.`,
        life: 5000
      });
      continue;
    }
    
    await uploadFile(file, documentId);
  }
}

async function uploadFile(file, documentId) {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    await ApiService.post(
      `/agent-antenne/dossiers/${dossierId.value}/documents/${documentId}/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    );

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: `Fichier "${file.name}" uploadé avec succès`,
      life: 3000
    });

    await loadDocuments();
    
  } catch (err) {
    console.error('Erreur upload:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur Upload',
      detail: err.message || 'Erreur lors de l\'upload',
      life: 5000
    });
  }
}

async function saveFormData(formData, documentId) {
  try {
    await ApiService.post(
      `/agent-antenne/dossiers/${dossierId.value}/documents/${documentId}/form-data`,
      { formData }
    );
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Données du formulaire sauvegardées',
      life: 3000
    });
    
    await loadDocuments();
    
  } catch (err) {
    console.error('Erreur sauvegarde:', err);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la sauvegarde',
      life: 5000
    });
  }
}

function confirmDeleteFile(file) {
  confirm.require({
    message: `Supprimer le fichier "${file.nomFichier}" ?`,
    header: 'Confirmer la suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteFile(file.id)
  });
}

async function deleteFile(fileId) {
  try {
    await ApiService.delete(`/agent-antenne/dossiers/${dossierId.value}/documents/piece-jointe/${fileId}`);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Fichier supprimé avec succès',
      life: 3000
    });
    
    await loadDocuments();
    
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Erreur lors de la suppression',
      life: 5000
    });
  }
}

async function downloadFile(file) {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const response = await fetch(`/agent-antenne/dossiers/${dossierId.value}/documents/piece-jointe/${file.id}/download`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) throw new Error('Erreur de téléchargement');
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.nomFichier;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de télécharger le fichier',
      life: 3000
    });
  }
}

// Utility functions
function isValidFileType(mimeType) {
  const validTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
  return validTypes.includes(mimeType);
}

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

function getFileIcon(extension) {
  const icons = {
    pdf: 'pi pi-file-pdf',
    jpg: 'pi pi-image',
    jpeg: 'pi pi-image',
    png: 'pi pi-image',
    gif: 'pi pi-image'
  };
  return icons[extension?.toLowerCase()] || 'pi pi-file';
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString) {
  if (!dateString) return '';
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(dateString));
}

function formatCurrency(amount) {
  if (!amount) return '0 DH';
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD'
  }).format(amount);
}

function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En cours',
    'APPROVED': 'Approuvé',
    'REJECTED': 'Rejeté',
    'RETURNED_FOR_COMPLETION': 'Retourné'
  };
  return labels[status] || status;
}

function getStatusSeverity(status) {
  const severities = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'RETURNED_FOR_COMPLETION': 'warning'
  };
  return severities[status] || 'secondary';
}

function getDocumentStatusLabel(status) {
  const labels = {
    'COMPLETE': 'Complet',
    'MISSING': 'Manquant',
    'PENDING': 'En attente'
  };
  return labels[status] || status;
}

function getDocumentStatusSeverity(status) {
  const severities = {
    'COMPLETE': 'success',
    'MISSING': 'danger',
    'PENDING': 'warning'
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

/* Additional Styling for Detail View */
.agent-antenne-dossier-detail {
  padding: 1.5rem;
  background: #f8f9fa;
  min-height: 100vh;
}

:deep(.p-card) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e5e7eb;
}

:deep(.p-card .p-card-header) {
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  border-radius: 12px 12px 0 0;
}

:deep(.p-card .p-card-title) {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.document-section {
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.document-header {
  padding: 1rem 1.25rem;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: between;
  align-items: center;
}

.document-content {
  padding: 1.25rem;
}

.file-item {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.75rem;
  transition: all 0.2s ease;
}

.file-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.1);
}

.progress-section {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-badge.draft {
  background: #f3f4f6;
  color: #6b7280;
}

.status-badge.submitted {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-badge.approved {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #dc2626;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.info-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
}

.info-label {
  font-weight: 500;
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.info-value {
  color: #1f2937;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Responsive improvements */
@media (max-width: 768px) {
  .agent-antenne-dossier-detail {
    padding: 1rem;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .document-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style>