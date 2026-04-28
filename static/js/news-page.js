import { startSite } from "./site-core.js";

const featureTitle = document.querySelector("#news-feature-title");
const featureSummary = document.querySelector("#news-feature-summary");
const newsGrid = document.querySelector("#news-grid");

startSite({
  getBanner({ currentShow, state }) {
    const show = currentShow();
    const firstNews = state.catalog?.news?.[0];
    return {
      title: "News & Promotions",
      summary: "Track the latest launches, member campaigns, and branch updates tied to your current cinema session.",
      stats: [show?.venue, "Editorial feed", `${(state.catalog?.news || []).length} stories`],
      actions: [{ href: "/booking.html", label: "Book now", kind: "primary-button" }],
      slides: [
        { kicker: "Featured story", title: firstNews?.title || "Cinema updates", body: firstNews?.summary || "Latest editorial updates appear here.", tone: "#38bdf8" },
        { kicker: "Campaigns", title: "Promotions update in real time", body: "Use the news section to surface offers, launches, and booking pushes tied to active sessions.", tone: "#f59e0b" },
        { kicker: "Live push", title: show?.venue || "Current branch", body: "Editorial content can be paired with the branch or title the user is currently viewing.", tone: "#ef4444" },
      ],
    };
  },
  render({ normalizeNewsItem, state }) {
    const newsItems = (state.catalog.news || []).map(normalizeNewsItem);
    const featured = newsItems[0];
    const secondary = newsItems.slice(1);

    featureTitle.textContent = featured?.title || "Latest campaign";
    featureSummary.textContent =
      featured?.summary ||
      "Return here for weekly launches, curated offers, and fresh editorial highlights from across the circuit.";
    newsGrid.innerHTML = (secondary.length ? secondary : newsItems.slice(0, 2))
      .map(
        (item) => `
          <article class="news-card">
            <span class="tag-pill">${item.tag}</span>
            <h3>${item.title}</h3>
            <p class="muted">${item.summary}</p>
            <a href="/booking.html" class="inline-link">Open booking</a>
          </article>
        `
      )
      .join("");
  },
});
