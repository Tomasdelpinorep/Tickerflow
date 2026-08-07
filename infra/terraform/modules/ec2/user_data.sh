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
echo "0 */6 * * * root aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_registry}" > /etc/cron.d/ecr-refresh

mkdir /opt/ticketflow/
cat > /opt/tickerflow/.env <<-EOF
SPRING_KAFKA_BOOTSTRAP_SERVERS=
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
EOF
