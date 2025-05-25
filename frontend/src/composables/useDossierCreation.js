import { ref, reactive, computed, watch } from 'vue';
import { useToast } from 'primevue/usetoast';
import DossierApiService from '@/services/DossierApiService';

export function useDossierCreation() {
  const toast = useToast();

  // État réactif
  const loading = reactive({
    rubriques: false,
    cdas: false,
    validation: false,
    creation: false,
    saveDraft: false
  });

  const formData = ref({
    agriculteur: {
      cin: '',
      nom: '',
      prenom: '',
      telephone: '',
      commune: '',
      province: ''
    },
    dossier: {
      saba: '',
      reference: '',
      cdaId: null,
      sousRubriqueId: null,
      dateDepot: new Date(),
      montantDemande: null
    },
    formulairesDynamiques: {}
  });

  const rubriques = ref([]);
  const cdas = ref([]);
  const selectedSousRubrique = ref(null);
  const validationErrors = ref({});
  const dossierSummary = ref(null);

  // Computed properties
  const isBasicInfoValid = computed(() => {
    const { agriculteur, dossier } = formData.value;
    return !!(
      agriculteur.cin &&
      agriculteur.nom &&
      agriculteur.prenom &&
      agriculteur.telephone &&
      dossier.saba &&
      dossier.cdaId &&
      dossier.montantDemande &&
      selectedSousRubrique.value
    );
  });

  const hasValidationErrors = computed(() => {
    return Object.keys(validationErrors.value).length > 0;
  });

  // Méthodes
  async function loadRubriques() {
    try {
      loading.rubriques = true;
      const response = await DossierApiService.getRubriques();
      rubriques.value = response.rubriques || [];
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: error.message,
        life: 3000
      });
    } finally {
      loading.rubriques = false;
    }
  }

  async function loadCDAs() {
    try {
      loading.cdas = true;
      cdas.value = await DossierApiService.getCDAs();
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: error.message,
        life: 3000
      });
    } finally {
      loading.cdas = false;
    }
  }

  function selectSousRubrique(sousRubrique) {
    selectedSousRubrique.value = sousRubrique;
    formData.value.dossier.sousRubriqueId = sousRubrique.id;
    formData.value.formulairesDynamiques = {};
  }

  async function validateDossier() {
    try {
      loading.validation = true;
      const response = await DossierApiService.validateDossier(formData.value);
      
      if (response.isValid) {
        validationErrors.value = {};
        return true;
      } else {
        validationErrors.value = response.suggestions || {};
        
        toast.add({
          severity: 'warn',
          summary: 'Validation',
          detail: 'Veuillez corriger les erreurs dans le formulaire',
          life: 3000
        });
        return false;
      }
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Erreur de validation',
        detail: error.message,
        life: 3000
      });
      return false;
    } finally {
      loading.validation = false;
    }
  }

  async function generatePreview() {
    try {
      dossierSummary.value = await DossierApiService.previewDossier(formData.value);
      return dossierSummary.value;
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: error.message,
        life: 3000
      });
      return null;
    }
  }

  async function createDossier() {
    try {
      loading.creation = true;
      
      const isValid = await validateDossier();
      if (!isValid) return null;

      const response = await DossierApiService.createDossier(formData.value);
      
      toast.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Dossier créé avec succès',
        life: 5000
      });

      return response;
    } catch (error) {
      if (error.type === 'validation') {
        validationErrors.value = error.fieldErrors || {};
        toast.add({
          severity: 'warn',
          summary: 'Données invalides',
          detail: error.message,
          life: 5000
        });
      } else {
        toast.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.message,
          life: 5000
        });
      }
      return null;
    } finally {
      loading.creation = false;
    }
  }

  async function saveDraft() {
    try {
      loading.saveDraft = true;
      await DossierApiService.saveDraft(formData.value);
      
      toast.add({
        severity: 'info',
        summary: 'Sauvegardé',
        detail: 'Brouillon sauvegardé avec succès',
        life: 3000
      });
      
      return true;
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Erreur',
        detail: error.message,
        life: 3000
      });
      return false;
    } finally {
      loading.saveDraft = false;
    }
  }

  async function checkSabaUniqueness(saba) {
    if (!saba || !DossierApiService.validateSabaFormat(saba).isValid) {
      return false;
    }

    try {
      const response = await DossierApiService.checkSabaUniqueness(saba);
      
      if (!response.isUnique) {
        validationErrors.value.saba = 'Ce numéro SABA est déjà utilisé';
        return false;
      } else {
        if (validationErrors.value.saba === 'Ce numéro SABA est déjà utilisé') {
          delete validationErrors.value.saba;
        }
        return true;
      }
    } catch (error) {
      console.error('Erreur vérification SABA:', error);
      return false;
    }
  }

  async function searchAgriculteur(cin) {
    if (!cin || cin.length < 8) return null;

    try {
      const agriculteur = await DossierApiService.searchAgriculteur(cin);
      
      if (agriculteur) {
        // Pré-remplir les champs
        formData.value.agriculteur = {
          ...formData.value.agriculteur,
          ...agriculteur
        };
        
        toast.add({
          severity: 'info',
          summary: 'Agriculteur trouvé',
          detail: `${agriculteur.prenom} ${agriculteur.nom}`,
          life: 3000
        });
      }
      
      return agriculteur;
    } catch (error) {
      console.error('Erreur recherche agriculteur:', error);
      return null;
    }
  }

  function resetForm() {
    formData.value = {
      agriculteur: {
        cin: '',
        nom: '',
        prenom: '',
        telephone: '',
        commune: '',
        province: ''
      },
      dossier: {
        saba: '',
        reference: '',
        cdaId: null,
        sousRubriqueId: null,
        dateDepot: new Date(),
        montantDemande: null
      },
      formulairesDynamiques: {}
    };
    
    selectedSousRubrique.value = null;
    validationErrors.value = {};
    dossierSummary.value = null;
  }

  function updateDynamicForm(documentName, data) {
    formData.value.formulairesDynamiques[documentName] = data;
  }

  // Initialisation
  async function initialize() {
    await Promise.all([
      loadRubriques(),
      loadCDAs()
    ]);
  }

  return {
    // État
    loading,
    formData,
    rubriques,
    cdas,
    selectedSousRubrique,
    validationErrors,
    dossierSummary,
    
    // Computed
    isBasicInfoValid,
    hasValidationErrors,
    
    // Méthodes
    loadRubriques,
    loadCDAs,
    selectSousRubrique,
    validateDossier,
    generatePreview,
    createDossier,
    saveDraft,
    checkSabaUniqueness,
    searchAgriculteur,
    resetForm,
    updateDynamicForm,
    initialize
  };
}