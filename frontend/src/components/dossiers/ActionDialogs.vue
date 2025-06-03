<template>
  <!-- Send to Commission Dialog -->
  <Dialog 
    v-model:visible="dialogs.sendToCommission.visible" 
    modal 
    header="Envoyer à la Commission AHA-AF"
    :style="{ width: '500px' }"
  >
    <div class="dialog-content">
      <div class="form-field">
        <label>Dossier</label>
        <div class="dossier-info">
          <strong>{{ dialogs.sendToCommission.dossier?.reference }}</strong>
          <small>{{ dialogs.sendToCommission.dossier?.agriculteurNom }}</small>
        </div>
      </div>

      <div class="form-field">
        <label for="priority">Priorité</label>
        <Dropdown 
          id="priority"
          v-model="dialogs.sendToCommission.priority"
          :options="priorityOptions"
          option-label="label"
          option-value="value"
          class="w-full"
        />
      </div>

      <div class="form-field">
        <label for="comment">Commentaire</label>
        <Textarea 
          id="comment"
          v-model="dialogs.sendToCommission.comment"
          rows="4"
          placeholder="Commentaires pour la commission..."
          class="w-full"
          autoResize
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
        label="Envoyer" 
        icon="pi pi-forward" 
        @click="confirmAction('sendToCommission')"
        :loading="dialogs.sendToCommission.loading"
        class="p-button-primary"
      />
    </template>
  </Dialog>

  <!-- Return to Antenne Dialog -->
  <Dialog 
    v-model:visible="dialogs.returnToAntenne.visible" 
    modal 
    header="Retourner à l'Antenne"
    :style="{ width: '500px' }"
  >
    <div class="dialog-content">
      <div class="form-field">
        <label>Dossier</label>
        <div class="dossier-info">
          <strong>{{ dialogs.returnToAntenne.dossier?.reference }}</strong>
          <small>{{ dialogs.returnToAntenne.dossier?.agriculteurNom }}</small>
        </div>
      </div>

      <div class="form-field">
        <label>Raisons du retour</label>
        <div class="reasons-list">
          <div class="reason-item" v-for="reason in returnReasons" :key="reason.value">
            <Checkbox 
              :inputId="reason.value"
              v-model="dialogs.returnToAntenne.reasons"
              :value="reason.value"
            />
            <label :for="reason.value" class="reason-label">{{ reason.label }}</label>
          </div>
        </div>
      </div>

      <div class="form-field">
        <label for="comment-return">Commentaire détaillé</label>
        <Textarea 
          id="comment-return"
          v-model="dialogs.returnToAntenne.comment"
          rows="4"
          placeholder="Précisez les éléments à corriger..."
          class="w-full"
          autoResize
        />
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
        label="Retourner" 
        icon="pi pi-undo" 
        @click="confirmAction('returnToAntenne')"
        :loading="dialogs.returnToAntenne.loading"
        class="p-button-warning"
      />
    </template>
  </Dialog>

  <!-- Reject Dialog -->
  <Dialog 
    v-model:visible="dialogs.reject.visible" 
    modal 
    header="Rejeter le Dossier"
    :style="{ width: '500px' }"
  >
    <div class="dialog-content">
      <div class="form-field">
        <label>Dossier</label>
        <div class="dossier-info">
          <strong>{{ dialogs.reject.dossier?.reference }}</strong>
          <small>{{ dialogs.reject.dossier?.agriculteurNom }}</small>
        </div>
      </div>

      <div class="form-field">
        <div class="rejection-warning">
          <i class="pi pi-exclamation-triangle"></i>
          <span>Cette action va rejeter définitivement le dossier.</span>
        </div>
      </div>

      <div class="form-field">
        <div class="checkbox-wrapper">
          <Checkbox 
            inputId="definitive"
            v-model="dialogs.reject.definitive"
            :binary="true"
          />
          <label for="definitive" class="confirm-label">
            Je confirme que ce rejet est définitif
          </label>
        </div>
      </div>

      <div class="form-field">
        <label for="comment-reject" class="required">Motif du rejet</label>
        <Textarea 
          id="comment-reject"
          v-model="dialogs.reject.comment"
          rows="4"
          placeholder="Expliquez les raisons du rejet..."
          class="w-full"
          :class="{ 'p-invalid': !dialogs.reject.comment && showRejectError }"
          autoResize
        />
        <small v-if="!dialogs.reject.comment && showRejectError" class="p-error">
          Le motif du rejet est obligatoire
        </small>
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
        label="Rejeter" 
        icon="pi pi-times-circle" 
        @click="confirmAction('reject')"
        :loading="dialogs.reject.loading"
        :disabled="!dialogs.reject.definitive || !dialogs.reject.comment"
        class="p-button-danger"
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
import Dropdown from 'primevue/dropdown';
import Checkbox from 'primevue/checkbox';

// Props & Emits
const props = defineProps({
  dialogs: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['action-confirmed', 'dialog-closed']);

// State
const showRejectError = ref(false);

// Options
const priorityOptions = [
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Faible', value: 'FAIBLE' }
];

const returnReasons = [
  { label: 'Documents manquants', value: 'DOCUMENTS_MANQUANTS' },
  { label: 'Documents non conformes', value: 'DOCUMENTS_NON_CONFORMES' },
  { label: 'Informations incomplètes', value: 'INFORMATIONS_INCOMPLETES' },
  { label: 'Formulaires mal remplis', value: 'FORMULAIRES_INCORRECTS' },
  { label: 'Justificatifs insuffisants', value: 'JUSTIFICATIFS_INSUFFISANTS' },
  { label: 'Autres', value: 'AUTRES' }
];

// Methods
function closeDialog(dialogType) {
  // Reset form data
  if (dialogType === 'sendToCommission') {
    props.dialogs.sendToCommission.comment = '';
    props.dialogs.sendToCommission.priority = 'NORMALE';
  } else if (dialogType === 'returnToAntenne') {
    props.dialogs.returnToAntenne.comment = '';
    props.dialogs.returnToAntenne.reasons = [];
  } else if (dialogType === 'reject') {
    props.dialogs.reject.comment = '';
    props.dialogs.reject.definitive = false;
    showRejectError.value = false;
  }
  
  emit('dialog-closed');
}

function confirmAction(actionType) {
  // Validation
  if (actionType === 'reject') {
    if (!props.dialogs.reject.comment.trim()) {
      showRejectError.value = true;
      return;
    }
    if (!props.dialogs.reject.definitive) {
      return;
    }
  }

  // Prepare action data
  let actionData = {
    action: actionType,
    dossier: props.dialogs[actionType].dossier
  };

  switch (actionType) {
    case 'sendToCommission':
      actionData.data = {
        comment: props.dialogs.sendToCommission.comment,
        priority: props.dialogs.sendToCommission.priority
      };
      break;
    case 'returnToAntenne':
      actionData.data = {
        comment: props.dialogs.returnToAntenne.comment,
        reasons: props.dialogs.returnToAntenne.reasons
      };
      break;
    case 'reject':
      actionData.data = {
        comment: props.dialogs.reject.comment,
        definitive: props.dialogs.reject.definitive
      };
      break;
  }

  emit('action-confirmed', actionData);
}
</script>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 0.5rem 0;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 600;
  color: var(--text-color);
}

.form-field label.required::after {
  content: ' *';
  color: var(--red-500);
}

.dossier-info {
  background: var(--surface-ground);
  padding: 1rem;
  border-radius: 6px;
  border: 1px solid var(--surface-border);
}

.dossier-info strong {
  display: block;
  color: var(--primary-color);
  font-size: 1.1rem;
  margin-bottom: 0.25rem;
}

.dossier-info small {
  color: var(--text-color-secondary);
}

.reasons-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: 6px;
  border: 1px solid var(--surface-border);
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.reason-label {
  font-weight: normal !important;
  color: var(--text-color) !important;
  cursor: pointer;
}

.rejection-warning {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(var(--red-500), 0.1);
  border: 1px solid rgba(var(--red-500), 0.2);
  border-radius: 6px;
  color: var(--red-500);
}

.rejection-warning i {
  font-size: 1.2rem;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--surface-ground);
  border-radius: 6px;
  border: 1px solid var(--surface-border);
}

.confirm-label {
  font-weight: 500 !important;
  color: var(--text-color) !important;
  cursor: pointer;
}

.w-full {
  width: 100%;
}

.p-error {
  color: var(--red-500);
  font-size: 0.8rem;
}

/* Dialog footer styling */
:deep(.p-dialog-footer) {
  padding: 1.5rem;
  border-top: 1px solid var(--surface-border);
  gap: 0.5rem;
  display: flex;
  justify-content: flex-end;
}

:deep(.p-dialog-footer .p-button) {
  margin-left: 0.5rem;
}

:deep(.p-dialog-footer .p-button:first-child) {
  margin-left: 0;
}

/* Responsive design */
@media (max-width: 768px) {
  :deep(.p-dialog) {
    width: 95vw !important;
    margin: 0 !important;
  }
  
  :deep(.p-dialog-footer) {
    flex-direction: column-reverse;
    gap: 0.5rem;
  }
  
  :deep(.p-dialog-footer .p-button) {
    margin-left: 0;
    width: 100%;
  }
}
</style>