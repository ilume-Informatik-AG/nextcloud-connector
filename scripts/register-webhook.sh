#!/usr/bin/env bash
# Registers/manages Nextcloud "Webhook Listeners" (webhook_listeners) webhooks via its OCS API,
# for use with the inbound connector - see nextcloud-connector-inbound/README.md for the full
# field reference, supported event classes, and the BPMN-side configuration.
#
# Docs:
#   https://docs.nextcloud.com/server/latest/admin_manual/webhook_listeners/index.html
#   https://docs.nextcloud.com/server/latest/developer_manual/_static/openapi.html#/operations/webhook_listeners-webhooks-index
#
# Usage:
#   ./scripts/register-webhook.sh list
#   ./scripts/register-webhook.sh get <id>
#   ./scripts/register-webhook.sh create <uri> <event-class> [secret] [header-name]
#   ./scripts/register-webhook.sh update <id> <uri> <event-class> [secret] [header-name]
#   ./scripts/register-webhook.sh delete <id>
#
# Env vars (defaults match this repo's local dev docker-compose setup, see .env.example):
#   NEXTCLOUD_URL             Base URL of the Nextcloud instance (default: http://localhost:9000)
#   NEXTCLOUD_ADMIN_USER      Admin username (default: demo)
#   NEXTCLOUD_ADMIN_PASSWORD  Admin password / app password (default: demo)
#
# Example:
#   ./scripts/register-webhook.sh create \
#     http://host.docker.internal:8080/inbound/my-webhook-id \
#     'OCP\Files\Events\Node\NodeCreatedEvent' \
#     a-long-random-value
set -euo pipefail

NEXTCLOUD_URL="${NEXTCLOUD_URL:-http://localhost:9000}"
NEXTCLOUD_ADMIN_USER="${NEXTCLOUD_ADMIN_USER:-demo}"
NEXTCLOUD_ADMIN_PASSWORD="${NEXTCLOUD_ADMIN_PASSWORD:-demo}"
DEFAULT_HEADER_NAME="X-Webhook-Secret"
API_BASE="${NEXTCLOUD_URL%/}/ocs/v2.php/apps/webhook_listeners/api/v1/webhooks"

usage() {
  cat <<EOF
Usage:
  ./scripts/register-webhook.sh list
  ./scripts/register-webhook.sh get <id>
  ./scripts/register-webhook.sh create <uri> <event-class> [secret] [header-name]
  ./scripts/register-webhook.sh update <id> <uri> <event-class> [secret] [header-name]
  ./scripts/register-webhook.sh delete <id>

Env vars:
  NEXTCLOUD_URL             Base URL of the Nextcloud instance (default: http://localhost:9000)
  NEXTCLOUD_ADMIN_USER      Admin username (default: demo)
  NEXTCLOUD_ADMIN_PASSWORD  Admin password / app password (default: demo)

Supported event classes (see nextcloud-connector-inbound/README.md for the full table):
  OCP\\Files\\Events\\Node\\NodeCreatedEvent
  OCP\\Files\\Events\\Node\\NodeWrittenEvent
  OCP\\Files\\Events\\Node\\NodeDeletedEvent
  OCP\\Files\\Events\\Node\\NodeRenamedEvent
  OCP\\Files\\Events\\Node\\NodeCopiedEvent
  OCP\\Files\\Events\\Node\\NodeTouchedEvent
EOF
}

curl_ocs() {
  curl -sf -u "${NEXTCLOUD_ADMIN_USER}:${NEXTCLOUD_ADMIN_PASSWORD}" \
    -H "OCS-APIRequest: true" -H "Accept: application/json" "$@"
}

put_webhook() {
  local target="$1" uri="$2" event="$3" secret="${4:-}" header_name="${5:-$DEFAULT_HEADER_NAME}"
  local headers_json="{}"
  if [ -n "$secret" ]; then
    headers_json="{\"${header_name}\": \"${secret}\"}"
  fi
  curl_ocs -X POST -H "Content-Type: application/json" "${target}" -d "{
    \"httpMethod\": \"POST\",
    \"uri\": \"${uri}\",
    \"event\": \"${event}\",
    \"headers\": ${headers_json}
  }"
  echo
}

cmd="${1:-}"
case "$cmd" in
  list)
    curl_ocs "${API_BASE}"
    echo
    ;;
  get)
    id="${2:?webhook id required, e.g. ./scripts/register-webhook.sh get 3}"
    curl_ocs "${API_BASE}/${id}"
    echo
    ;;
  create)
    uri="${2:?uri required}"
    event="${3:?event class required, e.g. 'OCP\\Files\\Events\\Node\\NodeCreatedEvent'}"
    put_webhook "${API_BASE}" "${uri}" "${event}" "${4:-}" "${5:-}"
    ;;
  update)
    id="${2:?webhook id required}"
    uri="${3:?uri required}"
    event="${4:?event class required}"
    put_webhook "${API_BASE}/${id}" "${uri}" "${event}" "${5:-}" "${6:-}"
    ;;
  delete)
    id="${2:?webhook id required}"
    curl_ocs -X DELETE "${API_BASE}/${id}"
    echo
    ;;
  *)
    usage
    exit 1
    ;;
esac
