<script setup>
import { computed, ref } from "vue";
import { RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import * as authApi from "@/api/auth.api";

const authStore = useAuthStore();
const user = computed(() => authStore.user);
const verifyMessage = ref("");
const verifyError = ref("");

async function resendVerification() {
  verifyMessage.value = "";
  verifyError.value = "";
  try {
    const response = await authApi.resendVerificationEmail();
    verifyMessage.value = response.data.message || "Verification email has been sent.";
  } catch (err) {
    verifyError.value = err.response?.data?.message || "Unable to send verification email.";
  }
}
</script>

<template>
  <section class="bg-brand-light dark:bg-slate-900 min-h-screen px-4 py-10">
    <div class="max-w-4xl mx-auto">
      <div class="mb-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white">
          Profile
        </h1>
        <p class="mt-2 text-slate-500 dark:text-slate-400">
          Manage your account information and contact details.
        </p>
      </div>

      <div
        v-if="user && !user.emailVerified"
        class="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-amber-950"
      >
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="font-black">Email verification required</p>
            <p class="mt-1 text-sm">Verify your email so ticket QR codes and purchase updates can be delivered safely.</p>
          </div>
          <button
            type="button"
            class="rounded-xl bg-brand-orange px-4 py-2 font-bold text-white hover:bg-orange-600"
            @click="resendVerification"
          >
            Resend email
          </button>
        </div>
        <p v-if="verifyMessage" class="mt-3 text-sm font-semibold text-green-700">{{ verifyMessage }}</p>
        <p v-if="verifyError" class="mt-3 text-sm font-semibold text-red-700">{{ verifyError }}</p>
      </div>

      <div
        v-if="user && user.role === 'CUSTOMER'"
        class="mb-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-800"
      >
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="font-black text-slate-900 dark:text-white">Want to create events?</p>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Register an organization with a business email to become an organizer.</p>
          </div>
          <RouterLink
            to="/organization/register"
            class="rounded-xl bg-brand-orange px-4 py-2 text-center font-bold text-white hover:bg-orange-600"
          >
            Register organization
          </RouterLink>
        </div>
      </div>

      <div class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm overflow-hidden">
        <div class="px-6 py-5 border-b border-slate-100 dark:border-slate-700 flex items-center gap-4">
          <div class="grid h-14 w-14 place-items-center rounded-full bg-brand-orange text-white text-xl font-black">
            {{ (user?.fullName || user?.email || "U").slice(0, 1).toUpperCase() }}
          </div>
          <div class="min-w-0">
            <h2 class="text-xl font-black text-slate-900 dark:text-white truncate">
              {{ user?.fullName || "User" }}
            </h2>
            <p class="text-sm text-slate-500 dark:text-slate-400 truncate">
              {{ user?.email }}
            </p>
          </div>
        </div>

        <div class="p-6 grid grid-cols-1 md:grid-cols-2 gap-5">
          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Full name</span>
            <input
              :value="user?.fullName || ''"
              disabled
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>

          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Email</span>
            <input
              :value="user?.email || ''"
              disabled
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>

          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Phone</span>
            <input
              :value="user?.phone || ''"
              disabled
              placeholder="Not provided"
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>

          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Role</span>
            <input
              :value="user?.role || ''"
              disabled
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>

          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Email status</span>
            <input
              :value="user?.emailVerified ? 'Verified' : 'Not verified'"
              disabled
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>

          <label class="block">
            <span class="block text-sm font-bold text-slate-700 dark:text-slate-200 mb-2">Organization</span>
            <input
              :value="user?.primaryOrganizationId || 'None'"
              disabled
              class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-700 dark:text-slate-200"
            />
          </label>
        </div>

        <div class="px-6 py-4 bg-slate-50 dark:bg-slate-900 border-t border-slate-100 dark:border-slate-700 flex justify-end">
          <button
            type="button"
            disabled
            class="px-5 py-2.5 rounded-xl bg-slate-300 text-slate-600 font-bold cursor-not-allowed dark:bg-slate-700 dark:text-slate-400"
          >
            Edit profile
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
