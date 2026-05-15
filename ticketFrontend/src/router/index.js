import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "home", component: () => import("@/views/HomeView.vue") },
    { path: "/events", name: "events", component: () => import("@/views/EventsView.vue") },
    { path: "/help", name: "help", component: () => import("@/views/HelpCenterView.vue") },
    { path: "/booking", name: "booking", component: () => import("@/views/BookView.vue"), meta: { requiresAuth: true } },
    { path: "/checkout", name: "checkout", component: () => import("@/views/CheckoutView.vue"), meta: { requiresAuth: true } },
    { path: "/checkout/success", name: "checkout-success", component: () => import("@/views/CheckoutSuccessView.vue") },
    { path: "/checkout/cancel", name: "checkout-cancel", component: () => import("@/views/CheckoutCancelView.vue") },
    { path: "/my-tickets", name: "my-tickets", component: () => import("@/views/MyTicketsView.vue"), meta: { requiresAuth: true } },
    { path: "/profile", name: "profile", component: () => import("@/views/ProfileView.vue"), meta: { requiresAuth: true } },
    { path: "/account", redirect: "/profile" },
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue") },
    { path: "/register", name: "register", component: () => import("@/views/RegisterView.vue") },
    { path: "/forgot-password", name: "forgot-password", component: () => import("@/views/ForgotPasswordView.vue") },
    { path: "/reset-password", name: "reset-password", component: () => import("@/views/ResetPasswordView.vue") },
    { path: "/verify-email", name: "verify-email", component: () => import("@/views/VerifyEmailView.vue") },
    { path: "/provider/register", name: "register-provider", component: () => import("@/views/RegisterOrganizationView.vue"), meta: { requiresAuth: true } },
    { path: "/provider/verify", name: "verify-provider", component: () => import("@/views/VerifyOrganizationView.vue") },
    { path: "/organization/register", redirect: "/provider/register" },
    { path: "/organization/verify", redirect: (to) => ({ path: "/provider/verify", query: to.query }) },
    { path: "/oauth2/callback", name: "oauth-callback", component: () => import("@/views/OAuthSuccessView.vue") },
    { path: "/create-movie", name: "create-movie", component: () => import("@/views/CreateEventView.vue"), meta: { requiresAuth: true } },
    { path: "/cinemas-management", name: "cinemas-management", component: () => import("@/views/MyEventsView.vue"), meta: { requiresAuth: true } },
    { path: "/create-event", redirect: "/create-movie" },
    { path: "/my-events", redirect: "/cinemas-management" },
    { path: "/membership", redirect: "/profile" },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();

  if (!authStore.hasCheckedAuth) {
    await authStore.checkAuth();
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if ((to.name === "login" || to.name === "register") && authStore.isLoggedIn) {
    return { name: "home" };
  }

  return true;
});

export default router;
