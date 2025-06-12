import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import AuthService from '../services/AuthService'
import RegisterView from '@/views/auth/RegisterView.vue'
import LandingPage from '@/views/LandingPage.vue'
import CreateDossierView from '@/views/agent_antenne/CreateDossierView.vue'
import AdminDocumentRequisView from '@/views/admin/AdminDocumentRequisView.vue'
import ProfileView from '@/views/common/ProfileView.vue'
import TerrainVisitsView from '@/views/agent_commission/TerrainVisitsView.vue'
import AgentAntenneDossierListView from '@/components/agent_antenne/dossier_list/AgentAntenneDossierListView.vue'
import AgentGUCDossierListView from '@/components/agent_guc/dossier_list/AgentGUCDossierListView.vue'
import AgentCommissionDossierListView from '@/components/agent_commission/dossier_list/AgentCommissionDossierListView.vue'
import AgentAntenneDossierDetailView from '@/components/agent_antenne/dossier_details/AgentAntenneDossierDetailView.vue'
import DocumentFillingView from '@/components/agent_antenne/document_filling/DocumentFillingView.vue'
import AgentGucDossierDetailView from '@/components/agent_guc/dossier_detail/AgentGucDossierDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingPage,
      meta: { guest: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guest: true, hideHeader: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true }
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guest: true, hideHeader: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true }
    },
    // Agent Antenne Routes
     {
      path: '/agent_antenne/dossiers',
      name: 'agent-antenne-dossiers-list',
      component: AgentAntenneDossierListView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Mes Dossiers' }
    },
    {
      path: '/agent_antenne/dossiers/create',
      name: 'create-dossier',
      component: CreateDossierView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Créer un Dossier' }
    },
    {
      path: '/agent_antenne/dossiers/:dossierId',
      name: 'agent-antenne-dossier-detail',
      component: AgentAntenneDossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Détails du Dossier' },
      props: true
    },
    {
      path: '/agent_antenne/dossiers/documents/:dossierId',
      name: 'agent-antenne-document-filling',
      component: DocumentFillingView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Remplissage des Documents' },
      props: true
    },
    {
      path: '/agent_antenne/dossiers/:dossierId/fiche',
      name: 'agent-antenne-fiche-approbation',
      component: () => import('@/views/agent_guc/FicheApprobationView.vue'),
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Fiche d\'Approbation' },
      props: true
    },

    // Admin Routes
    {
      path: '/admin/documents-requis',
      name: 'admin-documents-requis',
      component: AdminDocumentRequisView,
      meta: { requiresAuth: true, requiresRole: 'ADMIN', title: 'Gestion des Documents Requis' }
    },

    // Agent GUC Routes
    {
      path: '/agent_guc/dossiers',
      name: 'agent-guc-dossiers-list',
      component: AgentGUCDossierListView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Dossiers - Guichet Unique Central' }
    },
    {
      path: '/agent_guc/dossiers/:dossierId',
      name: 'agent-guc-dossier-detail',
      component: AgentGucDossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Détails du Dossier' },
      props: true
    },
     {
      path: '/agent_guc/dossiers/:dossierId/final-approval',
      name: 'agent-guc-final-approval',
      component: () => import('@/views/agent_guc/FinalApprobationView.vue'),
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Décision Finale d\'Approbation' },
      props: true
    },
    {
      path: '/agent_guc/dossiers/:dossierId/fiche',
      name: 'agent-guc-fiche-approbation',
      component: () => import('@/views/agent_guc/FicheApprobationView.vue'),
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Fiche d\'Approbation' },
      props: true
    },
    {
      path: '/agent_guc/dossiers/:dossierId/final-realization-approval',
      name: 'agent-guc-final-realization-approval',
      component: () => import('@/components/agent_guc/FinalRealizationApprovalView.vue'),
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Approbation Finale de la Réalisation' },
      props: true
    },

    // Agent Commission Routes
    {
      path: '/agent_commission/dossiers',
      name: 'agent-commission-dossiers-list',
      component: AgentCommissionDossierListView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION_TERRAIN', title: 'Dossiers - Commission Visite Terrain' }
    },
    {
      path: '/agent_commission/dossiers/:dossierId',
      name: 'agent-commission-dossier-detail',
      component: AgentGucDossierDetailView, // Reuse the GUC detail view
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION_TERRAIN', title: 'Détails du Dossier - Commission' },
      props: true
    },
    {
      path: '/agent_commission/terrain-visits',
      name: 'agent-commission-terrain-visits',
      component: TerrainVisitsView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION_TERRAIN', title: 'Visites Terrain - Commission' }
    },

    // Service Technique Routes
    {
      path: '/service_technique/dossiers',
      name: 'service-technique-dossiers-list',
      component: () => import('@/components/service_technique/ServiceTechniqueDossierListView.vue'),
      meta: { requiresAuth: true, requiresRole: 'SERVICE_TECHNIQUE', title: 'Dossiers - Service Technique' }
    },
    {
      path: '/service_technique/dossiers/:dossierId',
      name: 'service-technique-dossier-detail',
      component: AgentGucDossierDetailView, // Reuse the GUC detail view
      meta: { requiresAuth: true, requiresRole: 'SERVICE_TECHNIQUE', title: 'Détails du Dossier - Service Technique' },
      props: true
    },
    {
      path: '/service_technique/implementation-visits',
      name: 'service-technique-implementation-visits',
      component: () => import('@/components/service_technique/ImplementationVisitsView.vue'),
      meta: { requiresAuth: true, requiresRole: 'SERVICE_TECHNIQUE', title: 'Visites d\'Implémentation - Service Technique' }
    },
    {
      path: '/service_technique/visits/:visitId',
      name: 'service-technique-visit-detail',
      component: () => import('@/components/service_technique/ImplementationVisitDetailComponent.vue'),
      meta: { requiresAuth: true, requiresRole: 'SERVICE_TECHNIQUE', title: 'Détails de la Visite d\'Implémentation' },
      props: true
    },

    // Catch-all route for 404
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: NotFoundView
    }
  ]
})

// ✅ Fixed: Combined navigation guards into one
router.beforeEach((to, from, next) => {
  // Handle landing page class management
  if (to.name === 'landing' || to.path === '/' || to.path === '/login' || to.path === '/register') {
    document.body.classList.add('landing-page');
  } else {
    document.body.classList.remove('landing-page');
  }

  // Check if the user is authenticated
  const isAuthenticated = AuthService.isAuthenticated();
  const currentUser = AuthService.getCurrentUser();

  // Handle routes that require authentication
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      // Redirect to login if not authenticated
      next({ name: 'login', query: { redirect: to.fullPath } });
      return;
    }

    // Check for role requirements
    if (to.meta.requiresRole && currentUser?.role !== to.meta.requiresRole) {
      // User doesn't have the required role, redirect to dashboard
      next({ name: 'dashboard' });
      return;
    }

    // User is authenticated and has the required role (if any)
    next();
  }
  // Handle routes for guests only
  else if (to.matched.some(record => record.meta.guest) && isAuthenticated) {
    const userRole = currentUser?.role;
    switch (userRole) {
      case 'AGENT_ANTENNE':
        next({ path: '/agent_antenne/dossiers' });
        break;
      case 'AGENT_GUC':
        next({ path: '/agent_guc/dossiers' });
        break;
      case 'AGENT_COMMISSION_TERRAIN':
        next({ path: '/agent_commission/dossiers' });
        break;
      case 'ADMIN':
        next({ path: '/admin/documents-requis' });
        break;
      default:final
        next({ name: 'dashboard' });
    }
  }
  // Allow access to public routes
  else {
    next();
  }
})

// Set page title based on route meta
router.afterEach((to) => {
  const title = to.meta.title;
  if (title) {
    document.title = `${title} - SADSA ORMVAT`;
  } else {
    document.title = 'SADSA ORMVAT';
  }
});

export default router