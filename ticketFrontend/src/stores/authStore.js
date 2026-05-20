import { defineStore } from "pinia";
import { computed, ref } from "vue";
import * as authApi from "../api/auth.api";

export const useAuthStore = defineStore("auth", () => {
  const user = ref(null);
  const loading = ref(false);
  const error = ref("");
  const sessionNotice = ref("");
  const hasCheckedAuth = ref(false);
  let sessionTimerId;
  const notifiedThresholds = new Set();

  const isLoggedIn = computed(() => Boolean(user.value));
  const isAuthenticated = isLoggedIn;
  const isProvider = computed(() => ["PROVIDER", "ADMIN"].includes(user.value?.role));
  const isCustomer = computed(() => user.value?.role === "CUSTOMER");

  async function registerUser(payload) {
    return runAuthRequest(() => authApi.register(payload));
  }

  async function loginUser(payload) {
    return runAuthRequest(() => authApi.login(payload));
  }

  async function checkAuth() {
    loading.value = true;
    error.value = "";
    try {
      let response;
      try {
        response = await authApi.getCurrentUser();
      } catch (err) {
        if (err.response?.status !== 401) {
          throw err;
        }
        await authApi.refresh();
        response = await authApi.getCurrentUser();
      }
      user.value = response.data;
      scheduleCustomerSessionWarnings();
      return response.data;
    } catch {
      user.value = null;
      clearSessionTimers();
      return null;
    } finally {
      hasCheckedAuth.value = true;
      loading.value = false;
    }
  }

  async function fetchCurrentUser() {
    const currentUser = await checkAuth();
    if (!currentUser) {
      error.value = "Your session has expired. Please sign in again.";
      throw new Error(error.value);
    }
    return currentUser;
  }

  async function logoutUser() {
    try {
      await authApi.logout();
    } finally {
      clearSession();
    }
  }

  function clearSession() {
    user.value = null;
    hasCheckedAuth.value = true;
    sessionNotice.value = "";
    clearSessionTimers();
  }

  function setUser(nextUser) {
    user.value = nextUser;
    hasCheckedAuth.value = true;
    scheduleCustomerSessionWarnings();
  }

  async function runAuthRequest(requester) {
    loading.value = true;
    error.value = "";
    try {
      const response = await requester();
      user.value = response.data;
      hasCheckedAuth.value = true;
      scheduleCustomerSessionWarnings();
      return response.data;
    } catch (err) {
      error.value = getErrorMessage(err, "Authentication failed.");
      throw err;
    } finally {
      loading.value = false;
    }
  }

  return {
    user,
    loading,
    error,
    sessionNotice,
    hasCheckedAuth,
    isLoggedIn,
    isAuthenticated,
    isProvider,
    isCustomer,
    registerUser,
    loginUser,
    checkAuth,
    fetchCurrentUser,
    logoutUser,
    clearSession,
    setUser,
  };

  function scheduleCustomerSessionWarnings() {
    clearSessionTimers();
    sessionNotice.value = "";
    notifiedThresholds.clear();

    if (!user.value || user.value.role !== "CUSTOMER" || !user.value.sessionExpiresAt) {
      return;
    }

    sessionTimerId = window.setInterval(async () => {
      const expiresAt = new Date(user.value?.sessionExpiresAt).getTime();
      const remainingMs = expiresAt - Date.now();

      if (remainingMs <= 0) {
        sessionNotice.value = "Your customer session has expired. Please sign in again.";
        await logoutUser();
        window.location.assign("/login?reason=session-expired");
        return;
      }

      const remainingMinutes = Math.ceil(remainingMs / 60000);
      for (const threshold of [10, 5]) {
        if (remainingMinutes <= threshold && !notifiedThresholds.has(threshold)) {
          notifiedThresholds.add(threshold);
          sessionNotice.value = `For security, customer sessions expire automatically. About ${remainingMinutes} minute${remainingMinutes === 1 ? "" : "s"} remaining.`;
          break;
        }
      }
    }, 1000);
  }

  function clearSessionTimers() {
    if (sessionTimerId) {
      window.clearInterval(sessionTimerId);
      sessionTimerId = undefined;
    }
    notifiedThresholds.clear();
  }
});

function getErrorMessage(err, fallback) {
  return (
    err.response?.data?.message ||
    Object.values(err.response?.data?.details || {})[0] ||
    fallback
  );
}
