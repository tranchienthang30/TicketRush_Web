<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { confirmCheckout, previewCheckout } from '../api/ticketRushApi'
import { useAuthStore } from '@/stores/authStore'

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload'
const BOOKING_TIMER_STORAGE_KEY = 'ticketrush_booking_timer'

const router = useRouter()
const authStore = useAuthStore()

const checkoutPayload = ref(null)
const summary = ref(null)
const previewLoading = ref(false)
const confirmLoading = ref(false)
const isRedirectingToPayOS = ref(false)
const error = ref('')
const confirmResult = ref(null)
const voucherCodeInput = ref('')
const paymentMethod = ref('card')
const agreedTerms = ref(false)
const nowMs = ref(Date.now())
let countdownTimerHandle = null
const contact = ref({
  fullName: '',
  email: '',
  phone: '',
})

const paymentMethods = [
  {
    id: 'card',
    label: 'Bank QR',
    description: 'Scan QR for payment',
  },
  {
    id: 'wallet',
    label: 'E-Wallet',
    description: 'Momo, ZaloPay, VNPay QR.',
  },
]

const hasCheckoutData = computed(() => {
  return (
    Boolean(checkoutPayload.value?.eventId) &&
    Array.isArray(checkoutPayload.value?.seatIds) &&
    checkoutPayload.value.seatIds.length > 0
  )
})

const isConfirmDisabled = computed(() => {
  if (!hasCheckoutData.value || !summary.value) return true
  if (!contact.value.fullName.trim() || !contact.value.email.trim() || !contact.value.phone.trim())
    return true
  if (!agreedTerms.value) return true
  if (isLockExpired.value) return true
  return confirmLoading.value || isRedirectingToPayOS.value
})

const checkoutLockExpiryMs = computed(() => {
  const value = summary.value?.seatLockExpiresAt
  if (!value) return null
  const millis = new Date(value).getTime()
  return Number.isFinite(millis) ? millis : null
})

const sharedBookingExpiryMs = computed(() => {
  if (!checkoutPayload.value?.eventId) return null
  const raw = sessionStorage.getItem(BOOKING_TIMER_STORAGE_KEY)
  if (!raw) return null

  try {
    const payload = JSON.parse(raw)
    if (String(payload?.eventId || '') !== String(checkoutPayload.value.eventId)) {
      return null
    }
    const millis = Number(payload?.deadlineMs)
    return Number.isFinite(millis) ? millis : null
  } catch {
    return null
  }
})

const remainingLockSeconds = computed(() => {
  const expiry = sharedBookingExpiryMs.value ?? checkoutLockExpiryMs.value
  if (!expiry) return null
  return Math.max(0, Math.floor((expiry - nowMs.value) / 1000))
})

const isLockExpired = computed(() => {
  const remaining = remainingLockSeconds.value
  return remaining !== null && remaining <= 0
})

const remainingLockLabel = computed(() => {
  const remaining = remainingLockSeconds.value
  if (remaining === null) return '--:--'
  const mins = Math.floor(remaining / 60)
    .toString()
    .padStart(2, '0')
  const secs = (remaining % 60).toString().padStart(2, '0')
  return `${mins}:${secs}`
})

watch(remainingLockSeconds, (seconds) => {
  if (seconds === null || seconds > 0) return
  router.replace({ name: 'home', query: { booking: 'expired' } })
})

function formatMoney(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value || 0)
}

function parseApiError(err, fallbackMessage) {
  return err?.response?.data?.message || fallbackMessage
}

function loadCheckoutPayload() {
  const rawData = sessionStorage.getItem(CHECKOUT_STORAGE_KEY)
  if (!rawData) {
    checkoutPayload.value = null
    return
  }

  try {
    checkoutPayload.value = JSON.parse(rawData)
  } catch {
    checkoutPayload.value = null
  }
}

async function loadPreview(voucherCode = null) {
  if (!hasCheckoutData.value) return

  previewLoading.value = true
  error.value = ''

  try {
    summary.value = await previewCheckout({
      eventId: checkoutPayload.value.eventId,
      seatIds: checkoutPayload.value.seatIds,
      voucherCode,
    })
  } catch (err) {
    summary.value = null
    error.value = parseApiError(err, 'Unable to load checkout preview.')
  } finally {
    previewLoading.value = false
  }
}

async function applyVoucher() {
  const code = voucherCodeInput.value.trim()
  await loadPreview(code || null)
}

async function clearVoucher() {
  voucherCodeInput.value = ''
  await loadPreview(null)
}

async function submitCheckout() {
  if (isConfirmDisabled.value) return

  confirmLoading.value = true
  isRedirectingToPayOS.value = false
  error.value = ''
  confirmResult.value = null

  try {
    const response = await confirmCheckout({
      eventId: checkoutPayload.value.eventId,
      seatIds: checkoutPayload.value.seatIds,
      voucherCode: voucherCodeInput.value.trim() || null,
      paymentMethod: paymentMethod.value,
      fullName: contact.value.fullName.trim(),
      email: contact.value.email.trim(),
      phone: contact.value.phone.trim(),
    })

    const checkoutUrl = typeof response?.checkoutUrl === 'string' ? response.checkoutUrl.trim() : ''
    if (paymentMethod.value === 'card') {
      if (!checkoutUrl) {
        throw new Error('Payment link is missing from server response.')
      }

      isRedirectingToPayOS.value = true
      window.location.href = checkoutUrl
      return
    }

    confirmResult.value = response
    summary.value = response.summary || summary.value
    sessionStorage.removeItem(CHECKOUT_STORAGE_KEY)
    await router.push({ name: 'my-tickets', query: { payment: 'success' } })
  } catch (err) {
    if (paymentMethod.value === 'card') {
      error.value = parseApiError(
        err,
        'Unable to redirect to payOS. Please try again or choose another method.',
      )
    } else {
      error.value = parseApiError(err, 'Payment failed. Please try again.')
    }
  } finally {
    confirmLoading.value = false
    isRedirectingToPayOS.value = false
  }
}

function goBackToBooking() {
  if (checkoutPayload.value?.eventId) {
    router.push({ path: '/booking', query: { eventId: checkoutPayload.value.eventId } })
    return
  }
  router.push('/booking')
}

function fillContactFromCurrentUser() {
  const user = authStore.user
  if (!user) return
  contact.value.fullName = (user.fullName || '').trim()
  contact.value.email = (user.email || '').trim()
  contact.value.phone = (user.phone || '').trim()
}

onMounted(async () => {
  countdownTimerHandle = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)

  if (!authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      // Ignore here; route guard already handles auth redirect.
    }
  }
  fillContactFromCurrentUser()
  loadCheckoutPayload()
  if (hasCheckoutData.value) {
    await loadPreview(null)
  }
})

onUnmounted(() => {
  if (countdownTimerHandle) {
    clearInterval(countdownTimerHandle)
  }
})
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 pb-20">
    <section class="bg-brand-navy text-white">
      <div class="mx-auto max-w-7xl px-4 py-12 md:px-8 md:py-16">
        <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">Checkout</p>
        <h1 class="mt-3 text-4xl font-black tracking-tight md:text-5xl">Continue Checkout</h1>
        <p class="mt-4 max-w-2xl text-blue-100">
          All pricing, voucher, membership, and booking results are calculated directly from backend
          APIs.
        </p>
      </div>
    </section>

    <section class="mx-auto mt-10 max-w-7xl px-4 md:px-8">
      <div
        v-if="!hasCheckoutData"
        class="rounded-3xl border border-slate-200 bg-white p-10 text-center dark:border-slate-700 dark:bg-slate-800"
      >
        <h2 class="text-2xl font-black text-brand-navy dark:text-white">No checkout data found</h2>
        <p class="mt-3 text-slate-500 dark:text-slate-300">
          Please return to the seat selection page to start checkout.
        </p>
        <button
          type="button"
          @click="goBackToBooking"
          class="mt-6 inline-flex rounded-2xl bg-brand-orange px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-orange-600"
        >
          Back to Booking
        </button>
      </div>

      <div v-else class="grid items-start gap-8 lg:grid-cols-[1.25fr_0.95fr]">
        <div class="space-y-6">
          <section
            class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Contact</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">
              Ticket Delivery Info
            </h2>
            <div class="mt-6 grid gap-4 md:grid-cols-2">
              <label class="block md:col-span-2">
                <span
                  class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                  >Full Name</span
                >
                <input
                  v-model="contact.fullName"
                  type="text"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="Nguyen Van A"
                />
              </label>

              <label class="block">
                <span
                  class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                  >Email</span
                >
                <input
                  v-model="contact.email"
                  type="email"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="you@email.com"
                />
              </label>

              <label class="block">
                <span
                  class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                  >Phone Number</span
                >
                <input
                  v-model="contact.phone"
                  type="tel"
                  class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  placeholder="09xx xxx xxx"
                />
              </label>
            </div>
          </section>

          <section
            class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Payment</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">Payment Method</h2>
            <div class="mt-6 grid gap-3">
              <label
                v-for="method in paymentMethods"
                :key="method.id"
                class="flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition"
                :class="
                  paymentMethod === method.id
                    ? 'border-brand-orange bg-brand-orange/5'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-500'
                "
              >
                <input
                  v-model="paymentMethod"
                  :value="method.id"
                  type="radio"
                  class="mt-1 h-4 w-4 accent-brand-orange"
                />
                <div>
                  <p class="font-black text-brand-navy dark:text-white">{{ method.label }}</p>
                  <p class="text-sm text-slate-500 dark:text-slate-300">{{ method.description }}</p>
                </div>
              </label>
            </div>
          </section>

          <section
            class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Voucher</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">Discount Code</h2>
            <div class="mt-5 flex flex-col gap-3 md:flex-row">
              <input
                v-model="voucherCodeInput"
                type="text"
                placeholder="Enter discount code"
                class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 outline-none transition focus:border-brand-orange focus:ring-4 focus:ring-brand-orange/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
              />
              <button
                type="button"
                @click="applyVoucher"
                class="rounded-2xl bg-brand-navy px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-sky-800"
              >
                Apply
              </button>
              <button
                type="button"
                @click="clearVoucher"
                class="rounded-2xl border border-slate-300 px-6 py-3 text-sm font-black uppercase tracking-[0.18em] text-slate-600 transition hover:border-slate-400 dark:border-slate-600 dark:text-slate-200"
              >
                Clear
              </button>
            </div>
          </section>

          <section
            class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <label class="flex items-start gap-3">
              <input
                v-model="agreedTerms"
                type="checkbox"
                class="mt-1 h-4 w-4 accent-brand-orange"
              />
              <span class="text-sm text-slate-600 dark:text-slate-300">
                I confirm the payment details are correct and agree to the platform terms.
              </span>
            </label>

            <button
              type="button"
              :disabled="isConfirmDisabled"
              @click="submitCheckout"
              class="mt-6 w-full rounded-2xl px-6 py-4 text-sm font-black uppercase tracking-[0.2em] text-white transition"
              :class="
                isConfirmDisabled
                  ? 'bg-slate-300 cursor-not-allowed'
                  : 'bg-brand-orange hover:bg-orange-600'
              "
            >
              {{
                isRedirectingToPayOS
                  ? 'Redirecting to payOS...'
                  : confirmLoading
                    ? 'Processing...'
                    : 'Confirm Payment'
              }}
            </button>

            <div
              v-if="confirmResult"
              class="mt-4 rounded-2xl border border-green-200 bg-green-50 px-5 py-4 text-sm font-bold text-green-700"
            >
              Payment successful. Order ID: {{ confirmResult.orderId }}
            </div>
          </section>
        </div>

        <aside class="space-y-6 lg:sticky lg:top-24">
          <section
            class="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <p class="text-sm font-black uppercase tracking-[0.3em] text-brand-orange">Summary</p>
            <h2 class="mt-3 text-2xl font-black text-brand-navy dark:text-white">Order Summary</h2>

            <div
              v-if="previewLoading"
              class="mt-6 rounded-2xl bg-slate-50 px-4 py-6 text-center font-bold text-slate-500 dark:bg-slate-900 dark:text-slate-300"
            >
              Calculating price from backend...
            </div>

            <div v-else-if="summary" class="mt-6 space-y-4">
              <div
                class="rounded-2xl border px-4 py-4"
                :class="
                  isLockExpired
                    ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900 dark:bg-red-950/70 dark:text-red-200'
                    : 'border-orange-200 bg-orange-50 text-brand-orange dark:border-orange-900 dark:bg-orange-950/70 dark:text-orange-200'
                "
              >
                <p class="text-xs font-black uppercase tracking-[0.18em]">Time remaining</p>
                <p class="mt-2 text-2xl font-black">{{ remainingLockLabel }}</p>
                <p class="mt-1 text-sm font-bold">
                  {{
                    isLockExpired
                      ? 'Seat lock expired. Please go back and select seats again.'
                      : 'From seat lock until payment completion.'
                  }}
                </p>
              </div>

              <div class="rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900">
                <p
                  class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                >
                  Event
                </p>
                <p class="mt-2 text-lg font-black text-brand-navy dark:text-white">
                  {{ summary.eventTitle }}
                </p>
              </div>

              <div class="rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900">
                <p
                  class="text-xs font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                >
                  Seat codes ({{ summary.ticketCount }})
                </p>
                <p class="mt-2 text-sm font-bold text-brand-navy dark:text-white">
                  {{ summary.seatCodes.join(', ') }}
                </p>
              </div>

              <div class="space-y-3 text-sm">
                <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                  <span>Ticket subtotal</span>
                  <span class="font-bold">{{ formatMoney(summary.ticketSubtotal) }}</span>
                </div>
                <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                  <span>Platform fee</span>
                  <span class="font-bold">{{ formatMoney(summary.serviceFee) }}</span>
                </div>
                <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                  <span>Membership discount</span>
                  <span class="font-bold text-green-600"
                    >- {{ formatMoney(summary.membershipDiscount) }}</span
                  >
                </div>
                <div class="flex items-center justify-between text-slate-600 dark:text-slate-300">
                  <span>Voucher discount</span>
                  <span class="font-bold text-green-600"
                    >- {{ formatMoney(summary.voucherDiscount) }}</span
                  >
                </div>
              </div>

              <div class="border-t border-slate-200 pt-4 dark:border-slate-700">
                <div class="flex items-center justify-between">
                  <span
                    class="text-sm font-black uppercase tracking-[0.18em] text-slate-500 dark:text-slate-300"
                  >
                    Total payment
                  </span>
                  <span class="text-3xl font-black text-brand-orange">{{
                    formatMoney(summary.totalAmount)
                  }}</span>
                </div>
              </div>
            </div>
          </section>

          <div
            v-if="error"
            class="rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-sm font-bold text-red-700"
          >
            {{ error }}
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>
