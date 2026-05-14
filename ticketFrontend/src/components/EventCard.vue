<script setup>
import { computed } from "vue";

const props = defineProps({
  event: {
    type: Object,
    required: true,
  },
});

const image = computed(() => props.event.bannerUrl || props.event.image);
const date = computed(() => {
  const raw = props.event.startTime || props.event.date;
  if (!raw) return "Date TBA";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(raw));
});
const location = computed(() => props.event.locationName || props.event.location || props.event.city || "Location TBA");
const price = computed(() => {
  if (props.event.price) return props.event.price;
  if (props.event.minPrice === null || props.event.minPrice === undefined) return "Price TBA";
  const value = Number(props.event.minPrice);
  if (value === 0) return "Free";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
});
</script>

<template>
  <div class="bg-white dark:bg-slate-800 rounded-2xl overflow-hidden shadow-lg hover:-translate-y-2 transition-transform duration-300 group cursor-pointer flex flex-col border border-gray-100 dark:border-slate-700">
    <div class="relative h-48 overflow-hidden bg-slate-200 flex items-center justify-center text-gray-400">
      <img
        v-if="image"
        :src="image"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        alt="Event cover"
      />
      <span v-else class="italic px-4 text-center">[ Image: {{ event.title }} ]</span>

      <div v-if="event.tag || event.status" class="absolute top-3 left-3 bg-brand-orange text-white text-[10px] font-black px-3 py-1.5 rounded-lg shadow-sm uppercase tracking-wider">
        {{ event.tag || event.status }}
      </div>
    </div>

    <div class="p-5 flex flex-col flex-grow">
      <h4 class="font-bold text-lg mb-2 dark:text-white group-hover:text-brand-orange transition line-clamp-2">
        {{ event.title }}
      </h4>

      <div class="text-sm text-gray-500 dark:text-slate-400 mb-4 space-y-1">
        <p>{{ date }}</p>
        <p>{{ location }}</p>
      </div>

      <div class="mt-auto flex justify-between items-center pt-4 border-t border-gray-50 dark:border-slate-700">
        <span class="text-brand-orange font-black text-xl">{{ price }}</span>
        <button class="bg-brand-navy text-white px-4 py-2 rounded-lg hover:bg-brand-orange transition-all font-bold shadow-md active:scale-95">
          Booking
        </button>
      </div>
    </div>
  </div>
</template>
