<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getBookingEvent, getEvents } from "../api/ticketRushApi";

const CHECKOUT_STORAGE_KEY = "ticketrush_checkout_payload";
const SELECT_TIMEOUT_SECONDS = 10 * 60;

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref("");
const bookingEvent = ref(null);
const selectedSeats = ref([]);
const timerSeconds = ref(SELECT_TIMEOUT_SECONDS);
let timerHandle = null;

const visibleSeats = computed(() => {
  const sections = bookingEvent.value?.sections || [];
  return sections
    .flatMap((section) =>
      (section.seats || [])
        .filter((seat) => !seat.hidden)
        .map((seat) => ({
          ...seat,
          sectionName: section.name,
          seatTypeCode: (seat.seatTypeCode || "STANDARD").toUpperCase(),
          layoutX: seat.layoutX ?? seat.seatNumber,
          layoutY: seat.layoutY ?? rowToIndex(seat.rowLabel),
        })),
    )
    .sort((a, b) => {
      if (a.layoutY !== b.layoutY) return a.layoutY - b.layoutY;
      return a.layoutX - b.layoutX;
    });
});

const rowGroups = computed(() => {
  const grouped = visibleSeats.value.reduce((acc, seat) => {
    if (!acc[seat.rowLabel]) acc[seat.rowLabel] = [];
    acc[seat.rowLabel].push(seat);
    return acc;
  }, {});

  return Object.entries(grouped)
    .sort(([rowA], [rowB]) => rowA.localeCompare(rowB))
    .map(([rowLabel, seats]) => ({
      rowLabel,
      seats: seats.sort((a, b) => a.layoutX - b.layoutX),
    }));
});

const selectedSeatCodes = computed(() => selectedSeats.value.map((seat) => seat.seatCode));
const totalPrice = computed(() => selectedSeats.value.reduce((sum, seat) => sum + Number(seat.price || 0), 0));

const showtimeLabel = computed(() => {
  if (!bookingEvent.value?.startTime) return "";
  return new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(bookingEvent.value.startTime));
});

const eventDateLabel = computed(() => {
  if (!bookingEvent.value?.startTime) return "";
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(bookingEvent.value.startTime));
});

const timeoutLabel = computed(() => {
  const mins = Math.floor(timerSeconds.value / 60)
    .toString()
    .padStart(2, "0");
  const secs = (timerSeconds.value % 60).toString().padStart(2, "0");
  return `${mins}:${secs}`;
});

function rowToIndex(rowLabel) {
  return String(rowLabel || "A").toUpperCase().charCodeAt(0) - 64;
}

function formatMoney(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value || 0);
}

function isSeatSelected(seatId) {
  return selectedSeats.value.some((seat) => seat.id === seatId);
}

function canSelectSeat(seat) {
  return seat.status === "AVAILABLE";
}

function toggleSeat(seat) {
  if (!canSelectSeat(seat)) return;

  const index = selectedSeats.value.findIndex((selected) => selected.id === seat.id);
  if (index >= 0) {
    selectedSeats.value.splice(index, 1);
  } else {
    selectedSeats.value.push(seat);
  }
}

function seatClass(seat) {
  if (seat.status === "SOLD") {
    return "bg-red-100 border-red-300 text-red-700 cursor-not-allowed";
  }

  if (seat.status === "LOCKED") {
    return "bg-amber-100 border-amber-300 text-amber-700 cursor-not-allowed";
  }

  if (isSeatSelected(seat.id)) {
    return "bg-blue-500 border-blue-700 text-white shadow";
  }

  if (seat.seatTypeCode === "VIP") {
    return "bg-orange-100 border-orange-300 text-orange-700 hover:bg-orange-200";
  }

  if (seat.seatTypeCode === "COUPLE" || seat.seatTypeCode === "SWEETBOX") {
    return "bg-rose-100 border-rose-300 text-rose-700 hover:bg-rose-200";
  }

  return "bg-slate-100 border-slate-300 text-slate-700 hover:bg-slate-200";
}

function continueToCheckout() {
  if (!bookingEvent.value || selectedSeats.value.length === 0) return;

  sessionStorage.setItem(
    CHECKOUT_STORAGE_KEY,
    JSON.stringify({
      eventId: bookingEvent.value.eventId,
      seatIds: selectedSeats.value.map((seat) => seat.id),
    }),
  );

  router.push("/checkout");
}

function startTimer() {
  clearInterval(timerHandle);
  timerSeconds.value = SELECT_TIMEOUT_SECONDS;

  timerHandle = setInterval(() => {
    if (timerSeconds.value <= 0) {
      clearInterval(timerHandle);
      selectedSeats.value = [];
      return;
    }
    timerSeconds.value -= 1;
  }, 1000);
}

async function loadBookingData() {
  loading.value = true;
  error.value = "";
  selectedSeats.value = [];

  try {
    let eventId = route.query.eventId;
    if (!eventId) {
      const pageData = await getEvents({ page: 0, size: 1 });
      eventId = pageData?.content?.[0]?.id;
    }

    if (!eventId) {
      throw new Error("No movie found");
    }

    bookingEvent.value = await getBookingEvent(eventId);
    startTimer();
  } catch {
    bookingEvent.value = null;
    error.value = "Unable to load seat map. Please check movie/backend data.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadBookingData);
onUnmounted(() => clearInterval(timerHandle));
</script>

<template>
  <div class="min-h-screen bg-brand-light pb-20">
    <section class="mx-auto max-w-7xl px-4 pt-8 md:px-8">
      <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.2em] text-brand-orange">Booking</p>
            <h1 class="mt-1 text-2xl font-black text-brand-navy md:text-3xl">
              {{ bookingEvent?.title || "Choose Seats" }}
            </h1>
            <p class="mt-2 text-sm text-slate-500">
              {{ eventDateLabel }} - {{ bookingEvent?.hallName || "Screen Room" }}
            </p>
          </div>

          <div class="grid gap-2 text-right">
            <p class="text-sm font-bold text-slate-700">
              Showtime: <span class="text-brand-navy">{{ showtimeLabel }}</span>
            </p>
            <p class="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-black text-blue-700">
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

      <template v-else>
        <div class="rounded-[2rem] border border-slate-200 bg-white px-4 pb-8 pt-6 shadow-sm md:px-8">
          <div class="mx-auto mb-10 max-w-5xl">
            <div class="h-4 rounded-full bg-gradient-to-b from-amber-300 via-amber-200 to-transparent"></div>
            <p class="mt-3 text-center text-xs font-black uppercase tracking-[0.3em] text-slate-500">Screen</p>
          </div>

          <div class="overflow-x-auto">
            <div class="mx-auto min-w-[760px] max-w-4xl space-y-2">
              <div
                v-for="row in rowGroups"
                :key="row.rowLabel"
                class="flex items-center gap-2"
              >
                <div class="w-6 text-center text-xs font-black text-slate-500">{{ row.rowLabel }}</div>
                <button
                  v-for="seat in row.seats"
                  :key="seat.id"
                  type="button"
                  :disabled="!canSelectSeat(seat)"
                  @click="toggleSeat(seat)"
                  class="h-10 w-10 rounded-lg border-b-[3px] text-[11px] font-bold transition"
                  :class="seatClass(seat)"
                >
                  <span v-if="seat.status === 'SOLD'">X</span>
                  <span v-else>{{ seat.seatCode }}</span>
                </button>
                <div class="w-6 text-center text-xs font-black text-slate-500">{{ row.rowLabel }}</div>
              </div>
            </div>
          </div>

          <div class="mt-8 flex flex-wrap justify-center gap-5 text-sm font-bold text-slate-700">
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-red-100 border border-red-300"></span> Booked</div>
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-blue-500"></span> Your selection</div>
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-slate-100 border border-slate-300"></span> Standard</div>
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-orange-100 border border-orange-300"></span> VIP</div>
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-rose-100 border border-rose-300"></span> Couple</div>
          </div>
        </div>

        <div class="mt-6 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.15em] text-slate-500">Selected Seats</p>
              <p class="mt-2 text-xl font-black text-brand-navy">
                {{ selectedSeatCodes.length > 0 ? selectedSeatCodes.join(", ") : "No seats selected" }}
              </p>
            </div>
            <div>
              <p class="text-xs font-black uppercase tracking-[0.15em] text-slate-500">Total</p>
              <p class="mt-2 text-2xl font-black text-brand-orange">{{ formatMoney(totalPrice) }}</p>
            </div>
            <button
              type="button"
              :disabled="selectedSeats.length === 0"
              @click="continueToCheckout"
              class="rounded-2xl bg-brand-navy px-8 py-4 text-sm font-black uppercase tracking-[0.2em] text-white transition hover:bg-sky-800 disabled:cursor-not-allowed disabled:bg-slate-300"
            >
              Checkout
            </button>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>
