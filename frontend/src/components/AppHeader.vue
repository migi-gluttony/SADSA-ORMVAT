<template>
  <header v-if="!isAuthenticated" class="app-header" :class="{ 'header-hidden': isHeaderHidden }">
    <div class="logo-section">
      <div class="logo-container">
        <router-link to="/" class="logo-link">
          <!-- Primary Logo -->
          <div class="logo-placeholder primary-logo">
            <img src="@/assets/logo/sadsa.svg" alt="sadsa Logo" />
          </div>
        </router-link>
        <router-link to="/" class="logo-link">
          <!-- Secondary Logo -->
          <div class="logo-placeholder secondary-logo">
            <img src="@/assets/logo/ormvat.png" alt="ormvat Logo" />
          </div>
        </router-link>
      </div>
    </div>

    <div class="content-section">
      <div class="action-menu">
        <div class="">
          <Button @click="navigateToLogin" class="custom-green-button " outlined icon="pi pi-sign-in"
            label="Se Connecter" />
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
// In AppHeader.vue
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import AuthService from '../services/AuthService';
import Button from 'primevue/button';

const router = useRouter();

// Create a reactive reference to track authentication state
const authState = ref(AuthService.isAuthenticated());
const isHeaderHidden = ref(false);
const lastScrollPosition = ref(0);

// Function to update the auth state
const updateAuthState = () => {
  authState.value = AuthService.isAuthenticated();
};

// Track scroll position for sticky header behavior
const handleScroll = () => {
  const currentScrollPosition = window.pageYOffset || document.documentElement.scrollTop;

  // Show header when scrolling up, hide when scrolling down
  if (currentScrollPosition < lastScrollPosition.value) {
    isHeaderHidden.value = false;
  } else if (currentScrollPosition > 50) {
    isHeaderHidden.value = true;
  }

  lastScrollPosition.value = currentScrollPosition;
};

// Auth state computed property that will be reactive
const isAuthenticated = computed(() => authState.value);

// Add event listeners
onMounted(() => {
  window.addEventListener('auth-state-changed', updateAuthState);
  window.addEventListener('scroll', handleScroll);
});

// Clean up event listeners
onUnmounted(() => {
  window.removeEventListener('auth-state-changed', updateAuthState);
  window.removeEventListener('scroll', handleScroll);
});

// Navigation function
const navigateToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.app-header {
  background-color: var(--background-color);
  display: flex;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 75px;
  z-index: 1000;
  box-shadow: 2px 2px 2px 2px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  margin: 10px;
  border-radius: 5px;
}

.header-hidden {
  transform: translateY(-130%);
}

.logo-section {
  width: 250px;
  /* Match sidebar width */
  height: calc(100%-6px);
  background-color: var(--background-color);
  display: flex;
  align-items: center;
  justify-content: center;
  /* border-bottom: 1px solid var(--surface-border); */
  margin: 3px;
  border-radius: 5px;
  overflow: hidden;
  border: solid 2px var(--primary-color);
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 1rem;
  width: 100%;
}

.logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  width: 50px;
}

.logo-placeholder img {
  max-height: 100%;
  object-fit: contain;
  /* Makes logos white */
}

.content-section {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex: 1;
  padding: 0 1.5rem;
  background-color: var(--surface-card);
  border-bottom: 1px solid var(--surface-border);
}

.action-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto;
}

/* Responsive Adjustments */
@media (max-width: 768px) {
  .app-header {
    flex-direction: column;
    height: auto;
  }

  .logo-section {
    width: 100%;
    padding: 0.5rem 0;
    order: 1;
  }

  .content-section {
    width: 100%;
    order: 2;
    padding: 0.75rem 1rem;
  }
}

@media (max-width: 480px) {
  .logo-placeholder {
    height: 40px;
    width: 40px;
  }

  .logo-container {
    padding: 0 0.5rem;
  }
}



/* Custom button  */
.custom-green-button {
  height: 3.5rem;
  width: 12rem;
  font-size: large;
  border-radius: 5px;
  color: var(--primary-color);
  border-color: var(--primary-color);
}

/* Target the hover state specifically with high specificity */
.custom-green-button:hover,
.p-button.custom-green-button:enabled:hover,
.p-button.p-button-rounded.custom-green-button:enabled:hover {
  color: var(--primary-color)!important;
  border-color: var(--primary-color)!important;
background-color: rgba(var(--primary-color-rgb), 0.05)!important;
}




</style>