<script setup>
import { computed, onMounted, ref } from "vue";
import EventCard from "@/components/EventCard.vue";
import * as eventApi from "@/api/event.api";

const events = ref([]);
const categories = ref([]);
const loading = ref(false);
const error = ref("");

const eventsByCategory = computed(() =>
  categories.value
    .map((category) => ({
      ...category,
      events: events.value.filter((event) => event.categoryId === category.id),
    }))
    .filter((category) => category.events.length > 0)
);

onMounted(async () => {
  loading.value = true;
  try {
    const [categoryResponse, eventResponse] = await Promise.all([
      eventApi.getCategories(),
      eventApi.getPublicEvents(),
    ]);
    categories.value = categoryResponse.data;
    events.value = eventResponse.data;
  } catch (err) {
    error.value = "Unable to load events.";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="bg-brand-light dark:bg-slate-900 min-h-screen pb-20">
    <div class="bg-brand-navy py-16 px-4 text-center text-white mb-12 shadow-inner">
      <h1 class="text-4xl md:text-5xl text-brand-orange mb-4 uppercase tracking-tighter font-black">
        Discover Events
      </h1>
      <p class="text-blue-100 max-w-2xl mx-auto text-lg">
        Explore a wide range of categories and find the perfect event for you.
      </p>
    </div>

    <div class="max-w-7xl mx-auto px-4 md:px-8">
      <p v-if="loading" class="text-center text-slate-500 dark:text-slate-400">Loading events...</p>
      <p v-else-if="error" class="text-center text-red-600">{{ error }}</p>
      <p v-else-if="events.length === 0" class="text-center text-slate-500 dark:text-slate-400">
        No published events yet.
      </p>

      <section v-for="category in eventsByCategory" :key="category.id" class="mb-20">
        <div class="mb-8 border-b border-gray-200 dark:border-slate-800 pb-4">
          <div class="flex items-center gap-3 mb-2">
            <h2 class="text-3xl font-black text-brand-navy dark:text-white uppercase tracking-tight">
              {{ category.name }}
            </h2>
            <span class="bg-brand-orange/10 text-brand-orange text-sm font-bold px-3 py-1 rounded-full border border-brand-orange/20">
              {{ category.events.length }} Events
            </span>
          </div>
          <p class="text-gray-500 dark:text-gray-400 italic">{{ category.description }}</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
          <EventCard v-for="event in category.events" :key="event.id" :event="event" />
        </div>
      </section>
    </div>
  </div>
</template>
