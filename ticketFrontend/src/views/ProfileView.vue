<script setup>
import { computed, onMounted, ref } from "vue";
import { getProfileDashboard } from "../api/ticketRushApi";

const dashboard = ref(null);
const loading = ref(true);
const error = ref("");

const profile = computed(() => dashboard.value?.profile || {});
const membership = computed(() => dashboard.value?.membership || null);
const stats = computed(() => dashboard.value?.stats || []);
const upcomingTickets = computed(() => dashboard.value?.upcomingTickets || []);
const recentActivity = computed(() => dashboard.value?.recentActivity || []);

const displayProfile = computed(() => ({
  name: profile.value.fullName || "TicketRush User",
  email: profile.value.email || "",
  phone: profile.value.phone || "Not updated",
  city: "Vietnam",
  membership: membership.value?.active ? membership.value.planName : "No active plan",
  memberSince: formatDate(profile.value.createdAt),
  renewalDate: formatDate(membership.value?.endAt),
  initials: initials(profile.value.fullName || profile.value.email || "TU"),
}));

const accountDetails = computed(() => [
  { label: "Primary email", value: displayProfile.value.email || "Not updated" },
  { label: "Phone number", value: displayProfile.value.phone },
  { label: "Role", value: profile.value.role || "CUSTOMER" },
  { label: "Current tier", value: displayProfile.value.membership },
]);

const membershipBenefits = computed(() => {
  if (!membership.value?.active) {
    return [
      "Choose a membership plan to unlock discounts",
      "Member vouchers become available after subscribing",
      "Your active tier will appear here automatically",
    ];
  }

  return [
    `${membership.value.discountPercent}% discount on eligible orders`,
    `Active until ${displayProfile.value.renewalDate}`,
    "Priority membership benefits are applied from the API",
  ];
});

async function loadDashboard() {
  loading.value = true;
  error.value = "";

  try {
    dashboard.value = await getProfileDashboard();
  } catch (err) {
    error.value = "Could not load profile data. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

function initials(value) {
  return value
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}

function formatDate(value) {
  if (!value) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en", {
    month: "long",
    day: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

onMounted(loadDashboard);
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 pb-20">
    <section class="relative overflow-hidden bg-brand-navy text-white">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(249,115,22,0.28),_transparent_30%),linear-gradient(180deg,_rgba(255,255,255,0.04),_transparent)]"></div>
      <div class="relative max-w-7xl mx-auto px-4 md:px-8 py-16">
        <div class="grid gap-8 lg:grid-cols-[1.2fr_0.85fr] items-start">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
              Profile
            </p>
            <h1 class="mt-3 text-4xl md:text-5xl font-black tracking-tight">
              Your TicketRush home base.
            </h1>
            <p class="mt-5 max-w-2xl text-base md:text-lg text-blue-100 leading-relaxed">
              Keep account details, membership perks, and upcoming plans in one
              place so every booking feels quick and organized.
            </p>
          </div>

          <div
            class="rounded-[2rem] border border-white/10 bg-white/10 p-6 backdrop-blur"
          >
            <div v-if="loading" class="text-blue-100 font-bold">
              Loading profile...
            </div>

            <div v-else-if="error" class="text-orange-100 font-bold">
              {{ error }}
            </div>

            <template v-else>
              <div class="flex items-center gap-4">
                <div
                  class="flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-orange text-2xl font-black text-white"
                >
                  {{ displayProfile.initials }}
                </div>
                <div>
                  <p class="text-2xl font-black">{{ displayProfile.name }}</p>
                  <p class="text-sm text-blue-100">{{ displayProfile.email }}</p>
                </div>
              </div>

              <div class="mt-6 grid gap-4 sm:grid-cols-2">
                <div class="rounded-2xl bg-white/10 p-4">
                  <p class="text-xs font-black uppercase tracking-[0.2em] text-white/70">
                    Current tier
                  </p>
                  <p class="mt-2 text-xl font-black text-brand-orange">
                    {{ displayProfile.membership }}
                  </p>
                </div>
                <div class="rounded-2xl bg-white/10 p-4">
                  <p class="text-xs font-black uppercase tracking-[0.2em] text-white/70">
                    Renewal date
                  </p>
                  <p class="mt-2 text-xl font-black">
                    {{ displayProfile.renewalDate }}
                  </p>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 md:px-8 mt-10">
      <div
        v-if="loading"
        class="rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
      >
        Loading profile dashboard...
      </div>

      <div
        v-else-if="error"
        class="rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-bold text-red-700"
      >
        {{ error }}
      </div>

      <template v-else>
        <div class="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="item in stats"
            :key="item.label"
            class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <p class="text-sm font-black uppercase tracking-[0.18em] text-slate-400">
              {{ item.label }}
            </p>
            <p class="mt-4 text-3xl font-black text-brand-navy dark:text-white">
              {{ item.value }}
            </p>
          </div>
        </div>

        <div class="grid gap-6 xl:grid-cols-[0.95fr_1.4fr] mt-10">
          <aside class="space-y-6">
            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
                    Account
                  </p>
                  <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
                    Personal details
                  </h2>
                </div>
                <span
                  class="rounded-full bg-slate-100 px-3 py-1 text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:bg-slate-900 dark:text-slate-300"
                >
                  Member since {{ displayProfile.memberSince }}
                </span>
              </div>
              <div class="mt-6 space-y-4">
                <div
                  v-for="detail in accountDetails"
                  :key="detail.label"
                  class="rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900"
                >
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">
                    {{ detail.label }}
                  </p>
                  <p class="mt-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                    {{ detail.value }}
                  </p>
                </div>
              </div>
            </div>

            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
                Membership perks
              </p>
              <ul class="mt-5 space-y-4">
                <li
                  v-for="benefit in membershipBenefits"
                  :key="benefit"
                  class="flex items-start gap-3 text-sm leading-7 text-slate-600 dark:text-slate-300"
                >
                  <span
                    class="mt-1 inline-flex h-6 w-6 items-center justify-center rounded-full bg-brand-orange/10 text-xs font-black text-brand-orange"
                  >
                    OK
                  </span>
                  <span>{{ benefit }}</span>
                </li>
              </ul>

              <router-link
                to="/membership"
                class="mt-6 inline-flex rounded-2xl bg-brand-orange px-5 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-orange-600"
              >
                Manage membership
              </router-link>
            </div>
          </aside>

          <div class="space-y-6">
            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                <div>
                  <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
                    Upcoming
                  </p>
                  <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
                    Your next tickets
                  </h2>
                </div>
                <router-link
                  to="/events"
                  class="text-sm font-black uppercase tracking-[0.18em] text-brand-orange hover:underline"
                >
                  Explore more movies
                </router-link>
              </div>

              <div
                v-if="upcomingTickets.length === 0"
                class="mt-6 rounded-3xl bg-slate-50 px-5 py-8 text-center font-bold text-slate-500 dark:bg-slate-900 dark:text-slate-300"
              >
                No upcoming tickets yet.
              </div>

              <div v-else class="mt-6 space-y-4">
                <article
                  v-for="ticket in upcomingTickets"
                  :key="ticket.orderId"
                  class="rounded-3xl border border-slate-200 p-5 transition hover:border-brand-orange/40 dark:border-slate-700"
                >
                  <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                      <h3 class="text-xl font-black text-brand-navy dark:text-white">
                        {{ ticket.title }}
                      </h3>
                      <p class="mt-2 text-sm leading-7 text-slate-500 dark:text-slate-300">
                        {{ ticket.date }} - {{ ticket.location }}
                      </p>
                    </div>
                    <div class="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold text-slate-600 dark:bg-slate-900 dark:text-slate-200">
                      Seat: {{ ticket.seat }}
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
                Activity
              </p>
              <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
                Recent account changes
              </h2>
              <div class="mt-6 space-y-4">
                <div
                  v-for="(item, index) in recentActivity"
                  :key="`${item.title}-${item.occurredAt}`"
                  class="flex items-start gap-4 rounded-3xl bg-slate-50 px-5 py-5 dark:bg-slate-900"
                >
                  <div
                    class="mt-1 flex h-10 w-10 items-center justify-center rounded-2xl bg-brand-navy text-sm font-black text-white"
                  >
                    {{ index + 1 }}
                  </div>
                  <div>
                    <p class="text-sm font-bold text-slate-800 dark:text-white">
                      {{ item.title }}
                    </p>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-300">
                      {{ item.time }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>
