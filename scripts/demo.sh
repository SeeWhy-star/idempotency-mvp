#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:18080}"
account_id="demo-account"
request_body='{"merchantOrderNo":"demo-order-1001","amount":19.99,"currency":"USD"}'

echo "1) 首次创建"
first_response=$(curl --silent --show-error --fail-with-body \
  -X POST "$base_url/v1/payment_intents" \
  -H 'Content-Type: application/json' \
  -H "X-Account-Id: $account_id" \
  -H 'Idempotency-Key: demo-key-1001' \
  -d "$request_body")
echo "$first_response"

echo "2) 相同 key + 相同参数，重放第一次响应"
curl --silent --show-error --fail-with-body \
  -X POST "$base_url/v1/payment_intents" \
  -H 'Content-Type: application/json' \
  -H "X-Account-Id: $account_id" \
  -H 'Idempotency-Key: demo-key-1001' \
  -d "$request_body"
echo

echo "3) 相同 key + 不同参数，预期 HTTP 409"
curl --silent --show-error \
  -X POST "$base_url/v1/payment_intents" \
  -H 'Content-Type: application/json' \
  -H "X-Account-Id: $account_id" \
  -H 'Idempotency-Key: demo-key-1001' \
  -d '{"merchantOrderNo":"demo-order-1001","amount":29.99,"currency":"USD"}'
echo

payment_intent_id=$(printf '%s' "$first_response" | \
  python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
callback_body='{"channelTransactionId":"demo-channel-1","status":"SUCCEEDED"}'

echo "4) 首次回调"
curl --silent --show-error --fail-with-body \
  -X POST "$base_url/v1/callbacks/payments/$payment_intent_id" \
  -H 'Content-Type: application/json' \
  -d "$callback_body"
echo

echo "5) 重复回调：仍返回 accepted=true，transitioned=false"
curl --silent --show-error --fail-with-body \
  -X POST "$base_url/v1/callbacks/payments/$payment_intent_id" \
  -H 'Content-Type: application/json' \
  -d "$callback_body"
echo
