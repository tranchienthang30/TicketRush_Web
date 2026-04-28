import { startSite } from "./site-core.js";

const heroCard = document.querySelector("#home-hero-card");
const heroTitle = document.querySelector("#home-hero-title");
const heroDescription = document.querySelector("#home-hero-description");
const heroVenue = document.querySelector("#home-hero-venue");
const heroTime = document.querySelector("#home-hero-time");
const heroCategory = document.querySelector("#home-hero-category");

startSite({
  getBanner({ currentShow, fmtTime }) {
    const show = currentShow();
    return {
      title: "Platform Overview",
      summary: "Use the homepage as your control surface for discovery, live sessions, and fast movement into each booking workflow.",
      stats: [show?.venue, show ? fmtTime(show.startTime) : null, "Queue Enabled"],
      actions: [
        { href: "/booking.html", label: "Start booking", kind: "primary-button" },
        { href: "/movies.html", label: "Browse movies", kind: "ghost-button" },
      ],
      slides: [
        { kicker: "Featured show", title: show?.title || "Live release", body: show?.description || "Active release details appear here.", tone: show?.heroColor || "#f97316" },
        { kicker: "Live venue", title: show?.venue || "Current cinema", body: show ? `Session starts ${fmtTime(show.startTime)}.` : "Schedule information updates here.", tone: "#38bdf8" },
        { kicker: "Fast path", title: "Jump into booking", body: "Move from discovery to seat locking with a single action from the current page hero.", tone: "#ef4444" },
      ],
    };
  },
  render({ currentShow, fmtTime }) {
    const show = currentShow();
    heroCard.style.background = `linear-gradient(135deg, ${show.heroColor}, rgba(15, 23, 48, 0.98)), linear-gradient(180deg, rgba(18, 26, 49, 0.96), rgba(12, 18, 35, 0.96))`;
    heroTitle.textContent = show.title;
    heroDescription.textContent = show.description;
    heroVenue.textContent = show.venue;
    heroTime.textContent = fmtTime(show.startTime);
    heroCategory.textContent = show.category;
  },
});
