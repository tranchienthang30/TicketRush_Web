<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getBookingEvent, getEvents } from '../api/ticketRushApi';

const CHECKOUT_STORAGE_KEY = 'ticketrush_checkout_payload';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref('');
const bookingEvent = ref(null);
const selectedSeats = ref([]);

function formatPrice(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value || 0);
}

function formatEventDate(value) {
  if (!value) return { day: '--', month: '--', weekday: '--' };

  const date = new Date(value);
  return {
    day: String(date.getDate()).padStart(2, '0'),
    month: `Th. ${String(date.getMonth() + 1).padStart(2, '0')}`,
    weekday: new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(date),
  };
}

const eventDate = computed(() => formatEventDate(bookingEvent.value?.startTime));

const sectionRows = computed(() => {
  const sections = bookingEvent.value?.sections || [];

  return sections.map((section) => {
    const rowMap = new Map();

    for (const seat of section.seats || []) {
      if (!rowMap.has(seat.rowLabel)) {
        rowMap.set(seat.rowLabel, []);
      }
      rowMap.get(seat.rowLabel).push(seat);
    }

    const rows = [...rowMap.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([rowLabel, seats]) => ({
        rowLabel,
        seats: [...seats].sort((a, b) => a.seatNumber - b.seatNumber),
      }));

    return {
      ...section,
      rows,
    };
  });
});

const selectedSeatIds = computed(() => new Set(selectedSeats.value.map((seat) => seat.id)));

const totalPrice = computed(() => {
  return selectedSeats.value.reduce((sum, seat) => sum + Number(seat.price || 0), 0);
});

const selectedSeatLabel = computed(() => {
  if (selectedSeats.value.length === 0) return 'Chưa chọn ghế';

  return [...selectedSeats.value]
    .sort((a, b) => a.seatCode.localeCompare(b.seatCode))
    .map((seat) => seat.seatCode)
    .join(', ');
});

function sectionStyle(sectionName) {
  const normalized = String(sectionName || '').toLowerCase();
  if (normalized.includes('vip')) return 'vip';
  if (normalized.includes('couple')) return 'couple';
  return 'normal';
}

function toggleSeat(seat, sectionName) {
  if (seat.status !== 'AVAILABLE') return;

  const seatIndex = selectedSeats.value.findIndex((item) => item.id === seat.id);
  if (seatIndex >= 0) {
    selectedSeats.value.splice(seatIndex, 1);
    return;
  }

  selectedSeats.value.push({
    ...seat,
    sectionName,
  });
}

async function resolveEventId() {
  if (typeof route.query.eventId === 'string' && route.query.eventId.trim()) {
    return route.query.eventId.trim();
  }

  const page = await getEvents({ page: 0, size: 1 });
  return page?.content?.[0]?.id || null;
}

async function loadBookingEvent() {
  loading.value = true;
  error.value = '';
  selectedSeats.value = [];

  try {
    const eventId = await resolveEventId();
    if (!eventId) {
      error.value = 'Chưa có sự kiện nào để đặt vé.';
      bookingEvent.value = null;
      return;
    }

    bookingEvent.value = await getBookingEvent(eventId);

    if (route.query.eventId !== eventId) {
      router.replace({ path: '/booking', query: { eventId } });
    }
  } catch {
    error.value = 'Không tải được sơ đồ ghế. Vui lòng kiểm tra backend API.';
    bookingEvent.value = null;
  } finally {
    loading.value = false;
  }
}

function continueToCheckout() {
  if (!bookingEvent.value || selectedSeats.value.length === 0) return;

  const payload = {
    eventId: bookingEvent.value.eventId,
    seatIds: selectedSeats.value.map((seat) => seat.id),
    event: {
      title: bookingEvent.value.title,
      location: bookingEvent.value.location,
      startTime: bookingEvent.value.startTime,
      bannerUrl: bookingEvent.value.bannerUrl,
    },
  };

  sessionStorage.setItem(CHECKOUT_STORAGE_KEY, JSON.stringify(payload));
  router.push('/checkout');
}

onMounted(loadBookingEvent);
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-950 font-sans">
    <section class="relative h-[250px] md:h-[320px] w-full overflow-hidden">
      <div class="absolute inset-0">
        <img
          :src="bookingEvent?.bannerUrl || 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1600&q=80'"
          class="object-cover w-full h-full"
          alt="Banner"
        />
        <div class="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/80 to-slate-950/30"></div>
      </div>

      <div class="absolute inset-0 z-10 flex items-end px-6 md:px-12 pb-7">
        <div class="max-w-6xl mx-auto w-full text-white drop-shadow-[0_3px_12px_rgba(0,0,0,0.85)]">
          <p class="text-xs font-black tracking-[0.25em] uppercase text-brand-orange">Booking</p>
          <h1 class="mt-2 text-2xl md:text-4xl font-black tracking-tight uppercase">
            {{ bookingEvent?.title || 'Chọn ghế sự kiện' }}
          </h1>
          <p class="mt-2 text-slate-100 text-sm md:text-base font-semibold">
            {{ bookingEvent?.location || '---' }}
          </p>
        </div>
      </div>
    </section>

    <main class="max-w-7xl mx-auto py-8 px-4">
      <div v-if="loading" class="bg-white rounded-3xl border border-slate-200 p-8 text-center font-bold text-slate-500">
        Đang tải sơ đồ ghế...
      </div>

      <div v-else-if="error" class="bg-red-50 rounded-3xl border border-red-100 p-8 text-center font-bold text-red-700">
        {{ error }}
      </div>

      <template v-else-if="bookingEvent">
        <div class="flex justify-center mb-10">
          <div class="w-28 h-28 rounded-2xl border-2 bg-slate-900 border-slate-900 text-white shadow-lg flex flex-col items-center justify-center">
            <span class="text-[10px] uppercase font-bold opacity-70">{{ eventDate.month }}</span>
            <span class="text-3xl font-black my-1">{{ eventDate.day }}</span>
            <span class="text-[11px] font-medium capitalize">{{ eventDate.weekday }}</span>
          </div>
        </div>

        <div class="bg-white rounded-[40px] p-8 md:p-12 border border-slate-200 shadow-sm">
          <div class="relative w-full mb-14 flex flex-col items-center">
            <div class="w-2/3 h-2 bg-slate-300 rounded-full shadow-[0_10px_20px_rgba(0,0,0,0.05)]"></div>
            <span class="mt-4 text-slate-700 font-black tracking-[0.4em] text-xs uppercase">SCREEN</span>
          </div>

          <div class="space-y-8">
            <section v-for="section in sectionRows" :key="section.id">
              <h3 class="text-sm font-black uppercase tracking-[0.2em] text-slate-600 mb-4">
                {{ section.name }}
              </h3>

              <div class="overflow-x-auto pb-4">
                <div class="min-w-[640px] flex flex-col gap-3">
                  <div
                    v-for="row in section.rows"
                    :key="`${section.id}-${row.rowLabel}`"
                    class="flex items-center gap-3"
                  >
                    <div class="w-6 text-xs font-black text-slate-700">{{ row.rowLabel }}</div>

                    <div class="flex items-center gap-2">
                      <button
                        v-for="seat in row.seats"
                        :key="seat.id"
                        type="button"
                        class="w-9 h-9 md:w-10 md:h-10 rounded-lg flex items-center justify-center transition-all duration-200 text-[10px] font-bold border-b-4"
                        :class="[
                          seat.status !== 'AVAILABLE' ? 'bg-slate-200 border-slate-300 text-slate-600 cursor-not-allowed' : '',
                          seat.status === 'AVAILABLE' && !selectedSeatIds.has(seat.id) && sectionStyle(section.name) === 'normal' ? 'bg-white border-slate-300 text-slate-900 hover:bg-slate-100' : '',
                          seat.status === 'AVAILABLE' && !selectedSeatIds.has(seat.id) && sectionStyle(section.name) === 'vip' ? 'bg-orange-100 border-orange-300 text-orange-900 hover:bg-orange-200' : '',
                          seat.status === 'AVAILABLE' && !selectedSeatIds.has(seat.id) && sectionStyle(section.name) === 'couple' ? 'bg-red-100 border-red-300 text-red-900 hover:bg-red-200' : '',
                          selectedSeatIds.has(seat.id) ? 'bg-blue-600 border-blue-800 text-white -translate-y-1 shadow-md' : ''
                        ]"
                        @click="toggleSeat(seat, section.name)"
                      >
                        <span v-if="seat.status === 'AVAILABLE'">{{ seat.seatCode }}</span>
                        <span v-else>✖</span>
                      </button>
                    </div>

                    <div class="w-6 text-xs font-black text-slate-700">{{ row.rowLabel }}</div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <div class="flex flex-wrap justify-center gap-8 mt-12 text-xs font-black text-slate-800 uppercase tracking-wider">
            <div class="flex items-center gap-2"><div class="w-4 h-4 bg-white border border-slate-200 rounded"></div> Ghế thường</div>
            <div class="flex items-center gap-2"><div class="w-4 h-4 bg-orange-100 border border-orange-200 rounded"></div> Ghế VIP</div>
            <div class="flex items-center gap-2"><div class="w-4 h-4 bg-red-100 border border-red-200 rounded"></div> Ghế đôi</div>
            <div class="flex items-center gap-2"><div class="w-4 h-4 bg-blue-600 rounded"></div> Đang chọn</div>
            <div class="flex items-center gap-2"><div class="w-4 h-4 bg-slate-200 rounded"></div> Không khả dụng</div>
          </div>
        </div>

        <div class="mt-8 bg-white border border-slate-100 rounded-3xl p-6 shadow-xl flex flex-col md:flex-row items-center justify-between gap-6">
          <div class="flex items-center gap-6">
            <div class="hidden sm:block p-4 bg-slate-100 rounded-2xl text-slate-800">🎟️</div>
            <div>
              <p class="text-xs font-black text-slate-700 uppercase mb-1">Ghế đã chọn</p>
              <p class="text-xl font-black text-slate-900">{{ selectedSeatLabel }}</p>
            </div>
            <div class="h-10 w-[1px] bg-slate-100 mx-2 hidden md:block"></div>
            <div>
              <p class="text-xs font-black text-slate-700 uppercase mb-1">Tổng thanh toán</p>
              <p class="text-2xl font-black text-red-600">{{ formatPrice(totalPrice) }}</p>
            </div>
          </div>

          <button
            :disabled="selectedSeats.length === 0"
            @click="continueToCheckout"
            class="w-full md:w-auto px-12 py-4 bg-slate-900 text-white rounded-2xl font-black uppercase tracking-widest hover:bg-slate-800 disabled:bg-slate-200 disabled:text-slate-400 transition-all shadow-lg"
          >
            Tiếp tục thanh toán
          </button>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
::-webkit-scrollbar {
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}
</style>
