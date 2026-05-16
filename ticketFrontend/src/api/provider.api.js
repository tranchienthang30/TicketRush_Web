import apiClient from "./client";

export function requestProviderAccess() {
  return apiClient.post("/api/providers/request");
}

export function getPendingProviderRequests() {
  return apiClient.get("/api/admin/provider-requests");
}

export function approveProviderRequest(userId) {
  return apiClient.post(`/api/admin/provider-requests/${userId}/approve`);
}

export function rejectProviderRequest(userId, reason = "") {
  return apiClient.post(`/api/admin/provider-requests/${userId}/reject`, { reason });
}
