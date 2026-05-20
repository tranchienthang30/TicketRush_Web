<template>
  <div class="border p-4 rounded-lg">
    <h2 class="text-xl font-bold mb-2">Seat Configuration</h2>
    <div class="grid grid-cols-10 gap-1">
      <div
        v-for="(seat, index) in seatsGrid"
        :key="index"
        :class="seat.type === 'VIP' ? 'bg-yellow-400' : 'bg-gray-300'"
        class="w-8 h-8 flex items-center justify-center cursor-pointer rounded"
        @click="toggleSeatType(index)"
      >
        {{ seat.row }}{{ seat.number }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue';
import { defineProps, defineEmits } from 'vue';

defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
});

const emits = defineEmits(['update:modelValue']);

const seatsGrid = reactive([]);

// khởi tạo lưới 5 hàng x 10 ghế
for (let r = 1; r <= 5; r++) {
  for (let n = 1; n <= 10; n++) {
    seatsGrid.push({
      row: r,
      number: n,
      type: 'Regular'
    });
  }
}

// đồng bộ với v-model
watch(seatsGrid, () => {
  emits('update:modelValue', seatsGrid);
}, { deep: true });

function toggleSeatType(index) {
  seatsGrid[index].type = seatsGrid[index].type === 'VIP' ? 'Regular' : 'VIP';
}
</script>
