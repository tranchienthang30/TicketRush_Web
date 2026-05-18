<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const payload = ref(null);
const error = ref("");

const prettyJson = computed(() => (payload.value ? JSON.stringify(payload.value, null, 2) : ""));

const downloadFileName = computed(() => {
  const orderPart = String(payload.value?.orderId || "ticket").slice(0, 8);
  const seatPart = String(payload.value?.seatCode || "seat").replace(/[^a-zA-Z0-9_-]/g, "");
  return `ticket-${orderPart}-${seatPart || "seat"}.json`;
});

onMounted(() => {
  const token = String(route.query.payload || "");
  if (!token) {
    error.value = "Missing QR payload.";
    return;
  }

  try {
    payload.value = JSON.parse(fromBase64Url(token));
    downloadJson();
  } catch {
    error.value = "Invalid or corrupted QR payload.";
  }
});

function fromBase64Url(value) {
  const base64 = value
    .replace(/-/g, "+")
    .replace(/_/g, "/")
    .padEnd(Math.ceil(value.length / 4) * 4, "=");
  const binary = atob(base64);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function downloadJson() {
  if (!payload.value) return;
  const blob = new Blob([JSON.stringify(payload.value, null, 2)], { type: "application/json" });
  const downloadUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = downloadUrl;
  anchor.download = downloadFileName.value;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(downloadUrl);
}
</script>

<template>
  <main class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-950">
    <section class="mx-auto max-w-3xl rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
      <h1 class="text-2xl font-black text-brand-navy dark:text-white">Ticket QR Payload</h1>

      <div
        v-if="error"
        class="mt-4 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-700"
      >
        {{ error }}
      </div>

      <template v-else-if="payload">
        <p class="mt-3 text-sm text-slate-600 dark:text-slate-300">
          JSON file downloaded automatically. If it did not download, use the button below.
        </p>
        <button
          type="button"
          class="mt-4 rounded-xl bg-brand-navy px-5 py-2.5 text-sm font-black uppercase tracking-[0.14em] text-white transition hover:bg-sky-800"
          @click="downloadJson"
        >
          Download JSON
        </button>
        <pre class="mt-5 overflow-x-auto rounded-2xl bg-slate-900 p-4 text-xs text-slate-100">{{ prettyJson }}</pre>
      </template>
    </section>
  </main>
</template>
