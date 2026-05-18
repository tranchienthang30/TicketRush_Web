<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { getCategories, getEventBySlug } from "../api/ticketRushApi";

const route = useRoute();

const eventDetail = ref(null);
const categories = ref([]);
const loading = ref(true);
const error = ref("");

const fallbackImage =
  "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1600&q=80";

const categoryName = computed(() => {
  const categoryId = Number(eventDetail.value?.categoryId);
  return categories.value.find((category) => Number(category.id) === categoryId)?.name || "Event";
});

const heroImage = computed(() => eventDetail.value?.bannerUrl || fallbackImage);

const listingLabel = computed(() => {
  const listingType = String(eventDetail.value?.listingType || "").trim().toUpperCase().replace(/[\s-]+/g, "_");

  if (listingType === "UPCOMING") {
    return "Upcoming";
  }
  if (listingType === "SPECIAL") {
    return "Special";
  }
  return "Now Showing";
});

const durationText = computed(() => formatDuration(eventDetail.value?.durationMinutes));

const priceText = computed(() => {
  const price = Number(eventDetail.value?.minPrice);

  if (!Number.isFinite(price) || price <= 0) {
    return "Free";
  }

  return `from ${price.toLocaleString("vi-VN")} VND`;
});

const canBook = computed(() => {
  const event = eventDetail.value;
  if (!event) return false;

  const listingType = String(event.listingType || "").toUpperCase();
  const now = Date.now();
  const saleStart = event.saleStartTime ? new Date(event.saleStartTime).getTime() : null;
  const saleEnd = event.saleEndTime ? new Date(event.saleEndTime).getTime() : null;

  return listingType !== "UPCOMING" && (!saleStart || saleStart <= now) && (!saleEnd || saleEnd >= now);
});

const bookingRoute = computed(() => ({
  path: "/booking",
  query: { eventId: eventDetail.value?.id },
}));

const keyFacts = computed(() => [
  { label: "Category", value: categoryName.value },
  { label: "Genre", value: eventDetail.value?.genre },
  { label: "Country", value: eventDetail.value?.country },
  { label: "Duration", value: durationText.value },
  { label: "Section", value: listingLabel.value },
  { label: "Price", value: priceText.value },
].filter((item) => item.value));

const peopleFacts = computed(() => [
  { label: "Author / Creator", value: eventDetail.value?.authorName },
  { label: "Director", value: eventDetail.value?.directorName },
  { label: "Cast / Speakers", value: eventDetail.value?.castMembers },
  { label: "Performers", value: eventDetail.value?.performerNames },
  { label: "Singers", value: eventDetail.value?.singerNames },
].filter((item) => item.value));

onMounted(loadEvent);

async function loadEvent() {
  loading.value = true;
  error.value = "";

  try {
    const slug = String(route.params.slug || "");
    const [eventResponse, categoryResponse] = await Promise.all([
      getEventBySlug(slug),
      getCategories().catch(() => []),
    ]);
    eventDetail.value = eventResponse;
    categories.value = categoryResponse;
  } catch (err) {
    error.value = err.response?.data?.message || "Could not load this event.";
  } finally {
    loading.value = false;
  }
}

function formatDateTime(value) {
  if (!value) return "TBA";

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatDuration(value) {
  const minutes = Number(value);

  if (!Number.isFinite(minutes) || minutes <= 0) {
    return "TBA";
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
}
</script>

<template>
  <main class="min-h-screen bg-brand-light pb-20 dark:bg-slate-950">
    <section v-if="loading" class="mx-auto max-w-5xl px-4 py-20">
      <div class="rounded-3xl border border-slate-200 bg-white p-8 text-center font-black text-slate-500 shadow-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
        Loading event details...
      </div>
    </section>

    <section v-else-if="error" class="mx-auto max-w-5xl px-4 py-20">
      <div class="rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-black text-red-700">
        {{ error }}
      </div>
    </section>

    <template v-else-if="eventDetail">
      <section class="relative overflow-hidden bg-brand-navy text-white">
        <img :src="heroImage" alt="" class="absolute inset-0 h-full w-full object-cover opacity-35 blur-[2px]" />
        <div class="absolute inset-0 bg-gradient-to-r from-brand-navy via-brand-navy/85 to-brand-navy/35"></div>

        <div class="relative mx-auto grid max-w-7xl grid-cols-1 gap-10 px-4 py-14 md:px-8 lg:grid-cols-[420px_1fr] lg:py-20">
          <div class="overflow-hidden rounded-3xl border border-white/10 bg-white/10 shadow-2xl">
            <img :src="heroImage" :alt="eventDetail.title" class="h-[520px] w-full object-cover" />
          </div>

          <div class="flex flex-col justify-center">
            <div class="mb-5 flex flex-wrap gap-3">
              <span class="rounded-full bg-brand-orange px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-white">
                {{ categoryName }}
              </span>
              <span class="rounded-full bg-white/15 px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-white ring-1 ring-white/20">
                {{ listingLabel }}
              </span>
            </div>

            <h1 class="mb-5 text-4xl font-black leading-tight tracking-tight md:text-6xl">
              {{ eventDetail.title }}
            </h1>

            <p class="mb-8 max-w-3xl text-lg leading-8 text-blue-50">
              {{ eventDetail.description || "Full details for this event will be updated soon." }}
            </p>

            <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div class="rounded-2xl bg-white/10 p-5 ring-1 ring-white/15">
                <p class="text-xs font-black uppercase tracking-[0.18em] text-white/55">Start time</p>
                <p class="mt-2 text-xl font-black">{{ formatDateTime(eventDetail.startTime) }}</p>
              </div>
              <div class="rounded-2xl bg-white/10 p-5 ring-1 ring-white/15">
                <p class="text-xs font-black uppercase tracking-[0.18em] text-white/55">Venue</p>
                <p class="mt-2 text-xl font-black">{{ eventDetail.locationName || "Venue TBA" }}</p>
                <p class="mt-1 text-sm font-bold text-white/70">{{ eventDetail.city }}</p>
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-4">
              <RouterLink
                v-if="canBook"
                :to="bookingRoute"
                class="rounded-2xl bg-brand-orange px-8 py-4 text-sm font-black uppercase tracking-[0.16em] text-white shadow-xl transition hover:bg-orange-600"
              >
                Buy ticket
              </RouterLink>
              <span
                v-else
                class="rounded-2xl bg-white/15 px-8 py-4 text-sm font-black uppercase tracking-[0.16em] text-white ring-1 ring-white/20"
              >
                Coming soon
              </span>
              <RouterLink to="/events" class="font-black text-white/80 hover:text-white">
                Back to events
              </RouterLink>
            </div>
          </div>
        </div>
      </section>

      <section class="mx-auto grid max-w-7xl grid-cols-1 gap-8 px-4 py-12 md:px-8 lg:grid-cols-[1fr_380px]">
        <div class="space-y-8">
          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800 md:p-8">
            <p class="mb-3 text-xs font-black uppercase tracking-[0.22em] text-brand-orange">About this event</p>
            <h2 class="mb-4 text-3xl font-black text-brand-navy dark:text-white">Full information</h2>
            <p class="text-lg leading-8 text-slate-600 dark:text-slate-300">
              {{ eventDetail.description || "The provider has not added a detailed description yet." }}
            </p>
          </article>

          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800 md:p-8">
            <p class="mb-3 text-xs font-black uppercase tracking-[0.22em] text-brand-orange">Credits</p>
            <h2 class="mb-6 text-3xl font-black text-brand-navy dark:text-white">People involved</h2>
            <div v-if="peopleFacts.length" class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div
                v-for="fact in peopleFacts"
                :key="fact.label"
                class="rounded-2xl bg-slate-50 p-5 dark:bg-slate-900"
              >
                <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">{{ fact.label }}</p>
                <p class="mt-2 text-base font-black leading-7 text-slate-800 dark:text-white">{{ fact.value }}</p>
              </div>
            </div>
            <p v-else class="text-slate-500 dark:text-slate-300">
              Credits will be updated by the provider soon.
            </p>
          </article>
        </div>

        <aside class="space-y-6">
          <div class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <h2 class="mb-5 text-2xl font-black text-brand-navy dark:text-white">Event details</h2>
            <dl class="space-y-4">
              <div
                v-for="fact in keyFacts"
                :key="fact.label"
                class="flex items-start justify-between gap-4 border-b border-slate-100 pb-3 last:border-b-0 dark:border-slate-700"
              >
                <dt class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">{{ fact.label }}</dt>
                <dd class="max-w-[62%] text-right font-black text-slate-800 dark:text-white">{{ fact.value }}</dd>
              </div>
            </dl>
          </div>

          <div class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <h2 class="mb-5 text-2xl font-black text-brand-navy dark:text-white">Venue</h2>
            <p class="text-lg font-black text-slate-800 dark:text-white">{{ eventDetail.locationName || "Venue TBA" }}</p>
            <p class="mt-2 text-slate-500 dark:text-slate-300">{{ eventDetail.address || "Address will be updated soon." }}</p>
            <p class="mt-1 text-slate-500 dark:text-slate-300">{{ eventDetail.city }}</p>
          </div>
        </aside>
      </section>
    </template>
  </main>
</template>
