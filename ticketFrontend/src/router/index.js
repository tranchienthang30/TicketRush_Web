import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '/src/views/HomeView.vue' // 1. Nhập căn phòng HomeView vào
import EventsView from '../views/EventsView.vue'
import HelpCenterView from '../views/HelpCenterView.vue'
import BookView from '../views/BookView.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/events', name: 'events', component: EventsView },
    {path: '/help', name: 'help', component: HelpCenterView },
    {path: '/booking', name: 'booking', component: BookView } // Thêm dòng này
  ],
})

export default router