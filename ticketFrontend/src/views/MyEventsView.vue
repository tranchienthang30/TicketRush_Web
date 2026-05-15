<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import * as eventApi from "@/api/event.api";

const events = ref([]);
const loading = ref(false);
const error = ref("");

onMounted(async () => {
  loading.value = true;
  try {
    const response = await eventApi.getMyEvents();
    events.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load your movies.";
  } finally {
    loading.value = false;
  }
});

function formatDate(value) {
  return value
    ? new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value))
    : "TBA";
}

function formatDuration(minutes) {
  if (!minutes) return "TBA";
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  if (hours > 0 && remainingMinutes > 0) return `${hours}h ${remainingMinutes}m`;
  if (hours > 0) return `${hours}h`;
  return `${remainingMinutes}m`;
}

function formatListingType(type) {
  return String(type || "NOW_SHOWING").replaceAll("_", " ");
}
</script>

<template>
  <div class="max-w-6xl mx-auto p-6">
    <div class="flex items-center justify-between gap-4 mb-6">
      <h1 class="text-3xl font-black text-brand-navy dark:text-white">Cinemas Management</h1>
      <RouterLink to="/create-movie" class="rounded-xl bg-brand-orange px-5 py-3 font-black text-white hover:bg-orange-600">
        Create Movie
      </RouterLink>
    </div>

    <p v-if="loading" class="text-gray-500">Loading movies...</p>
    <p v-else-if="error" class="text-red-600">{{ error }}</p>
    <div v-else-if="events.length === 0" class="text-gray-500">
      You have not created any movies yet.
    </div>

    <div v-else class="grid gap-4">
      <div v-for="event in events" :key="event.id" class="border border-slate-200 dark:border-slate-700 rounded-2xl p-5 bg-white dark:bg-slate-800 flex flex-col md:flex-row md:justify-between md:items-center gap-4">
        <div>
          <div class="flex items-center gap-3 mb-2">
            <h2 class="text-xl font-black text-slate-900 dark:text-white">{{ event.title }}</h2>
            <span class="text-xs font-black bg-brand-orange/10 text-brand-orange px-2 py-1 rounded">{{ event.status }}</span>
          </div>
          <p class="text-gray-500 dark:text-slate-400">Start: {{ formatDate(event.startTime) }}</p>
          <p class="text-gray-500 dark:text-slate-400">Duration: {{ formatDuration(event.durationMinutes) }} | Section: {{ formatListingType(event.listingType) }}</p>
          <p class="text-gray-500 dark:text-slate-400">Seats: {{ event.totalSeats }} | Provider: {{ event.seatProvider }}</p>
        </div>
        <div class="flex gap-2">
          <RouterLink :to="`/events?event=${event.slug}`" class="bg-brand-navy text-white px-4 py-2 rounded-lg font-bold">View</RouterLink>
          <button class="bg-slate-200 text-slate-600 px-4 py-2 rounded-lg font-bold cursor-not-allowed" disabled>Edit later</button>
        </div>
      </div>
    </div>
  </div>
</template>
