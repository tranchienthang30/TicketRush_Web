const state = {
  clientId: localStorage.getItem("ticketrush-client") || crypto.randomUUID(),
  token: localStorage.getItem("ticketrush-token") || "",
  catalog: null,
  session: { authenticated: false },
  dashboard: { events: [], audience: [] },
  orders: [],
  activeShowId: Number(localStorage.getItem("ticketrush-show")) || 0,
  detail: null,
  pollId: null,
};

localStorage.setItem("ticketrush-client", state.clientId);

const page = document.body.dataset.page;

const el = {
  clientChip: document.querySelector("#client-chip"),
  authActions: document.querySelector("#auth-actions"),
  memberSummary: document.querySelector("#member-summary"),
  eventsPanel: document.querySelector("#events-panel"),
  ordersPanel: document.querySelector("#orders-panel"),
  mainContent: document.querySelector("#main-content"),
  pageBannerTitle: document.querySelector("#page-banner-title"),
  pageBannerSummary: document.querySelector("#page-banner-summary"),
  pageBannerStats: document.querySelector("#page-banner-stats"),
  pageBannerActions: document.querySelector("#page-banner-actions"),
  loginDialog: document.querySelector("#login-dialog"),
  registerDialog: document.querySelector("#register-dialog"),
  checkoutDialog: document.querySelector("#checkout-dialog"),
  loginForm: document.querySelector("#login-form"),
  registerForm: document.querySelector("#register-form"),
  checkoutForm: document.querySelector("#checkout-form"),
};

const fmtMoney = (cents) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(cents);

const fmtTime = (value) =>
  new Date(value).toLocaleString("en-GB", { hour12: false, dateStyle: "medium", timeStyle: "short" });

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    "X-Client-Id": state.clientId,
    ...(options.headers || {}),
  };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;

  const res = await fetch(path, { ...options, headers });
  const data = await res.json();
  if (!res.ok || data.error) throw new Error(data.error || "Request failed");
  return data;
}

async function bootstrap() {
  state.catalog = await api("/api/catalog");
  state.session = await api("/api/auth/session", { method: "POST", body: JSON.stringify({}) });
  if (!state.activeShowId && state.catalog.shows.length) state.activeShowId = state.catalog.shows[0].id;
  state.dashboard = await api("/api/admin/dashboard");
  await loadCurrentShow();
  renderShell();
  renderPage();
}

async function loadCurrentShow() {
  if (!state.activeShowId) return;
  localStorage.setItem("ticketrush-show", String(state.activeShowId));
  state.detail = await api(`/api/shows/${state.activeShowId}/detail?client_id=${state.clientId}`);
  state.orders = await api(`/api/shows/${state.activeShowId}/orders`, { method: "POST", body: JSON.stringify({}) });
}

function renderShell() {
  el.clientChip.textContent = `Client ${state.clientId.slice(0, 8)}`;
  renderAuthButtons();
  renderMemberSummary();
  renderShowSidebar();
  renderOrders();
  renderPageBanner();
}

function renderAuthButtons() {
  if (state.session.authenticated) {
    el.authActions.innerHTML = `
      <span class="pill">${state.session.user.name}</span>
      <button class="ghost-button" id="logout-btn">Sign Out</button>
    `;
    document.querySelector("#logout-btn").addEventListener("click", async () => {
      state.token = "";
      localStorage.removeItem("ticketrush-token");
      await bootstrap();
    });
    return;
  }

  el.authActions.innerHTML = `
    <button class="text-button" id="open-login">Sign In</button>
    <button class="primary-button" id="open-register">Create Account</button>
  `;
  document.querySelector("#open-login").addEventListener("click", () => el.loginDialog.showModal());
  document.querySelector("#open-register").addEventListener("click", () => el.registerDialog.showModal());
}

function renderMemberSummary() {
  if (!state.session.authenticated) {
    el.memberSummary.innerHTML = `
      <article class="admin-card">
        <p class="eyebrow">Guest</p>
        <h3>Not signed in yet</h3>
        <p class="muted">Sign in to store tickets, track points, and unlock member perks.</p>
      </article>
    `;
    return;
  }

  const user = state.session.user;
  el.memberSummary.innerHTML = `
    <article class="admin-card">
      <p class="eyebrow">Member Profile</p>
      <h3>${user.name}</h3>
      <p class="muted">${user.email}</p>
      <div class="quick-stats">
        <span class="mini-chip ${tierClass(user.tier)}">${user.tier}</span>
        <span class="mini-chip">${user.points} pts</span>
      </div>
    </article>
  `;
}

function renderShowSidebar() {
  el.eventsPanel.innerHTML = state.catalog.shows
    .map(
      (show) => `
        <article class="theater-card ${show.id === state.activeShowId ? "active-card" : ""}" data-show-id="${show.id}">
          <p class="eyebrow">${show.category}</p>
          <h3>${show.venue}</h3>
          <p class="muted">${show.title}</p>
          <div class="quick-stats">
            <span class="mini-chip">${fmtTime(show.startTime)}</span>
            <span class="mini-chip">Sold ${show.stats.soldSeats}/${show.stats.totalSeats}</span>
          </div>
        </article>
      `
    )
    .join("");

  [...el.eventsPanel.querySelectorAll("[data-show-id]")].forEach((card) =>
    card.addEventListener("click", async () => {
      state.activeShowId = Number(card.dataset.showId);
      await loadCurrentShow();
      renderShell();
      renderPage();
    })
  );
}

function renderOrders() {
  if (!state.orders.length) {
    el.ordersPanel.innerHTML = `
      <article class="ticket-card">
        <p class="muted">No orders yet for the currently selected show.</p>
      </article>
    `;
    return;
  }

  el.ordersPanel.innerHTML = state.orders
    .map(
      (order) => `
        <article class="ticket-card">
          <h4>${order.eventTitle}</h4>
          <p class="muted">${order.venue}</p>
          <p class="muted">${fmtTime(order.startTime)}</p>
          <p><strong>${order.totalLabel}</strong></p>
          ${order.items
            .map(
              (item) => `
                <div class="ticket-item">
                  <div class="ticket-qr">${item.qrSvg}</div>
                  <div>
                    <h4>${item.seatKey}</h4>
                    <p class="muted">${item.zoneLabel} / ${item.priceLabel}</p>
                    <small>${item.qrPayload}</small>
                  </div>
                </div>
              `
            )
            .join("")}
        </article>
      `
    )
    .join("");
}

function renderPage() {
  const renderers = {
    home: renderHomePage,
    movies: renderMoviesPage,
    theaters: renderTheatersPage,
    pricing: renderPricingPage,
    news: renderNewsPage,
    member: renderMemberPage,
    booking: renderBookingPage,
    admin: renderAdminPage,
  };
  (renderers[page] || renderHomePage)();
}

function renderPageBanner() {
  const show = currentShow();
  const configs = {
    home: {
      title: "Platform Overview",
      summary: "Use the homepage as your control surface for discovery, live sessions, and fast movement into each booking workflow.",
      stats: [show?.venue, show ? fmtTime(show.startTime) : null, "Queue Enabled"],
      actions: [
        { href: "/booking.html", label: "Start booking", kind: "primary-button" },
        { href: "/movies.html", label: "Browse movies", kind: "ghost-button" },
      ],
    },
    pricing: {
      title: "Ticket Pricing",
      summary: "Review the pricing structure for each seat class, then move into the live seat map where the final ticket value is determined by the seat you actually lock.",
      stats: [show?.title, show?.venue, show ? `From ${fmtMoney(minPriceCents(show))}` : null],
      actions: [
        { href: "/booking.html", label: "Open seat map", kind: "primary-button" },
        { href: "/theaters.html", label: "Change cinema", kind: "ghost-button" },
      ],
    },
    news: {
      title: "News & Promotions",
      summary: "Track the latest launches, member campaigns, and branch updates tied to your current cinema session.",
      stats: [show?.venue, "Editorial feed", `${(state.catalog?.news || []).length} stories`],
      actions: [
        { href: "/booking.html", label: "Book now", kind: "primary-button" },
      ],
    },
    member: {
      title: "Membership",
      summary: "See your tier, review ticket history, and move into checkout with a better reward profile already attached.",
      stats: [
        state.session.authenticated ? state.session.user?.tier : "Guest Mode",
        state.session.authenticated ? `${state.session.user?.points || 0} pts` : "Sign in for rewards",
        show?.venue,
      ],
      actions: state.session.authenticated
        ? [{ href: "/booking.html", label: "Use member benefits", kind: "primary-button" }]
        : [{ action: "login", label: "Sign in", kind: "primary-button" }],
    },
    movies: {
      title: "Now Showing & Coming Soon",
      summary: "Review the active line-up, compare release timing, and move into booking from the title that fits your night.",
      stats: [`${state.catalog?.shows?.length || 0} titles`, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/booking.html", label: "Go to booking", kind: "primary-button" }],
    },
    theaters: {
      title: "Theater Directory",
      summary: "Switch between active branches, compare schedules, and keep the current session synced across the rest of the site.",
      stats: [`${state.catalog?.theaters?.length || 0} branches`, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/booking.html", label: "Pick seats", kind: "primary-button" }],
    },
    booking: {
      title: "Seat Selection & Checkout",
      summary: "Live seat availability, lock timers, and checkout are all tied to the active session shown here.",
      stats: [show?.title, show?.venue, show ? fmtTime(show.startTime) : null],
      actions: [{ href: "/pricing.html", label: "View pricing", kind: "ghost-button" }],
    },
    admin: {
      title: "Admin Dashboard",
      summary: "Monitor sales, occupancy, and audience mix while controlling new launches from the same workspace.",
      stats: [`${state.dashboard?.events?.length || 0} sessions`, "Live metrics", show?.venue],
      actions: [{ href: "/booking.html", label: "Open customer flow", kind: "ghost-button" }],
    },
  };

  const config = configs[page] || configs.home;
  el.pageBannerTitle.textContent = config.title;
  el.pageBannerSummary.textContent = config.summary;
  el.pageBannerStats.innerHTML = config.stats.filter(Boolean).map((item) => `<span class="mini-chip">${item}</span>`).join("");
  el.pageBannerActions.innerHTML = config.actions
    .map((action, index) => {
      if (action.action === "login") {
        return `<button class="${action.kind}" data-banner-action="login">${action.label}</button>`;
      }
      return `<a class="${action.kind}" href="${action.href}" ${index > 0 ? "" : ""}>${action.label}</a>`;
    })
    .join("");

  el.pageBannerActions.querySelector('[data-banner-action="login"]')?.addEventListener("click", () => el.loginDialog.showModal());
}

function minPriceCents(show = currentShow()) {
  const prices = (show?.pricing || []).map((item) => Number(item.priceCents)).filter(Boolean);
  return prices.length ? Math.min(...prices) : 0;
}

function renderHomePage() {
  const show = currentShow();
  el.mainContent.innerHTML = `
    <section class="hero-card" style="background: linear-gradient(135deg, ${show.heroColor}, rgba(15, 23, 48, 0.98)), linear-gradient(180deg, rgba(18, 26, 49, 0.96), rgba(12, 18, 35, 0.96));">
      <div class="hero-copy">
        <div>
          <p class="eyebrow">Featured Show</p>
          <div class="hero-title">${show.title}</div>
        </div>
        <p class="hero-sub">${show.description}</p>
        <div class="meta-row">
          <span class="meta-pill">${show.venue}</span>
          <span class="meta-pill">${fmtTime(show.startTime)}</span>
          <span class="meta-pill">Queue + Seat Locking</span>
        </div>
      </div>
      <div class="hero-poster">
        <p class="poster-kicker">${show.category}</p>
        <h3>Move from the homepage into each dedicated cinema workflow with a single click.</h3>
      </div>
    </section>
    <section class="section-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Quick Access</p>
          <h2>Jump straight into the right experience</h2>
        </div>
      </div>
      <div class="movie-grid">
        ${quickCard("Movies", "/movies.html", "Browse now showing and upcoming titles")}
        ${quickCard("Theaters", "/theaters.html", "Explore branches and live showtimes")}
        ${quickCard("Pricing", "/pricing.html", "Clear ticket tiers and seat class pricing")}
        ${quickCard("News", "/news.html", "Campaigns, launches, and weekly offers")}
        ${quickCard("Member", "/member.html", "Accounts, rewards, and loyalty benefits")}
        ${quickCard("Booking", "/booking.html", "Queue entry, seat picking, and checkout")}
      </div>
    </section>
  `;
}

function renderMoviesPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Movies</p>
          <h2>Now showing and coming soon</h2>
        </div>
      </div>
      <div class="movie-grid">
        ${state.catalog.shows
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
          .join("")}
      </div>
    </section>
  `;
}

function renderTheatersPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Theaters</p>
          <h2>Branches and active sessions</h2>
        </div>
      </div>
      <div class="movie-grid">
        ${state.catalog.theaters
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
          .join("")}
      </div>
    </section>
  `;
}

function renderPricingPage() {
  const show = currentShow();
  const pricing = show?.pricing || [];

  el.mainContent.innerHTML = `
    <section class="section-card pricing-hero">
      <p class="lead-copy">Use this page as a clear pricing reference before entering the live seat map. Final checkout value is always calculated from the actual seats you choose in Booking.</p>
      <div class="pricing-grid">
        ${pricing
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
          .join("")}
      </div>
    </section>
  `;
}

function renderNewsPage() {
  const newsItems = (state.catalog.news || []).map(normalizeNewsItem);
  const featured = newsItems[0];
  const secondary = newsItems.slice(1);

  el.mainContent.innerHTML = `
    <section class="section-card news-hero">
      <div class="news-feature">
        <div>
          <p class="eyebrow">Featured Story</p>
          <h3>${featured?.title || "Latest campaign"}</h3>
          <p class="lead-copy">${featured?.summary || "Return here for weekly launches, curated offers, and fresh editorial highlights from across the circuit."}</p>
        </div>
      </div>
      <div class="news-grid">
        ${(secondary.length ? secondary : newsItems.slice(0, 2))
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
          .join("")}
      </div>
    </section>
  `;
}

function normalizeNewsItem(item = {}) {
  const titleMap = {
    "Mo them cum rap Riverside": "Riverside cinema expansion is now open",
    "Tuan le member day": "Member Week is live",
    "Gio vang bap nuoc": "Late-show snack hour",
  };
  const summaryMap = {
    "Cum rap moi voi phong chieu 4K va ghe doi.": "A new branch with 4K projection, couple seats, and a faster online booking lane.",
    "Nhan gap doi diem cho don dat ve qua web.": "Earn double reward points on confirmed web bookings throughout the campaign.",
    "Giam gia combo cho suat chieu sau 20:30.": "Enjoy discounted combo bundles for screenings starting after 20:30.",
  };
  const tagMap = {
    "Thong bao": "Announcement",
    "Khuyen mai": "Promotion",
    "Tin moi": "Update",
  };

  return {
    ...item,
    title: titleMap[item.title] || item.title || "Latest campaign",
    summary: summaryMap[item.summary] || item.summary || "Fresh editorial updates will appear here.",
    tag: tagMap[item.tag] || item.tag || "Update",
  };
}

function renderMemberPage() {
  const signedIn = state.session.authenticated;
  const user = state.session.user || {};

  el.mainContent.innerHTML = `
    <section class="section-card member-hero">
      <div class="member-status-row">
        <span class="pill ${tierClass(signedIn ? user.tier : "Guest Mode")}">${signedIn ? user.tier : "Guest Mode"}</span>
      </div>
      <div class="member-dashboard">
        <article class="member-card-main">
          <p class="eyebrow">${signedIn ? "Account Overview" : "Welcome"}</p>
          <h3>${signedIn ? user.name : "Join the Starlight Rush club"}</h3>
          <p class="muted">${signedIn ? user.email : "Create an account to track points, retain ticket history, and unlock exclusive booking perks."}</p>
          <div class="member-stat-row">
            <div class="member-stat">
              <span>Tier</span>
              <strong>${signedIn ? user.tier : "Guest"}</strong>
            </div>
            <div class="member-stat">
              <span>Points</span>
              <strong>${signedIn ? user.points : 0}</strong>
            </div>
          </div>
        </article>
        <article class="member-card-side">
          <p class="eyebrow">Benefits</p>
          <div class="stack">
            <p class="muted">Priority booking profile autofill.</p>
            <p class="muted">Reward points after successful checkout.</p>
            <p class="muted">Ticket history pinned to your account.</p>
          </div>
        </article>
      </div>
      <div class="member-layout">
        <article class="admin-card">
          <p class="eyebrow">Order History</p>
          <div class="stack">
            ${state.orders.length ? state.orders.map((order) => `<p class="muted">${order.eventTitle} / ${order.totalLabel}</p>`).join("") : "<p class='muted'>No purchases yet.</p>"}
          </div>
        </article>
        <article class="admin-card">
          <p class="eyebrow">Demo Access</p>
          <p class="muted">Email: <strong>member@starlightrush.vn</strong></p>
          <p class="muted">Password: <strong>123456</strong></p>
          <p class="muted">Use the header buttons to sign in or create a new account in the modal flow.</p>
        </article>
      </div>
    </section>
  `;
}

function tierClass(tier) {
  const value = String(tier || "").toLowerCase();
  if (value.includes("platinum")) return "tier-platinum";
  if (value.includes("gold")) return "tier-gold";
  if (value.includes("silver")) return "tier-silver";
  if (value.includes("guest")) return "tier-guest";
  return "tier-default";
}

function renderBookingPage() {
  startPolling();
  const { queue, zones = [], seats = [], selection = {}, show } = state.detail;

  if (queue.state === "waiting") {
    el.mainContent.innerHTML = `
      <section class="section-card">
        <article class="queue-card">
          <p class="eyebrow">Virtual Queue</p>
          <h3>You are currently in the waiting room</h3>
          <p>Current position: <strong>${queue.position}</strong></p>
          <p class="muted">You will be admitted in controlled batches. Please keep this page open.</p>
        </article>
      </section>
    `;
    return;
  }

  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Booking</p>
          <h2>Queue, seat picking, and checkout</h2>
        </div>
        <div class="meta-row">
          <span class="meta-pill">${show.venue}</span>
          <span class="meta-pill">${fmtTime(show.startTime)}</span>
        </div>
      </div>
      <section class="booking-layout">
        <div class="booking-card">
          <div class="screen">Screen</div>
          <div class="legend">
            <span class="mini-chip">Green: available</span>
            <span class="mini-chip">Gold: yours</span>
            <span class="mini-chip">Gray: locked / sold</span>
          </div>
          <div class="zone-grid">
            ${zones
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
              .join("")}
          </div>
        </div>
        <div class="stack">
          <article class="booking-card">
            <p class="eyebrow">Order</p>
            <h3>${show.title}</h3>
            <p class="muted">${selection.count || 0} seats currently locked for you.</p>
            <p><strong>${fmtMoney(selection.totalCents || 0)}</strong></p>
            <p class="muted">Expires at: ${selection.expiresAt ? fmtTime(selection.expiresAt) : "not started"}</p>
            <button class="primary-button" id="checkout-btn" ${selection.count ? "" : "disabled"}>Proceed to payment</button>
          </article>
        </div>
      </section>
    </section>
  `;

  [...document.querySelectorAll("[data-seat-id]")].forEach((button) =>
    button.addEventListener("click", async () => {
      const seatId = Number(button.dataset.seatId);
      const mine = seats.filter((seat) => seat.isMine).map((seat) => seat.id);
      const next = mine.includes(seatId) ? mine.filter((id) => id !== seatId) : [...mine, seatId];
      await api(`/api/shows/${state.activeShowId}/hold`, { method: "POST", body: JSON.stringify({ seatIds: next }) });
      await refresh();
    })
  );

  document.querySelector("#checkout-btn")?.addEventListener("click", () => {
    if (state.session.authenticated) {
      el.checkoutForm.email.value = state.session.user.email;
      el.checkoutForm.name.value = state.session.user.name;
    }
    el.checkoutDialog.showModal();
  });
}

function renderAdminPage() {
  stopPolling();
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Admin</p>
          <h2>Revenue, occupancy, and launch controls</h2>
        </div>
      </div>
      <div class="stat-grid">
        ${state.dashboard.events
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
          .join("")}
      </div>
      <div class="member-layout">
        <article class="admin-card">
          <p class="eyebrow">Audience Mix</p>
          <div class="stack">
            ${state.dashboard.audience.length ? state.dashboard.audience.map((item) => `<p>${item.gender} / ${item.ageBucket}: <strong>${item.count}</strong></p>`).join("") : "<p class='muted'>No audience data yet.</p>"}
          </div>
        </article>
        <article class="admin-card">
          <p class="eyebrow">Create New Session</p>
          <form id="admin-form" class="stack">
            <input name="title" placeholder="Movie title" required />
            <input name="category" placeholder="Category" value="Now Showing" required />
            <input name="venue" placeholder="Cinema branch" required />
            <input name="startTime" type="datetime-local" required />
            <input name="heroColor" value="#0ea5e9" required />
            <textarea name="description" placeholder="Short description" required></textarea>
            <button class="primary-button" type="submit">Create session</button>
          </form>
        </article>
      </div>
    </section>
  `;

  document.querySelector("#admin-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    payload.startTime = new Date(payload.startTime).toISOString();
    payload.zones = [
      { code: "VIP", label: "VIP Hall", rowsCount: 4, seatsPerRow: 8, priceCents: 160000 },
      { code: "STD", label: "Standard Hall", rowsCount: 8, seatsPerRow: 14, priceCents: 95000 },
      { code: "BAL", label: "Balcony", rowsCount: 4, seatsPerRow: 10, priceCents: 55000 },
    ];
    await api("/api/admin/shows", { method: "POST", body: JSON.stringify(payload) });
    await bootstrap();
  });
}

function currentShow() {
  return state.catalog.shows.find((show) => show.id === state.activeShowId) || state.catalog.shows[0];
}

function quickCard(title, href, description) {
  return `
    <a class="movie-card page-link-card" href="${href}">
      <p class="eyebrow">Page</p>
      <h3>${title}</h3>
      <p class="muted">${description}</p>
    </a>
  `;
}

async function refresh() {
  state.catalog = await api("/api/catalog");
  state.dashboard = await api("/api/admin/dashboard");
  state.session = await api("/api/auth/session", { method: "POST", body: JSON.stringify({}) });
  await loadCurrentShow();
  renderShell();
  renderPage();
}

function startPolling() {
  if (page !== "booking") return;
  stopPolling();
  state.pollId = window.setInterval(refresh, 3000);
}

function stopPolling() {
  if (state.pollId) {
    clearInterval(state.pollId);
    state.pollId = null;
  }
}

document.querySelectorAll("[data-close-dialog]").forEach((button) =>
  button.addEventListener("click", () => button.closest("dialog").close())
);

el.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
  const session = await api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) });
  state.token = session.token;
  localStorage.setItem("ticketrush-token", state.token);
  el.loginDialog.close();
  await bootstrap();
});

el.registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
  const session = await api("/api/auth/register", { method: "POST", body: JSON.stringify(payload) });
  state.token = session.token;
  localStorage.setItem("ticketrush-token", state.token);
  el.registerDialog.close();
  await bootstrap();
});

el.checkoutForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
  const result = await api(`/api/shows/${state.activeShowId}/checkout`, { method: "POST", body: JSON.stringify(payload) });
  if (result.pointsEarned) alert(`Payment completed. You earned ${result.pointsEarned} points.`);
  el.checkoutDialog.close();
  event.currentTarget.reset();
  await bootstrap();
});

bootstrap().catch((error) => {
  document.body.innerHTML = `<pre>${error.message}</pre>`;
});
