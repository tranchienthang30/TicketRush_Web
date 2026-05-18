<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { getMyTickets, getTicketDetail } from "../api/ticketRushApi";

const loading = ref(true);
const error = ref("");
const tickets = ref([]);
const selectedStatus = ref("upcoming");
const selectedTicket = ref(null);
const selectedTicketDetail = ref(null);
const detailLoading = ref(false);
const detailError = ref("");

const statusOptions = [
  { value: "upcoming", label: "Upcoming" },
  { value: "past", label: "Past" },
  { value: "all", label: "All" },
];

const statusLabelMap = {
  PENDING: "Pending",
  PAID: "Paid",
  EXPIRED: "Expired",
  CANCELLED: "Cancelled",
};

const statusClassMap = {
  PENDING: "bg-amber-100 text-amber-700 border-amber-200",
  PAID: "bg-emerald-100 text-emerald-700 border-emerald-200",
  EXPIRED: "bg-slate-200 text-slate-700 border-slate-300",
  CANCELLED: "bg-red-100 text-red-700 border-red-200",
};

async function loadTickets() {
  loading.value = true;
  error.value = "";

  try {
    tickets.value = (await getMyTickets({
      status: selectedStatus.value,
      limit: 50,
    })) || [];
  } catch {
    error.value = "Unable to load ticket list. Please check backend API.";
    tickets.value = [];
  } finally {
    loading.value = false;
  }
}

function formatDate(value) {
  if (!value) return "N/A";
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function statusLabel(status) {
  return statusLabelMap[status] || status || "N/A";
}

function statusClass(status) {
  return statusClassMap[status] || "bg-slate-100 text-slate-600 border-slate-200";
}

function ticketIdentity(ticket) {
  const shortOrder = String(ticket.orderId || "").slice(0, 8).toUpperCase();
  const seat = (ticket.seat || "N/A").replace(/\s+/g, "");
  return `TR-${shortOrder}-${seat}`;
}

function openTicketDetail(ticket) {
  selectedTicket.value = ticket;
  selectedTicketDetail.value = null;
  detailError.value = "";
  detailLoading.value = true;
  document.body.classList.add("overflow-hidden");

  getTicketDetail(ticket.orderId)
    .then((data) => {
      selectedTicketDetail.value = data;
    })
    .catch((err) => {
      detailError.value = err?.response?.data?.message || "Unable to load ticket details.";
    })
    .finally(() => {
      detailLoading.value = false;
    });
}

function closeTicketDetail() {
  selectedTicket.value = null;
  selectedTicketDetail.value = null;
  detailError.value = "";
  detailLoading.value = false;
  document.body.classList.remove("overflow-hidden");
}

const detailTickets = computed(() => selectedTicketDetail.value?.tickets || []);

function qrImageUrl(content) {
  if (!content) return "";
  return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(content)}`;
}

watch(selectedStatus, loadTickets);

onMounted(loadTickets);

onUnmounted(() => {
  document.body.classList.remove("overflow-hidden");
});
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 pb-20">
    <section class="bg-brand-navy text-white">
      <div class="max-w-7xl mx-auto px-4 md:px-8 py-14 md:py-16">
        <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
          My Tickets
        </p>
        <h1 class="mt-3 text-4xl md:text-5xl font-black tracking-tight">
          Your Tickets
        </h1>
        <p class="mt-4 max-w-2xl text-blue-100">
          Review all booked tickets, order statuses, and open each ticket to view check-in QR.
        </p>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 md:px-8 mt-8">
      <div class="flex flex-wrap gap-3">
        <button
          v-for="item in statusOptions"
          :key="item.value"
          type="button"
          @click="selectedStatus = item.value"
          class="rounded-full border px-5 py-2 text-sm font-black uppercase tracking-[0.15em] transition"
          :class="selectedStatus === item.value
            ? 'bg-brand-orange border-brand-orange text-white'
            : 'bg-white border-slate-200 text-slate-600 hover:border-slate-300 dark:bg-slate-800 dark:border-slate-700 dark:text-slate-300'"
        >
          {{ item.label }}
        </button>
      </div>

      <div
        v-if="loading"
        class="mt-6 rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
      >
        Loading tickets...
      </div>

      <div
        v-else-if="error"
        class="mt-6 rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-bold text-red-700"
      >
        {{ error }}
      </div>

      <div
        v-else-if="tickets.length === 0"
        class="mt-6 rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
      >
        No tickets found for the current filter.
      </div>

      <div v-else class="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="ticket in tickets"
          :key="`${ticket.orderId}-${ticket.seat}`"
          class="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-lg dark:border-slate-700 dark:bg-slate-800"
        >
          <div class="flex items-start justify-between gap-3">
            <p class="text-xs font-black uppercase tracking-[0.18em] text-brand-orange">
              {{ ticket.date }}
            </p>
            <span
              class="rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.13em]"
              :class="statusClass(ticket.status)"
            >
              {{ statusLabel(ticket.status) }}
            </span>
          </div>

          <h2 class="mt-3 line-clamp-2 text-xl font-black text-brand-navy dark:text-white">
            {{ ticket.title }}
          </h2>

          <div class="mt-4 space-y-2 text-sm text-slate-600 dark:text-slate-300">
            <p>Venue: {{ ticket.location }}</p>
            <p>Seat: {{ ticket.seat }}</p>
            <p>Ticket code: <span class="font-bold text-slate-700 dark:text-slate-100">{{ ticketIdentity(ticket) }}</span></p>
          </div>

          <button
            type="button"
            @click="openTicketDetail(ticket)"
            class="mt-5 w-full rounded-xl bg-brand-navy px-4 py-3 text-sm font-black uppercase tracking-[0.16em] text-white transition hover:bg-sky-800"
          >
            View details & QR
          </button>
        </article>
      </div>
    </section>

    <div
      v-if="selectedTicket"
      class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/70 p-4"
      @click.self="closeTicketDetail"
    >
      <div class="w-full max-w-3xl rounded-[2rem] bg-white p-6 md:p-8 shadow-2xl dark:bg-slate-800">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.18em] text-brand-orange">
              Ticket Detail
            </p>
            <h3 class="mt-2 text-2xl font-black text-brand-navy dark:text-white">
              {{ selectedTicket.title }}
            </h3>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-300">
              {{ formatDate(selectedTicket.startTime) }} - {{ selectedTicket.location }}
            </p>
          </div>

          <button
            type="button"
            @click="closeTicketDetail"
            class="rounded-full border border-slate-200 p-2 text-slate-500 transition hover:bg-slate-100 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
          >
            ✕
          </button>
        </div>

        <div
          v-if="detailLoading"
          class="mt-6 rounded-2xl border border-slate-200 p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:text-slate-300"
        >
          Loading ticket details...
        </div>

        <div
          v-else-if="detailError"
          class="mt-6 rounded-2xl border border-red-100 bg-red-50 p-6 text-sm font-bold text-red-700"
        >
          {{ detailError }}
        </div>

        <div v-else-if="selectedTicketDetail" class="mt-6 space-y-4">
          <div class="rounded-2xl border border-slate-200 p-5 dark:border-slate-700">
            <div class="space-y-3 text-sm text-slate-600 dark:text-slate-300">
              <p>
                Order ID:
                <span class="font-bold text-slate-800 dark:text-white">{{ selectedTicketDetail.orderId }}</span>
              </p>
              <p>
                Status:
                <span class="font-bold text-slate-800 dark:text-white">{{ statusLabel(selectedTicketDetail.orderStatus) }}</span>
              </p>
              <p>
                Total:
                <span class="font-bold text-slate-800 dark:text-white">{{ selectedTicketDetail.displayTotal }}</span>
              </p>
              <p>
                Event time:
                <span class="font-bold text-slate-800 dark:text-white">{{ selectedTicketDetail.date }}</span>
              </p>
            </div>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <article
              v-for="item in detailTickets"
              :key="item.orderItemId"
              class="rounded-2xl border border-slate-200 p-4 dark:border-slate-700"
            >
              <img
                :src="qrImageUrl(item.qrContent || item.qrCode)"
                alt="Ticket QR"
                class="mx-auto h-44 w-44 rounded-xl border border-slate-200 bg-white p-2"
              />
              <div class="mt-3 space-y-1 text-xs text-slate-600 dark:text-slate-300">
                <p>Seat: <span class="font-bold text-slate-800 dark:text-white">{{ item.seatCode }}</span></p>
                <p>Ticket status: <span class="font-bold text-slate-800 dark:text-white">{{ item.ticketStatus }}</span></p>
                <p>QR code: <span class="font-bold text-slate-800 dark:text-white">{{ item.qrCode || "N/A" }}</span></p>
              </div>
            </article>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
