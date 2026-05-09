<script setup>
import { ref, onMounted, onUnmounted, computed } from "vue";
import { useRouter } from "vue-router";

defineProps(["isDark"]);
const emit = defineEmits(["toggle-theme"]);

const router = useRouter();

const showProfileMenu = ref(false);

const isLoggedIn = true;


const closeMenu = (e) => {
  if (!e.target.closest(".profile-dropdown-container")) {
    showProfileMenu.value = false;
  }
};


onMounted(() => window.addEventListener("click", closeMenu));
onUnmounted(() => window.removeEventListener("click", closeMenu));
</script>

<template>
  <header class="sticky top-0 z-50 shadow-md">
    <div class="bg-brand-navy text-white py-3.5 px-4 md:px-8 flex items-center justify-between">
      <router-link
        to="/"
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

      <div class="flex items-center gap-6 lg:gap-8">
        <router-link
          to="/my-tickets"
          class="hidden sm:flex items-center gap-2 hover:text-brand-orange transition text-base md:text-lg font-bold whitespace-nowrap group"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="transition-transform duration-300 group-hover:-rotate-12 group-hover:scale-110"
          >
            <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"></path>
            <line x1="13" x2="13" y1="5" y2="19"></line>
          </svg>
          My tickets
        </router-link>

        <div class="flex items-center gap-4 profile-dropdown-container relative">
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
            @click.stop="showProfileMenu = !showProfileMenu"
            class="flex items-center gap-2 border-2 border-brand-orange rounded-full px-4 py-2 text-sm md:text-base font-bold hover:bg-brand-orange transition"
          >
            <span>
              {{ currentUser?.fullName || currentUser?.email || "User" }}
            </span>
            <span>⌄</span>
          </button>

          <div
            v-if="isLoggedIn && showProfileMenu"
            class="absolute right-0 top-12 w-56 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-gray-100 dark:border-slate-700 overflow-hidden text-slate-800 dark:text-white"
          >
            <div class="px-4 py-3 border-b border-gray-100 dark:border-slate-700">
              <p class="font-bold truncate">
                {{ currentUser?.fullName || "User" }}
              </p>
              <p class="text-xs text-gray-500 truncate">
                {{ currentUser?.email }}
              </p>
            </div>

            <router-link
              to="/my-events"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              My Events
            </router-link>

            <router-link
              to="/create-event"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Create Event
            </router-link>

            <button
              type="button"
              class="w-full text-left px-4 py-3 text-red-600 hover:bg-red-50 dark:hover:bg-slate-700"
              @click="handleLogout"
            >
              Logout
            </button>
          </div>
        </div>

        <button
          @click.stop="emit('toggle-theme')"
          class="p-2.5 rounded-full hover:bg-white/10 transition text-xl"
        >
          {{ isDark ? "🌙" : "☀️" }}
        </button>
      </div>
    </div>

    <div class="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-4 md:px-8 py-3 overflow-x-auto shadow-sm">
      <nav class="flex justify-center gap-12 lg:gap-16 items-center whitespace-nowrap">
        <router-link
          v-for="item in [
            { name: 'Events', path: '/events' },
            { name: 'Create Events', path: '/create-event' },
            { name: 'My Events', path: '/my-events' },
            { name: 'Membership', path: '/membership' },
            { name: 'Help Center', path: '/help' }
          ]"
          :key="item.name"
          :to="item.path"
          class="relative py-2 text-base md:text-lg font-extrabold text-slate-600 dark:text-slate-300 hover:text-brand-navy dark:hover:text-brand-orange transition-all duration-300 group tracking-wide"
        >
          {{ item.name }}
          <span class="absolute bottom-0 left-0 w-0 h-[3px] bg-brand-orange transition-all duration-300 group-hover:w-full"></span>
        </router-link>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.slide-fade-enter-active {
  transition: all 0.2s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.1s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>