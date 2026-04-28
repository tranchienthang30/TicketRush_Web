import { startSite } from "./site-core.js";

const moviesGrid = document.querySelector("#movies-grid");

startSite({
  getBanner({ currentShow, fmtTime, state }) {
    const show = currentShow();
    return {
      title: "Now Showing & Coming Soon",
      summary: "Review the active line-up, compare release timing, and move into booking from the title that fits your night.",
      stats: [`${state.catalog?.shows?.length || 0} titles`, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/booking.html", label: "Go to booking", kind: "primary-button" }],
      slides: [
        { kicker: "Now showing", title: show?.title || "Active line-up", body: show?.description || "Current title information appears here.", tone: show?.heroColor || "#38bdf8" },
        { kicker: "Schedule", title: show?.venue || "Cinema branch", body: show ? `Session scheduled for ${fmtTime(show.startTime)}.` : "Session timing appears here.", tone: "#f59e0b" },
        { kicker: "Decision point", title: "Compare first, book second", body: "Use this area to rotate through titles before entering the seat map.", tone: "#ef4444" },
      ],
    };
  },
  render({ fmtTime, state }) {
    moviesGrid.innerHTML = state.catalog.shows
      .map(
        (show) => `
          <article class="movie-card">
            <div class="movie-cover" style="background: linear-gradient(180deg, rgba(255,255,255,0.05), rgba(0,0,0,0.3)), linear-gradient(135deg, ${show.heroColor}, #0f172a);"></div>
            <p class="eyebrow">${show.category}</p>
            <h3>${show.title}</h3>
            <p class="muted">${show.description}</p>
            <div class="quick-stats">
              <span class="mini-chip">${show.venue}</span>
              <span class="mini-chip">${fmtTime(show.startTime)}</span>
            </div>
          </article>
        `
      )
      .join("");
  },
});
