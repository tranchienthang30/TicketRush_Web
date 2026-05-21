<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelPayOSCheckout, completePayOSCheckout } from '@/api/ticketRushApi'

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload'
const router = useRouter()
const route = useRoute()
const message = ref('Payment successful. Redirecting to My Tickets...')

function isFailedReturn() {
  const code = String(route.query.code || '').trim()
  return code !== '' && code !== '00'
}

onMounted(async () => {
  sessionStorage.removeItem(CHECKOUT_STORAGE_KEY)
  const orderCode = Number(route.query.orderCode)

  if (isFailedReturn()) {
    if (Number.isFinite(orderCode) && orderCode > 0) {
      try {
        await cancelPayOSCheckout(orderCode)
      } catch {
        // Ignore: lock might already be released by scheduler/webhook.
      }
    }
    message.value = 'Payment was not completed. Redirecting to Home...'
    window.setTimeout(() => {
      router.replace({ name: 'home' })
    }, 1200)
    return
  }

  if (Number.isFinite(orderCode) && orderCode > 0) {
    try {
      await completePayOSCheckout(orderCode)
    } catch {
      // If webhook already processed or endpoint is temporarily unavailable,
      // user still gets redirected to My Tickets.
    }
  }

  window.setTimeout(() => {
    router.replace({ name: 'my-tickets', query: { payment: 'success' } })
  }, 1200)
})
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 flex items-center justify-center px-4">
    <div
      class="w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm dark:border-slate-700 dark:bg-slate-800"
    >
      <h1 class="text-2xl font-black text-brand-navy dark:text-white">Payment Status</h1>
      <p class="mt-4 text-slate-600 dark:text-slate-300">{{ message }}</p>
    </div>
  </div>
</template>
