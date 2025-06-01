<template>
  <div class="dossier-list-router">
    <!-- Agent Antenne View -->
    <AntenneListView v-if="userRole === 'AGENT_ANTENNE'" />
    
    <!-- Agent GUC View -->
    <GucListView v-else-if="userRole === 'AGENT_GUC'" />
    
    <!-- Agent Commission Terrain View -->
    <CommissionListView v-else-if="userRole === 'AGENT_COMMISSION_TERRAIN'" />
    
    <!-- Admin View (uses GUC view with additional permissions) -->
    <GucListView v-else-if="userRole === 'ADMIN'" />
    
    <!-- Fallback/Error -->
    <div v-else class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Accès non autorisé</h3>
      <p>Votre rôle ({{ userRole }}) ne permet pas l'accès à cette fonctionnalité.</p>
      <Button label="Retour" icon="pi pi-arrow-left" @click="goBack" class="p-button-outlined" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AuthService from '@/services/AuthService';

// Import role-specific components
import AntenneListView from '@/components/agent_antenne/dossier_list/AntenneListView.vue';
import GucListView from '@/components/agent_guc/dossier_list/GucListView.vue';
import CommissionListView from '@/components/agent_guc/dossier_list/GucListView.vue';

// PrimeVue components
import Button from 'primevue/button';

const router = useRouter();
const userRole = ref('');

onMounted(() => {
  const user = AuthService.getCurrentUser();
  if (!user) {
    router.push('/login');
    return;
  }
  
  userRole.value = user.role;
  console.log('DossierListView loaded for role:', userRole.value);
});

function goBack() {
  router.go(-1);
}
</script>

<style scoped>
.dossier-list-router {
  width: 100%;
  height: 100%;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  gap: 1.5rem;
  color: var(--text-secondary);
  background: var(--card-background);
  border: 1px solid var(--card-border);
  border-radius: var(--border-radius-md);
  margin: 2rem;
  min-height: 400px;
}

.error-container i {
  font-size: 3rem;
  color: var(--danger-color);
}

.error-container h3 {
  color: var(--text-color);
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.error-container p {
  color: var(--text-secondary);
  margin: 0;
  text-align: center;
  line-height: 1.6;
}
</style>