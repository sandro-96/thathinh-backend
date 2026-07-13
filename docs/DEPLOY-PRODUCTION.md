# Deploy Production — Backend

Hướng dẫn đầy đủ (shared EC2 với salsales, SSM, CI/CD): repo **`sandro-96/thathinh-web`** → `docs/DEPLOY-PRODUCTION.md`

## File ops trong repo này

| Path | Mục đích |
|------|----------|
| `ops/ec2/thathinh.service` | systemd unit (port 8081) |
| `ops/ec2/load-env.sh` | Nạp SSM → `/opt/thathinh/env.sh` |
| `ops/ec2/caddy-snippet.caddy` | Block Caddy `api.thathinh.vn` |
| `ops/ec2/setup-shared-ec2.sh` | Script setup một lần trên EC2 |
| `ops/aws/create-prod-buckets.sh` | Tạo S3 buckets |
| `ops/aws/ssm-parameters.example.env` | Danh sách biến SSM |
| `ops/aws/iam-*.json` | Policy mở rộng IAM |
| `.github/workflows/deploy-backend-prod.yml` | CI deploy `main` → S3 → SSM → EC2 |

## Health

- Local EC2: `http://127.0.0.1:8081/actuator/health`
- Public: `https://api.thathinh.vn/actuator/health`
