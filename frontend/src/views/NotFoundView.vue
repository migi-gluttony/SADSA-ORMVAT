<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <h2>Page non trouvée</h2>
      <p>La page que vous recherchez n'existe pas ou a été déplacée.</p>
      <Button 
        label="Retour à l'accueil" 
        icon="pi pi-home" 
        @click="goHome" 
        class="p-button-primary"
      />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/');
};
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  text-align: center;
}

.not-found-content {
  max-width: 500px;
  padding: 2rem;
}

h1 {
  font-size: 6rem;
  margin: 0;
  color: var(--primary-color);
}

h2 {
  margin-bottom: 1rem;
}

p {
  margin-bottom: 2rem;
}
</style>