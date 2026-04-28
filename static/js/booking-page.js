import { startSite } from "./site-core.js";

const queueSection = document.querySelector("#booking-queue-section");
const queuePosition = document.querySelector("#booking-queue-position");
const mainSection = document.querySelector("#booking-main-section");
const bookingVenue = document.querySelector("#booking-venue");
const bookingStartTime = document.querySelector("#booking-start-time");
const bookingZoneGrid = document.querySelector("#booking-zone-grid");
const bookingShowTitle = document.querySelector("#booking-show-title");
const bookingSelectionCount = document.querySelector("#booking-selection-count");
const bookingSelectionTotal = document.querySelector("#booking-selection-total");
const bookingSelectionExpires = document.querySelector("#booking-selection-expires");
const checkoutButton = document.querySelector("#checkout-btn");

let latestApp = null;
let latestSeats = [];

bookingZoneGrid.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-seat-id]");
  if (!button || !latestApp) return;

  const seatId = Number(button.dataset.seatId);
  const mine = latestSeats.filter((seat) => seat.isMine).map((seat) => seat.id);
  const next = mine.includes(seatId) ? mine.filter((id) => id !== seatId) : [...mine, seatId];

  await latestApp.api(`/api/shows/${latestApp.state.activeShowId}/hold`, {
    method: "POST",
    body: JSON.stringify({ seatIds: next }),
  });
  await latestApp.refresh();
});

checkoutButton.addEventListener("click", () => {
  if (!latestApp) return;
  if (latestApp.state.session.authenticated) {
    latestApp.el.checkoutForm.email.value = latestApp.state.session.user.email;
    latestApp.el.checkoutForm.name.value = latestApp.state.session.user.name;
  }
  latestApp.el.checkoutDialog.showModal();
});

startSite({
  enablePolling: true,
  getBanner({ currentShow, fmtTime }) {
    const show = currentShow();
    return {
      title: "Seat Selection & Checkout",
      summary: "Live seat availability, lock timers, and checkout are all tied to the active session shown here.",
      stats: [show?.title, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/pricing.html", label: "View pricing", kind: "ghost-button" }],
      slides: [
        { kicker: "Seat map", title: "Live availability", body: "Seats change state in real time while locks and releases update around the current session.", tone: "#22c55e" },
        { kicker: "Order timer", title: "Short lock window", body: "The booking flow is designed around temporary holds, queue admission, and decisive checkout.", tone: "#f59e0b" },
        { kicker: "Current event", title: show?.title || "Selected show", body: show?.venue || "Venue information", tone: "#ef4444" },
      ],
    };
  },
  render(app) {
    latestApp = app;
    const { fmtMoney, fmtTime, state } = app;
    const { queue, zones = [], seats = [], selection = {}, show } = state.detail;
    latestSeats = seats;

    if (queue.state === "waiting") {
      queuePosition.textContent = String(queue.position);
      queueSection.classList.remove("hidden");
      mainSection.classList.add("hidden");
      return;
    }

    queueSection.classList.add("hidden");
    mainSection.classList.remove("hidden");
    bookingVenue.textContent = show.venue;
    bookingStartTime.textContent = fmtTime(show.startTime);
    bookingShowTitle.textContent = show.title;
    bookingSelectionCount.textContent = `${selection.count || 0} seats currently locked for you.`;
    bookingSelectionTotal.textContent = fmtMoney(selection.totalCents || 0);
    bookingSelectionExpires.textContent = `Expires at: ${selection.expiresAt ? fmtTime(selection.expiresAt) : "not started"}`;
    checkoutButton.disabled = !selection.count;
    bookingZoneGrid.innerHTML = zones
      .map((zone) => {
        const zoneSeats = seats.filter((seat) => seat.zoneCode === zone.code);
        const rows = [...new Set(zoneSeats.map((seat) => seat.rowLabel))];
        return `
          <article class="theater-card">
            <div class="section-heading">
              <div>
                <p class="eyebrow">${zone.code}</p>
                <h3>${zone.label}</h3>
              </div>
              <span class="pill">${zone.priceLabel}</span>
            </div>
            <div class="seat-grid">
              ${rows
                .map((row) => {
                  const rowSeats = zoneSeats.filter((seat) => seat.rowLabel === row);
                  return `
                    <div class="seat-row">
                      <span class="row-label">${row}</span>
                      ${rowSeats
                        .map((seat) => {
                          const cls = seat.status === "sold" ? "sold" : seat.isMine ? "mine" : seat.status === "locked" ? "locked" : "available";
                          const disabled = seat.status === "sold" || (seat.status === "locked" && !seat.isMine);
                          return `<button class="seat ${cls}" data-seat-id="${seat.id}" ${disabled ? "disabled" : ""} title="${seat.seatKey} - ${seat.priceLabel}"></button>`;
                        })
                        .join("")}
                    </div>
                  `;
                })
                .join("")}
            </div>
          </article>
        `;
      })
      .join("");
  },
});
