<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

defineProps(["isDark"]);
const emit = defineEmits(["toggle-theme"]);

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const showProfileMenu = ref(false);

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
      { name: "Help Center", path: "/help" },
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
}

function isActiveNav(path) {
  if (path === "/") {
    return route.path === "/";
  }
  return route.path === path || route.path.startsWith(`${path}/`);
}

async function handleLogout() {
  await authStore.logoutUser();
  showProfileMenu.value = false;
  router.push("/login");
}

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

      <div class="hidden md:flex flex-1 mx-10">
        <input
          type="text"
          placeholder="Events..."
          class="w-full max-w-md px-5 py-2 rounded-full text-slate-900 focus:outline-none focus:ring-4 focus:ring-brand-orange/50 transition-all text-base"
        />
      </div>

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
          aria-label="Toggle theme"
        >
          {{ isDark ? "Dark" : "Light" }}
        </button>
      </div>
    </div>

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
