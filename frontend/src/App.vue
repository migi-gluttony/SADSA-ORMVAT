<template>
  <div class="app-container" :class="{ 
    'dark-mode': isDarkMode, 
    'sidebar-collapsed': sidebarCollapsed,
    'sidebar-visible': isAuthenticated,
    'no-header': isAuthenticated || hideHeader
  }">
    <AppHeader v-if="!hideHeader" />
    
    <div class="main-layout" :class="{ 'no-header': isAuthenticated || hideHeader }">
      <AppSidebar 
        v-if="isAuthenticated"
        :collapsed="sidebarCollapsed"
        @toggle="toggleSidebar"
      />
      
      <main class="app-content" :class="{ 'no-sidebar': !isAuthenticated }">
        <RouterView />
      </main>
    </div>
    
    <Button 
      class="theme-toggle"
      @click="toggleTheme"
      :icon="isDarkMode ? 'pi pi-sun' : 'pi pi-moon'"
      tooltip="Changer de thème"
      rounded
      text
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import AppSidebar from '@/components/AppSidebar.vue';
import Button from 'primevue/button';
import { ThemeService } from '@/services/ThemeService';
import AuthService from '@/services/AuthService';

// Get route instance
const route = useRoute();

// Sidebar state
const sidebarCollapsed = ref(false);

// Track authentication state
const isAuthenticated = ref(AuthService.isAuthenticated());

// Theme state
const isDarkMode = ref(false);

// Computed property to check if header should be hidden
const hideHeader = computed(() => {
  return route.meta.hideHeader === true;
});

// Toggle sidebar collapsed state
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  // Save preference to localStorage
  localStorage.setItem('sidebar-collapsed', sidebarCollapsed.value);
};

// Toggle theme between light and dark
const toggleTheme = () => {
  const newTheme = ThemeService.toggleTheme();
  isDarkMode.value = newTheme === 'dark';
};

// Update authentication state when it changes
const updateAuthState = () => {
  isAuthenticated.value = AuthService.isAuthenticated();
  // If user logs out, restore default sidebar state
  if (!isAuthenticated.value) {
    sidebarCollapsed.value = false;
  }
};

onMounted(() => {
  // Initialize theme
  ThemeService.init();
  
  // Set initial dark mode state
  isDarkMode.value = ThemeService.getTheme() === 'dark';
  
  // Set initial sidebar state from localStorage if authenticated
  if (isAuthenticated.value) {
    const savedSidebarState = localStorage.getItem('sidebar-collapsed');
    if (savedSidebarState !== null) {
      sidebarCollapsed.value = savedSidebarState === 'true';
    }
  }
  
  // Watch for theme changes
  const observer = new MutationObserver(() => {
    isDarkMode.value = document.documentElement.classList.contains(ThemeService.DARK_MODE_CLASS);
  });
  
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class']
  });
  
  // Listen for authentication changes
  window.addEventListener('auth-state-changed', updateAuthState);
});
</script>

<style>
/* Global styles */
:root {
  --text-color: #213547;
  --background-color: #ffffff;
  --content-padding: 1rem;
  --sidebar-width: 250px;
  --sidebar-collapsed-width: 60px;
  --header-height: 75px;
}

.dark-mode {
  --text-color: #ffffffde;
  --background-color: #1e1e1e;
}

body {
  margin: 0;
  padding: 0;
  color: var(--text-color);
  background-color: var(--background-color);
  transition: color 0.3s, background-color 0.3s;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout {
  display: flex;
  flex: 1;
  /* padding-top: var(--header-height); */
}

/* Remove top padding when header is not present */
.main-layout.no-header {
  padding-top: 0;
}

/* Also adjust sidebar padding when no header */
.no-header .app-sidebar {
  padding-top: 0;
}

.app-content {
  flex: 1;
  padding: 0rem;
  transition: margin-left 0.3s ease;
  min-height: calc(100vh - var(--header-height));
}

/* Adjust content height when no header */
.no-header .app-content {
  min-height: 100vh;
}

/* Only apply margin when sidebar is visible */
.sidebar-visible .app-content:not(.no-sidebar) {
  margin-left: var(--sidebar-width);
}

.sidebar-visible.sidebar-collapsed .app-content:not(.no-sidebar) {
  margin-left: var(--sidebar-collapsed-width);
}

/* No margin when no sidebar */
.app-content.no-sidebar {
  margin-left: 0;
}

.theme-toggle {
  position: fixed !important;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: 3rem !important;
  height: 3rem !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

/* Responsive styles */
@media (max-width: 768px) {
  .app-content {
    margin-left: 0 !important;
    padding: 1rem;
  }
  
  :root {
    --header-height: 60px;
  }
}
</style>