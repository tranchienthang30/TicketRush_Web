<script setup>
import { reactive, ref } from "vue";
import * as organizationApi from "@/api/organization.api";

const form = reactive({
  name: "",
  businessEmail: "",
});

const loading = ref(false);
const message = ref("");
const error = ref("");

async function submit() {
  message.value = "";
  error.value = "";

  if (!form.name || !form.businessEmail) {
    error.value = "Please enter organization name and business email.";
    return;
  }

  loading.value = true;
  try {
    const response = await organizationApi.registerOrganization({
      name: form.name,
      businessEmail: form.businessEmail,
    });
    message.value = response.data.message || "Verification email has been sent.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to register organization.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 px-4 py-10">
    <div class="max-w-xl mx-auto bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-gray-100 dark:border-slate-700 p-8">
      <h1 class="text-3xl font-black text-brand-navy dark:text-white mb-2">Register organization</h1>
      <p class="text-slate-500 dark:text-slate-400 mb-8">
        Verify a business email to become an organizer and manage events under your organization.
      </p>

      <form class="space-y-5" @submit.prevent="submit">
        <label class="block">
          <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Organization name</span>
          <input v-model.trim="form.name" type="text" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30" />
        </label>

        <label class="block">
          <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Business email</span>
          <input v-model.trim="form.businessEmail" type="email" placeholder="events@company.com" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30" />
        </label>

        <p v-if="message" class="text-sm text-green-700 bg-green-50 border border-green-100 rounded-xl px-4 py-3">{{ message }}</p>
        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">{{ error }}</p>

        <button type="submit" :disabled="loading" class="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-60 text-white font-black py-3 rounded-xl transition">
          {{ loading ? "Sending verification..." : "Send verification email" }}
        </button>
      </form>
    </div>
  </div>
</template>
