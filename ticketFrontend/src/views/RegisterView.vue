<script setup>
import { reactive, ref } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "../stores/authStore";

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  fullName: "",
  email: "",
  password: "",
});

const error = ref("");

async function handleRegister() {
  error.value = "";

  if (!form.fullName || !form.email || !form.password) {
    error.value = "Please enter all required information";
    return;
  }

  if (form.password.length < 6) {
    error.value = "Password must be at least 6 characters";
    return;
  }

  try {
    await authStore.registerUser({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
    });

    router.push("/");
  } catch (err) {
    error.value = authStore.error || "Registration failed";
  }
}
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-3xl shadow-2xl border border-gray-100 dark:border-slate-700 p-8">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white">
          Create account
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Create an account to book movie seats and save your tickets.
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="handleRegister">
        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Full name
          </label>
          <input
            v-model="form.fullName"
            type="text"
            placeholder="Nguyen Van A"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30"
          />
        </div>

        <div>
          <label class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">
            Email
          </label>
          <input
            v-model="form.email"
            type="email"
            placeholder="you@example.com"
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
            placeholder="At least 6 characters"
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
