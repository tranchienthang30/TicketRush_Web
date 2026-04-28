import { startSite } from "./site-core.js";

const adminStatGrid = document.querySelector("#admin-stat-grid");
const adminAudienceGrid = document.querySelector("#admin-audience-grid");
const adminForm = document.querySelector("#admin-form");

let latestApp = null;

adminForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!latestApp) return;

  const form = new FormData(event.currentTarget);
  const payload = Object.fromEntries(form.entries());
  payload.startTime = new Date(payload.startTime).toISOString();
  payload.zones = [
    { code: "VIP", label: "VIP Hall", rowsCount: 4, seatsPerRow: 8, priceCents: 160000 },
    { code: "STD", label: "Standard Hall", rowsCount: 8, seatsPerRow: 14, priceCents: 95000 },
    { code: "BAL", label: "Balcony", rowsCount: 4, seatsPerRow: 10, priceCents: 55000 },
  ];
  await latestApp.api("/api/admin/shows", { method: "POST", body: JSON.stringify(payload) });
  await latestApp.bootstrap();
});

startSite({
  getBanner({ currentShow, state }) {
    const show = currentShow();
    return {
      title: "Admin Dashboard",
      summary: "Monitor sales, occupancy, and audience mix while controlling new launches from the same workspace.",
      stats: [`${state.dashboard?.events?.length || 0} sessions`, "Live metrics", show?.venue],
      actions: [{ href: "/booking.html", label: "Open customer flow", kind: "ghost-button" }],
      slides: [
        { kicker: "Live metrics", title: "Operations overview", body: "Track occupancy, revenue, and queue behavior from the same admin surface.", tone: "#38bdf8" },
        { kicker: "Launch control", title: `${state.dashboard?.events?.length || 0} active sessions`, body: "Add new sessions and manage pricing zones directly from the dashboard flow.", tone: "#f59e0b" },
        { kicker: "Audience mix", title: "Customer insight", body: "Demographic snapshots help organizers understand who is buying and when.", tone: "#ef4444" },
      ],
    };
  },
  render(app) {
    latestApp = app;
    const { state } = app;
    adminStatGrid.innerHTML = state.dashboard.events
      .map(
        (event) => `
          <article class="stat-card">
            <p class="eyebrow">${new Date(event.startTime).toLocaleDateString("en-GB")}</p>
            <h3>${event.title}</h3>
            <p><strong>${event.revenueLabel}</strong></p>
            <p class="muted">Sold ${event.soldSeats}/${event.totalSeats} seats / locked ${event.lockedSeats} / fill ${event.fillRate}%</p>
          </article>
        `
      )
      .join("");
    adminAudienceGrid.innerHTML = state.dashboard.audience.length
      ? state.dashboard.audience
          .map((item) => `<p>${item.gender} / ${item.ageBucket}: <strong>${item.count}</strong></p>`)
          .join("")
      : "<p class='muted'>No audience data yet.</p>";
  },
});
