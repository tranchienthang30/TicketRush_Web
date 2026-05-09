<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-3xl font-bold mb-6">Create New Event</h1>

    <form @submit.prevent="handleCreateEvent" class="space-y-6">
      <div>
        <label class="block font-semibold mb-1">Event Title</label>
        <input v-model="form.title" type="text" placeholder="Enter event title"
               class="w-full border rounded-lg p-2" />
      </div>

      <div>
        <label class="block font-semibold mb-1">Description</label>
        <textarea v-model="form.description" rows="4" class="w-full border rounded-lg p-2"></textarea>
      </div>

      <div>
        <label class="block font-semibold mb-1">Thumbnail URL</label>
        <input v-model="form.thumbnailUrl" type="text" class="w-full border rounded-lg p-2" />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block font-semibold mb-1">Location</label>
          <input v-model="form.location" type="text" class="w-full border rounded-lg p-2" />
        </div>

        <div>
          <label class="block font-semibold mb-1">Price per Ticket</label>
          <input v-model.number="form.price" type="number" class="w-full border rounded-lg p-2" />
        </div>

        <div>
          <label class="block font-semibold mb-1">Start Time</label>
          <input v-model="form.startTime" type="datetime-local" class="w-full border rounded-lg p-2" />
        </div>

        <div>
          <label class="block font-semibold mb-1">End Time</label>
          <input v-model="form.endTime" type="datetime-local" class="w-full border rounded-lg p-2" />
        </div>

        <div>
          <label class="block font-semibold mb-1">Total Tickets</label>
          <input v-model.number="form.totalTickets" type="number" class="w-full border rounded-lg p-2" />
        </div>
      </div>

      <!-- Seat Configuration -->
      <EventSeatConfig v-model="form.seats" />

      <button type="submit" class="bg-brand-orange text-white px-6 py-3 rounded-lg font-bold">
        Create Event
      </button>
    </form>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useEventStore } from '../stores/eventStore';
import EventSeatConfig from '../components/EventSeatConfig.vue';

const store = useEventStore();

const form = reactive({
  title: '',
  description: '',
  thumbnailUrl: '',
  location: '',
  startTime: '',
  endTime: '',
  price: 0,
  totalTickets: 0,
  seats: [] // {row, number, type: 'VIP'|'Regular'}
});

function handleCreateEvent() {
  // validation cơ bản
  if (!form.title || !form.startTime || !form.endTime || form.totalTickets <= 0) return;

  // lưu tạm vào store (frontend)
  store.createEvent(form);

  alert('Event created successfully! You can manage it in My Events.');
}
</script>