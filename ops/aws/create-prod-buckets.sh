#!/bin/bash
# Tạo S3 buckets prod cho Thả Thính (chạy một lần từ máy có AWS CLI).
set -euo pipefail

REGION=ap-southeast-1

for bucket in thathinh-web-prod thathinh-deploy-prod thathinh-backup-prod; do
  echo "Creating s3://$bucket"
  aws s3 mb "s3://$bucket" --region "$REGION" 2>/dev/null || echo "  (exists)"
done

echo "thathinh-assets — bucket upload đã có sẵn, bỏ qua nếu tồn tại."

# Lifecycle backup 30 ngày
cat > /tmp/thathinh-backup-lifecycle.json <<'EOF'
{
  "Rules": [
    {
      "ID": "expire-mongo-backups",
      "Status": "Enabled",
      "Filter": { "Prefix": "mongo/" },
      "Expiration": { "Days": 30 }
    }
  ]
}
EOF
aws s3api put-bucket-lifecycle-configuration \
  --bucket thathinh-backup-prod \
  --lifecycle-configuration file:///tmp/thathinh-backup-lifecycle.json

echo ""
echo "Done. Tiếp theo:"
echo "  1. Tạo CloudFront distribution cho thathinh-web-prod (OAC, ACM us-east-1)"
echo "  2. Gắn policy IAM cho github-actions-deploy + ec2-salsales-prod"
echo "  3. Xem docs/DEPLOY-PRODUCTION.md"
