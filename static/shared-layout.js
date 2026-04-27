const shell = document.querySelector(".page-shell");
const page = document.body.dataset.page || "home";

const pageTitles = {
  home: "Platform Overview",
  movies: "Now Showing & Coming Soon",
  theaters: "Theater Directory",
  pricing: "Ticket Pricing",
  news: "News & Promotions",
  member: "Membership",
  booking: "Seat Selection & Checkout",
  admin: "Admin Dashboard",
};

shell.innerHTML = `
  <header class="site-header">
    <div class="brand-block">
      <div class="brand-mark">SR</div>
      <div>
        <p class="eyebrow">Microservices Cinema Platform</p>
        <h1>Starlight Rush</h1>
      </div>
    </div>
    <nav class="main-nav">
      ${navLink("Home", "/")}
      ${navLink("Movies", "/movies.html")}
      ${navLink("Theaters", "/theaters.html")}
      ${navLink("Pricing", "/pricing.html")}
      ${navLink("News", "/news.html")}
      ${navLink("Member", "/member.html")}
      ${navLink("Booking", "/booking.html")}
      ${navLink("Admin", "/admin.html")}
    </nav>
    <div class="auth-row" id="auth-actions"></div>
  </header>

  <main class="page-grid">
    <aside class="sidebar-card">
      <div class="stack-lg">
        <section>
          <p class="eyebrow">Current Session</p>
          <div class="client-chip" id="client-chip"></div>
        </section>
        <section id="member-summary"></section>
        <section>
          <div class="section-heading">
            <h3>Active Cinemas</h3>
            <span class="pill">Live</span>
          </div>
          <div id="events-panel" class="stack"></div>
        </section>
        <section>
          <div class="section-heading">
            <h3>My Tickets</h3>
            <span class="pill">QR Pass</span>
          </div>
          <div id="orders-panel" class="stack"></div>
        </section>
      </div>
    </aside>

    <section class="content-column">
      <section class="section-card page-banner">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Current Page</p>
            <h2 id="page-banner-title">${pageTitles[page] || pageTitles.home}</h2>
          </div>
        </div>
        <div class="page-banner-body">
          <div class="page-banner-copy">
            <p class="muted" id="page-banner-summary"></p>
            <div class="quick-stats" id="page-banner-stats"></div>
          </div>
          <div class="page-banner-actions" id="page-banner-actions"></div>
        </div>
      </section>
      <div id="main-content"></div>
    </section>
  </main>

  <dialog id="login-dialog">
    <form id="login-form" class="checkout-form">
      <h3>Sign In</h3>
      <label>Email <input name="email" type="email" placeholder="member@starlightrush.vn" required /></label>
      <label>Password <input name="password" type="password" placeholder="123456" required /></label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Close</button>
        <button type="submit" class="primary-button">Sign In</button>
      </div>
    </form>
  </dialog>

  <dialog id="register-dialog">
    <form id="register-form" class="checkout-form">
      <h3>Create Account</h3>
      <label>Full Name <input name="name" required /></label>
      <label>Email <input name="email" type="email" required /></label>
      <label>Password <input name="password" type="password" required /></label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Close</button>
        <button type="submit" class="primary-button">Register</button>
      </div>
    </form>
  </dialog>

  <dialog id="checkout-dialog">
    <form id="checkout-form" class="checkout-form">
      <h3>Confirm Order</h3>
      <label>Full Name <input name="name" required /></label>
      <label>Email <input name="email" type="email" required /></label>
      <label>Age <input name="age" type="number" min="10" max="99" required /></label>
      <label>Gender
        <select name="gender" required>
          <option value="Female">Female</option>
          <option value="Male">Male</option>
          <option value="Other">Other</option>
        </select>
      </label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Close</button>
        <button type="submit" class="primary-button">Pay Now</button>
      </div>
    </form>
  </dialog>
`;

function navLink(label, href) {
  const active = (page === "home" && href === "/") || href.endsWith(`${page}.html`);
  return `<a class="${active ? "nav-active" : ""}" href="${href}">${label}</a>`;
}
