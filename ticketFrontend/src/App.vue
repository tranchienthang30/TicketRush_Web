<script setup>
import { ref, onMounted } from 'vue';
import Header from './components/layout/Header.vue';
import Footer from './components/layout/Footer.vue';

const isDark = ref(false);

const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark');
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

onMounted(() => {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col bg-brand-light dark:bg-brand-dark transition-colors duration-300">
    <Header :isDark="isDark" @toggle-theme="toggleTheme" />
    
    <main class="flex-grow">
      <router-view />
    </main>

    <Footer />
  </div>
</template>