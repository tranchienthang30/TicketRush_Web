# TicketRush

Ban microservices Java cho web dat ve xem phim `Starlight Rush`.

## Kien truc

- Gateway/Public web server: `8000`
- Auth service: `9001`
- Catalog service: `9002`
- Booking service: `9003`
- Admin service: `9004`
- Backend da tach class/service rieng: `GatewayService`, `AuthService`, `CatalogService`, `BookingService`, `AdminService`
- Frontend da tach theo trang: moi trang co HTML rieng va file JS entry rieng
- Cau truc frontend: `static/html`, `static/css`, `static/js`

## Chuc nang

- Trang rieng: `Home`, `Phim`, `Rap`, `Gia ve`, `Tin moi`, `Thanh vien`, `Dat ve`, `Admin`
- Dang nhap / dang ky qua popup dung duoc tren moi trang
- Member points va tier (`Member`, `Silver`, `Gold`, `Platinum`)
- Virtual queue, giu ghe 10 phut, checkout gia lap, QR ticket
- Admin dashboard doanh thu, fill-rate, audience stats, tao suat chieu moi

## Chay ung dung

```powershell
javac -d out src\ticketrush\*.java
java -cp out ticketrush.Main
```

Mo `http://127.0.0.1:8000`

## Entrypoint rieng cho tung service

```powershell
java -cp out ticketrush.AuthMain
java -cp out ticketrush.CatalogMain
java -cp out ticketrush.BookingMain
java -cp out ticketrush.AdminMain
java -cp out ticketrush.GatewayMain
```

## Tai khoan demo

- Email: `member@starlightrush.vn`
- Password: `123456`
