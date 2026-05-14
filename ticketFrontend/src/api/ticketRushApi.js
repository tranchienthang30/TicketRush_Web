import apiClient from "./client";

export async function getHome() {
  const { data } = await apiClient.get("/home");
  return data;
}

export async function getGroupedEvents(limitPerCategory = 4) {
  const { data } = await apiClient.get("/events/grouped", {
    params: { limitPerCategory },
  });
  return data;
}

export async function getEvents(params = {}) {
  const { data } = await apiClient.get("/events", { params });
  return data;
}

export async function getBookingEvent(eventId) {
  const { data } = await apiClient.get(`/events/${eventId}/booking`);
  return data;
}

export async function getMembershipPlans() {
  const { data } = await apiClient.get("/membership/plans");
  return data;
}

export async function getCurrentMembership() {
  const { data } = await apiClient.get("/me/membership");
  return data;
}

export async function subscribeMembership(planId) {
  const { data } = await apiClient.post("/me/membership/subscribe", { planId });
  return data;
}

export async function cancelMembership() {
  const { data } = await apiClient.patch("/me/membership/cancel");
  return data;
}

export async function getProfile() {
  const { data } = await apiClient.get("/me");
  return data;
}

export async function getProfileDashboard() {
  const { data } = await apiClient.get("/me/dashboard");
  return data;
}

export async function getMyTickets(params = {}) {
  const { data } = await apiClient.get("/me/tickets", { params });
  return data;
}

export async function getTicketDetail(orderId) {
  const { data } = await apiClient.get(`/me/tickets/${orderId}`);
  return data;
}

export async function previewCheckout(payload) {
  const { data } = await apiClient.post("/me/checkout/preview", payload);
  return data;
}

export async function confirmCheckout(payload) {
  const { data } = await apiClient.post("/me/checkout/confirm", payload);
  return data;
}

export async function updateProfile(payload) {
  const { data } = await apiClient.put("/me", payload);
  return data;
}
