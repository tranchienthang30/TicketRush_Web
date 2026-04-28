import { startSite } from "./site-core.js";

const pricingGrid = document.querySelector("#pricing-grid");

startSite({
  getBanner({ currentShow, fmtMoney, minPriceCents }) {
    const show = currentShow();
    return {
      title: "Ticket Pricing",
      summary: "Review the pricing structure for each seat class, then move into the live seat map where the final ticket value is determined by the seat you actually lock.",
      stats: [show?.title, show?.venue, show ? `From ${fmtMoney(minPriceCents(show))}` : null],
      actions: [],
      slides: [
        { kicker: "Reference", title: "Pricing stays in sync", body: "Every tier on this page is pulled from the active show, so the booking screen uses the same source of truth.", tone: "#f59e0b" },
        { kicker: "Current entry", title: show?.title || "Current show", body: show?.venue || "Venue details", tone: show?.heroColor || "#38bdf8" },
        { kicker: "Checkout rule", title: "Final total comes from seats", body: "The amount you pay is always calculated from the exact seats you lock in the booking flow.", tone: "#ef4444" },
      ],
    };
  },
  render({ currentShow }) {
    const show = currentShow();
    const pricing = show?.pricing || [];

    pricingGrid.innerHTML = pricing
      .map(
        (price, index) => `
          <article class="pricing-card ${index === 0 ? "pricing-card-featured" : ""}">
            <p class="eyebrow">Seat Class</p>
            <h3>${price.label}</h3>
            <div class="price-stack">
              <div class="price-row">
                <span>Current show price</span>
                <strong>${price.priceLabel}</strong>
              </div>
            </div>
            <p class="muted">${index === 0 ? "Best for premium viewing, couples, and marquee openings." : index === 1 ? "Balanced comfort for regular movie nights." : "A practical pick for casual sessions and groups."}</p>
            <a class="primary-button pricing-select" href="/booking.html">Open seat map</a>
          </article>
        `
      )
      .join("");
  },
});
