const shell = document.querySelector(".page-shell");
const page = document.body.dataset.page || "home";

const pageTitles = {
  home: "Tổng quan nền tảng",
  movies: "Danh sách phim",
  theaters: "Lịch chiếu theo rạp",
  pricing: "Bảng giá vé",
  news: "Tin mới và khuyến mãi",
  member: "Thành viên và điểm thưởng",
  booking: "Chọn ghế và thanh toán",
  admin: "Bảng điều khiển quản trị",
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
      ${navLink("Phim", "/movies.html")}
      ${navLink("Rạp", "/theaters.html")}
      ${navLink("Giá vé", "/pricing.html")}
      ${navLink("Tin moi", "/news.html")}
      ${navLink("Thành viên", "/member.html")}
      ${navLink("Đặt vé", "/booking.html")}
      ${navLink("Admin", "/admin.html")}
    </nav>
    <div class="auth-row" id="auth-actions"></div>
  </header>

  <main class="page-grid">
    <aside class="sidebar-card">
      <div class="stack-lg">
        <section>
          <p class="eyebrow">Phiên đặt vé</p>
          <div class="client-chip" id="client-chip"></div>
        </section>
        <section id="member-summary"></section>
        <section>
          <div class="section-heading">
            <h3>Cụm rạp đang hoạt động</h3>
            <span class="pill">Live</span>
          </div>
          <div id="events-panel" class="stack"></div>
        </section>
        <section>
          <div class="section-heading">
            <h3>Vé của tôi</h3>
            <span class="pill">QR Ticket</span>
          </div>
          <div id="orders-panel" class="stack"></div>
        </section>
      </div>
    </aside>

    <section class="content-column">
      <section class="section-card page-banner">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Trang hiện tại</p>
            <h2>${pageTitles[page] || pageTitles.home}</h2>
          </div>
        </div>
      </section>
      <div id="main-content"></div>
    </section>
  </main>

  <dialog id="login-dialog">
    <form id="login-form" class="checkout-form">
      <h3>Đăng nhập</h3>
      <label>Email <input name="email" type="email" placeholder="member@starlightrush.vn" required /></label>
      <label>Mật khẩu <input name="password" type="password" placeholder="123456" required /></label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Đóng</button>
        <button type="submit" class="primary-button">Đăng nhập</button>
      </div>
    </form>
  </dialog>

  <dialog id="register-dialog">
    <form id="register-form" class="checkout-form">
      <h3>Đăng ký</h3>
      <label>Họ tên <input name="name" required /></label>
      <label>Email <input name="email" type="email" required /></label>
      <label>Mật khẩu <input name="password" type="password" required /></label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Đóng</button>
        <button type="submit" class="primary-button">Tạo tài khoản</button>
      </div>
    </form>
  </dialog>

  <dialog id="checkout-dialog">
    <form id="checkout-form" class="checkout-form">
      <h3>Xác nhận đơn hàng</h3>
      <label>Họ tên <input name="name" required /></label>
      <label>Email <input name="email" type="email" required /></label>
      <label>Tuổi <input name="age" type="number" min="10" max="99" required /></label>
      <label>Giới tính
        <select name="gender" required>
          <option value="Female">Nữ</option>
          <option value="Male">Nam</option>
          <option value="Other">Khác</option>
        </select>
      </label>
      <div class="dialog-actions">
        <button type="button" class="ghost-button" data-close-dialog>Đóng</button>
        <button type="submit" class="primary-button">Xác nhận thanh toán</button>
      </div>
    </form>
  </dialog>
`;

function navLink(label, href) {
  const active = (page === "home" && href === "/") || href.endsWith(`${page}.html`);
  return `<a class="${active ? "nav-active" : ""}" href="${href}">${label}</a>`;
}
