<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

const props = defineProps({
  event: {
    type: Object,
    required: true,
  },
});

const router = useRouter();

const fallbackImages = [
  "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=700&q=80",
  "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=700&q=80",
];

const imageSrc = computed(() => {
  if (props.event.image) {
    return props.event.image;
  }

  const seed = String(props.event.id || props.event.title || "");
  const index = [...seed].reduce((sum, char) => sum + char.charCodeAt(0), 0) % fallbackImages.length;
  return fallbackImages[index];
});

const durationText = computed(() => {
  const duration =
    props.event.durationMinutes ?? props.event.duration ?? props.event.runtime ?? props.event.length;

  if (!duration) return "TBA";

  const minutes = Number(duration);
  if (!Number.isFinite(minutes) || minutes <= 0) {
    return String(duration);
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours > 0 && remainingMinutes > 0) {
    return `${hours}h ${remainingMinutes}m`;
  }
  if (hours > 0) {
    return `${hours}h`;
  }
  return `${remainingMinutes}m`;
});

const canBook = computed(() => {
  if (typeof props.event.bookable === "boolean") {
    return props.event.bookable;
  }

  const tag = String(props.event.tag || "").trim().toUpperCase();
  return !["COMING SOON", "BOOKING CLOSED", "SOLD OUT"].includes(tag);
});

const unavailableLabel = computed(() => {
  const tag = String(props.event.tag || "").trim().toUpperCase();
  if (tag === "BOOKING CLOSED") return "BOOKING CLOSED";
  if (tag === "SOLD OUT") return "SOLD OUT";
  return "COMING SOON";
});

const detailPath = computed(() => (props.event.slug ? `/events/${props.event.slug}` : "/events"));

function openDetail() {
  router.push(detailPath.value);
}

function openDetailFromKeyboard(event) {
  if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    openDetail();
  }
}
</script>

<template>
  <div
    class="bg-white dark:bg-slate-800 rounded-2xl overflow-hidden shadow-md hover:-translate-y-2 transition-transform duration-300 group cursor-pointer border border-gray-100 dark:border-slate-700"
    role="link"
    tabindex="0"
    @click="openDetail"
    @keydown="openDetailFromKeyboard"
  >
    <div class="relative h-64 overflow-hidden bg-slate-200 flex items-center justify-center text-gray-400">
      <img
        :src="imageSrc"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        alt="Event cover"
      />

      <!-- left tag (e.g., HOT, P) -->
      <div
        v-if="event.tag"
        class="absolute top-3 left-3 bg-brand-orange text-white text-xs font-black px-3 py-1.5 rounded-lg shadow-sm uppercase tracking-wider"
      >
        {{ event.tag }}
      </div>

      <!-- age / rating badge -->
      <div v-if="event.rating" class="absolute top-3 right-3 bg-white/90 text-brand-navy text-xs font-bold px-2 py-1 rounded-md shadow-sm">
        {{ event.rating }}
      </div>
    </div>

    <div class="p-4 md:p-5">
      <h4 class="font-bold text-lg mb-1 dark:text-white group-hover:text-brand-orange transition line-clamp-2">
        {{ event.title }}
      </h4>

      <div class="text-sm text-gray-500 dark:text-slate-400 mb-3 flex items-center gap-3">
        <span class="inline-block">Category: {{ event.category || 'Event' }}</span>
        <span class="inline-block">/</span>
        <span class="inline-block">Duration: {{ durationText }}</span>
      </div>

      <div class="flex items-center justify-between">
        <div class="text-brand-orange font-black text-lg">{{ event.price || '' }}</div>

        <router-link
          v-if="canBook"
          :to="`/booking?eventId=${event.id}`"
          class="bg-brand-navy text-white px-4 py-2 rounded-lg hover:bg-brand-orange transition-all font-bold shadow-md active:scale-95"
          @click.stop
        >
          BUY TICKET
        </router-link>
        <span
          v-else
          class="bg-slate-200 text-slate-500 px-4 py-2 rounded-lg font-bold cursor-not-allowed dark:bg-slate-700 dark:text-slate-300"
        >
          {{ unavailableLabel }}
        </span>
      </div>
    </div>
  </div>
</template>
