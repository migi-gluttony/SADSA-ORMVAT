import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import AuthService from './services/AuthService'

// Import PrimeVue components
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Checkbox from 'primevue/checkbox'
import Dropdown from 'primevue/dropdown'
import Toast from 'primevue/toast'
import ProgressBar from 'primevue/progressbar'
import Tooltip from 'primevue/tooltip'
import ConfirmDialog from 'primevue/confirmdialog'
import Dialog from 'primevue/dialog'

// Import PrimeFlex (responsive CSS utilities)
import 'primeflex/primeflex.css'

// Import PrimeIcons
import 'primeicons/primeicons.css'

// Create app instance
const app = createApp(App)

// Configure PrimeVue with dark mode support
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.dark-mode', // CSS class for dark mode
    }
  }
  
})

// Add global properties
app.config.globalProperties.$axios = axios

// Configure axios baseURL for API
axios.defaults.baseURL = import.meta.env.VITE_API_URL || '/api'

// Use PrimeVue services
app.use(ToastService)
app.use(ConfirmationService)

// Register PrimeVue components
app.component('Button', Button)
app.component('InputText', InputText)
app.component('Password', Password)
app.component('Checkbox', Checkbox)
app.component('Dropdown', Dropdown)
app.component('Toast', Toast)
app.component('ProgressBar', ProgressBar)
app.component('ConfirmDialog', ConfirmDialog)
app.component('Dialog', Dialog)

// Register directives
app.directive('tooltip', Tooltip)

// Use router
app.use(router)

// Initialize theme based on saved preference or system preference
const initTheme = () => {
  const savedTheme = localStorage.getItem('app-theme-preference')
  
  if (savedTheme === 'dark') {
    document.documentElement.classList.add('dark-mode')
  } else if (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    // If no saved preference, use system preference
    document.documentElement.classList.add('dark-mode')
  }
}

// Check token on page load
const checkAuthOnLoad = () => {
  // Check if token is expired and handle logout
  AuthService.checkTokenAndLogout()
}

// Initialize app theme
initTheme()

// Check authentication
checkAuthOnLoad()

// Add event listener for when the page is refreshed or loaded
window.addEventListener('load', checkAuthOnLoad)

// Mount the app
app.mount('#app')