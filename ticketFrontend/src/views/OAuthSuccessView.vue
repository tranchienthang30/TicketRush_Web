<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const error = ref("");

onMounted(async () => {
  try {
    await authStore.fetchCurrentUser();
    router.push(route.query.redirect?.toString() || "/");
  } catch {
    error.value = "Google sign-in failed or the session cookie was not set.";
    authStore.clearSession();
    setTimeout(() => router.push("/login"), 1200);
  }
});
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-brand-light dark:bg-slate-900 px-4">
    <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-xl p-8 text-center max-w-md">
      <h1 class="text-2xl font-black text-brand-navy dark:text-white mb-3">
        Signing in with Google...
      </h1>

      <p v-if="!error" class="text-gray-500 dark:text-gray-400">
        We are verifying your account.
      </p>

      <p v-else class="text-red-600">
        {{ error }}
      </p>
    </div>
  </div>
</template>
