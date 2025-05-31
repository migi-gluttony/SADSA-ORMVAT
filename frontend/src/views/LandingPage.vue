<template>
  <div class="landing-container">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">SADSA</h1>
        <h2>Système Automatisé de Demande de Subventions Agricoles</h2>
        <p class="hero-subtitle">
          Plateforme complète pour la gestion, le suivi et l'automatisation
          des demandes de subventions agricoles de l'ORMVAT dans la région du Tadla
        </p>
        <div class="hero-buttons">
          <Button label="Se Connecter" icon="pi pi-user" class="p-button-rounded custom-green-button"
            @click="navigateTo('/login')" /> 
          <Button label="En Savoir Plus" icon="pi pi-info-circle"
            class="p-button-rounded p-button-outlined en-savoir-button custom-green-button-2" @click="scrollToFeatures" />
        </div>
      </div>
      <div class="hero-image">
        <img src="@/assets/illustrations/Farmer-amico.svg" alt="Agriculture et subventions"
          class="hero-img" />
      </div>
    </div>

    <!-- Statistics Strip -->
    <div class="stats-strip">
      <div class="stat-item">
        <div class="stat-icon">
          <i class="pi pi-folder"></i>
        </div>
        <div class="stat-value">500+</div>
        <div class="stat-label">Dossiers Traités</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon">
          <i class="pi pi-building"></i>
        </div>
        <div class="stat-value">5</div>
        <div class="stat-label">Antennes CDA</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon">
          <i class="pi pi-users"></i>
        </div>
        <div class="stat-value">50+</div>
        <div class="stat-label">Agents</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon">
          <i class="pi pi-map"></i>
        </div>
        <div class="stat-value">3600</div>
        <div class="stat-label">km² Couverts</div>
      </div>
    </div>

    <!-- Features Section -->
    <div id="features" class="features-section">
      <div class="section-header">
        <h2>Fonctionnalités Principales</h2>
        <p>Une plateforme complète pour gérer l'ensemble du processus des subventions agricoles</p>
      </div>

      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-folder-plus"></i>
          </div>
          <h3>Gestion des Dossiers</h3>
          <p>Création, modification et suivi centralisé des dossiers de subvention avec traçabilité complète.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-sitemap"></i>
          </div>
          <h3>Workflow Automatisé</h3>
          <p>Routage automatique des dossiers entre les antennes, GUC et commission technique selon les délais réglementaires.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-chart-bar"></i>
          </div>
          <h3>Reporting et Statistiques</h3>
          <p>Tableaux de bord personnalisables avec analyses prédictives et exports multiples formats.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-file"></i>
          </div>
          <h3>Gestion Documentaire</h3>
          <p>Stockage sécurisé, classification automatique et recherche avancée des documents.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-bell"></i>
          </div>
          <h3>Alertes et Notifications</h3>
          <p>Système d'alertes automatiques pour le respect des délais et les actions requises.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <i class="pi pi-shield"></i>
          </div>
          <h3>Sécurité Avancée</h3>
          <p>Authentification JWT, chiffrement des données et gestion des droits d'accès par rôle.</p>
        </div>
      </div>
    </div>

    <!-- For Agents Section -->
    <div class="agents-section">
      <div class="content-half">
        <h2>Pour les Agents d'Antenne</h2>
        <p class="section-subtitle">Simplifiez la gestion des dossiers agricoles</p>
        <ul class="benefits-list">
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Création et modification rapide des dossiers</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Suivi en temps réel de l'état des dossiers</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Import/Export des données Excel</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Respect automatique des délais réglementaires</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Interface intuitive et professionnelle</span>
          </li>
        </ul>
        <Button label="Commencer" icon="pi pi-arrow-right" class="custom-green-button-3 p-button-rounded" @click="navigateTo('/login')" />
      </div>
      <div class="image-half">
        <img src="@/assets/illustrations/Working-amico.svg" alt="Agent travaillant sur dossiers"
          class="rounded-image" />
      </div>
    </div>

    <!-- For Administration Section -->
    <div class="admin-section">
      <div class="image-half">
        <img src="@/assets/illustrations/Control Panel-rafiki.svg" alt="Gestion et administration" class="rounded-image" />
      </div>
      <div class="content-half">
        <h2>Pour les Gestionnaires et Commission</h2>
        <p class="section-subtitle">Outils d'évaluation et de suivi efficaces</p>
        <ul class="benefits-list">
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Tableau de bord centralisé pour tous les dossiers</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Workflow d'approbation et de réalisation</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Gestion des observations et commentaires</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Planification des visites de terrain</span>
          </li>
          <li>
            <i class="pi pi-check-circle"></i>
            <span>Rapports automatisés et statistiques</span>
          </li>
        </ul>
        <Button label="Découvrir" icon="pi pi-arrow-right" class="custom-green-button-3 p-button-rounded" @click="navigateTo('/login')" />
      </div>
    </div>

    <!-- Process Section -->
    <div class="process-section">
      <div class="section-header">
        <h2>Processus Simplifié</h2>
        <p>Suivez le parcours automatisé de vos dossiers de subvention</p>
      </div>
      
      <div class="process-steps">
        <div class="step-card">
          <div class="step-number">1</div>
          <h3>Phase Antenne</h3>
          <p>Dépôt et création du dossier électronique</p>
          <span class="step-duration">3 jours max</span>
        </div>
        
        <div class="step-arrow">
          <i class="pi pi-arrow-right"></i>
        </div>
        
        <div class="step-card">
          <div class="step-number">2</div>
          <h3>Guichet Unique</h3>
          <p>Réception et transmission vers la commission</p>
          <span class="step-duration">2 jours max</span>
        </div>
        
        <div class="step-arrow">
          <i class="pi pi-arrow-right"></i>
        </div>
        
        <div class="step-card">
          <div class="step-number">3</div>
          <h3>Commission Technique</h3>
          <p>Évaluation et approbation du dossier</p>
          <span class="step-duration">15 jours max</span>
        </div>
        
        <div class="step-arrow">
          <i class="pi pi-arrow-right"></i>
        </div>
        
        <div class="step-card">
          <div class="step-number">4</div>
          <h3>Réalisation</h3>
          <p>Visite de terrain et constat final</p>
          <span class="step-duration">23 jours max</span>
        </div>
      </div>
    </div>

    <!-- Call to Action -->
    <div class="cta-section">
      <h2>Prêt à moderniser la gestion de vos subventions agricoles ?</h2>
      <p>Rejoignez la plateforme utilisée par l'ORMVAT pour la région du Tadla</p>
      <Button label="Se Connecter" icon="pi pi-sign-in" class="custom-green-button-2 p-button-rounded p-button-lg"
        @click="navigateTo('/login')" />
    </div>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="footer-content">
        <div class="footer-logo">
          <img src="@/assets/logo/sadsa-full.png" alt="Logo ORMVAT" class="logo-img" />
          <div class="logo-text">
            <h3>SADSA ORMVAT</h3>
            <p>Système Automatisé de Demande de Subventions Agricoles</p>
          </div>
        </div>

        <div class="footer-links">
          <h4>Navigation</h4>
          <ul>
            <li><a href="#" @click.prevent="navigateTo('/')">Accueil</a></li>
            <li><a href="#" @click.prevent="scrollToFeatures">Fonctionnalités</a></li>
            <li><a href="#" @click.prevent="navigateTo('/login')">Se connecter</a></li>
            <li><a href="#" @click.prevent="navigateTo('/reset-password')">Mot de passe oublié</a></li>
          </ul>
        </div>

        <div class="footer-contact">
          <h4>Contact</h4>
          <p><i class="pi pi-envelope"></i> contact@ormvat.ma</p>
          <p><i class="pi pi-phone"></i> +212 5 23 XX XX XX</p>
          <p><i class="pi pi-globe"></i> www.ormvat.ma</p>
          <p><i class="pi pi-envelope"></i> sadsa@ormvat.ma</p>
        </div>
      </div>

      <div class="footer-map-section">
        <div class="map-info">
          <div class="footer-logo ormvat-logo">
            <img src="@/assets/logo/ormvat-full.png" alt="Logo ORMVAT" class="logo-img" />
            <div class="contact-info">
              <h3>Office Régional de Mise en Valeur Agricole du Tadla "ORMVAT"</h3>
              <p><i class="pi pi-map-marker"></i> BP 244. Fquih Ben Salah, Maroc</p>
                            <p><i class="pi pi-globe"></i> <a href="https://ormvatadla.ma/"
                  target="_blank">https://ormvatadla.ma/</a></p>
          <p><i class="pi pi-phone"></i> +212 5 23 43 50 23/35/48</p>
                    <p><i class="pi pi-print"></i> +212 5 23 43 50 18/22</p>
                        <p><i class="pi pi-envelope"></i> contact@ormvatadla.ma</p>


            </div>
          </div>
        </div>
        <div class="map-container">
          <!-- Map showing Tadla region -->
          <iframe
            src="https://www.google.com/maps/embed?pb=!1m14!1m8!1m3!1d13458.929308323543!2d-6.6867021!3d32.5065827!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0xf2dbc6624fb86938!2sClub%20Ormvat%2C%20Fquih%20Ben%20Salah!5e0!3m2!1sen!2sma!4v1666103600866!5m2!1sen!2sma"
            width="100%" height="300" style="border:0;" allowfullscreen="" loading="lazy"
            referrerpolicy="no-referrer-when-downgrade"></iframe>
        </div>
      </div>

      <div class="footer-bottom">
        <p>&copy; 2025 ORMVAT - Office Régional de Mise en Valeur Agricole du Tadla. Tous droits réservés.</p>
      </div>
    </footer>
  </div>
</template>

<script>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Button from 'primevue/button';

export default {
  components: {
    Button
  },
  setup() {
    const router = useRouter();

    // Navigation methods
    const navigateTo = (path) => {
      router.push(path);
    };

    // Scroll to features section
    const scrollToFeatures = () => {
      document.getElementById('features').scrollIntoView({
        behavior: 'smooth'
      });
    };

    onMounted(() => {
      // Any initialization code if needed
    });

    return {
      navigateTo,
      scrollToFeatures
    };
  }
};
</script>

<style scoped>
:root {
  /* SADSA Color Scheme - Light Mode */
  --primary-color: #01723e;
  --primary-color-rgb: 1, 114, 62;
  --secondary-color: #efb157;
  --accent-color: #2e7145;
  --text-color: #213547;
  --background-color: #ffffff;
}

.dark-mode {
  /* SADSA Color Scheme - Dark Mode */
  --primary-color: #01864a;
  --primary-color-rgb: 1, 134, 74;
  --secondary-color: #d7983f;
  --accent-color: #3a8757;
  --text-color: #e5e7eb;
  --background-color: #1e1e1e;
}

.landing-container {
  max-width: 100%;
  overflow-x: hidden;
  margin: 0;
  padding: 0;
  font-family: 'Rubik', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: var(--text-color);
  background-color: var(--background-color);
}

/* Hero Section */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  padding: 4rem 6rem;
  padding-top: calc(75px + 3rem);
  background: var(--background-color);
  color: var(--text-color);
  border-radius: 0 0 5px 5px;
  gap: 2rem;
  margin: 0;
}

.hero-content {
  flex: 1;
  max-width: 60%;
}

.hero-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 6.3rem;
  margin-bottom: 1rem;
  background: linear-gradient(to right, var(--primary-color), var(--border-color));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  padding-left: 0.5rem;
}

.hero-subtitle {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  opacity: 0.9;
  line-height: 1.6;
}

.hero-buttons {
  display: flex;
  gap: 1rem;
}

.hero-image {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.hero-img {
  max-width: 100%;
  width: 60%;
  transition: transform 0.3s;
}

.hero-img:hover {
  transform: translateY(-5px);
}

/* Stats Strip */
.stats-strip {
  display: flex;
  justify-content: space-around;
  padding: 2rem;
  margin: 0 auto;
  max-width: 1200px;
  transform: translateY(-50px);
  background-color: var(--background-color);
  border-radius: 5px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-item {
  text-align: center;
  padding: 1rem;
}

.stat-icon {
  font-size: 2rem;
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 1rem;
  color: #666;
}

/* Features Section */
.features-section {
  padding: 4rem 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 3rem;
}

.section-header h2 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 2.2rem;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.section-header p {
  font-size: 1.1rem;
  color: #666;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
}

.feature-card {
  background-color: var(--background-color);
  padding: 2rem;
  border-radius: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid rgba(var(--primary-color-rgb), 0.1);
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 25px rgba(var(--primary-color-rgb), 0.15);
  border-color: var(--primary-color);
}

.feature-icon {
  font-size: 2rem;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.feature-card h3 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: var(--text-color);
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

/* Process Section */
.process-section {
  padding: 5rem 2rem;
  background-color: #f8f9fa;
  margin: 4rem 0;
}

.dark-mode .process-section {
  background-color: rgba(255, 255, 255, 0.03);
}

.process-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 3rem;
}

.step-card {
  background-color: var(--background-color);
  padding: 2rem;
  border-radius: 5px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 200px;
  border: 2px solid var(--primary-color);
}

.step-number {
  background-color: var(--primary-color);
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-weight: bold;
  font-size: 1.2rem;
}

.step-card h3 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.step-duration {
  color: var(--secondary-color);
  font-weight: bold;
  font-size: 0.9rem;
}

.step-arrow {
  color: var(--primary-color);
  font-size: 1.5rem;
}

/* Agents and Admin Sections */
.agents-section,
.admin-section {
  display: flex;
  align-items: center;
  padding: 5rem 2rem;
  gap: 3rem;
  max-width: 1200px;
  margin: 0 auto;
}

.admin-section {
  background-color: #f5f5f5;
  border-radius: 5px;
  margin: 4rem auto;
}

.dark-mode .admin-section {
  background-color: rgba(255, 255, 255, 0.03);
}

.content-half {
  flex: 1;
}

.content-half h2 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 2rem;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.section-subtitle {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 2rem;
}

.benefits-list {
  list-style: none;
  padding: 0;
  margin-bottom: 2rem;
}

.benefits-list li {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  color: var(--text-color);
}

.benefits-list li i {
  color: var(--primary-color);
  margin-right: 1rem;
  font-size: 1.2rem;
}

.image-half {
  flex: 1;
  display: flex;
  justify-content: center;
}

.rounded-image {
  max-width: 100%;
}

/* Call to Action */
.cta-section {
  text-align: center;
  padding: 5rem 2rem;
  color: var(--text-color);
  border-radius: 5px 5px 0 0;
  margin: 0;
}

.cta-section h2 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 2.2rem;
  margin-bottom: 1rem;
}

.cta-section p {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

/* Footer */
.landing-footer {
  background-color: #f2f2f2;
  padding: 4rem 2rem 1rem;
  margin: 0;
}

.dark-mode .landing-footer {
  background-color: #222;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  max-width: 1200px;
  margin: 0 auto 2rem;
}

.footer-map-section {
  display: flex;
  max-width: 1200px;
  margin: 0 auto 2rem;
  gap: 2rem;
  flex-wrap: wrap;
}

.map-container {
  flex: 3;
  min-width: 300px;
  border-radius: 5px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.map-info {
  flex: 2;
  min-width: 250px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.footer-logo {
  flex: 1;
  min-width: 200px;
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
}

.logo-img {
  height: 150px;
  margin-bottom: 15px;
  object-fit: contain;
}

.ormvat-logo {
  flex: 1.5;
}

.logo-text h3 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 1.5rem;
  margin: 0 0 0.5rem;
  color: var(--text-color);
}

.logo-text p {
  color: #666;
}

.contact-info p {
  font-size: 0.85rem;
  line-height: 1.4;
  margin-bottom: 0.5rem;
  color: #666;
  display: flex;
  align-items: flex-start;
}

.contact-info p i {
  margin-right: 0.5rem;
  margin-top: 0.25rem;
  color: var(--primary-color);
}

.footer-links,
.footer-contact {
  flex: 1;
  min-width: 200px;
  margin-bottom: 1.5rem;
}

.footer-links h4,
.footer-contact h4 {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 1.1rem;
  margin-bottom: 1rem;
  color: var(--text-color);
  position: relative;
  padding-bottom: 0.5rem;
}

.footer-links h4:after,
.footer-contact h4:after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 40px;
  height: 2px;
  background-color: var(--primary-color);
}

.footer-links ul {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 0.5rem;
}

.footer-links a {
  color: #666;
  text-decoration: none;
  transition: color 0.2s, transform 0.2s;
  display: inline-block;
  position: relative;
}

.footer-links a:hover {
  color: var(--primary-color);
  transform: translateX(5px);
}

.footer-contact p {
  display: flex;
  align-items: center;
  color: #666;
  margin-bottom: 0.5rem;
}

.footer-contact i {
  margin-right: 0.5rem;
  color: var(--primary-color);
}

.footer-bottom {
  text-align: center;
  padding-top: 2rem;
  border-top: 1px solid #e0e0e0;
  color: #666;
}

.dark-mode .footer-bottom {
  border-top-color: #333;
}

/* Custom Buttons */
.custom-green-button {
  height: 3.5rem;
  width: 12rem;
  font-size: large;
  border-radius: 5px;
  color: var(--primary-color);
  border-color: var(--primary-color);
  background-color: rgba(0, 0, 0, 0);
}

.custom-green-button:hover,
.p-button.custom-green-button:enabled:hover {
  color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  background-color: rgba(var(--primary-color-rgb), 0.05) !important;
}

.custom-green-button-2 {
  height: 3.5rem;
  width: 12rem;
  font-size: large;
  border-radius: 5px;
  color: var(--primary-color);
  border-color: var(--primary-color);
  background-color: rgba(0, 0, 0, 0);
}

.custom-green-button-2:hover,
.p-button.custom-green-button-2:enabled:hover {
  color: var(--primary-color) !important;
  background-color: rgba(var(--primary-color-rgb), 0.05) !important;
}

.custom-green-button-3 {
  height: 3rem;
  width: 10rem;
  font-size: large;
  border-radius: 5px;
  color: var(--primary-color);
  border-color: var(--primary-color);
  background-color: rgba(0, 0, 0, 0) !important;
}

.custom-green-button-3:hover,
.p-button.custom-green-button-3:enabled:hover {
  color: var(--primary-color) !important;
  background-color: rgba(var(--primary-color-rgb), 0.05) !important;
}

/* Dark mode specific adjustments */
.dark-mode .feature-card,
.dark-mode .stats-strip,
.dark-mode .step-card {
  background-color: #252424;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.dark-mode .stat-label,
.dark-mode .section-header p,
.dark-mode .feature-card p,
.dark-mode .section-subtitle,
.dark-mode .footer-logo p,
.dark-mode .footer-links a,
.dark-mode .footer-contact p,
.dark-mode .footer-bottom {
  color: #999;
}

/* Responsive Design */
@media (max-width: 1024px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .process-steps {
    flex-direction: column;
  }
  
  .step-arrow {
    transform: rotate(90deg);
  }
}

@media (max-width: 768px) {
  .hero-section {
    flex-direction: column;
    text-align: center;
    padding: 3rem 1rem;
    padding-top: calc(60px + 1rem);
  }

  .hero-title {
    font-size: 3rem;
  }

  .stats-strip {
    flex-wrap: wrap;
    transform: translateY(0);
    margin-top: 2rem;
  }

  .stat-item {
    flex: 1 0 45%;
  }

  .agents-section,
  .admin-section {
    flex-direction: column;
    padding: 3rem 1rem;
  }

  .admin-section {
    flex-direction: column-reverse;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .step-card {
    min-width: 100%;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .stat-item {
    flex: 1 0 100%;
  }

  .section-header h2,
  .cta-section h2 {
    font-size: 1.8rem;
  }
}
</style>