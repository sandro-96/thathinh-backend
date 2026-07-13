#!/bin/bash
# Nạp secrets từ SSM Parameter Store → /opt/thathinh/env.sh
# Chạy: sudo bash /opt/thathinh/load-env.sh
set -euo pipefail

REGION=ap-southeast-1
PREFIX=/thathinh/prod
OUT=/opt/thathinh/env.sh

aws ssm get-parameters-by-path \
  --region "$REGION" \
  --path "$PREFIX" \
  --with-decryption \
  --recursive \
  --query "Parameters[].[Name,Value]" \
  --output text \
  | while IFS=$'\t' read -r name value; do
      key="${name##*/}"
      printf '%s=%s\n' "$key" "$value"
    done > "$OUT.tmp"

chmod 600 "$OUT.tmp"
mv "$OUT.tmp" "$OUT"
chown ec2-user:ec2-user "$OUT"
echo "Loaded $(wc -l < "$OUT") params into $OUT"
