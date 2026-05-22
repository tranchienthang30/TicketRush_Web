<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import {
  getBookingEvent,
  getCategories,
  getEventBySlug,
  getVirtualQueueStatus,
  joinVirtualQueue,
  lockBookingSeats,
  releaseBookingSeats,
} from '../api/ticketRushApi'

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload'
const BOOKING_TIMER_STORAGE_KEY = 'ticketrush_booking_timer'
const SELECT_TIMEOUT_SECONDS = 600
const SEAT_POLL_INTERVAL_MS = 3000
const QUEUE_POLL_INTERVAL_MS = 5000
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
const queueStatus = ref(null)
const queueError = ref('')
const selectedSeatsioObjects = ref([])
const seatsioRenderError = ref('')
const timerSeconds = ref(SELECT_TIMEOUT_SECONDS)
const bookingCountdownDeadlineMs = ref(null)
const hasRedirectedOnTimeout = ref(false)
let timerHandle = null
let pollHandle = null
let queuePollHandle = null
let toastSequence = 0
let seatStatusSnapshot = new Map()
let seatsioChart = null
let seatsioScriptPromise = null

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
          seatTypeName: seat.seatTypeName || section.name,
          visualColorHex: normalizeHexColor(seat.visualColorHex || section.visualColorHex),
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

const selectedSeatsioLabels = computed(() =>
  selectedSeatsioObjects.value.map((object) => object.label).filter(Boolean),
)
const selectedSeatsioTotal = computed(() =>
  selectedSeatsioObjects.value.reduce((sum, object) => sum + Number(object.price || 0), 0),
)
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
const seatLegendItems = computed(() => {
  const seen = new Set()
  const items = []
  for (const seat of visibleSeats.value) {
    const label = normalizeSeatLegendLabel(
      seat.seatTypeName || seat.sectionName || seat.seatTypeCode || 'Standard',
    )
    const color = seat.visualColorHex || defaultSeatColor(seat.seatTypeCode)
    const key = label.toUpperCase()
    if (seen.has(key)) continue
    seen.add(key)
    items.push({ label, color })
  }
  return items
})

const screenLabel = computed(() => {
  const hallName = String(bookingEvent.value?.hallName || '')
  const hallMatch = hallName.match(/(SCREEN-\d+)/i)
  if (hallMatch) return hallMatch[1].toUpperCase()

  for (const seat of visibleSeats.value) {
    const source = `${seat.seatTypeName || ''} ${seat.sectionName || ''} ${seat.seatTypeCode || ''}`
    const match = source.match(/(SCREEN-\d+)/i)
    if (match) return match[1].toUpperCase()
  }

  return ''
})

const screenDisplayLabel = computed(() => {
  if (!screenLabel.value) return ''
  const number = screenLabel.value.replace(/SCREEN-/i, '')
  return `Screen - ${number}`
})
const totalPrice = computed(() =>
  usesSeatsio.value
    ? selectedSeatsioTotal.value
    : selectedSeats.value.reduce((sum, seat) => sum + Number(seat.price || 0), 0),
)
const categoryName = computed(() => {
  const categoryId = Number(eventDetail.value?.categoryId)
  if (!Number.isFinite(categoryId) || categoryId <= 0) {
    return ''
  }
  return categories.value.find((category) => Number(category.id) === categoryId)?.name || ''
})

const isWaitingRoom = computed(() => queueStatus.value?.status === 'WAITING')
const queueCooldownSeconds = computed(() => {
  const message = String(queueStatus.value?.message || '')
  const secondsMatch = message.match(/(\d+)\s*seconds?/i)
  if (!secondsMatch) return null
  const value = Number(secondsMatch[1])
  return Number.isFinite(value) && value > 0 ? value : null
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
const usesSeatsio = computed(() => bookingEvent.value?.seatProvider === 'SEATS_IO')
const seatsioReady = computed(() =>
  Boolean(bookingEvent.value?.externalSeatWorkspaceKey && bookingEvent.value?.externalSeatEventKey),
)
const seatsioPricing = computed(() => ({
  allFeesIncluded: false,
  showSectionPricingOverlay: true,
  priceFormatter: (price) => formatMoney(price),
  prices: (bookingEvent.value?.sections || []).map((section) => ({
    category: section.name,
    price: Number(section.basePrice || 0),
  })),
}))
const seatsioPriceByCategory = computed(() => {
  const prices = new Map()
  for (const section of bookingEvent.value?.sections || []) {
    const price = Number(section.basePrice || 0)
    for (const key of [section.name, section.seatTypeCode]) {
      const normalized = String(key || '').trim().toLowerCase()
      if (normalized) {
        prices.set(normalized, price)
      }
    }
  }
  return prices
})

function rowToIndex(rowLabel) {
  return (
    String(rowLabel || 'A')
      .toUpperCase()
      .charCodeAt(0) - 64
  )
}

function normalizeSeatLegendLabel(rawLabel) {
  const normalized = String(rawLabel || '')
    .trim()
    .toUpperCase()
  if (normalized.includes('COUPLE')) return 'Couple'
  if (normalized.includes('VIP')) return 'VIP'
  if (normalized.includes('STANDARD')) return 'Standard'
  return String(rawLabel || 'Standard').trim() || 'Standard'
}

function formatMoney(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value || 0)
}

function formatDateTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('en-US', {
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

function normalizeHexColor(color) {
  const normalized = String(color || '')
    .trim()
    .toUpperCase()
  return /^#[0-9A-F]{6}$/.test(normalized) ? normalized : ''
}

function defaultSeatColor(seatTypeCode) {
  switch ((seatTypeCode || 'STANDARD').toUpperCase()) {
    case 'VIP':
      return '#F97316'
    case 'COUPLE':
    case 'SWEETBOX':
      return '#E11D48'
    case 'WHEELCHAIR':
      return '#16A34A'
    default:
      return '#CBD5E1'
  }
}

function readableTextColor(hexColor) {
  const color = normalizeHexColor(hexColor).slice(1)
  if (!color) return '#334155'
  const red = parseInt(color.slice(0, 2), 16)
  const green = parseInt(color.slice(2, 4), 16)
  const blue = parseInt(color.slice(4, 6), 16)
  const luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
  return luminance > 0.6 ? '#334155' : '#FFFFFF'
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
    return 'bg-slate-500 border-slate-700 text-white cursor-not-allowed dark:bg-slate-600 dark:border-slate-500'
  }

  if (isUnitSelected(unit)) {
    return 'bg-blue-500 border-blue-700 text-white shadow'
  }

  if (unit.seats.some((seat) => seat.status === 'LOCKED')) {
    return 'bg-amber-100 border-amber-300 text-amber-700 cursor-not-allowed dark:bg-amber-950/60 dark:border-amber-700 dark:text-amber-200'
  }

  const leadSeat = unit.seats[0]
  if (leadSeat.visualColorHex) {
    return 'hover:opacity-90'
  }

  if (leadSeat.seatTypeCode === 'COUPLE' || leadSeat.seatTypeCode === 'SWEETBOX') {
    return 'bg-rose-100 border-rose-300 text-rose-700 hover:bg-rose-200 dark:bg-rose-950/70 dark:border-rose-700 dark:text-rose-200 dark:hover:bg-rose-900'
  }

  if (leadSeat.seatTypeCode === 'VIP') {
    return 'bg-orange-100 border-orange-300 text-orange-700 hover:bg-orange-200 dark:bg-orange-950/70 dark:border-orange-700 dark:text-orange-200 dark:hover:bg-orange-900'
  }

  if (leadSeat.seatTypeCode === 'WHEELCHAIR') {
    return 'bg-emerald-100 border-emerald-300 text-emerald-700 hover:bg-emerald-200 dark:bg-emerald-950/70 dark:border-emerald-700 dark:text-emerald-200 dark:hover:bg-emerald-900'
  }

  return 'bg-slate-100 border-slate-300 text-slate-700 hover:bg-slate-200 dark:bg-slate-800 dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-700'
}

function seatUnitStyle(row, unitIndex, unit) {
  const style = {}
  if (unitIndex === 0 && row.offset > 0) {
    style.gridColumnStart = row.offset + 1
  }
  const leadSeat = unit?.seats?.[0]
  if (
    leadSeat?.visualColorHex &&
    unit.seats.every((seat) => seat.status === 'AVAILABLE') &&
    !isUnitSelected(unit)
  ) {
    style.backgroundColor = leadSeat.visualColorHex
    style.borderColor = leadSeat.visualColorHex
    style.color = readableTextColor(leadSeat.visualColorHex)
  }
  return Object.keys(style).length > 0 ? style : null
}

function continueToCheckout() {
  if (usesSeatsio.value) {
    seatActionError.value = ''
    return
  }
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

function loadPersistedBookingDeadline(eventId) {
  if (!eventId) return null
  const raw = sessionStorage.getItem(BOOKING_TIMER_STORAGE_KEY)
  if (!raw) return null

  try {
    const payload = JSON.parse(raw)
    const storedEventId = String(payload?.eventId || '')
    const deadlineMs = Number(payload?.deadlineMs)
    if (storedEventId !== String(eventId) || !Number.isFinite(deadlineMs)) {
      return null
    }
    return deadlineMs
  } catch {
    return null
  }
}

function persistBookingDeadline(eventId, deadlineMs) {
  if (!eventId || !Number.isFinite(deadlineMs)) return
  sessionStorage.setItem(
    BOOKING_TIMER_STORAGE_KEY,
    JSON.stringify({
      eventId: String(eventId),
      deadlineMs,
    }),
  )
}

function ensureBookingDeadline(eventId) {
  const now = Date.now()
  const persisted = loadPersistedBookingDeadline(eventId)
  if (persisted && persisted > now) {
    bookingCountdownDeadlineMs.value = persisted
    timerSeconds.value = Math.max(0, Math.floor((persisted - now) / 1000))
    return
  }

  const deadline = now + SELECT_TIMEOUT_SECONDS * 1000
  bookingCountdownDeadlineMs.value = deadline
  timerSeconds.value = SELECT_TIMEOUT_SECONDS
  persistBookingDeadline(eventId, deadline)
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
    const countdownDeadline = bookingCountdownDeadlineMs.value
    if (!countdownDeadline) {
      return
    }

    const remainingSeconds = Math.max(0, Math.floor((countdownDeadline - Date.now()) / 1000))
    timerSeconds.value = remainingSeconds
  }, 1000)
}

function redirectHomeOnTimeout() {
  if (hasRedirectedOnTimeout.value) return
  hasRedirectedOnTimeout.value = true
  selectedSeatIds.value = []
  clearQueuePolling()
  router.replace({ name: 'home', query: { booking: 'expired' } })
}

function syncSelectedLocksFromServer() {
  const seats = (bookingEvent.value?.sections || []).flatMap((section) => section.seats || [])
  selectedSeatIds.value = seats
    .filter((seat) => seat.lockedByCurrentUser && seat.status === 'LOCKED')
    .map((seat) => seat.id)
}

async function ensureQueueAccess(eventId) {
  queueError.value = ''
  const status = await joinVirtualQueue(eventId)
  queueStatus.value = status

  if (status.status === 'READY') {
    clearQueuePolling()
    return true
  }

  startQueuePolling(eventId)
  return false
}

function startQueuePolling(eventId) {
  clearQueuePolling()
  queuePollHandle = setInterval(async () => {
    try {
      let status = await getVirtualQueueStatus(eventId)
      if (status.status === 'NOT_JOINED') {
        status = await joinVirtualQueue(eventId)
      }

      queueStatus.value = status
      queueError.value = ''

      if (status.status === 'READY') {
        clearQueuePolling()
        await loadBookingData()
      }
    } catch (err) {
      queueError.value =
        err?.response?.data?.message || 'Không thể cập nhật vị trí hàng chờ. Vui lòng thử lại.'
    }
  }, QUEUE_POLL_INTERVAL_MS)
}

function clearQueuePolling() {
  clearInterval(queuePollHandle)
  queuePollHandle = null
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
    if (!usesSeatsio.value) {
      syncSelectedLocksFromServer()
    }
  } catch (err) {
    if (surfaceError) {
      error.value =
        err?.response?.data?.message || 'Unable to load seat map. Please check event/backend data.'
    }
  }
}

function startSeatPolling() {
  if (usesSeatsio.value) return
  clearInterval(pollHandle)
  pollHandle = setInterval(() => {
    refreshSeatMap({ notifyChanges: true })
  }, SEAT_POLL_INTERVAL_MS)
}

async function loadBookingData() {
  clearInterval(pollHandle)
  clearQueuePolling()
  loading.value = true
  hasRedirectedOnTimeout.value = false
  error.value = ''
  needsEventSelection.value = false
  seatActionError.value = ''
  bookingToasts.value = []
  queueStatus.value = null
  queueError.value = ''
  selectedSeatIds.value = []
  selectedSeatsioObjects.value = []
  seatsioRenderError.value = ''
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

    ensureBookingDeadline(eventId)

    const hasQueueAccess = await ensureQueueAccess(eventId)
    if (!hasQueueAccess) {
      bookingEvent.value = null
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
    if (!usesSeatsio.value) {
      startSeatPolling()
    }
  } catch {
    bookingEvent.value = null
    error.value = 'Unable to load seat map. Please check event/backend data.'
  } finally {
    loading.value = false
    if (usesSeatsio.value && bookingEvent.value && !error.value) {
      await nextTick()
      await renderSeatsioChart()
    }
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
  clearQueuePolling()
  destroySeatsioChart()
})

watch(timerSeconds, (seconds) => {
  if (seconds > 0) return
  if (loading.value || error.value || needsEventSelection.value || isWaitingRoom.value) return
  redirectHomeOnTimeout()
})

async function renderSeatsioChart() {
  destroySeatsioChart()
  if (!usesSeatsio.value || !seatsioReady.value) {
    seatsioRenderError.value = 'This seats.io event is missing workspace or event keys.'
    return
  }

  try {
    await loadSeatsioScript()
    seatsioChart = new window.seatsio.SeatingChart({
      divId: 'seatsio-booking-chart',
      workspaceKey: bookingEvent.value.externalSeatWorkspaceKey,
      event: bookingEvent.value.externalSeatEventKey,
      session: 'continue',
      pricing: seatsioPricing.value,
      onObjectSelected: (object) => {
        const selection = seatsioSelectionFromObject(object)
        if (
          selection.label &&
          !selectedSeatsioObjects.value.some((item) => item.label === selection.label)
        ) {
          selectedSeatsioObjects.value = [...selectedSeatsioObjects.value, selection]
        }
      },
      onObjectDeselected: (object) => {
        const label = String(object?.label || object?.id || '').trim()
        selectedSeatsioObjects.value = selectedSeatsioObjects.value.filter((item) => item.label !== label)
      },
    }).render()
  } catch (err) {
    seatsioRenderError.value = err?.message || 'Unable to render seats.io chart.'
  }
}

function destroySeatsioChart() {
  if (seatsioChart?.destroy) {
    seatsioChart.destroy()
  }
  seatsioChart = null
}

function loadSeatsioScript() {
  if (window.seatsio) return Promise.resolve()
  if (seatsioScriptPromise) return seatsioScriptPromise

  seatsioScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = seatsioCdnUrl()
    script.async = true
    script.onload = resolve
    script.onerror = () => reject(new Error('Unable to load seats.io chart.js'))
    document.head.appendChild(script)
  })
  return seatsioScriptPromise
}

function seatsioCdnUrl() {
  const region = import.meta.env.VITE_SEATSIO_REGION || 'oc'
  return `https://cdn-${region}.seatsio.net/chart.js`
}

function seatsioSelectionFromObject(object) {
  const label = String(object?.label || object?.id || '').trim()
  const category = seatsioObjectCategory(object)
  const normalizedCategory = category.toLowerCase()
  const directPrice = Number(object?.pricing?.price ?? object?.price)
  const mappedPrice = seatsioPriceByCategory.value.get(normalizedCategory)
  const price = Number.isFinite(directPrice) && directPrice >= 0
    ? directPrice
    : Number(mappedPrice || 0)

  return {
    label,
    category: category || 'Category',
    price,
  }
}

function seatsioObjectCategory(object) {
  const category = object?.category
  if (typeof category === 'string') {
    return category.trim()
  }
  return String(
    category?.key ||
      category?.label ||
      object?.categoryKey ||
      object?.categoryLabel ||
      object?.categoryName ||
      '',
  ).trim()
}
</script>

<template>
  <div class="min-h-screen bg-brand-light pb-20 dark:bg-slate-900">
    <div class="pointer-events-none fixed right-4 top-24 z-[80] w-[min(90vw,360px)] space-y-2">
      <div
        v-for="toast in bookingToasts"
        :key="toast.id"
        class="rounded-2xl border px-4 py-3 text-sm font-bold shadow-lg"
        :class="
          toast.tone === 'error'
            ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900 dark:bg-red-950/80 dark:text-red-200'
            : toast.tone === 'warn'
              ? 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-800 dark:bg-amber-950/80 dark:text-amber-200'
              : 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900 dark:bg-blue-950/80 dark:text-blue-200'
        "
      >
        {{ toast.message }}
      </div>
    </div>

    <section class="mx-auto max-w-7xl px-4 pt-8 md:px-8">
      <div
        class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800 md:p-8"
      >
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.2em] text-brand-orange">Booking</p>
            <h1 class="mt-1 text-2xl font-black text-brand-navy dark:text-white md:text-3xl">
              {{ bookingEvent?.title || 'Choose Seats' }}
            </h1>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">
              {{ eventDateLabel }} - {{ bookingEvent?.hallName || 'Venue Area' }}
              <span v-if="screenDisplayLabel"> - {{ screenDisplayLabel }}</span>
            </p>
          </div>

          <div class="grid gap-2 text-right">
            <p class="text-sm font-bold text-slate-700 dark:text-slate-200">
              Start time:
              <span class="text-brand-navy dark:text-brand-orange">{{ showtimeLabel }}</span>
            </p>
            <p
              class="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-black text-blue-700 dark:border-blue-900 dark:bg-blue-950/60 dark:text-blue-200"
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
        class="rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
      >
        Loading seat map...
      </div>

      <div
        v-else-if="error"
        class="rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-bold text-red-700 dark:border-red-900 dark:bg-red-950/60 dark:text-red-200"
      >
        {{ error }}
      </div>

      <div
        v-else-if="needsEventSelection"
        class="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-950/60"
      >
        <p class="text-lg font-black text-amber-800 dark:text-amber-100">
          Please select an event first.
        </p>
        <p class="mt-2 text-sm font-semibold text-amber-700 dark:text-amber-200">
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

      <div
        v-else-if="isWaitingRoom"
        class="mx-auto max-w-3xl rounded-3xl border border-blue-100 bg-white p-8 text-center shadow-sm dark:border-blue-900 dark:bg-slate-800"
      >
        <p class="text-xs font-black uppercase tracking-[0.22em] text-brand-orange">
          Virtual Queue
        </p>
        <h2 class="mt-3 text-3xl font-black text-brand-navy dark:text-white">Phòng chờ đặt vé</h2>
        <p
          class="mx-auto mt-4 max-w-xl text-base font-semibold leading-7 text-slate-600 dark:text-slate-300"
        >
          Vui lòng chờ
          <span class="font-black text-brand-orange">{{ queueCooldownSeconds ?? '...' }}</span>
          giây rồi thử lại.
        </p>

        <p
          v-if="queueError"
          class="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700"
        >
          {{ queueError }}
        </p>

        <button
          type="button"
          class="mt-6 rounded-2xl bg-brand-navy px-7 py-3 text-sm font-black uppercase tracking-[0.14em] text-white transition hover:bg-sky-800"
          @click="loadBookingData"
        >
          Kiểm tra ngay
        </button>
      </div>

      <template v-else>
        <div class="grid gap-6 lg:grid-cols-[3fr_2fr]">
          <section class="space-y-6">
            <div
              v-if="usesSeatsio"
              class="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800 md:p-6"
            >
              <div class="mb-4 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                <div>
                  <p class="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Advanced seating</p>
                  <h2 class="mt-1 text-xl font-black text-brand-navy dark:text-white">Choose seats on the seats.io map</h2>
                </div>
                <p class="text-xs font-bold text-slate-500 dark:text-slate-300">
                  {{ selectedSeatsioObjects.length }} selected
                </p>
              </div>

              <div
                v-if="seatsioRenderError"
                class="mb-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700"
              >
                {{ seatsioRenderError }}
              </div>
              <div
                id="seatsio-booking-chart"
                class="min-h-[680px] overflow-hidden rounded-2xl bg-slate-100 dark:bg-slate-900"
              ></div>
            </div>

            <div
              v-else
              class="rounded-[2rem] border border-slate-200 bg-white px-4 pb-8 pt-6 shadow-sm dark:border-slate-700 dark:bg-slate-800 md:px-8"
            >
              <div class="mx-auto mb-10 max-w-5xl">
                <div
                  class="h-4 rounded-full bg-gradient-to-b from-amber-300 via-amber-200 to-transparent"
                ></div>
                <p
                  class="mt-3 text-center text-xs font-black uppercase tracking-[0.3em] text-slate-500 dark:text-slate-300"
                >
                  Stage / Venue
                </p>
              </div>

              <div class="overflow-x-auto">
                <div class="mx-auto min-w-[760px] max-w-4xl space-y-2">
                  <div v-for="row in rowGroups" :key="row.rowLabel" class="flex items-center gap-2">
                    <div
                      class="w-6 text-center text-xs font-black text-slate-500 dark:text-slate-300"
                    >
                      {{ row.rowLabel }}
                    </div>
                    <div class="grid w-[664px] grid-cols-[repeat(14,minmax(0,1fr))] gap-2">
                      <button
                        v-for="(unit, unitIndex) in row.units"
                        :key="unit.key"
                        type="button"
                        :disabled="!canSelectSeatUnit(unit) || seatActionLoading"
                        @click="toggleSeatUnit(unit)"
                        :style="seatUnitStyle(row, unitIndex, unit)"
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
                    <div
                      class="w-6 text-center text-xs font-black text-slate-500 dark:text-slate-300"
                    >
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
                class="mx-auto mt-8 grid max-w-4xl grid-cols-1 gap-x-8 gap-y-4 text-sm font-bold text-slate-700 sm:grid-cols-2 md:grid-cols-3 dark:text-slate-200"
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
                    class="inline-block h-4 w-4 rounded border border-amber-300 bg-amber-100"
                  ></span>
                  Temporarily locked
                </div>
                <div
                  v-for="item in seatLegendItems"
                  :key="`${item.label}-${item.color}`"
                  class="flex items-center gap-2"
                >
                  <span
                    class="inline-block h-4 w-4 rounded border border-slate-300 dark:border-slate-600"
                    :style="{ backgroundColor: item.color }"
                  ></span>
                  {{ item.label }}
                </div>
              </div>
            </div>

            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <div class="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
                <div>
                  <p
                    class="text-xs font-black uppercase tracking-[0.15em] text-slate-500 dark:text-slate-300"
                  >
                    {{ usesSeatsio ? 'Selected objects' : 'Selected Seats' }}
                  </p>
                  <p class="mt-2 text-xl font-black text-brand-navy dark:text-white">
                    {{
                      usesSeatsio
                        ? selectedSeatsioObjects.length > 0
                          ? selectedSeatsioLabels.join(', ')
                          : 'No seats selected'
                        : selectedSeatCodes.length > 0
                          ? selectedSeatCodes.join(', ')
                          : 'No seats selected'
                    }}
                  </p>
                  <div
                    v-if="usesSeatsio && selectedSeatsioObjects.length > 0"
                    class="mt-3 flex flex-wrap gap-2"
                  >
                    <span
                      v-for="object in selectedSeatsioObjects"
                      :key="object.label"
                      class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-bold text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
                    >
                      {{ object.label }} · {{ object.category }} · {{ formatMoney(object.price) }}
                    </span>
                  </div>
                </div>
                <div>
                  <p
                    class="text-xs font-black uppercase tracking-[0.15em] text-slate-500 dark:text-slate-300"
                  >
                    Total
                  </p>
                  <p class="mt-2 text-2xl font-black text-brand-orange">
                    {{ formatMoney(totalPrice) }}
                  </p>
                </div>
                <button
                  v-if="!usesSeatsio"
                  type="button"
                  :disabled="selectedSeatIds.length === 0 || seatActionLoading"
                  @click="continueToCheckout"
                  class="rounded-2xl bg-brand-navy px-8 py-4 text-sm font-black uppercase tracking-[0.2em] text-white transition hover:bg-sky-800 disabled:cursor-not-allowed disabled:bg-slate-300"
                >
                  Checkout
                </button>
                <div
                  v-else
                  class="rounded-2xl border border-blue-200 bg-blue-50 px-5 py-3 text-sm font-black text-blue-700 dark:border-blue-900 dark:bg-blue-950/60 dark:text-blue-200"
                >
                  Demo selection only
                </div>
              </div>
              <p v-if="usesSeatsio" class="mt-4 text-sm font-bold text-slate-500 dark:text-slate-300">
                Advanced seats.io checkout is not connected yet. This demo shows selected objects and
                prices from TicketRush category mapping.
              </p>
            </div>
          </section>

          <aside class="space-y-6">
            <div
              class="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <div class="h-[240px] bg-slate-100 dark:bg-slate-900">
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
                <h2 class="mt-2 text-2xl font-black text-brand-navy dark:text-white">
                  {{ eventDetail?.title || bookingEvent?.title || 'Event detail' }}
                </h2>
                <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
                  {{
                    eventDetail?.description || 'Provider is updating detailed event information.'
                  }}
                </p>
              </div>
            </div>

            <div
              class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <div class="rounded-2xl bg-slate-50 p-4 dark:bg-slate-900">
                <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Venue</p>
                <p class="mt-2 text-sm font-black text-slate-800 dark:text-white">
                  {{ eventDetail?.locationName || bookingEvent?.location || 'Venue TBA' }}
                </p>
                <p
                  v-if="eventDetail?.address"
                  class="mt-1 text-sm text-slate-600 dark:text-slate-300"
                >
                  {{ eventDetail.address }}
                </p>
                <p v-if="eventDetail?.city" class="mt-1 text-sm text-slate-600 dark:text-slate-300">
                  {{ eventDetail.city }}
                </p>
              </div>

              <div class="mt-4 rounded-2xl bg-slate-50 p-4 dark:bg-slate-900">
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
                    <dd
                      class="max-w-[68%] text-right text-sm font-bold text-slate-700 dark:text-slate-200"
                    >
                      {{ fact.value }}
                    </dd>
                  </div>
                </dl>
              </div>
            </div>

            <div
              v-if="creditsInformation.length"
              class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <p class="text-xs font-black uppercase tracking-[0.16em] text-slate-400">Credits</p>
              <dl class="mt-2 grid gap-2">
                <div
                  v-for="credit in creditsInformation"
                  :key="credit.label"
                  class="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-900"
                >
                  <dt class="text-[11px] font-black uppercase tracking-[0.08em] text-slate-400">
                    {{ credit.label }}
                  </dt>
                  <dd class="mt-1 text-sm font-bold text-slate-700 dark:text-slate-200">
                    {{ credit.value }}
                  </dd>
                </div>
              </dl>
            </div>
          </aside>
        </div>
      </template>
    </main>
  </div>
</template>
