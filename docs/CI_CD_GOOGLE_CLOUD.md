# TicketRush CI/CD với Google Cloud VM, Docker Compose và Nginx

Tài liệu này mô tả cách triển khai TicketRush theo hướng tiết kiệm chi phí cho bài tập lớn:

- GitHub Actions chạy CI cho backend và frontend.
- Google Compute Engine VM chạy Docker Compose.
- Nginx làm reverse proxy và phục vụ Vue static files.
- Spring Boot chạy sau Nginx.
- PostgreSQL, Redis và uploads dùng Docker volume trên VM.

## 1. Kiến trúc

```text
User
  -> Nginx :80/:443
    -> Vue static frontend
    -> /api, /oauth2, /uploads -> Spring Boot backend :8080
      -> PostgreSQL
      -> Redis
```

Luồng CI/CD:

```text
Push / Pull Request
  -> GitHub Actions CI
     -> mvn clean test
     -> npm ci + lint check + vite build

Merge main
  -> GitHub Actions Deploy
     -> SSH vào Google VM
     -> git pull --ff-only origin main
     -> docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

## 2. File đã thêm/chỉnh

- `.github/workflows/ci.yml`: chạy test backend và build frontend.
- `.github/workflows/deploy-google-vm.yml`: deploy lên Google VM qua SSH sau khi CI thành công.
- `docker-compose.prod.yml`: stack production.
- `ticket/Dockerfile`: build Spring Boot backend.
- `infra/nginx/Dockerfile`: build Vue và đóng vào Nginx.
- `infra/nginx/default.conf`: route `/api`, `/oauth2`, `/uploads` về backend.
- `.env.prod.example`: mẫu biến môi trường production.
- `ticket/src/main/resources/application.yml`: đổi các config production sang env var.
- `CorsConfig.java`: CORS đọc từ env.
- `AuthCookieService.java` và `CookieUtils.java`: cookie secure/same-site đọc từ env.

## 3. Tạo Google Cloud VM

Khuyến nghị cho demo:

- Machine type: `e2-small` hoặc `e2-medium`.
- OS: Ubuntu 22.04 LTS hoặc 24.04 LTS.
- Disk: 30GB đến 50GB.
- Region: chọn gần Việt Nam nếu có thể, ví dụ Singapore/Asia.
- Firewall: mở HTTP `80`, HTTPS `443`, SSH `22`.

Không nên dùng GKE/Kubernetes cho bài này vì tốn chi phí và phức tạp không cần thiết.

## 4. Cài Docker trên VM

SSH vào VM rồi cài Docker:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc > /dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
```

Đăng xuất SSH rồi đăng nhập lại, sau đó kiểm tra:

```bash
docker --version
docker compose version
```

## 5. Clone repo trên VM

Ví dụ đặt code ở `/opt/ticketrush`:

```bash
sudo mkdir -p /opt/ticketrush
sudo chown -R $USER:$USER /opt/ticketrush
git clone https://github.com/YOUR_ORG/YOUR_REPO.git /opt/ticketrush
cd /opt/ticketrush
```

Nếu repo private, dùng deploy key hoặc GitHub token phù hợp.

## 6. Tạo file `.env.prod` trên VM

Copy từ mẫu:

```bash
cp .env.prod.example .env.prod
nano .env.prod
```

Cấu hình tối thiểu:

```env
PUBLIC_BASE_URL=http://YOUR_VM_EXTERNAL_IP_OR_DOMAIN

POSTGRES_DB=ticketrush
POSTGRES_USER=ticketrush
POSTGRES_PASSWORD=strong-db-password

JWT_SECRET=long-random-secret-at-least-32-characters

COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
JPA_SHOW_SQL=false
```

Khi đã có domain và HTTPS, đổi:

```env
PUBLIC_BASE_URL=https://your-domain.com
COOKIE_SECURE=true
COOKIE_SAME_SITE=Lax
```

Các key PayOS, Google OAuth, seats.io, SMTP điền vào cùng file này. Không commit `.env.prod`.

## 7. Chạy thử production trên VM

```bash
cd /opt/ticketrush
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
```

Mở trình duyệt:

```text
http://YOUR_VM_EXTERNAL_IP_OR_DOMAIN
```

Nếu backend fail vì Flyway/schema, kiểm tra log:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs backend
```

## 8. Cấu hình GitHub Secrets

Vào GitHub repo:

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Thêm các secret:

```text
GCP_VM_HOST=external-ip-or-domain
GCP_VM_USER=ubuntu
GCP_VM_DEPLOY_PATH=/opt/ticketrush
GCP_VM_SSH_PRIVATE_KEY=private-key-content
```

Tạo SSH key deploy từ máy cá nhân hoặc VM:

```bash
ssh-keygen -t ed25519 -C "ticketrush-github-actions" -f ticketrush_github_actions
```

Thêm public key vào VM:

```bash
cat ticketrush_github_actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Nội dung private key `ticketrush_github_actions` đưa vào secret `GCP_VM_SSH_PRIVATE_KEY`.

## 9. Cách CI/CD hoạt động

Khi push hoặc mở Pull Request:

- Backend job tạo PostgreSQL và Redis service trong GitHub Actions.
- Backend chạy `./mvnw -B clean test`.
- Frontend chạy `npm ci`, `npm run lint:check`, `npm run build`.

Khi CI trên nhánh `main` thành công:

- Workflow deploy SSH vào VM.
- VM pull code mới.
- Docker Compose build lại backend/frontend.
- Container restart.
- Flyway migrate database khi backend start.

## 10. Nginx route quan trọng

Nginx đang route:

```text
/                  -> Vue SPA
/api/              -> Spring Boot
/oauth2/authorization/ -> Spring Boot Google OAuth start
/login/oauth2/     -> Spring Boot OAuth callback
/oauth2/callback   -> Vue SPA OAuth result page
/uploads/          -> Spring Boot static upload handler
/actuator/         -> 404 public
```

Nhờ vậy frontend có thể dùng:

```env
VITE_API_BASE_URL=https://your-domain.com
```

và vẫn gọi được:

```text
https://your-domain.com/api/...
```

## 11. HTTPS

Giai đoạn đầu có thể chạy HTTP bằng IP để demo nhanh. Khi có domain, cài Certbot hoặc dùng một reverse proxy HTTPS ở host.

Ví dụ hướng đơn giản:

```bash
sudo apt install -y certbot
```

Sau đó có thể cấu hình Nginx host làm HTTPS termination rồi proxy vào container port `80`, hoặc thay container Nginx để mount certificate. Với bài tập lớn, cách host Nginx/Certbot dễ thao tác hơn nhưng cần tránh chạy trùng port với container Nginx.

## 12. Backup database

Vì Postgres đang chạy trong container, cần backup trước khi demo hoặc trước khi sửa migration:

```bash
docker exec ticketrush-postgres pg_dump -U ticketrush ticketrush > ticketrush_backup.sql
```

Khôi phục:

```bash
cat ticketrush_backup.sql | docker exec -i ticketrush-postgres psql -U ticketrush ticketrush
```

## 13. Kiểm soát chi phí Google Cloud

Nên bật Budget Alert:

- 20%
- 50%
- 80%
- 100%

Không bật GKE/Kubernetes, không tạo nhiều VM, không dùng Cloud SQL cấu hình lớn nếu chưa cần.

## 14. Checklist trước khi demo

- CI pass trên GitHub.
- `docker compose ps` trên VM đều healthy/running.
- Vào được trang chủ.
- Đăng ký/đăng nhập được.
- Provider tạo event, upload banner được.
- Customer booking được trong sale window.
- Admin dashboard mở được.
- `/actuator` không public.
- Backup database đã có.
