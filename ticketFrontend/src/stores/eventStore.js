import { defineStore } from "pinia";
import { ref } from "vue";

const EVENTS_KEY = "ticketrush_my_events_draft";

export const useEventStore = defineStore("events", () => {
  const myEvents = ref(readStoredEvents());

  function createEvent(payload) {
    const event = {
      ...JSON.parse(JSON.stringify(payload)),
      id: crypto.randomUUID(),
      status: "DRAFT",
      createdAt: new Date().toISOString(),
    };
    myEvents.value.unshift(event);
    localStorage.setItem(EVENTS_KEY, JSON.stringify(myEvents.value));
    return event;
  }

  return {
    myEvents,
    createEvent,
  };
});

function readStoredEvents() {
  try {
    return JSON.parse(localStorage.getItem(EVENTS_KEY) || "[]");
  } catch {
    localStorage.removeItem(EVENTS_KEY);
    return [];
  }
}
