<template>
  <div class="app-header">
    <div class="header-content">
      <!-- Search Section -->
      <div class="search-section">
        <div class="search-container">
          <div class="search-input-wrapper">
            <i class="pi pi-search search-icon"></i>
            <InputText 
              v-model="searchValue" 
              :placeholder="searchPlaceholder" 
              class="search-input"
              @input="handleSearch"
            />
            <button 
              v-if="searchValue" 
              class="clear-button" 
              @click="clearSearch"
              title="Effacer la recherche"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>
        </div>
      </div>
      
      <!-- Actions Section -->
      <div class="actions-section">
        <!-- Notifications -->
        <div class="action-item">
          <button class="action-button notification-button" title="Notifications">
            <i class="pi pi-bell"></i>
            <span v-if="notificationCount > 0" class="notification-badge">
              {{ notificationCount > 99 ? '99+' : notificationCount }}
            </span>
          </button>
        </div>

        <!-- Theme Toggle -->
        <div class="action-item">
          <button class="action-button theme-button" @click="toggleTheme" :title="isDarkMode ? 'Mode clair' : 'Mode sombre'">
            <i :class="isDarkMode ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>
        </div>

        <!-- Quick Actions -->
        <div class="action-item">
          <button class="action-button" title="Aide" @click="$emit('help-click')">
            <i class="pi pi-question-circle"></i>
          </button>
        </div>
        
        <div class="action-item">
          <button class="action-button" title="Paramètres" @click="$emit('settings-click')">
            <i class="pi pi-cog"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import InputText from 'primevue/inputtext';
import { ThemeService } from '@/services/ThemeService';

// Props
const props = defineProps({
  searchPlaceholder: {
    type: String,
    default: 'Rechercher dans SADSA...'
  },
  initialSearchValue: {
    type: String,
    default: ''
  },
  notificationCount: {
    type: Number,
    default: 0
  }
});

// Emits
const emit = defineEmits(['search', 'notification-click', 'help-click', 'settings-click']);

// Component state
const searchValue = ref(props.initialSearchValue);
const isDarkMode = computed(() => ThemeService.getTheme() === 'dark');

// Methods
function handleSearch() {
  emit('search', searchValue.value);
}

function clearSearch() {
  searchValue.value = '';
  handleSearch();
}

function toggleTheme() {
  ThemeService.toggleTheme();
}

// Watch props
watch(() => props.initialSearchValue, (newValue) => {
  searchValue.value = newValue;
});
</script>

<style scoped>
.app-header {
  background: var(--card-background);
  border-bottom: 1px solid var(--card-border);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
  margin-bottom: var(--component-spacing);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem var(--component-spacing);
  max-width: 100%;
  gap: var(--component-spacing);
}

/* Search Section */
.search-section {
  flex: 1;
  max-width: 500px;
}

.search-container {
  width: 100%;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
  z-index: 2;
  transition: color 0.3s ease;
}

.search-input {
  width: 100%;
  height: 36px;
  padding: 0 2.5rem 0 2.25rem;
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-lg);
  background: var(--background-color);
  color: var(--text-color);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  background: var(--card-background);
  box-shadow: 0 0 0 2px rgba(var(--primary-color-rgb), 0.1);
}

.search-input:focus + .search-icon {
  color: var(--primary-color);
}

.clear-button {
  position: absolute;
  right: 0.5rem;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--border-radius-sm);
  font-size: 0.75rem;
  transition: all 0.2s ease;
  z-index: 2;
}

.clear-button:hover {
  color: var(--text-color);
  background: var(--clr-surface-tonal-a10);
}

/* Actions Section */
.actions-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.action-item {
  display: flex;
  align-items: center;
}

.action-button {
  position: relative;
  width: 36px;
  height: 36px;
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  background: var(--card-background);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.action-button:hover {
  background: var(--section-background);
  color: var(--text-color);
  border-color: var(--primary-color);
}

.action-button:active {
  transform: translateY(1px);
}

/* Notification Badge */
.notification-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: var(--danger-color);
  color: var(--clr-light-a0);
  font-size: 0.625rem;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.25rem;
  box-shadow: var(--shadow-sm);
  border: 2px solid var(--card-background);
}

/* Special button states */
.notification-button:has(.notification-badge):hover {
  border-color: var(--danger-color);
}

.theme-button:hover {
  background: var(--clr-primary-a0);
  color: var(--clr-light-a0);
  border-color: var(--clr-primary-a0);
}

/* Responsive Design */
@media (max-width: 768px) {
  .header-content {
    padding: 0.75rem 1rem;
    gap: 0.75rem;
  }
  
  .search-section {
    max-width: none;
    flex: 1;
  }
  
  .actions-section {
    gap: 0.375rem;
  }
  
  .action-button {
    width: 32px;
    height: 32px;
    font-size: 0.8125rem;
  }
}

@media (max-width: 640px) {
  .header-content {
    padding: 0.5rem;
    gap: 0.5rem;
  }
  
  .search-input {
    height: 32px;
    font-size: 0.8125rem;
  }
  
  .action-button:last-child {
    display: none;
  }
}

/* Focus and Accessibility */
.action-button:focus-visible,
.search-input:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* Loading States */
.search-input:disabled,
.action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-button:disabled:hover {
  background: var(--card-background);
  color: var(--text-secondary);
  transform: none;
  border-color: var(--card-border);
}
</style>