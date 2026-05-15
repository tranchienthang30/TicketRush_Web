<script setup>
import { onMounted, ref } from "vue";
import { useRoute, RouterLink } from "vue-router";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/stores/authStore";

const route = useRoute();
const authStore = useAuthStore();
const status = ref("Verifying your email...");
const isError = ref(false);

onMounted(async () => {
  const token = route.query.token?.toString();
  if (!token) {
    status.value = "Verification token is missing.";
    isError.value = true;
    return;
  }

  try {
    const response = await authApi.verifyEmail(token);
    await authStore.checkAuth();
    status.value = response.data.message || "Email has been verified successfully.";
  } catch (err) {
    status.value = err.response?.data?.message || "Verification link is invalid or expired.";
    isError.value = true;
  }
});
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-gray-100 dark:border-slate-700 p-8 text-center">
      <h1 class="text-3xl font-black text-brand-navy dark:text-white mb-4">Email verification</h1>
      <p :class="isError ? 'text-red-600' : 'text-green-700'">{{ status }}</p>
      <RouterLink to="/profile" class="inline-block mt-6 text-brand-orange font-bold hover:underline">Go to profile</RouterLink>
    </div>
  </div>
</template>
