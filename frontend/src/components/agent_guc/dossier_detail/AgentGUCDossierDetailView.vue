<template>
  <div class="agent-guc-dossier-detail">
    <!-- Navigation -->
    <div class="mb-4">
      <Button 
        label="Retour aux dossiers GUC" 
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
                <Tag 
                  v-if="currentPhase"
                  :value="getPhaseLabel()" 
                  :severity="getPhaseSeverity()"
                />
              </div>
            </div>
            <div class="flex gap-2">
              <!-- Final Approval Button (Phase 4) -->
              <Button 
                v-if="needsFinalApproval()"
                label="Décision Finale" 
                icon="pi pi-gavel" 
                @click="goToFinalApproval"
                severity="success"
              />
              
              <!-- Final Realization Approval Button (Phase 8) -->
              <Button 
                v-if="needsFinalRealizationApproval()"
                label="Approbation Finale Réalisation" 
                icon="pi pi-check-square" 
                @click="goToFinalRealizationApproval"
                severity="success"
              />
              
              <!-- View Fiche Button -->
              <Button 
                v-if="hasApprovedFiche()"
                label="Voir Fiche" 
                icon="pi pi-file-text" 
                @click="goToFiche"
                severity="success"
              />
              
              <!-- Phase-based Actions -->
              <SplitButton 
                v-if="hasPhaseActions()"
                :model="getPhaseActionMenuItems()" 
                @click="handlePrimaryPhaseAction()"
                :label="getPrimaryPhaseActionLabel()"
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
              <div class="space-y-2">
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
            </div>

            <!-- Projet Info -->
            <div class="col-12 md:col-6">
              <h4 class="mt-0 mb-3">Projet</h4>
              <div class="space-y-2">
                <div><strong>Type:</strong> {{ dossierDetail.projet?.sousRubrique }}</div>
                <div><strong>Rubrique:</strong> {{ dossierDetail.projet?.rubrique }}</div>
                <div><strong>Référence:</strong> {{ dossierDetail.reference }}</div>
                <div v-if="dossierDetail.montantSubvention">
                  <strong>Montant:</strong> {{ formatCurrency(dossierDetail.montantSubvention) }}
                </div>
                <div><strong>Antenne:</strong> {{ dossierDetail.antenne?.designation }}</div>
                <div><strong>Créé par:</strong> {{ dossierDetail.utilisateurCreateur?.nom }}</div>
              </div>
            </div>
          </div>

          <!-- Workflow Location & Timing -->
          <div class="mt-4 p-3 surface-50 border-round">
            <div class="flex justify-content-between align-items-center">
              <div>
                <h5 class="m-0">Étape Actuelle: {{ dossierDetail.timing?.currentStep }}</h5>
                <div class="flex align-items-center gap-2 mt-1">
                  <i class="pi pi-map-marker"></i>
                  <span class="text-color-secondary">{{ getWorkflowLocationText() }}</span>
                </div>
                <p class="m-0 text-sm text-color-secondary mt-1">
                  Assigné à: {{ dossierDetail.timing?.assignedTo }}
                </p>
              </div>
              <div class="text-right">
                <div class="text-2xl font-bold" :class="{
                  'text-red-500': dossierDetail.timing?.enRetard,
                  'text-orange-500': dossierDetail.timing?.joursRestants <= 2,
                  'text-green-500': dossierDetail.timing?.joursRestants > 2
                }">
                  {{ dossierDetail.timing?.enRetard ? 
                    `${dossierDetail.timing.joursRetard} jour(s) de retard` : 
                    `${dossierDetail.timing.joursRestants} jour(s) restants` 
                  }}
                </div>
                <p class="text-sm text-color-secondary">
                  Délai maximum: {{ dossierDetail.timing?.delaiMaxJours }} jours
                </p>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Commission Feedback Section -->
      <Card v-if="dossierDetail.commissionFeedback">
        <template #title>
          <i class="pi pi-map-marker mr-2"></i>Retour de la Commission Visite Terrain
        </template>
        <template #content>
          <div v-if="dossierDetail.commissionFeedback.decisionMade">
            <!-- Commission Decision -->
            <div class="flex justify-content-between align-items-center mb-4">
              <div class="flex align-items-center gap-3">
                <Tag 
                  :value="dossierDetail.commissionFeedback.approved ? 'TERRAIN APPROUVÉ' : 'TERRAIN REJETÉ'" 
                  :severity="dossierDetail.commissionFeedback.approved ? 'success' : 'danger'"
                  class="text-lg font-bold"
                />
                <div class="text-sm text-600">
                  <i class="pi pi-calendar mr-1"></i>
                  Visite effectuée le {{ formatDate(dossierDetail.commissionFeedback.dateVisite) }}
                </div>
              </div>
              <div class="text-sm text-600" v-if="dossierDetail.commissionFeedback.agentCommissionName">
                <i class="pi pi-user mr-1"></i>
                Par: {{ dossierDetail.commissionFeedback.agentCommissionName }}
              </div>
            </div>

            <!-- Commission Details -->
            <div class="grid">
              <div class="col-12 md:col-6" v-if="dossierDetail.commissionFeedback.observations">
                <h5>Observations Terrain</h5>
                <div class="p-3 bg-gray-50 border-round">
                  {{ dossierDetail.commissionFeedback.observations }}
                </div>
              </div>
              
              <div class="col-12 md:col-6" v-if="dossierDetail.commissionFeedback.recommandations">
                <h5>Recommandations</h5>
                <div class="p-3 bg-blue-50 border-round">
                  {{ dossierDetail.commissionFeedback.recommandations }}
                </div>
              </div>

              <div class="col-12" v-if="dossierDetail.commissionFeedback.conclusion">
                <h5>Conclusion de la Commission</h5>
                <div class="p-3 border-round" :class="{
                  'bg-green-50': dossierDetail.commissionFeedback.approved,
                  'bg-red-50': !dossierDetail.commissionFeedback.approved
                }">
                  {{ dossierDetail.commissionFeedback.conclusion }}
                </div>
              </div>

              <!-- Technical Details -->
              <div class="col-12" v-if="hasCommissionVisitDetails()">
                <h5>Détails de la Visite</h5>
                <div class="grid">
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.coordonneesGPS">
                    <strong>Coordonnées GPS:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.coordonneesGPS }}</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.dureeVisite">
                    <strong>Durée:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.dureeVisite }} minutes</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.commissionFeedback.conditionsMeteo">
                    <strong>Conditions météo:</strong><br>
                    <small>{{ dossierDetail.commissionFeedback.conditionsMeteo }}</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Photos Section -->
            <div v-if="dossierDetail.commissionFeedback.photosUrls && dossierDetail.commissionFeedback.photosUrls.length > 0" class="mt-4">
              <h5>Photos du Terrain</h5>
              <div class="flex gap-2 flex-wrap">
                <div v-for="(photo, index) in dossierDetail.commissionFeedback.photosUrls" :key="index" class="border-round overflow-hidden">
                  <img :src="photo" :alt="`Photo terrain ${index + 1}`" class="w-8rem h-6rem object-cover cursor-pointer" 
                       @click="openPhotoDialog(photo)" />
                </div>
              </div>
            </div>
          </div>
          
          <!-- No Commission Decision Yet -->
          <div v-else class="text-center py-4">
            <i class="pi pi-clock text-4xl text-orange-500 mb-3"></i>
            <h4>En attente de la décision de la Commission</h4>
            <p class="text-600">La commission n'a pas encore rendu sa décision concernant la visite terrain.</p>
            <Tag value="VISITE EN COURS" severity="warning" />
          </div>
        </template>
      </Card>

      <!-- Service Technique Feedback Section -->
      <Card v-if="dossierDetail.serviceTechniqueFeedback">
        <template #title>
          <i class="pi pi-cog mr-2"></i>Retour du Service Technique
        </template>
        <template #content>
          <div v-if="dossierDetail.serviceTechniqueFeedback.decisionMade">
            <!-- Service Technique Decision -->
            <div class="flex justify-content-between align-items-center mb-4">
              <div class="flex align-items-center gap-3">
                <Tag 
                  :value="dossierDetail.serviceTechniqueFeedback.approved ? 'RÉALISATION APPROUVÉE' : 'RÉALISATION REJETÉE'" 
                  :severity="dossierDetail.serviceTechniqueFeedback.approved ? 'success' : 'danger'"
                  class="text-lg font-bold"
                />
                <div class="text-sm text-600">
                  <i class="pi pi-calendar mr-1"></i>
                  Visite effectuée le {{ formatDate(dossierDetail.serviceTechniqueFeedback.dateVisite) }}
                </div>
              </div>
              <div class="text-sm text-600" v-if="dossierDetail.serviceTechniqueFeedback.agentServiceTechniqueName">
                <i class="pi pi-user mr-1"></i>
                {{ dossierDetail.serviceTechniqueFeedback.agentServiceTechniqueName }}
              </div>
            </div>

            <!-- Service Technique Details -->
            <div class="grid">
              <!-- Progress and Status -->
              <div class="col-12 md:col-4" v-if="dossierDetail.serviceTechniqueFeedback.pourcentageAvancement">
                <h5>Avancement du Projet</h5>
                <div class="flex align-items-center gap-2 mb-2">
                  <ProgressBar 
                    :value="dossierDetail.serviceTechniqueFeedback.pourcentageAvancement" 
                    class="flex-1"
                  />
                  <span class="font-bold">{{ dossierDetail.serviceTechniqueFeedback.pourcentageAvancement }}%</span>
                </div>
                <div class="text-sm text-600">
                  Statut: {{ getStatutVisiteLabel(dossierDetail.serviceTechniqueFeedback.statutVisite) }}
                </div>
              </div>

              <!-- Observations -->
              <div class="col-12 md:col-8" v-if="dossierDetail.serviceTechniqueFeedback.observations">
                <h5>Observations de la Visite</h5>
                <div class="p-3 bg-blue-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.observations }}
                </div>
              </div>

              <!-- Recommendations -->
              <div class="col-12 md:col-6" v-if="dossierDetail.serviceTechniqueFeedback.recommandations">
                <h5>Recommandations</h5>
                <div class="p-3 bg-green-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.recommandations }}
                </div>
              </div>

              <!-- Problems Detected -->
              <div class="col-12 md:col-6" v-if="dossierDetail.serviceTechniqueFeedback.problemesDetectes">
                <h5>Problèmes Détectés</h5>
                <div class="p-3 bg-orange-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.problemesDetectes }}
                </div>
              </div>

              <!-- Corrective Actions -->
              <div class="col-12" v-if="dossierDetail.serviceTechniqueFeedback.actionsCorrectives">
                <h5>Actions Correctives Prises</h5>
                <div class="p-3 bg-purple-50 border-round">
                  {{ dossierDetail.serviceTechniqueFeedback.actionsCorrectives }}
                </div>
              </div>

              <!-- Conclusion -->
              <div class="col-12" v-if="dossierDetail.serviceTechniqueFeedback.conclusion">
                <h5>Conclusion du Service Technique</h5>
                <div class="p-3 border-round" :class="{
                  'bg-green-50': dossierDetail.serviceTechniqueFeedback.approved,
                  'bg-red-50': !dossierDetail.serviceTechniqueFeedback.approved
                }">
                  {{ dossierDetail.serviceTechniqueFeedback.conclusion }}
                </div>
              </div>

              <!-- Technical Details -->
              <div class="col-12" v-if="hasServiceTechniqueVisitDetails()">
                <h5>Détails de la Visite</h5>
                <div class="grid">
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.coordonneesGPS">
                    <strong>Coordonnées GPS:</strong><br>
                    <small>{{ dossierDetail.serviceTechniqueFeedback.coordonneesGPS }}</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.dureeVisite">
                    <strong>Durée:</strong><br>
                    <small>{{ dossierDetail.serviceTechniqueFeedback.dureeVisite }} minutes</small>
                  </div>
                  <div class="col-4" v-if="dossierDetail.serviceTechniqueFeedback.dateConstat">
                    <strong>Date constat:</strong><br>
                    <small>{{ formatDate(dossierDetail.serviceTechniqueFeedback.dateConstat) }}</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Photos Section -->
            <div v-if="dossierDetail.serviceTechniqueFeedback.photosUrls && dossierDetail.serviceTechniqueFeedback.photosUrls.length > 0" class="mt-4">
              <h5>Photos de la Réalisation</h5>
              <div class="flex gap-2 flex-wrap">
                <div v-for="(photo, index) in dossierDetail.serviceTechniqueFeedback.photosUrls" :key="index" class="border-round overflow-hidden">
                  <img :src="photo" :alt="`Photo réalisation ${index + 1}`" class="w-8rem h-6rem object-cover cursor-pointer" 
                       @click="openPhotoDialog(photo)" />
                </div>
              </div>
            </div>
          </div>
          
          <!-- No Service Technique Decision Yet -->
          <div v-else class="text-center py-4">
            <i class="pi pi-clock text-4xl text-orange-500 mb-3"></i>
            <h4>En attente de la décision du Service Technique</h4>
            <p class="text-600">Le Service Technique n'a pas encore rendu sa décision concernant la réalisation du projet.</p>
            <Tag value="RÉALISATION EN COURS" severity="warning" />
          </div>
        </template>
      </Card>

      <!-- Documents Section -->
      <Card>
        <template #title>
          <i class="pi pi-file-edit mr-2"></i>Documents et Formulaires
        </template>
        <template #subtitle>
          Documents soumis par l'antenne - lecture seule
        </template>
        <template #content>
          <div v-if="getDocuments().length > 0" class="grid">
            <div 
              v-for="doc in getDocuments()" 
              :key="doc.id"
              class="col-12 md:col-6 lg:col-4"
            >
              <div class="document-card border-1 surface-border border-round p-3">
                <div class="document-header flex align-items-center justify-content-between mb-2">
                  <div class="flex align-items-center">
                    <i :class="getDocumentIcon(doc)" class="text-primary mr-2"></i>
                    <span class="font-medium">{{ getDocumentName(doc) }}</span>
                  </div>
                  <Tag 
                    :value="getDocumentStatusLabel(doc)" 
                    :severity="getDocumentStatusSeverity(doc)"
                    class="text-xs"
                  />
                </div>
                
                <div class="document-meta text-sm text-color-secondary mb-3">
                  <div>{{ formatDate(doc.dateUpload || doc.dateCreation) }}</div>
                  <div v-if="getDocumentType(doc)">{{ getDocumentType(doc) }}</div>
                  <div v-if="doc.description" class="mt-1">{{ doc.description }}</div>
                </div>

                <div class="document-actions flex gap-2">
                  <!-- Preview button for images/PDFs -->
                  <Button 
                    v-if="canPreviewDocument(doc)"
                    icon="pi pi-eye" 
                    size="small"
                    @click="openDocumentPreview(doc)"
                    class="p-button-outlined p-button-sm"
                    v-tooltip.top="'Aperçu'"
                  />
                  
                  <!-- Download button -->
                  <Button 
                    v-if="hasFile(doc)"
                    icon="pi pi-download" 
                    size="small"
                    @click="downloadDocument(doc)"
                    class="p-button-outlined p-button-sm"
                    v-tooltip.top="'Télécharger'"
                  />
                  
                  <!-- View form data button -->
                  <Button 
                    v-if="hasFormData(doc)"
                    icon="pi pi-list" 
                    size="small"
                    @click="openFormDataViewer(doc)"
                    class="p-button-outlined p-button-sm"
                    v-tooltip.top="'Voir formulaire'"
                  />
                  
                  <!-- View details button -->
                  <Button 
                    icon="pi pi-info-circle" 
                    size="small"
                    @click="openDocumentDetails(doc)"
                    class="p-button-outlined p-button-sm"
                    v-tooltip.top="'Détails'"
                  />
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="text-center py-4">
            <i class="pi pi-file text-4xl text-400 mb-3"></i>
            <p class="text-600">Aucun document disponible</p>
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
    </div>

    <!-- Action Dialogs -->
    <Dialog 
      v-model:visible="actionDialogs.sendToCommission.visible"
      header="Envoyer à la Commission"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div class="field">
          <label for="commissionComment" class="block mb-2">Commentaire</label>
          <Textarea 
            id="commissionComment"
            v-model="actionDialogs.sendToCommission.comment"
            rows="3"
            class="w-full"
            placeholder="Commentaire pour la commission..."
          />
        </div>
        <div class="field">
          <label for="priority" class="block mb-2">Priorité</label>
          <Dropdown 
            id="priority"
            v-model="actionDialogs.sendToCommission.priority"
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
          @click="actionDialogs.sendToCommission.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Envoyer" 
          @click="confirmAction('sendToCommission')"
          :loading="actionDialogs.sendToCommission.loading"
          class="p-button-success"
        />
      </template>
    </Dialog>

    <Dialog 
      v-model:visible="actionDialogs.sendToServiceTechnique.visible"
      header="Envoyer au Service Technique"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div class="field">
          <label for="serviceTechniqueComment" class="block mb-2">Commentaire</label>
          <Textarea 
            id="serviceTechniqueComment"
            v-model="actionDialogs.sendToServiceTechnique.comment"
            rows="3"
            class="w-full"
            placeholder="Commentaire pour le service technique..."
          />
        </div>
        <div class="field">
          <label for="serviceTechniquePriority" class="block mb-2">Priorité</label>
          <Dropdown 
            id="serviceTechniquePriority"
            v-model="actionDialogs.sendToServiceTechnique.priority"
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
          @click="actionDialogs.sendToServiceTechnique.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Envoyer" 
          @click="confirmAction('sendToServiceTechnique')"
          :loading="actionDialogs.sendToServiceTechnique.loading"
          class="p-button-success"
        />
      </template>
    </Dialog>

    <Dialog 
      v-model:visible="actionDialogs.validateRealization.visible"
      header="Valider la Réalisation"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div class="field">
          <label for="validateRealizationComment" class="block mb-2">Commentaire de validation</label>
          <Textarea 
            id="validateRealizationComment"
            v-model="actionDialogs.validateRealization.comment"
            rows="3"
            class="w-full"
            placeholder="Commentaires sur la validation de la réalisation..."
          />
        </div>
      </div>
      <template #footer>
        <Button 
          label="Annuler" 
          @click="actionDialogs.validateRealization.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Valider" 
          @click="confirmAction('validateRealization')"
          :loading="actionDialogs.validateRealization.loading"
          class="p-button-success"
        />
      </template>
    </Dialog>

    <Dialog 
      v-model:visible="actionDialogs.returnToAntenne.visible"
      header="Retourner à l'Antenne"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div class="field">
          <label for="returnComment" class="block mb-2">Motif du retour</label>
          <Textarea 
            id="returnComment"
            v-model="actionDialogs.returnToAntenne.comment"
            rows="3"
            class="w-full"
            placeholder="Précisez le motif du retour à l'antenne..."
          />
        </div>
        <div class="field">
          <label class="block mb-2">Documents manquants (optionnel)</label>
          <div class="grid">
            <div v-for="reason in returnReasons" :key="reason.value" class="col-6">
              <div class="flex align-items-center">
                <Checkbox 
                  :id="reason.value"
                  v-model="actionDialogs.returnToAntenne.reasons"
                  :value="reason.value"
                />
                <label :for="reason.value" class="ml-2">{{ reason.label }}</label>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <Button 
          label="Annuler" 
          @click="actionDialogs.returnToAntenne.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Retourner" 
          @click="confirmAction('returnToAntenne')"
          :loading="actionDialogs.returnToAntenne.loading"
          class="p-button-warning"
        />
      </template>
    </Dialog>

    <Dialog 
      v-model:visible="actionDialogs.reject.visible"
      header="Rejeter le Dossier"
      modal
      :style="{ width: '500px' }"
    >
      <div class="space-y-4">
        <div class="field">
          <label for="rejectComment" class="block mb-2">Motif du rejet</label>
          <Textarea 
            id="rejectComment"
            v-model="actionDialogs.reject.comment"
            rows="3"
            class="w-full"
            placeholder="Précisez le motif du rejet..."
          />
        </div>
        <div class="field">
          <div class="flex align-items-center">
            <Checkbox 
              id="definitiveReject"
              v-model="actionDialogs.reject.definitive"
              :binary="true"
            />
            <label for="definitiveReject" class="ml-2">Rejet définitif (ne peut plus être resoumis)</label>
          </div>
        </div>
      </div>
      <template #footer>
        <Button 
          label="Annuler" 
          @click="actionDialogs.reject.visible = false"
          class="p-button-outlined"
        />
        <Button 
          label="Rejeter" 
          @click="confirmAction('reject')"
          :loading="actionDialogs.reject.loading"
          class="p-button-danger"
        />
      </template>
    </Dialog>

    <!-- Document Preview Dialog -->
    <Dialog
      v-model:visible="documentPreviewDialog.visible"
      :modal="true"
      :style="{ width: '90vw', height: '90vh' }"
      :header="documentPreviewDialog.document ? getDocumentName(documentPreviewDialog.document) : 'Aperçu du document'"
      :maximizable="true"
      @show="loadDocumentPreview"
    >
      <div class="preview-dialog-content">
        <div v-if="documentPreviewDialog.loading" class="preview-loading">
          <ProgressSpinner />
          <p>Chargement du document...</p>
        </div>
        
        <div v-else-if="documentPreviewDialog.error" class="preview-error">
          <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-3"></i>
          <h3>Erreur de prévisualisation</h3>
          <p>{{ documentPreviewDialog.error }}</p>
          <Button
            label="Télécharger plutôt"
            icon="pi pi-download"
            @click="downloadDocument(documentPreviewDialog.document)"
            class="mt-3"
          />
        </div>
        
        <div
          v-else-if="documentPreviewDialog.contentType && documentPreviewDialog.contentType.startsWith('image/')"
          class="image-preview"
        >
          <img
            :src="documentPreviewDialog.dataUrl"
            :alt="getDocumentName(documentPreviewDialog.document)"
            class="max-w-full max-h-full object-contain"
          />
        </div>
        
        <div
          v-else-if="documentPreviewDialog.contentType === 'application/pdf'"
          class="pdf-preview"
        >
          <object
            :data="documentPreviewDialog.dataUrl"
            type="application/pdf"
            width="100%"
            height="100%"
          >
            <p>
              Ce PDF ne peut pas être affiché dans votre navigateur.
              <Button
                label="Télécharger le PDF"
                icon="pi pi-download"
                @click="downloadDocument(documentPreviewDialog.document)"
                class="mt-3"
              />
            </p>
          </object>
        </div>
        
        <div v-else class="preview-not-available">
          <i class="pi pi-file-export text-4xl text-400 mb-3"></i>
          <h3>Aperçu non disponible</h3>
          <p>
            Ce type de document ({{ getDocumentType(documentPreviewDialog.document) }}) 
            ne peut pas être prévisualisé directement.
          </p>
          <Button
            label="Télécharger le document"
            icon="pi pi-download"
            @click="downloadDocument(documentPreviewDialog.document)"
            class="mt-3"
          />
        </div>
      </div>
    </Dialog>

    <!-- Document Details Dialog -->
    <Dialog
      v-model:visible="documentDetailsDialog.visible"
      :modal="true"
      :style="{ width: '600px' }"
      header="Détails du Document"
    >
      <div v-if="documentDetailsDialog.document" class="document-details">
        <div class="detail-row">
          <strong>Nom:</strong> {{ getDocumentName(documentDetailsDialog.document) }}
        </div>
        <div class="detail-row">
          <strong>Type:</strong> {{ getDocumentType(documentDetailsDialog.document) }}
        </div>
        <div class="detail-row">
          <strong>Statut:</strong> 
          <Tag 
            :value="getDocumentStatusLabel(documentDetailsDialog.document)" 
            :severity="getDocumentStatusSeverity(documentDetailsDialog.document)"
          />
        </div>
        <div class="detail-row">
          <strong>Date de création:</strong> {{ formatDate(documentDetailsDialog.document.dateUpload || documentDetailsDialog.document.dateCreation) }}
        </div>
        <div v-if="documentDetailsDialog.document.description" class="detail-row">
          <strong>Description:</strong> {{ documentDetailsDialog.document.description }}
        </div>
        <div v-if="documentDetailsDialog.document.validationNotes" class="detail-row">
          <strong>Notes de validation:</strong> {{ documentDetailsDialog.document.validationNotes }}
        </div>
        <div class="detail-row">
          <strong>A un fichier:</strong> 
          <i :class="hasFile(documentDetailsDialog.document) ? 'pi pi-check text-green-500' : 'pi pi-times text-red-500'"></i>
        </div>
        <div class="detail-row">
          <strong>A des données de formulaire:</strong> 
          <i :class="hasFormData(documentDetailsDialog.document) ? 'pi pi-check text-green-500' : 'pi pi-times text-red-500'"></i>
        </div>
      </div>
      
      <template #footer>
        <div class="flex gap-2">
          <Button 
            v-if="hasFile(documentDetailsDialog.document)"
            label="Télécharger" 
            icon="pi pi-download"
            @click="downloadDocument(documentDetailsDialog.document)"
            class="p-button-outlined"
          />
          <Button 
            v-if="hasFormData(documentDetailsDialog.document)"
            label="Voir Formulaire" 
            icon="pi pi-list"
            @click="openFormDataViewer(documentDetailsDialog.document)"
            class="p-button-outlined"
          />
          <Button 
            label="Fermer" 
            @click="documentDetailsDialog.visible = false"
            class="p-button-outlined"
          />
        </div>
      </template>
    </Dialog>

    <!-- Form Data Viewer Dialog -->
    <FormDataViewerDialog
      v-model:visible="formDataDialog.visible"
      :form="formDataDialog.form"
      :dossier="dossierDetail"
      @add-note="handleAddNoteFromForm"
    />

    <!-- Photo Dialog -->
    <Dialog v-model:visible="photoDialog.visible" modal :style="{ width: '80vw', height: '80vh' }" header="Photo">
      <img :src="photoDialog.url" alt="Photo" class="w-full h-full object-contain" />
    </Dialog>

    <ConfirmDialog />
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import ApiService from '@/services/ApiService';
import AuthService from '@/services/AuthService';

// Components
import FormDataViewerDialog from '@/components/dossiers/FormDataViewerDialog.vue';

import Button from 'primevue/button';
import SplitButton from 'primevue/splitbutton';
import Card from 'primevue/card';
import Tag from 'primevue/tag';
import ProgressSpinner from 'primevue/progressspinner';
import ProgressBar from 'primevue/progressbar';
import Message from 'primevue/message';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import Dropdown from 'primevue/dropdown';
import Checkbox from 'primevue/checkbox';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();

// State
const loading = ref(true);
const error = ref('');
const dossierDetail = ref(null);
const currentPhase = ref(null);

// Dialog states for documents
const documentPreviewDialog = ref({
  visible: false,
  loading: false,
  error: null,
  document: null,
  dataUrl: '',
  contentType: ''
});

const documentDetailsDialog = ref({
  visible: false,
  document: null
});

const formDataDialog = ref({
  visible: false,
  form: null
});

const photoDialog = ref({
  visible: false,
  url: ''
});

// Blob URLs to clean up
const blobUrls = ref([]);

// Computed
const dossierId = computed(() => parseInt(route.params.dossierId));
const currentUser = computed(() => AuthService.getCurrentUser());

// Dialog states
const actionDialogs = ref({
  sendToCommission: { visible: false, loading: false, comment: '', priority: 'NORMALE' },
  sendToServiceTechnique: { visible: false, loading: false, comment: '', priority: 'NORMALE' },
  validateRealization: { visible: false, loading: false, comment: '' },
  returnToAntenne: { visible: false, loading: false, comment: '', reasons: [] },
  reject: { visible: false, loading: false, comment: '', definitive: false }
});

// Options
const priorityOptions = ref([
  { label: 'Normale', value: 'NORMALE' },
  { label: 'Haute', value: 'HAUTE' },
  { label: 'Faible', value: 'FAIBLE' }
]);

const returnReasons = ref([
  { label: 'Documents manquants', value: 'documents_manquants' },
  { label: 'Informations incomplètes', value: 'infos_incompletes' },
  { label: 'Format incorrect', value: 'format_incorrect' },
  { label: 'Signatures manquantes', value: 'signatures_manquantes' }
]);

onMounted(() => {
  loadDossierDetail();
});

// Clean up blob URLs when component is destroyed
onBeforeUnmount(() => {
  blobUrls.value.forEach((url) => {
    URL.revokeObjectURL(url);
  });
});

async function loadDossierDetail() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await ApiService.get(`/agent-guc/dossiers/detail/${dossierId.value}`);
    
    if (response && typeof response === 'object') {
      dossierDetail.value = {
        id: response.id,
        numeroDossier: response.numeroDossier,
        saba: response.saba,
        reference: response.reference,
        statut: response.statut,
        dateCreation: response.dateCreation,
        dateSubmission: response.dateSubmission,
        dateApprobation: response.dateApprobation,
        montantSubvention: response.montantSubvention,
        agriculteur: response.agriculteur || {},
        antenne: response.antenne || {},
        projet: response.projet || {},
        utilisateurCreateur: response.utilisateurCreateur || {},
        timing: response.timing || {},
        workflowHistory: Array.isArray(response.workflowHistory) ? response.workflowHistory : [],
        documents: Array.isArray(response.documents) ? response.documents : [],
        pieceJointes: Array.isArray(response.pieceJointes) ? response.pieceJointes : [],
        availableActions: Array.isArray(response.availableActions) ? response.availableActions : [],
        commissionFeedback: response.commissionFeedback || { decisionMade: false },
        serviceTechniqueFeedback: response.serviceTechniqueFeedback || { decisionMade: false },
        // Set default empty arrays for frontend computed properties
        availableForms: [],
        notes: []
      };
      
      currentPhase.value = detectPhase(response);
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
  router.push('/agent_guc/dossiers');
}

// Get documents from either documents or pieceJointes field
function getDocuments() {
  const documents = dossierDetail.value?.documents || [];
  const pieceJointes = dossierDetail.value?.pieceJointes || [];
  
  // Return whichever has content, prefer documents field
  if (documents.length > 0) {
    return documents;
  }
  return pieceJointes;
}

// Document handling methods
function getDocumentName(doc) {
  return doc.title || doc.nomDocument || doc.nomFichier || 'Document sans nom';
}

function getDocumentIcon(doc) {
  const type = getDocumentType(doc);
  if (type.includes('PDF')) return 'pi pi-file-pdf';
  if (type.includes('Word')) return 'pi pi-file-word';
  if (type.includes('Image')) return 'pi pi-image';
  if (hasFormData(doc)) return 'pi pi-list';
  return 'pi pi-file';
}

function getDocumentType(doc) {
  // Check if it has form data first
  if (hasFormData(doc)) {
    return 'Formulaire électronique';
  }
  
  // Check document type field from backend
  if (doc.documentType) {
    const typeLabels = {
      'SCANNED_DOCUMENT': 'Document scanné',
      'FORM_DATA': 'Formulaire électronique', 
      'TERRAIN_PHOTO': 'Photo terrain',
      'SUPPORTING_DOC': 'Document support',
      'GENERATED_REPORT': 'Rapport généré',
      'MIXED': 'Document mixte'
    };
    return typeLabels[doc.documentType] || doc.documentType;
  }
  
  // Check file type from file name or path
  const filePath = doc.cheminFichier || doc.nomDocument || '';
  if (!filePath) return 'Document';

  const extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
  switch (extension) {
    case '.pdf': return 'Document PDF';
    case '.doc': return 'Document Word';
    case '.docx': return 'Document Word';
    case '.jpg':
    case '.jpeg': return 'Image JPEG';
    case '.png': return 'Image PNG';
    case '.gif': return 'Image GIF';
    case '.svg': return 'Image SVG';
    default: return filePath ? `Document ${extension}` : 'Document';
  }
}

function getDocumentStatusLabel(doc) {
  const status = doc.statut || doc.status;
  const labels = {
    'PENDING': 'En attente',
    'COMPLETE': 'Complet',
    'REJECTED': 'Rejeté',
    'MISSING': 'Manquant',
    'DRAFT': 'Brouillon'
  };
  return labels[status] || status || 'Non défini';
}

function getDocumentStatusSeverity(doc) {
  const status = doc.statut || doc.status;
  const severities = {
    'PENDING': 'warning',
    'COMPLETE': 'success',
    'REJECTED': 'danger',
    'MISSING': 'danger',
    'DRAFT': 'secondary'
  };
  return severities[status] || 'secondary';
}

function hasFile(doc) {
  return (doc.cheminFichier && doc.cheminFichier.trim() !== '') || 
         (doc.nomDocument && doc.nomDocument.trim() !== '' && !hasFormData(doc));
}

function hasFormData(doc) {
  return doc.formData && 
         doc.formData.trim() !== '' && 
         doc.formData !== '{}' && 
         doc.formData !== 'null';
}

function canPreviewDocument(doc) {
  if (!hasFile(doc)) return false;
  
  const filePath = doc.cheminFichier || doc.nomDocument || doc.nomFichier || '';
  if (!filePath) return false;
  
  const extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
  return ['.pdf', '.jpg', '.jpeg', '.png', '.gif'].includes(extension);
}

function openDocumentPreview(doc) {
  documentPreviewDialog.value.document = doc;
  documentPreviewDialog.value.loading = true;
  documentPreviewDialog.value.error = null;
  documentPreviewDialog.value.dataUrl = '';
  documentPreviewDialog.value.contentType = '';
  documentPreviewDialog.value.visible = true;
}

function loadDocumentPreview() {
  if (!documentPreviewDialog.value.document) return;

  const doc = documentPreviewDialog.value.document;
  documentPreviewDialog.value.loading = true;
  documentPreviewDialog.value.error = null;

  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  const baseUrl = import.meta.env.VITE_API_URL || '/api';
  const previewUrl = `${baseUrl}/agent-guc/dossiers/documents/preview/${doc.id}`;

  fetch(previewUrl, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`Erreur de prévisualisation: ${response.status} ${response.statusText}`);
      }

      const contentType = response.headers.get('content-type');
      documentPreviewDialog.value.contentType = contentType;

      return response.blob();
    })
    .then((blob) => {
      const url = URL.createObjectURL(blob);
      blobUrls.value.push(url);
      documentPreviewDialog.value.dataUrl = url;
      documentPreviewDialog.value.loading = false;
    })
    .catch((error) => {
      console.error('Preview error:', error);
      documentPreviewDialog.value.error = error.message || 'Impossible de prévisualiser le fichier';
      documentPreviewDialog.value.loading = false;
    });
}

function downloadDocument(doc) {
  if (!doc || !hasFile(doc)) {
    toast.add({
      severity: 'warn',
      summary: 'Attention',
      detail: 'Ce document ne contient pas de fichier à télécharger',
      life: 3000
    });
    return;
  }

  toast.add({
    severity: 'info',
    summary: 'Téléchargement',
    detail: 'Préparation du téléchargement...',
    life: 2000
  });

  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  const baseUrl = import.meta.env.VITE_API_URL || '/api';

  fetch(`${baseUrl}/agent-guc/dossiers/documents/download/${doc.id}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`Erreur de téléchargement: ${response.status} ${response.statusText}`);
      }

      const contentType = response.headers.get('content-type');
      let filename = getDocumentName(doc);

      // Try to get filename from Content-Disposition header
      const disposition = response.headers.get('content-disposition');
      if (disposition && disposition.includes('filename=')) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        const matches = filenameRegex.exec(disposition);
        if (matches && matches[1]) {
          filename = matches[1].replace(/['"]/g, '');
        }
      }

      // Add extension based on content type if missing
      if (!filename.includes('.') && contentType) {
        if (contentType.includes('pdf')) filename += '.pdf';
        else if (contentType.includes('msword')) filename += '.doc';
        else if (contentType.includes('wordprocessingml')) filename += '.docx';
        else if (contentType.includes('jpeg')) filename += '.jpg';
        else if (contentType.includes('png')) filename += '.png';
      }

      filename = filename.replace(/[/\\?%*:|"<>]/g, '-');

      return response.blob().then((blob) => ({ blob, filename }));
    })
    .then(({ blob, filename }) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();

      setTimeout(() => {
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }, 100);

      toast.add({
        severity: 'success',
        summary: 'Téléchargement réussi',
        detail: `Fichier "${filename}" téléchargé avec succès`,
        life: 3000
      });
    })
    .catch((error) => {
      console.error('Download error:', error);
      toast.add({
        severity: 'error',
        summary: 'Erreur de téléchargement',
        detail: error.message || 'Impossible de télécharger le fichier',
        life: 3000
      });
    });
}

function openDocumentDetails(doc) {
  documentDetailsDialog.value.document = doc;
  documentDetailsDialog.value.visible = true;
}

function openFormDataViewer(doc) {
  if (!hasFormData(doc)) {
    toast.add({
      severity: 'warn',
      summary: 'Attention',
      detail: 'Ce document ne contient pas de données de formulaire',
      life: 3000
    });
    return;
  }

  try {
    const formData = JSON.parse(doc.formData);
    const form = {
      id: doc.id,
      title: getDocumentName(doc),
      description: doc.description || 'Données du formulaire électronique',
      isCompleted: true,
      formData: formData,
      formConfig: doc.formConfig ? JSON.parse(doc.formConfig) : null,
      lastModified: doc.dateUpload || doc.dateCreation,
      requiredDocuments: []
    };

    formDataDialog.value.form = form;
    formDataDialog.value.visible = true;
  } catch (error) {
    console.error('Error parsing form data:', error);
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les données du formulaire',
      life: 3000
    });
  }
}

function handleAddNoteFromForm(noteData) {
  // TODO: Implement note addition functionality
  toast.add({
    severity: 'info',
    summary: 'Fonctionnalité à venir',
    detail: 'L\'ajout de notes sera disponible prochainement',
    life: 3000
  });
}

function openPhotoDialog(photoUrl) {
  photoDialog.value.url = photoUrl;
  photoDialog.value.visible = true;
}

// Phase detection
function detectPhase(dossierDetail) {
  // Use timing information from backend
  if (dossierDetail.timing?.currentStep) {
    const step = dossierDetail.timing.currentStep;
    if (step.includes('Phase GUC')) {
      if (step.includes('Final') || dossierDetail.statut === 'APPROVED' || dossierDetail.statut === 'APPROVED_AWAITING_FARMER') {
        return 4; // Final approval phase
      }
      return 2; // Initial GUC review phase
    }
    if (step.includes('Commission') || step.includes('AHA-AF')) {
      return 3; // Commission phase
    }
    if (step.includes('Service Technique')) {
      return 7; // Service technique phase
    }
    if (step.includes('Antenne')) {
      return 1; // Antenne phase
    }
    if (step.includes('Réalisation')) {
      return 6; // Realization review phase
    }
  }
  
  // Fallback based on status and available actions
  if (dossierDetail.availableActions && Array.isArray(dossierDetail.availableActions)) {
    const actions = dossierDetail.availableActions.map(a => a.action);
    if (actions.includes('final-approval')) {
      return 4; // Final approval
    }
    if (actions.includes('final-realization-approval')) {
      return 8; // Final realization approval
    }
    if (actions.includes('assign-commission')) {
      return 2; // Initial GUC review
    }
    if (actions.includes('validate-realization')) {
      return 6; // Realization review
    }
  }
  
  return 2; // Default to phase 2 for GUC
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
  const actions = dossierDetail.value?.availableActions || [];
  return actions.some(action => action.action === 'final-approval');
}

function needsFinalRealizationApproval() {
  const actions = dossierDetail.value?.availableActions || [];
  return actions.some(action => action.action === 'final-realization-approval');
}

function hasApprovedFiche() {
  const statut = dossierDetail.value?.statut;
  return statut === 'APPROVED' || 
         statut === 'AWAITING_FARMER' ||
         statut === 'REALIZATION_IN_PROGRESS' ||
         statut === 'COMPLETED' ||
         dossierDetail.value?.dateApprobation;
}

function hasPhaseActions() {
  return dossierDetail.value?.availableActions && 
         Array.isArray(dossierDetail.value.availableActions) && 
         dossierDetail.value.availableActions.length > 0;
}

function getPhaseActionMenuItems() {
  if (!dossierDetail.value?.availableActions) return [];
  
  return dossierDetail.value.availableActions.map(action => {
    const actionMap = {
      'assign-commission': {
        label: 'Envoyer à la Commission',
        icon: 'pi pi-forward',
        command: () => showActionDialog('sendToCommission')
      },
      'send-to-service-technique': {
        label: 'Envoyer au Service Technique',
        icon: 'pi pi-cog',
        command: () => showActionDialog('sendToServiceTechnique')
      },
      'return': {
        label: 'Retourner à l\'Antenne',
        icon: 'pi pi-undo',
        command: () => showActionDialog('returnToAntenne')
      },
      'reject': {
        label: 'Rejeter',
        icon: 'pi pi-times-circle',
        command: () => showActionDialog('reject')
      },
      'final-approval': {
        label: 'Approuver',
        icon: 'pi pi-check',
        command: () => goToFinalApproval()
      },
      'validate-realization': {
        label: 'Valider Réalisation',
        icon: 'pi pi-check-circle',
        command: () => showActionDialog('validateRealization')
      },
      'final-realization-approval': {
        label: 'Approbation Finale Réalisation',
        icon: 'pi pi-check-square',
        command: () => goToFinalRealizationApproval()
      }
    };
    
    return actionMap[action.action] || {
      label: action.label || action.action,
      icon: 'pi pi-cog',
      command: () => console.log('Action not implemented:', action.action)
    };
  });
}

function getPrimaryPhaseActionLabel() {
  const actions = dossierDetail.value?.availableActions || [];
  if (actions.length === 0) return 'Actions';
  
  const primaryAction = actions[0];
  const labelMap = {
    'assign-commission': 'Commission',
    'send-to-service-technique': 'Service Technique',
    'final-approval': 'Approuver',
    'validate-realization': 'Valider',
    'final-realization-approval': 'Approbation Finale',
    'return': 'Retourner',
    'reject': 'Rejeter'
  };
  
  return labelMap[primaryAction.action] || primaryAction.label || 'Actions';
}

function handlePrimaryPhaseAction() {
  const actions = dossierDetail.value?.availableActions || [];
  if (actions.length === 0) return;
  
  const primaryAction = actions[0];
  switch (primaryAction.action) {
    case 'assign-commission':
      showActionDialog('sendToCommission');
      break;
    case 'send-to-service-technique':
      showActionDialog('sendToServiceTechnique');
      break;
    case 'final-approval':
      goToFinalApproval();
      break;
    case 'validate-realization':
      showActionDialog('validateRealization');
      break;
    case 'final-realization-approval':
      goToFinalRealizationApproval();
      break;
    case 'return':
      showActionDialog('returnToAntenne');
      break;
    case 'reject':
      showActionDialog('reject');
      break;
    default:
      console.log('Primary action not implemented:', primaryAction.action);
  }
}

function getWorkflowLocationText() {
  // Use timing assignedTo field from backend
  if (dossierDetail.value?.timing?.assignedTo) {
    return dossierDetail.value.timing.assignedTo;
  }
  
  // Fallback to current step parsing
  const currentStep = dossierDetail.value?.timing?.currentStep;
  if (currentStep) {
    if (currentStep.includes('GUC')) return 'Guichet Unique Central';
    if (currentStep.includes('Commission') || currentStep.includes('AHA-AF')) return 'Commission Terrain';
    if (currentStep.includes('Service Technique')) return 'Service Technique';
    if (currentStep.includes('Antenne')) return 'Antenne';
  }
  
  return 'Guichet Unique Central';
}

function goToFinalApproval() {
  router.push(`/agent_guc/dossiers/${dossierId.value}/final-approval`);
}

function goToFinalRealizationApproval() {
  router.push(`/agent_guc/dossiers/${dossierId.value}/final-realization-approval`);
}

function goToFiche() {
  router.push(`/agent_guc/dossiers/${dossierId.value}/fiche`);
}

function showActionDialog(action) {
  if (actionDialogs.value[action]) {
    actionDialogs.value[action].visible = true;
    actionDialogs.value[action].comment = '';
    actionDialogs.value[action].priority = 'NORMALE';
    if (action === 'returnToAntenne') {
      actionDialogs.value[action].reasons = [];
    }
    if (action === 'reject') {
      actionDialogs.value[action].definitive = false;
    }
  }
}

async function confirmAction(action) {
  try {
    const dialog = actionDialogs.value[action];
    dialog.loading = true;
    
    let endpoint = '';
    let payload = {};
    
    switch (action) {
      case 'sendToCommission':
        endpoint = `/agent-guc/dossiers/assign-commission/${dossierId.value}`;
        payload = { 
          commentaire: dialog.comment,
          priorite: dialog.priority
        };
        break;
      case 'sendToServiceTechnique':
        endpoint = `/agent-guc/dossiers/send-to-service-technique/${dossierId.value}`;
        payload = { 
          commentaire: dialog.comment,
          priorite: dialog.priority
        };
        break;
      case 'validateRealization':
        endpoint = `/agent-guc/dossiers/validate-realization/${dossierId.value}`;
        payload = { 
          commentaire: dialog.comment
        };
        break;
      case 'returnToAntenne':
        endpoint = `/agent-guc/dossiers/return/${dossierId.value}`;
        payload = { 
          motif: 'Complétion requise',
          commentaire: dialog.comment
        };
        break;
      case 'reject':
        endpoint = `/agent-guc/dossiers/reject/${dossierId.value}`;
        payload = { 
          motif: 'Rejet du dossier',
          commentaire: dialog.comment
        };
        break;
    }
    
    const response = await ApiService.post(endpoint, payload);
    
    if (response && response.success !== false) {
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: response.message || 'Action effectuée avec succès',
        life: 4000
      });
      
      dialog.visible = false;
      
      // Force refresh after a short delay to ensure backend transaction is committed
      setTimeout(async () => {
        await loadDossierDetail();
        console.log('Dossier detail reloaded after action:', action);
      }, 1000);
    }
    
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: error.message || 'Une erreur est survenue',
      life: 4000
    });
  } finally {
    actionDialogs.value[action].loading = false;
  }
}

function hasCommissionVisitDetails() {
  const feedback = dossierDetail.value?.commissionFeedback;
  return feedback?.coordonneesGPS || feedback?.dureeVisite || feedback?.conditionsMeteo;
}

function hasServiceTechniqueVisitDetails() {
  const feedback = dossierDetail.value?.serviceTechniqueFeedback;
  return feedback?.coordonneesGPS || feedback?.dureeVisite || feedback?.dateConstat;
}

function getStatutVisiteLabel(statut) {
  const labels = {
    'PROGRAMMEE': 'Programmée',
    'EN_COURS': 'En cours',
    'TERMINEE': 'Terminée',
    'REPORTEE': 'Reportée',
    'ANNULEE': 'Annulée'
  };
  return labels[statut] || statut;
}

// Utility functions
function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Brouillon',
    'SUBMITTED': 'Soumis',
    'IN_REVIEW': 'En cours',
    'APPROVED': 'Approuvé',
    'AWAITING_FARMER': 'En attente fermier',
    'REALIZATION_IN_PROGRESS': 'Réalisation en cours',
    'REJECTED': 'Rejeté',
    'COMPLETED': 'Terminé',
    'CANCELLED': 'Annulé',
    'RETURNED_FOR_COMPLETION': 'Retourné pour complétion',
    'PENDING_CORRECTION': 'En attente correction'
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
    'REALIZATION_IN_PROGRESS': 'warning',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'CANCELLED': 'secondary',
    'RETURNED_FOR_COMPLETION': 'warning',
    'PENDING_CORRECTION': 'warning'
  };
  return severities[status] || 'secondary';
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
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date));
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

/* Document card styles */
.document-card {
  transition: box-shadow 0.2s ease;
}

.document-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.document-header {
  min-height: 2rem;
}

.document-meta {
  min-height: 3rem;
}

.document-actions {
  flex-wrap: wrap;
}

/* Dialog styles */
.preview-dialog-content {
  height: 80vh;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.preview-error,
.preview-not-available {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  padding: 2rem;
}

.preview-error h3,
.preview-not-available h3 {
  margin: 1rem 0 0.5rem;
}

.preview-error p,
.preview-not-available p {
  margin: 0 0 1rem;
  color: var(--text-color-secondary);
  max-width: 500px;
}

.image-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  background-color: var(--surface-ground);
  border-radius: 4px;
  overflow: hidden;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.pdf-preview {
  height: 100%;
  width: 100%;
}

.pdf-preview object {
  border: none;
  background-color: white;
}

.document-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--surface-300);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-row strong {
  min-width: 150px;
  font-weight: 600;
}
</style>