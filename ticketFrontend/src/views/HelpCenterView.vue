<script setup>
import { ref } from "vue";

const helpCategories = ref([
  {
    id: "movies",
    title: "Movies & Showtimes",
    icon: "01",
    articles: [
      {
        id: "m1",
        title: "How to find a movie?",
        content:
          '<p class="mb-4">Open the <strong>Movies</strong> page from the top menu to browse films by genre. You can also use the search bar to look for a title, cinema, or city.</p><p>Movie data is refreshed from the backend API.</p>',
      },
      {
        id: "m2",
        title: "How to book seats",
        content:
          '<p class="mb-4">Choose a movie, open the booking page, select available seats on the cinema map, then continue to checkout.</p><ul class="list-disc pl-6 space-y-2"><li>Step 1: Choose movie</li><li>Step 2: Pick seats</li><li>Step 3: Pay and receive tickets</li></ul>',
      },
    ],
  },
  {
    id: "account",
    title: "Account & Tickets",
    icon: "02",
    articles: [
      {
        id: "a1",
        title: "Where are my tickets?",
        content:
          "<p>All purchased movie tickets are stored in <strong>My Tickets</strong>. Open a ticket to view seat details and the QR code for check-in.</p>",
      },
      {
        id: "a2",
        title: "Membership benefits",
        content:
          "<p>Membership perks can apply discounts, early screening access, and faster support depending on the active plan.</p>",
      },
    ],
  },
  {
    id: "payment",
    title: "Payment & Policies",
    icon: "03",
    articles: [
      {
        id: "p1",
        title: "Using vouchers",
        content:
          "<p>Enter a voucher code at checkout. The backend will calculate voucher, membership discount, platform fee, and final total automatically.</p>",
      },
      {
        id: "p2",
        title: "Refund policy",
        content:
          "<p>Movie tickets are generally non-refundable unless a screening is canceled or rescheduled by the cinema. Please check the terms shown during checkout.</p>",
      },
    ],
  },
]);

const expandedCategory = ref("movies");
const activeArticle = ref(helpCategories.value[0].articles[0]);

const toggleCategory = (categoryId) => {
  expandedCategory.value = expandedCategory.value === categoryId ? null : categoryId;
};

const selectArticle = (article) => {
  activeArticle.value = article;
  window.scrollTo({ top: 0, behavior: "smooth" });
};
</script>

<template>
  <div class="bg-slate-50 dark:bg-slate-900 min-h-screen pb-20">
    <div class="bg-brand-navy py-16 px-4 text-center">
      <h1 class="text-3xl md:text-5xl font-black text-white mb-4 tracking-tight">How can we help you?</h1>
      <div class="max-w-2xl mx-auto relative">
        <input
          type="text"
          placeholder="Search movie booking help..."
          class="w-full px-6 py-4 rounded-full text-lg shadow-lg focus:outline-none focus:ring-4 focus:ring-brand-orange/50"
        />
        <button class="absolute right-3 top-2.5 bg-brand-orange text-white p-2.5 rounded-full hover:bg-orange-500 transition">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" x2="16.65" y1="21" y2="16.65"></line>
          </svg>
        </button>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-4 md:px-8 mt-10">
      <div class="flex flex-col md:flex-row gap-8 lg:gap-12">
        <aside class="w-full md:w-1/3 lg:w-1/4 flex-shrink-0">
          <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-700 overflow-hidden sticky top-24">
            <div v-for="category in helpCategories" :key="category.id" class="border-b border-slate-100 dark:border-slate-700 last:border-0">
              <button
                @click="toggleCategory(category.id)"
                class="w-full flex items-center justify-between p-4 text-left font-bold transition-colors hover:bg-slate-50 dark:hover:bg-slate-700/50"
                :class="expandedCategory === category.id ? 'text-brand-orange' : 'text-slate-700 dark:text-slate-200'"
              >
                <div class="flex items-center gap-3">
                  <span class="text-xs font-black rounded-lg bg-slate-100 px-2 py-1 dark:bg-slate-900">{{ category.icon }}</span>
                  <span>{{ category.title }}</span>
                </div>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  class="transition-transform duration-300"
                  :class="expandedCategory === category.id ? 'rotate-180 text-brand-orange' : 'text-slate-400'"
                >
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>

              <div v-show="expandedCategory === category.id" class="bg-slate-50 dark:bg-slate-900/50 px-4 py-2">
                <ul class="space-y-1">
                  <li v-for="article in category.articles" :key="article.id">
                    <button
                      @click="selectArticle(article)"
                      class="w-full text-left py-2 px-3 rounded-lg text-sm transition-all duration-200"
                      :class="activeArticle?.id === article.id ? 'bg-orange-100 dark:bg-brand-orange/20 text-brand-orange font-bold' : 'text-slate-600 dark:text-slate-400 hover:text-brand-orange dark:hover:text-white hover:bg-slate-200 dark:hover:bg-slate-700'"
                    >
                      {{ article.title }}
                    </button>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </aside>

        <main class="w-full md:w-2/3 lg:w-3/4">
          <Transition name="fade" mode="out-in">
            <div :key="activeArticle?.id" class="bg-white dark:bg-slate-800 rounded-3xl p-8 lg:p-12 shadow-sm border border-slate-200 dark:border-slate-700 min-h-[500px]">
              <div v-if="activeArticle">
                <div class="text-sm text-slate-400 font-bold uppercase tracking-wider mb-4 flex items-center gap-2">
                  <span>Help Center</span>
                  <span>/</span>
                  <span class="text-brand-orange">{{ activeArticle.title }}</span>
                </div>

                <h2 class="text-3xl md:text-4xl font-black text-slate-800 dark:text-white mb-8">{{ activeArticle.title }}</h2>
                <div class="prose prose-lg dark:prose-invert max-w-none text-slate-600 dark:text-slate-300" v-html="activeArticle.content"></div>

                <div class="mt-16 pt-8 border-t border-slate-100 dark:border-slate-700 flex flex-col sm:flex-row items-center justify-between gap-4">
                  <p class="font-bold text-slate-500 dark:text-slate-400">Was this article helpful?</p>
                  <div class="flex gap-3">
                    <button class="px-6 py-2 rounded-full border-2 border-slate-200 dark:border-slate-600 font-bold hover:border-green-500 hover:text-green-500 transition">Yes</button>
                    <button class="px-6 py-2 rounded-full border-2 border-slate-200 dark:border-slate-600 font-bold hover:border-red-500 hover:text-red-500 transition">No</button>
                  </div>
                </div>
              </div>

              <div v-else class="h-full flex flex-col items-center justify-center text-slate-400">
                <span class="text-6xl mb-4">?</span>
                <h3 class="text-2xl font-bold">Select an article</h3>
                <p>Choose a topic from the sidebar to start reading.</p>
              </div>
            </div>
          </Transition>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
