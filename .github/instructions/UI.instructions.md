**Role:** You are an Expert Senior Frontend Developer and UI/UX Designer. 
**Task:** Build the core frontend layout and components for an event ticketing and management web application called "Starlight".

### 1. Tech Stack & Architecture
* **Framework:** Vue 3 using Composition API (`<script setup>`).
* **Styling:** Tailwind CSS (v3).
* **Routing & State:** Vue Router and Pinia.
* **Target File Structure (Output code must map to these paths):**
    * `src/App.vue` (Root layout)
    * `src/components`
    * `src/assets`
    * `src/router`
    * `src/service`
    * `src/stores`
    * `src/assets`
    * `src/views/`

### 2. Design System & Theming
* **General Vibe:** Modern, clean, spacious (good use of whitespace), and highly responsive.
* **Color Palette:**
    * Primary Deep Sea Blue: `#006494` (Use for main header backgrounds or strong text).
    * Accent Bright Orange: (Tailwind's `orange-500` or `#f97316`) for Call-to-Action buttons, active states, and ticket prices.
    * Backgrounds: White/Light Gray for light mode.
* **Dark/Light Mode:** Must support dynamic theme toggling. Use a reactive state (`ref`) to toggle the `dark` class on the root HTML element. Use Tailwind's `dark:` modifier for all background and text colors (e.g., `bg-white dark:bg-slate-900 text-gray-800 dark:text-gray-100`).

### 3. Component Specifications

**A. Header.vue (Sticky & Responsive)**
* **Position:** Sticky at the top (`sticky top-0 z-50`).
* **Row 1 (Top Bar - Deep Sea Blue Background):**
    * *Left:* Site Logo (Clickable `router-link` back to Home `/`).
    * *Center:* Search Bar component (Input field to search events).
    * *Right:* * "Purchased Tickets" button/icon.
        * Theme Toggle Button (Sun/Moon icon).
        * Auth Section: Create a mock state `const isLoggedIn = ref(true)`. 
            * If `false`: Show "Login / Register" buttons.
            * If `true`: Show User Avatar. On hover, display a choice box/dropdown menu containing "Profile" and "Logout".
* **Row 2 (Bottom Bar - Light Background / Dark in Dark Mode):**
    * *Navigation Links:* Events, Buy Tickets, Create Events, Your Events, Membership.
    * *Hover Effect:* Smooth bottom underline effect (using borders or pseudo-elements) on hover.
* **Mobile Responsive Behavior:** On screens smaller than `md` (768px), **hide Row 2 completely** and replace it with a Hamburger Menu icon in Row 1. Clicking the hamburger menu should open a mobile-friendly side drawer or dropdown containing the Row 2 links.

**B. Footer.vue**
* Contains sections for: About Us, Contact Info (Email, Address, Phone), Support/Help Center links, and Copyright.
* Background should be dark (e.g., Deep Sea Blue or Slate-900).

**C. App.vue (Main Layout)**
* Must wrap the entire application.
* Structure: `<Header />` at the top, a `<main class="flex-grow min-h-screen">` containing `<router-view />` in the middle, and `<Footer />` at the bottom.

**D. HomeView.vue (Landing Page)**
* Displays between the Header and Footer.
* Should include a Hero Section/Banner.
* Should include a "Recent Events" section displaying a grid of Event Cards.
* **Images:** For any place that requires an image (Hero banner, Event thumbnails, Avatar), use a generic gray placeholder div with the text "Insert Image" centered inside it.

### 4. Code Generation Rules
1.  Provide the complete, copy-pasteable code for each file mentioned above.
2.  Start each code block with a comment indicating its filepath (e.g., `// filepath: src/components/layout/Header.vue`).
3.  Ensure all interactive elements (dropdowns, mobile menu, theme toggle) have their logic fully implemented using Vue 3 `<script setup>`.
4.  Do not leave logic blank; write the actual Tailwind classes and Vue directives (`v-if`, `v-for`, `@click`).