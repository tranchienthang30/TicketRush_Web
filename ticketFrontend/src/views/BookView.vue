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
    .map(([rowLabel, seats]) => {
      const sortedSeats = seats.sort((a, b) => a.layoutX - b.layoutX);
      const units = [];

      for (let index = 0; index < sortedSeats.length; index += 1) {
        const current = sortedSeats[index];
        const next = sortedSeats[index + 1];

        if (isCoupleSeat(current) && next && isCoupleSeat(next) && next.seatNumber === current.seatNumber + 1) {
          units.push({
            key: `${current.id}-${next.id}`,
            seats: [current, next],
            label: `${current.rowLabel}${current.seatNumber}-${current.rowLabel}${next.seatNumber}`,
            type: "pair",
          });
          index += 1;
          continue;
        }

        units.push({
          key: current.id,
          seats: [current],
          label: current.seatCode,
          type: "single",
        });
      }

      const totalSpan = units.reduce((sum, unit) => sum + (unit.type === "pair" ? 2 : 1), 0);
      const offset = Math.max(0, Math.floor((14 - totalSpan) / 2));

      return { rowLabel, units, offset };
    });
});

const selectedSeatCodes = computed(() => {
  const selectedIds = new Set(selectedSeats.value.map((seat) => seat.id));
  const codes = [];

  for (const row of rowGroups.value) {
    for (const unit of row.units) {
      if (unit.seats.every((seat) => selectedIds.has(seat.id))) {
        codes.push(unit.label);
      }
    }
  }

  return codes;
});
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

function isCoupleSeat(seat) {
  return seat.seatTypeCode === "COUPLE" || seat.seatTypeCode === "SWEETBOX";
}

function canSelectSeatUnit(unit) {
  return unit.seats.every((seat) => seat.status === "AVAILABLE");
}

function isUnitSelected(unit) {
  return unit.seats.every((seat) => isSeatSelected(seat.id));
}

function toggleSeatUnit(unit) {
  if (!canSelectSeatUnit(unit)) return;

  if (isUnitSelected(unit)) {
    const removeIds = new Set(unit.seats.map((seat) => seat.id));
    selectedSeats.value = selectedSeats.value.filter((seat) => !removeIds.has(seat.id));
    return;
  }

  for (const seat of unit.seats) {
    if (!isSeatSelected(seat.id)) {
      selectedSeats.value.push(seat);
    }
  }
}

function seatClass(unit) {
  if (unit.seats.some((seat) => seat.status === "SOLD")) {
    return "bg-slate-500 border-slate-700 text-white cursor-not-allowed";
  }

  if (unit.seats.some((seat) => seat.status === "LOCKED")) {
    return "bg-amber-100 border-amber-300 text-amber-700 cursor-not-allowed";
  }

  if (isUnitSelected(unit)) {
    return "bg-blue-500 border-blue-700 text-white shadow";
  }

  const leadSeat = unit.seats[0];

  if (leadSeat.seatTypeCode === "COUPLE" || leadSeat.seatTypeCode === "SWEETBOX") {
    return "bg-rose-100 border-rose-300 text-rose-700 hover:bg-rose-200";
  }

  if (leadSeat.seatTypeCode === "VIP") {
    return "bg-orange-100 border-orange-300 text-orange-700 hover:bg-orange-200";
  }

  return "bg-slate-100 border-slate-300 text-slate-700 hover:bg-slate-200";
}

function seatUnitStyle(row, unitIndex) {
  if (unitIndex === 0 && row.offset > 0) {
    return { gridColumnStart: row.offset + 1 };
  }
  return null;
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
      throw new Error("No event found");
    }

    bookingEvent.value = await getBookingEvent(eventId);
    startTimer();
  } catch {
    bookingEvent.value = null;
    error.value = "Unable to load seat map. Please check event/backend data.";
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
              {{ eventDateLabel }} - {{ bookingEvent?.hallName || "Venue Area" }}
            </p>
          </div>

          <div class="grid gap-2 text-right">
            <p class="text-sm font-bold text-slate-700">
              Start time: <span class="text-brand-navy">{{ showtimeLabel }}</span>
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
            <p class="mt-3 text-center text-xs font-black uppercase tracking-[0.3em] text-slate-500">Stage / Venue</p>
          </div>

          <div class="overflow-x-auto">
            <div class="mx-auto min-w-[760px] max-w-4xl space-y-2">
              <div v-for="row in rowGroups" :key="row.rowLabel" class="flex items-center gap-2">
                <div class="w-6 text-center text-xs font-black text-slate-500">{{ row.rowLabel }}</div>
                <div class="grid w-[664px] grid-cols-[repeat(14,minmax(0,1fr))] gap-2">
                  <button
                    v-for="(unit, unitIndex) in row.units"
                    :key="unit.key"
                    type="button"
                    :disabled="!canSelectSeatUnit(unit)"
                    @click="toggleSeatUnit(unit)"
                    :style="seatUnitStyle(row, unitIndex)"
                    class="h-10 w-full rounded-lg border-b-[3px] text-[11px] font-bold transition"
                    :class="[seatClass(unit), unit.type === 'pair' ? 'col-span-2' : 'col-span-1']"
                  >
                    <span v-if="unit.seats.some((seat) => seat.status === 'SOLD')">X</span>
                    <span v-else-if="unit.seats.some((seat) => seat.status === 'LOCKED')">X</span>
                    <span v-else>{{ unit.label }}</span>
                  </button>
                </div>
                <div class="w-6 text-center text-xs font-black text-slate-500">{{ row.rowLabel }}</div>
              </div>
            </div>
          </div>

          <div class="mt-8 flex flex-wrap justify-center gap-5 text-sm font-bold text-slate-700">
            <div class="flex items-center gap-2"><span class="inline-block h-4 w-4 rounded bg-slate-500 border border-slate-700"></span> Booked</div>
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
