import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/authStore'
import './assets/css/main.css' 

const savedTheme = localStorage.getItem('theme')
const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
const shouldUseDark = savedTheme ? savedTheme === 'dark' : prefersDark
document.documentElement.classList.toggle('dark', shouldUseDark)
document.body?.classList.toggle('dark', shouldUseDark)
document.documentElement.style.colorScheme = shouldUseDark ? 'dark' : 'light'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

useAuthStore(pinia).checkAuth().finally(() => {
  app.use(router)
  app.mount('#app')
})
