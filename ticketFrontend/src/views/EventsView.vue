<script setup>
import { onMounted, ref } from "vue";
import { getGroupedEvents } from "../api/ticketRushApi";
import EventCard from "../components/EventCard.vue";

const categories = ref([]);
const loading = ref(true);
const error = ref("");

async function loadEvents() {
  loading.value = true;
  error.value = "";

  try {
    categories.value = await getGroupedEvents(8);
  } catch (err) {
    error.value = "Could not load movies. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadEvents);
</script>

<template>
  <div class="bg-brand-light dark:bg-slate-900 min-h-screen pb-20">
    <div class="bg-brand-navy py-16 px-4 text-center text-white mb-12 shadow-inner">
      <h1 class="text-4xl md:text-5xl text-brand-orange mb-4 uppercase tracking-tighter font-black">
        Now Showing
      </h1>
      <p class="text-blue-100 max-w-2xl mx-auto text-lg">
        Browse movie genres, compare showtimes, and pick your next cinema night.
      </p>
    </div>

    <div class="max-w-7xl mx-auto px-4 md:px-8">
      <div
        v-if="loading"
        class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
      >
        Loading movies...
      </div>

      <div
        v-else-if="error"
        class="bg-red-50 border border-red-100 text-red-700 rounded-3xl p-8 text-center font-bold"
      >
        {{ error }}
      </div>

      <div
        v-else-if="categories.length === 0"
        class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
      >
        No published movies are available yet.
      </div>

      <template v-else>
        <section v-for="category in categories" :key="category.id" class="mb-20">
          <div class="mb-8 border-b border-gray-200 dark:border-slate-800 pb-4">
            <div class="flex items-center gap-3 mb-2">
              <h2 class="text-3xl font-black text-brand-navy dark:text-white uppercase tracking-tight">
                {{ category.name }}
              </h2>
              <span class="bg-brand-orange/10 text-brand-orange text-sm font-bold px-3 py-1 rounded-full border border-brand-orange/20">
                {{ category.events.length }} Movies
              </span>
            </div>
            <p class="text-gray-500 dark:text-gray-400 italic">
              {{ category.description }}
            </p>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
            <EventCard
              v-for="event in category.events"
              :key="event.id"
              :event="event"
            />
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
