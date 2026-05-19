import apiClient from "./client";

export function getAdminDashboard() {
  return apiClient.get("/api/admin/dashboard");
}

export function getAdminUsers() {
  return apiClient.get("/api/admin/users");
}

export function updateAdminUserRole(userId, role) {
  return apiClient.put(`/api/admin/users/${userId}/role`, { role });
}

export function deleteAdminUser(userId) {
  return apiClient.delete(`/api/admin/users/${userId}`);
}

export function getAdminSystem() {
  return apiClient.get("/api/admin/system");
}
