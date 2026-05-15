<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload'
const router = useRouter()

onMounted(() => {
  sessionStorage.removeItem(CHECKOUT_STORAGE_KEY)
  window.setTimeout(() => {
    router.replace({ name: 'home' })
  }, 1200)
})
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4">
    <div
      class="w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm dark:border-slate-700 dark:bg-slate-800"
    >
      <h1 class="text-2xl font-black text-brand-navy dark:text-white">Payment Canceled</h1>
      <p class="mt-4 text-slate-600 dark:text-slate-300">
        Your payment was canceled. Redirecting to Home...
      </p>
    </div>
  </div>
</template>
