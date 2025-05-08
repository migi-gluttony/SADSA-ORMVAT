import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // In vite.config.js
server: {
  port: 5555,
  proxy: {
    '/api': {
      target: 'http://localhost:9999', // Change this to 9999
      changeOrigin: true,
      secure: false
    }
  }
},
})
