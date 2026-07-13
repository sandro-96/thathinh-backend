#!/bin/bash
# One-time setup Thả Thính trên EC2 đã chạy salsales (Amazon Linux 2023 ARM).
# Chạy trên EC2 qua SSM Session Manager (sudo bash setup-shared-ec2.sh).
set -euo pipefail

APP_DIR=/opt/thathinh
LOG_DIR=/var/log/caddy

echo "==> Creating $APP_DIR"
mkdir -p "$APP_DIR"
chown -R ec2-user:ec2-user "$APP_DIR"

echo "==> Installing systemd unit"
cp "$APP_DIR/thathinh.service" /etc/systemd/system/thathinh.service 2>/dev/null || {
  echo "Copy thathinh.service to $APP_DIR first, then re-run."
  exit 1
}
systemctl daemon-reload
systemctl enable thathinh

echo "==> Caddy log dir"
mkdir -p "$LOG_DIR"
chown caddy:caddy "$LOG_DIR" 2>/dev/null || true

echo "==> Placeholder JAR (replace by CI deploy)"
if [ ! -f "$APP_DIR/app.jar" ]; then
  touch "$APP_DIR/app.jar"
  chown ec2-user:ec2-user "$APP_DIR/app.jar"
fi

echo "==> load-env.sh"
chmod +x "$APP_DIR/load-env.sh"
bash "$APP_DIR/load-env.sh" || echo "WARN: SSM params not ready yet — set /thathinh/prod/* first"

echo ""
echo "Next steps:"
echo "  1. Add api.thathinh.vn block to Caddyfile (see ops/ec2/caddy-snippet.caddy)"
echo "  2. sudo systemctl reload caddy"
echo "  3. DNS: api.thathinh.vn A -> EIP 13.215.133.238"
echo "  4. systemctl start thathinh"
echo "  5. curl -s http://127.0.0.1:8081/actuator/health"
