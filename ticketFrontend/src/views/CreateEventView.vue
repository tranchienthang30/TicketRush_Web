<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import * as eventApi from "@/api/event.api";
import * as organizationApi from "@/api/organization.api";

const router = useRouter();
const authStore = useAuthStore();

const currentStep = ref(1);
const categories = ref([]);
const loading = ref(false);
const error = ref("");
const message = ref("");

const organizationForm = reactive({
  name: "",
  businessEmail: "",
});

const form = reactive({
  title: "",
  description: "",
  bannerUrl: "",
  categoryId: "",
  locationName: "",
  city: "",
  address: "",
  startTime: "",
  endTime: "",
  saleStartTime: "",
  saleEndTime: "",
  seatProvider: "INTERNAL",
  externalSeatChartKey: "",
  sections: [
    { name: "Standard", basePrice: 300000, rowCount: 5, seatsPerRow: 20 },
  ],
  payoutBankName: "",
  payoutAccountName: "",
  payoutAccountNumber: "",
  termsAccepted: false,
});

const isOrganizerReady = computed(() =>
  ["ORGANIZER", "ADMIN"].includes(authStore.user?.role) && authStore.user?.primaryOrganizationId
);

onMounted(async () => {
  const response = await eventApi.getCategories();
  categories.value = response.data;
});

async function requestOrganizationVerification() {
  error.value = "";
  message.value = "";
  if (!organizationForm.name || !organizationForm.businessEmail) {
    error.value = "Please enter organization name and business email.";
    return;
  }

  loading.value = true;
  try {
    const response = await organizationApi.registerOrganization({
      name: organizationForm.name,
      businessEmail: organizationForm.businessEmail,
    });
    message.value = response.data.message || "Verification email has been sent.";
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to request organization verification.";
  } finally {
    loading.value = false;
  }
}

function addSection() {
  form.sections.push({
    name: `Section ${form.sections.length + 1}`,
    basePrice: 300000,
    rowCount: 3,
    seatsPerRow: 20,
  });
}

function removeSection(index) {
  if (form.sections.length > 1) {
    form.sections.splice(index, 1);
  }
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
}

function previousStep() {
  error.value = "";
  currentStep.value -= 1;
}

async function submitEvent() {
  error.value = "";
  if (!form.termsAccepted) {
    error.value = "You must accept the organizer terms before submitting.";
    return;
  }

  loading.value = true;
  try {
    const response = await eventApi.createEvent(toPayload());
    router.push(`/events?created=${response.data.slug}`);
  } catch (err) {
    error.value = err.response?.data?.message || "Unable to create event.";
  } finally {
    loading.value = false;
  }
}

function isStepOneValid() {
  return Boolean(form.title && form.categoryId && form.locationName && form.city && form.startTime && form.endTime);
}

function isStepTwoValid() {
  if (form.seatProvider === "SEATS_IO") {
    return Boolean(form.externalSeatChartKey);
  }
  return form.sections.length > 0 && form.sections.every((section) =>
    section.name && section.basePrice >= 0 && section.rowCount > 0 && section.seatsPerRow > 0
  );
}

function toPayload() {
  return {
    title: form.title,
    description: form.description || null,
    bannerUrl: form.bannerUrl || null,
    categoryId: Number(form.categoryId),
    locationName: form.locationName,
    city: form.city,
    address: form.address || null,
    startTime: toInstant(form.startTime),
    endTime: toInstant(form.endTime),
    saleStartTime: form.saleStartTime ? toInstant(form.saleStartTime) : null,
    saleEndTime: form.saleEndTime ? toInstant(form.saleEndTime) : null,
    seatProvider: form.seatProvider,
    externalSeatChartKey: form.seatProvider === "SEATS_IO" ? form.externalSeatChartKey : null,
    payoutBankName: form.payoutBankName || null,
    payoutAccountName: form.payoutAccountName || null,
    payoutAccountNumber: form.payoutAccountNumber || null,
    termsAccepted: form.termsAccepted,
    sections: form.sections.map((section) => ({
      name: section.name,
      basePrice: Number(section.basePrice),
      rowCount: Number(section.rowCount),
      seatsPerRow: Number(section.seatsPerRow),
    })),
  };
}

function toInstant(value) {
  return new Date(value).toISOString();
}
</script>

<template>
  <section class="min-h-screen bg-brand-light dark:bg-slate-900 px-4 py-8">
    <div class="max-w-7xl mx-auto">
      <div v-if="!isOrganizerReady" class="max-w-2xl mx-auto bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8">
        <h1 class="text-3xl font-black text-brand-navy dark:text-white mb-3">Verify your organization</h1>
        <p class="text-slate-500 dark:text-slate-400 mb-6">
          To create events, verify a business email. After verification, your account becomes an organizer and you can continue creating events.
        </p>

        <form class="space-y-5" @submit.prevent="requestOrganizationVerification">
          <label class="block">
            <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Organization name</span>
            <input v-model.trim="organizationForm.name" type="text" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30" />
          </label>
          <label class="block">
            <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Business email</span>
            <input v-model.trim="organizationForm.businessEmail" type="email" placeholder="events@company.com" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-4 focus:ring-brand-orange/30" />
          </label>

          <p v-if="message" class="text-sm text-green-700 bg-green-50 border border-green-100 rounded-xl px-4 py-3">{{ message }}</p>
          <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">{{ error }}</p>

          <button type="submit" :disabled="loading" class="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-60 text-white font-black py-3 rounded-xl transition">
            {{ loading ? "Sending..." : "Send verification email" }}
          </button>
        </form>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-8">
        <aside class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 h-fit sticky top-32">
          <h2 class="font-black text-brand-navy dark:text-white mb-5">Create event</h2>
          <ol class="space-y-3">
            <li v-for="step in [
              { id: 1, label: 'Event information' },
              { id: 2, label: 'Seat setup' },
              { id: 3, label: 'Payment & confirmation' }
            ]" :key="step.id" class="flex items-center gap-3">
              <span :class="currentStep === step.id ? 'bg-brand-orange text-white' : 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-300'" class="grid h-8 w-8 place-items-center rounded-full text-sm font-black">
                {{ step.id }}
              </span>
              <span :class="currentStep === step.id ? 'text-brand-orange' : 'text-slate-600 dark:text-slate-300'" class="font-bold">{{ step.label }}</span>
            </li>
          </ol>
        </aside>

        <main class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm p-6 md:p-8">
          <div class="mb-6 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-5">
            <h1 class="text-2xl font-black text-brand-navy dark:text-white mb-2">Organizer rules</h1>
            <ul class="text-sm text-slate-600 dark:text-slate-300 space-y-1 list-disc pl-5">
              <li>Use accurate event information, official images, venue, and sale period.</li>
              <li>Ticket sections and seat labels must match the actual venue setup.</li>
              <li>Payout information must belong to the verified organization.</li>
              <li>This MVP publishes events immediately; admin review will replace this later.</li>
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
              <label class="block">
                <span class="block text-sm font-bold mb-2 text-slate-700 dark:text-slate-200">Category</span>
                <select v-model="form.categoryId" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white">
                  <option value="">Select category</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
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
              <button type="button" :class="form.seatProvider === 'INTERNAL' ? 'bg-brand-orange text-white' : 'bg-slate-100 dark:bg-slate-700 dark:text-white'" class="rounded-xl px-4 py-3 font-bold" @click="form.seatProvider = 'INTERNAL'">Internal</button>
              <button type="button" :class="form.seatProvider === 'SEATS_IO' ? 'bg-brand-orange text-white' : 'bg-slate-100 dark:bg-slate-700 dark:text-white'" class="rounded-xl px-4 py-3 font-bold" @click="form.seatProvider = 'SEATS_IO'">seats.io</button>
            </div>

            <div v-if="form.seatProvider === 'SEATS_IO'" class="rounded-xl border border-blue-200 bg-blue-50 p-5 text-blue-950">
              <p class="font-black mb-2">seats.io integration placeholder</p>
              <p class="text-sm mb-4">Create the chart in seats.io, then paste its chart key here. The backend stores the key and keeps local ticket/seat ownership in TicketRush.</p>
              <label class="block">
                <span class="block text-sm font-bold mb-2">Chart key</span>
                <input v-model.trim="form.externalSeatChartKey" placeholder="chart-key-from-seats-io" class="w-full px-4 py-3 rounded-xl border border-blue-200 bg-white text-slate-900" />
              </label>
            </div>

            <div v-else class="space-y-4">
              <div v-for="(section, index) in form.sections" :key="index" class="grid grid-cols-1 md:grid-cols-5 gap-3 rounded-xl border border-slate-200 dark:border-slate-700 p-4">
                <label class="block md:col-span-2">
                  <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Name</span>
                  <input v-model.trim="section.name" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                </label>
                <label class="block">
                  <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Price</span>
                  <input v-model.number="section.basePrice" type="number" min="0" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                </label>
                <label class="block">
                  <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Rows</span>
                  <input v-model.number="section.rowCount" type="number" min="1" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                </label>
                <label class="block">
                  <span class="block text-xs font-bold mb-1 text-slate-600 dark:text-slate-300">Seats/row</span>
                  <input v-model.number="section.seatsPerRow" type="number" min="1" class="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 dark:text-white" />
                </label>
                <button type="button" class="md:col-span-5 text-left text-sm font-bold text-red-600" @click="removeSection(index)">Remove section</button>
              </div>
              <button type="button" class="rounded-xl border border-brand-orange px-4 py-2 font-bold text-brand-orange" @click="addSection">Add section</button>
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
              <span class="text-sm text-slate-600 dark:text-slate-300">I confirm the event information is accurate and accept TicketRush organizer terms. This MVP will publish the event immediately; admin approval will be added later.</span>
            </label>
          </div>

          <div class="mt-8 flex justify-between border-t border-slate-100 dark:border-slate-700 pt-5">
            <button v-if="currentStep > 1" type="button" class="rounded-xl px-5 py-3 font-bold text-slate-600 dark:text-slate-200" @click="previousStep">Back</button>
            <span v-else></span>
            <button v-if="currentStep < 3" type="button" class="rounded-xl bg-brand-orange px-6 py-3 font-black text-white hover:bg-orange-600" @click="nextStep">Continue</button>
            <button v-else type="button" :disabled="loading" class="rounded-xl bg-brand-orange px-6 py-3 font-black text-white hover:bg-orange-600 disabled:opacity-60" @click="submitEvent">
              {{ loading ? "Publishing..." : "Confirm & publish" }}
            </button>
          </div>
        </main>
      </div>
    </div>
  </section>
</template>
