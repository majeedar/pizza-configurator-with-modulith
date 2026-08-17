#!/usr/bin/env bash
# One-time bootstrap for a fresh Hetzner VM (agent.md §27). Run from the repo
# root as: DOMAIN=example.com CERTBOT_EMAIL=admin@example.com ./infrastructure/nginx/init-letsencrypt.sh
#
# Why this two-step dance (dummy cert -> real cert) is necessary: nginx
# refuses to start a `listen 443 ssl` server block if the certificate files
# referenced by ssl_certificate/ssl_certificate_key don't exist yet, but
# certbot's webroot HTTP-01 challenge (used to obtain the *real* certificate)
# requires nginx to already be serving port 80 for
# /.well-known/acme-challenge/. A short-lived self-signed placeholder breaks
# that circular dependency: nginx starts, certbot swaps it for a real cert,
# nginx reloads. This is the standard community pattern for nginx+certbot in
# Compose (not something specific to this project) — reused here rather than
# reinvented.
set -euo pipefail

: "${DOMAIN:?Set DOMAIN, e.g. DOMAIN=example.com}"
: "${CERTBOT_EMAIL:?Set CERTBOT_EMAIL, e.g. CERTBOT_EMAIL=admin@example.com}"

COMPOSE="docker compose -f compose.prod.yaml"
DATA_PATH="./infrastructure/nginx/certbot"

echo "### Creating dummy self-signed certificate for ${DOMAIN} ..."
mkdir -p "$DATA_PATH/conf/live/$DOMAIN"
docker run --rm -v "$DATA_PATH/conf:/etc/letsencrypt" certbot/certbot \
  sh -c "openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout '/etc/letsencrypt/live/$DOMAIN/privkey.pem' \
    -out '/etc/letsencrypt/live/$DOMAIN/fullchain.pem' \
    -subj '/CN=localhost'"

echo "### Starting nginx with the dummy certificate ..."
$COMPOSE up -d nginx

echo "### Deleting dummy certificate ..."
docker run --rm -v "$DATA_PATH/conf:/etc/letsencrypt" certbot/certbot \
  sh -c "rm -rf /etc/letsencrypt/live/$DOMAIN /etc/letsencrypt/archive/$DOMAIN /etc/letsencrypt/renewal/$DOMAIN.conf"

echo "### Requesting the real Let's Encrypt certificate ..."
docker run --rm \
  -v "$DATA_PATH/conf:/etc/letsencrypt" \
  -v "$DATA_PATH/www:/var/www/certbot" \
  certbot/certbot certonly --webroot -w /var/www/certbot \
    --cert-name "$DOMAIN" \
    -d "app.$DOMAIN" -d "staff.$DOMAIN" -d "api.$DOMAIN" \
    --email "$CERTBOT_EMAIL" --agree-tos --no-eff-email
# --cert-name pins the on-disk directory to live/$DOMAIN/ regardless of
# which -d comes first — matches ssl_certificate paths in
# infrastructure/nginx/templates/default.conf.template.

echo "### Reloading nginx with the real certificate ..."
$COMPOSE exec nginx nginx -s reload

echo "### Done. Renewal runs automatically via the 'certbot' service's twice-daily loop (see compose.prod.yaml)."
