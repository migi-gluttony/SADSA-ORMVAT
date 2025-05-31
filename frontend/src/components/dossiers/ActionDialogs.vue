<template>
  <!-- Send to GUC Dialog -->
  <Dialog 
    v-model:visible="dialogs.sendToGUC.visible" 
    modal 
    header="Envoyer au GUC"
    :style="{ width: '500px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-send confirmation-icon"></i>
      <div class="confirmation-text">
        <p>Confirmer l'envoi de ce dossier au Guichet Unique Central ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogs.sendToGUC.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogs.sendToGUC.dossier) }}<br>
          <small>Type: {{ dialogs.sendToGUC.dossier?.sousRubriqueDesignation }}</small>
        </div>
        <p class="info-text">Une fois envoyé, le dossier ne pourra plus être modifié à l'antenne et sera traité par le GUC.</p>
      </div>
    </div>
    
    <div class="action-comment">
      <label for="sendComment">Commentaire pour le GUC (optionnel)</label>
      <Textarea 
        id="sendComment"
        v-model="dialogs.sendToGUC.comment" 
        rows="3" 
        placeholder="Commentaires ou instructions pour le GUC..."
      />
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('sendToGUC')"
        class="p-button-outlined"
      />
      <Button 
        label="Envoyer au GUC" 
        icon="pi pi-send" 
        @click="confirmAction('sendToGUC')"
        class="p-button-success"
        :loading="dialogs.sendToGUC.loading"
      />
    </template>
  </Dialog>

  <!-- Send to Commission Dialog -->
  <Dialog 
    v-model:visible="dialogs.sendToCommission.visible" 
    modal 
    header="Envoyer à la Commission AHA-AF"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-forward confirmation-icon"></i>
      <div class="confirmation-text">
        <p>Envoyer ce dossier à la Commission AHA-AF pour visite terrain ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogs.sendToCommission.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogs.sendToCommission.dossier) }}<br>
          <small>Type: {{ dialogs.sendToCommission.dossier?.sousRubriqueDesignation }}</small>
        </div>
        <p class="info-text">La commission effectuera une visite terrain pour évaluer la conformité du projet.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="commissionComment">Commentaire pour la Commission *</label>
        <Textarea 
          id="commissionComment"
          v-model="dialogs.sendToCommission.comment" 
          rows="3" 
          placeholder="Instructions spécifiques pour la commission..."
          :class="{ 'p-invalid': !dialogs.sendToCommission.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <label for="commissionPriority">Priorité du dossier</label>
        <Select 
          id="commissionPriority"
          v-model="dialogs.sendToCommission.priority" 
          :options="priorityOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Sélectionner la priorité"
          class="w-full"
        />
      </div>
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('sendToCommission')"
        class="p-button-outlined"
      />
      <Button 
        label="Envoyer à la Commission" 
        icon="pi pi-forward" 
        @click="confirmAction('sendToCommission')"
        class="p-button-info"
        :loading="dialogs.sendToCommission.loading"
        :disabled="!dialogs.sendToCommission.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Return to Antenne Dialog -->
  <Dialog 
    v-model:visible="dialogs.returnToAntenne.visible" 
    modal 
    header="Retourner à l'Antenne"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-undo warning-icon"></i>
      <div class="confirmation-text">
        <p>Retourner ce dossier à l'antenne pour complétion ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogs.returnToAntenne.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogs.returnToAntenne.dossier) }}
        </div>
        <p class="warning-text">Le dossier repassera en mode modification à l'antenne.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="returnComment">Motif du retour *</label>
        <Textarea 
          id="returnComment"
          v-model="dialogs.returnToAntenne.comment" 
          rows="4" 
          placeholder="Expliquez pourquoi le dossier est retourné et ce qui doit être corrigé..."
          :class="{ 'p-invalid': !dialogs.returnToAntenne.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <label>Documents ou informations manquants</label>
        <div class="checkbox-group">
          <div class="checkbox-item" v-for="reason in returnReasons" :key="reason.value">
            <Checkbox 
              :id="reason.value"
              v-model="dialogs.returnToAntenne.reasons" 
              :value="reason.value"
            />
            <label :for="reason.value">{{ reason.label }}</label>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('returnToAntenne')"
        class="p-button-outlined"
      />
      <Button 
        label="Retourner à l'Antenne" 
        icon="pi pi-undo" 
        @click="confirmAction('returnToAntenne')"
        class="p-button-warning"
        :loading="dialogs.returnToAntenne.loading"
        :disabled="!dialogs.returnToAntenne.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Reject Dialog -->
  <Dialog 
    v-model:visible="dialogs.reject.visible" 
    modal 
    header="Rejeter le Dossier"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-times-circle danger-icon"></i>
      <div class="confirmation-text">
        <p>Rejeter définitivement ce dossier ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogs.reject.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogs.reject.dossier) }}
        </div>
        <p class="danger-text">Cette action est <strong>définitive</strong> et ne peut pas être annulée.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="rejectComment">Motif du rejet *</label>
        <Textarea 
          id="rejectComment"
          v-model="dialogs.reject.comment" 
          rows="4" 
          placeholder="Expliquez clairement les raisons du rejet du dossier..."
          :class="{ 'p-invalid': !dialogs.reject.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <div class="checkbox-item">
          <Checkbox 
            id="definitiveReject"
            v-model="dialogs.reject.definitive" 
            :binary="true"
          />
          <label for="definitiveReject">Rejet définitif (l'agriculteur ne pourra pas resoumettre)</label>
        </div>
      </div>
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('reject')"
        class="p-button-outlined"
      />
      <Button 
        label="Rejeter le Dossier" 
        icon="pi pi-times-circle" 
        @click="confirmAction('reject')"
        class="p-button-danger"
        :loading="dialogs.reject.loading"
        :disabled="!dialogs.reject.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Delete Dialog -->
  <Dialog 
    v-model:visible="dialogs.delete.visible" 
    modal 
    header="Confirmer la suppression"
    :style="{ width: '450px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-exclamation-triangle danger-icon"></i>
      <div class="confirmation-text">
        <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogs.delete.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogs.delete.dossier) }}
        </div>
        <p class="danger-text">Cette action est <strong>irréversible</strong>.</p>
      </div>
    </div>
    
    <div class="action-comment">
      <label for="deleteComment">Motif de suppression (optionnel)</label>
      <Textarea 
        id="deleteComment"
        v-model="dialogs.delete.comment" 
        rows="3" 
        placeholder="Raison de la suppression..."
      />
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('delete')"
        class="p-button-outlined"
      />
      <Button 
        label="Supprimer" 
        icon="pi pi-trash" 
        @click="confirmAction('delete')"
        class="p-button-danger"
        :loading="dialogs.delete.loading"
      />
    </template>
  </Dialog>

  <!-- Delete File Dialog -->
  <Dialog 
    v-model:visible="dialogs.deleteFile.visible" 
    modal 
    header="Supprimer le fichier"
    :style="{ width: '400px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-exclamation-triangle warning-icon"></i>
      <div class="confirmation-text">
        <p>Supprimer ce fichier ?</p>
        <div class="file-summary">
          <strong>{{ dialogs.deleteFile.file?.nomFichier }}</strong><br>
          <small>{{ dialogs.deleteFile.file?.typeDocument }}</small>
        </div>
        <p class="warning-text">Cette action ne peut pas être annulée.</p>
      </div>
    </div>

    <template #footer>
      <Button 
        label="Annuler" 
        icon="pi pi-times" 
        @click="closeDialog('deleteFile')"
        class="p-button-outlined"
      />
      <Button 
        label="Supprimer" 
        icon="pi pi-trash" 
        @click="confirmAction('deleteFile')"
        class="p-button-danger"
        :loading="dialogs.deleteFile.loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue';

// PrimeVue components
import Dialog from 'primevue/dialog';
import Button from 'primevue/button';
import Textarea from 'primevue/textarea';
import Select from 'primevue/select';
import Checkbox from 'primevue/checkbox';

// Props
const props = defineProps({
  dialogs: {
    type: Object,
    required: true
  }
});

// Emits
const emit = defineEmits(['action-confirmed', 'dialog-closed']);

// Options
const priorityOptions = ref([
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

const returnReasons = ref([
  { label: 'Documents manquants', value: 'DOCUMENTS_MANQUANTS' },
  { label: 'Informations incomplètes', value: 'INFORMATIONS_INCOMPLETES' },
  { label: 'Erreurs dans les formulaires', value: 'ERREURS_FORMULAIRES' },
  { label: 'Pièces justificatives non conformes', value: 'PIECES_NON_CONFORMES' },
  { label: 'Montant incorrect', value: 'MONTANT_INCORRECT' },
  { label: 'Type de projet non éligible', value: 'TYPE_NON_ELIGIBLE' },
  { label: 'Autres (voir commentaires)', value: 'AUTRES' }
]);

// Methods
function getAgriculteurName(dossier) {
  if (!dossier) return '';
  return `${dossier.agriculteurPrenom || ''} ${dossier.agriculteurNom || ''}`.trim();
}

function closeDialog(action) {
  emit('dialog-closed');
}

function confirmAction(action) {
  const dialog = props.dialogs[action];
  
  let data = {
    comment: dialog.comment || ''
  };
  
  // Add action-specific data
  switch (action) {
    case 'sendToCommission':
      data.priority = dialog.priority || 'NORMALE';
      break;
    case 'returnToAntenne':
      data.reasons = dialog.reasons || [];
      break;
    case 'reject':
      data.definitive = dialog.definitive || false;
      break;
  }
  
  // Set loading state
  dialog.loading = true;
  
  emit('action-confirmed', {
    action,
    dossier: dialog.dossier,
    file: dialog.file,
    data
  });
}
</script>

<style scoped>
/* Action confirmation styles */
.action-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.confirmation-icon {
  color: var(--primary-color);
  font-size: 2rem;
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.warning-icon {
  color: var(--warning-color);
  font-size: 2rem;
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.danger-icon {
  color: var(--danger-color);
  font-size: 2rem;
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.confirmation-text {
  flex: 1;
}

.confirmation-text p {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-size: 1rem;
  line-height: 1.5;
}

.dossier-summary {
  background: var(--section-background);
  padding: 1rem;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--card-border);
  margin: 1rem 0;
  font-size: 0.9rem;
}

.file-summary {
  background: var(--section-background);
  padding: 0.75rem;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--card-border);
  margin: 1rem 0;
  font-size: 0.9rem;
}

.info-text {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0;
  line-height: 1.4;
}

.warning-text {
  color: var(--warning-color);
  font-weight: 500;
  font-size: 0.9rem;
  margin: 0;
}

.danger-text {
  color: var(--danger-color);
  font-weight: 500;
  font-size: 0.9rem;
  margin: 0;
}

/* Form styles */
.action-form {
  margin-top: 1.5rem;
}

.action-comment {
  margin-top: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--text-color);
  font-size: 0.9rem;
}

.form-group .w-full {
  width: 100%;
}

/* Checkbox styles */
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.checkbox-item label {
  margin: 0;
  font-weight: 400;
  font-size: 0.9rem;
  cursor: pointer;
  line-height: 1.4;
}

/* Validation styles */
:deep(.p-invalid) {
  border-color: var(--danger-color) !important;
}

:deep(.p-invalid:focus) {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2) !important;
}

/* Dialog content spacing */
:deep(.p-dialog-content) {
  padding: 1.5rem;
}

:deep(.p-dialog-footer) {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--card-border);
  background: var(--section-background);
}

/* Button spacing in footer */
:deep(.p-dialog-footer .p-button) {
  margin-left: 0.5rem;
}

:deep(.p-dialog-footer .p-button:first-child) {
  margin-left: 0;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .action-confirmation {
    flex-direction: column;
    text-align: center;
  }
  
  .confirmation-icon,
  .warning-icon,
  .danger-icon {
    align-self: center;
    margin-top: 0;
  }
  
  .checkbox-group {
    align-items: flex-start;
  }
  
  .checkbox-item {
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .checkbox-item label {
    margin-top: 0.125rem;
  }
}

/* Dark mode adjustments */
.dark-mode .dossier-summary,
.dark-mode .file-summary {
  background: var(--clr-surface-a20);
  border-color: var(--clr-surface-a30);
}

.dark-mode .confirmation-text p {
  color: var(--text-color);
}

.dark-mode .form-group label {
  color: var(--text-color);
}
</style>