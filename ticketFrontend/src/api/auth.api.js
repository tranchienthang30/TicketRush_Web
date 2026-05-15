import apiClient, { API_BASE_URL } from "./client";

export function register(payload) {
  return apiClient.post("/api/auth/register", payload);
}

export function login(payload) {
  return apiClient.post("/api/auth/login", payload);
}

export function getCurrentUser() {
  return apiClient.get("/api/users/me", { skipAuthRedirect: true });
}

export function logout() {
  return apiClient.post("/api/auth/logout");
}

export function refresh() {
  return apiClient.post("/api/auth/refresh", null, {
    skipAuthRedirect: true,
    skipAuthRefresh: true,
  });
}

export function forgotPassword(payload) {
  return apiClient.post("/api/auth/forgot-password", payload, {
    skipAuthRedirect: true,
    skipAuthRefresh: true,
  });
}

export function resetPassword(payload) {
  return apiClient.post("/api/auth/reset-password", payload, {
    skipAuthRedirect: true,
    skipAuthRefresh: true,
  });
}

export function verifyEmail(token) {
  return apiClient.post("/api/auth/verify-email", null, {
    params: { token },
    skipAuthRedirect: true,
    skipAuthRefresh: true,
  });
}

export function resendVerificationEmail() {
  return apiClient.post("/api/auth/resend-verification-email");
}

export function loginWithGoogle() {
  window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
}
