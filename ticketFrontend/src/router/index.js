import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "home", component: () => import("@/views/HomeView.vue"), meta: { customerSurface: true } },
    { path: "/events", name: "events", component: () => import("@/views/EventsView.vue"), meta: { customerSurface: true } },
    { path: "/events/:slug", name: "event-detail", component: () => import("@/views/EventDetailView.vue") },
    { path: "/help", name: "help", component: () => import("@/views/HelpCenterView.vue") },
    { path: "/booking", name: "booking", component: () => import("@/views/BookView.vue"), meta: { requiresAuth: true, requiresCustomer: true } },
    { path: "/checkout", name: "checkout", component: () => import("@/views/CheckoutView.vue"), meta: { requiresAuth: true, requiresCustomer: true } },
    { path: "/checkout/success", name: "checkout-success", component: () => import("@/views/CheckoutSuccessView.vue") },
    { path: "/checkout/cancel", name: "checkout-cancel", component: () => import("@/views/CheckoutCancelView.vue") },
    { path: "/ticket-qr", name: "ticket-qr-payload", component: () => import("@/views/TicketQrPayloadView.vue") },
    { path: "/my-tickets", name: "my-tickets", component: () => import("@/views/MyTicketsView.vue"), meta: { requiresAuth: true, requiresCustomer: true } },
    { path: "/profile", name: "profile", component: () => import("@/views/ProfileView.vue"), meta: { requiresAuth: true } },
    { path: "/account", redirect: "/profile" },
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue") },
    { path: "/register", name: "register", component: () => import("@/views/RegisterView.vue") },
    { path: "/forgot-password", name: "forgot-password", component: () => import("@/views/ForgotPasswordView.vue") },
    { path: "/reset-password", name: "reset-password", component: () => import("@/views/ResetPasswordView.vue") },
    { path: "/verify-email", name: "verify-email", component: () => import("@/views/VerifyEmailView.vue") },
    { path: "/provider/register", redirect: "/profile" },
    { path: "/provider/verify", redirect: (to) => ({ path: "/verify-email", query: to.query }) },
    { path: "/organization/register", redirect: "/profile" },
    { path: "/organization/verify", redirect: (to) => ({ path: "/verify-email", query: to.query }) },
    { path: "/oauth2/callback", name: "oauth-callback", component: () => import("@/views/OAuthSuccessView.vue") },
    { path: "/provider/home", name: "provider-home", component: () => import("@/views/ProviderHomeView.vue"), meta: { requiresAuth: true, requiresProvider: true } },
    { path: "/provider/dashboard", redirect: "/provider/home" },
    { path: "/create-event", name: "create-event", component: () => import("@/views/CreateEventView.vue"), meta: { requiresAuth: true, requiresProvider: true } },
    { path: "/events/:id/edit", name: "edit-event", component: () => import("@/views/CreateEventView.vue"), meta: { requiresAuth: true, requiresProvider: true } },
    { path: "/my-events", name: "my-events", component: () => import("@/views/MyEventsView.vue"), meta: { requiresAuth: true, requiresProvider: true } },
    { path: "/admin/dashboard", name: "admin-dashboard", component: () => import("@/views/AdminDashboardView.vue"), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: "/admin/users", name: "admin-users", component: () => import("@/views/AdminUsersView.vue"), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: "/admin/system", name: "admin-system", component: () => import("@/views/AdminSystemView.vue"), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: "/admin/provider-requests", name: "admin-provider-requests", component: () => import("@/views/AdminProviderRequestsView.vue"), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: "/create-movie", redirect: "/create-event" },
    { path: "/cinemas-management", redirect: "/my-events" },
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

  if (to.meta.customerSurface && ["ADMIN", "PROVIDER"].includes(authStore.user?.role)) {
    return roleHome(authStore.user.role);
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== "ADMIN") {
    return { name: "home" };
  }

  if (to.meta.requiresProvider && authStore.user?.role !== "PROVIDER") {
    return { name: "profile", query: { provider: "required" } };
  }

  if (to.meta.requiresCustomer && authStore.user?.role !== "CUSTOMER") {
    return roleHome(authStore.user?.role);
  }

  if ((to.name === "login" || to.name === "register") && authStore.isLoggedIn) {
    return { name: "home" };
  }

  return true;
});

export default router;

function roleHome(role) {
  if (role === "ADMIN") {
    return { name: "admin-dashboard" };
  }
  if (role === "PROVIDER") {
    return { name: "provider-home" };
  }
  return { name: "home" };
}
