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
  loginDialog: document.querySelector("#login-dialog"),
  registerDialog: document.querySelector("#register-dialog"),
  checkoutDialog: document.querySelector("#checkout-dialog"),
  loginForm: document.querySelector("#login-form"),
  registerForm: document.querySelector("#register-form"),
  checkoutForm: document.querySelector("#checkout-form"),
};

const fmtMoney = (cents) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(cents);
const fmtTime = (value) =>
  new Date(value).toLocaleString("vi-VN", { hour12: false, dateStyle: "medium", timeStyle: "short" });

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", "X-Client-Id": state.clientId, ...(options.headers || {}) };
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
}

function renderAuthButtons() {
  if (state.session.authenticated) {
    el.authActions.innerHTML = `<span class="pill">${state.session.user.name}</span><button class="ghost-button" id="logout-btn">Đăng xuất</button>`;
    document.querySelector("#logout-btn").addEventListener("click", async () => {
      state.token = "";
      localStorage.removeItem("ticketrush-token");
      await bootstrap();
    });
  } else {
    el.authActions.innerHTML = `<button class="text-button" id="open-login">Đăng nhập</button><button class="primary-button" id="open-register">Đăng ký</button>`;
    document.querySelector("#open-login").addEventListener("click", () => el.loginDialog.showModal());
    document.querySelector("#open-register").addEventListener("click", () => el.registerDialog.showModal());
  }
}

function renderMemberSummary() {
  if (!state.session.authenticated) {
    el.memberSummary.innerHTML = `<article class="admin-card"><p class="eyebrow">Member</p><h3>Khách vãng lai</h3><p class="muted">Đăng nhập để lưu vé, tích điểm và xem lịch sử đơn hàng.</p></article>`;
    return;
  }
  const user = state.session.user;
  el.memberSummary.innerHTML = `<article class="admin-card"><p class="eyebrow">Member</p><h3>${user.name}</h3><p class="muted">${user.email}</p><div class="quick-stats"><span class="mini-chip">${user.tier}</span><span class="mini-chip">${user.points} điểm</span></div></article>`;
}

function renderShowSidebar() {
  el.eventsPanel.innerHTML = state.catalog.shows
    .map(
      (show) => `<article class="theater-card ${show.id === state.activeShowId ? "active-card" : ""}" data-show-id="${show.id}"><p class="eyebrow">${show.category}</p><h3>${show.venue}</h3><p class="muted">${show.title}</p><div class="quick-stats"><span class="mini-chip">${fmtTime(show.startTime)}</span><span class="mini-chip">Đã bán ${show.stats.soldSeats}/${show.stats.totalSeats}</span></div></article>`
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
    el.ordersPanel.innerHTML = `<article class="ticket-card"><p class="muted">Chưa có đơn hàng nào cho suất chiếu đang chọn.</p></article>`;
    return;
  }
  el.ordersPanel.innerHTML = state.orders
    .map(
      (order) => `<article class="ticket-card"><h4>${order.eventTitle}</h4><p class="muted">${order.venue}</p><p class="muted">${fmtTime(order.startTime)}</p><p><strong>${order.totalLabel}</strong></p>${order.items
        .map(
          (item) => `<div class="ticket-item"><div class="ticket-qr">${item.qrSvg}</div><div><h4>${item.seatKey}</h4><p class="muted">${item.zoneLabel} · ${item.priceLabel}</p><small>${item.qrPayload}</small></div></div>`
        )
        .join("")}</article>`
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

function renderHomePage() {
  const show = currentShow();
  el.mainContent.innerHTML = `
    <section class="hero-card" style="background: linear-gradient(135deg, ${show.heroColor}, rgba(15, 23, 48, 0.98)), linear-gradient(180deg, rgba(18, 26, 49, 0.96), rgba(12, 18, 35, 0.96));">
      <div class="hero-copy">
        <div>
          <p class="eyebrow">Trang chủ</p>
          <div class="hero-title">${show.title}</div>
        </div>
        <p class="hero-sub">${show.description}</p>
        <div class="meta-row">
          <span class="meta-pill">${show.venue}</span>
          <span class="meta-pill">${fmtTime(show.startTime)}</span>
          <span class="meta-pill">Member, queue, khóa ghế</span>
        </div>
      </div>
      <div class="hero-poster">
        <p class="poster-kicker">${show.category}</p>
        <h3>Từ trang chủ bạn có thể đi qua từng chức năng riêng biệt.</h3>
      </div>
    </section>
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Điều hướng nhanh</p><h2>Chuyển qua từng trang chức năng</h2></div></div>
      <div class="movie-grid">
        ${quickCard("Phim", "/movies.html", "Danh sách phim đang chiếu và sắp chiếu")}
        ${quickCard("Rạp", "/theaters.html", "Lịch chiếu theo cụm rạp")}
        ${quickCard("Giá vé", "/pricing.html", "Bảng giá theo khu ghế")}
        ${quickCard("Tin mới", "/news.html", "Thông báo và khuyến mãi")}
        ${quickCard("Thành viên", "/member.html", "Đăng nhập, đăng ký, điểm thưởng")}
        ${quickCard("Đặt vé", "/booking.html", "Queue, chọn ghế và checkout")}
      </div>
    </section>`;
}

function renderMoviesPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Phim</p><h2>Dang chieu va sap chieu</h2></div></div>
      <div class="movie-grid">
        ${state.catalog.shows
          .map(
            (show) => `<article class="movie-card"><div class="movie-cover" style="background: linear-gradient(180deg, rgba(255,255,255,0.05), rgba(0,0,0,0.3)), linear-gradient(135deg, ${show.heroColor}, #0f172a);"></div><p class="eyebrow">${show.category}</p><h3>${show.title}</h3><p class="muted">${show.description}</p><div class="quick-stats"><span class="mini-chip">${show.venue}</span><span class="mini-chip">${fmtTime(show.startTime)}</span></div></article>`
          )
          .join("")}
      </div>
    </section>`;
}

function renderTheatersPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Rap</p><h2>Lich chieu theo cum rap</h2></div></div>
      <div class="movie-grid">
        ${state.catalog.theaters
          .map(
            (theater) => `<article class="movie-card"><p class="eyebrow">Cum rap</p><h3>${theater.name}</h3><div class="stack">${theater.movies.map((movie) => `<p class="muted">${movie.title} · ${fmtTime(movie.startTime)}</p>`).join("")}</div></article>`
          )
          .join("")}
      </div>
    </section>`;
}

function renderPricingPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Gia ve</p><h2>Bang gia theo khu ghế</h2></div></div>
      <div class="movie-grid">
        ${state.catalog.pricing
          .map((price) => `<article class="movie-card"><p class="eyebrow">Bang gia</p><h3>${price.label}</h3><p class="muted">Ngay thuong: ${price.weekday} VND</p><p class="muted">Cuoi tuan: ${price.weekend} VND</p></article>`)
          .join("")}
      </div>
    </section>`;
}

function renderNewsPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Tin moi</p><h2>Khuyen mai va thong bao</h2></div></div>
      <div class="movie-grid">
        ${state.catalog.news
          .map((item) => `<article class="movie-card"><p class="eyebrow">${item.tag}</p><h3>${item.title}</h3><p class="muted">${item.summary}</p></article>`)
          .join("")}
      </div>
    </section>`;
}

function renderMemberPage() {
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Thanh vien</p><h2>Dang nhap, dang ky, diem thuong</h2></div></div>
      <div class="member-layout">
        <article class="admin-card">
          ${
            state.session.authenticated
              ? `<p class="eyebrow">Thong tin</p><h3>${state.session.user.name}</h3><p class="muted">${state.session.user.email}</p><div class="quick-stats"><span class="mini-chip">${state.session.user.tier}</span><span class="mini-chip">${state.session.user.points} diem</span></div><p class="muted">Moi checkout thanh cong se cong them diem vao tai khoan.</p>`
              : `<p class="eyebrow">Khach</p><h3>Chua dang nhap</h3><p class="muted">Mo popup dang nhap / dang ky tu thanh dieu huong tren cung de su dung member points.</p><div class="quick-stats"><span class="mini-chip">Demo account</span><span class="mini-chip">member@starlightrush.vn / 123456</span></div>`
          }
        </article>
        <article class="admin-card">
          <p class="eyebrow">Lich su don hang</p>
          <div class="stack">
            ${state.orders.length ? state.orders.map((order) => `<p class="muted">${order.eventTitle} · ${order.totalLabel}</p>`).join("") : "<p class='muted'>Chua co don hang nao.</p>"}
          </div>
        </article>
      </div>
    </section>`;
}

function renderBookingPage() {
  startPolling();
  const { queue, zones = [], seats = [], selection = {}, show } = state.detail;
  if (queue.state === "waiting") {
    el.mainContent.innerHTML = `<section class="section-card"><article class="queue-card"><p class="eyebrow">Virtual Queue</p><h3>Ban dang o phong cho</h3><p>Vi tri hien tai: <strong>${queue.position}</strong></p><p class="muted">He thong se admit theo batch. Khong can tai lai trang.</p></article></section>`;
    return;
  }
  el.mainContent.innerHTML = `
    <section class="section-card">
      <div class="section-heading"><div><p class="eyebrow">Dat ve</p><h2>Queue, chon ghe, checkout</h2></div><div class="meta-row"><span class="meta-pill">${show.venue}</span><span class="meta-pill">${fmtTime(show.startTime)}</span></div></div>
      <section class="booking-layout">
        <div class="booking-card">
          <div class="screen">Screen</div>
          <div class="legend"><span class="mini-chip">Xanh: available</span><span class="mini-chip">Vang: cua ban</span><span class="mini-chip">Xam: locked / sold</span></div>
          <div class="zone-grid">
            ${zones
              .map((zone) => {
                const zoneSeats = seats.filter((seat) => seat.zoneCode === zone.code);
                const rows = [...new Set(zoneSeats.map((seat) => seat.rowLabel))];
                return `<article class="theater-card"><div class="section-heading"><div><p class="eyebrow">${zone.code}</p><h3>${zone.label}</h3></div><span class="pill">${zone.priceLabel}</span></div><div class="seat-grid">${rows
                  .map((row) => {
                    const rowSeats = zoneSeats.filter((seat) => seat.rowLabel === row);
                    return `<div class="seat-row"><span class="row-label">${row}</span>${rowSeats
                      .map((seat) => {
                        const cls = seat.status === "sold" ? "sold" : seat.isMine ? "mine" : seat.status === "locked" ? "locked" : "available";
                        const disabled = seat.status === "sold" || (seat.status === "locked" && !seat.isMine);
                        return `<button class="seat ${cls}" data-seat-id="${seat.id}" ${disabled ? "disabled" : ""} title="${seat.seatKey} - ${seat.priceLabel}"></button>`;
                      })
                      .join("")}</div>`;
                  })
                  .join("")}</div></article>`;
              })
              .join("")}
          </div>
        </div>
        <div class="stack">
          <article class="booking-card">
            <p class="eyebrow">Don hang</p>
            <h3>${show.title}</h3>
            <p class="muted">${selection.count || 0} ghe dang duoc giu.</p>
            <p><strong>${fmtMoney(selection.totalCents || 0)}</strong></p>
            <p class="muted">Het han: ${selection.expiresAt ? fmtTime(selection.expiresAt) : "chua co"}</p>
            <button class="primary-button" id="checkout-btn" ${selection.count ? "" : "disabled"}>Thanh toan</button>
          </article>
        </div>
      </section>
    </section>`;

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
      <div class="section-heading"><div><p class="eyebrow">Admin</p><h2>Dashboard va tao suat chieu</h2></div></div>
      <div class="stat-grid">
        ${state.dashboard.events
          .map(
            (event) => `<article class="stat-card"><p class="eyebrow">${new Date(event.startTime).toLocaleDateString("vi-VN")}</p><h3>${event.title}</h3><p><strong>${event.revenueLabel}</strong></p><p class="muted">Ban ${event.soldSeats}/${event.totalSeats} ghe · lock ${event.lockedSeats} · lap day ${event.fillRate}%</p></article>`
          )
          .join("")}
      </div>
      <div class="member-layout">
        <article class="admin-card"><p class="eyebrow">Audience Stats</p><div class="stack">${state.dashboard.audience.length ? state.dashboard.audience.map((item) => `<p>${item.gender} / ${item.ageBucket}: <strong>${item.count}</strong></p>`).join("") : "<p class='muted'>Chua co du lieu.</p>"}</div></article>
        <article class="admin-card">
          <p class="eyebrow">Tao suat chieu</p>
          <form id="admin-form" class="stack">
            <input name="title" placeholder="Ten phim" required />
            <input name="category" placeholder="Danh muc" value="Phim dang chieu" required />
            <input name="venue" placeholder="Cum rap" required />
            <input name="startTime" type="datetime-local" required />
            <input name="heroColor" value="#0ea5e9" required />
            <textarea name="description" placeholder="Mo ta ngan" required></textarea>
            <button class="primary-button" type="submit">Tao suat chieu</button>
          </form>
        </article>
      </div>
    </section>`;
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
  return `<a class="movie-card page-link-card" href="${href}"><p class="eyebrow">Trang</p><h3>${title}</h3><p class="muted">${description}</p></a>`;
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
  if (result.pointsEarned) alert(`Thanh toan thanh cong. Ban nhan them ${result.pointsEarned} diem.`);
  el.checkoutDialog.close();
  event.currentTarget.reset();
  await bootstrap();
});

bootstrap().catch((error) => {
  document.body.innerHTML = `<pre>${error.message}</pre>`;
});
