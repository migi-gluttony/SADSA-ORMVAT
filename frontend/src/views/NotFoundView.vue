<template>
    <div class="not-found">
      <div class="not-found-content">
        <div class="error-code">404</div>
        <h1>Page Not Found</h1>
        <p>The page you are looking for doesn't exist or has been moved.</p>
        <Button label="Go to Dashboard" icon="pi pi-home" @click="navigateToDashboard" class="mt-4" />
      </div>
    </div>
  </template>
  
  <script setup>
  import { useRouter } from 'vue-router';
  import AuthService from '../services/AuthService';
  
  const router = useRouter();
  
  const navigateToDashboard = () => {
    if (AuthService.isAuthenticated()) {
      router.push('/dashboard');
    } else {
      router.push('/login');
    }
  };
  </script>
  
  <style scoped>
  .not-found {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    text-align: center;
    padding: 2rem;
  }
  
  .not-found-content {
    max-width: 400px;
  }
  
  .error-code {
    font-size: 8rem;
    font-weight: 700;
    color: var(--primary-color);
    line-height: 1;
    margin-bottom: 1rem;
    opacity: 0.4;
  }
  
  h1 {
    font-size: 2rem;
    margin-bottom: 1rem;
  }
  
  p {
    color: var(--text-color-secondary);
    margin-bottom: 2rem;
  }
  </style>