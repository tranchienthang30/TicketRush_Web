<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const error = ref("");

onMounted(async () => {
  const token = route.query.token;

  if (!token) {
    error.value = "Không tìm thấy token Google OAuth";
    setTimeout(() => router.push("/login"), 1200);
    return;
  }

  try {
    authStore.setTokenFromOAuth(token);
    await authStore.fetchCurrentUser();

    router.push("/");
  } catch (err) {
    error.value = "Đăng nhập Google thất bại";
    authStore.logout();

    setTimeout(() => router.push("/login"), 1200);
  }
});
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-brand-light dark:bg-slate-900 px-4">
    <div class="bg-white dark:bg-slate-800 rounded-3xl shadow-xl p-8 text-center max-w-md">
      <h1 class="text-2xl font-black text-brand-navy dark:text-white mb-3">
        Đang đăng nhập bằng Google...
      </h1>

      <p v-if="!error" class="text-gray-500 dark:text-gray-400">
        Hệ thống đang xác thực tài khoản của bạn.
      </p>

      <p v-else class="text-red-600">
        {{ error }}
      </p>
    </div>
  </div>
</template>