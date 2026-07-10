# Deploy Staging — Thả Thính Backend (Railway)

Spring Boot + MongoDB, đóng gói bằng `Dockerfile`. Railway đã cấu hình sẵn qua
`railway.toml` (builder Dockerfile, healthcheck `/actuator/health`). Staging
deploy từ nhánh **`develop`**.

## 1. Tạo service trên Railway

1. Railway → **New Project → Deploy from GitHub repo** → chọn `sandro-96/thathinh-backend`.
2. Chọn branch **`develop`**.
3. Railway tự đọc `railway.toml` → build bằng `Dockerfile`, expose cổng từ `$PORT`.
4. (Nếu chưa có DB) thêm plugin **MongoDB** trong project, hoặc dùng **MongoDB Atlas** free tier.

## 2. Environment Variables (Railway → service → Variables)

> ⚠️ App dùng biến `CORS_ORIGIN_0` / `CORS_ORIGIN_1` (KHÔNG phải `CORS_ORIGINS`).
> `PORT` do Railway tự cấp — không cần set.

### Bắt buộc
| Key | Giá trị staging | Ghi chú |
|-----|-----------------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Bật cấu hình production |
| `MONGODB_URI` | `mongodb+srv://…` | Atlas/Railway Mongo staging |
| `MONGODB_DATABASE` | `thathinhdb` | Có thể để mặc định |
| `JWT_SECRET` | *(chuỗi ≥ 32 ký tự)* | Bí mật, sinh ngẫu nhiên |
| `FRONTEND_URL` | `https://<fe-staging>.vercel.app` | Dùng cho link email |
| `CORS_ORIGIN_0` | `https://<fe-staging>.vercel.app` | Cho phép FE gọi API |

### S3 (bắt buộc khi profile = prod — gửi ảnh trong chat)
| Key | Ghi chú |
|-----|---------|
| `AWS_S3_BUCKET` | tên bucket staging |
| `AWS_S3_REGION` | vd `ap-southeast-1` |
| `AWS_S3_PUBLIC_URL_BASE` | vd `https://<bucket>.s3.<region>.amazonaws.com` |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | IAM user quyền tối thiểu (PutObject/GetObject) |

> Không có S3 cho staging? Đặt thêm `AWS_S3_REQUIRED=false` để app vẫn khởi động
> (khi đó tính năng gửi ảnh sẽ báo lỗi khi dùng).

### Email (tùy chọn)
| Key | Ghi chú |
|-----|---------|
| `EMAIL_VERIFICATION_REQUIRED` | `false` để bỏ bước xác thực email trên staging |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | Nếu muốn bật gửi email thật |

### Seed admin (tùy chọn)
| Key | Ghi chú |
|-----|---------|
| `ADMIN_SEED_ENABLED` | `true` để tạo admin lần đầu |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Thông tin admin |

### Google OAuth / Redis (tùy chọn)
| Key | Ghi chú |
|-----|---------|
| `GOOGLE_CLIENT_ID` | Nếu dùng đăng nhập Google |
| `REDIS_ENABLED` | Giữ `false` cho staging 1 instance |

## 3. Kiểm tra sau deploy

- [ ] `https://<be-staging>.up.railway.app/actuator/health` trả `{"status":"UP"}` (Mongo UP).
- [ ] FE staging gọi `/api/...` không dính lỗi CORS.
- [ ] WebSocket `/ws` handshake OK.
- [ ] Sau khi có domain BE, cập nhật `VITE_API_BASE_URL` / `VITE_WS_URL` bên Vercel rồi redeploy FE.

## 4. Lưu ý bảo mật

- Không commit secrets — chỉ đặt ở Railway Variables (`.env` đã trong `.gitignore`).
- `JWT_SECRET` staging khác production.
- Dùng bucket + IAM key riêng cho staging, tách khỏi production.
