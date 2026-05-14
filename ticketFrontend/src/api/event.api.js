import apiClient from "./client";

export function getCategories() {
  return apiClient.get("/api/categories");
}

export function getPublicEvents() {
  return apiClient.get("/api/events");
}

export function createEvent(payload) {
  return apiClient.post("/api/events", payload);
}

export function getMyEvents() {
  return apiClient.get("/api/events/my-events");
}
