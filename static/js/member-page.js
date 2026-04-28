import { startSite } from "./site-core.js";

const tierPill = document.querySelector("#member-tier-pill");
const overviewEyebrow = document.querySelector("#member-overview-eyebrow");
const memberName = document.querySelector("#member-name");
const memberEmail = document.querySelector("#member-email");
const tierValue = document.querySelector("#member-tier-value");
const pointsValue = document.querySelector("#member-points-value");
const ordersHistory = document.querySelector("#member-orders-history");

startSite({
  getBanner({ currentShow, state }) {
    const show = currentShow();
    return {
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
      slides: [
        { kicker: "Tier status", title: state.session.authenticated ? state.session.user?.tier : "Guest Mode", body: state.session.authenticated ? `${state.session.user?.points || 0} points available for the next booking.` : "Sign in to unlock points, history, and faster checkout.", tone: "#f59e0b" },
        { kicker: "Benefits", title: "Membership rewards travel with you", body: "Your profile, points, and ticket history remain visible while browsing shows and checking out.", tone: "#38bdf8" },
        { kicker: "Ticket memory", title: "Every purchase stays attached", body: "Confirmed orders remain tied to your account for easy access and QR retrieval.", tone: "#ef4444" },
      ],
    };
  },
  render({ state, tierClass }) {
    const signedIn = state.session.authenticated;
    const user = state.session.user || {};
    const tierLabel = signedIn ? user.tier : "Guest Mode";
    tierPill.className = `pill ${tierClass(tierLabel)}`;
    tierPill.textContent = tierLabel;
    overviewEyebrow.textContent = signedIn ? "Account Overview" : "Welcome";
    memberName.textContent = signedIn ? user.name : "Join the Starlight Rush club";
    memberEmail.textContent = signedIn
      ? user.email
      : "Create an account to track points, retain ticket history, and unlock exclusive booking perks.";
    tierValue.textContent = signedIn ? user.tier : "Guest";
    pointsValue.textContent = String(signedIn ? user.points : 0);
    ordersHistory.innerHTML = state.orders.length
      ? state.orders.map((order) => `<p class="muted">${order.eventTitle} / ${order.totalLabel}</p>`).join("")
      : "<p class='muted'>No purchases yet.</p>";
  },
});
