<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getProfile } from '../api/ticketRushApi'

defineProps(['isDark'])
const emit = defineEmits(['toggle-theme'])

const showProfileMenu = ref(false)
const isLoggedIn = true
const route = useRoute()
const currentUser = ref({
  fullName: 'Alex Nguyen',
  email: 'alex@starlight.vn',
})

const navItems = [
  { name: 'Home', path: '/' },
  { name: 'Movies', path: '/events' },
  { name: 'Booking', path: '/booking' },
  { name: 'My Tickets', path: '/my-tickets' },
  { name: 'Membership', path: '/membership' },
  { name: 'Help Center', path: '/help' },
]

const closeMenu = (event) => {
  if (!event.target.closest('.profile-dropdown-container')) {
    showProfileMenu.value = false
  }
}

function isActiveNav(path) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

async function loadCurrentUser() {
  try {
    const profile = await getProfile()
    currentUser.value = {
      fullName: profile.fullName,
      email: profile.email,
    }
  } catch (err) {
    // Keep the local demo user in the header if the API is not reachable.
  }
}

onMounted(() => {
  window.addEventListener('click', closeMenu)
  loadCurrentUser()
})
onUnmounted(() => window.removeEventListener('click', closeMenu))
</script>

<template>
  <header class="sticky top-0 z-50 shadow-md">
    <div
      class="relative z-20 bg-brand-navy text-white py-3.5 px-4 md:px-8 flex items-center justify-between"
    >
      <router-link
        to="/"
        class="text-2xl md:text-3xl font-black tracking-tighter hover:opacity-90 transition-all flex-shrink-0"
      >
        STAR<span class="text-brand-orange">LIGHT</span>
      </router-link>

      <div class="hidden md:flex flex-1 mx-10">
        <input
          type="text"
          placeholder="Movies..."
          class="w-full max-w-md px-5 py-2 rounded-full text-slate-900 focus:outline-none focus:ring-4 focus:ring-brand-orange/50 transition-all text-base"
        />
      </div>

      <div class="flex items-center gap-6 lg:gap-8">
        <div class="flex items-center gap-4 profile-dropdown-container relative">
          <router-link
            v-if="!isLoggedIn"
            to="/profile"
            class="text-brand-orange font-bold hover:text-white border-2 border-brand-orange hover:bg-brand-orange px-5 py-2 rounded-full transition-all duration-300 text-sm md:text-base whitespace-nowrap"
          >
            Open Profile
          </router-link>

          <button
            v-else
            type="button"
            @click.stop="showProfileMenu = !showProfileMenu"
            class="flex items-center gap-2 border-2 border-brand-orange rounded-full px-4 py-2 text-sm md:text-base font-bold hover:bg-brand-orange transition"
          >
            <span>
              {{ currentUser?.fullName || currentUser?.email || 'User' }}
            </span>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="transition-transform duration-200"
              :class="showProfileMenu ? 'rotate-180' : ''"
            >
              <polyline points="6 9 12 15 18 9"></polyline>
            </svg>
          </button>

          <div
            v-if="isLoggedIn && showProfileMenu"
            class="absolute right-0 top-12 z-30 w-56 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-gray-100 dark:border-slate-700 overflow-hidden text-slate-800 dark:text-white"
          >
            <div class="px-4 py-3 border-b border-gray-100 dark:border-slate-700">
              <p class="font-bold truncate">
                {{ currentUser?.fullName || 'User' }}
              </p>
              <p class="text-xs text-gray-500 truncate">
                {{ currentUser?.email }}
              </p>
            </div>

            <router-link
              to="/profile"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Profile
            </router-link>

            <router-link
              to="/my-tickets"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              My Tickets
            </router-link>

            <router-link
              to="/booking"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Booking
            </router-link>

            <router-link
              to="/membership"
              class="block px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700"
              @click="showProfileMenu = false"
            >
              Membership
            </router-link>
          </div>
        </div>

        <button
          @click.stop="emit('toggle-theme')"
          class="p-2.5 rounded-full hover:bg-white/10 transition text-sm font-bold uppercase tracking-[0.2em]"
        >
          {{ isDark ? 'Moon' : 'Sun' }}
        </button>
      </div>
    </div>

    <div
      class="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-4 md:px-8 py-3 overflow-x-auto shadow-sm"
    >
      <nav class="flex justify-center gap-12 lg:gap-16 items-center whitespace-nowrap">
        <router-link
          v-for="item in navItems"
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
