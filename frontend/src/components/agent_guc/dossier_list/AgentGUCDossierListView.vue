<template>
  <div class="dossier-list-container p-4">
    <!-- Header -->
    <div class="flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold text-900">
          <i class="pi pi-folder-open mr-2"></i>
          Dossiers - Guichet Unique Central
        </h1>
        <p class="text-600" v-if="dossiers.length > 0">
          {{ dossiers.length }} dossier(s) trouvé(s) - Tous les dossiers soumis
        </p>
      </div>
      <div class="flex gap-2">
        <Button 
          label="Actualiser" 
          icon="pi pi-refresh" 
          @click="loadDossiers" 
          :loading="loading"
          class="p-button-outlined" 
        />
        <Button 
          label="Export Excel" 
          icon="pi pi-file-excel" 
          @click="exportToExcel"
          class="p-button-outlined" 
        />
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid mb-4" v-if="statistics">
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-primary">{{ statistics.totalDossiers || 0 }}</div>
              <div class="text-color-secondary">Total dossiers</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-orange-500">{{ statistics.dossiersAttenteTraitement || 0 }}</div>
              <div class="text-color-secondary">En attente traitement</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-500">{{ statistics.dossiersApprouves || 0 }}</div>
              <div class="text-color-secondary">Approuvés</div>
            </div>
          </template>
        </Card>
      </div>
      <div class="col-12 md:col-3" v-if="statistics.dossiersEnRetard > 0">
        <Card>
          <template #content>
            <div class="text-center">
              <div class="text-3xl font-bold text-red-500">{{ statistics.dossiersEnRetard }}</div>
              <div class="text-color-secondary">En retard</div>
            </div>
          </template>
        </Card>
      </div>
    </div>

    <!-- Filters -->
    <Card class="mb-4">
      <template #content>
        <div class="grid">
          <div class="col-12 md:col-3">
            <label class="block text-900 font-medium mb-2">Recherche</label>
            <InputText 
              v-model="searchTerm" 
              placeholder="Rechercher par SABA, CIN, nom..." 
              class="w-full"
              @input="handleSearch"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Statut</label>
            <Dropdown 
              v-model="filters.statut" 
              :options="statusOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Tous les statuts" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Type de Projet</label>
            <Dropdown 
              v-model="filters.sousRubriqueId" 
              :options="sousRubriqueOptions" 
              optionLabel="designation" 
              optionValue="id"
              placeholder="Tous les types" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Antenne</label>
            <Dropdown 
              v-model="filters.antenneId" 
              :options="antenneOptions" 
              optionLabel="designation" 
              optionValue="id"
              placeholder="Toutes les antennes" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-2">
            <label class="block text-900 font-medium mb-2">Priorité</label>
            <Dropdown 
              v-model="filters.priorite" 
              :options="prioriteOptions" 
              optionLabel="label" 
              optionValue="value"
              placeholder="Toutes priorités" 
              class="w-full"
              @change="applyFilters"
              :clearable="true"
            />
          </div>
          <div class="col-12 md:col-1 flex align-items-end">
            <Button 
              label="Effacer" 
              icon="pi pi-times" 
              @click="clearFilters" 
              class="p-button-text w-full" 
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Loading State -->
    <div v-if="loading" class="text-center p-6">
      <ProgressSpinner size="50px" />
      <div class="mt-3">Chargement des dossiers...</div>
    </div>

    <!-- Error State -->
    <Card v-else-if="error" class="text-center">
      <template #content>
        <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-3"></i>
        <h3>Erreur de chargement</h3>
        <p class="text-600">{{ error }}</p>
        <Button 
          label="Réessayer" 
          icon="pi pi-refresh" 
          @click="loadDossiers" 
          class="p-button-outlined mt-3" 
        />
      </template>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="dossiers.length === 0" class="text-center">
      <template #content>
        <i class="pi pi-folder-open text-4xl text-600 mb-3"></i>
        <h3>Aucun dossier trouvé</h3>
        <p class="text-600" v-if="hasActiveFilters">
          Aucun dossier ne correspond à vos critères de recherche.
        </p>
        <p class="text-600" v-else>
          Aucun dossier n'a encore été soumis au GUC.
        </p>
        <div class="mt-3">
          <Button 
            v-if="hasActiveFilters" 
            label="Effacer les filtres" 
            icon="pi pi-filter-slash" 
            @click="clearFilters"
            class="p-button-outlined" 
          />
        </div>
      </template>
    </Card>

    <!-- Dossiers Table -->
    <Card v-else>
      <template #content>
        <DataTable 
          :value="dossiers" 
          :paginator="true" 
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
          currentPageReportTemplate="Affichage de {first} à {last} sur {totalRecords} dossiers"
          responsiveLayout="scroll"
        >
          <Column field="reference" header="Référence" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.reference }}</div>
                <div class="text-sm text-600">SABA: {{ slotProps.data.saba }}</div>
              </div>
            </template>
          </Column>

          <Column field="agriculteurNom" header="Agriculteur" :sortable="true">
            <template #body="slotProps">
              <div>
                <div class="font-semibold">{{ slotProps.data.agriculteurNom }}</div>
                <div class="text-sm text-600">
                  <i class="pi pi-id-card mr-1"></i>
                  {{ slotProps.data.agriculteurCin }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="sousRubriqueDesignation" header="Type de Projet">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">{{ slotProps.data.sousRubriqueDesignation }}</div>
                <div class="text-sm text-600" v-if="slotProps.data.montantSubvention">
                  <i class="pi pi-money-bill mr-1"></i>
                  {{ formatCurrency(slotProps.data.montantSubvention) }}
                </div>
              </div>
            </template>
          </Column>

          <Column header="Antenne">
            <template #body="slotProps">
              <div>
                <div class="font-semibold text-sm">{{ slotProps.data.antenneDesignation }}</div>
                <div class="text-sm text-600">{{ slotProps.data.cdaNom }}</div>
              </div>
            </template>
          </Column>

          <Column header="Phase">
            <template #body="slotProps">
              <div class="flex align-items-center gap-2">
                <Tag :value="getPhaseLabel(slotProps.data)" :severity="getPhaseSeverity(slotProps.data)" />
                <div v-if="slotProps.data.notesCount > 0" class="flex align-items-center gap-1 text-600">
                  <i class="pi pi-comment text-xs"></i>
                  <span class="text-xs">{{ slotProps.data.notesCount }}</span>
                </div>
              </div>
            </template>
          </Column>

          <Column header="Commission">
            <template #body="slotProps">
              <div class="text-sm">
                <div v-if="slotProps.data.commissionDecisionMade" class="flex flex-column gap-1">
                  <Tag 
                    :value="slotProps.data.commissionApproved ? 'TERRAIN APPROUVÉ' : 'TERRAIN REJETÉ'" 
                    :severity="slotProps.data.commissionApproved ? 'success' : 'danger'"
                    class="text-xs"
                  />
                  <div class="text-xs text-600" v-if="slotProps.data.commissionComments">
                    {{ truncateText(slotProps.data.commissionComments, 50) }}
                  </div>
                  <div class="text-xs text-500" v-if="slotProps.data.commissionDecisionDate">
                    {{ formatDate(slotProps.data.commissionDecisionDate) }}
                  </div>
                </div>
                <div v-else class="flex align-items-center gap-1">
                  <Tag value="EN ATTENTE" severity="warning" class="text-xs" />
                  <i class="pi pi-clock text-xs text-600"></i>
                </div>
              </div>
            </template>
          </Column>

          <Column field="statut" header="Statut" :sortable="true">
            <template #body="slotProps">
              <Tag 
                :value="getStatusLabel(slotProps.data.statut)" 
                :severity="getStatusSeverity(slotProps.data.statut)" 
              />
            </template>
          </Column>

          <Column field="timing" header="Délais">
            <template #body="slotProps">
              <div class="text-sm">
                <div class="flex align-items-center mb-1">
                  <i class="pi pi-clock mr-1"></i>
                  <span 
                    v-if="slotProps.data.joursRestants > 0" 
                    class="text-600"
                  >
                    {{ slotProps.data.joursRestants }} jour(s) restant(s)
                  </span>
                  <span 
                    v-else-if="slotProps.data.enRetard" 
                    class="text-red-500 font-semibold"
                  >
                    En retard ({{ slotProps.data.joursRetard }} jour(s))
                  </span>
                  <span v-else class="text-600">Délai respecté</span>
                </div>
                <div class="text-xs text-500">
                  Créé le {{ formatDate(slotProps.data.dateCreation) }}
                </div>
              </div>
            </template>
          </Column>

          <Column field="actions" header="Actions">
            <template #body="slotProps">
              <div class="flex gap-1">
                <!-- View Details -->
                <Button 
                  icon="pi pi-eye" 
                  @click="viewDossierDetail(slotProps.data.id)"
                  class="p-button-outlined p-button-sm" 
                  v-tooltip.top="'Voir détails'" 
                />
                
                <!-- Add Note -->
                <Button 
                  icon="pi pi-comment" 
                  @click="showAddNoteDialog(slotProps.data)"
                  class="p-button-info p-button-outlined p-button-sm" 
                  v-tooltip.top="'Ajouter note'" 
                />

                <!-- Available Actions from Backend -->
                <template v-for="action in slotProps.data.availableActions" :key="action.action">
                  <!-- Assign to Commission -->
                  <Button 
                    v-if="action.action === 'assign-commission'"
                    icon="pi pi-forward" 
                    @click="confirmAssignCommission(slotProps.data)"
                    class="p-button-info p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  
                  <!-- Send to Service Technique -->
                  <Button 
                    v-if="action.action === 'send-to-service-technique'"
                    icon="pi pi-cog" 
                    @click="confirmSendToServiceTechnique(slotProps.data)"
                    class="p-button-success p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  
                  <!-- Final Approval Button for Phase 4 -->
                  <Button 
                    v-if="action.action === 'final-approval'"
                    icon="pi pi-gavel" 
                    @click="goToFinalApproval(slotProps.data)"
                    class="p-button-success p-button-sm" 
                    v-tooltip.top="'Finaliser l\'Approbation'" 
                  />
                  
                  <!-- Validate Realization -->
                  <Button 
                    v-if="action.action === 'validate-realization'"
                    icon="pi pi-check-circle" 
                    @click="confirmValidateRealization(slotProps.data)"
                    class="p-button-success p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  
                  <!-- Return to Antenne -->
                  <Button 
                    v-if="action.action === 'return'"
                    icon="pi pi-undo" 
                    @click="confirmReturnToAntenne(slotProps.data)"
                    class="p-button-warning p-button-outlined p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                  
                  <!-- Reject (only for phase 2) -->
                  <Button 
                    v-if="action.action === 'reject'"
                    icon="pi pi-times" 
                    @click="confirmRejectDossier(slotProps.data)"
                    class="p-button-danger p-button-outlined p-button-sm" 
                    v-tooltip.top="action.label" 
                  />
                </template>

                <!-- View Fiche Button (approved dossiers) -->
                <Button 
                  v-if="hasApprovedFiche(slotProps.data)"
                  icon="pi pi-file-text" 
                  @click="goToFiche(slotProps.data)"
                  class="p-button-success p-button-sm" 
                  v-tooltip.top="'Voir Fiche d\'Approbation'" 
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Assign to Commission Dialog -->
    <Dialog 
      v-model:visible="assignCommissionDialog.visible" 
      modal 
      header="Envoyer à la Commission AHA-AF"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-forward text-2xl mr-3"></i>
        <div>
          <p>Envoyer ce dossier à la Commission visite terrain ?</p>
          <div class="mt-2 p-2 bg-blue-50 border-round">
            <strong>{{ assignCommissionDialog.dossier?.reference }}</strong><br>
            {{ assignCommissionDialog.dossier?.agriculteurNom }}<br>
            <small>Type: {{ assignCommissionDialog.dossier?.sousRubriqueDesignation }}</small>
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="commissionComment">Commentaire pour la Commission *</label>
        <Textarea 
          id="commissionComment"
          v-model="assignCommissionDialog.comment" 
          rows="3" 
          placeholder="Instructions spécifiques pour la commission..."
          class="w-full"
          :class="{ 'p-invalid': !assignCommissionDialog.comment?.trim() }"
        />
      </div>
      
      <div class="field">
        <label for="commissionPriority">Priorité du dossier</label>
        <Dropdown 
          id="commissionPriority"
          v-model="assignCommissionDialog.priority" 
          :options="prioriteOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Sélectionner la priorité"
          class="w-full"
        />
      </div>

      <p class="text-600 text-sm">
        La commission effectuera une visite terrain pour évaluer la conformité du projet.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="assignCommissionDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Envoyer à la Commission" 
          icon="pi pi-forward" 
          @click="assignToCommission"
          class="p-button-info"
          :loading="assignCommissionDialog.loading"
          :disabled="!assignCommissionDialog.comment?.trim()"
        />
      </template>
    </Dialog>

    <!-- Send to Service Technique Dialog -->
    <Dialog 
      v-model:visible="sendToServiceTechniqueDialog.visible" 
      modal 
      header="Envoyer au Service Technique"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-cog text-2xl mr-3"></i>
        <div>
          <p>Envoyer ce dossier au Service Technique pour supervision de la réalisation ?</p>
          <div class="mt-2 p-2 bg-green-50 border-round">
            <strong>{{ sendToServiceTechniqueDialog.dossier?.reference }}</strong><br>
            {{ sendToServiceTechniqueDialog.dossier?.agriculteurNom }}<br>
            <small>Type: {{ sendToServiceTechniqueDialog.dossier?.sousRubriqueDesignation }}</small>
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="serviceTechniqueComment">Commentaire pour le Service Technique *</label>
        <Textarea 
          id="serviceTechniqueComment"
          v-model="sendToServiceTechniqueDialog.comment" 
          rows="3" 
          placeholder="Instructions spécifiques pour le service technique..."
          class="w-full"
          :class="{ 'p-invalid': !sendToServiceTechniqueDialog.comment?.trim() }"
        />
      </div>
      
      <div class="field">
        <label for="serviceTechniquePriority">Priorité du dossier</label>
        <Dropdown 
          id="serviceTechniquePriority"
          v-model="sendToServiceTechniqueDialog.priority" 
          :options="prioriteOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Sélectionner la priorité"
          class="w-full"
        />
      </div>

      <p class="text-600 text-sm">
        Le service technique supervisera la réalisation du projet et effectuera le contrôle de conformité.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="sendToServiceTechniqueDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Envoyer au Service Technique" 
          icon="pi pi-cog" 
          @click="sendToServiceTechnique"
          class="p-button-success"
          :loading="sendToServiceTechniqueDialog.loading"
          :disabled="!sendToServiceTechniqueDialog.comment?.trim()"
        />
      </template>
    </Dialog>

    <!-- Validate Realization Dialog -->
    <Dialog 
      v-model:visible="validateRealizationDialog.visible" 
      modal 
      header="Valider la Réalisation"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-check-circle text-2xl text-green-500 mr-3"></i>
        <div>
          <p>Valider la réalisation de ce projet ?</p>
          <div class="mt-2 p-2 bg-green-50 border-round">
            <strong>{{ validateRealizationDialog.dossier?.reference }}</strong><br>
            {{ validateRealizationDialog.dossier?.agriculteurNom }}
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="validateComment">Commentaire de validation *</label>
        <Textarea 
          id="validateComment"
          v-model="validateRealizationDialog.comment" 
          rows="4" 
          placeholder="Commentaires sur la validation de la réalisation..."
          class="w-full"
          :class="{ 'p-invalid': !validateRealizationDialog.comment?.trim() }"
        />
      </div>

      <p class="text-green-600 text-sm font-semibold">
        Cette action marquera le dossier comme <strong>terminé</strong>.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="validateRealizationDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Valider la Réalisation" 
          icon="pi pi-check-circle" 
          @click="validateRealization"
          class="p-button-success"
          :loading="validateRealizationDialog.loading"
          :disabled="!validateRealizationDialog.comment?.trim()"
        />
      </template>
    </Dialog>

    <!-- Reject Dialog -->
    <Dialog 
      v-model:visible="rejectDialog.visible" 
      modal 
      header="Rejeter le Dossier"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-times-circle text-2xl text-red-500 mr-3"></i>
        <div>
          <p>Rejeter définitivement ce dossier ?</p>
          <div class="mt-2 p-2 bg-red-50 border-round">
            <strong>{{ rejectDialog.dossier?.reference }}</strong><br>
            {{ rejectDialog.dossier?.agriculteurNom }}
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="rejectComment">Motif du rejet *</label>
        <Textarea 
          id="rejectComment"
          v-model="rejectDialog.comment" 
          rows="4" 
          placeholder="Expliquez clairement les raisons du rejet du dossier..."
          class="w-full"
          :class="{ 'p-invalid': !rejectDialog.comment?.trim() }"
        />
      </div>

      <p class="text-red-600 text-sm font-semibold">
        Cette action est <strong>définitive</strong> et ne peut pas être annulée.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="rejectDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Rejeter le Dossier" 
          icon="pi pi-times-circle" 
          @click="rejectDossier"
          class="p-button-danger"
          :loading="rejectDialog.loading"
          :disabled="!rejectDialog.comment?.trim()"
        />
      </template>
    </Dialog>

    <!-- Return to Antenne Dialog -->
    <Dialog 
      v-model:visible="returnDialog.visible" 
      modal 
      header="Retourner à l'Antenne"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-undo text-2xl text-orange-500 mr-3"></i>
        <div>
          <p>Retourner ce dossier à l'antenne pour complétion ?</p>
          <div class="mt-2 p-2 bg-orange-50 border-round">
            <strong>{{ returnDialog.dossier?.reference }}</strong><br>
            {{ returnDialog.dossier?.agriculteurNom }}
          </div>
        </div>
      </div>
      
      <div class="field">
        <label for="returnComment">Motif du retour *</label>
        <Textarea 
          id="returnComment"
          v-model="returnDialog.comment" 
          rows="4" 
          placeholder="Expliquez pourquoi le dossier est retourné et ce qui doit être corrigé..."
          class="w-full"
          :class="{ 'p-invalid': !returnDialog.comment?.trim() }"
        />
      </div>

      <p class="text-orange-600 text-sm">
        Le dossier repassera en mode modification à l'antenne.
      </p>
      
      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="returnDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Retourner à l'Antenne" 
          icon="pi pi-undo" 
          @click="returnToAntenne"
          class="p-button-warning"
          :loading="returnDialog.loading"
          :disabled="!returnDialog.comment?.trim()"
        />
      </template>
    </Dialog>

    <!-- Add Note Dialog -->
    <Dialog 
      v-model:visible="addNoteDialog.visible" 
      modal 
      header="Ajouter une Note"
      :style="{ width: '500px' }"
    >
      <div class="field">
        <label for="noteContent">Note *</label>
        <Textarea 
          id="noteContent"
          v-model="addNoteDialog.content" 
          rows="4" 
          placeholder="Entrez votre note..."
          class="w-full"
          :class="{ 'p-invalid': !addNoteDialog.content?.trim() }"
        />
      </div>

      <template #footer>
        <Button 
          label="Annuler" 
          icon="pi pi-times" 
          @click="addNoteDialog.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Ajouter" 
          icon="pi pi-plus" 
          @click="addNote"
          class="p-button-primary"
          :loading="addNoteDialog.loading"
          :disabled="!addNoteDialog.content?.trim()"
        />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// PrimeVue components
import Button from 'primevue/button';
import Card from 'primevue/card';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import Textarea from 'primevue/textarea';
import ProgressSpinner from 'primevue/progressspinner';
import Tag from 'primevue/tag';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';

const router = useRouter();
const toast = useToast();

// State
const loading = ref(false);
const error = ref(null);
const dossiers = ref([]);
const statistics = ref(null);
const searchTerm = ref('');

// Filters
const filters = reactive({
  statut: null,
  sousRubriqueId: null,
  antenneId: null,
  priorite: null
});

// Filter options
const statusOptions = ref([
  { label: 'Soumis', value: 'SUBMITTED' },
  { label: 'En révision', value: 'IN_REVIEW' },
  { label: 'Approuvé', value: 'APPROVED' },
  { label: 'En attente fermier', value: 'AWAITING_FARMER' },
  { label: 'Rejeté', value: 'REJECTED' },
  { label: 'Terminé', value: 'COMPLETED' }
]);

const sousRubriqueOptions = ref([
  { id: 1, designation: 'FILIERES VEGETALES - Équipements' },
  { id: 2, designation: 'FILIERES VEGETALES - Matériel' },
  { id: 3, designation: 'FILIERES ANIMALES - Équipements' },
  { id: 4, designation: 'FILIERES ANIMALES - Installations' },
  { id: 5, designation: 'AMENAGEMENT HYDRO-AGRICOLE' },
  { id: 6, designation: 'AMELIORATION FONCIERE' }
]);

const antenneOptions = ref([
  { id: 1, designation: 'Antenne Bni Amir' },
  { id: 2, designation: 'Antenne Souk Sebt' },
  { id: 3, designation: 'Antenne Ouldad M\'Bark' },
  { id: 4, designation: 'Antenne Dar Ouled Zidouh' },
  { id: 5, designation: 'Antenne Afourer' }
]);

const prioriteOptions = ref([
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

// Dialogs
const assignCommissionDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  comment: '',
  priority: 'NORMALE'
});

const sendToServiceTechniqueDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  comment: '',
  priority: 'NORMALE'
});

const validateRealizationDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  comment: ''
});

const rejectDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  comment: ''
});

const returnDialog = ref({
  visible: false,
  dossier: null,
  loading: false,
  comment: ''
});

const addNoteDialog = ref({
  visible: false,
  dossier: null,
  content: '',
  loading: false
});

// Computed
const hasActiveFilters = computed(() => {
  return searchTerm.value?.trim() || filters.statut || filters.sousRubriqueId || filters.antenneId || filters.priorite;
});

// Methods
onMounted(async () => {
  await loadDossiers();
  startAutoRefresh();
});

onUnmounted(() => {
  stopAutoRefresh();
});

let refreshInterval = null;

function startAutoRefresh() {
  refreshInterval = setInterval(() => {
    loadDossiers();
  }, 30000); // Refresh every 30 seconds
}

function stopAutoRefresh() {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
}

async function loadDossiers() {
  try {
    loading.value = true;
    error.value = null;

    const params = {};
    
    // Add search and filters
    if (searchTerm.value?.trim()) params.searchTerm = searchTerm.value.trim();
    if (filters.statut) params.statut = filters.statut;
    if (filters.sousRubriqueId) params.sousRubriqueId = filters.sousRubriqueId;
    if (filters.antenneId) params.antenneId = filters.antenneId;
    if (filters.priorite) params.priorite = filters.priorite;

    const response = await ApiService.get('/agent-guc/dossiers', params);
    dossiers.value = response.dossiers || [];
    
    // Calculate statistics from loaded data
    calculateStatistics();
    
  } catch (err) {
    console.error('Erreur lors du chargement des dossiers:', err);
    error.value = err.message || 'Impossible de charger les dossiers';
    dossiers.value = [];
    statistics.value = null;
  } finally {
    loading.value = false;
  }
}

function calculateStatistics() {
  const total = dossiers.value.length;
  const enAttente = dossiers.value.filter(d => d.statut === 'SUBMITTED' || d.statut === 'IN_REVIEW').length;
  const approuves = dossiers.value.filter(d => d.statut === 'APPROVED' || d.statut === 'AWAITING_FARMER' || d.statut === 'COMPLETED').length;
  const enRetard = dossiers.value.filter(d => d.enRetard).length;

  statistics.value = {
    totalDossiers: total,
    dossiersAttenteTraitement: enAttente,
    dossiersApprouves: approuves,
    dossiersEnRetard: enRetard
  };
}

function handleSearch() {
  // Debounce search
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    applyFilters();
  }, 500);
}

let searchTimeout = null;

function applyFilters() {
  loadDossiers();
}

function clearFilters() {
  Object.assign(filters, {
    statut: null,
    sousRubriqueId: null,
    antenneId: null,
    priorite: null
  });
  searchTerm.value = '';
  loadDossiers();
}

function viewDossierDetail(dossierId) {
  router.push(`/agent_guc/dossiers/${dossierId}`);
}

function goToFiche(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/fiche`);
}

function goToFinalApproval(dossier) {
  router.push(`/agent_guc/dossiers/${dossier.id}/final-approval`);
}

// Dialog functions
function confirmAssignCommission(dossier) {
  assignCommissionDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: '',
    priority: 'NORMALE'
  };
}

function confirmSendToServiceTechnique(dossier) {
  sendToServiceTechniqueDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: '',
    priority: 'NORMALE'
  };
}

function confirmValidateRealization(dossier) {
  validateRealizationDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmRejectDossier(dossier) {
  rejectDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function confirmReturnToAntenne(dossier) {
  returnDialog.value = {
    visible: true,
    dossier: dossier,
    loading: false,
    comment: ''
  };
}

function showAddNoteDialog(dossier) {
  addNoteDialog.value = {
    visible: true,
    dossier: dossier,
    content: '',
    loading: false
  };
}

// Action methods
async function assignToCommission() {
  try {
    assignCommissionDialog.value.loading = true;
    
    const dossier = assignCommissionDialog.value.dossier;
    const endpoint = `/agent-guc/dossiers/assign-commission/${dossier.id}`;
    const payload = { 
      commentaire: assignCommissionDialog.value.comment, 
      priorite: assignCommissionDialog.value.priority 
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Dossier assigné à la Commission',
        life: 3000
      });
      
      assignCommissionDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    assignCommissionDialog.value.loading = false;
  }
}

async function sendToServiceTechnique() {
  try {
    sendToServiceTechniqueDialog.value.loading = true;
    
    const dossier = sendToServiceTechniqueDialog.value.dossier;
    const endpoint = `/agent-guc/dossiers/send-to-service-technique/${dossier.id}`;
    const payload = { 
      commentaire: sendToServiceTechniqueDialog.value.comment, 
      priorite: sendToServiceTechniqueDialog.value.priority 
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Dossier envoyé au Service Technique',
        life: 3000
      });
      
      sendToServiceTechniqueDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    sendToServiceTechniqueDialog.value.loading = false;
  }
}

async function validateRealization() {
  try {
    validateRealizationDialog.value.loading = true;
    
    const dossier = validateRealizationDialog.value.dossier;
    const endpoint = `/agent-guc/dossiers/validate-realization/${dossier.id}`;
    const payload = { 
      commentaire: validateRealizationDialog.value.comment
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Réalisation validée avec succès',
        life: 3000
      });
      
      validateRealizationDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    validateRealizationDialog.value.loading = false;
  }
}

async function rejectDossier() {
  try {
    rejectDialog.value.loading = true;
    
    const dossier = rejectDialog.value.dossier;
    const endpoint = `/agent-guc/dossiers/reject/${dossier.id}`;
    const payload = { 
      motif: 'Rejet du dossier', 
      commentaire: rejectDialog.value.comment 
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Dossier rejeté',
        life: 3000
      });
      
      rejectDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    rejectDialog.value.loading = false;
  }
}

async function returnToAntenne() {
  try {
    returnDialog.value.loading = true;
    
    const dossier = returnDialog.value.dossier;
    const endpoint = `/agent-guc/dossiers/return/${dossier.id}`;
    const payload = { 
      motif: 'Complétion requise', 
      commentaire: returnDialog.value.comment 
    };

    const response = await ApiService.post(endpoint, payload);

    if (response.success) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Dossier retourné à l\'antenne',
        life: 3000
      });
      
      returnDialog.value.visible = false;
      setTimeout(() => loadDossiers(), 500);
    }

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue',
      life: 3000
    });
  } finally {
    returnDialog.value.loading = false;
  }
}

async function addNote() {
  try {
    addNoteDialog.value.loading = true;

    // Simulate note API call - replace with actual endpoint when available
    await new Promise(resolve => setTimeout(resolve, 1000));

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Note ajoutée avec succès',
      life: 3000
    });

    addNoteDialog.value.visible = false;
    setTimeout(() => loadDossiers(), 500);

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'ajouter la note',
      life: 3000
    });
  } finally {
    addNoteDialog.value.loading = false;
  }
}

// Utility functions
function getPhaseLabel(dossier) {
  return dossier.currentStep || 'Phase inconnue';
}

function getPhaseSeverity(dossier) {
  const phase = dossier.currentStep;
  if (phase?.includes('GUC')) return 'warning';
  if (phase?.includes('Commission')) return 'info';
  return 'secondary';
}

function getStatusLabel(status) {
  const statusMap = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En révision',
    'APPROVED': 'Approuvé',
    'AWAITING_FARMER': 'En attente fermier',
    'REJECTED': 'Rejeté',
    'COMPLETED': 'Terminé',
    'RETURNED_FOR_COMPLETION': 'Retourné'
  };
  return statusMap[status] || status;
}

function getStatusSeverity(status) {
  const severityMap = {
    'DRAFT': 'secondary',
    'SUBMITTED': 'info',
    'IN_REVIEW': 'warning',
    'APPROVED': 'success',
    'AWAITING_FARMER': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'RETURNED_FOR_COMPLETION': 'warning'
  };
  return severityMap[status] || 'info';
}

function hasApprovedFiche(dossier) {
  return dossier.statut === 'APPROVED' || 
         dossier.statut === 'AWAITING_FARMER' ||
         dossier.statut === 'COMPLETED' ||
         !!dossier.dateApprobation;
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
  return new Intl.DateTimeFormat('fr-FR').format(new Date(date));
}

function truncateText(text, maxLength) {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

async function exportToExcel() {
  try {
    const response = await fetch('/api/agent-guc/dossiers/export', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${AuthService.getToken()}`
      },
      body: JSON.stringify(filters)
    });

    if (!response.ok) throw new Error('Erreur d\'export');

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `dossiers_guc_${new Date().toISOString().split('T')[0]}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Export Excel généré avec succès',
      life: 3000
    });

  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible d\'exporter les dossiers',
      life: 3000
    });
  }
}
</script>

<style scoped>
.dossier-list-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
</style>