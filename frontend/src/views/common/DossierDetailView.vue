<template>
  <div class="dossier-detail-router">
    <!-- Agent Antenne View -->
    <AntenneDetailView v-if="userRole === 'AGENT_ANTENNE'" />
    
    <!-- Agent GUC View -->
    <GucDetailView v-else-if="userRole === 'AGENT_GUC'" />
    
    <!-- Agent Commission Terrain View -->
    <CommissionDetailView v-else-if="userRole === 'AGENT_COMMISSION_TERRAIN'" />
    
    <!-- Admin View (uses GUC view with additional permissions) -->
    <GucDetailView v-else-if="userRole === 'ADMIN'" />
    
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
import AntenneDetailView from '@/components/agent_antenne/dossier_details/AntenneDetailView.vue';
import GucDetailView from '@/components/agent_guc/dossier_detail/GucDetailView.vue';
import CommissionDetailView from '@/components/agent_commission/dossier_detail/CommissionDetailView.vue';

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
  console.log('DossierDetailView loaded for role:', userRole.value);
});

function goBack() {
  router.go(-1);
}
</script>
