<script setup>
import { computed, onMounted, ref } from "vue";
import { getAdminSystem } from "@/api/admin.api";

const system = ref(null);
const loading = ref(true);
const error = ref("");

const cards = computed(() => [
  { label: "Spring Boot health", value: system.value?.healthStatus || "UNKNOWN", note: "Actuator health endpoint" },
  { label: "Uptime", value: system.value?.uptime || "0s", note: "Process uptime from Micrometer" },
  { label: "HTTP requests", value: formatNumber(system.value?.httpRequestCount), note: "Observed server requests" },
]);

onMounted(loadSystem);

async function loadSystem() {
  loading.value = true;
  error.value = "";
  try {
    const response = await getAdminSystem();
    system.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load system monitoring.";
  } finally {
    loading.value = false;
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(Number(value || 0));
}

function formatPercent(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "0%";
  return `${Math.round(number * 100)}%`;
}

function formatBytes(value) {
  const bytes = Number(value || 0);
  if (bytes >= 1024 * 1024 * 1024) return `${formatNumber(bytes / 1024 / 1024 / 1024)} GB`;
  if (bytes >= 1024 * 1024) return `${formatNumber(bytes / 1024 / 1024)} MB`;
  if (bytes >= 1024) return `${formatNumber(bytes / 1024)} KB`;
  return `${formatNumber(bytes)} B`;
}

function metricValue(metric) {
  if (metric.value == null) return "N/A";
  if (metric.unit === "bytes") return formatBytes(metric.value);
  if (metric.unit === "ratio") return formatPercent(metric.value);
  if (metric.unit === "ms") return `${formatNumber(metric.value)} ms`;
  return formatNumber(metric.value);
}
</script>

<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">Admin</p>
      <div class="mt-2 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 class="text-3xl font-black text-brand-navy dark:text-white">System</h1>
          <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-500 dark:text-slate-300">
            Visual monitoring backed by Spring Boot Actuator and Micrometer metrics.
          </p>
        </div>
        <button class="rounded-xl bg-brand-navy px-4 py-3 text-sm font-black text-white" @click="loadSystem">
          Refresh
        </button>
      </div>

      <p v-if="loading" class="mt-8 rounded-2xl bg-white p-6 font-bold text-slate-500 shadow-sm dark:bg-slate-800 dark:text-slate-300">
        Loading system metrics...
      </p>
      <p v-else-if="error" class="mt-8 rounded-2xl border border-red-100 bg-red-50 p-6 font-bold text-red-700">
        {{ error }}
      </p>

      <template v-else-if="system">
        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div v-for="card in cards" :key="card.label" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <p class="text-xs font-black uppercase tracking-[0.18em] text-slate-400">{{ card.label }}</p>
            <p class="mt-3 text-3xl font-black text-brand-navy dark:text-white">{{ card.value }}</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">{{ card.note }}</p>
          </div>
        </div>

        <div class="mt-6 grid gap-4 lg:grid-cols-2">
          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <h2 class="mb-5 text-xl font-black text-slate-900 dark:text-white">Runtime</h2>
            <div class="space-y-4">
              <div>
                <div class="mb-2 flex justify-between text-sm font-bold text-slate-600 dark:text-slate-300">
                  <span>Process CPU</span>
                  <span>{{ formatPercent(system.processCpuUsage) }}</span>
                </div>
                <div class="h-3 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-900">
                  <div class="h-full bg-brand-orange" :style="{ width: `${Math.min(100, Number(system.processCpuUsage || 0) * 100)}%` }"></div>
                </div>
              </div>
              <div>
                <div class="mb-2 flex justify-between text-sm font-bold text-slate-600 dark:text-slate-300">
                  <span>System CPU</span>
                  <span>{{ formatPercent(system.systemCpuUsage) }}</span>
                </div>
                <div class="h-3 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-900">
                  <div class="h-full bg-brand-navy" :style="{ width: `${Math.min(100, Number(system.systemCpuUsage || 0) * 100)}%` }"></div>
                </div>
              </div>
              <div>
                <div class="mb-2 flex justify-between text-sm font-bold text-slate-600 dark:text-slate-300">
                  <span>Heap</span>
                  <span>{{ formatBytes(system.heapUsedBytes) }} / {{ formatBytes(system.heapMaxBytes) }}</span>
                </div>
                <div class="h-3 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-900">
                  <div class="h-full bg-emerald-500" :style="{ width: `${Math.min(100, Number(system.heapUsedBytes || 0) / Math.max(1, Number(system.heapMaxBytes || 1)) * 100)}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <h2 class="mb-5 text-xl font-black text-slate-900 dark:text-white">Micrometer metrics</h2>
            <div class="space-y-3">
              <div v-for="metric in system.metrics" :key="metric.name" class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3 dark:bg-slate-900">
                <span>
                  <span class="block font-black text-slate-800 dark:text-white">{{ metric.label }}</span>
                  <span class="text-xs font-bold text-slate-400">{{ metric.name }}</span>
                </span>
                <span class="font-black text-brand-navy dark:text-white">{{ metricValue(metric) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>
