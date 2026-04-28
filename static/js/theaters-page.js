import { startSite } from "./site-core.js";

const theatersGrid = document.querySelector("#theaters-grid");

startSite({
  getBanner({ currentShow, fmtTime, state }) {
    const show = currentShow();
    return {
      title: "Theater Directory",
      summary: "Switch between active branches, compare schedules, and keep the current session synced across the rest of the site.",
      stats: [`${state.catalog?.theaters?.length || 0} branches`, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/booking.html", label: "Pick seats", kind: "primary-button" }],
      slides: [
        { kicker: "Branch view", title: show?.venue || "Active cinema", body: "The selected branch stays synced across pricing, booking, and member flows.", tone: "#38bdf8" },
        { kicker: "Live session", title: show?.title || "Show preview", body: show ? `Current session starts ${fmtTime(show.startTime)}.` : "Live schedule details appear here.", tone: "#f59e0b" },
        { kicker: "Switch fast", title: "Move between cinemas instantly", body: "Change the active venue on the left and the rest of the pages update around it.", tone: "#ef4444" },
      ],
    };
  },
  render({ fmtTime, state }) {
    theatersGrid.innerHTML = state.catalog.theaters
      .map(
        (theater) => `
          <article class="movie-card">
            <p class="eyebrow">Cinema Branch</p>
            <h3>${theater.name}</h3>
            <div class="stack">
              ${theater.movies.map((movie) => `<p class="muted">${movie.title} / ${fmtTime(movie.startTime)}</p>`).join("")}
            </div>
          </article>
        `
      )
      .join("");
  },
});
