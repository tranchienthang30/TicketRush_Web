<script setup>
import { reactive, ref } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import { getRecaptchaToken } from "@/utils/recaptcha";

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  fullName: "",
  email: "",
  phone: "",
  password: "",
});

const error = ref("");

async function handleRegister() {
  error.value = "";

  if (!form.fullName || !form.email || !form.password) {
    error.value = "Please fill in all required fields.";
    return;
  }

  if (form.password.length < 8 || !/\d/.test(form.password) || !/[A-Za-z]/.test(form.password)) {
    error.value = "Password must be at least 8 characters and include both letters and numbers.";
    return;
  }

  try {
    await authStore.registerUser({
      fullName: form.fullName,
      email: form.email,
      phone: form.phone || null,
      password: form.password,
      recaptchaToken: await getRecaptchaToken("register"),
    });

    router.push("/");
  } catch {
    error.value = authStore.error || "Registration failed. Please try again.";
  }
}
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-gray-100 dark:border-slate-700 p-8">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white">
          Create account
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Create an account to book tickets and host events.
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="handleRegister">
        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Full name
          </label>
          <input
            v-model.trim="form.fullName"
            type="text"
            autocomplete="name"
            placeholder="Nguyen Van A"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

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
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Phone
          </label>
          <input
            v-model.trim="form.phone"
            type="tel"
            autocomplete="tel"
            placeholder="0912345678"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Password
          </label>
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="At least 8 characters, letters and numbers"
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
          {{ authStore.loading ? "Creating account..." : "Register" }}
        </button>
      </form>

      <p class="text-center mt-6 text-sm text-gray-500 dark:text-gray-400">
        Already have an account?
        <RouterLink to="/login" class="text-brand-orange font-bold hover:underline">
          Login
        </RouterLink>
      </p>
    </div>
  </div>
</template>
