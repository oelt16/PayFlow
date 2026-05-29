#!/usr/bin/env bash
# Set AWS environment variables for local Floci emulation
# Usage:  source scripts/set-aws-env.sh
#         (the "source" is REQUIRED — it keeps the vars in the current shell)

export AWS_DEFAULT_REGION="us-east-1"
export AWS_ENDPOINT_URL="http://localhost:4566"
export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"

echo "✅ AWS env vars set for local Floci emulation"
echo "   Region:      ${AWS_DEFAULT_REGION}"
echo "   Endpoint:    ${AWS_ENDPOINT_URL}"
echo ""
echo "Now you can run: aws secretsmanager get-secret-value --secret-id /payflow/local/payment-service/db"
