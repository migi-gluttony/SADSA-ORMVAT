<template>
  <div class="user-info-header">
    <div class="search-container">
      <i class="pi pi-search"></i>
      <InputText 
        v-model="searchValue" 
        :placeholder="searchPlaceholder" 
        class="search-input"
        @input="handleSearch"
      />
      <button 
        class="clear-button" 
        v-if="searchValue" 
        @click="clearSearch"
      >
        <i class="pi pi-times"></i>
      </button>
    </div>
    
    <div class="user-container">
      <div class="notifications">
        <button class="notification-button">
          <i class="pi pi-bell"></i>
          <span class="notification-badge" v-if="notificationCount > 0">{{ notificationCount }}</span>
        </button>
      </div>
      
      <div class="user-profile">
        <div class="avatar">
          <img v-if="userAvatar" :src="userAvatar" alt="User Avatar" />
          <div v-else class="avatar-placeholder">
            {{ getEmailInitial() }}
          </div>
        </div>
        <div class="user-info">
          <div class="user-name-with-role">
            <span class="user-full-name">{{ userName }}</span>
            <span class="user-role-badge">{{ userRole }}</span>
          </div>
          <div class="user-email">{{ userEmail }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AuthService from '@/services/AuthService';
import InputText from 'primevue/inputtext';

const router = useRouter();


// Props
const props = defineProps({
  searchPlaceholder: {
    type: String,
    default: 'Rechercher...'
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
const emit = defineEmits(['search']);

// Component state
const searchValue = ref(props.initialSearchValue);
const currentUser = ref(AuthService.getCurrentUser());

// Computed properties
const userName = computed(() => {
  if (!currentUser.value) return 'Utilisateur';
  return `${currentUser.value?.prenom || ''} ${currentUser.value?.nom || ''}`.trim();
});

// Add this watch to log the user data whenever it changes
watch(currentUser, (newValue) => {
  console.log('Current user from auth service:', newValue);
}, { immediate: true }); // The immediate option logs the initial value


const userEmail = computed(() => {
  return currentUser.value?.email || '';
});

const userAvatar = computed(() => {
  return null; // Set to image URL if you have user avatars
});

const userRole = computed(() => {
  return currentUser.value?.role || 'Utilisateur';
});

// Methods
function getEmailInitial() {
  if (!currentUser.value?.email) return '';
  
  // Get the first character of the email and convert to uppercase
  return currentUser.value.email.charAt(0).toUpperCase();
}

function handleSearch() {
  emit('search', searchValue.value);
}

function clearSearch() {
  searchValue.value = '';
  handleSearch();
}

// Watch props
watch(() => props.initialSearchValue, (newValue) => {
  searchValue.value = newValue;
});

// Update user when auth state changes
onMounted(() => {
  // Listen for auth state changes to update the user data
  window.addEventListener('auth-state-changed', () => {
    currentUser.value = AuthService.getCurrentUser();
  });
});
</script>

<style scoped>
.user-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background-color: #ffffff;
  margin-bottom: 1.5rem;
  height: 64px;
  width: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  transition: background-color 0.3s ease;
}

.dark-mode .user-info-header {
  background-color: #121212;
}

.search-container {
  flex: 1;
  max-width: 85%;
  margin-right: 1rem;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  height: 36px;
  border-radius: 18px;
  background-color: #f5f5f5;
  border: 1px solid #eaeaea;
  color: var(--text-color);
  font-size: 0.9rem;
  padding: 0 2.5rem 0 2.5rem;
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.search-input::placeholder {
  color: #a0a0a0;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: #ffffff;
}

.search-container .pi-search {
  position: absolute;
  left: 0.8rem;
  color: var(--primary-color);
  font-size: 0.9rem;
}

/* Clear button styles */
.clear-button {
  position: absolute;
  right: 0.8rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-color);
  cursor: pointer;
  font-size: 0.9rem;
}

.dark-mode .clear-button {
  color: rgba(255, 255, 255, 0.6);
}

/* Dark mode overrides */
.dark-mode .search-input {
  background-color: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
}

.dark-mode .search-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.dark-mode .search-input:focus {
  background-color: rgba(255, 255, 255, 0.15);
  border-color: transparent;
}

.dark-mode .search-container .pi-search {
  color: rgba(255, 255, 255, 0.6);
}

.user-container {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  position: relative;
}

.notifications {
  position: relative;
}

.notification-button {
  background: none;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  color: var(--primary-color);
  transition: background-color 0.2s;
}

.notification-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.notification-button i {
  font-size: 1.1rem;
}

.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #ef4444;
  color: white;
  font-size: 0.7rem;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* Dark mode overrides for notification button */
.dark-mode .notification-button {
  color: #fff;
}

.dark-mode .notification-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem;
  border-radius: 20px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background-color: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name-with-role {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.user-full-name {
  font-weight: 500;
  font-size: 0.9rem;
  color: var(--text-color);
}

.user-role-badge {
  background-color: var(--primary-color);
  color: white;
  font-size: 0.7rem;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.user-email {
  font-size: 0.75rem;
  color: var(--text-secondary-color, #666);
}

/* Dark mode overrides for user info */
.dark-mode .user-full-name {
  color: #fff;
}

.dark-mode .user-email {
  color: rgba(255, 255, 255, 0.7);
}

/* Responsive styles */
@media (max-width: 768px) {
  .user-info-header {
    flex-direction: row;
    padding: 0.75rem 1rem;
  }
  
  .search-container {
    max-width: 50%;
  }
  
  .user-container {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .search-container {
    max-width: 70%;
  }
  
  .user-info {
    display: none;
  }
  
  .user-container {
    gap: 0.5rem;
  }
}
</style>