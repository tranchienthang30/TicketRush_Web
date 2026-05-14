import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest?._retry &&
      !originalRequest?.skipAuthRefresh &&
      !originalRequest?.url?.includes("/api/auth/login") &&
      !originalRequest?.url?.includes("/api/auth/register") &&
      !originalRequest?.url?.includes("/api/auth/refresh")
    ) {
      originalRequest._retry = true;

      try {
        await apiClient.post("/api/auth/refresh", null, {
          skipAuthRedirect: true,
          skipAuthRefresh: true,
        });
        return apiClient(originalRequest);
      } catch {
        // Fall through to the normal 401 redirect below.
      }
    }

    if (
      error.response?.status === 401 &&
      !originalRequest?.skipAuthRedirect &&
      !["/login", "/register"].includes(window.location.pathname)
    ) {
      const redirect = encodeURIComponent(window.location.pathname + window.location.search);
      window.location.assign(`/login?redirect=${redirect}`);
    }

    return Promise.reject(error);
  },
);

export { API_BASE_URL };
export default apiClient;
