# Deploy Staging — Thả Thính Backend (Render)

Spring Boot + MongoDB, đóng gói bằng `Dockerfile`. Staging deploy từ nhánh
**`develop`**. Đã có sẵn `render.yaml` (Blueprint) để Render tự tạo service.

> Trước đây định dùng Railway (`railway.toml` vẫn giữ lại), nhưng chuyển sang
> **Render** do Railway hết hạn mức trial.

## 1. Tạo service trên Render

**Cách nhanh (Blueprint):**
1. https://dashboard.render.com → **New → Blueprint**.
2. Chọn repo `sandro-96/thathinh-backend` → Render đọc `render.yaml`.
3. Nhập các biến `sync: false` (secrets) khi được hỏi → **Apply**.

**Hoặc thủ công:**
1. **New → Web Service** → connect `thathinh-backend`, branch `develop`.
2. Runtime **Docker** (tự nhận `Dockerfile`). Region: Singapore. Plan: Free.
3. Health Check Path: `/actuator/health`.

> Render tự cấp biến `PORT` — app đọc `${PORT}` nên **không cần set PORT**.
> Plan Free sẽ **ngủ sau ~15 phút** không có traffic; request đầu sau khi ngủ
> sẽ chậm (Spring Boot cold start ~30–60s).

## 2. Environment Variables

> ⚠️ App dùng `CORS_ORIGIN_0` / `CORS_ORIGIN_1` (KHÔNG phải `CORS_ORIGINS`).

### Bắt buộc
| Key | Giá trị staging | Ghi chú |
|-----|-----------------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Đã set sẵn trong `render.yaml` |
| `MONGODB_URI` | `mongodb+srv://…` | URI bạn đã có |
| `MONGODB_DATABASE` | `thathinhdb` | Đã set sẵn |
| `JWT_SECRET` | *(chuỗi ≥ 32 ký tự)* | Bí mật, sinh ngẫu nhiên |
| `FRONTEND_URL` | `https://<fe-staging>.vercel.app` | Link trong email |
| `CORS_ORIGIN_0` | `https://<fe-staging>.vercel.app` | Cho phép FE gọi API |

### Tùy chọn (đã đặt mặc định tiện cho staging trong `render.yaml`)
| Key | Mặc định | Ghi chú |
|-----|----------|---------|
| `EMAIL_VERIFICATION_REQUIRED` | `false` | Bỏ bước xác thực email |
| `AWS_S3_REQUIRED` | `false` | Không có S3 vẫn chạy; gửi ảnh sẽ báo lỗi/nhẹ nhàng fallback avatar |
| `REDIS_ENABLED` | `false` | Chạy in-memory (1 instance) |

### S3 (chỉ cần nếu muốn bật gửi ảnh)
`AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_S3_PUBLIC_URL_BASE`, `AWS_ACCESS_KEY`,
`AWS_SECRET_KEY` — khi đó đặt `AWS_S3_REQUIRED=true`.

### Khác (tùy chọn)
`GOOGLE_CLIENT_ID` (đăng nhập Google), `ADMIN_SEED_ENABLED=true` +
`ADMIN_EMAIL`/`ADMIN_PASSWORD` (tạo admin lần đầu),
`MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` (gửi email thật).

## 3. Kiểm tra sau deploy

- [ ] `https://<be-staging>.onrender.com/actuator/health` trả `{"status":"UP"}`.
- [ ] FE staging gọi `/api/...` không dính lỗi CORS.
- [ ] WebSocket `/ws` handshake OK.
- [ ] Cập nhật `VITE_API_BASE_URL`/`VITE_WS_URL` bên Vercel = domain Render rồi redeploy FE.

## 4. Lưu ý bảo mật

- Không commit secrets — chỉ đặt ở Render (`.env` đã trong `.gitignore`).
- `JWT_SECRET` staging khác production.
- Nếu dùng S3, tạo bucket + IAM key riêng cho staging.
