#!/bin/bash
set -e

APP_NAME=fillin
NETWORK=fillin_network
NGINX_CONTAINER=nginx
NGINX_CONF=./nginx/conf.d/app.conf

BLUE=backend-blue
GREEN=backend-green

echo "🔍 Checking current live container..."

if docker ps --format '{{.Names}}' | grep -q "${BLUE}"; then
  CURRENT=blue
  NEXT=green
else
  CURRENT=green
  NEXT=blue
fi

echo "✅ Current live: $CURRENT"
echo "🚀 Deploying to: $NEXT"

# 1️⃣ 새 컨테이너 기동
docker compose up -d backend-${NEXT}

echo "⏳ Waiting for health check..."
for i in {1..20}; do
  STATUS=$(docker inspect \
    --format='{{.State.Health.Status}}' \
    fillin-${NEXT} 2>/dev/null || echo "starting")

  if [ "$STATUS" = "healthy" ]; then
    echo "✅ backend-${NEXT} is healthy"
    break
  fi

  echo "⏳ backend-${NEXT} not healthy yet..."
  sleep 5
done

if [ "$STATUS" != "healthy" ]; then
  echo "❌ Health check failed. Aborting."
  exit 1
fi

# 2️⃣ nginx upstream 전환
echo "🔁 Switching nginx upstream to $NEXT"

sed -i "s/backend-${CURRENT}:8080/backend-${NEXT}:8080/g" $NGINX_CONF

docker exec $NGINX_CONTAINER nginx -s reload

# 3️⃣ 기존 컨테이너 종료
echo "🧹 Stopping old container: backend-${CURRENT}"
docker compose stop backend-${CURRENT}

echo "🎉 Deployment completed successfully!"