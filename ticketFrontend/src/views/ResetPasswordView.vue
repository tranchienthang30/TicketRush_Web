<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import * as authApi from "@/api/auth.api";

const route = useRoute();
const router = useRouter();

const form = reactive({
  password: "",
  confirmPassword: "",
});

const loading = ref(false);
const error = ref("");
const message = ref("");

async function submit() {
  error.value = "";
  message.value = "";

  const token = route.query.token?.toString();
  if (!token) {
    error.value = "Reset token is missing.";
    return;
  }
  if (form.password.length < 8 || !/\d/.test(form.password) || !/[A-Za-z]/.test(form.password)) {
    error.value = "Password must be at least 8 characters and include both letters and numbers.";
    return;
  }
  if (form.password !== form.confirmPassword) {
    error.value = "Passwords do not match.";
    return;
  }

  loading.value = true;
  try {
    const response = await authApi.resetPassword({
      token,
      password: form.password,
    });
    message.value = response.data.message || "Password has been reset successfully.";
    setTimeout(() => router.push("/login"), 1200);
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to reset password. Please request a new link.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-gray-100 dark:border-slate-700 p-8">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white">
          Reset password
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Choose a new password for your TicketRush account.
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="submit">
        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            New password
          </label>
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="At least 8 characters, letters and numbers"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Confirm password
          </label>
          <input
            v-model="form.confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="Repeat your new password"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <p v-if="message" class="text-sm text-green-700 bg-green-50 border border-green-100 rounded-xl px-4 py-3">
          {{ message }}
        </p>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">
          {{ error }}
        </p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-60 text-white font-black py-3 rounded-xl transition active:scale-95"
        >
          {{ loading ? "Resetting..." : "Reset password" }}
        </button>
      </form>

      <p class="text-center mt-6 text-sm text-gray-500 dark:text-gray-400">
        Need another link?
        <RouterLink to="/forgot-password" class="text-brand-orange font-bold hover:underline">
          Request one
        </RouterLink>
      </p>
    </div>
  </div>
</template>
