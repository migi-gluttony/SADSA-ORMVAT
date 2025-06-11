<template>
  <div class="document-filling-container">
    <!-- Navigation -->
    <div class="mb-4">
      <Button 
        label="Retour aux détails" 
        icon="pi pi-arrow-left" 
        @click="goBack"
        class="p-button-outlined"
      />
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center p-6">
      <ProgressSpinner size="50px" />
      <p class="mt-3">Chargement des informations du dossier...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center p-6">
      <Message severity="error" :closable="false">
        {{ error }}
      </Message>
      <Button 
        label="Réessayer" 
        icon="pi pi-refresh" 
        @click="loadDossierData"
        class="p-button-outlined mt-3"
      />
    </div>

    <!-- Main Content -->
    <div v-else-if="dossierData" class="space-y-4">
      
      <!-- Dossier Header -->
      <Card>
        <template #header>          <div class="flex justify-content-between align-items-center p-3">
            <div>
              <h2 class="m-0">{{ dossierData.dossier?.numeroDossier || 'Numéro non disponible' }}</h2>
              <p class="m-0 text-color-secondary">{{ dossierData.dossier?.saba || '' }}</p>
            </div>
            <Tag 
              :value="getStatusLabel(dossierData.dossier?.status || 'UNKNOWN')" 
              :severity="getStatusSeverity(dossierData.dossier?.status || 'UNKNOWN')"
            />
          </div>
        </template>
        <template #content>
          <!-- Agriculteur Info -->          <div class="grid">
            <div class="col-12 md:col-6">
              <h4 class="mt-0">Agriculteur</h4>
              <p><strong>Nom:</strong> {{ dossierData.dossier?.agriculteurPrenom || '' }} {{ dossierData.dossier?.agriculteurNom || '' }}</p>
              <p><strong>CIN:</strong> {{ dossierData.dossier?.agriculteurCin || '' }}</p>
              <p><strong>Téléphone:</strong> {{ dossierData.dossier?.agriculteurTelephone || '' }}</p>
            </div>
            <div class="col-12 md:col-6">
              <h4 class="mt-0">Projet</h4>
              <p><strong>Type:</strong> {{ dossierData.dossier?.sousRubriqueDesignation || '' }}</p>
              <p><strong>Antenne:</strong> {{ dossierData.dossier?.antenneDesignation || '' }}</p>
            </div>
          </div>

          <!-- Progress -->
          <div v-if="dossierData.statistics" class="mt-4">
            <div class="flex justify-content-between align-items-center mb-2">
              <h4 class="m-0">Progression des Documents</h4>
              <span class="text-xl font-bold text-primary">
                {{ Math.round(dossierData.statistics.pourcentageCompletion || 0) }}%
              </span>
            </div>
            <ProgressBar :value="dossierData.statistics.pourcentageCompletion || 0" />
            <div class="flex justify-content-between text-sm text-color-secondary mt-2">
              <span>{{ dossierData.statistics.documentsCompletes || 0 }} complétés</span>
              <span>{{ dossierData.statistics.totalDocuments || 0 }} total</span>
            </div>
          </div>
        </template>
      </Card>

      <!-- Documents Section -->
      <Card>
        <template #title>
          <i class="pi pi-file-edit"></i> Documents Requis
        </template>
        <template #content>          <div class="space-y-4">
            <div 
              v-for="document in (dossierData.documentsRequis || [])" 
              :key="document.documentRequisId || document.id"
              class="border-1 surface-border border-round p-3"
            >
              <!-- Document Header -->
              <div class="flex justify-content-between align-items-start mb-3">
                <div class="flex-1">
                  <h4 class="m-0 mb-2">
                    {{ document.nomDocument || 'Document sans nom' }}
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
                  </h4>
                  <p v-if="document.description" class="m-0 text-color-secondary">
                    {{ document.description }}
                  </p>
                </div>
                <Tag 
                  :value="getDocumentStatusLabel(document.status)" 
                  :severity="getDocumentStatusSeverity(document.status)"
                />
              </div>

              <!-- File Upload Section -->
              <div class="mb-4">
                <h5 class="mb-2">
                  <i class="pi pi-upload"></i> Fichiers
                </h5>
                  <FileUpload
                  v-if="document.documentRequisId"
                  mode="basic"
                  accept=".pdf,.jpg,.jpeg,.png,.gif"
                  :maxFileSize="10000000"
                  :multiple="true"
                  @select="(event) => onFileSelect(event, document.documentRequisId)"
                  :auto="false"
                  chooseLabel="Sélectionner fichier(s)"
                  class="mb-3"
                />
                <div v-else class="p-3 surface-100 border-round text-center">
                  <i class="pi pi-exclamation-triangle text-orange-500"></i>
                  <p class="mt-2 mb-0">Upload non disponible - ID document manquant</p>
                </div>
                <small class="block text-color-secondary">
                  Formats acceptés: PDF, JPG, PNG, GIF (max 10MB par fichier)
                </small>

                <!-- Uploaded Files -->
                <div v-if="getUploadedFiles(document).length > 0" class="mt-3">
                  <h6 class="mb-2">Fichiers uploadés ({{ getUploadedFiles(document).length }})</h6>
                  <div class="space-y-2">
                    <div 
                      v-for="file in getUploadedFiles(document)" 
                      :key="file.id"
                      class="flex justify-content-between align-items-center p-2 surface-50 border-round"
                    >
                      <div class="flex align-items-center">
                        <i :class="getFileIcon(file.nomFichier)" class="text-primary mr-2"></i>
                        <div>
                          <div class="font-medium">{{ file.nomFichier }}</div>
                          <div class="text-sm text-color-secondary">
                            {{ formatDate(file.dateUpload) }}
                          </div>
                        </div>
                      </div>
                      <div class="flex gap-2">
                        <Button 
                          icon="pi pi-download" 
                          @click="downloadFile(file)"
                          class="p-button-outlined"
                          size="small"
                        />
                        <Button 
                          icon="pi pi-trash" 
                          @click="confirmDeleteFile(file)"
                          class="p-button-danger p-button-outlined"
                          size="small"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>              <!-- Form Section -->
              <div v-if="document.formStructure && Object.keys(document.formStructure).length > 0">
                <h5 class="mb-2">
                  <i class="pi pi-list"></i> Formulaire
                </h5>
                <div class="surface-50 p-3 border-round">
                  <DynamicForm
                    v-if="document.formStructure && document.documentRequisId"
                    :formStructure="document.formStructure"
                    :formData="getFormDataForDocument(document)"
                    @form-save="(data) => saveFormData(data, document.documentRequisId)"
                  />
                  <div v-else class="text-center p-4 text-color-secondary">
                    <i class="pi pi-info-circle"></i>
                    <p class="mt-2">Aucun formulaire disponible pour ce document</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Confirmation Dialogs -->
    <ConfirmDialog />
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
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
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();

const loading = ref(true);
const error = ref('');
const dossierData = ref({
  dossier: {},
  documentsRequis: [],
  statistics: {}
});

const dossierId = computed(() => parseInt(route.params.dossierId));

onMounted(() => {
  loadDossierData();
});

async function loadDossierData() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await ApiService.get(`/agent_antenne/documents/dossier/${dossierId.value}`);
    
    // Ensure we have proper data structure
    if (response) {
      const data = response.data || response;
      dossierData.value = {
        dossier: data.dossier || {},
        documentsRequis: Array.isArray(data.documentsRequis) ? data.documentsRequis : [],
        statistics: data.statistics || {}
      };
      
      // Ensure each document has proper structure
      if (dossierData.value.documentsRequis) {
        dossierData.value.documentsRequis = dossierData.value.documentsRequis.map(doc => ({
          ...doc,
          pieces: Array.isArray(doc.pieces) ? doc.pieces : [],
          formStructure: doc.formStructure || {}
        }));
      }
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

function getUploadedFiles(document) {
  if (!document || !document.pieces || !Array.isArray(document.pieces)) {
    return [];
  }
  return document.pieces.filter(piece => piece && piece.hasFile) || [];
}

function getFormDataForDocument(document) {
  if (!document || !document.pieces || !Array.isArray(document.pieces)) {
    return {};
  }
  
  const formDataPiece = document.pieces.find(piece => piece && piece.hasFormData);
  return formDataPiece && formDataPiece.formData ? formDataPiece.formData : {};
}

async function onFileSelect(event, documentId) {
  if (!event || !event.files || !Array.isArray(event.files)) {
    console.warn('Invalid file selection event');
    return;
  }
  
  if (!documentId) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'ID du document manquant',
      life: 5000
    });
    return;
  }
  
  const files = event.files;
  
  for (const file of files) {
    if (!file || !file.type || !file.name) {
      toast.add({
        severity: 'error',
        summary: 'Fichier invalide',
        detail: 'Fichier corrompu ou incomplet',
        life: 5000
      });
      continue;
    }
    
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
    formData.append('title', `Document scanné - ${file.name}`);
    formData.append('description', 'Document uploadé par l\'agent antenne');
    
    // Create a custom axios instance for this multipart request
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const response = await fetch(`/api/agent_antenne/documents/upload-scanned/${dossierId.value}/${documentId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
        // Don't set Content-Type - let browser set it with boundary
      },
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: `Fichier "${file.name}" uploadé avec succès`,
      life: 3000
    });

    await loadDossierData();
    
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
    const requestData = {
      dossierId: dossierId.value,
      documentRequisId: documentId,
      formData: formData,
      title: 'Données formulaire',
      description: 'Données saisies par l\'agent antenne'
    };
    
    await ApiService.post('/agent_antenne/documents/save-form-data', requestData);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Données du formulaire sauvegardées',
      life: 3000
    });
    
    await loadDossierData();
    
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
    await ApiService.delete(`/agent_antenne/documents/piece/${fileId}`);
    
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Fichier supprimé avec succès',
      life: 3000
    });
    
    await loadDossierData();
    
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
    const response = await fetch(`/api/agent_antenne/documents/download/${file.id}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
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

function goBack() {
  router.push(`/agent_antenne/dossiers/${dossierId.value}`);
}

// Utility functions
function isValidFileType(mimeType) {
  const validTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
  return validTypes.includes(mimeType);
}

function getFileIcon(fileName) {
  if (!fileName) return 'pi pi-file';
  
  const extension = fileName.split('.').pop()?.toLowerCase();
  const icons = {
    pdf: 'pi pi-file-pdf',
    jpg: 'pi pi-image',
    jpeg: 'pi pi-image',
    png: 'pi pi-image',
    gif: 'pi pi-image'
  };
  return icons[extension] || 'pi pi-file';
}

function formatDate(dateString) {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    
    // Use a more compatible approach
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

function getStatusLabel(status) {
  if (!status) return 'Statut inconnu';
  const labels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En cours',
    'APPROVED': 'Approuvé',
    'REJECTED': 'Rejeté',
    'UNKNOWN': 'Statut inconnu'
  };
  return labels[status] || status;
}

function getStatusSeverity(status) {
  if (!status) return 'secondary';
  const severities = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'UNKNOWN': 'secondary'
  };
  return severities[status] || 'secondary';
}

function getDocumentStatusLabel(status) {
  if (!status) return 'Statut inconnu';
  const labels = {
    'COMPLETE': 'Complet',
    'MISSING': 'Manquant',
    'PENDING': 'En attente'
  };
  return labels[status] || status;
}

function getDocumentStatusSeverity(status) {
  if (!status) return 'secondary';
  const severities = {
    'COMPLETE': 'success',
    'MISSING': 'danger',
    'PENDING': 'warning'
  };
  return severities[status] || 'secondary';
}
</script>

<style scoped>
.space-y-4 > * + * {
  margin-top: 1rem;
}

.space-y-2 > * + * {
  margin-top: 0.5rem;
}
</style>