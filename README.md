# Thả Thính Backend

Spring Boot 3 + MongoDB API cho thathinh.vn

## Chạy local

```bash
# Cần MongoDB chạy tại localhost:27017
cp .env.example .env
./mvnw spring-boot:run
```

API: http://localhost:8080/api  
Swagger: http://localhost:8080/swagger-ui/index.html  
WebSocket: ws://localhost:8080/ws

## Admin mặc định

- Email: `admin@thathinh.vn`
- Password: `Admin@123`

## Deploy (Railway)

1. Tạo project Railway, connect repo
2. Thêm MongoDB plugin
3. Set env: `MONGODB_URI`, `JWT_SECRET`, `CORS_ORIGIN_0=https://thathinh.vn`
4. Domain: `api.thathinh.vn`
