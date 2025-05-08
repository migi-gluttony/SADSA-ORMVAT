import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
// Use dynamic imports for components that might not be available yet
const DashboardView = () => import('@/views/DashboardView.vue')
const NotFoundView = () => import('@/views/NotFoundView.vue')
import AuthService from '@/services/AuthService'
import RegisterView from '@/views/auth/RegisterView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guest: true }
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
      meta: { guest: true }
    },
    // Catch-all route for 404
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: NotFoundView
    }
  ]
})

// Navigation guard to check authentication
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
  if (to.name === 'login') {
    document.body.classList.add('login-page');
  } else {
    document.body.classList.remove('login-page');
  }
  next();
});

export default router