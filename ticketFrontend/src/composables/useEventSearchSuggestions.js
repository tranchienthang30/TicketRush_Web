import { computed, onUnmounted, ref, watch } from "vue";
import { getEvents } from "@/api/ticketRushApi";

export function useEventSearchSuggestions(searchTerm, options = {}) {
  const minLength = options.minLength ?? 2;
  const size = options.size ?? 6;
  const delayMs = options.delayMs ?? 220;

  const suggestions = ref([]);
  const suggestionsLoading = ref(false);
  const suggestionsOpen = ref(false);
  const suggestionQuery = computed(() => String(searchTerm.value || "").trim());

  let timerId = null;
  let requestId = 0;

  function clearTimer() {
    if (timerId) {
      window.clearTimeout(timerId);
      timerId = null;
    }
  }

  function resetSuggestions() {
    suggestions.value = [];
    suggestionsLoading.value = false;
  }

  function openSuggestions() {
    suggestionsOpen.value = true;
  }

  function closeSuggestions() {
    suggestionsOpen.value = false;
    requestId += 1;
    clearTimer();
    resetSuggestions();
  }

  const showSuggestions = computed(
    () => suggestionsOpen.value && suggestionQuery.value.length >= minLength,
  );

  watch(
    () => [suggestionQuery.value, suggestionsOpen.value],
    ([currentQuery, isOpen]) => {
      clearTimer();

      if (!isOpen || currentQuery.length < minLength) {
        requestId += 1;
        resetSuggestions();
        return;
      }

      const currentRequestId = ++requestId;
      suggestionsLoading.value = true;

      timerId = window.setTimeout(async () => {
        try {
          const data = await getEvents({ q: currentQuery, size });
          if (currentRequestId !== requestId) return;
          suggestions.value = data?.content || [];
        } catch {
          if (currentRequestId === requestId) {
            suggestions.value = [];
          }
        } finally {
          if (currentRequestId === requestId) {
            suggestionsLoading.value = false;
          }
        }
      }, delayMs);
    },
  );

  onUnmounted(() => {
    requestId += 1;
    clearTimer();
  });

  return {
    suggestionQuery,
    suggestions,
    suggestionsLoading,
    showSuggestions,
    openSuggestions,
    closeSuggestions,
  };
}
