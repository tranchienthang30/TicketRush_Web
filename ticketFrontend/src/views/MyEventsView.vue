<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-3xl font-bold mb-6">My Screenings</h1>

    <div v-if="events.length === 0" class="text-gray-500">
      You have not created any screenings yet.
    </div>

    <div class="grid gap-4">
      <div v-for="event in events" :key="event.id" class="border rounded-lg p-4 flex justify-between items-center">
        <div>
          <h2 class="text-xl font-bold">{{ event.title }}</h2>
          <p class="text-gray-500">Start: {{ event.startTime }} | End: {{ event.endTime }}</p>
          <p class="text-gray-500">Seats left: {{ event.totalTickets }}</p>
        </div>
        <div class="flex gap-2">
          <button class="bg-blue-500 text-white px-4 py-2 rounded">View</button>
          <button class="bg-green-500 text-white px-4 py-2 rounded">Edit</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useEventStore } from '../stores/eventStore';

const store = useEventStore();

const events = computed(() => store.myEvents);
</script>
