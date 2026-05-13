<script setup>
import { onMounted, ref } from "vue";
import { getHome } from "../api/ticketRushApi";
import EventCard from "../components/EventCard.vue";

const featuredCategories = ref([]);
const loading = ref(true);
const error = ref("");

async function loadHome() {
  loading.value = true;
  error.value = "";

  try {
    const data = await getHome();
    featuredCategories.value = data.featuredCategories || [];
  } catch (err) {
    error.value = "Could not load featured events. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadHome);
</script>

<template>
  <div class="max-w-7xl mx-auto py-10 px-4 md:px-8">
    <section class="w-full h-64 md:h-96 bg-brand-navy rounded-3xl mb-16 flex items-center justify-center overflow-hidden relative shadow-2xl">
      <div class="text-center z-10 px-4">
        <h2 class="text-4xl md:text-7xl font-black text-white mb-6 uppercase tracking-tighter italic">
          STAR<span class="text-brand-orange">LIGHT</span> EVENTS
        </h2>
        <p class="text-blue-100 mb-8 text-lg font-medium">
          Book your next extraordinary experience
        </p>
        <router-link
          to="/events"
          class="inline-flex bg-brand-orange hover:scale-110 transition-transform px-10 py-4 rounded-full text-white font-black shadow-lg uppercase tracking-widest text-sm"
        >
          Explore Now
        </router-link>
      </div>
      <div class="absolute inset-0 bg-gradient-to-r from-brand-navy via-transparent to-brand-navy opacity-50"></div>
      <img
        src="https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80"
        class="absolute inset-0 w-full h-full object-cover opacity-30"
        alt="Banner background"
      />
    </section>

    <div
      v-if="loading"
      class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
    >
      Loading featured events...
    </div>

    <div
      v-else-if="error"
      class="bg-red-50 border border-red-100 text-red-700 rounded-3xl p-8 text-center font-bold"
    >
      {{ error }}
    </div>

    <div
      v-else-if="featuredCategories.length === 0"
      class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
    >
      No featured events are available yet.
    </div>

    <template v-else>
      <section v-for="category in featuredCategories" :key="category.id" class="mb-20">
        <div class="flex justify-between items-end mb-8 border-b border-gray-100 dark:border-slate-800 pb-4">
          <div>
            <h3 class="text-3xl font-black text-brand-navy dark:text-white tracking-tight uppercase">
              {{ category.name }}
            </h3>
            <div class="h-1.5 w-20 bg-brand-orange mt-2 rounded-full"></div>
          </div>
          <router-link to="/events" class="text-brand-orange font-bold hover:underline flex items-center gap-2 group">
            View all <span class="group-hover:translate-x-1 transition-transform">-&gt;</span>
          </router-link>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          <EventCard
            v-for="event in category.events"
            :key="event.id"
            :event="event"
          />
        </div>
      </section>
    </template>
  </div>
</template>
