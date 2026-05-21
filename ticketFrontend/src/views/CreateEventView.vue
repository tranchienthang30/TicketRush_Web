<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import * as eventApi from "@/api/event.api";
import * as providerApi from "@/api/provider.api";
import * as seatsioApi from "@/api/seatsio.api";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const currentStep = ref(1);
const categories = ref([]);
const loading = ref(false);
const uploadLoading = ref(false);
const error = ref("");
const message = ref("");
const seatsioWorkspace = ref(null);
const seatsioLoading = ref(false);
const seatsioMessage = ref("");
const seatsioError = ref("");
let seatsioScriptPromise = null;
let seatsioDesigner = null;

const form = reactive({
  title: "",
  description: "",
  genre: "",
  country: "",
  authorName: "",
  directorName: "",
  castMembers: "",
  performerNames: "",
  singerNames: "",
  bannerUrl: "",
  categoryId: "",
  durationMinutes: 120,
  listingType: "NOW_SHOWING",
  locationName: "",
  city: "",
  address: "",
  startTime: "",
  endTime: "",
  saleStartTime: "",
  saleEndTime: "",
  seatProvider: "INTERNAL",
  externalSeatChartKey: "",
  externalSeatWorkspaceKey: "",
  externalSeatEventKey: "",
  seatsioVenueType: "WITH_SECTIONS_AND_FLOORS",
  sections: [
    { name: "Standard", basePrice: 300000, rowCount: 5, seatsPerRow: 20 },
  ],
  internalSeatRows: [
    {
      rowLabel: "A",
      seatCount: 20,
      ranges: [
        { startSeat: 1, endSeat: 20, seatTypeCode: "STANDARD", seatTypeName: "Standard", visualColorHex: "#CBD5E1", price: 300000, status: "AVAILABLE", accessible: false },
      ],
    },
  ],
  payoutBankName: "",
  payoutAccountName: "",
  payoutAccountNumber: "",
  termsAccepted: false,
});

const isProviderReady = computed(() =>
  ["PROVIDER", "ADMIN"].includes(authStore.user?.role)
);
const isEditMode = computed(() => Boolean(route.params.id));
const isAdvancedSeatSetup = computed(() => currentStep.value === 2 && form.seatProvider === "SEATS_IO");

const providerRequestStatus = computed(() => authStore.user?.providerRequestStatus || null);
const seatTypeOptions = [
  { value: "STANDARD", label: "Standard", color: "#CBD5E1" },
  { value: "VIP", label: "VIP", color: "#F97316" },
  { value: "COUPLE", label: "Couple", color: "#E11D48" },
  { value: "SWEETBOX", label: "Sweetbox", color: "#E11D48" },
  { value: "WHEELCHAIR", label: "Accessible", color: "#16A34A" },
];

const configuredSeatTypes = computed(() => {
  const seen = new Set();
  const items = [];
  for (const row of form.internalSeatRows) {
    for (const range of row.ranges || []) {
      const label = String(range.seatTypeName || seatTypeLabel(range.seatTypeCode)).trim();
      const color = normalizeHexColor(range.visualColorHex) || seatTypeColor(range.seatTypeCode);
      const key = `${label.toUpperCase()}-${color}`;
      if (seen.has(key)) continue;
      seen.add(key);
      items.push({ label, color });
    }
  }
  return items.length > 0 ? items : [{ label: "Standard", color: "#CBD5E1" }];
});

onMounted(async () => {
  const response = await eventApi.getCategories();
  categories.value = response.data;
  if (isProviderReady.value) {
    await loadSeatsioWorkspace();
  }
  if (isEditMode.value) {
    await loadEventForEdit();
  }
});

watch(
  () => form.listingType,
  (listingType) => {
    if (listingType === "NOW_SHOWING" || listingType === "SPECIAL") {
      const nowValue = toDatetimeLocal(new Date().toISOString());
      if (!form.saleStartTime || new Date(form.saleStartTime).getTime() > Date.now()) {
        form.saleStartTime = nowValue;
      }
      if (!form.saleEndTime || new Date(form.saleEndTime).getTime() < Date.now()) {
        const eventEndMs = form.endTime ? new Date(form.endTime).getTime() : null;
        form.saleEndTime =
          Number.isFinite(eventEndMs) && eventEndMs > Date.now()
            ? form.endTime
            : toDatetimeLocal(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString());
      }
    }
  },
);

async function requestProviderVerification() {
  error.value = "";
  message.value = "";

  loading.value = true;
  try {
    const response = await providerApi.requestProviderAccess();
    authStore.setUser(response.data.data);
    message.value = response.data.message || "Provider request has been submitted.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to request provider access.";
  } finally {
    loading.value = false;
  }
}

function addSection() {
  form.sections.push({
    name: `Section ${form.sections.length + 1}`,
    basePrice: 300000,
    rowCount: form.seatProvider === "SEATS_IO" ? 1 : 3,
    seatsPerRow: form.seatProvider === "SEATS_IO" ? 1 : 20,
  });
}

function removeSection(index) {
  if (form.sections.length > 1) {
    form.sections.splice(index, 1);
  }
}

function applySeatsioCategories(categories) {
  const existingPrices = new Map(
    form.sections.map((section) => [
      String(section.name || "").trim().toLowerCase(),
      Number(section.basePrice || 0),
    ]),
  );
  const sections = categories
    .map((category) => {
      const key = String(category.key || "").trim();
      const label = String(category.label || key).trim();
      if (!key) return null;
      const price =
        existingPrices.get(key.toLowerCase()) ??
        existingPrices.get(label.toLowerCase()) ??
        0;
      return {
        name: key,
        label,
        color: category.color || "",
        basePrice: price,
        rowCount: 1,
        seatsPerRow: 1,
      };
    })
    .filter(Boolean);

  if (sections.length > 0) {
    form.sections.splice(0, form.sections.length, ...sections);
  }
}

function addSeatRow() {
  const label = nextRowLabel(form.internalSeatRows.length);
  form.internalSeatRows.push({
    rowLabel: label,
    seatCount: 20,
    ranges: [
      defaultSeatRange(1, 20),
    ],
  });
}

function removeSeatRow(index) {
  if (form.internalSeatRows.length > 1) {
    form.internalSeatRows.splice(index, 1);
  }
}

function addSeatRange(row) {
  row.ranges.push({
    ...defaultSeatRange(1, Math.min(Number(row.seatCount || 1), 1)),
  });
}

function removeSeatRange(row, rangeIndex) {
  if (row.ranges.length > 1) {
    row.ranges.splice(rangeIndex, 1);
  }
}

function defaultSeatRange(startSeat = 1, endSeat = 1) {
  return {
    startSeat,
    endSeat,
    seatTypeCode: "STANDARD",
    seatTypeName: "Standard",
    visualColorHex: "#CBD5E1",
    price: 300000,
    status: "AVAILABLE",
    accessible: false,
  };
}

function seatTypeOption(code) {
  return seatTypeOptions.find((type) => type.value === code) || seatTypeOptions[0];
}

function seatTypeLabel(code) {
  return seatTypeOption(code).label;
}

function seatTypeColor(code) {
  return seatTypeOption(code).color;
}

function normalizeHexColor(color) {
  const normalized = String(color || "").trim().toUpperCase();
  return /^#[0-9A-F]{6}$/.test(normalized) ? normalized : "";
}

function applySeatTypePreset(range) {
  const option = seatTypeOption(range.seatTypeCode);
  if (!range.seatTypeName || seatTypeOptions.some((type) => type.label === range.seatTypeName)) {
    range.seatTypeName = option.label;
  }
  if (!normalizeHexColor(range.visualColorHex) || seatTypeOptions.some((type) => type.color === range.visualColorHex)) {
    range.visualColorHex = option.color;
  }
  range.accessible = range.seatTypeCode === "WHEELCHAIR" ? true : Boolean(range.accessible);
}

function nextStep() {
  error.value = "";
  if (currentStep.value === 1 && !isStepOneValid()) {
    error.value = "Please complete event information before continuing.";
    return;
  }
  if (currentStep.value === 2 && !isStepTwoValid()) {
    error.value = "Please complete seating configuration before continuing.";
    return;
  }
  currentStep.value += 1;
  if (currentStep.value === 2 && form.seatProvider === "SEATS_IO") {
    nextTick(renderSeatsioDesigner);
  }
}

function previousStep() {
  error.value = "";
  currentStep.value -= 1;
}

async function submitEvent() {
  error.value = "";
  if (!form.termsAccepted) {
    error.value = "You must accept the provider terms before submitting.";
    return;
  }

  loading.value = true;
  try {
    if (isEditMode.value) {
      await eventApi.updateEvent(route.params.id, toPayload());
      router.push("/my-events");
      return;
    }
    const response = await eventApi.createEvent(toPayload());
    router.push(`/events/${response.data.slug}`);
  } catch (err) {
    error.value = err.response?.data?.message || (isEditMode.value ? "Unable to update event." : "Unable to create event.");
  } finally {
    loading.value = false;
  }
}

async function loadEventForEdit() {
  loading.value = true;
  error.value = "";
  try {
    const response = await eventApi.getMyEvent(route.params.id);
    fillForm(response.data);
    if (form.seatProvider === "SEATS_IO") {
      await nextTick();
      await renderSeatsioDesigner();
    }
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to load event for editing.";
  } finally {
    loading.value = false;
  }
}

async function uploadBanner(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  uploadLoading.value = true;
  error.value = "";
  try {
    const response = await eventApi.uploadEventBanner(file);
    form.bannerUrl = response.data.url;
    message.value = "Banner image uploaded.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to upload banner image.";
  } finally {
    uploadLoading.value = false;
    event.target.value = "";
  }
}

function isStepOneValid() {
  return Boolean(
    form.title &&
      form.categoryId &&
      form.durationMinutes > 0 &&
      form.locationName &&
      form.city &&
      form.startTime &&
      form.endTime,
  );
}

function isStepTwoValid() {
  if (form.seatProvider === "SEATS_IO") {
    return Boolean(form.externalSeatWorkspaceKey && form.externalSeatChartKey && form.externalSeatEventKey) && areSectionsValid();
  }
  return areInternalSeatRowsValid();
}

function areSectionsValid() {
  return form.sections.length > 0 && form.sections.every((section) =>
    section.name && section.basePrice >= 0 && section.rowCount > 0 && section.seatsPerRow > 0
  );
}

function areInternalSeatRowsValid() {
  const labels = new Set();
  return form.internalSeatRows.length > 0 && form.internalSeatRows.every((row) => {
    const label = String(row.rowLabel || "").trim().toUpperCase();
    if (!label || labels.has(label) || Number(row.seatCount) < 1) return false;
    labels.add(label);
    const usedSeats = new Set();
    return row.ranges.length > 0 && row.ranges.every((range) => {
      const start = Number(range.startSeat);
      const end = Number(range.endSeat);
      if (
        !range.seatTypeCode ||
        !String(range.seatTypeName || "").trim() ||
        !normalizeHexColor(range.visualColorHex) ||
        Number(range.price) < 0 ||
        start < 1 ||
        end < start ||
        end > Number(row.seatCount)
      ) {
        return false;
      }
      for (let seat = start; seat <= end; seat += 1) {
        if (usedSeats.has(seat)) return false;
        usedSeats.add(seat);
      }
      return true;
    });
  });
}

function toPayload() {
  return {
    title: form.title,
    description: form.description || null,
    genre: form.genre || null,
    country: form.country || null,
    authorName: form.authorName || null,
    directorName: form.directorName || null,
    castMembers: form.castMembers || null,
    performerNames: form.performerNames || null,
    singerNames: form.singerNames || null,
    bannerUrl: form.bannerUrl || null,
    categoryId: Number(form.categoryId),
    durationMinutes: Number(form.durationMinutes),
    listingType: form.listingType,
    locationName: form.locationName,
    city: form.city,
    address: form.address || null,
    startTime: toInstant(form.startTime),
    endTime: toInstant(form.endTime),
    saleStartTime: form.saleStartTime ? toInstant(form.saleStartTime) : null,
    saleEndTime: form.saleEndTime ? toInstant(form.saleEndTime) : null,
    seatProvider: form.seatProvider,
    externalSeatChartKey: form.seatProvider === "SEATS_IO" ? form.externalSeatChartKey : null,
    externalSeatWorkspaceKey: form.seatProvider === "SEATS_IO" ? form.externalSeatWorkspaceKey : null,
    externalSeatEventKey: form.seatProvider === "SEATS_IO" ? form.externalSeatEventKey : null,
    payoutBankName: form.payoutBankName || null,
    payoutAccountName: form.payoutAccountName || null,
    payoutAccountNumber: form.payoutAccountNumber || null,
    termsAccepted: form.termsAccepted,
    sections: form.sections.map((section) => ({
      name: section.name,
      basePrice: Number(section.basePrice),
      rowCount: form.seatProvider === "SEATS_IO" ? 1 : Number(section.rowCount),
      seatsPerRow: form.seatProvider === "SEATS_IO" ? 1 : Number(section.seatsPerRow),
    })),
    internalSeatRows: form.seatProvider === "INTERNAL"
      ? form.internalSeatRows.map((row) => ({
        rowLabel: String(row.rowLabel || "").trim().toUpperCase(),
        seatCount: Number(row.seatCount),
        ranges: row.ranges.map((range) => ({
          startSeat: Number(range.startSeat),
          endSeat: Number(range.endSeat),
          seatTypeCode: range.seatTypeCode,
          seatTypeName: String(range.seatTypeName || seatTypeLabel(range.seatTypeCode)).trim(),
          visualColorHex: normalizeHexColor(range.visualColorHex) || seatTypeColor(range.seatTypeCode),
          price: Number(range.price),
          status: range.status,
          accessible: Boolean(range.accessible),
        })),
      }))
      : null,
  };
}

function toInstant(value) {
  return new Date(value).toISOString();
}

function fillForm(event) {
  form.title = event.title || "";
  form.description = event.description || "";
  form.genre = event.genre || "";
  form.country = event.country || "";
  form.authorName = event.authorName || "";
  form.directorName = event.directorName || "";
  form.castMembers = event.castMembers || "";
  form.performerNames = event.performerNames || "";
  form.singerNames = event.singerNames || "";
  form.bannerUrl = event.bannerUrl || "";
  form.categoryId = event.categoryId || "";
  form.durationMinutes = event.durationMinutes || 120;
  form.listingType = event.listingType || "NOW_SHOWING";
  form.locationName = event.locationName || "";
  form.city = event.city || "";
  form.address = event.address || "";
  form.startTime = toDatetimeLocal(event.startTime);
  form.endTime = toDatetimeLocal(event.endTime);
  form.saleStartTime = toDatetimeLocal(event.saleStartTime);
  form.saleEndTime = toDatetimeLocal(event.saleEndTime);
  form.seatProvider = event.seatProvider || "INTERNAL";
  form.externalSeatChartKey = event.externalSeatChartKey || "";
  form.externalSeatWorkspaceKey = event.externalSeatWorkspaceKey || "";
  form.externalSeatEventKey = event.externalSeatEventKey || "";
  form.sections = Array.isArray(event.sections) && event.sections.length > 0
    ? event.sections.map((section) => ({
      name: section.name,
      basePrice: Number(section.basePrice || 0),
      rowCount: event.seatProvider === "SEATS_IO" ? 1 : Number(section.rowCount || 1),
      seatsPerRow: event.seatProvider === "SEATS_IO" ? 1 : Number(section.seatsPerRow || 1),
    }))
    : [{ name: "Standard", basePrice: 300000, rowCount: 1, seatsPerRow: 1 }];
  form.internalSeatRows = Array.isArray(event.internalSeatRows) && event.internalSeatRows.length > 0
    ? event.internalSeatRows.map((row) => ({
      rowLabel: row.rowLabel,
      seatCount: Number(row.seatCount || 1),
      ranges: Array.isArray(row.ranges) && row.ranges.length > 0
        ? row.ranges.map((range) => ({
          startSeat: Number(range.startSeat),
          endSeat: Number(range.endSeat),
          seatTypeCode: range.seatTypeCode || "STANDARD",
          seatTypeName: range.seatTypeName || seatTypeLabel(range.seatTypeCode || "STANDARD"),
          visualColorHex: normalizeHexColor(range.visualColorHex) || seatTypeColor(range.seatTypeCode || "STANDARD"),
          price: Number(range.price || 0),
          status: range.status === "SOLD" ? "SOLD" : "AVAILABLE",
          accessible: Boolean(range.accessible),
        }))
        : [{ ...defaultSeatRange(1, Number(row.seatCount || 1)), price: Number(event.minPrice || 300000) }],
    }))
    : [
      {
        rowLabel: "A",
        seatCount: 20,
        ranges: [
          { ...defaultSeatRange(1, 20), price: Number(event.minPrice || 300000) },
        ],
      },
    ];
  form.termsAccepted = true;
}

function toDatetimeLocal(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function nextRowLabel(index) {
  let label = "";
  let value = index;
  do {
    label = String.fromCharCode(65 + (value % 26)) + label;
    value = Math.floor(value / 26) - 1;
  } while (value >= 0);
  return label;
}

function useSeatProvider(provider) {
  form.seatProvider = provider;
  seatsioError.value = "";
  if (provider === "SEATS_IO") {
    if (!seatsioWorkspace.value) {
      loadSeatsioWorkspace();
    }
    nextTick(renderSeatsioDesigner);
  } else {
    destroySeatsioDesigner();
  }
}

async function loadSeatsioWorkspace() {
  seatsioError.value = "";
  try {
    const response = await seatsioApi.getWorkspace();
    applyWorkspace(response.data);
  } catch (err) {
    seatsioError.value = err.response?.data?.message || "Unable to load seats.io workspace.";
  }
}

async function ensureSeatsioWorkspace() {
  seatsioLoading.value = true;
  seatsioError.value = "";
  seatsioMessage.value = "";
  try {
    const response = await seatsioApi.ensureWorkspace();
    applyWorkspace(response.data);
    seatsioMessage.value = "Seats.io workspace is ready for this provider.";
    await nextTick();
    await renderSeatsioDesigner();
  } catch (err) {
    seatsioError.value = err.response?.data?.message || "Unable to create seats.io workspace.";
  } finally {
    seatsioLoading.value = false;
  }
}

async function createSeatsioChart() {
  seatsioLoading.value = true;
  seatsioError.value = "";
  seatsioMessage.value = "";
  try {
    const response = await seatsioApi.createChart({
      name: form.title || "TicketRush chart",
      venueType: form.seatsioVenueType,
    });
    form.externalSeatChartKey = response.data.key;
    seatsioMessage.value = "Chart created. Use the designer below to publish the seating layout.";
    await syncSeatsioCategoriesFromChart({ silent: true });
    await nextTick();
    await renderSeatsioDesigner();
  } catch (err) {
    seatsioError.value = err.response?.data?.message || "Unable to create seats.io chart.";
  } finally {
    seatsioLoading.value = false;
  }
}

async function createSeatsioEventFromChart() {
  if (!form.externalSeatChartKey) {
    seatsioError.value = "Create or enter a chart key first.";
    return;
  }
  seatsioLoading.value = true;
  seatsioError.value = "";
  seatsioMessage.value = "";
  try {
    if (form.externalSeatEventKey) {
      await syncSeatsioCategoriesFromChart({ silent: true });
      seatsioMessage.value = "Seats.io event key is already attached. Categories were synced from the chart.";
      return;
    }
    const response = await seatsioApi.createSeatsioEvent({
      chartKey: form.externalSeatChartKey,
      name: form.title || "TicketRush event",
      date: form.startTime ? form.startTime.slice(0, 10) : null,
    });
    const eventKey = response.data?.eventKey || response.data?.key;
    if (!eventKey) {
      throw new Error("Seats.io event was created but no event key was returned.");
    }
    form.externalSeatEventKey = eventKey;
    await syncSeatsioCategoriesFromChart({ silent: true });
    seatsioMessage.value = "Seats.io chart saved and event key attached.";
  } catch (err) {
    seatsioError.value = err.response?.data?.message || err.message || "Unable to create seats.io event.";
  } finally {
    seatsioLoading.value = false;
  }
}

async function syncSeatsioCategoriesFromChart({ silent = false } = {}) {
  if (!form.externalSeatChartKey) {
    if (!silent) {
      seatsioError.value = "Create or enter a chart key first.";
    }
    return;
  }
  if (!silent) {
    seatsioLoading.value = true;
    seatsioError.value = "";
    seatsioMessage.value = "";
  }
  try {
    const response = await seatsioApi.getChartCategories(form.externalSeatChartKey);
    const chartCategories = Array.isArray(response.data) ? response.data : [];
    if (chartCategories.length === 0) {
      if (!silent) {
        seatsioError.value = "No seats.io categories were found for this chart.";
      }
      return;
    }
    applySeatsioCategories(chartCategories);
    if (!silent) {
      seatsioMessage.value = "Seats.io categories loaded. Add prices before continuing.";
    }
  } catch (err) {
    if (!silent) {
      seatsioError.value = err.response?.data?.message || "Unable to load seats.io categories.";
    }
  } finally {
    if (!silent) {
      seatsioLoading.value = false;
    }
  }
}

function applyWorkspace(workspace) {
  seatsioWorkspace.value = workspace;
  if (workspace?.workspaceKey && !form.externalSeatWorkspaceKey) {
    form.externalSeatWorkspaceKey = workspace.workspaceKey;
  }
}

async function renderSeatsioDesigner() {
  if (
    form.seatProvider !== "SEATS_IO" ||
    !form.externalSeatChartKey ||
    !seatsioWorkspace.value?.secretKey
  ) {
    return;
  }
  try {
    await loadSeatsioScript(seatsioWorkspace.value.cdnUrl);
    destroySeatsioDesigner();
    seatsioDesigner = new window.seatsio.SeatingChartDesigner({
      divId: "seatsio-designer",
      secretKey: seatsioWorkspace.value.secretKey,
      chartKey: form.externalSeatChartKey,
    }).render();
  } catch (err) {
    seatsioError.value = err?.message || "Unable to load seats.io designer.";
  }
}

function destroySeatsioDesigner() {
  if (seatsioDesigner?.destroy) {
    seatsioDesigner.destroy();
  }
  seatsioDesigner = null;
}

function loadSeatsioScript(cdnUrl) {
  if (window.seatsio) return Promise.resolve();
  if (seatsioScriptPromise) return seatsioScriptPromise;

  seatsioScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = cdnUrl || "https://cdn-oc.seatsio.net/chart.js";
    script.async = true;
    script.onload = resolve;
    script.onerror = () => reject(new Error("Unable to load seats.io chart.js"));
    document.head.appendChild(script);
  });
  return seatsioScriptPromise;
}
</script>

<template>
  <section class="min-h-screen bg-brand-light dark:bg-slate-900 px-4 py-8">
    <div class="max-w-7xl mx-auto">
      <div v-if="!isProviderReady" class="max-w-2xl mx-auto bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white mb-3">Provider approval required</h1>
        <p class="text-slate-500 dark:text-slate-400 mb-6">
          To create events, request provider access and wait for admin approval. TicketRush will send an email verification link again when you submit the request.
        </p>

        <form class="space-y-5" @submit.prevent="requestProviderVerification">
          <p
            v-if="providerRequestStatus === 'PENDING'"
            class="text-sm text-orange-700 bg-orange-50 border border-orange-100 rounded-xl px-4 py-3"
          >
            Waiting for admin's approval. You can continue booking tickets as a customer.
          </p>

          <p v-if="message" class="text-sm text-green-700 bg-green-50 border border-green-100 rounded-xl px-4 py-3">{{ message }}</p>
          <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">{{ error }}</p>

          <button
            v-if="providerRequestStatus !== 'PENDING'"
            type="submit"
            :disabled="loading"
            class="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-60 text-white font-black py-3 rounded-xl transition"
          >
            {{ loading ? "Submitting..." : "Becoming Providers" }}
          </button>
          <RouterLink to="/profile" class="block text-center text-sm font-black text-brand-orange hover:underline">
            Go to profile
          </RouterLink>
        </form>
      </div>

      <div
        v-else
        :class="[
          'grid grid-cols-1 transition-all duration-200',
          isAdvancedSeatSetup ? 'gap-4 xl:grid-cols-[88px_minmax(0,1fr)]' : 'gap-8 lg:grid-cols-[280px_1fr]',
        ]"
      >
        <aside
          :class="[
            'bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 h-fit sticky top-24 transition-all duration-200',
            isAdvancedSeatSetup ? 'rounded-xl p-3' : 'rounded-2xl p-5',
          ]"
        >
          <h2
            :class="[
              'font-black text-brand-navy dark:text-white',
              isAdvancedSeatSetup ? 'mb-3 text-center text-xs uppercase tracking-[0.18em]' : 'mb-5',
            ]"
          >
            {{ isAdvancedSeatSetup ? "Steps" : isEditMode ? "Edit event" : "Create event" }}
          </h2>
          <ol :class="isAdvancedSeatSetup ? 'flex justify-center gap-2 xl:flex-col xl:items-center' : 'space-y-3'">
            <li v-for="step in [
              { id: 1, label: 'Event information' },
              { id: 2, label: 'Seat setup' },
              { id: 3, label: 'Payment & confirmation' }
            ]" :key="step.id" :title="step.label" :class="isAdvancedSeatSetup ? 'flex justify-center' : 'flex items-center gap-3'">
              <span
                :class="[
                  currentStep === step.id ? 'bg-brand-orange text-white' : 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-300',
                  isAdvancedSeatSetup ? 'h-9 w-9' : 'h-8 w-8',
                ]"
                class="grid place-items-center rounded-full text-sm font-black"
              >
                {{ step.id }}
              </span>
              <span
                v-if="!isAdvancedSeatSetup"
                :class="currentStep === step.id ? 'text-brand-orange' : 'text-slate-600 dark:text-slate-300'"
                class="font-bold"
              >
                {{ step.label }}
              </span>
            </li>
          </ol>
        </aside>

        <main
          :class="[
            'bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm transition-all duration-200',
            isAdvancedSeatSetup ? 'p-3 md:p-4' : 'p-6 md:p-8',
          ]"
        >
          <div
            :class="[
              'rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 transition-all duration-200',
              isAdvancedSeatSetup ? 'mb-4 p-3 md:flex md:items-center md:justify-between md:gap-4' : 'mb-6 p-5',
            ]"
          >
            <div>
              <h1
                :class="[
                  'font-black text-brand-navy dark:text-white',
                  isAdvancedSeatSetup ? 'text-base' : 'mb-2 text-2xl',
                ]"
              >
                {{ isAdvancedSeatSetup ? "Advanced seat setup" : isEditMode ? "Update event" : "Provider rules" }}
              </h1>
              <p v-if="isAdvancedSeatSetup" class="mt-1 text-xs font-bold text-slate-500 dark:text-slate-300">
                Configure workspace, chart, seats.io event, then map categories to TicketRush prices.
              </p>
            </div>
            <div v-if="isAdvancedSeatSetup" class="mt-3 flex flex-wrap gap-2 md:mt-0">
              <span class="rounded-lg bg-white px-3 py-1.5 text-xs font-black text-slate-600 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:ring-slate-700">
                Workspace {{ form.externalSeatWorkspaceKey ? "ready" : "missing" }}
              </span>
              <span class="rounded-lg bg-white px-3 py-1.5 text-xs font-black text-slate-600 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:ring-slate-700">
                Chart {{ form.externalSeatChartKey ? "ready" : "missing" }}
              </span>
              <span class="rounded-lg bg-white px-3 py-1.5 text-xs font-black text-slate-600 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:ring-slate-700">
                Event {{ form.externalSeatEventKey ? "ready" : "missing" }}
              </span>
            </div>
            <ul v-else class="text-sm text-slate-600 dark:text-slate-300 space-y-1 list-disc pl-5">
              <li>Use accurate event information, official images, venue, and sale period.</li>
              <li>Ticket sections and seat labels must match the actual venue setup.</li>
              <li>Payout information must belong to the approved provider account.</li>
              <li>This MVP publishes events immediately; admin review for event content can be added later.</li>
            </ul>
          </div>

          <p v-if="error" class="mb-5 text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">{{ error }}</p>

          <div v-if="currentStep === 1" class="space-y-5">
            <h2 class="text-xl font-black text-slate-900 dark:text-white">Event information</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <label class="block md:col-span-2">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Title</span>
                <input v-model.trim="form.title" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block md:col-span-2">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Description</span>
                <textarea v-model.trim="form.description" rows="4" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white"></textarea>
              </label>
              <label class="block md:col-span-2">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Banner image URL</span>
                <input v-model.trim="form.bannerUrl" placeholder="https://..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block md:col-span-2">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Upload banner image</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" class="w-full px-4 py-3 rounded-xl border border-dashed border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" @change="uploadBanner" />
                <span class="mt-2 block text-xs font-bold text-slate-500">
                  {{ uploadLoading ? "Uploading..." : "JPG, PNG, or WebP up to 5MB." }}
                </span>
              </label>
              <div v-if="form.bannerUrl" class="md:col-span-2 overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-100">
                <img :src="form.bannerUrl" alt="Event banner preview" class="h-56 w-full object-cover" />
              </div>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Genre</span>
                <input v-model.trim="form.genre" placeholder="Pop concert, comedy, animation..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Country</span>
                <input v-model.trim="form.country" placeholder="Vietnam, Japan, South Korea..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Author / creator</span>
                <input v-model.trim="form.authorName" placeholder="Writer, creator, organizer..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Director</span>
                <input v-model.trim="form.directorName" placeholder="Director, stage director..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block md:col-span-2">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Cast / speakers</span>
                <input v-model.trim="form.castMembers" placeholder="Actors, speakers, teams..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Performers</span>
                <input v-model.trim="form.performerNames" placeholder="Band, dance crew, host..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Singers</span>
                <input v-model.trim="form.singerNames" placeholder="Singer, vocalist..." class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Category</span>
                <select v-model="form.categoryId" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white">
                  <option value="">Select category</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
                </select>
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Duration (minutes)</span>
                <input v-model.number="form.durationMinutes" type="number" min="1" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Event section</span>
                <select v-model="form.listingType" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white">
                  <option value="NOW_SHOWING">Now Showing</option>
                  <option value="UPCOMING">Upcoming</option>
                  <option value="SPECIAL">Special</option>
                </select>
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Venue</span>
                <input v-model.trim="form.locationName" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">City</span>
                <input v-model.trim="form.city" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Address</span>
                <input v-model.trim="form.address" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Start time</span>
                <input v-model="form.startTime" type="datetime-local" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">End time</span>
                <input v-model="form.endTime" type="datetime-local" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Sale start</span>
                <input v-model="form.saleStartTime" type="datetime-local" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Sale end</span>
                <input v-model="form.saleEndTime" type="datetime-local" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
              </label>
            </div>
          </div>

          <div v-if="currentStep === 2" class="space-y-5">
            <h2 class="text-xl font-black text-slate-900 dark:text-white">Seat setup</h2>
            <div class="grid grid-cols-2 gap-3 max-w-md">
              <button type="button" :disabled="isEditMode" :class="form.seatProvider === 'INTERNAL' ? 'bg-brand-orange text-white' : 'bg-slate-100 dark:bg-slate-700 dark:text-white'" class="rounded-xl px-4 py-3 font-bold disabled:opacity-60" @click="useSeatProvider('INTERNAL')">Simple</button>
              <button type="button" :disabled="isEditMode" :class="form.seatProvider === 'SEATS_IO' ? 'bg-brand-orange text-white' : 'bg-slate-100 dark:bg-slate-700 dark:text-white'" class="rounded-xl px-4 py-3 font-bold disabled:opacity-60" @click="useSeatProvider('SEATS_IO')">Advanced</button>
            </div>

            <div v-if="form.seatProvider === 'SEATS_IO'" class="space-y-5">
              <div class="rounded-xl border border-blue-200 bg-blue-50 p-5 text-blue-950">
                <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div>
                    <p class="font-black">seats.io provider workspace</p>
                    <p class="mt-1 text-sm">
                      Each provider gets a separate workspace. TicketRush keeps the secret key server-side and uses it only inside provider tools.
                    </p>
                    <p v-if="seatsioWorkspace?.workspaceKey" class="mt-2 text-xs font-bold text-blue-800">
                      Workspace: {{ seatsioWorkspace.workspaceName }} / {{ seatsioWorkspace.workspaceKey }}
                    </p>
                    <p v-else-if="seatsioWorkspace?.message" class="mt-2 text-xs font-bold text-blue-800">
                      {{ seatsioWorkspace.message }}
                    </p>
                  </div>
                  <button
                    type="button"
                    :disabled="seatsioLoading"
                    class="rounded-xl bg-brand-navy px-4 py-3 text-sm font-black text-white disabled:opacity-60"
                    @click="ensureSeatsioWorkspace"
                  >
                    {{ seatsioLoading ? "Working..." : "Prepare workspace" }}
                  </button>
                </div>
                <p v-if="seatsioMessage" class="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm font-bold text-green-700">{{ seatsioMessage }}</p>
                <p v-if="seatsioError" class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm font-bold text-red-700">{{ seatsioError }}</p>
              </div>

              <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
                <label class="block">
                  <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Workspace key</span>
                  <input v-model.trim="form.externalSeatWorkspaceKey" placeholder="public workspace key" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
                </label>
                <label class="block">
                  <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Chart key</span>
                  <input v-model.trim="form.externalSeatChartKey" placeholder="chart key" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" @change="renderSeatsioDesigner" />
                </label>
                <label class="block">
                  <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Event key</span>
                  <input v-model.trim="form.externalSeatEventKey" placeholder="event key" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white" />
                </label>
              </div>

              <div class="grid grid-cols-1 gap-3 md:grid-cols-[1fr_auto_auto] md:items-end">
                <label class="block">
                  <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Chart type</span>
                  <select v-model="form.seatsioVenueType" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white">
                    <option value="WITH_SECTIONS_AND_FLOORS">Sections and floors</option>
                    <option value="SIMPLE">Simple</option>
                    <option value="WITH_ZONES">Zones</option>
                  </select>
                </label>
                <button type="button" :disabled="seatsioLoading" class="rounded-xl border border-brand-orange px-4 py-3 font-black text-brand-orange disabled:opacity-60" @click="createSeatsioChart">
                  Create chart
                </button>
                <button type="button" :disabled="seatsioLoading || !form.externalSeatChartKey" class="rounded-xl bg-brand-orange px-4 py-3 font-black text-white disabled:opacity-60" @click="createSeatsioEventFromChart">
                  Save chart
                </button>
              </div>

              <div class="rounded-xl border border-slate-200 dark:border-slate-700 p-3">
                <div class="mb-3 flex items-center justify-between gap-3">
                  <p class="font-black text-slate-900 dark:text-white">Chart designer</p>
                  <button type="button" class="text-sm font-black text-brand-orange hover:underline" @click="renderSeatsioDesigner">
                    Reload designer
                  </button>
                </div>
                <div class="overflow-x-auto">
                  <div id="seatsio-designer" class="h-[calc(100vh-260px)] min-h-[680px] min-w-[1180px] overflow-visible rounded-xl bg-slate-100 dark:bg-slate-900"></div>
                </div>
              </div>

              <div class="space-y-4">
                <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div>
                    <p class="font-black text-slate-900 dark:text-white">Ticket categories shown in TicketRush checkout</p>
                    <p class="mt-1 text-sm font-bold text-slate-500 dark:text-slate-300">
                      Category must match the seats.io category key. Load from chart, then set the TicketRush price for each category.
                    </p>
                  </div>
                  <button
                    type="button"
                    :disabled="seatsioLoading || !form.externalSeatChartKey"
                    class="rounded-xl border border-brand-orange px-4 py-2 text-sm font-black text-brand-orange disabled:opacity-60"
                    @click="syncSeatsioCategoriesFromChart()"
                  >
                    Load from chart
                  </button>
                </div>
                <div v-for="(section, index) in form.sections" :key="index" class="grid grid-cols-1 md:grid-cols-5 gap-3 rounded-xl border border-slate-200 dark:border-slate-700 p-4">
                  <label class="block md:col-span-2">
                    <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Category</span>
                    <input v-model.trim="section.name" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                  </label>
                  <label class="block">
                    <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Price</span>
                    <input v-model.number="section.basePrice" type="number" min="0" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                  </label>
                  <input v-model.number="section.rowCount" type="hidden" />
                  <input v-model.number="section.seatsPerRow" type="hidden" />
                  <button type="button" class="md:col-span-5 text-left text-sm font-bold text-red-600" @click="removeSection(index)">Remove category</button>
                </div>
                <button type="button" class="rounded-xl border border-brand-orange px-4 py-2 font-bold text-brand-orange" @click="addSection">Add category</button>
              </div>
            </div>

            <div v-else class="space-y-4">
              <div class="flex flex-wrap gap-4 rounded-xl border border-slate-200 p-4 text-sm font-bold text-slate-600 dark:border-slate-700 dark:text-slate-300">
                <span v-for="type in configuredSeatTypes" :key="`${type.label}-${type.color}`" class="flex items-center gap-2">
                  <span class="h-4 w-4 rounded" :style="{ backgroundColor: type.color }"></span>
                  {{ type.label }}
                </span>
              </div>

              <div v-for="(row, rowIndex) in form.internalSeatRows" :key="rowIndex" class="space-y-4 rounded-xl border border-slate-200 dark:border-slate-700 p-4">
                <div class="grid grid-cols-1 gap-3 md:grid-cols-[120px_160px_1fr_auto] md:items-end">
                  <label class="block">
                    <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Row</span>
                    <input v-model.trim="row.rowLabel" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                  </label>
                  <label class="block">
                    <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Seats in row</span>
                    <input v-model.number="row.seatCount" type="number" min="1" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                  </label>
                  <p class="text-sm font-bold text-slate-500 dark:text-slate-300">
                    Configure non-overlapping ranges inside this row. Seat cells shown to customers keep labels like {{ row.rowLabel || 'A' }}1.
                  </p>
                  <button type="button" class="text-sm font-bold text-red-600" @click="removeSeatRow(rowIndex)">Remove row</button>
                </div>

                <div class="space-y-3">
                  <div v-for="(range, rangeIndex) in row.ranges" :key="rangeIndex" class="grid grid-cols-1 gap-3 rounded-lg bg-slate-50 p-3 dark:bg-slate-900 md:grid-cols-9 md:items-end">
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">From</span>
                      <input v-model.number="range.startSeat" type="number" min="1" :max="row.seatCount" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white" />
                    </label>
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">To</span>
                      <input v-model.number="range.endSeat" type="number" min="1" :max="row.seatCount" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white" />
                    </label>
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Behavior</span>
                      <select v-model="range.seatTypeCode" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white" @change="applySeatTypePreset(range)">
                        <option v-for="type in seatTypeOptions" :key="type.value" :value="type.value">{{ type.label }}</option>
                      </select>
                    </label>
                    <label class="block md:col-span-2">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Display type</span>
                      <input v-model.trim="range.seatTypeName" placeholder="Early Bird, Premium..." class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white" />
                    </label>
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Color</span>
                      <input v-model="range.visualColorHex" type="color" class="h-10 w-full rounded-lg border border-slate-200 bg-white p-1 dark:border-slate-700 dark:bg-slate-800" />
                    </label>
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Price</span>
                      <input v-model.number="range.price" type="number" min="0" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white" />
                    </label>
                    <label class="block">
                      <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Initial status</span>
                      <select v-model="range.status" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 dark:text-white">
                        <option value="AVAILABLE">Available</option>
                        <option value="SOLD">Booked</option>
                      </select>
                    </label>
                    <button type="button" class="text-left text-sm font-bold text-red-600" @click="removeSeatRange(row, rangeIndex)">Remove</button>
                  </div>
                </div>

                <button type="button" class="rounded-xl border border-brand-orange px-4 py-2 font-bold text-brand-orange" @click="addSeatRange(row)">Add range</button>
              </div>
              <button type="button" class="rounded-xl bg-brand-navy px-4 py-2 font-bold text-white" @click="addSeatRow">Add row</button>
            </div>
          </div>

          <div v-if="currentStep === 3" class="space-y-5">
            <h2 class="text-xl font-black text-slate-900 dark:text-white">Payment & confirmation</h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Bank name</span>
                <input v-model.trim="form.payoutBankName" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Account name</span>
                <input v-model.trim="form.payoutAccountName" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
              </label>
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Account number</span>
                <input v-model.trim="form.payoutAccountNumber" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
              </label>
            </div>
            <label class="flex items-start gap-3 rounded-xl border border-slate-200 dark:border-slate-700 p-4">
              <input v-model="form.termsAccepted" type="checkbox" class="mt-1" />
              <span class="text-sm text-slate-600 dark:text-slate-300">I confirm the event information is accurate and accept TicketRush provider terms. This MVP will publish the event immediately; admin approval will be added later.</span>
            </label>
          </div>

          <div class="mt-8 flex justify-between border-t border-slate-100 dark:border-slate-700 pt-5">
            <button v-if="currentStep > 1" type="button" class="rounded-xl px-5 py-3 font-bold text-slate-600 dark:text-slate-200" @click="previousStep">Back</button>
            <span v-else></span>
            <button v-if="currentStep < 3" type="button" class="rounded-xl bg-brand-orange px-6 py-3 font-black text-white hover:bg-orange-600" @click="nextStep">Continue</button>
            <button v-else type="button" :disabled="loading" class="rounded-xl bg-brand-orange px-6 py-3 font-black text-white hover:bg-orange-600 disabled:opacity-60" @click="submitEvent">
              {{ loading ? (isEditMode ? "Saving..." : "Publishing...") : (isEditMode ? "Save changes" : "Confirm & publish") }}
            </button>
          </div>
        </main>
      </div>
    </div>
  </section>
</template>
