import apiClient from "./client";

export function register(payload) {
  return apiClient.post("/auth/register", payload);
}

export function login(payload) {
  return apiClient.post("/auth/login", payload);
}

export function getCurrentUser() {
  return apiClient.get("/auth/me");
}

export function loginWithGoogle() {
  window.location.href = "http://localhost:8080/oauth2/authorization/google";
}