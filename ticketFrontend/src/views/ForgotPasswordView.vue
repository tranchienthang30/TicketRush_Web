<script setup>
import { reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import * as authApi from "@/api/auth.api";
import { getRecaptchaToken } from "@/utils/recaptcha";

const form = reactive({
  email: "",
});

const loading = ref(false);
const error = ref("");
const message = ref("");

async function submit() {
  error.value = "";
  message.value = "";

  if (!form.email) {
    error.value = "Please enter your email address.";
    return;
  }

  loading.value = true;
  try {
    const response = await authApi.forgotPassword({
      email: form.email,
      recaptchaToken: await getRecaptchaToken("forgot_password"),
    });
    message.value = response.data.message || "If this email exists, a password reset link has been sent.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to send reset email. Please try again.";
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
          Forgot password
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Enter your email and we will send a secure reset link.
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="submit">
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
          {{ loading ? "Sending..." : "Send reset link" }}
        </button>
      </form>

      <p class="text-center mt-6 text-sm text-gray-500 dark:text-gray-400">
        Remember your password?
        <RouterLink to="/login" class="text-brand-orange font-bold hover:underline">
          Login
        </RouterLink>
      </p>
    </div>
  </div>
</template>
