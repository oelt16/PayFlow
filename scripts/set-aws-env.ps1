# Set AWS environment variables for local Floci emulation
# Usage:  . .\scripts\set-aws-env.ps1
#         (the dot + space is REQUIRED — it keeps the vars in the current session)

$env:AWS_DEFAULT_REGION    = "us-east-1"
$env:AWS_ENDPOINT_URL      = "http://localhost:4566"
$env:AWS_ACCESS_KEY_ID     = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"

Write-Host "✅ AWS env vars set for local Floci emulation" -ForegroundColor Green
Write-Host "   Region:      $env:AWS_DEFAULT_REGION" -ForegroundColor Cyan
Write-Host "   Endpoint:    $env:AWS_ENDPOINT_URL" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now you can run: aws secretsmanager get-secret-value --secret-id /payflow/local/payment-service/db" -ForegroundColor Yellow
