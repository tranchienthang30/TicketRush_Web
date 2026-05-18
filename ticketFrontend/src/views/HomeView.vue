<script setup>
import { computed, onMounted, ref } from "vue";
import { getEvents } from "../api/ticketRushApi";

const movies = ref([]);
const loading = ref(true);
const error = ref("");
const activeHeroIndex = ref(0);

const fallbackImages = [
  "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1400&q=85",
  "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1400&q=85",
  "https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=1400&q=85",
  "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=1400&q=85",
  "https://images.unsplash.com/photo-1497032205916-ac775f0649ae?auto=format&fit=crop&w=1400&q=85",
];

async function loadHome() {
  loading.value = true;
  error.value = "";

  try {
    const data = await getEvents({ size: 24 });
    movies.value = data.content || [];
  } catch (err) {
    error.value = "Could not load featured events. Please check the backend API.";
  } finally {
    loading.value = false;
  }
}

function normalizeListingType(movie) {
  return String(movie.listingType || "NOW_SHOWING").trim().toUpperCase().replace(/[\s-]+/g, "_");
}

function movieImage(movie, index = 0) {
  if (movie?.image) return movie.image;
  const seed = String(movie?.id || movie?.title || index);
  const fallbackIndex = [...seed].reduce((sum, char) => sum + char.charCodeAt(0), 0) % fallbackImages.length;
  return fallbackImages[fallbackIndex];
}

function movieTarget(movie) {
  return movie.bookable ? `/booking?eventId=${movie.id}` : "/events";
}

function movieMeta(movie) {
  return [movie.category || "Event", movie.date].filter(Boolean).join("   ");
}

function eventSubtitle(movie) {
  return [movie.price, movie.date, movie.location].filter(Boolean).slice(0, 2).join(" / ");
}

function normalizeTag(movie) {
  return String(movie.tag || "").trim().toUpperCase().replace(/[\s-]+/g, "_");
}

function formatDuration(minutes) {
  if (!minutes) return "";
  const total = Number(minutes);
  if (!Number.isFinite(total) || total <= 0) return "";
  const hours = Math.floor(total / 60);
  const remainingMinutes = total % 60;
  if (hours > 0 && remainingMinutes > 0) return `${hours}h ${remainingMinutes}m`;
  if (hours > 0) return `${hours}h`;
  return `${remainingMinutes}m`;
}

function previousHero() {
  if (heroMovies.value.length === 0) return;
  activeHeroIndex.value = (activeHeroIndex.value - 1 + heroMovies.value.length) % heroMovies.value.length;
}

function nextHero() {
  if (heroMovies.value.length === 0) return;
  activeHeroIndex.value = (activeHeroIndex.value + 1) % heroMovies.value.length;
}

const nowShowing = computed(() => {
  const currentMovies = movies.value.filter((movie) => normalizeListingType(movie) !== "UPCOMING");
  return (currentMovies.length > 0 ? currentMovies : movies.value).slice(0, 8);
});

const upcomingMovies = computed(() =>
  movies.value.filter((movie) => normalizeListingType(movie) === "UPCOMING").slice(0, 8),
);

const heroMovies = computed(() => {
  const source = nowShowing.value.length > 0 ? nowShowing.value : movies.value;
  return source.slice(0, 4);
});

const heroMovie = computed(() => heroMovies.value[activeHeroIndex.value] || null);

const promotionEvents = computed(() => {
  const specialEvents = movies.value.filter((movie) => {
    const listingType = normalizeListingType(movie);
    const tag = normalizeTag(movie);
    return listingType === "SPECIAL" || ["SPECIAL", "SELLING_FAST", "HOT"].includes(tag);
  });

  const source = specialEvents.length > 0 ? specialEvents : nowShowing.value;
  return source.slice(0, 3);
});

const highlightEvents = computed(() => {
  const promotionIds = new Set(promotionEvents.value.map((movie) => movie.id));
  const source = movies.value.filter((movie) => !promotionIds.has(movie.id));
  return (source.length > 0 ? source : movies.value).slice(0, 3);
});

onMounted(loadHome);
</script>

<template>
  <div class="home-shell">
    <section class="hero-wrap">
      <div class="hero-stage">
        <template v-if="heroMovie">
          <img
            :src="movieImage(heroMovie, activeHeroIndex)"
            :alt="heroMovie.title"
            class="hero-image"
          />
          <div class="hero-shade"></div>
          <div class="hero-side-shade"></div>

          <div class="hero-copy">
            <p class="hero-kicker">
              {{ heroMovie.tag || "Now Showing" }}
            </p>
            <h1 class="hero-title">
              {{ heroMovie.title }}
            </h1>
            <p class="hero-meta">
              {{ heroMovie.category || "Event" }}
              <span v-if="formatDuration(heroMovie.durationMinutes)"> / {{ formatDuration(heroMovie.durationMinutes) }}</span>
              <span v-if="heroMovie.date"> / {{ heroMovie.date }}</span>
            </p>
            <div class="hero-actions">
              <router-link
                :to="movieTarget(heroMovie)"
                class="hero-primary"
              >
                {{ heroMovie.bookable ? "Buy ticket" : "Coming soon" }}
              </router-link>
              <router-link
                to="/events"
                class="hero-secondary"
              >
                All events
              </router-link>
            </div>
          </div>

          <div class="hero-controls">
            <button
              type="button"
              class="hero-arrow hero-arrow-left"
              aria-label="Previous banner"
              @click="previousHero"
            >
              <svg class="hero-chevron" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 18L9 12L15 6" />
              </svg>
            </button>
            <button
              type="button"
              class="hero-arrow hero-arrow-right"
              aria-label="Next banner"
              @click="nextHero"
            >
              <svg class="hero-chevron" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 18L15 12L9 6" />
              </svg>
            </button>
          </div>

          <div class="hero-dots">
            <button
              v-for="(_, index) in heroMovies"
              :key="index"
              type="button"
              class="hero-dot"
              :class="{ 'hero-dot-active': activeHeroIndex === index }"
              :aria-label="`Show banner ${index + 1}`"
              @click="activeHeroIndex = index"
            ></button>
          </div>
        </template>
      </div>
    </section>

    <main class="home-main">
      <div
        v-if="loading"
        class="home-state"
      >
        Loading featured events...
      </div>

      <div
        v-else-if="error"
        class="home-error"
      >
        {{ error }}
      </div>

      <template v-else>
        <div class="movie-column">
          <section>
            <div class="section-head">
              <div>
                <h2 class="section-title">
                  <span class="section-dot"></span>
                  Available now
                </h2>
                <div class="section-line"></div>
              </div>
              <router-link to="/events" class="section-link">
                Xem tất cả →
              </router-link>
            </div>

            <div
              v-if="nowShowing.length === 0"
              class="home-state"
            >
              No featured events are available yet.
            </div>

            <div v-else class="movie-grid">
              <router-link
                v-for="(movie, index) in nowShowing"
                :key="movie.id"
                :to="movieTarget(movie)"
                class="movie-card"
              >
                <div class="movie-poster">
                  <img
                    :src="movieImage(movie, index)"
                    :alt="movie.title"
                  />
                </div>
                <p class="movie-meta">
                  {{ movieMeta(movie) }}
                </p>
                <h3 class="movie-title">
                  {{ movie.title }}
                </h3>
              </router-link>
            </div>
          </section>

          <section v-if="upcomingMovies.length">
            <div class="section-head">
              <div>
                <h2 class="section-title">
                  <span class="section-dot"></span>
                  Upcoming events
                </h2>
                <div class="section-line"></div>
              </div>
              <router-link to="/events" class="section-link">
                Xem tất cả →
              </router-link>
            </div>

            <div class="movie-grid">
              <router-link
                v-for="(movie, index) in upcomingMovies"
                :key="movie.id"
                to="/events"
                class="movie-card"
              >
                <div class="movie-poster">
                  <img
                    :src="movieImage(movie, index + 8)"
                    :alt="movie.title"
                  />
                </div>
                <p class="movie-meta">
                  {{ movieMeta(movie) }}
                </p>
                <h3 class="movie-title">
                  {{ movie.title }}
                </h3>
              </router-link>
            </div>
          </section>
        </div>

        <aside
          v-if="promotionEvents.length || highlightEvents.length"
          class="home-sidebar"
        >
          <section v-if="promotionEvents.length">
            <div class="section-head">
              <div>
                <h2 class="section-title">Khuyến mãi</h2>
                <div class="section-line"></div>
              </div>
              <router-link to="/events" class="section-link">
                Xem tất cả →
              </router-link>
            </div>

            <div class="side-list">
              <router-link
                v-for="(promotion, index) in promotionEvents"
                :key="promotion.id"
                :to="movieTarget(promotion)"
                class="side-tile side-tile-large"
              >
                <img
                  :src="movieImage(promotion, index + 12)"
                  :alt="promotion.title"
                />
                <div class="side-overlay"></div>
                <div class="side-copy">
                  <p>{{ promotion.title }}</p>
                  <span>{{ eventSubtitle(promotion) }}</span>
                </div>
              </router-link>
            </div>
          </section>

          <section v-if="highlightEvents.length">
            <div class="section-head">
              <div>
                <h2 class="section-title">Event highlights</h2>
                <div class="section-line"></div>
              </div>
              <router-link to="/events" class="section-link">
                Xem tất cả →
              </router-link>
            </div>

            <div class="side-list">
              <router-link
                v-for="(highlight, index) in highlightEvents"
                :key="highlight.id"
                :to="movieTarget(highlight)"
                class="side-tile"
              >
                <img
                  :src="movieImage(highlight, index + 16)"
                  :alt="highlight.title"
                />
                <div class="side-overlay"></div>
                <p class="event-title">
                  {{ highlight.title }}
                </p>
              </router-link>
            </div>
          </section>
        </aside>
      </template>
    </main>
  </div>
</template>

<style scoped>
.home-shell {
  min-height: 100vh;
  background: #f8fafc;
  color: #0f172a;
  padding-bottom: 64px;
}

:global(.dark) .home-shell {
  background: #0f172a;
  color: #f8fafc;
}

.hero-wrap {
  max-width: 1820px;
  margin: 0 auto;
  padding: 20px 20px 0;
}

.hero-stage {
  position: relative;
  height: 560px;
  overflow: hidden;
  border: 1px solid rgba(0, 100, 148, 0.18);
  border-radius: 24px;
  background: #006494;
  box-shadow: 0 24px 60px rgba(0, 100, 148, 0.18);
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-shade,
.hero-side-shade {
  position: absolute;
  inset: 0;
}

.hero-shade {
  background: linear-gradient(to top, rgba(0, 100, 148, 0.92) 0%, rgba(0, 100, 148, 0.46) 34%, rgba(0, 100, 148, 0.04) 72%);
}

.hero-side-shade {
  background: linear-gradient(to right, rgba(0, 70, 104, 0.9), rgba(0, 100, 148, 0.12) 52%, rgba(0, 70, 104, 0.28));
}

.hero-copy {
  position: absolute;
  left: 56px;
  bottom: 62px;
  max-width: 650px;
}

.hero-kicker {
  margin-bottom: 14px;
  color: #fb923c;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.28em;
  text-transform: uppercase;
}

.hero-title {
  color: #fff;
  font-size: 56px;
  font-weight: 950;
  line-height: 0.98;
  text-transform: uppercase;
}

.hero-meta {
  margin-top: 18px;
  color: #d6dde8;
  font-size: 15px;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.hero-primary,
.hero-secondary {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  border-radius: 999px;
  padding: 0 24px;
  font-size: 13px;
  font-weight: 950;
  text-transform: uppercase;
  transition: background-color 160ms ease, border-color 160ms ease;
}

.hero-primary {
  background: #f97316;
  color: #fff;
  box-shadow: 0 16px 30px rgba(249, 115, 22, 0.28);
}

.hero-primary:hover {
  background: #ea580c;
}

.hero-secondary {
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.hero-secondary:hover {
  background: rgba(255, 255, 255, 0.18);
}

.hero-controls {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-arrow {
  position: absolute;
  top: 50%;
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  transform: translateY(-50%);
  border: 0;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.38);
  color: #fff;
  padding: 0;
  pointer-events: auto;
}

.hero-arrow-left {
  top: calc(50% - 14.2px);
  left: 8px;
}

.hero-arrow-right {
  right: 8px;
}

.hero-arrow:hover {
  background: rgba(0, 0, 0, 0.62);
}

.hero-chevron {
  display: block;
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.hero-dots {
  position: absolute;
  bottom: 22px;
  left: 50%;
  display: flex;
  gap: 8px;
  transform: translateX(-50%);
}

.hero-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.36);
  transition: width 160ms ease, background-color 160ms ease;
}

.hero-dot-active {
  width: 30px;
  background: #f97316;
}

.home-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 34px;
  max-width: 1180px;
  margin: 34px auto 0;
  padding: 0 20px;
}

.movie-column,
.home-sidebar,
.side-list {
  display: grid;
  gap: 44px;
}

.home-state,
.home-error {
  grid-column: 1 / -1;
  border-radius: 18px;
  padding: 32px;
  text-align: center;
  font-weight: 800;
}

.home-state {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

:global(.dark) .home-state {
  border-color: #334155;
  background: #1e293b;
  color: #cbd5e1;
}

.home-error {
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #b91c1c;
}

:global(.dark) .home-error {
  border-color: rgba(248, 113, 113, 0.36);
  background: rgba(127, 29, 29, 0.22);
  color: #fecaca;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #006494;
  font-size: 23px;
  font-weight: 950;
  letter-spacing: -0.02em;
}

:global(.dark) .section-title {
  color: #f8fafc;
}

.section-dot {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #f97316;
}

.section-line {
  width: 56px;
  height: 2px;
  margin-top: 10px;
  background: #f97316;
}

.section-link {
  color: #006494;
  font-size: 14px;
  font-weight: 850;
  text-underline-offset: 8px;
}

:global(.dark) .section-link {
  color: #f8fafc;
}

.section-link:hover {
  text-decoration: underline;
}

.movie-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px 20px;
}

.movie-card {
  display: block;
  min-width: 0;
}

.movie-poster {
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
}

:global(.dark) .movie-poster {
  border-color: #334155;
  background: #1e293b;
}

.movie-poster img,
.side-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 400ms ease;
}

.movie-card:hover .movie-poster img,
.side-tile:hover img {
  transform: scale(1.045);
}

.movie-meta {
  margin-top: 12px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-title {
  display: -webkit-box;
  min-height: 42px;
  margin-top: 5px;
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  font-weight: 950;
  line-height: 1.35;
  text-transform: uppercase;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

:global(.dark) .movie-title {
  color: #f8fafc;
}

.movie-card:hover .movie-title {
  color: #f97316;
}

.side-list {
  gap: 18px;
}

.side-tile {
  position: relative;
  display: block;
  height: 98px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

:global(.dark) .side-tile {
  border-color: #334155;
  background: #1e293b;
}

.side-tile-large {
  height: 116px;
}

.side-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.82), rgba(0, 0, 0, 0.28), transparent);
}

.side-copy {
  position: absolute;
  inset: 0 auto 0 0;
  display: flex;
  max-width: 72%;
  flex-direction: column;
  justify-content: center;
  padding: 0 18px;
}

.side-copy p,
.event-title {
  color: #fff;
  font-size: 14px;
  font-weight: 950;
  line-height: 1.2;
  text-transform: uppercase;
}

.side-copy span {
  margin-top: 5px;
  color: #d6dde8;
  font-size: 12px;
  font-weight: 700;
}

.event-title {
  position: absolute;
  bottom: 16px;
  left: 16px;
  max-width: 80%;
}

@media (max-width: 1100px) {
  .home-main {
    grid-template-columns: 1fr;
  }

  .home-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .hero-stage {
    height: 430px;
    border-radius: 20px;
  }

  .hero-copy {
    right: 24px;
    left: 24px;
    bottom: 44px;
  }

  .hero-title {
    font-size: 34px;
  }

  .movie-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>
