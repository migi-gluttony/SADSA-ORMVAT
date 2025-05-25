import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import AuthService from '../services/AuthService'
import RegisterView from '@/views/auth/RegisterView.vue'
import LandingPage from '@/views/LandingPage.vue'
import CreateDossierView from '@/views/agent_antenne/CreateDossierView.vue'
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
    // agent antenne 
    {
      path: '/agent_antenne/dossiers/create',
      name: 'create-dossier',
      component: CreateDossierView,
      meta: {
        requiresAuth: true,
        requiresRole: 'AGENT_ANTENNE',
        title: 'Créer un dossier'
      }
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
    // Redirect authenticated users to dashboard
    next({ name: 'dashboard' });
  }
  // Allow access to public routes
  else {
    next();
  }
})

router.beforeEach((to, from, next) => {
  if (to.name === '' || to.path === '/' || to.path === '/login' || to.path === '/register') {
    document.body.classList.add('landing-page');
  } else {
    document.body.classList.remove('landing-page');
  }
  next();
});

export default router