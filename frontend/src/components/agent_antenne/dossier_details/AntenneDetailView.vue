<template>
  <DossierDetailBase 
    :breadcrumb-root="'Mes Dossiers'"
    :user-role="'AGENT_ANTENNE'"
    @dossier-loaded="handleDossierLoaded"
    ref="baseComponent"
  >
    <!-- Header Actions -->
    <template #header-actions="{ dossier }">
      <Button 
        v-if="canEdit(dossier)"
        label="Envoyer au GUC" 
        icon="pi pi-send" 
        @click="confirmSendToGUC(dossier)"
        class="p-button-success"
      />
      <Button 
        v-if="canDelete(dossier)"
        label="Supprimer" 
        icon="pi pi-trash" 
        @click="confirmDelete(dossier)"
        class="p-button-danger"
      />
    </template>

    <!-- Forms Section -->
    <template #forms-section="{ dossier, forms }">
      <div class="forms-section">
        <div class="section-header">
          <h2><i class="pi pi-file-edit"></i> Documents et Formulaires</h2>
          <p>Complétez tous les documents requis pour ce type de projet</p>
        </div>

        <!-- Editable forms -->
        <div class="forms-actions">
          <Button 
            label="Remplir les Documents" 
            icon="pi pi-file-edit" 
            @click="goToDocumentFilling"
            class="p-button-success p-button-lg"
            :disabled="!canEdit(dossier.dossier)"
          />
          <p class="forms-help">
            Cliquez pour accéder à l'interface de remplissage des documents et formulaires requis.
          </p>
        </div>

        <!-- Forms Overview -->
        <div v-if="forms && forms.length > 0" class="forms-overview">
          <h3>Aperçu des formulaires ({{ forms.length }})</h3>
          <div class="forms-grid">
            <div 
              v-for="form in forms" 
              :key="form.formId"
              class="form-card"
              :class="{ 'form-completed': form.isCompleted }"
            >
              <div class="form-info">
                <h4>{{ form.title }}</h4>
                <div class="form-tags">
                  <Tag v-if="form.isCompleted" value="Complété" severity="success" />
                  <Tag v-else value="En attente" severity="warning" />
                </div>
              </div>
              <p v-if="form.description" class="form-description">{{ form.description }}</p>
              <div v-if="form.lastModified" class="form-meta">
                <small>Dernière modification: {{ formatDate(form.lastModified) }}</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Files Actions -->
    <template #files-actions="{ files }">
      <Button 
        label="Gérer les documents" 
        icon="pi pi-external-link" 
        @click="goToDocumentFilling"
        :disabled="!canEdit(currentDossier)"
        class="p-button-outlined p-button-sm"
      />
    </template>

    <!-- File Actions -->
    <template #file-actions="{ file }">
      <Button 
        v-if="canDeleteFile(file)"
        icon="pi pi-trash" 
        @click="confirmDeleteFile(file)"
        class="p-button-danger p-button-outlined p-button-sm"
        v-tooltip.top="'Supprimer'"
      />
    </template>
  </DossierDetailBase>

  <!-- Action Dialogs -->
  <ActionDialogs 
    :dialogs="actionDialogs"
    @action-confirmed="handleActionConfirmed"
    @dialog-closed="handleDialogClosed"
  />
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import DossierDetailBase from '@/components/dossiers/DossierDetailBase.vue';
import ActionDialogs from '@/components/dossiers/ActionDialogs.vue';
import ApiService from '@/services/ApiService';

// PrimeVue components
import Button from 'primevue/button';
import Tag from 'primevue/tag';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const baseComponent = ref();

// State
const currentDossier = ref(null);

// Dialogs
const actionDialogs = reactive({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  delete: { visible: false, dossier: null, loading: false, comment: '' },
  deleteFile: { visible: false, file: null, loading: false }
});

// Permission methods
function canEdit(dossier) {
  const status = dossier?.statut;
  return status === 'DRAFT' || status === 'Brouillon' || status === 'RETURNED_FOR_COMPLETION' || status === 'Retourné pour complétion';
}

function canDelete(dossier) {
  const status = dossier?.statut;
  return status === 'DRAFT' || status === 'Brouillon';
}

function canDeleteFile(file) {
  return canEdit(currentDossier.value);
}

// Methods
function handleDossierLoaded(dossier) {
  currentDossier.value = dossier.dossier;
}

function goToDocumentFilling() {
  router.push(`/agent_antenne/dossiers/documents/${route.params.dossierId}`);
}

function confirmSendToGUC(dossier) {
  actionDialogs.sendToGUC = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmDelete(dossier) {
  actionDialogs.delete = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmDeleteFile(file) {
  actionDialogs.deleteFile = {
    visible: true,
    file: file,
    loading: false
  };
}

async function handleActionConfirmed(actionData) {
  try {
    const { action, dossier, file, data } = actionData;
    
    let endpoint = '';
    let payload = {};
    
    switch (action) {
      case 'sendToGUC':
        endpoint = `/dossiers/${dossier.id}/send-to-guc`;
        payload = { commentaire: data.comment };
        break;
      case 'delete':
        endpoint = `/dossiers/${dossier.id}`;
        payload = { motif: data.comment };
        break;
      case 'deleteFile':
        endpoint = `/dossiers/${route.params.dossierId}/documents/piece-jointe/${file.id}`;
        payload = {};
        break;
    }
    
    const method = (action === 'delete' || action === 'deleteFile') ? 'delete' : 'post';
    const response = await ApiService[method](endpoint, payload);
    
    if (response.success || action === 'deleteFile') {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Action effectuée avec succès',
        life: 4000
      });
      
      if (action === 'delete') {
        router.push('/agent_antenne/dossiers');
      } else if (baseComponent.value) {
        baseComponent.value.loadDossierDetail();
      }
    }
    
  } catch (error) {
    console.error(`Erreur lors de l'action ${actionData.action}:`, error);
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

<style scoped>
/* Forms Section */
.forms-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1.5rem;
}

.section-header h2 {
  color: var(--primary-color);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-header p {
  color: #6b7280;
  margin: 0;
}

.forms-actions {
  text-align: center;
  padding: 2rem;
  background: var(--section-background);
  border-radius: 12px;
  border: 2px dashed var(--primary-color);
  margin-bottom: 2rem;
}

.forms-actions .p-button-lg {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}

.forms-help {
  margin-top: 1rem;
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.4;
}

.forms-overview {
  background: var(--section-background);
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.forms-overview h3 {
  color: var(--primary-color);
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.forms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.form-card {
  background: var(--background-color);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  transition: all 0.3s ease;
}

.form-card.form-completed {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.form-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.form-info h4 {
  margin: 0;
  color: #374151;
  font-size: 0.9rem;
  flex: 1;
}

.form-tags {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.form-description {
  color: #6b7280;
  font-size: 0.8rem;
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.form-meta {
  color: #6b7280;
  font-size: 0.75rem;
  margin-bottom: 0.5rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .forms-grid {
    grid-template-columns: 1fr;
  }
}
</style>