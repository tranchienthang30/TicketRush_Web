<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getAdminDashboard } from "@/api/admin.api";

const dashboard = ref(null);
const loading = ref(true);
const error = ref("");

const cards = computed(() => [
  {
    label: "Users",
    value: formatNumber(dashboard.value?.totalUsers),
    note: "Registered customer, provider, and admin accounts.",
  },
  {
    label: "Events",
    value: formatNumber(dashboard.value?.totalEvents),
    note: "All events created across providers.",
  },
  {
    label: "Platform revenue",
    value: formatMoney(dashboard.value?.platformRevenue),
    note: `${formatPercent(dashboard.value?.platformFeeRate)} fee collected from sold tickets.`,
  },
]);

onMounted(loadDashboard);

async function loadDashboard() {
  loading.value = true;
  error.value = "";
  try {
    const response = await getAdminDashboard();
    dashboard.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load admin dashboard.";
  } finally {
    loading.value = false;
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0));
}

function formatMoney(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
}

function formatPercent(value) {
  return `${Math.round(Number(value || 0) * 100)}%`;
}
</script>

<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Admin</p>
      <div class="mt-2 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 class="text-3xl font-black text-brand-navy dark:text-white">Dashboard</h1>
          <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-500 dark:text-slate-300">
            Platform overview for accounts, events, ticket sales, and the 5% fee collected by TicketRush.
          </p>
        </div>
        <button class="rounded-xl bg-brand-navy px-4 py-3 text-sm font-black text-white" @click="loadDashboard">
          Refresh
        </button>
      </div>

      <p v-if="loading" class="mt-8 rounded-2xl bg-white p-6 font-bold text-slate-500 shadow-sm dark:bg-slate-800 dark:text-slate-300">
        Loading dashboard...
      </p>
      <p v-else-if="error" class="mt-8 rounded-2xl border border-red-100 bg-red-50 p-6 font-bold text-red-700">
        {{ error }}
      </p>

      <template v-else-if="dashboard">
        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div v-for="card in cards" :key="card.label" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">{{ card.label }}</p>
            <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">{{ card.value }}</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">{{ card.note }}</p>
          </div>
        </div>

        <div class="mt-6 grid gap-4 lg:grid-cols-2">
          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-xl font-black text-slate-900 dark:text-white">Users by role</h2>
              <RouterLink to="/admin/users" class="text-sm font-black text-brand-orange">Open users</RouterLink>
            </div>
            <div class="space-y-3">
              <div v-for="role in dashboard.usersByRole" :key="role.role" class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
                <span class="font-black text-slate-700 dark:text-slate-200">{{ role.role }}</span>
                <span class="font-black text-brand-navy dark:text-white">{{ formatNumber(role.count) }}</span>
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <h2 class="mb-4 text-xl font-black text-slate-900 dark:text-white">Event status</h2>
            <div class="space-y-3">
              <div v-for="status in dashboard.eventsByStatus" :key="status.status" class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
                <span class="font-black text-slate-700 dark:text-slate-200">{{ status.status }}</span>
                <span class="font-black text-brand-navy dark:text-white">{{ formatNumber(status.count) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <h2 class="text-xl font-black text-slate-900 dark:text-white">Sales summary</h2>
          <div class="mt-4 grid gap-4 md:grid-cols-3">
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-900">
              <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Tickets sold</p>
              <p class="mt-2 text-2xl font-black text-brand-navy dark:text-white">{{ formatNumber(dashboard.totalTicketsSold) }}</p>
            </div>
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-900">
              <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Gross ticket revenue</p>
              <p class="mt-2 text-2xl font-black text-brand-navy dark:text-white">{{ formatMoney(dashboard.grossTicketRevenue) }}</p>
            </div>
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-900">
              <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">TicketRush fee</p>
              <p class="mt-2 text-2xl font-black text-brand-orange">{{ formatMoney(dashboard.platformRevenue) }}</p>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>
