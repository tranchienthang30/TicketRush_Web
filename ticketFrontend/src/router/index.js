import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "home", component: () => import("@/views/HomeView.vue") },
    { path: "/events", name: "events", component: () => import("@/views/EventsView.vue") },
    { path: "/help", name: "help", component: () => import("@/views/HelpCenterView.vue") },
    { path: "/booking", name: "booking", component: () => import("@/views/BookView.vue") },
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue") },
    { path: "/register", name: "register", component: () => import("@/views/RegisterView.vue") },
    { path: "/forgot-password", name: "forgot-password", component: () => import("@/views/ForgotPasswordView.vue") },
    { path: "/reset-password", name: "reset-password", component: () => import("@/views/ResetPasswordView.vue") },
    { path: "/verify-email", name: "verify-email", component: () => import("@/views/VerifyEmailView.vue") },
    { path: "/organization/verify", name: "verify-organization", component: () => import("@/views/VerifyOrganizationView.vue") },
    {
      path: "/oauth2/callback",
      name: "oauth-callback",
      component: () => import("@/views/OAuthSuccessView.vue"),
    },
    {
      path: "/profile",
      name: "profile",
      component: () => import("@/views/UserProfileView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/organization/register",
      name: "register-organization",
      component: () => import("@/views/RegisterOrganizationView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/create-event",
      name: "create-event",
      component: () => import("@/views/CreateEventView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/my-events",
      name: "my-events",
      component: () => import("@/views/MyEventsView.vue"),
      meta: { requiresAuth: true },
    },
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
