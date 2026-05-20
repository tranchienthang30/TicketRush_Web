<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";
import { useEventSearchSuggestions } from "@/composables/useEventSearchSuggestions";

defineOptions({ name: "AppHeader" });
defineProps(["isDark"]);
const emit = defineEmits(["toggle-theme"]);

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const showProfileMenu = ref(false);
const searchTerm = ref("");
const {
  suggestionQuery,
  suggestions: searchSuggestions,
  suggestionsLoading,
  showSuggestions,
  openSuggestions,
  closeSuggestions,
} = useEventSearchSuggestions(searchTerm);

const isLoggedIn = computed(() => authStore.isAuthenticated);
const currentUser = computed(() => authStore.user);
const currentRole = computed(() => currentUser.value?.role || "GUEST");
const displayName = computed(() => currentUser.value?.fullName || currentUser.value?.email || "User");
const initials = computed(() => {
  const source = displayName.value.trim();
  if (!source) return "U";
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
});
const isProvider = computed(() => currentRole.value === "PROVIDER");
const isAdmin = computed(() => currentRole.value === "ADMIN");
const canUseCatalogSearch = computed(() => !isProvider.value && !isAdmin.value);
const homePath = computed(() => {
  if (isAdmin.value) return "/admin/dashboard";
  if (isProvider.value) return "/provider/home";
  return "/";
});

const visibleNavItems = computed(() => {
  if (isAdmin.value) {
    return [
      { name: "Dashboard", path: "/admin/dashboard" },
      { name: "User", path: "/admin/users" },
      { name: "System", path: "/admin/system" },
      { name: "Provider Requests", path: "/admin/provider-requests" },
    ];
  }

  if (isProvider.value) {
    return [
      { name: "Home", path: "/provider/home" },
      { name: "Create Event", path: "/create-event" },
      { name: "Manage Events", path: "/my-events" },
      { name: "Help Center", path: "/help" },
    ];
  }

  return [
    { name: "Home", path: "/" },
    { name: "Events", path: "/events" },
    { name: "Booking", path: "/booking" },
    { name: "Help Center", path: "/help" },
  ];
});

function closeMenu(event) {
  if (!event.target.closest(".profile-dropdown-container")) {
    showProfileMenu.value = false;
  }
  if (!event.target.closest(".site-search-container")) {
    closeSuggestions();
  }
}

function isActiveNav(path) {
  if (path === "/") {
    return route.path === "/";
  }
  return route.path === path || route.path.startsWith(`${path}/`);
}

function queryText(value) {
  if (Array.isArray(value)) {
    return queryText(value[0]);
  }
  return String(value || "").trim();
}

function handleSearch() {
  const q = searchTerm.value.trim();
  showProfileMenu.value = false;
  closeSuggestions();

  router.push({
    path: "/events",
    query: q ? { q } : {},
  });
}

function suggestionMeta(event) {
  return [event.category || "Event", event.location, event.date].filter(Boolean).join(" / ");
}

function suggestionTarget(event) {
  if (event.slug) {
    return { path: `/events/${event.slug}` };
  }
  const q = event.title || suggestionQuery.value;
  return {
    path: "/events",
    query: q ? { q } : {},
  };
}

function selectSearchSuggestion(event) {
  searchTerm.value = event.title || suggestionQuery.value;
  showProfileMenu.value = false;
  closeSuggestions();
  router.push(suggestionTarget(event));
}

async function handleLogout() {
  await authStore.logoutUser();
  showProfileMenu.value = false;
  router.push("/login");
}

watch(
  () => [route.path, route.query.q],
  () => {
    searchTerm.value = route.path === "/events" ? queryText(route.query.q) : "";
  },
  { immediate: true },
);

onMounted(() => window.addEventListener("click", closeMenu));
onUnmounted(() => window.removeEventListener("click", closeMenu));
</script>

<template>
  <header class="sticky top-0 z-50 shadow-md overflow-visible">
    <div class="relative z-40 bg-brand-navy text-white py-3.5 px-4 md:px-8 flex items-center justify-between">
      <router-link
        :to="homePath"
        class="text-2xl md:text-3xl font-black tracking-tighter hover:opacity-90 transition-all flex-shrink-0"
      >
        STAR<span class="text-brand-orange">LIGHT</span>
      </router-link>

      <form
        v-if="canUseCatalogSearch"
        class="site-search-container hidden flex-1 mx-10 md:flex"
        role="search"
        @submit.prevent="handleSearch"
      >
        <div class="relative w-full max-w-md">
          <label for="site-event-search" class="sr-only">Search events</label>
          <input
            id="site-event-search"
            v-model="searchTerm"
            type="text"
            placeholder="Search events, city, venue..."
            class="w-full rounded-full px-5 py-2.5 pr-12 text-base text-slate-900 transition-all focus:outline-none focus:ring-4 focus:ring-brand-orange/50"
            autocomplete="off"
            @focus="openSuggestions"
            @keydown.esc="closeSuggestions"
          />
          <button
            type="submit"
            class="absolute right-1 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full text-brand-navy transition hover:bg-brand-orange hover:text-white"
            aria-label="Search events"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="m21 21-4.35-4.35m1.35-5.65a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2.4"
              />
            </svg>
          </button>

          <div
            v-if="showSuggestions"
            class="absolute left-0 right-0 top-full z-[140] mt-2 overflow-hidden rounded-2xl border border-slate-200 bg-white text-slate-900 shadow-2xl dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            role="listbox"
          >
            <div v-if="suggestionsLoading" class="px-4 py-3 text-sm font-bold text-slate-500 dark:text-slate-300">
              Searching...
            </div>
            <template v-else-if="searchSuggestions.length">
              <button
                v-for="event in searchSuggestions"
                :key="event.id"
                type="button"
                class="block w-full px-4 py-3 text-left transition hover:bg-slate-50 dark:hover:bg-slate-700"
                role="option"
                @mousedown.prevent
                @click="selectSearchSuggestion(event)"
              >
                <span class="block truncate text-sm font-black">{{ event.title }}</span>
                <span class="mt-0.5 block truncate text-xs font-semibold text-slate-500 dark:text-slate-300">
                  {{ suggestionMeta(event) }}
                </span>
              </button>
              <button
                type="button"
                class="block w-full border-t border-slate-100 px-4 py-3 text-left text-sm font-black text-brand-orange transition hover:bg-orange-50 dark:border-slate-700 dark:hover:bg-slate-700"
                @mousedown.prevent
                @click="handleSearch"
              >
                Search all results for "{{ suggestionQuery }}"
              </button>
            </template>
            <button
              v-else
              type="button"
              class="block w-full px-4 py-3 text-left text-sm font-bold text-slate-500 transition hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-700"
              @mousedown.prevent
              @click="handleSearch"
            >
              Search for "{{ suggestionQuery }}"
            </button>
          </div>
        </div>
      </form>

      <div class="flex items-center gap-4 lg:gap-6">
        <router-link
          v-if="isLoggedIn && currentRole === 'CUSTOMER'"
          to="/my-tickets"
          class="hidden sm:flex items-center gap-2 hover:text-brand-orange transition text-base md:text-lg font-bold whitespace-nowrap"
        >
          My Tickets
        </router-link>

        <div class="profile-dropdown-container relative z-[100]">
          <router-link
            v-if="!isLoggedIn"
            to="/login"
            class="text-brand-orange font-bold hover:text-white border-2 border-brand-orange hover:bg-brand-orange px-5 py-2 rounded-full transition-all duration-300 text-sm md:text-base whitespace-nowrap"
          >
            Login / Register
          </router-link>

          <button
            v-else
            type="button"
            :aria-expanded="showProfileMenu"
            aria-haspopup="menu"
            @click.stop="showProfileMenu = !showProfileMenu"
            class="flex items-center gap-2 border-2 border-brand-orange rounded-full pl-2 pr-4 py-1.5 text-sm md:text-base font-bold hover:bg-brand-orange transition max-w-[220px]"
          >
            <span class="grid h-8 w-8 flex-shrink-0 place-items-center rounded-full bg-brand-orange text-white text-xs font-black">
              {{ initials }}
            </span>
            <span class="truncate">
              {{ displayName }}
            </span>
            <span class="text-xs leading-none">v</span>
          </button>

          <div
            v-if="isLoggedIn && showProfileMenu"
            class="absolute right-0 top-full mt-3 w-72 z-[120] overflow-hidden rounded-xl border border-slate-200 bg-white text-slate-800 shadow-2xl dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            role="menu"
          >
            <div class="px-4 py-4 border-b border-gray-100 dark:border-slate-700">
              <div class="flex items-center gap-3">
                <span class="grid h-10 w-10 flex-shrink-0 place-items-center rounded-full bg-brand-orange text-white text-sm font-black">
                  {{ initials }}
                </span>
                <div class="min-w-0">
                  <p class="font-bold truncate">
                    {{ displayName }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                    {{ currentUser?.email }}
                  </p>
                </div>
              </div>
            </div>

            <router-link
              to="/profile"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Profile
            </router-link>

            <router-link
              v-if="currentRole === 'CUSTOMER'"
              to="/my-tickets"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              My Tickets
            </router-link>

            <router-link
              v-if="isAdmin"
              to="/admin/dashboard"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Admin Dashboard
            </router-link>

            <router-link
              v-if="isAdmin"
              to="/admin/users"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              User Management
            </router-link>

            <router-link
              v-if="isAdmin"
              to="/admin/system"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              System Status
            </router-link>

            <router-link
              v-if="isAdmin"
              to="/admin/provider-requests"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Provider Requests
            </router-link>

            <router-link
              v-if="isProvider"
              to="/provider/home"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Provider Home
            </router-link>

            <router-link
              v-if="isProvider"
              to="/create-event"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Create Event
            </router-link>

            <router-link
              v-if="isProvider"
              to="/my-events"
              class="block px-4 py-3 text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Events Management
            </router-link>

            <div class="border-t border-gray-100 dark:border-slate-700">
              <button
                type="button"
                class="w-full text-left px-4 py-3 text-sm font-semibold text-red-600 hover:bg-red-50 dark:hover:bg-slate-700"
                @click="handleLogout"
              >
                Logout
              </button>
            </div>
          </div>
        </div>

        <button
          type="button"
          @click.stop="emit('toggle-theme')"
          class="p-2.5 rounded-full hover:bg-white/10 transition text-sm font-bold uppercase tracking-[0.2em]"
          :aria-label="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
        >
          {{ isDark ? "Light" : "Dark" }}
        </button>
      </div>
    </div>

    <form
      v-if="canUseCatalogSearch"
      class="site-search-container relative z-30 bg-brand-navy px-4 pb-3 md:hidden"
      role="search"
      @submit.prevent="handleSearch"
    >
      <label for="mobile-event-search" class="sr-only">Search events</label>
      <div class="relative">
        <input
          id="mobile-event-search"
          v-model="searchTerm"
          type="text"
          placeholder="Search events..."
          class="w-full rounded-full px-4 py-2.5 pr-12 text-sm font-semibold text-slate-900 focus:outline-none focus:ring-4 focus:ring-brand-orange/50"
          autocomplete="off"
          @focus="openSuggestions"
          @keydown.esc="closeSuggestions"
        />
        <button
          type="submit"
          class="absolute right-1 top-1/2 grid h-9 w-9 -translate-y-1/2 place-items-center rounded-full text-brand-navy transition hover:bg-brand-orange hover:text-white"
          aria-label="Search events"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="m21 21-4.35-4.35m1.35-5.65a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2.4"
            />
          </svg>
        </button>

        <div
          v-if="showSuggestions"
          class="absolute left-4 right-4 top-full z-[140] mt-2 overflow-hidden rounded-2xl border border-slate-200 bg-white text-slate-900 shadow-2xl dark:border-slate-700 dark:bg-slate-800 dark:text-white"
          role="listbox"
        >
          <div v-if="suggestionsLoading" class="px-4 py-3 text-sm font-bold text-slate-500 dark:text-slate-300">
            Searching...
          </div>
          <template v-else-if="searchSuggestions.length">
            <button
              v-for="event in searchSuggestions"
              :key="event.id"
              type="button"
              class="block w-full px-4 py-3 text-left transition hover:bg-slate-50 dark:hover:bg-slate-700"
              role="option"
              @mousedown.prevent
              @click="selectSearchSuggestion(event)"
            >
              <span class="block truncate text-sm font-black">{{ event.title }}</span>
              <span class="mt-0.5 block truncate text-xs font-semibold text-slate-500 dark:text-slate-300">
                {{ suggestionMeta(event) }}
              </span>
            </button>
            <button
              type="button"
              class="block w-full border-t border-slate-100 px-4 py-3 text-left text-sm font-black text-brand-orange transition hover:bg-orange-50 dark:border-slate-700 dark:hover:bg-slate-700"
              @mousedown.prevent
              @click="handleSearch"
            >
              Search all results for "{{ suggestionQuery }}"
            </button>
          </template>
          <button
            v-else
            type="button"
            class="block w-full px-4 py-3 text-left text-sm font-bold text-slate-500 transition hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-700"
            @mousedown.prevent
            @click="handleSearch"
          >
            Search for "{{ suggestionQuery }}"
          </button>
        </div>
      </div>
    </form>

    <div class="relative z-10 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-4 md:px-8 py-3 overflow-x-auto shadow-sm">
      <nav class="flex justify-center gap-10 lg:gap-14 items-center whitespace-nowrap">
        <router-link
          v-for="item in visibleNavItems"
          :key="item.name"
          :to="item.path"
          class="relative py-2 text-base md:text-lg font-extrabold transition-all duration-300 group tracking-wide"
          :class="
            isActiveNav(item.path)
              ? 'text-brand-navy dark:text-brand-orange'
              : 'text-slate-600 dark:text-slate-300 hover:text-brand-navy dark:hover:text-brand-orange'
          "
        >
          {{ item.name }}
          <span
            class="absolute bottom-0 left-0 h-[3px] bg-brand-orange transition-all duration-300"
            :class="isActiveNav(item.path) ? 'w-full' : 'w-0 group-hover:w-full'"
          ></span>
        </router-link>
      </nav>
    </div>
  </header>
</template>
