#!/bin/bash

# Means: stop the script immediately if any command fails. Without it, if Docker installation fails, the script keeps
# running and tries to pull images
set -euo pipefail

yum update -y
yum install -y docker
# start docker on reboot
systemctl enable docker
#starts docker now
systemctl start docker

# Docker auth
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_registry}

# create cron job to refresh credentials every 6 hours
yum install -y cronie
systemctl enable crond
systemctl start crond
echo "0 */6 * * * root aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_registry}" > /etc/cron.d/ecr-refresh

mkdir /opt/tickerflow/
cat > /opt/tickerflow/.env <<-EOF
SCHEMA_REGISTRY_URL=${schema_registry_url}
SCHEMA_REGISTRY_API_KEY=${schema_registry_api_key}
SCHEMA_REGISTRY_API_SECRET=${schema_registry_api_secret}
SPRING_PROFILES_ACTIVE=prod
SPRING_KAFKA_BOOTSTRAP_SERVERS=${spring_kafka_bootstrap_servers}
SPRING_KAFKA_API_KEY=${spring_kafka_api_key}
SPRING_KAFKA_API_SECRET=${spring_kafka_api_secret}
SPRING_DATASOURCE_URL=${spring_datasource_url}
SPRING_DATASOURCE_USERNAME=${spring_datasource_username}
SPRING_DATASOURCE_PASSWORD=${spring_datasource_password}
MAILGUN_SMTP_USERNAME=${mailgun_smtp_username}
MAILGUN_SMTP_PASSWORD=${mailgun_smtp_password}
PERSONAL_EMAIL=${personal_email}
FINNHUB_API_KEY=${finnhub_api_key}
EOF

docker run -d \
  --name ingestion-service \
  --restart unless-stopped \
  --env-file /opt/tickerflow/.env \
  "${ecr_registry}"/tickerflow/ingestion-service:latest

docker run -d \
  --name candle-aggregator \
  --restart unless-stopped \
  --env-file /opt/tickerflow/.env \
  "${ecr_registry}"/tickerflow/candle-aggregator:latest

docker run -d \
  --name moving-avg-processor \
  --restart unless-stopped \
  --env-file /opt/tickerflow/.env \
  "${ecr_registry}"/tickerflow/moving-avg-processor:latest

docker run -d \
  --name trading-engine \
  --restart unless-stopped \
  --env-file /opt/tickerflow/.env \
  "${ecr_registry}"/tickerflow/trading-engine:latest

docker run -d \
  --name notification-service \
  --restart unless-stopped \
  --env-file /opt/tickerflow/.env \
  "${ecr_registry}"/tickerflow/notification-service:latest