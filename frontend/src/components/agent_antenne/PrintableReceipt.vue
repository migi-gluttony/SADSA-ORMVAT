<template>
  <div class="printable-receipt print-only">
    <div class="receipt-document">
      <!-- Header with logos and title -->
      <div class="print-header">
        <div class="header-logo left-logo">
          <img src="/src/assets/logo/logo_ministre.jpg" alt="Armoiries du Maroc" />
        </div>
        <div class="header-text">
          <div class="header-title">
            <p><strong>Royaume du Maroc</strong></p>
            <p>Ministère de l'Agriculture, de la Pêche Maritime</p>
            <p>et du Développement Rural et des Eaux et Forêts</p>
            <p><strong>OFFICE RÉGIONAL DE MISE EN VALEUR AGRICOLE DU TADLA</strong></p>
          </div>
        </div>
        <div class="header-logo right-logo">
          <img src="/src/assets/logo/logo-ormvat-full-original.jpg" alt="Logo ORMVAT" />
        </div>
      </div>
      
      <hr class="header-separator" />
      
      <!-- Document Title -->
      <div class="document-title">
        <h1>RÉCÉPISSÉ DE DÉPÔT DE DEMANDE DE SUBVENTION</h1>
        <h2>Système Automatisé de Demande de Subventions Agricoles (SADSA)</h2>
        <h3>N° {{ receipt.numeroRecepisse }}</h3>
      </div>
      
      <!-- Receipt Content -->
      <div class="content-area">
        <div class="receipt-info">
          <div class="info-section">
            <h3>INFORMATIONS DU DEMANDEUR</h3>
            <table class="info-table">
              <tr>
                <td class="label">Nom complet :</td>
                <td class="value">{{ receipt.nomComplet }}</td>
              </tr>
              <tr>
                <td class="label">CIN :</td>
                <td class="value">{{ receipt.cin }}</td>
              </tr>
              <tr>
                <td class="label">Téléphone :</td>
                <td class="value">{{ receipt.telephone }}</td>
              </tr>
            </table>
          </div>

          <div class="info-section">
            <h3>INFORMATIONS DU DOSSIER</h3>
            <table class="info-table">
              <tr>
                <td class="label">Type de projet :</td>
                <td class="value">{{ receipt.typeProduit }}</td>
              </tr>
              <tr>
                <td class="label">Numéro SABA :</td>
                <td class="value"><strong>{{ receipt.saba }}</strong></td>
              </tr>
              <tr>
                <td class="label">Montant demandé :</td>
                <td class="value">{{ formatCurrency(receipt.montantDemande) }}</td>
              </tr>
              <tr>
                <td class="label">CDA :</td>
                <td class="value">{{ receipt.cdaNom }}</td>
              </tr>
              <tr>
                <td class="label">Antenne :</td>
                <td class="value">{{ receipt.antenne }}</td>
              </tr>
              <tr>
                <td class="label">Date de dépôt :</td>
                <td class="value">{{ formatDate(receipt.dateDepot) }}</td>
              </tr>
            </table>
          </div>

          <div class="important-notice">
            <h3>INFORMATIONS IMPORTANTES</h3>
            <ul>
              <li>Ce récépissé atteste du dépôt de votre demande de subvention agricole.</li>
              <li>Conservez précieusement ce document pour tout suivi de votre dossier.</li>
              <li>Le numéro SABA est votre référence unique pour toute correspondance.</li>
              <li>Vous serez contacté pour toute information complémentaire nécessaire.</li>
              <li>Le traitement de votre dossier se fait selon les délais réglementaires en vigueur.</li>
            </ul>
          </div>

          <div class="process-info">
            <h3>ÉTAPES DE TRAITEMENT</h3>
            <div class="process-steps">
              <div class="process-step">
                <div class="step-number">1</div>
                <div class="step-content">
                  <strong>Phase Antenne</strong>
                  <p>Réception et vérification du dossier (3 jours max)</p>
                </div>
              </div>
              <div class="process-step">
                <div class="step-number">2</div>
                <div class="step-content">
                  <strong>Guichet Unique Central</strong>
                  <p>Transmission vers la commission (2 jours max)</p>
                </div>
              </div>
              <div class="process-step">
                <div class="step-number">3</div>
                <div class="step-content">
                  <strong>Commission Technique</strong>
                  <p>Évaluation et décision (15 jours max)</p>
                </div>
              </div>
              <div class="process-step">
                <div class="step-number">4</div>
                <div class="step-content">
                  <strong>Réalisation</strong>
                  <p>Visite de terrain et validation (23 jours max)</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Footer section -->
      <div class="footer-section">
        <div class="contact-info">
          <p><strong>Pour toute information :</strong></p>
          <p>ORMVAT - BP 244, Fquih Ben Salah</p>
          <p>Tél : +212 5 23 43 50 23/35/48</p>
          <p>Email : sadsa@ormvatadla.ma</p>
        </div>
        <div class="signature-area">
          <p>Fait à {{ receipt.antenne }}, le {{ formatDate(receipt.dateDepot) }}</p>
          <div class="signature">
            <p>Cachet et Signature</p>
            <div class="signature-line"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  receipt: {
    type: Object,
    required: true
  }
});

function formatCurrency(amount) {
  if (!amount) return '0,00 DH';
  
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD',
    minimumFractionDigits: 2
  }).format(amount);
}

function formatDate(date) {
  if (!date) return '';
  
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(new Date(date));
}
</script>

<style>
/* Print-only styles */
@media print {
  body * {
    visibility: hidden;
  }
  
  .print-only, .print-only * {
    visibility: visible;
  }
  
  .printable-receipt {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
  
  .printable-receipt * {
    color: black !important;
    background: white !important;
    font-family: Arial, sans-serif !important;
    box-shadow: none !important;
  }
  
  @page {
    size: A4;
    margin: 1.5cm;
  }
}

.receipt-document {
  position: relative;
  min-height: 100vh;
  padding: 20px;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
}

/* Header styles */
.print-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.header-logo {
  width: 80px;
  height: 80px;
}

.header-logo img {
  width: 100%;
  height: auto;
  object-fit: contain;
}

.header-text {
  text-align: center;
  flex: 1;
}

.header-title p {
  margin: 3px 0;
  font-size: 11px;
  line-height: 1.2;
}

.header-separator {
  border: none;
  border-top: 2px solid black;
  margin: 15px 0 25px 0;
}

/* Document title */
.document-title {
  text-align: center;
  margin-bottom: 40px;
}

.document-title h1 {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  text-decoration: underline;
}

.document-title h2 {
  font-size: 12px;
  font-weight: normal;
  margin: 5px 0;
  font-style: italic;
}

.document-title h3 {
  font-size: 14px;
  font-weight: bold;
  margin: 10px 0;
  border: 2px solid black;
  padding: 8px;
  display: inline-block;
}

/* Content styles */
.content-area {
  width: 100%;
  margin-bottom: 30px;
}

.receipt-info {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.info-section {
  margin-bottom: 20px;
}

.info-section h3 {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 10px;
  text-decoration: underline;
  color: black;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table td {
  padding: 6px 8px;
  border: 1px solid black;
  font-size: 11px;
}

.info-table .label {
  background-color: #f0f0f0;
  font-weight: bold;
  width: 30%;
}

.info-table .value {
  width: 70%;
}

.important-notice {
  border: 2px solid black;
  padding: 15px;
  background-color: #f9f9f9;
}

.important-notice h3 {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 10px;
  text-align: center;
}

.important-notice ul {
  margin: 0;
  padding-left: 20px;
  font-size: 10px;
  line-height: 1.4;
}

.important-notice li {
  margin-bottom: 5px;
}

.process-info {
  margin-top: 20px;
}

.process-info h3 {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 15px;
  text-decoration: underline;
}

.process-steps {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.process-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

.step-number {
  width: 25px;
  height: 25px;
  border-radius: 50%;
  background-color: black;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 12px;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-content strong {
  font-size: 10px;
  display: block;
  margin-bottom: 3px;
}

.step-content p {
  font-size: 9px;
  margin: 0;
  line-height: 1.3;
}

/* Footer styles */
.footer-section {
  display: flex;
  justify-content: space-between;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid black;
}

.contact-info {
  width: 45%;
  font-size: 10px;
  line-height: 1.4;
}

.contact-info p {
  margin: 2px 0;
}

.signature-area {
  width: 45%;
  text-align: center;
  font-size: 10px;
}

.signature {
  margin-top: 20px;
}

.signature-line {
  border-top: 1px solid black;
  height: 50px;
  margin-top: 15px;
  width: 150px;
  margin-left: auto;
  margin-right: auto;
}

/* Screen view styles for preview */
@media screen {
  .printable-receipt {
    max-width: 210mm;
    min-height: 297mm;
    margin: 0 auto;
    background: white;
    box-shadow: 0 0 20px rgba(0,0,0,0.1);
  }
}
</style>