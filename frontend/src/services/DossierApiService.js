import ApiService from './ApiService';

/**
 * Service API pour la gestion des dossiers
 */
const DossierApiService = {
  
  // ===== CRÉATION DE DOSSIERS =====
  
  /**
   * Récupère toutes les rubriques avec leurs sous-rubriques
   */
  async getRubriques() {
    try {
      return await ApiService.get('/agent_antenne/dossiers/rubriques');
    } catch (error) {
      console.error('Erreur lors de la récupération des rubriques:', error);
      throw new Error('Impossible de charger les types de projets');
    }
  },

  /**
   * Récupère la liste des CDAs disponibles
   */
  async getCDAs() {
    try {
      return await ApiService.get('/agent_antenne/dossiers/cdas');
    } catch (error) {
      console.error('Erreur lors de la récupération des CDAs:', error);
      throw new Error('Impossible de charger les CDAs');
    }
  },

  /**
   * Valide les données d'un dossier
   */
  async validateDossier(dossierData) {
    try {
      return await ApiService.post('/agent_antenne/dossiers/validate', dossierData);
    } catch (error) {
      console.error('Erreur lors de la validation:', error);
      throw new Error('Erreur de validation: ' + (error.message || 'Données invalides'));
    }
  },

  /**
   * Génère un aperçu du dossier
   */
  async previewDossier(dossierData) {
    try {
      return await ApiService.post('/agent_antenne/dossiers/preview', dossierData);
    } catch (error) {
      console.error('Erreur lors de la génération de l\'aperçu:', error);
      throw new Error('Impossible de générer l\'aperçu du dossier');
    }
  },

  /**
   * Crée un nouveau dossier
   */
  async createDossier(dossierData) {
    try {
      return await ApiService.post('/agent_antenne/dossiers/create', dossierData);
    } catch (error) {
      console.error('Erreur lors de la création du dossier:', error);
      
      // Gestion des erreurs spécifiques
      if (error.fieldErrors) {
        throw {
          type: 'validation',
          message: 'Données invalides',
          fieldErrors: error.fieldErrors,
          globalErrors: error.globalErrors
        };
      }
      
      throw new Error(error.message || 'Erreur lors de la création du dossier');
    }
  },

  /**
   * Sauvegarde un dossier en brouillon
   */
  async saveDraft(dossierData) {
    try {
      return await ApiService.post('/agent_antenne/dossiers/save-draft', dossierData);
    } catch (error) {
      console.error('Erreur lors de la sauvegarde:', error);
      throw new Error('Impossible de sauvegarder le brouillon');
    }
  },

  /**
   * Vérifie l'unicité d'un numéro SABA
   */
  async checkSabaUniqueness(saba) {
    try {
      return await ApiService.get('/agent_antenne/dossiers/check-saba', { saba });
    } catch (error) {
      console.error('Erreur lors de la vérification SABA:', error);
      return { isUnique: false, message: 'Erreur de vérification' };
    }
  },

  /**
   * Recherche un agriculteur par CIN
   */
  async searchAgriculteur(cin) {
    try {
      return await ApiService.get('/agent_antenne/dossiers/agriculteurs/search', { cin });
    } catch (error) {
      console.error('Erreur lors de la recherche:', error);
      return null;
    }
  },

  // ===== GESTION DES DOSSIERS =====

  /**
   * Récupère la liste des dossiers avec pagination et filtres
   */
  async getDossiers(page = 1, size = 20, filters = {}) {
    try {
      const params = {
        page: page - 1, // Spring pagination commence à 0
        size,
        ...filters
      };
      
      return await ApiService.get('/agent_antenne/dossiers', params);
    } catch (error) {
      console.error('Erreur lors de la récupération des dossiers:', error);
      throw new Error('Impossible de charger les dossiers');
    }
  },

  /**
   * Récupère un dossier par son ID
   */
  async getDossier(dossierId) {
    try {
      return await ApiService.get(`/agent_antenne/dossiers/${dossierId}`);
    } catch (error) {
      console.error('Erreur lors de la récupération du dossier:', error);
      throw new Error('Dossier non trouvé');
    }
  },

  /**
   * Met à jour un dossier
   */
  async updateDossier(dossierId, dossierData) {
    try {
      return await ApiService.put(`/agent_antenne/dossiers/${dossierId}`, dossierData);
    } catch (error) {
      console.error('Erreur lors de la mise à jour:', error);
      throw new Error('Impossible de mettre à jour le dossier');
    }
  },

  /**
   * Supprime un dossier (si autorisé)
   */
  async deleteDossier(dossierId) {
    try {
      return await ApiService.delete(`/agent_antenne/dossiers/${dossierId}`);
    } catch (error) {
      console.error('Erreur lors de la suppression:', error);
      throw new Error('Impossible de supprimer le dossier');
    }
  },

  // ===== DOCUMENTS ET FICHIERS =====

  /**
   * Upload un fichier pour un dossier
   */
  async uploadDocument(dossierId, documentType, file, progressCallback) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('documentType', documentType);
      
      return await ApiService.uploadFiles(
        `/agent_antenne/dossiers/${dossierId}/documents`, 
        formData, 
        progressCallback
      );
    } catch (error) {
      console.error('Erreur lors de l\'upload:', error);
      throw new Error('Impossible d\'uploader le fichier');
    }
  },

  /**
   * Télécharge un document
   */
  async downloadDocument(dossierId, documentId) {
    try {
      const response = await fetch(`/api/agent_antenne/dossiers/${dossierId}/documents/${documentId}/download`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
        }
      });
      
      if (!response.ok) {
        throw new Error('Erreur de téléchargement');
      }
      
      return response.blob();
    } catch (error) {
      console.error('Erreur lors du téléchargement:', error);
      throw new Error('Impossible de télécharger le document');
    }
  },

  // ===== RÉCÉPISSÉS ET RAPPORTS =====

  /**
   * Génère un récépissé pour un dossier
   */
  async generateRecepisse(dossierId) {
    try {
      return await ApiService.get(`/agent_antenne/dossiers/${dossierId}/recepisse`);
    } catch (error) {
      console.error('Erreur lors de la génération du récépissé:', error);
      throw new Error('Impossible de générer le récépissé');
    }
  },

  /**
   * Exporte les dossiers en Excel
   */
  async exportDossiers(filters = {}) {
    try {
      const response = await fetch('/api/agent_antenne/dossiers/export', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
        },
        body: JSON.stringify(filters)
      });
      
      if (!response.ok) {
        throw new Error('Erreur d\'export');
      }
      
      return response.blob();
    } catch (error) {
      console.error('Erreur lors de l\'export:', error);
      throw new Error('Impossible d\'exporter les dossiers');
    }
  },

  // ===== STATISTIQUES =====

  /**
   * Récupère les statistiques de création de dossiers
   */
  async getCreationStats(period = '30d') {
    try {
      return await ApiService.get('/agent_antenne/dossiers/stats', { period });
    } catch (error) {
      console.error('Erreur lors de la récupération des statistiques:', error);
      return {
        totalDossiers: 0,
        dossiersCeMois: 0,
        dossiersEnCours: 0,
        dossiersApprouves: 0
      };
    }
  },

  // ===== UTILITAIRES =====

  /**
   * Génère un numéro SABA automatique
   */
  async generateSabaNumber() {
    try {
      return await ApiService.get('/agent_antenne/dossiers/generate-saba');
    } catch (error) {
      console.error('Erreur lors de la génération SABA:', error);
      
      // Génération côté client en cas d'erreur
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const sequence = String(Math.floor(Math.random() * 9999) + 1).padStart(4, '0');
      
      return {
        saba: `${sequence}${month}/${year}/001`,
        generated: true,
        clientSide: true
      };
    }
  },

  /**
   * Valide le format d'un numéro SABA
   */
  validateSabaFormat(saba) {
    const sabaRegex = /^\d{6}\/\d{4}\/\d{3}$/;
    return {
      isValid: sabaRegex.test(saba),
      message: sabaRegex.test(saba) ? 'Format valide' : 'Format invalide (attendu: 000XXX/YYYY/ZZZ)'
    };
  },

  /**
   * Formatte les données pour l'affichage
   */
  formatDossierForDisplay(dossier) {
    return {
      ...dossier,
      nomCompletAgriculteur: `${dossier.agriculteur?.prenom || ''} ${dossier.agriculteur?.nom || ''}`.trim(),
      montantFormate: this.formatCurrency(dossier.montantDemande),
      dateDepotFormatee: this.formatDate(dossier.dateDepot),
      statutLibelle: this.getStatutLibelle(dossier.statut)
    };
  },

  /**
   * Formatte une devise
   */
  formatCurrency(amount) {
    if (!amount) return '0,00 DH';
    
    return new Intl.NumberFormat('fr-MA', {
      style: 'currency',
      currency: 'MAD',
      minimumFractionDigits: 2
    }).format(amount);
  },

  /**
   * Formatte une date
   */
  formatDate(date) {
    if (!date) return '';
    
    return new Intl.DateTimeFormat('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(new Date(date));
  },

  /**
   * Récupère le libellé d'un statut
   */
  getStatutLibelle(statut) {
    const statuts = {
      'BROUILLON': 'Brouillon',
      'SOUMIS': 'Soumis',
      'EN_COURS': 'En cours',
      'APPROUVE': 'Approuvé',
      'REJETE': 'Rejeté',
      'REALISE': 'Réalisé'
    };
    
    return statuts[statut] || statut;
  },

  /**
   * Récupère la couleur d'un statut pour l'affichage
   */
  getStatutColor(statut) {
    const colors = {
      'BROUILLON': 'secondary',
      'SOUMIS': 'info',
      'EN_COURS': 'warning',
      'APPROUVE': 'success',
      'REJETE': 'danger',
      'REALISE': 'success'
    };
    
    return colors[statut] || 'secondary';
  }
};

export default DossierApiService;