<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import * as eventApi from "@/api/event.api";

const events = ref([]);
const loading = ref(false);
const error = ref("");

const totalEvents = computed(() => events.value.length);
const publishedEvents = computed(() => events.value.filter((event) => event.status === "PUBLISHED").length);
const totalSeats = computed(() =>
  events.value.reduce((sum, event) => sum + Number(event.totalSeats || 0), 0),
);

onMounted(async () => {
  loading.value = true;
  try {
    const response = await eventApi.getMyEvents();
    events.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load provider dashboard.";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Provider</p>
          <h1 class="mt-2 text-3xl font-black text-brand-navy dark:text-white">Dashboard</h1>
          <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-500 dark:text-slate-300">
            Quick overview of your created events. Revenue and booking trends can be added here later.
          </p>
        </div>
        <RouterLink to="/create-event" class="rounded-xl bg-brand-orange px-5 py-3 text-sm font-black text-white hover:bg-orange-600">
          Creating
        </RouterLink>
      </div>

      <p v-if="error" class="mt-6 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-700">
        {{ error }}
      </p>

      <div class="mt-8 grid gap-4 md:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Total Events</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">{{ loading ? "..." : totalEvents }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Published</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">{{ loading ? "..." : publishedEvents }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">Configured Seats</p>
          <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">{{ loading ? "..." : totalSeats }}</p>
        </div>
      </div>

      <div class="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-xl font-black text-brand-navy dark:text-white">Recent events</h2>
          <RouterLink to="/my-events" class="text-sm font-black text-brand-orange hover:underline">
            Managements
          </RouterLink>
        </div>
        <div v-if="loading" class="mt-5 text-sm font-bold text-slate-500 dark:text-slate-300">
          Loading events...
        </div>
        <div v-else-if="events.length === 0" class="mt-5 text-sm font-bold text-slate-500 dark:text-slate-300">
          You have not created any events yet.
        </div>
        <div v-else class="mt-5 divide-y divide-slate-100 dark:divide-slate-700">
          <div
            v-for="event in events.slice(0, 5)"
            :key="event.id"
            class="flex flex-col gap-2 py-4 md:flex-row md:items-center md:justify-between"
          >
            <div>
              <p class="font-black text-slate-900 dark:text-white">{{ event.title }}</p>
              <p class="text-sm text-slate-500 dark:text-slate-300">{{ event.locationName || event.city || "Location not set" }}</p>
            </div>
            <span class="w-fit rounded-full bg-orange-50 px-3 py-1 text-xs font-black uppercase tracking-[0.15em] text-brand-orange">
              {{ event.status }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
