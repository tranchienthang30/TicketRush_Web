<script setup>
import { onMounted, ref } from "vue";
import EventCard from "@/components/EventCard.vue";
import * as eventApi from "@/api/event.api";

const featuredEvents = ref([]);

onMounted(async () => {
  try {
    const response = await eventApi.getPublicEvents();
    featuredEvents.value = response.data.slice(0, 8);
  } catch {
    featuredEvents.value = [];
  }
});
</script>

<template>
  <div class="max-w-7xl mx-auto py-10 px-4 md:px-8">
    <section class="w-full h-64 md:h-96 bg-brand-navy rounded-3xl mb-16 flex items-center justify-center overflow-hidden relative shadow-2xl">
      <div class="text-center z-10 px-4">
        <h2 class="text-4xl md:text-7xl font-black text-white mb-6 uppercase tracking-tighter italic">
          STAR<span class="text-brand-orange">LIGHT</span> EVENTS
        </h2>
        <p class="text-blue-100 mb-8 text-lg font-medium">Book your next extraordinary experience</p>
        <router-link to="/events" class="inline-block bg-brand-orange hover:scale-110 transition-transform px-10 py-4 rounded-full text-white font-black shadow-lg uppercase tracking-widest text-sm">
          Explore Now
        </router-link>
      </div>
      <div class="absolute inset-0 bg-gradient-to-r from-brand-navy via-transparent to-brand-navy opacity-50"></div>
      <img src="https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80" class="absolute inset-0 w-full h-full object-cover opacity-30" alt="Banner background" />
    </section>

    <section class="mb-20">
      <div class="flex justify-between items-end mb-8 border-b border-gray-100 dark:border-slate-800 pb-4">
        <div>
          <h3 class="text-3xl font-black text-brand-navy dark:text-white tracking-tight uppercase">
            Featured Events
          </h3>
          <div class="h-1.5 w-20 bg-brand-orange mt-2 rounded-full"></div>
        </div>
        <router-link to="/events" class="text-brand-orange font-bold hover:underline flex items-center gap-2 group">
          View all <span class="group-hover:translate-x-1 transition-transform">-></span>
        </router-link>
      </div>

      <p v-if="featuredEvents.length === 0" class="text-slate-500 dark:text-slate-400">
        No published events yet.
      </p>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <EventCard v-for="event in featuredEvents" :key="event.id" :event="event" />
      </div>
    </section>
  </div>
</template>
