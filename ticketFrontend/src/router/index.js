import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '/src/views/HomeView.vue' // 1. Nhập căn phòng HomeView vào
import EventsView from '../views/EventsView.vue'
import HelpCenterView from '../views/HelpCenterView.vue'
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import OAuthSuccessView from "../views/OAuthSuccessView.vue";
import CreateEventView from '@/views/CreateEventView.vue';
import MyEventsView from '@/views/MyEventsView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/events', name: 'events', component: EventsView },
    {path: '/help', name: 'help', component: HelpCenterView },
    {
    path: "/login",
    name: "login",
    component: LoginView,
  },
  {
    path: "/register",
    name: "register",
    component: RegisterView,
  },
  {
    path: "/oauth-success",
    name: "oauth-success",
    component: OAuthSuccessView,
  },
   {path: '/create-event', name: 'create-event', component: CreateEventView },
    {path: '/my-events', name: 'my-events', component: MyEventsView },
  ],
})

export default router