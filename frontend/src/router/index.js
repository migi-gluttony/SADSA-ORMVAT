import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import AuthService from '../services/AuthService'
import RegisterView from '@/views/auth/RegisterView.vue'
import LandingPage from '@/views/LandingPage.vue'
import CreateDossierView from '@/views/agent_antenne/CreateDossierView.vue'
import DossierListView from '@/views/agent_antenne/DossierListView.vue'
import DossierDetailView from '@/views/agent_antenne/DossierDetailView.vue'
import AdminDocumentRequisView from '@/views/admin/AdminDocumentRequisView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '',
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
    
    // Agent Antenne Routes
    {
      path: '/agent_antenne',
      meta: { requiresAuth: true, requiresRole: 'AGENT_ANTENNE' },
      children: [
        {
          path: 'dossiers',
          name: 'dossiers-list',
          component: DossierListView,
          meta: { title: 'Mes Dossiers' }
        },
        {
          path: 'dossiers/create',
          name: 'create-dossier',
          component: CreateDossierView,
          meta: { title: 'Créer un Dossier' }
        },
        {
          path: 'dossiers/:dossierId',
          name: 'dossier-detail',
          component: DossierDetailView,
          meta: { title: 'Détails du Dossier' },
          props: true
        },
        {
          path: 'dossiers/:dossierId/forms',
          name: 'dossier-forms',
          component: DossierDetailView,
          meta: { title: 'Formulaires du Dossier' },
          props: true
        }
      ]
    },
    // Admin Routes
    {
      path: '/admin',
      meta: { requiresAuth: true, requiresRole: 'ADMIN' },
      children: [
        {
          path: 'documents-requis',
          name: 'admin-documents-requis',
          component: AdminDocumentRequisView,
          meta: { title: 'Gestion des Documents Requis' }
        }
      ]
    },
    // Catch-all route for 404
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: NotFoundView
    }
  ]
})

// Navigation guard to check authentication and role-based access
router.beforeEach((to, from, next) => {
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
    // Redirect authenticated users to their appropriate dashboard
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
        next({ path: '/admin/dashboard' });
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

// Add landing page class management
router.beforeEach((to, from, next) => {
  if (to.name === '' || to.path === '/' || to.path === '/login' || to.path === '/register') {
    document.body.classList.add('landing-page');
  } else {
    document.body.classList.remove('landing-page');
  }
  next();
});

export default router