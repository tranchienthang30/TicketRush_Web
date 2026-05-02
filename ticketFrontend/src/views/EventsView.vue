<script setup>
import { ref, onMounted } from 'vue';
import EventCard from '../components/EventCard.vue'; // 1. Import Component vào đây

// 1. Mock Data (Giả lập dữ liệu trả về từ Backend API)
const categories = ref([
  {
    id: 1,
    name: "Music & Concerts",
    description: "Experience the best live performances and music festivals.",
    events: [
      { id: 101, title: "Rock the Night 2024", date: "Dec 15, 2024", location: "Hanoi Opera House", price: "450,000 VND", tag: "Hot", image: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=500&q=80" },
      { id: 102, title: "Jazz & Wine Evening", date: "Jan 10, 2025", location: "Saigon Rooftop", price: "1,200,000 VND", tag: "Exclusive", image: "https://images.unsplash.com/photo-1514525253344-a8130a2185d0?auto=format&fit=crop&w=500&q=80" },
      { id: 103, title: "Underground EDM Rave", date: "Dec 30, 2024", location: "Warehouse District", price: "300,000 VND", tag: "New", image: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=500&q=80" },
    ]
  },
  {
    id: 2,
    name: "Sports & Fitness",
    description: "Join the most exciting matches and athletic challenges.",
    events: [
      { id: 201, title: "City Half-Marathon", date: "Feb 20, 2025", location: "Hoan Kiem Lake", price: "500,000 VND", tag: "Early Bird", image: "https://images.unsplash.com/photo-1533560904424-a0c61dc306fc?auto=format&fit=crop&w=500&q=80" },
      { id: 202, title: "Basketball All-Stars", date: "Mar 05, 2025", location: "Military Zone 7 Stadium", price: "250,000 VND", tag: "Selling Fast", image: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=500&q=80" },
    ]
  },
  {
    id: 3,
    name: "Workshops & Seminars",
    description: "Learn new skills and network with industry experts.",
    events: [
      { id: 301, title: "UI/UX Design Masterclass", date: "Dec 22, 2024", location: "Tech Hub Center", price: "2,000,000 VND", tag: "Limited Space", image: "https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=500&q=80" },
      { id: 302, title: "Digital Marketing 101", date: "Jan 15, 2025", location: "Online (Zoom)", price: "FREE", tag: "Open", image: "https://images.unsplash.com/photo-1432888622747-4eb9a8f2c20e?auto=format&fit=crop&w=500&q=80" },
    ]
  }
]);

// Sau này bạn sẽ dùng hàm này để gọi API thật
// onMounted(async () => {
//   const response = await fetch('your-backend-api/categories');
//   categories.value = await response.json();
// });
</script>

<template>
  <div class="bg-brand-light dark:bg-slate-900 min-h-screen pb-20">
    <div class="bg-brand-navy py-16 px-4 text-center text-white mb-12 shadow-inner">
      <h1 class="text-4xl md:text-5xl text-brand-orange mb-4 uppercase tracking-tighter font-black">
        Discover Events
      </h1>
      <p class="text-blue-100 max-w-2xl mx-auto text-lg">
        Explore a wide range of categories and find the perfect event for you.
      </p>
    </div>

    <div class="max-w-7xl mx-auto px-4 md:px-8">
      
      <section v-for="category in categories" :key="category.id" class="mb-20">
        
        <div class="mb-8 border-b border-gray-200 dark:border-slate-800 pb-4">
          <div class="flex items-center gap-3 mb-2">
            <h2 class="text-3xl font-black text-brand-navy dark:text-white uppercase tracking-tight">
              {{ category.name }}
            </h2>
            <span class="bg-brand-orange/10 text-brand-orange text-sm font-bold px-3 py-1 rounded-full border border-brand-orange/20">
              {{ category.events.length }} Events
            </span>
          </div>
          <p class="text-gray-500 dark:text-gray-400 italic">{{ category.description }}</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
          <EventCard 
            v-for="event in category.events" 
            :key="event.id" 
            :event="event" 
          />
        </div>

      </section>

    </div>
  </div>
</template>

<style scoped>
/* Bạn có thể thêm các hiệu ứng riêng cho trang này ở đây */
</style>