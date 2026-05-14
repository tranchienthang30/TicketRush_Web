import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import EventsView from "../views/EventsView.vue";
import HelpCenterView from "../views/HelpCenterView.vue";
import BookView from "../views/BookView.vue";
import CheckoutView from "../views/CheckoutView.vue";
import MyTicketsView from "../views/MyTicketsView.vue";
import MembershipView from "../views/MembershipView.vue";
import ProfileView from "../views/ProfileView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "home", component: HomeView },
    { path: "/events", name: "events", component: EventsView },
    { path: "/help", name: "help", component: HelpCenterView },
    { path: "/booking", name: "booking", component: BookView },
    { path: "/checkout", name: "checkout", component: CheckoutView },
    { path: "/my-tickets", name: "my-tickets", component: MyTicketsView },
    { path: "/membership", name: "membership", component: MembershipView },
    { path: "/profile", name: "profile", component: ProfileView },
  ],
});

export default router;
