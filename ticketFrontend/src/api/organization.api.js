import apiClient from "./client";

export function registerOrganization(payload) {
  return apiClient.post("/api/organizations/register", payload);
}

export function verifyOrganization(token) {
  return apiClient.post("/api/organizations/verify", null, {
    params: { token },
    skipAuthRedirect: true,
    skipAuthRefresh: true,
  });
}

export function getMyOrganizations() {
  return apiClient.get("/api/organizations/me");
}
