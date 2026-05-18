import apiClient from "./client";

export function getWorkspace() {
  return apiClient.get("/api/provider/seatsio/workspace");
}

export function ensureWorkspace() {
  return apiClient.post("/api/provider/seatsio/workspace");
}

export function createChart(payload) {
  return apiClient.post("/api/provider/seatsio/charts", payload);
}

export function createSeatsioEvent(payload) {
  return apiClient.post("/api/provider/seatsio/events", payload);
}
