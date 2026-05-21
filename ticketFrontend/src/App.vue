<script setup>
import { ref, onMounted } from "vue";
import Header from "./layout/Header.vue";
import Footer from "./layout/Footer.vue";

const isDark = ref(document.documentElement.classList.contains("dark"));

function applyTheme(nextTheme, persist = true) {
  isDark.value = nextTheme;
  document.documentElement.classList.toggle("dark", nextTheme);
  document.body?.classList.toggle("dark", nextTheme);
  document.documentElement.style.colorScheme = nextTheme ? "dark" : "light";
  if (persist) {
    localStorage.setItem("theme", nextTheme ? "dark" : "light");
  }
}

const toggleTheme = () => {
  applyTheme(!isDark.value);
};

onMounted(() => {
  applyTheme(isDark.value, false);
});
</script>

<template>
  <div
    class="min-h-screen flex flex-col bg-brand-light text-slate-900 transition-colors duration-300 dark:bg-slate-900 dark:text-slate-100"
    :class="{ dark: isDark }"
  >
    <Header :isDark="isDark" @toggle-theme="toggleTheme" />

    <main class="flex-grow">
      <router-view />
    </main>

    <Footer />
  </div>
</template>
