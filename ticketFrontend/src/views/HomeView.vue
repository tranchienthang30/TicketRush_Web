<script setup>
import { ref } from 'vue';
import EventCard from '../components/EventCard.vue'; // 1. Tận dụng component dùng chung

// Dữ liệu mẫu hiển thị trên Homepage (thường là các sự kiện nổi bật nhất)
const featuredCategories = ref([
  {
    id: 1,
    name: "🔥 Trending Now",
    events: [
      { id: 101, title: "Summer Vibe Fest 2024", date: "Dec 20, 2024", location: "Ho Chi Minh City", price: "500,000 VND", tag: "Hot", image: "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=500&q=80" },
      { id: 102, title: "Live Concert: Autumn Leaves", date: "Aug 15, 2024", location: "Hanoi", price: "350,000 VND", tag: "New", image: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=500&q=80" },
      { id: 103, title: "EDM Nightout", date: "Sep 01, 2024", location: "Da Nang", price: "400,000 VND", tag: "Selling Fast", image: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=500&q=80" },
      { id: 104, title: "Acoustic in the Woods", date: "Oct 10, 2024", location: "Da Lat", price: "250,000 VND", tag: "Popular", image: "https://images.unsplash.com/photo-1514525253344-a8130a2185d0?auto=format&fit=crop&w=500&q=80" },
    ]
  },
  {
    id: 2,
    name: "⚽ Sports Highlights",
    events: [
      { id: 201, title: "V-League Final", date: "Nov 05, 2024", location: "My Dinh Stadium", price: "150,000 VND", tag: "Hot", image: "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=500&q=80" },
      { id: 202, title: "42km City Marathon", date: "Nov 20, 2024", location: "Hanoi", price: "800,000 VND", tag: "Early Bird", image: "https://images.unsplash.com/photo-1533560904424-a0c61dc306fc?auto=format&fit=crop&w=500&q=80" },
      { id: 203, title: "Cross-Country Cycling", date: "Dec 01, 2024", location: "Hue", price: "Free", tag: "New", image: "https://images.unsplash.com/photo-1541534741688-6078c6bc35e5?auto=format&fit=crop&w=500&q=80" },
      { id: 204, title: "VBA Basketball Championship", date: "Dec 15, 2024", location: "Ho Chi Minh City", price: "200,000 VND", tag: "", image: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=500&q=80" },
    ]
  }
]);
</script>

<template>
  <div class="max-w-7xl mx-auto py-10 px-4 md:px-8">
    
    <section class="w-full h-64 md:h-96 bg-brand-navy rounded-3xl mb-16 flex items-center justify-center overflow-hidden relative shadow-2xl">
      <div class="text-center z-10 px-4">
        <h2 class="text-4xl md:text-7xl font-black text-white mb-6 uppercase tracking-tighter italic">
          STAR<span class="text-brand-orange">LIGHT</span> EVENTS
        </h2>
        <p class="text-blue-100 mb-8 text-lg font-medium">Book your next extraordinary experience</p>
        <button class="bg-brand-orange hover:scale-110 transition-transform px-10 py-4 rounded-full text-white font-black shadow-lg uppercase tracking-widest text-sm">
          Explore Now
        </button>
      </div>
      <div class="absolute inset-0 bg-gradient-to-r from-brand-navy via-transparent to-brand-navy opacity-50"></div>
      <img src="https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80" class="absolute inset-0 w-full h-full object-cover opacity-30" alt="Banner background" />
    </section>

    <section v-for="category in featuredCategories" :key="category.id" class="mb-20">
      
      <div class="flex justify-between items-end mb-8 border-b border-gray-100 dark:border-slate-800 pb-4">
        <div>
          <h3 class="text-3xl font-black text-brand-navy dark:text-white tracking-tight uppercase">
            {{ category.name }}
          </h3>
          <div class="h-1.5 w-20 bg-brand-orange mt-2 rounded-full"></div>
        </div>
        <router-link to="/events" class="text-brand-orange font-bold hover:underline flex items-center gap-2 group">
          View all <span class="group-hover:translate-x-1 transition-transform">→</span>
        </router-link>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <EventCard 
          v-for="event in category.events" 
          :key="event.id" 
          :event="event" 
        />
      </div>
      
    </section>

  </div>
</template>