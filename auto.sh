#!/usr/bin/env bash
set -e

# Lấy đường dẫn tuyệt đối của thư mục chứa script này (chính là thư mục gốc dự án)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Khởi động Docker Compose từ thư mục gốc..."
cd "$SCRIPT_DIR"

if [ -f "$SCRIPT_DIR/.env" ]; then
  echo "Nạp biến môi trường từ file .env..."
  set -a
  # shellcheck disable=SC1091
  . "$SCRIPT_DIR/.env"
  set +a
fi

docker compose up -d

echo "Chờ database khởi động (5 giây)..."
sleep 5   # Tăng lên nếu DB cần nhiều thời gian hơn

echo "Chạy Spring Boot từ thư mục 'ticket'..."
cd "$SCRIPT_DIR/ticket"
./mvnw spring-boot:run   # hoặc mvn spring-boot:run nếu bạn dùng Maven toàn cục
