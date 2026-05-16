<script setup>
import { onMounted, ref } from "vue";
import * as providerApi from "@/api/provider.api";

const requests = ref([]);
const loading = ref(false);
const actionLoadingId = ref("");
const error = ref("");
const message = ref("");
const rejectReasons = ref({});

onMounted(loadRequests);

async function loadRequests() {
  loading.value = true;
  error.value = "";
  try {
    const response = await providerApi.getPendingProviderRequests();
    requests.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load provider requests.";
  } finally {
    loading.value = false;
  }
}

async function approve(user) {
  await runAction(user.id, async () => {
    const response = await providerApi.approveProviderRequest(user.id);
    message.value = response.data.message || "Provider request has been approved.";
  });
}

async function reject(user) {
  await runAction(user.id, async () => {
    const response = await providerApi.rejectProviderRequest(user.id, rejectReasons.value[user.id] || "");
    message.value = response.data.message || "Provider request has been rejected.";
  });
}

async function runAction(userId, action) {
  actionLoadingId.value = userId;
  error.value = "";
  message.value = "";
  try {
    await action();
    await loadRequests();
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to update provider request.";
  } finally {
    actionLoadingId.value = "";
  }
}

function formatDate(value) {
  if (!value) return "Not available";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
</script>

<template>
  <section class="min-h-screen bg-brand-light px-4 py-10 dark:bg-slate-900">
    <div class="mx-auto max-w-6xl">
      <div class="mb-8 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-sm font-black uppercase tracking-[0.25em] text-brand-orange">
            Admin
          </p>
          <h1 class="mt-2 text-3xl font-black text-brand-navy dark:text-white">
            Provider Requests
          </h1>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">
            Review customer requests before granting event creation and management access.
          </p>
        </div>
        <button
          type="button"
          class="rounded-xl border border-brand-orange px-5 py-3 text-sm font-black text-brand-orange hover:bg-brand-orange hover:text-white"
          @click="loadRequests"
        >
          Refresh
        </button>
      </div>

      <p v-if="message" class="mb-5 rounded-xl border border-green-100 bg-green-50 px-4 py-3 text-sm font-bold text-green-700">
        {{ message }}
      </p>
      <p v-if="error" class="mb-5 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-700">
        {{ error }}
      </p>

      <div class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div v-if="loading" class="p-8 text-center font-bold text-slate-500 dark:text-slate-300">
          Loading provider requests...
        </div>
        <div v-else-if="requests.length === 0" class="p-8 text-center font-bold text-slate-500 dark:text-slate-300">
          No pending provider requests.
        </div>
        <div v-else class="divide-y divide-slate-100 dark:divide-slate-700">
          <article
            v-for="user in requests"
            :key="user.id"
            class="grid gap-5 p-5 lg:grid-cols-[1fr_320px]"
          >
            <div>
              <div class="flex flex-wrap items-center gap-3">
                <h2 class="text-xl font-black text-slate-900 dark:text-white">
                  {{ user.fullName }}
                </h2>
                <span class="rounded-full bg-orange-50 px-3 py-1 text-xs font-black uppercase tracking-[0.15em] text-brand-orange">
                  {{ user.providerRequestStatus }}
                </span>
                <span
                  class="rounded-full px-3 py-1 text-xs font-black uppercase tracking-[0.15em]"
                  :class="user.emailVerified ? 'bg-green-50 text-green-700' : 'bg-slate-100 text-slate-500'"
                >
                  {{ user.emailVerified ? "Email verified" : "Email pending" }}
                </span>
              </div>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">
                {{ user.email }}
              </p>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">
                Requested at {{ formatDate(user.providerRequestedAt) }}
              </p>
            </div>

            <div class="space-y-3">
              <textarea
                v-model.trim="rejectReasons[user.id]"
                rows="2"
                placeholder="Optional rejection reason"
                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
              ></textarea>
              <div class="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  :disabled="actionLoadingId === user.id"
                  class="rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-white hover:bg-orange-600 disabled:opacity-60"
                  @click="approve(user)"
                >
                  Approve
                </button>
                <button
                  type="button"
                  :disabled="actionLoadingId === user.id"
                  class="rounded-xl border border-red-200 px-4 py-3 text-sm font-black text-red-600 hover:bg-red-50 disabled:opacity-60"
                  @click="reject(user)"
                >
                  Reject
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>
