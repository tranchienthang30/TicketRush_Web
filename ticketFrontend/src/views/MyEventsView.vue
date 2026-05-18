<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import * as eventApi from "@/api/event.api";
import * as seatsioApi from "@/api/seatsio.api";

const events = ref([]);
const loading = ref(false);
const error = ref("");
const seatsioWorkspace = ref(null);
const seatsioError = ref("");
const selectedSeatsioEvent = ref(null);
let seatsioScriptPromise = null;
let seatsioManager = null;

onMounted(async () => {
  loading.value = true;
  try {
    const [response, workspaceResponse] = await Promise.all([
      eventApi.getMyEvents(),
      seatsioApi.getWorkspace().catch(() => ({ data: null })),
    ]);
    events.value = response.data;
    seatsioWorkspace.value = workspaceResponse.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load your events.";
  } finally {
    loading.value = false;
  }
});

const groupedEvents = computed(() => [
  { key: "live", title: "Live now", events: events.value.filter(isLiveEvent) },
  { key: "upcoming", title: "Upcoming", events: events.value.filter(isUpcomingEvent) },
  { key: "past", title: "Past", events: events.value.filter(isPastEvent) },
]);

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

function isLiveEvent(event) {
  const now = Date.now();
  const start = event.startTime ? new Date(event.startTime).getTime() : null;
  const end = event.endTime ? new Date(event.endTime).getTime() : null;
  return start && end && start <= now && end >= now;
}

function isUpcomingEvent(event) {
  return event.startTime && new Date(event.startTime).getTime() > Date.now();
}

function isPastEvent(event) {
  return event.endTime && new Date(event.endTime).getTime() < Date.now();
}

function canOpenBooking(event) {
  const listingType = String(event.listingType || "").toUpperCase();
  const now = Date.now();
  const saleStart = event.saleStartTime ? new Date(event.saleStartTime).getTime() : null;
  const saleEnd = event.saleEndTime ? new Date(event.saleEndTime).getTime() : null;
  return listingType !== "UPCOMING" && (!saleStart || saleStart <= now) && (!saleEnd || saleEnd >= now);
}

async function openSeatsioManager(event) {
  seatsioError.value = "";
  if (!event.externalSeatEventKey) {
    seatsioError.value = "This seats.io event does not have an event key yet.";
    return;
  }
  if (!seatsioWorkspace.value?.secretKey || seatsioWorkspace.value.workspaceKey !== event.externalSeatWorkspaceKey) {
    seatsioError.value = "This event was created with manual seats.io keys, so open it in seats.io directly.";
    return;
  }

  selectedSeatsioEvent.value = event;
  await nextTick();
  await loadSeatsioScript(seatsioWorkspace.value.cdnUrl);
  destroySeatsioManager();
  seatsioManager = new window.seatsio.EventManager({
    divId: "seatsio-event-manager",
    secretKey: seatsioWorkspace.value.secretKey,
    event: event.externalSeatEventKey,
    mode: "manageForSaleConfig",
  }).render();
}

function closeSeatsioManager() {
  destroySeatsioManager();
  selectedSeatsioEvent.value = null;
}

function destroySeatsioManager() {
  if (seatsioManager?.destroy) {
    seatsioManager.destroy();
  }
  seatsioManager = null;
}

function loadSeatsioScript(cdnUrl) {
  if (window.seatsio) return Promise.resolve();
  if (seatsioScriptPromise) return seatsioScriptPromise;

  seatsioScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = cdnUrl || "https://cdn-eu.seatsio.net/chart.js";
    script.async = true;
    script.onload = resolve;
    script.onerror = () => reject(new Error("Unable to load seats.io chart.js"));
    document.head.appendChild(script);
  });
  return seatsioScriptPromise;
}
</script>

<template>
  <div class="max-w-6xl mx-auto p-6">
    <div class="flex items-center justify-between gap-4 mb-6">
      <h1 class="text-3xl font-black text-brand-navy dark:text-white">Manage Events</h1>
      <RouterLink to="/create-event" class="rounded-xl bg-brand-orange px-5 py-3 font-black text-white hover:bg-orange-600">
        Create Event
      </RouterLink>
    </div>

    <p v-if="loading" class="text-gray-500">Loading events...</p>
    <p v-else-if="error" class="text-red-600">{{ error }}</p>
    <div v-else-if="events.length === 0" class="text-gray-500">
      You have not created any events yet.
    </div>
    <div v-else>
      <p v-if="seatsioError" class="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-700">{{ seatsioError }}</p>

      <div class="space-y-8">
        <section v-for="group in groupedEvents" :key="group.key" class="space-y-3">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-black text-slate-900 dark:text-white">{{ group.title }}</h2>
            <span class="text-sm font-black text-slate-500">{{ group.events.length }}</span>
          </div>

          <div v-if="group.events.length === 0" class="rounded-xl border border-dashed border-slate-200 p-5 text-sm font-bold text-slate-500 dark:border-slate-700">
            No events in this section.
          </div>

          <div v-else class="grid gap-4">
            <div v-for="event in group.events" :key="event.id" class="border border-slate-200 dark:border-slate-700 rounded-2xl p-5 bg-white dark:bg-slate-800 flex flex-col md:flex-row md:justify-between md:items-center gap-4">
              <div>
                <div class="flex flex-wrap items-center gap-3 mb-2">
                  <h3 class="text-xl font-black text-slate-900 dark:text-white">{{ event.title }}</h3>
                  <span class="text-xs font-black bg-brand-orange/10 text-brand-orange px-2 py-1 rounded">{{ event.status }}</span>
                  <span class="text-xs font-black bg-slate-100 text-slate-600 px-2 py-1 rounded dark:bg-slate-700 dark:text-slate-200">{{ event.seatProvider }}</span>
                </div>
                <p class="text-gray-500 dark:text-slate-400">Start: {{ formatDate(event.startTime) }}</p>
                <p class="text-gray-500 dark:text-slate-400">Duration: {{ formatDuration(event.durationMinutes) }} | Section: {{ formatListingType(event.listingType) }}</p>
                <p class="text-gray-500 dark:text-slate-400">Seats: {{ event.totalSeats }} | City: {{ event.city || "TBA" }}</p>
                <p v-if="event.seatProvider === 'SEATS_IO'" class="text-xs font-bold text-slate-400">
                  seats.io: {{ event.externalSeatChartKey || "no chart" }} / {{ event.externalSeatEventKey || "no event" }}
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <RouterLink :to="event.slug ? `/events/${event.slug}` : '/events'" class="bg-brand-navy text-white px-4 py-2 rounded-lg font-bold">View</RouterLink>
                <RouterLink
                  v-if="canOpenBooking(event)"
                  :to="`/booking?eventId=${event.id}`"
                  class="bg-brand-orange text-white px-4 py-2 rounded-lg font-bold"
                >
                  Booking
                </RouterLink>
                <button v-else class="bg-slate-100 text-slate-400 px-4 py-2 rounded-lg font-bold cursor-not-allowed" disabled>
                  Booking closed
                </button>
                <RouterLink :to="`/events/${event.id}/edit`" class="bg-slate-200 text-slate-700 px-4 py-2 rounded-lg font-bold">Edit</RouterLink>
                <button
                  v-if="event.seatProvider === 'SEATS_IO'"
                  class="bg-slate-200 text-slate-700 px-4 py-2 rounded-lg font-bold"
                  @click="openSeatsioManager(event)"
                >
                  Manage seats
                </button>
                <button v-else class="bg-slate-100 text-slate-400 px-4 py-2 rounded-lg font-bold cursor-not-allowed" disabled>Internal seats</button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div v-if="selectedSeatsioEvent" class="fixed inset-0 z-50 bg-slate-950/70 p-4">
      <div class="mx-auto flex h-full max-w-6xl flex-col rounded-2xl bg-white p-4 shadow-2xl dark:bg-slate-900">
        <div class="mb-3 flex items-center justify-between gap-4">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.18em] text-brand-orange">seats.io manager</p>
            <h2 class="text-xl font-black text-slate-900 dark:text-white">{{ selectedSeatsioEvent.title }}</h2>
          </div>
          <button class="rounded-xl bg-slate-100 px-4 py-2 font-black text-slate-700 dark:bg-slate-800 dark:text-slate-100" @click="closeSeatsioManager">
            Close
          </button>
        </div>
        <div id="seatsio-event-manager" class="min-h-0 flex-1 overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700"></div>
      </div>
    </div>
  </div>
</template>
