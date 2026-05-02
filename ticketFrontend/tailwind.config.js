/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Cài đặt sẵn bộ màu Cam - Xanh Navy đặc trưng của TicketRush
        brand: {
          navy: '#006494',    // Xanh Navy (dùng cho Header, Footer, Text)
          orange: '#f97316',  // Cam tươi (dùng cho nút bấm, nhấn mạnh)
          light: '#f8fafc'    // Trắng xám (dùng cho nền tổng thể)
        }
      }
    },
  },
  plugins: [],
}