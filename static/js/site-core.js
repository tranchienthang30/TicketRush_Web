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
  bannerTimer: null,
  bannerIndex: 0,
};

localStorage.setItem("ticketrush-client", state.clientId);

const el = {
  clientChip: document.querySelector("#client-chip"),
  authActions: document.querySelector("#auth-actions"),
  memberSummary: document.querySelector("#member-summary"),
  eventsPanel: document.querySelector("#events-panel"),
  ordersPanel: document.querySelector("#orders-panel"),
  pageBannerTitle: document.querySelector("#page-banner-title"),
  pageBannerSummary: document.querySelector("#page-banner-summary"),
  pageBannerStats: document.querySelector("#page-banner-stats"),
  pageBannerActions: document.querySelector("#page-banner-actions"),
  pageBannerStage: document.querySelector("#page-banner-stage"),
  pageBannerDots: document.querySelector("#page-banner-dots"),
  loginDialog: document.querySelector("#login-dialog"),
  registerDialog: document.querySelector("#register-dialog"),
  checkoutDialog: document.querySelector("#checkout-dialog"),
  loginForm: document.querySelector("#login-form"),
  registerForm: document.querySelector("#register-form"),
  checkoutForm: document.querySelector("#checkout-form"),
};

let activePage = null;
let handlersBound = false;

export function startSite(pageModule) {
  activePage = pageModule;
  bindGlobalHandlers();
  bootstrap().catch((error) => {
    document.body.innerHTML = `<pre>${error.message}</pre>`;
  });
}

export function fmtMoney(cents) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(cents);
}

export function fmtTime(value) {
  return new Date(value).toLocaleString("en-GB", {
    hour12: false,
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export function normalizeNewsItem(item = {}) {
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

export function tierClass(tier) {
  const value = String(tier || "").toLowerCase();
  if (value.includes("platinum")) return "tier-platinum";
  if (value.includes("gold")) return "tier-gold";
  if (value.includes("silver")) return "tier-silver";
  if (value.includes("guest")) return "tier-guest";
  return "tier-default";
}

export function minPriceCents(show = currentShow()) {
  const prices = (show?.pricing || []).map((item) => Number(item.priceCents)).filter(Boolean);
  return prices.length ? Math.min(...prices) : 0;
}

export function currentShow() {
  const shows = state.catalog?.shows || [];
  return shows.find((show) => show.id === state.activeShowId) || shows[0];
}

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
  state.orders = await api(`/api/shows/${state.activeShowId}/orders`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

function renderShell() {
  el.clientChip.textContent = `Client ${state.clientId.slice(0, 8)}`;
  renderAuthButtons();
  renderMemberSummary();
  renderShowSidebar();
  renderOrders();
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
  if (!activePage) return;
  if (activePage.enablePolling) {
    startPolling();
  } else {
    stopPolling();
  }

  const app = getAppContext();
  renderPageBanner(activePage.getBanner?.(app) || {});
  activePage.render?.(app);
}

function renderPageBanner(config) {
  el.pageBannerTitle.textContent = config.title || "";
  el.pageBannerSummary.textContent = config.summary || "";
  el.pageBannerStats.innerHTML = (config.stats || [])
    .filter(Boolean)
    .map((item) => `<span class="mini-chip">${item}</span>`)
    .join("");
  el.pageBannerActions.innerHTML = (config.actions || [])
    .map((action) => {
      if (action.action === "login") {
        return `<button class="${action.kind}" data-banner-action="login">${action.label}</button>`;
      }
      return `<a class="${action.kind}" href="${action.href}">${action.label}</a>`;
    })
    .join("");

  el.pageBannerActions
    .querySelector('[data-banner-action="login"]')
    ?.addEventListener("click", () => el.loginDialog.showModal());
  renderBannerSlides(config.slides || []);
}

function renderBannerSlides(slides) {
  clearInterval(state.bannerTimer);
  state.bannerTimer = null;
  state.bannerIndex = 0;

  if (!slides.length) {
    el.pageBannerStage.innerHTML = "";
    el.pageBannerDots.innerHTML = "";
    return;
  }

  const draw = () => {
    const slide = slides[state.bannerIndex] || slides[0];
    el.pageBannerStage.innerHTML = `
      <article class="banner-slide" style="--banner-tone: ${slide.tone || "#38bdf8"}">
        <div class="banner-slide-copy">
          <p class="eyebrow">${slide.kicker || "Spotlight"}</p>
          <h3>${slide.title || ""}</h3>
          <p class="muted">${slide.body || ""}</p>
        </div>
        <div class="banner-orb banner-orb-a"></div>
        <div class="banner-orb banner-orb-b"></div>
      </article>
    `;
    el.pageBannerDots.innerHTML = slides
      .map(
        (_, index) =>
          `<button class="banner-dot ${index === state.bannerIndex ? "active" : ""}" type="button" data-banner-dot="${index}" aria-label="Show slide ${index + 1}"></button>`
      )
      .join("");

    [...el.pageBannerDots.querySelectorAll("[data-banner-dot]")].forEach((button) =>
      button.addEventListener("click", () => {
        state.bannerIndex = Number(button.dataset.bannerDot);
        draw();
      })
    );
  };

  draw();
  if (slides.length > 1) {
    state.bannerTimer = window.setInterval(() => {
      state.bannerIndex = (state.bannerIndex + 1) % slides.length;
      draw();
    }, 4200);
  }
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
  stopPolling();
  state.pollId = window.setInterval(refresh, 3000);
}

function stopPolling() {
  if (state.pollId) {
    clearInterval(state.pollId);
    state.pollId = null;
  }
}

function bindGlobalHandlers() {
  if (handlersBound) return;
  handlersBound = true;

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
    const result = await api(`/api/shows/${state.activeShowId}/checkout`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    if (result.pointsEarned) alert(`Payment completed. You earned ${result.pointsEarned} points.`);
    el.checkoutDialog.close();
    event.currentTarget.reset();
    await bootstrap();
  });
}

function getAppContext() {
  return {
    api,
    bootstrap,
    currentShow,
    el,
    fmtMoney,
    fmtTime,
    minPriceCents,
    normalizeNewsItem,
    refresh,
    state,
    tierClass,
  };
}
