<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import {
  getBookingEvent,
  getCategories,
  getEventBySlug,
  lockBookingSeats,
  releaseBookingSeats,
} from '../api/ticketRushApi'

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload'
const SELECT_TIMEOUT_SECONDS = 60
const SEAT_POLL_INTERVAL_MS = 3000
const TOAST_DURATION_MS = 3600
const MAX_TOASTS = 4

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref('')
const needsEventSelection = ref(false)
const bookingEvent = ref(null)
const eventDetail = ref(null)
const categories = ref([])
const selectedSeatIds = ref([])
const seatActionLoading = ref(false)
const seatActionError = ref('')
const bookingToasts = ref([])
const timerSeconds = ref(SELECT_TIMEOUT_SECONDS)
let timerHandle = null
let pollHandle = null
let toastSequence = 0
let seatStatusSnapshot = new Map()

const fallbackImage =
  'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1600&q=80'

const visibleSeats = computed(() => {
  const sections = bookingEvent.value?.sections || []
  return sections
    .flatMap((section) =>
      (section.seats || [])
        .filter((seat) => !seat.hidden)
        .map((seat) => ({
          ...seat,
          sectionName: section.name,
          seatTypeCode: (seat.seatTypeCode || 'STANDARD').toUpperCase(),
          layoutX: seat.layoutX ?? seat.seatNumber,
          layoutY: seat.layoutY ?? rowToIndex(seat.rowLabel),
        })),
    )
    .sort((a, b) => {
      if (a.layoutY !== b.layoutY) return a.layoutY - b.layoutY
      return a.layoutX - b.layoutX
    })
})

const rowGroups = computed(() => {
  const grouped = visibleSeats.value.reduce((acc, seat) => {
    if (!acc[seat.rowLabel]) acc[seat.rowLabel] = []
    acc[seat.rowLabel].push(seat)
    return acc
  }, {})

  return Object.entries(grouped)
    .sort(([rowA], [rowB]) => rowA.localeCompare(rowB))
    .map(([rowLabel, seats]) => {
      const sortedSeats = seats.sort((a, b) => a.layoutX - b.layoutX)
      const units = []

      for (let index = 0; index < sortedSeats.length; index += 1) {
        const current = sortedSeats[index]
        const next = sortedSeats[index + 1]

        if (
          isCoupleSeat(current) &&
          next &&
          isCoupleSeat(next) &&
          next.seatNumber === current.seatNumber + 1
        ) {
          units.push({
            key: `${current.id}-${next.id}`,
            seats: [current, next],
            label: `${current.rowLabel}${current.seatNumber}-${current.rowLabel}${next.seatNumber}`,
            type: 'pair',
          })
          index += 1
          continue
        }

        units.push({
          key: current.id,
          seats: [current],
          label: current.seatCode,
          type: 'single',
        })
      }

      const totalSpan = units.reduce((sum, unit) => sum + (unit.type === 'pair' ? 2 : 1), 0)
      const offset = Math.max(0, Math.floor((14 - totalSpan) / 2))

      return { rowLabel, units, offset }
    })
})

const selectedSeats = computed(() => {
  const selectedIds = new Set(selectedSeatIds.value)
  return visibleSeats.value.filter((seat) => selectedIds.has(seat.id))
})

const selectedSeatCodes = computed(() => {
  const selectedIds = new Set(selectedSeatIds.value)
  const codes = []

  for (const row of rowGroups.value) {
    for (const unit of row.units) {
      if (unit.seats.every((seat) => selectedIds.has(seat.id))) {
        codes.push(unit.label)
      }
    }
  }

  return codes
})
const totalPrice = computed(() =>
  selectedSeats.value.reduce((sum, seat) => sum + Number(seat.price || 0), 0),
)
const activeLockExpiryMs = computed(() => {
  const lockExpiryValues = selectedSeats.value
    .map((seat) => (seat.lockExpiresAt ? new Date(seat.lockExpiresAt).getTime() : null))
    .filter((value) => Number.isFinite(value))

  if (lockExpiryValues.length === 0) return null
  return Math.min(...lockExpiryValues)
})
const categoryName = computed(() => {
  const categoryId = Number(eventDetail.value?.categoryId)
  if (!Number.isFinite(categoryId) || categoryId <= 0) {
    return ''
  }
  return categories.value.find((category) => Number(category.id) === categoryId)?.name || ''
})

const eventInformation = computed(() => {
  const event = eventDetail.value
  if (!event) return []

  const minPrice = Number(event.minPrice)
  return [
    { label: 'Category', value: categoryName.value },
    { label: 'Genre', value: event.genre },
    { label: 'Country', value: event.country },
    { label: 'Duration', value: formatDuration(event.durationMinutes) },
    { label: 'Start', value: formatDateTime(event.startTime) },
    { label: 'Sale window', value: formatSaleWindow(event.saleStartTime, event.saleEndTime) },
    {
      label: 'From price',
      value: Number.isFinite(minPrice) && minPrice > 0 ? formatMoney(minPrice) : 'Free',
    },
  ].filter((item) => item.value)
})

const creditsInformation = computed(() => {
  const event = eventDetail.value
  if (!event) return []

  return [
    { label: 'Author / Creator', value: event.authorName },
    { label: 'Director', value: event.directorName },
    { label: 'Cast / Speakers', value: event.castMembers },
    { label: 'Performers', value: event.performerNames },
    { label: 'Singers', value: event.singerNames },
  ].filter((item) => item.value)
})

const showtimeLabel = computed(() => {
  if (!bookingEvent.value?.startTime) return ''
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(bookingEvent.value.startTime))
})

const eventDateLabel = computed(() => {
  if (!bookingEvent.value?.startTime) return ''
  return new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(bookingEvent.value.startTime))
})

const timeoutLabel = computed(() => {
  const mins = Math.floor(timerSeconds.value / 60)
    .toString()
    .padStart(2, '0')
  const secs = (timerSeconds.value % 60).toString().padStart(2, '0')
  return `${mins}:${secs}`
})
const currentUserId = computed(() => String(authStore.user?.id || '').toLowerCase())

function rowToIndex(rowLabel) {
  return (
    String(rowLabel || 'A')
      .toUpperCase()
      .charCodeAt(0) - 64
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value || 0)
}

function formatDateTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'full',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDuration(value) {
  const minutes = Number(value)
  if (!Number.isFinite(minutes) || minutes <= 0) {
    return ''
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (hours > 0 && remainingMinutes > 0) {
    return `${hours}h ${remainingMinutes}m`
  }
  if (hours > 0) {
    return `${hours}h`
  }
  return `${remainingMinutes}m`
}

function formatSaleWindow(start, end) {
  if (!start && !end) return ''
  if (start && end) {
    return `${formatDateTime(start)} - ${formatDateTime(end)}`
  }
  if (start) {
    return `From ${formatDateTime(start)}`
  }
  return `Until ${formatDateTime(end)}`
}

function isSeatSelected(seatId) {
  return selectedSeatIds.value.includes(seatId)
}

function isCoupleSeat(seat) {
  return seat.seatTypeCode === 'COUPLE' || seat.seatTypeCode === 'SWEETBOX'
}

function isSeatLockedByCurrentUser(seat) {
  if (seat.status !== 'LOCKED') return false
  if (!seat.lockOwnerUserId || !currentUserId.value) return false
  return String(seat.lockOwnerUserId).toLowerCase() === currentUserId.value
}

function withSeatOwnership(eventData) {
  if (!eventData || !Array.isArray(eventData.sections)) return eventData
  return {
    ...eventData,
    sections: eventData.sections.map((section) => ({
      ...section,
      seats: (section.seats || []).map((seat) => ({
        ...seat,
        lockedByCurrentUser: isSeatLockedByCurrentUser(seat),
      })),
    })),
  }
}

function canSelectSeatUnit(unit) {
  if (isUnitSelected(unit)) {
    return unit.seats.every((seat) => seat.lockedByCurrentUser)
  }
  return unit.seats.every((seat) => seat.status === 'AVAILABLE')
}

function isUnitSelected(unit) {
  return unit.seats.every((seat) => isSeatSelected(seat.id))
}

async function toggleSeatUnit(unit) {
  if (!canSelectSeatUnit(unit) || !bookingEvent.value?.eventId || seatActionLoading.value) return

  seatActionLoading.value = true
  seatActionError.value = ''
  const seatIds = unit.seats.map((seat) => seat.id)

  try {
    if (isUnitSelected(unit)) {
      await releaseBookingSeats({
        eventId: bookingEvent.value.eventId,
        seatIds,
      })
      selectedSeatIds.value = selectedSeatIds.value.filter((id) => !seatIds.includes(id))
    } else {
      await lockBookingSeats({
        eventId: bookingEvent.value.eventId,
        seatIds,
      })
      const selectedIdSet = new Set(selectedSeatIds.value)
      for (const seatId of seatIds) {
        selectedIdSet.add(seatId)
      }
      selectedSeatIds.value = Array.from(selectedIdSet)
    }

    await refreshSeatMap()
  } catch (err) {
    seatActionError.value =
      err?.response?.data?.message ||
      'Seat status has changed. Please wait for auto-refresh and try again.'
    await refreshSeatMap()
  } finally {
    seatActionLoading.value = false
  }
}

function seatClass(unit) {
  if (unit.seats.some((seat) => seat.status === 'SOLD')) {
    return 'bg-slate-500 border-slate-700 text-white cursor-not-allowed'
  }

  if (isUnitSelected(unit)) {
    return 'bg-blue-500 border-blue-700 text-white shadow'
  }

  if (unit.seats.some((seat) => seat.status === 'LOCKED')) {
    return 'bg-amber-100 border-amber-300 text-amber-700 cursor-not-allowed'
  }

  const leadSeat = unit.seats[0]

  if (leadSeat.seatTypeCode === 'COUPLE' || leadSeat.seatTypeCode === 'SWEETBOX') {
    return 'bg-rose-100 border-rose-300 text-rose-700 hover:bg-rose-200'
  }

  if (leadSeat.seatTypeCode === 'VIP') {
    return 'bg-orange-100 border-orange-300 text-orange-700 hover:bg-orange-200'
  }

  return 'bg-slate-100 border-slate-300 text-slate-700 hover:bg-slate-200'
}

function seatUnitStyle(row, unitIndex) {
  if (unitIndex === 0 && row.offset > 0) {
    return { gridColumnStart: row.offset + 1 }
  }
  return null
}

function continueToCheckout() {
  if (!bookingEvent.value || selectedSeatIds.value.length === 0) return

  sessionStorage.setItem(
    CHECKOUT_STORAGE_KEY,
    JSON.stringify({
      eventId: bookingEvent.value.eventId,
      seatIds: selectedSeatIds.value,
    }),
  )

  router.push('/checkout')
}

function pushBookingToast(message, tone = 'info') {
  const id = toastSequence++
  const toast = { id, message, tone }
  bookingToasts.value = [toast, ...bookingToasts.value].slice(0, MAX_TOASTS)

  window.setTimeout(() => {
    bookingToasts.value = bookingToasts.value.filter((item) => item.id !== id)
  }, TOAST_DURATION_MS)
}

function seatVisualState(seat) {
  if (seat.status === 'SOLD') return 'SOLD'
  if (seat.status === 'LOCKED' && seat.lockedByCurrentUser) return 'LOCKED_BY_ME'
  if (seat.status === 'LOCKED') return 'LOCKED_BY_OTHER'
  return 'AVAILABLE'
}

function buildSeatStatusSnapshot(eventData) {
  const snapshot = new Map()
  const sections = eventData?.sections || []

  for (const section of sections) {
    for (const seat of section.seats || []) {
      snapshot.set(seat.id, {
        seatCode: seat.seatCode,
        state: seatVisualState(seat),
      })
    }
  }

  return snapshot
}

function syncSeatChangeToasts(nextSnapshot) {
  if (!seatStatusSnapshot.size) {
    seatStatusSnapshot = nextSnapshot
    return
  }

  for (const [seatId, next] of nextSnapshot.entries()) {
    const previous = seatStatusSnapshot.get(seatId)
    if (!previous || previous.state === next.state) continue

    if (previous.state === 'AVAILABLE' && next.state === 'LOCKED_BY_OTHER') {
      pushBookingToast(`Seat ${next.seatCode} vừa được người khác giữ chỗ.`, 'warn')
      continue
    }

    if (previous.state === 'LOCKED_BY_OTHER' && next.state === 'AVAILABLE') {
      pushBookingToast(`Seat ${next.seatCode} vừa được mở lại.`, 'info')
      continue
    }

    if (previous.state !== 'SOLD' && next.state === 'SOLD') {
      pushBookingToast(`Seat ${next.seatCode} đã được thanh toán.`, 'error')
      continue
    }

    if (previous.state === 'LOCKED_BY_ME' && next.state === 'AVAILABLE') {
      pushBookingToast(`Giữ chỗ ghế ${next.seatCode} đã hết hạn.`, 'warn')
    }
  }

  seatStatusSnapshot = nextSnapshot
}

function startTimer() {
  clearInterval(timerHandle)
  timerHandle = setInterval(() => {
    const activeExpiry = activeLockExpiryMs.value
    if (!activeExpiry) {
      timerSeconds.value = SELECT_TIMEOUT_SECONDS
      return
    }

    const remainingSeconds = Math.max(0, Math.floor((activeExpiry - Date.now()) / 1000))
    timerSeconds.value = remainingSeconds
  }, 1000)
}

function syncSelectedLocksFromServer() {
  const seats = (bookingEvent.value?.sections || []).flatMap((section) => section.seats || [])
  selectedSeatIds.value = seats
    .filter((seat) => seat.lockedByCurrentUser && seat.status === 'LOCKED')
    .map((seat) => seat.id)
}

async function refreshSeatMap({ surfaceError = false, notifyChanges = false } = {}) {
  const eventId = route.query.eventId
  if (!eventId) return

  try {
    const latest = withSeatOwnership(await getBookingEvent(eventId))
    const nextSnapshot = buildSeatStatusSnapshot(latest)

    if (notifyChanges) {
      syncSeatChangeToasts(nextSnapshot)
    } else {
      seatStatusSnapshot = nextSnapshot
    }

    bookingEvent.value = latest
    syncSelectedLocksFromServer()
  } catch (err) {
    if (surfaceError) {
      error.value =
        err?.response?.data?.message || 'Unable to load seat map. Please check event/backend data.'
    }
  }
}

function startSeatPolling() {
  clearInterval(pollHandle)
  pollHandle = setInterval(() => {
    refreshSeatMap({ notifyChanges: true })
  }, SEAT_POLL_INTERVAL_MS)
}

async function loadBookingData() {
  clearInterval(pollHandle)
  loading.value = true
  error.value = ''
  needsEventSelection.value = false
  seatActionError.value = ''
  bookingToasts.value = []
  selectedSeatIds.value = []
  eventDetail.value = null
  categories.value = []
  seatStatusSnapshot = new Map()

  try {
    if (!authStore.hasCheckedAuth || !authStore.user?.id) {
      await authStore.checkAuth()
    }

    const eventId = route.query.eventId

    if (!eventId) {
      bookingEvent.value = null
      needsEventSelection.value = true
      return
    }

    await refreshSeatMap({ surfaceError: true, notifyChanges: false })
    if (!bookingEvent.value) {
      return
    }

    if (bookingEvent.value?.slug) {
      const [eventResponse, categoryResponse] = await Promise.all([
        getEventBySlug(bookingEvent.value.slug).catch(() => null),
        getCategories().catch(() => []),
      ])
      eventDetail.value = eventResponse
      categories.value = categoryResponse
    }
    startSeatPolling()
  } catch {
    bookingEvent.value = null
    error.value = 'Unable to load seat map. Please check event/backend data.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  startTimer()
  await loadBookingData()
})

watch(
  () => route.query.eventId,
  async () => {
    await loadBookingData()
  },
)

onUnmounted(() => {
  clearInterval(timerHandle)
  clearInterval(pollHandle)
})
</script>

<template>
  <div class="min-h-screen bg-brand-light pb-20">
    <div class="pointer-events-none fixed right-4 top-24 z-[80] w-[min(90vw,360px)] space-y-2">
      <div
        v-for="toast in bookingToasts"
        :key="toast.id"
        class="rounded-2xl border px-4 py-3 text-sm font-bold shadow-lg"
        :class="
          toast.tone === 'error'
            ? 'border-red-200 bg-red-50 text-red-700'
            : toast.tone === 'warn'
              ? 'border-amber-200 bg-amber-50 text-amber-700'
              : 'border-blue-200 bg-blue-50 text-blue-700'
        "
      >
        {{ toast.message }}
      </div>
    </div>

    <section class="mx-auto max-w-7xl px-4 pt-8 md:px-8">
      <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.2em] text-brand-orange">Booking</p>
            <h1 class="mt-1 text-2xl font-black text-brand-navy md:text-3xl">
              {{ bookingEvent?.title || 'Choose Seats' }}
            </h1>
            <p class="mt-2 text-sm text-slate-500">
              {{ eventDateLabel }} - {{ bookingEvent?.hallName || 'Venue Area' }}
            </p>
          </div>

          <div class="grid gap-2 text-right">
            <p class="text-sm font-bold text-slate-700">
              Start time: <span class="text-brand-navy">{{ showtimeLabel }}</span>
            </p>
            <p
              class="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-black text-blue-700"
            >
              Seat selection timer: {{ timeoutLabel }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <main class="mx-auto mt-6 max-w-7xl px-4 md:px-8">
      <div
        v-if="loading"
        class="rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500"
      >
        Loading seat map...
      </div>

      <div
        v-else-if="error"
        class="rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-bold text-red-700"
      >
        {{ error }}
      </div>

      <div
        v-else-if="needsEventSelection"
        class="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center"
      >
        <p class="text-lg font-black text-amber-800">Please select an event first.</p>
        <p class="mt-2 text-sm font-semibold text-amber-700">
          Go to Events, choose your event, then continue to Booking.
        </p>
        <button
          type="button"
          class="mt-5 rounded-xl bg-brand-navy px-6 py-3 text-sm font-black uppercase tracking-[0.14em] text-white transition hover:bg-sky-800"
          @click="router.push('/events')"
        >
          Go to Events
        </button>
      </div>

      <template v-else>
        <div class="grid gap-6 lg:grid-cols-[3fr_2fr]">
          <section class="space-y-6">
            <div
              class="rounded-[2rem] border border-slate-200 bg-white px-4 pb-8 pt-6 shadow-sm md:px-8"
            >
              <div class="mx-auto mb-10 max-w-5xl">
                <div
                  class="h-4 rounded-full bg-gradient-to-b from-amber-300 via-amber-200 to-transparent"
                ></div>
                <p
                  class="mt-3 text-center text-xs font-black uppercase tracking-[0.3em] text-slate-500"
                >
                  Stage / Venue
                </p>
              </div>

              <div class="overflow-x-auto">
                <div class="mx-auto min-w-[760px] max-w-4xl space-y-2">
                  <div v-for="row in rowGroups" :key="row.rowLabel" class="flex items-center gap-2">
                    <div class="w-6 text-center text-xs font-black text-slate-500">
                      {{ row.rowLabel }}
                    </div>
                    <div class="grid w-[664px] grid-cols-[repeat(14,minmax(0,1fr))] gap-2">
                      <button
                        v-for="(unit, unitIndex) in row.units"
                        :key="unit.key"
                        type="button"
                        :disabled="!canSelectSeatUnit(unit) || seatActionLoading"
                        @click="toggleSeatUnit(unit)"
                        :style="seatUnitStyle(row, unitIndex)"
                        class="h-10 w-full rounded-lg border-b-[3px] text-[11px] font-bold transition"
                        :class="[
                          seatClass(unit),
                          unit.type === 'pair' ? 'col-span-2' : 'col-span-1',
                        ]"
                      >
                        <span v-if="unit.seats.some((seat) => seat.status === 'SOLD')">X</span>
                        <span v-else-if="unit.seats.some((seat) => seat.status === 'LOCKED')"
                          >X</span
                        >
                        <span v-else>{{ unit.label }}</span>
                      </button>
                    </div>
                    <div class="w-6 text-center text-xs font-black text-slate-500">
                      {{ row.rowLabel }}
                    </div>
                  </div>
                </div>
              </div>

              <div
                v-if="seatActionError"
                class="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700"
              >
                {{ seatActionError }}
              </div>

              <div
                class="mt-8 flex flex-wrap justify-center gap-5 text-sm font-bold text-slate-700"
              >
                <div class="flex items-center gap-2">
                  <span
                    class="inline-block h-4 w-4 rounded bg-slate-500 border border-slate-700"
                  ></span>
                  Booked
                </div>
                <div class="flex items-center gap-2">
                  <span class="inline-block h-4 w-4 rounded bg-blue-500"></span> Your selection
                </div>
                <div class="flex items-center gap-2">
                  <span
                    class="inline-block h-4 w-4 rounded bg-slate-100 border border-slate-300"
                  ></span>
                  Standard
                </div>
                <div class="flex items-center gap-2">
                  <span
                    class="inline-block h-4 w-4 rounded bg-orange-100 border border-orange-300"
                  ></span>
                  VIP
                </div>
                <div class="flex items-center gap-2">
                  <span
                    class="inline-block h-4 w-4 rounded bg-rose-100 border border-rose-300"
                  ></span>
                  Couple
                </div>
              </div>
            </div>

            <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
              <div class="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
                <div>
                  <p class="text-xs font-black uppercase tracking-[0.15em] text-slate-500">
                    Selected Seats
                  </p>
                  <p class="mt-2 text-xl font-black text-brand-navy">
                    {{
                      selectedSeatCodes.length > 0
                        ? selectedSeatCodes.join(', ')
                        : 'No seats selected'
                    }}
                  </p>
                </div>
                <div>
                  <p class="text-xs font-black uppercase tracking-[0.15em] text-slate-500">Total</p>
                  <p class="mt-2 text-2xl font-black text-brand-orange">
                    {{ formatMoney(totalPrice) }}
                  </p>
                </div>
                <button
                  type="button"
                  :disabled="selectedSeatIds.length === 0 || seatActionLoading"
                  @click="continueToCheckout"
                  class="rounded-2xl bg-brand-navy px-8 py-4 text-sm font-black uppercase tracking-[0.2em] text-white transition hover:bg-sky-800 disabled:cursor-not-allowed disabled:bg-slate-300"
                >
                  Checkout
                </button>
              </div>
            </div>
          </section>

          <aside class="space-y-6">
            <div class="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
              <div class="h-[240px] bg-slate-100">
                <img
                  :src="eventDetail?.bannerUrl || bookingEvent?.bannerUrl || fallbackImage"
                  :alt="eventDetail?.title || bookingEvent?.title"
                  class="h-full w-full object-cover"
                />
              </div>

              <div class="p-6">
                <p class="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">
                  Selected event
                </p>
                <h2 class="mt-2 text-2xl font-black text-brand-navy">
                  {{ eventDetail?.title || bookingEvent?.title || 'Event detail' }}
                </h2>
                <p class="mt-3 text-sm leading-7 text-slate-600">
                  {{
                    eventDetail?.description || 'Provider is updating detailed event information.'
                  }}
                </p>
              </div>
            </div>

            <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
              <div class="rounded-2xl bg-slate-50 p-4">
                <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Venue</p>
                <p class="mt-2 text-sm font-black text-slate-800">
                  {{ eventDetail?.locationName || bookingEvent?.location || 'Venue TBA' }}
                </p>
                <p v-if="eventDetail?.address" class="mt-1 text-sm text-slate-600">
                  {{ eventDetail.address }}
                </p>
                <p v-if="eventDetail?.city" class="mt-1 text-sm text-slate-600">
                  {{ eventDetail.city }}
                </p>
              </div>

              <div class="mt-4 rounded-2xl bg-slate-50 p-4">
                <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">
                  Information
                </p>
                <dl class="mt-2 space-y-2">
                  <div
                    v-for="fact in eventInformation"
                    :key="fact.label"
                    class="flex items-start justify-between gap-3"
                  >
                    <dt class="text-xs font-black uppercase tracking-[0.1em] text-slate-400">
                      {{ fact.label }}
                    </dt>
                    <dd class="max-w-[68%] text-right text-sm font-bold text-slate-700">
                      {{ fact.value }}
                    </dd>
                  </div>
                </dl>
              </div>
            </div>

            <div
              v-if="creditsInformation.length"
              class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm"
            >
              <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Credits</p>
              <dl class="mt-2 grid gap-2">
                <div
                  v-for="credit in creditsInformation"
                  :key="credit.label"
                  class="rounded-xl bg-slate-50 px-3 py-2"
                >
                  <dt class="text-[11px] font-black uppercase tracking-[0.08em] text-slate-400">
                    {{ credit.label }}
                  </dt>
                  <dd class="mt-1 text-sm font-bold text-slate-700">{{ credit.value }}</dd>
                </div>
              </dl>
            </div>
          </aside>
        </div>
      </template>
    </main>
  </div>
</template>
