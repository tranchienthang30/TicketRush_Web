import apiClient from "./client";

export async function getHome() {
  const { data } = await apiClient.get("/api/home");
  return data;
}

export async function getGroupedEvents(limitPerCategory = 4) {
  const { data } = await apiClient.get("/api/events/grouped", {
    params: { limitPerCategory },
  });
  return data;
}

export async function getEvents(params = {}) {
  const { data } = await apiClient.get("/api/events", { params });
  return data;
}

export async function getEventBySlug(slug) {
  const { data } = await apiClient.get(`/api/events/slug/${slug}`);
  return data;
}

export async function getCategories() {
  const { data } = await apiClient.get("/api/categories");
  return data;
}

export async function getBookingEvent(eventId) {
  const { data } = await apiClient.get(`/api/events/${eventId}/booking`);
  return data;
}

export async function joinVirtualQueue(eventId) {
  const { data } = await apiClient.post(`/api/me/virtual-queue/events/${eventId}/join`);
  return data;
}

export async function getVirtualQueueStatus(eventId) {
  const { data } = await apiClient.get(`/api/me/virtual-queue/events/${eventId}/status`);
  return data;
}

export async function leaveVirtualQueue(eventId) {
  const { data } = await apiClient.delete(`/api/me/virtual-queue/events/${eventId}`);
  return data;
}

export async function lockBookingSeats(payload) {
  const { data } = await apiClient.post("/api/me/checkout/seats/lock", payload);
  return data;
}

export async function releaseBookingSeats(payload) {
  const { data } = await apiClient.post("/api/me/checkout/seats/release", payload);
  return data;
}

export async function getMembershipPlans() {
  const { data } = await apiClient.get("/api/membership/plans");
  return data;
}

export async function getCurrentMembership() {
  const { data } = await apiClient.get("/api/me/membership");
  return data;
}

export async function subscribeMembership(planId) {
  const { data } = await apiClient.post("/api/me/membership/subscribe", { planId });
  return data;
}

export async function cancelMembership() {
  const { data } = await apiClient.patch("/api/me/membership/cancel");
  return data;
}

export async function getProfile() {
  const { data } = await apiClient.get("/api/me");
  return data;
}

export async function getProfileDashboard() {
  const { data } = await apiClient.get("/api/me/dashboard");
  return data;
}

export async function getMyTickets(params = {}) {
  const { data } = await apiClient.get("/api/me/tickets", { params });
  return data;
}

export async function getTicketDetail(orderId) {
  const { data } = await apiClient.get(`/api/me/tickets/${orderId}`);
  return data;
}

export async function previewCheckout(payload) {
  const { data } = await apiClient.post("/api/me/checkout/preview", payload);
  return data;
}

export async function confirmCheckout(payload) {
  const { data } = await apiClient.post("/api/me/checkout/confirm", payload);
  return data;
}

export async function completePayOSCheckout(orderCode) {
  const { data } = await apiClient.post("/api/me/checkout/payos/complete", null, {
    params: { orderCode },
  });
  return data;
}

export async function cancelPayOSCheckout(orderCode) {
  const { data } = await apiClient.post("/api/me/checkout/payos/cancel", null, {
    params: { orderCode },
  });
  return data;
}

export async function updateProfile(payload) {
  const { data } = await apiClient.put("/api/me", payload);
  return data;
}
