<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import { loginWithGoogle } from "@/api/auth.api";
import { getRecaptchaToken } from "@/utils/recaptcha";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  email: "",
  password: "",
});

const error = ref(route.query.oauthError?.toString() || "");

async function handleLogin() {
  error.value = "";

  if (!form.email || !form.password) {
    error.value = "Please enter your email and password.";
    return;
  }

  try {
    await authStore.loginUser({
      email: form.email,
      password: form.password,
      recaptchaToken: await getRecaptchaToken("login"),
    });

    router.push(route.query.redirect?.toString() || "/");
  } catch {
    error.value = authStore.error || "Login failed. Please try again.";
  }
}
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-gray-100 dark:border-slate-700 p-8">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white">
          Welcome back
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Sign in to book event tickets and manage your orders.
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="handleLogin">
        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Email
          </label>
          <input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            placeholder="you@example.com"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <div>
          <div class="flex items-center justify-between gap-3 mb-2">
            <label class="block text-sm font-bold text-slate-700 dark:text-slate-200">
              Password
            </label>
            <RouterLink to="/forgot-password" class="text-sm font-bold text-brand-orange hover:underline">
              Forgot password?
            </RouterLink>
          </div>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="At least 8 characters"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">
          {{ error }}
        </p>

        <button
          type="submit"
          :disabled="authStore.loading"
          class="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-60 text-white font-black py-3 rounded-xl transition active:scale-95"
        >
          {{ authStore.loading ? "Signing in..." : "Login" }}
        </button>
      </form>

      <div class="my-6 flex items-center gap-3">
        <div class="h-px flex-1 bg-gray-200 dark:bg-slate-700"></div>
        <span class="text-sm text-gray-400">or</span>
        <div class="h-px flex-1 bg-gray-200 dark:bg-slate-700"></div>
      </div>

      <button
        type="button"
        @click="loginWithGoogle"
        class="w-full border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700 text-slate-800 dark:text-white font-bold py-3 rounded-xl transition"
      >
        Continue with Google
      </button>

      <p class="text-center mt-6 text-sm text-gray-500 dark:text-gray-400">
        Do not have an account?
        <RouterLink to="/register" class="text-brand-orange font-bold hover:underline">
          Register
        </RouterLink>
      </p>
    </div>
  </div>
</template>
