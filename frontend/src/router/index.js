import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import AuthService from '../services/AuthService'
import RegisterView from '@/views/auth/RegisterView.vue'
import LandingPage from '@/views/LandingPage.vue'
import CreateDossierView from '@/views/agent_antenne/CreateDossierView.vue'
import DossierListView from '@/views/common/DossierListView.vue'
import DossierDetailView from '@/views/common/DossierDetailView.vue'
import AdminDocumentRequisView from '@/views/admin/AdminDocumentRequisView.vue'
import DocumentFillingView from '@/views/agent_antenne/DocumentFillingView.vue'
import ProfileView from '@/views/common/ProfileView.vue'
import TerrainVisitsView from '@/views/commission/TerrainVisitsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',  // ✅ Fixed: Added proper name
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
      component: DossierListView,
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
      component: DossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Détails du Dossier' },
      props: true
    },
    {
      path: '/agent_antenne/dossiers/:dossierId/forms',
      name: 'agent-antenne-dossier-forms',
      component: DossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Formulaires du Dossier' },
      props: true
    },
    {
      path: '/agent_antenne/dossiers/documents/:dossierId',
      name: 'dossier-documents',
      component: DocumentFillingView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE', title: 'Documents du Dossier' },
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
      component: DossierListView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Dossiers - Guichet Unique Central' }
    },
    {
      path: '/agent_guc/dossiers/:dossierId',
      name: 'agent-guc-dossier-detail',
      component: DossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_GUC', title: 'Détails du Dossier' },
      props: true
    },

    // Agent Commission Routes
    {
      path: '/agent_commission/dossiers',
      name: 'agent-commission-dossiers-list',
      component: DossierListView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION', title: 'Dossiers - Commission AHA-AF' }
    },
    {
      path: '/agent_commission/dossiers/:dossierId',
      name: 'agent-commission-dossier-detail',
      component: DossierDetailView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION', title: 'Détails du Dossier' },
      props: true
    },
    {
      path: '/agent_commission/terrain-visits',
      name: 'agent-commission-terrain-visits',
      component:TerrainVisitsView,
      meta: { requiresAuth: true, requiresRole: 'AGENT_COMMISSION', title: 'Visites Terrain - Commission AHA-AF' }
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
    // ✅ Fixed: Redirect authenticated users to valid routes
    const userRole = currentUser?.role;
    switch (userRole) {
      case 'AGENT_ANTENNE':
        next({ path: '/agent_antenne/dossiers' });
        break;
      case 'AGENT_GUC':
        next({ path: '/agent_guc/dossiers' });
        break;
      case 'AGENT_COMMISSION':
        next({ path: '/agent_commission/dossiers' });
        break;
      case 'ADMIN':
        next({ path: '/admin/documents-requis' }); // ✅ Fixed: Valid admin route
        break;
      default:
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