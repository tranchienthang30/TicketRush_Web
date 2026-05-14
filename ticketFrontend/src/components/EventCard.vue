<script setup>
import { computed } from "vue";

const props = defineProps({
  event: {
    type: Object,
    required: true,
  },
});

const fallbackImages = [
  "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=700&q=80",
];

const imageSrc = computed(() => {
  if (props.event.image) {
    return props.event.image;
  }

  const seed = String(props.event.id || props.event.title || "");
  const index = [...seed].reduce((sum, char) => sum + char.charCodeAt(0), 0) % fallbackImages.length;
  return fallbackImages[index];
});
</script>

<template>
  <div class="bg-white dark:bg-slate-800 rounded-2xl overflow-hidden shadow-lg hover:-translate-y-2 transition-transform duration-300 group cursor-pointer flex flex-col border border-gray-100 dark:border-slate-700">
    <div class="relative h-48 overflow-hidden bg-slate-200 flex items-center justify-center text-gray-400">
      <img
        :src="imageSrc"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        alt="Event cover"
      />

      <div
        v-if="event.tag"
        class="absolute top-3 left-3 bg-brand-orange text-white text-[10px] font-black px-3 py-1.5 rounded-lg shadow-sm uppercase tracking-wider"
      >
        {{ event.tag }}
      </div>
    </div>

    <div class="p-5 flex flex-col flex-grow">
      <h4 class="font-bold text-lg mb-2 dark:text-white group-hover:text-brand-orange transition line-clamp-2">
        {{ event.title }}
      </h4>

      <div class="text-sm text-gray-500 dark:text-slate-400 mb-4 space-y-1">
        <p>Date: {{ event.date }}</p>
        <p>Location: {{ event.location }}</p>
      </div>

      <div class="mt-auto flex justify-between items-center pt-4 border-t border-gray-50 dark:border-slate-700">
        <span class="text-brand-orange font-black text-xl">{{ event.price }}</span>
        <router-link
          :to="`/booking?eventId=${event.id}`"
          class="bg-brand-navy text-white px-4 py-2 rounded-lg hover:bg-brand-orange transition-all font-bold shadow-md active:scale-95"
        >
          Booking
        </router-link>
      </div>
    </div>
  </div>
</template>
