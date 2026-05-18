<script setup>
import { computed, onMounted, ref } from "vue";
import { getEvents } from "../api/ticketRushApi";
import EventCard from "../components/EventCard.vue";

const eventsPage = ref(null);
const eventsList = ref([]);
const loading = ref(true);
const error = ref("");
const selectedTab = ref("NOW_SHOWING");

const tabDefinitions = [
  {
    id: "NOW_SHOWING",
    label: "Now Showing",
    title: "Now Showing",
    description: "Events currently open for booking.",
  },
  {
    id: "UPCOMING",
    label: "Upcoming",
    title: "Upcoming",
    description: "Events opening for booking soon.",
  },
  {
    id: "SPECIAL",
    label: "Special",
    title: "Special Events",
    description: "Limited, VIP, or seasonal ticket drops.",
  },
];

async function loadEvents() {
  loading.value = true;
  error.value = "";

  try {
    eventsPage.value = await getEvents({ size: 50 });
    eventsList.value = eventsPage.value?.content || [];
  } catch (err) {
    error.value = "Could not load events. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

function normalizeListingType(event) {
  const explicitType = event.listingType || event.eventType || event.eventStatus;
  const normalized = String(explicitType || "").trim().toUpperCase().replace(/[\s-]+/g, "_");

  if (["NOW_SHOWING", "UPCOMING", "SPECIAL"].includes(normalized)) {
    return normalized;
  }

  const tag = String(event.tag || "").toLowerCase();
  if (["special", "vip", "limited"].some((item) => tag.includes(item))) {
    return "SPECIAL";
  }
  if (["coming soon", "coming", "preview", "soon"].some((item) => tag.includes(item))) {
    return "UPCOMING";
  }
  return "NOW_SHOWING";
}

function groupEventsByCategory(events) {
  const categoryMap = new Map();

  for (const event of events) {
    const categoryName = event.category || "Other";
    if (!categoryMap.has(categoryName)) {
      categoryMap.set(categoryName, []);
    }
    categoryMap.get(categoryName).push(event);
  }

  return Array.from(categoryMap.entries()).map(([name, events]) => ({
    name,
    events,
  }));
}

const groupedEvents = computed(() =>
  tabDefinitions.map((tab) => {
    const tabEvents = eventsList.value.filter((event) => normalizeListingType(event) === tab.id);
    return {
      ...tab,
      events: tabEvents,
      categories: groupEventsByCategory(tabEvents),
    };
  }),
);

const activeGroup = computed(
  () => groupedEvents.value.find((group) => group.id === selectedTab.value) || groupedEvents.value[0],
);

onMounted(loadEvents);
</script>

<template>
  <div class="bg-brand-light dark:bg-slate-900 min-h-screen pb-20">
    <div class="bg-brand-navy py-16 px-4 text-center text-white mb-12 shadow-inner">
      <h1 class="text-4xl md:text-5xl text-brand-orange mb-4 uppercase tracking-tighter font-black">
        Events
      </h1>
      <p class="text-blue-100 max-w-2xl mx-auto text-lg">
        Browse tickets by section, then by category: Music, Show, Concert, Cinema, Sport, Festival, and more.
      </p>
    </div>

    <div class="max-w-7xl mx-auto px-4 md:px-8">
      <div class="bg-white dark:bg-slate-800 rounded-3xl p-3 mb-8 flex flex-wrap gap-3 items-center justify-center">
        <button
          v-for="group in groupedEvents"
          :key="group.id"
          :class="[
            'px-5 py-2 rounded-full font-bold uppercase tracking-wide text-sm transition',
            selectedTab === group.id
              ? 'bg-brand-navy text-white'
              : 'bg-white text-brand-navy border border-gray-200 dark:bg-slate-900 dark:text-white dark:border-slate-700',
          ]"
          @click="selectedTab = group.id"
        >
          {{ group.label }} ({{ group.events.length }})
        </button>
      </div>

      <div
        v-if="loading"
        class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
      >
        Loading events...
      </div>

      <div
        v-else-if="error"
        class="bg-red-50 border border-red-100 text-red-700 rounded-3xl p-8 text-center font-bold"
      >
        {{ error }}
      </div>

      <div
        v-else-if="eventsList.length === 0"
        class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
      >
        No published events are available yet.
      </div>

      <template v-else>
        <section v-if="activeGroup.events.length" class="mb-16">
          <div class="mb-6 flex items-center justify-between">
            <div>
              <h2 class="text-3xl font-black text-brand-navy dark:text-white tracking-tight">{{ activeGroup.title }}</h2>
              <p class="text-gray-500 dark:text-gray-400">{{ activeGroup.description }}</p>
            </div>
            <div class="text-sm text-gray-600 dark:text-slate-300 font-bold">{{ activeGroup.events.length }} Events</div>
          </div>

          <div class="space-y-12">
            <section
              v-for="categoryGroup in activeGroup.categories"
              :key="categoryGroup.name"
            >
              <div class="mb-5 flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-700">
                <div>
                  <h3 class="text-2xl font-black uppercase tracking-tight text-brand-navy dark:text-white">
                    {{ categoryGroup.name }}
                  </h3>
                  <p class="text-sm font-bold text-slate-500 dark:text-slate-300">
                    {{ categoryGroup.events.length }} events
                  </p>
                </div>
                <span class="rounded-full bg-brand-orange/10 px-3 py-1 text-xs font-black uppercase tracking-[0.18em] text-brand-orange">
                  {{ categoryGroup.name }}
                </span>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
                <EventCard v-for="event in categoryGroup.events" :key="event.id" :event="event" />
              </div>
            </section>
          </div>
        </section>

        <div
          v-else
          class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-3xl p-8 text-center font-bold text-slate-500 dark:text-slate-300"
        >
          No events in this section yet.
        </div>
      </template>
    </div>
  </div>
</template>
