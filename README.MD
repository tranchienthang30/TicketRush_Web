# TicketRush

## Ports
Frontend: http://localhost:5173
Backend: http://localhost:8080
PostgreSQL: localhost:5432
Redis: localhost:6379

## Run our group database
Open docker desktop
> docker compose up -d

## Run backend
cd ticket
> mvn spring-boot:run( cần cài maven trong máy cùng jdk21)

## Config web
resources/application.yml

## Run frontend
cd frontend
> npm install
> npm run dev

## Reset local database
> docker compose down -v
> docker compose up -d

## Database rule
- DBML( mô tả cơ sở dữ liệu) is stored in docs
- Can install database client in vs code extension to view database's structure
(Connection String => postgresql://ticketrush:ticketrush@localhost:5432/ticketrush)
- Real schema is created by Flyway migrations(file sql khởi tạo cở sở dữ liệu ở resources/db/migration)
- Nếu muốn xem các phiên bản cơ sở dữ liệu đã được flyway tích hợp
>SELECT version, description, type, script, installed_on, success
>FROM flyway_schema_history
>ORDER BY installed_rank;

- Any DB change must add a new migration

## Auto run docker container and run springboot web
- Open docker desktop
- Use git bash
> ./auto.sh
