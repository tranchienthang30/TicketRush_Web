import apiClient, { API_BASE_URL } from "./client";

export function getCategories() {
  return apiClient.get("/api/categories");
}

export function getPublicEvents() {
  return apiClient.get("/api/events");
}

export function getEventBySlug(slug) {
  return apiClient.get(`/api/events/slug/${slug}`);
}

export function createEvent(payload) {
  return apiClient.post("/api/events", payload);
}

export function updateEvent(eventId, payload) {
  return apiClient.put(`/api/events/${eventId}`, payload);
}

export function getMyEvent(eventId) {
  return apiClient.get(`/api/events/my-events/${eventId}`);
}

export function getMyEventBookingSummary(eventId) {
  return apiClient.get(`/api/events/my-events/${eventId}/booking-summary`);
}

export async function uploadEventBanner(file) {
  const formData = new FormData();
  formData.append("file", file);
  const response = await apiClient.post("/api/uploads/event-banner", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  const url = response.data?.url || "";
  return {
    ...response,
    data: {
      ...response.data,
      url: url.startsWith("http") ? url : `${API_BASE_URL}${url}`,
    },
  };
}

export function getMyEvents() {
  return apiClient.get("/api/events/my-events");
}
