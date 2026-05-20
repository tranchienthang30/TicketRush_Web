<script setup>
import { computed, onMounted, ref } from "vue";
import {
  getCurrentMembership,
  getMembershipPlans,
  subscribeMembership,
} from "../api/ticketRushApi";

const plans = ref([]);
const currentMembership = ref(null);
const loading = ref(true);
const actionLoadingId = ref(null);
const error = ref("");

const highlights = computed(() => [
  {
    label: "Active tier",
    value: currentMembership.value?.planName || "Ready",
    description: "Your current membership status from TicketRush API.",
  },
  {
    label: "Member discount",
    value: currentMembership.value?.discountPercent
      ? `${currentMembership.value.discountPercent}%`
      : "0%",
    description: "Discount rate applied to eligible orders.",
  },
  {
    label: "Available plans",
    value: String(plans.value.length),
    description: "Active membership packages currently available.",
  },
]);

const benefitSteps = [
  {
    title: "Unlock presales",
    description:
      "Grab limited inventory before the crowd and avoid last-minute rushes.",
  },
  {
    title: "Collect rewards",
    description:
      "Earn points on every order and convert them into credits or upgrades.",
  },
  {
    title: "Stay event-ready",
    description:
      "Keep event tickets, ticket history, and membership perks in one profile hub.",
  },
];

const faqs = [
  {
    id: 1,
    question: "Can I cancel my membership at any time?",
    answer:
      "Yes. You can stop renewal from your account page, and your perks stay active until the current billing cycle ends.",
  },
  {
    id: 2,
    question: "Do perks apply to every event on the platform?",
    answer:
      "Most TicketRush-hosted events are included. Some partner-specific tickets may have different terms listed on the event page.",
  },
  {
    id: 3,
    question: "Can I upgrade from Silver to Gold later?",
    answer:
      "Yes. Upgrades are prorated so you only pay the difference for the remaining cycle.",
  },
];

const openFaqId = ref(1);

async function loadMembership() {
  loading.value = true;
  error.value = "";

  try {
    const [planData, membershipData] = await Promise.all([
      getMembershipPlans(),
      getCurrentMembership(),
    ]);
    plans.value = planData || [];
    currentMembership.value = membershipData;
  } catch {
    error.value = "Could not load membership data. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

async function choosePlan(plan) {
  actionLoadingId.value = plan.id;
  error.value = "";

  try {
    currentMembership.value = await subscribeMembership(plan.id);
  } catch {
    error.value = "Could not update membership. Please try again.";
  } finally {
    actionLoadingId.value = null;
  }
}

function toggleFaq(faqId) {
  openFaqId.value = openFaqId.value === faqId ? null : faqId;
}

function planAccent(plan) {
  if (plan.featured) {
    return "border-brand-orange ring-4 ring-brand-orange/15 shadow-2xl";
  }
  return "border-slate-200 dark:border-slate-700";
}

function planButtonClass(plan) {
  if (currentMembership.value?.planId === plan.id && currentMembership.value?.active) {
    return "bg-slate-100 text-slate-500 border border-slate-200 cursor-default dark:bg-slate-900 dark:text-slate-400 dark:border-slate-700";
  }
  if (plan.featured) {
    return "bg-brand-orange text-white hover:bg-orange-600 border border-brand-orange";
  }
  return "bg-brand-navy text-white hover:bg-sky-800 border border-brand-navy dark:bg-slate-100 dark:text-slate-900 dark:hover:bg-white";
}

function planDuration(plan) {
  return plan.durationDays ? `/ ${plan.durationDays} days` : "";
}

onMounted(loadMembership);
</script>

<template>
  <div class="min-h-screen bg-brand-light dark:bg-slate-900 pb-20">
    <section class="relative overflow-hidden bg-brand-navy text-white">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_right,_rgba(249,115,22,0.35),_transparent_35%),linear-gradient(135deg,_rgba(255,255,255,0.04),_transparent)]"></div>
      <div class="relative max-w-7xl mx-auto px-4 md:px-8 py-16 md:py-20">
        <div class="grid gap-10 lg:grid-cols-[1.25fr_0.9fr] items-center">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange mb-4">
              Membership
            </p>
            <h1 class="text-4xl md:text-6xl font-black tracking-tight leading-none max-w-3xl">
              Book smarter, move faster, and get more from every event night.
            </h1>
            <p class="mt-6 max-w-2xl text-base md:text-lg text-blue-100 leading-relaxed">
              TicketRush Membership keeps your best perks in one place, from
              early access and rewards to premium support when demand spikes.
            </p>
            <div class="mt-8 flex flex-wrap gap-4">
              <a
                href="#plans"
                class="inline-flex items-center justify-center rounded-full bg-brand-orange px-7 py-3 text-sm font-black uppercase tracking-[0.2em] text-white transition hover:scale-[1.02] hover:bg-orange-600"
              >
                View Plans
              </a>
              <router-link
                to="/profile"
                class="inline-flex items-center justify-center rounded-full border border-white/25 bg-white/10 px-7 py-3 text-sm font-black uppercase tracking-[0.2em] text-white transition hover:bg-white/15"
              >
                Open Profile
              </router-link>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-3 lg:grid-cols-1">
            <div
              v-for="item in highlights"
              :key="item.label"
              class="rounded-3xl border border-white/10 bg-white/10 p-6 backdrop-blur"
            >
              <p class="text-sm font-bold uppercase tracking-[0.25em] text-white/70">
                {{ item.label }}
              </p>
              <p class="mt-4 text-4xl font-black text-brand-orange">
                {{ item.value }}
              </p>
              <p class="mt-3 text-sm leading-relaxed text-blue-100">
                {{ item.description }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 md:px-8 mt-10">
      <div class="grid gap-6 md:grid-cols-3">
        <div
          v-for="(step, index) in benefitSteps"
          :key="step.title"
          class="rounded-3xl border border-slate-200 bg-white p-7 shadow-sm dark:border-slate-700 dark:bg-slate-800"
        >
          <div
            class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-orange/10 text-lg font-black text-brand-orange"
          >
            {{ index + 1 }}
          </div>
          <h2 class="text-2xl font-black text-brand-navy dark:text-white">
            {{ step.title }}
          </h2>
          <p class="mt-3 text-sm leading-7 text-slate-500 dark:text-slate-300">
            {{ step.description }}
          </p>
        </div>
      </div>
    </section>

    <section id="plans" class="max-w-7xl mx-auto px-4 md:px-8 mt-16">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
            Plans
          </p>
          <h2 class="mt-3 text-3xl md:text-4xl font-black text-brand-navy dark:text-white">
            Choose the rhythm that fits your event life.
          </h2>
        </div>
        <p class="max-w-xl text-sm leading-7 text-slate-500 dark:text-slate-300">
          Plans are loaded from the TicketRush membership API and reflect active
          packages in the database.
        </p>
      </div>

      <div
        v-if="loading"
        class="mt-8 rounded-3xl border border-slate-200 bg-white p-8 text-center font-bold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
      >
        Loading membership plans...
      </div>

      <div
        v-else-if="error"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 p-8 text-center font-bold text-red-700"
      >
        {{ error }}
      </div>

      <div v-else class="mt-8 grid gap-6 xl:grid-cols-3">
        <article
          v-for="plan in plans"
          :key="plan.id"
          class="rounded-[2rem] border bg-white p-8 transition duration-300 hover:-translate-y-1 dark:bg-slate-800"
          :class="planAccent(plan)"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <p
                v-if="plan.featured"
                class="mb-4 inline-flex rounded-full bg-brand-orange/10 px-3 py-1 text-xs font-black uppercase tracking-[0.2em] text-brand-orange"
              >
                Most popular
              </p>
              <h3 class="text-2xl font-black text-brand-navy dark:text-white">
                {{ plan.name }}
              </h3>
              <p class="mt-3 text-sm leading-7 text-slate-500 dark:text-slate-300">
                {{ plan.description }}
              </p>
            </div>
          </div>

          <div class="mt-8 flex items-end gap-2">
            <span class="text-4xl font-black text-brand-navy dark:text-white">
              {{ plan.displayPrice }}
            </span>
            <span class="pb-1 text-sm font-semibold text-slate-400">
              {{ planDuration(plan) }}
            </span>
          </div>

          <ul class="mt-8 space-y-4">
            <li
              v-for="perk in plan.perks"
              :key="perk"
              class="flex items-start gap-3 text-sm leading-7 text-slate-600 dark:text-slate-200"
            >
              <span
                class="mt-1 inline-flex h-6 w-6 items-center justify-center rounded-full bg-brand-orange/10 text-xs font-black text-brand-orange"
              >
                OK
              </span>
              <span>{{ perk }}</span>
            </li>
          </ul>

          <button
            type="button"
            :disabled="actionLoadingId === plan.id || (currentMembership?.planId === plan.id && currentMembership?.active)"
            class="mt-8 inline-flex w-full items-center justify-center rounded-2xl px-5 py-3 text-sm font-black uppercase tracking-[0.18em] transition disabled:opacity-80"
            :class="planButtonClass(plan)"
            @click="choosePlan(plan)"
          >
            <span v-if="actionLoadingId === plan.id">Updating...</span>
            <span v-else-if="currentMembership?.planId === plan.id && currentMembership?.active">Current Plan</span>
            <span v-else>Choose {{ plan.name }}</span>
          </button>
        </article>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 md:px-8 mt-16">
      <div
        class="rounded-[2rem] border border-slate-200 bg-white p-8 md:p-10 shadow-sm dark:border-slate-700 dark:bg-slate-800"
      >
        <div class="grid gap-8 lg:grid-cols-[1fr_1.1fr]">
          <div>
            <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
              Why members stay
            </p>
            <h2 class="mt-3 text-3xl font-black text-brand-navy dark:text-white">
              The experience is built for high-demand nights.
            </h2>
            <p class="mt-4 text-sm leading-7 text-slate-500 dark:text-slate-300">
              When queues get busy, small advantages matter. Membership keeps
              your checkout details ready, preserves your rewards history, and
              opens doors to events that sell out quickly.
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="rounded-3xl bg-slate-50 p-6 dark:bg-slate-900">
              <p class="text-3xl font-black text-brand-orange">{{ plans.length }}</p>
              <p class="mt-2 text-sm font-bold uppercase tracking-[0.18em] text-slate-500">
                Active plans
              </p>
            </div>
            <div class="rounded-3xl bg-slate-50 p-6 dark:bg-slate-900">
              <p class="text-3xl font-black text-brand-orange">
                {{ currentMembership?.active ? currentMembership.planName : "None" }}
              </p>
              <p class="mt-2 text-sm font-bold uppercase tracking-[0.18em] text-slate-500">
                Current tier
              </p>
            </div>
            <div class="rounded-3xl bg-slate-50 p-6 dark:bg-slate-900 sm:col-span-2">
              <p class="text-sm leading-7 text-slate-500 dark:text-slate-300">
                Membership data now comes from your PostgreSQL seed through the
                Spring Boot API, so changes made here update the current demo user.
              </p>
              <p class="mt-4 text-sm font-black uppercase tracking-[0.18em] text-brand-navy dark:text-white">
                TicketRush Membership API
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="max-w-5xl mx-auto px-4 md:px-8 mt-16">
      <div class="text-center">
        <p class="text-sm font-black uppercase tracking-[0.35em] text-brand-orange">
          FAQ
        </p>
        <h2 class="mt-3 text-3xl md:text-4xl font-black text-brand-navy dark:text-white">
          Questions before you join?
        </h2>
      </div>

      <div class="mt-8 space-y-4">
        <article
          v-for="faq in faqs"
          :key="faq.id"
          class="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800"
        >
          <button
            type="button"
            class="flex w-full items-center justify-between gap-4 px-6 py-5 text-left"
            @click="toggleFaq(faq.id)"
          >
            <span class="text-lg font-black text-brand-navy dark:text-white">
              {{ faq.question }}
            </span>
            <span
              class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-xl font-bold text-brand-navy transition dark:bg-slate-900 dark:text-white"
            >
              {{ openFaqId === faq.id ? "-" : "+" }}
            </span>
          </button>
          <div
            v-if="openFaqId === faq.id"
            class="border-t border-slate-100 px-6 pb-6 pt-4 text-sm leading-7 text-slate-500 dark:border-slate-700 dark:text-slate-300"
          >
            {{ faq.answer }}
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
