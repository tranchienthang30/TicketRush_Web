/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          navy: "#006494",
          orange: "#f97316",
          light: "#f8fafc",
          dark: "#0f172a",
        },
      },
    },
  },
  plugins: [],
}
