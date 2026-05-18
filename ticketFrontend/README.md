# ticketFrontend

## Project Setup

### 1. Di chuyển vào thư mục frontend
cd ticketrush-frontend

### 2. Cài đặt các thư viện (dependencies)
npm install

### 3. Chạy dự án ở chế độ phát triển
npm run dev

## QR quét từ điện thoại (không dùng localhost)

Tạo file `.env` trong thư mục `ticketFrontend`:

```bash
VITE_QR_PUBLIC_BASE_URL=https://your-public-domain
```

`your-public-domain` có thể là domain deploy thật hoặc domain tunnel (ngrok/cloudflared).
