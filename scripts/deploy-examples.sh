#!/usr/bin/env bash
# Deploys every example BPMN process/form under examples/ to a locally running Camunda 8
# REST API (defaults to c8run on localhost:8080, matching application-c8run.yml).
#
# Usage:
#   ./scripts/deploy-examples.sh [rest-address]
#   CAMUNDA_REST_ADDRESS=http://localhost:8080 ./scripts/deploy-examples.sh
set -euo pipefail

REST_ADDRESS="${1:-${CAMUNDA_REST_ADDRESS:-http://localhost:8080}}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLES_DIR="$SCRIPT_DIR/../examples"

form_args=()
count=0
while IFS= read -r -d '' file; do
  form_args+=(-F "resources=@${file}")
  echo "  + ${file#"$EXAMPLES_DIR"/}"
  count=$((count + 1))
done < <(find "$EXAMPLES_DIR" \( -name '*.bpmn' -o -name '*.form' \) -print0)

if [ "$count" -eq 0 ]; then
  echo "No .bpmn/.form files found under $EXAMPLES_DIR" >&2
  exit 1
fi

echo "Deploying $count resource(s) to ${REST_ADDRESS}/v2/deployments ..."
curl -sf -X POST "${REST_ADDRESS}/v2/deployments" "${form_args[@]}"
echo
echo "Done."
