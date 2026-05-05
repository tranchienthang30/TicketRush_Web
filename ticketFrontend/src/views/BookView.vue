<script setup>
import { ref, computed } from 'vue';

// 1. Dữ liệu phim giả lập (Giữ nguyên logic cũ)
const movie = ref({
  title: "PHI VỤ THANH TOÁN HÀO MÔN-T18",
  format: "2D",
  duration: "105 phút",
  director: "John Patton Ford",
  cast: "Glen Powell, Margaret Qualley, Topher Grace, Ed Harris",
  releaseDate: "01/05/2026",
  description: "Bị gia đình giàu có từ mặt ngay từ khi chào đời, Becket Redfellow — một người lao động bình dân — sẵn sàng làm mọi thứ để giành lại quyền thừa kế của mình.",
  rating: "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên (18+)",
  banner: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=1920&q=80",
  poster: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=400&q=80"
});

const dates = ref([
  { id: 1, month: "Th. 05", day: "05", weekday: "Thứ ba" },
  { id: 2, month: "Th. 05", day: "06", weekday: "Thứ tư" },
  { id: 3, month: "Th. 05", day: "07", weekday: "Thứ năm" },
]);
const selectedDate = ref(1);

const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'];
const cols = 14;
const PRICES = { normal: 50000, vip: 85000, couple: 120000 };
const seats = ref([]);

const generateSeats = () => {
  rows.forEach(row => {
    let rowSeats = [];
    for (let i = cols; i >= 1; i--) {
      let type = 'normal';
      let isBooked = false;
      let isHidden = false;
      if (row === 'I') {
        if (i > 12 || i === 5 || i === 6) isHidden = true;
        else type = 'couple';
      } else if (['D', 'E', 'F', 'G', 'H'].includes(row)) {
        if (i >= 3 && i <= 12) type = 'vip';
      }
      if (!isHidden && Math.random() < 0.1) isBooked = true;
      rowSeats.push({ id: `${row}${i}`, row, type, isBooked, isHidden, price: PRICES[type] });
    }
    seats.value.push(rowSeats);
  });
};
generateSeats();

const selectedSeats = ref([]);
const toggleSeat = (seat) => {
  if (seat.isBooked || seat.isHidden) return;
  const index = selectedSeats.value.findIndex(s => s.id === seat.id);
  if (index > -1) selectedSeats.value.splice(index, 1);
  else selectedSeats.value.push(seat);
};

const totalPrice = computed(() => selectedSeats.value.reduce((sum, seat) => sum + seat.price, 0));
const formatPrice = (p) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-950 font-sans">
    
    <section class="relative h-[250px] md:h-[350px] w-full overflow-hidden">
      <div class="absolute inset-0">
        <img :src="movie.banner" class="object-cover w-full h-full" alt="Banner" />
        <div class="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/80 to-slate-950/30"></div>
      </div>
      
      <div class="absolute inset-0 z-10 flex items-center px-6 md:px-12">
        <div class="max-w-6xl mx-auto w-full flex gap-8 items-end pb-8">
          <img :src="movie.poster" class="hidden md:block w-40 h-60 object-cover rounded-xl shadow-2xl border-4 border-white/20" />
          <div class="text-white drop-shadow-[0_3px_12px_rgba(0,0,0,0.85)]">
            <div class="flex items-center gap-3 mb-2">
              <span class="bg-red-600 text-white text-[10px] font-bold px-2 py-0.5 rounded">T18</span>
              <h1 class="text-2xl md:text-4xl font-black tracking-tight uppercase">{{ movie.title }}</h1>
            </div>
            <p class="text-slate-100 text-sm md:text-base font-semibold">⏳ {{ movie.duration }} | 🎬 {{ movie.director }}</p>
          </div>
        </div>
      </div>
    </section>

    <main class="max-w-7xl mx-auto py-8 px-4">
      
      <div class="flex justify-center gap-2 mb-10">
        <button 
          v-for="date in dates" :key="date.id"
          @click="selectedDate = date.id"
          class="w-24 h-24 rounded-2xl flex flex-col items-center justify-center transition-all border-2"
          :class="selectedDate === date.id 
            ? 'bg-slate-900 border-slate-900 text-white shadow-lg' 
            : 'bg-white border-slate-200 text-slate-800 hover:border-slate-400 hover:bg-slate-50'"
        >
          <span class="text-[10px] uppercase font-bold opacity-70">{{ date.month }}</span>
          <span class="text-3xl font-black my-1">{{ date.day }}</span>
          <span class="text-[10px] font-medium">{{ date.weekday }}</span>
        </button>
      </div>

      <div class="bg-white rounded-[40px] p-8 md:p-12 border border-slate-200 shadow-sm">
        
        <div class="relative w-full mb-16 flex flex-col items-center">
          <div class="w-2/3 h-2 bg-slate-300 rounded-full shadow-[0_10px_20px_rgba(0,0,0,0.05)]"></div>
          <span class="mt-4 text-slate-700 font-black tracking-[0.4em] text-xs uppercase">SCREEN</span>
        </div>

        <div class="overflow-x-auto pb-6">
          <div class="min-w-[800px] flex flex-col items-center gap-3">
            <div v-for="(rowSeats, rowIndex) in seats" :key="rowIndex" class="flex gap-3 items-center">
              <div class="w-6 text-xs font-black text-slate-700">{{ rows[rowIndex] }}</div>
              
              <div 
                v-for="seat in rowSeats" :key="seat.id"
                @click="toggleSeat(seat)"
                class="w-8 h-8 md:w-10 md:h-10 rounded-lg flex items-center justify-center transition-all duration-200 text-[10px] font-bold"
                :class="[
                  seat.isHidden ? 'invisible' : 'cursor-pointer border-b-4',
                  seat.isBooked ? 'bg-slate-200 border-slate-300 text-slate-600 cursor-not-allowed' : '',
                  !seat.isBooked && !selectedSeats.includes(seat) && seat.type === 'normal' ? 'bg-white border-slate-300 text-slate-900 hover:bg-slate-100' : '',
                  !seat.isBooked && !selectedSeats.includes(seat) && seat.type === 'vip' ? 'bg-orange-100 border-orange-300 text-orange-900 hover:bg-orange-200' : '',
                  !seat.isBooked && !selectedSeats.includes(seat) && seat.type === 'couple' ? 'bg-red-100 border-red-300 text-red-900 hover:bg-red-200' : '',
                  selectedSeats.includes(seat) ? 'bg-blue-600 border-blue-800 text-white -translate-y-1 shadow-md' : ''
                ]"
              >
                <span v-if="!seat.isBooked && !seat.isHidden">{{ seat.id }}</span>
                <span v-if="seat.isBooked">✖</span>
              </div>

              <div class="w-6 text-xs font-black text-slate-700">{{ rows[rowIndex] }}</div>
            </div>
          </div>
        </div>

        <div class="flex flex-wrap justify-center gap-8 mt-12 text-xs font-black text-slate-800 uppercase tracking-wider">
          <div class="flex items-center gap-2"><div class="w-4 h-4 bg-white border border-slate-200 rounded"></div> Ghế thường</div>
          <div class="flex items-center gap-2"><div class="w-4 h-4 bg-orange-100 border border-orange-200 rounded"></div> Ghế VIP</div>
          <div class="flex items-center gap-2"><div class="w-4 h-4 bg-red-100 border border-red-200 rounded"></div> Ghế đôi</div>
          <div class="flex items-center gap-2"><div class="w-4 h-4 bg-blue-600 rounded"></div> Đang chọn</div>
          <div class="flex items-center gap-2"><div class="w-4 h-4 bg-slate-200 rounded"></div> Đã bán</div>
        </div>
      </div>

      <div class="mt-8 bg-white border border-slate-100 rounded-3xl p-6 shadow-xl flex flex-col md:flex-row items-center justify-between gap-6">
        <div class="flex items-center gap-6">
          <div class="hidden sm:block p-4 bg-slate-100 rounded-2xl text-slate-800">🎟️</div>
          <div>
            <p class="text-xs font-black text-slate-700 uppercase mb-1">Ghế đã chọn</p>
            <p class="text-xl font-black text-slate-900">
              {{ selectedSeats.length > 0 ? selectedSeats.map(s => s.id).join(', ') : 'Chưa chọn ghế' }}
            </p>
          </div>
          <div class="h-10 w-[1px] bg-slate-100 mx-2 hidden md:block"></div>
          <div>
            <p class="text-xs font-black text-slate-700 uppercase mb-1">Tổng thanh toán</p>
            <p class="text-2xl font-black text-red-600">{{ formatPrice(totalPrice) }}</p>
          </div>
        </div>
        
        <button 
          :disabled="selectedSeats.length === 0"
          class="w-full md:w-auto px-12 py-4 bg-slate-900 text-white rounded-2xl font-black uppercase tracking-widest hover:bg-slate-800 disabled:bg-slate-200 disabled:text-slate-400 transition-all shadow-lg"
        >
          Tiếp tục thanh toán
        </button>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* Scrollbar mượt cho phần sơ đồ ghế */
::-webkit-scrollbar {
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}
</style>
