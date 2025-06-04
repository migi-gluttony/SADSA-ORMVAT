<template>
  <!-- Send to GUC Dialog -->
  <Dialog 
    :visible="dialogsState.sendToGUC.visible" 
    @update:visible="updateDialogVisibility('sendToGUC', $event)"
    modal 
    header="Envoyer au GUC"
    :style="{ width: '500px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-send confirmation-icon"></i>
      <div class="confirmation-text">
        <p>Confirmer l'envoi de ce dossier au Guichet Unique Central ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogsState.sendToGUC.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogsState.sendToGUC.dossier) }}<br>
          <small>Type: {{ dialogsState.sendToGUC.dossier?.sousRubriqueDesignation }}</small>
        </div>
        <p class="info-text">Une fois envoyé, le dossier ne pourra plus être modifié à l'antenne et sera traité par le GUC.</p>
      </div>
    </div>
    
    <div class="action-comment">
      <label for="sendComment">Commentaire pour le GUC (optionnel)</label>
      <Textarea 
        id="sendComment"
        v-model="dialogsState.sendToGUC.comment" 
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
        :loading="dialogsState.sendToGUC.loading"
      />
    </template>
  </Dialog>

  <!-- Send to Commission Dialog -->
  <Dialog 
    :visible="dialogsState.sendToCommission.visible" 
    @update:visible="updateDialogVisibility('sendToCommission', $event)"
    modal 
    header="Envoyer à la Commission AHA-AF"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-forward confirmation-icon"></i>
      <div class="confirmation-text">
        <p>Envoyer ce dossier à la Commission visite terrain ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogsState.sendToCommission.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogsState.sendToCommission.dossier) }}<br>
          <small>Type: {{ dialogsState.sendToCommission.dossier?.sousRubriqueDesignation }}</small>
        </div>
        <p class="info-text">La commission effectuera une visite terrain pour évaluer la conformité du projet.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="commissionComment">Commentaire pour la Commission *</label>
        <Textarea 
          id="commissionComment"
          v-model="dialogsState.sendToCommission.comment" 
          rows="3" 
          placeholder="Instructions spécifiques pour la commission..."
          :class="{ 'p-invalid': !dialogsState.sendToCommission.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <label for="commissionPriority">Priorité du dossier</label>
        <Select 
          id="commissionPriority"
          v-model="dialogsState.sendToCommission.priority" 
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
        :loading="dialogsState.sendToCommission.loading"
        :disabled="!dialogsState.sendToCommission.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Return to Antenne Dialog -->
  <Dialog 
    :visible="dialogsState.returnToAntenne.visible" 
    @update:visible="updateDialogVisibility('returnToAntenne', $event)"
    modal 
    header="Retourner à l'Antenne"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-undo warning-icon"></i>
      <div class="confirmation-text">
        <p>Retourner ce dossier à l'antenne pour complétion ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogsState.returnToAntenne.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogsState.returnToAntenne.dossier) }}
        </div>
        <p class="warning-text">Le dossier repassera en mode modification à l'antenne.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="returnComment">Motif du retour *</label>
        <Textarea 
          id="returnComment"
          v-model="dialogsState.returnToAntenne.comment" 
          rows="4" 
          placeholder="Expliquez pourquoi le dossier est retourné et ce qui doit être corrigé..."
          :class="{ 'p-invalid': !dialogsState.returnToAntenne.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <label>Documents ou informations manquants</label>
        <div class="checkbox-group">
          <div class="checkbox-item" v-for="reason in returnReasons" :key="reason.value">
            <Checkbox 
              :id="reason.value"
              v-model="dialogsState.returnToAntenne.reasons" 
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
        :loading="dialogsState.returnToAntenne.loading"
        :disabled="!dialogsState.returnToAntenne.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Reject Dialog -->
  <Dialog 
    :visible="dialogsState.reject.visible" 
    @update:visible="updateDialogVisibility('reject', $event)"
    modal 
    header="Rejeter le Dossier"
    :style="{ width: '600px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-times-circle danger-icon"></i>
      <div class="confirmation-text">
        <p>Rejeter définitivement ce dossier ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogsState.reject.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogsState.reject.dossier) }}
        </div>
        <p class="danger-text">Cette action est <strong>définitive</strong> et ne peut pas être annulée.</p>
      </div>
    </div>
    
    <div class="action-form">
      <div class="form-group">
        <label for="rejectComment">Motif du rejet *</label>
        <Textarea 
          id="rejectComment"
          v-model="dialogsState.reject.comment" 
          rows="4" 
          placeholder="Expliquez clairement les raisons du rejet du dossier..."
          :class="{ 'p-invalid': !dialogsState.reject.comment?.trim() }"
        />
      </div>
      
      <div class="form-group">
        <div class="checkbox-item">
          <Checkbox 
            id="definitiveReject"
            v-model="dialogsState.reject.definitive" 
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
        :loading="dialogsState.reject.loading"
        :disabled="!dialogsState.reject.comment?.trim()"
      />
    </template>
  </Dialog>

  <!-- Delete Dialog -->
  <Dialog 
    :visible="dialogsState.delete.visible" 
    @update:visible="updateDialogVisibility('delete', $event)"
    modal 
    header="Confirmer la suppression"
    :style="{ width: '450px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-exclamation-triangle danger-icon"></i>
      <div class="confirmation-text">
        <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
        <div class="dossier-summary">
          <strong>{{ dialogsState.delete.dossier?.reference }}</strong><br>
          {{ getAgriculteurName(dialogsState.delete.dossier) }}
        </div>
        <p class="danger-text">Cette action est <strong>irréversible</strong>.</p>
      </div>
    </div>
    
    <div class="action-comment">
      <label for="deleteComment">Motif de suppression (optionnel)</label>
      <Textarea 
        id="deleteComment"
        v-model="dialogsState.delete.comment" 
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
        :loading="dialogsState.delete.loading"
      />
    </template>
  </Dialog>

  <!-- Delete File Dialog -->
  <Dialog 
    :visible="dialogsState.deleteFile.visible" 
    @update:visible="updateDialogVisibility('deleteFile', $event)"
    modal 
    header="Supprimer le fichier"
    :style="{ width: '400px' }"
  >
    <div class="action-confirmation">
      <i class="pi pi-exclamation-triangle warning-icon"></i>
      <div class="confirmation-text">
        <p>Supprimer ce fichier ?</p>
        <div class="file-summary">
          <strong>{{ dialogsState.deleteFile.file?.nomFichier }}</strong><br>
          <small>{{ dialogsState.deleteFile.file?.typeDocument }}</small>
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
        :loading="dialogsState.deleteFile.loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';

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

// Internal state management
const dialogsState = reactive({
  sendToGUC: { visible: false, dossier: null, loading: false, comment: '' },
  sendToCommission: { visible: false, dossier: null, loading: false, comment: '', priority: 'NORMALE' },
  returnToAntenne: { visible: false, dossier: null, loading: false, comment: '', reasons: [] },
  reject: { visible: false, dossier: null, loading: false, comment: '', definitive: false },
  delete: { visible: false, dossier: null, loading: false, comment: '' },
  deleteFile: { visible: false, file: null, loading: false }
});

// Watch props and sync internal state
watch(() => props.dialogs, (newDialogs) => {
  if (newDialogs) {
    Object.keys(dialogsState).forEach(key => {
      if (newDialogs[key]) {
        Object.assign(dialogsState[key], newDialogs[key]);
      }
    });
  }
}, { immediate: true, deep: true });

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

function updateDialogVisibility(dialogKey, visible) {
  dialogsState[dialogKey].visible = visible;
  if (!visible) {
    emit('dialog-closed');
  }
}

function closeDialog(action) {
  dialogsState[action].visible = false;
  emit('dialog-closed');
}

function confirmAction(action) {
  const dialog = dialogsState[action];
  
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

